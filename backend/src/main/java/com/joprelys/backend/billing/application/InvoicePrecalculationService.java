package com.joprelys.backend.billing.application;

import com.joprelys.backend.billing.api.InvoiceResponse;
import com.joprelys.backend.billing.infrastructure.persistence.*;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.*;
import com.joprelys.backend.prescription.infrastructure.persistence.*;
import com.joprelys.backend.spatial.infrastructure.persistence.RoomRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Calcul préventif d'une facture à partir des données cliniques d'une visite
 * (hospitalisation, soins, médicaments, blocs opératoires).
 * N'écrit rien en base — lecture seule.
 */
@Service
public class InvoicePrecalculationService {

    private final ConventionTariffService conventionTariffService;
    private final InsuranceConventionRepository insuranceConventionRepository;
    private final VisitRepository visitRepository;
    private final HospitalizationRepository hospitalizationRepository;
    private final HospitalizationDailyCareRepository dailyCareRepository;
    private final PatientConsumptionRepository patientConsumptionRepository;
    private final OperatingReportRepository operatingReportRepository;
    private final ConsultationRepository consultationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final RoomRepository roomRepository;

    public InvoicePrecalculationService(ConventionTariffService conventionTariffService,
                                        InsuranceConventionRepository insuranceConventionRepository,
                                        VisitRepository visitRepository,
                                        HospitalizationRepository hospitalizationRepository,
                                        HospitalizationDailyCareRepository dailyCareRepository,
                                        PatientConsumptionRepository patientConsumptionRepository,
                                        OperatingReportRepository operatingReportRepository,
                                        ConsultationRepository consultationRepository,
                                        PrescriptionRepository prescriptionRepository,
                                        RoomRepository roomRepository) {
        this.conventionTariffService = conventionTariffService;
        this.insuranceConventionRepository = insuranceConventionRepository;
        this.visitRepository = visitRepository;
        this.hospitalizationRepository = hospitalizationRepository;
        this.dailyCareRepository = dailyCareRepository;
        this.patientConsumptionRepository = patientConsumptionRepository;
        this.operatingReportRepository = operatingReportRepository;
        this.consultationRepository = consultationRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    public InvoiceResponse precalculate(UUID patientId, UUID visitId, UUID insuranceConventionId) {
        VisitEntity visit = null;
        if (visitId != null) {
            visit = visitRepository.findById(visitId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable"));
        }

        InsuranceConventionEntity convention = null;
        if (insuranceConventionId != null) {
            convention = insuranceConventionRepository.findById(insuranceConventionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Convention introuvable"));
        }

        InvoiceEntity invoice = new InvoiceEntity(patientId, visitId, "PRE-CALC", convention);

        // 1. Consultation
        BigDecimal consultationPrice = conventionTariffService.getTariff("CS", new BigDecimal("15000"));
        if (visit != null) {
            invoice.addItem(new InvoiceItemEntity(
                    "Consultation - " + visit.getReason(),
                    InvoiceItemType.CONSULTATION,
                    consultationPrice,
                    BigDecimal.ONE,
                    null
            ));
        }

        // 2. Hospitalisation
        if (visit != null) {
            Optional<HospitalizationEntity> hospOpt = hospitalizationRepository.findByVisitId(visitId);
            if (hospOpt.isPresent()) {
                HospitalizationEntity hosp = hospOpt.get();
                addHospitalizationItems(invoice, hosp);
            }
        }

        // 3. Médicaments (ordonnances)
        if (visit != null) {
            addPrescriptionItems(invoice, visitId);
        }

        return InvoiceResponse.fromEntity(invoice);
    }

    // -------------------------------------------------------
    // Helpers privés
    // -------------------------------------------------------

    private void addHospitalizationItems(InvoiceEntity invoice, HospitalizationEntity hosp) {
        // Frais de séjour
        LocalDate start = hosp.getAdmittedAt().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate end = hosp.getDischargedAt() != null
                ? hosp.getDischargedAt().atZone(ZoneId.systemDefault()).toLocalDate()
                : LocalDate.now();
        long days = Math.max(1, ChronoUnit.DAYS.between(start, end));

        String comfort = "STANDARD";
        BigDecimal stayPrice = conventionTariffService.getTariff("ROOM_STANDARD", new BigDecimal("10000"));
        if (hosp.getRoomNumber() != null) {
            try {
                String comfortLevel = roomRepository.findAll().stream()
                        .filter(r -> r.getRoomNumber().equalsIgnoreCase(hosp.getRoomNumber()))
                        .map(r -> r.getComfortLevel())
                        .findFirst()
                        .orElse("STANDARD");
                comfort = comfortLevel.toUpperCase();
                stayPrice = conventionTariffService.getTariff(
                        "ROOM_" + comfort,
                        comfort.equals("VIP") ? new BigDecimal("25000") : new BigDecimal("10000")
                );
            } catch (Exception ignored) {}
        }

        invoice.addItem(new InvoiceItemEntity(
                "Frais de séjour en chambre (" + comfort + ", N° " + hosp.getRoomNumber() + ", " + days + " nuits)",
                InvoiceItemType.STAY_FEE,
                stayPrice,
                BigDecimal.valueOf(days),
                null
        ));

        addDailyCares(invoice, hosp.getId());
        addConsumptions(invoice, hosp.getId());
        addOperatingReports(invoice, hosp.getId());
    }

    private void addDailyCares(InvoiceEntity invoice, UUID hospitalizationId) {
        try {
            List<HospitalizationDailyCareEntity> cares =
                    dailyCareRepository.findByHospitalizationIdOrderByPerformedAtDesc(hospitalizationId);
            for (HospitalizationDailyCareEntity dc : cares) {
                if (dc.isBillable()) {
                    BigDecimal carePrice = dc.getPrice() != null
                            ? dc.getPrice()
                            : conventionTariffService.getTariff(dc.getCareType(), new BigDecimal("5000"));
                    String label = "Soin : " + dc.getCareType()
                            + (dc.getDescription() != null && !dc.getDescription().isEmpty()
                            ? " (" + dc.getDescription() + ")" : "");
                    invoice.addItem(new InvoiceItemEntity(label, InvoiceItemType.AMI_CARE, carePrice, BigDecimal.ONE, null));
                }
            }
        } catch (Exception ignored) {}
    }

    private void addConsumptions(InvoiceEntity invoice, UUID hospitalizationId) {
        try {
            List<PatientConsumptionEntity> consumptions =
                    patientConsumptionRepository.findByHospitalizationIdOrderByConsumedAtDesc(hospitalizationId);
            for (PatientConsumptionEntity pc : consumptions) {
                BigDecimal consPrice = pc.getUnitPrice() != null && pc.getUnitPrice().compareTo(BigDecimal.ZERO) > 0
                        ? pc.getUnitPrice()
                        : conventionTariffService.getTariff(pc.getItemName(), new BigDecimal("1500"));
                invoice.addItem(new InvoiceItemEntity(
                        "Consommation : " + pc.getItemName(),
                        InvoiceItemType.MEDICATION,
                        consPrice,
                        BigDecimal.valueOf(pc.getQuantity()),
                        null
                ));
            }
        } catch (Exception ignored) {}
    }

    private void addOperatingReports(InvoiceEntity invoice, UUID hospitalizationId) {
        try {
            List<OperatingReportEntity> reports =
                    operatingReportRepository.findByHospitalizationIdOrderByOperationDateDesc(hospitalizationId);
            for (OperatingReportEntity op : reports) {
                if (!op.isValidated()) continue;
                BigDecimal kValue = conventionTariffService.getTariff("K", new BigDecimal("1000"));

                if (op.getkSurgeonValue() > 0) {
                    invoice.addItem(new InvoiceItemEntity(
                            "CRO : Honoraires Chirurgien (K " + op.getkSurgeonValue() + " - " + op.getProcedureName() + ")",
                            InvoiceItemType.K_SURGEON,
                            kValue.multiply(BigDecimal.valueOf(op.getkSurgeonValue())),
                            BigDecimal.ONE, null
                    ));
                }
                if (op.getkAnesthesistValue() > 0) {
                    invoice.addItem(new InvoiceItemEntity(
                            "CRO : Honoraires Anesthésiste (K " + op.getkAnesthesistValue() + " - " + op.getProcedureName() + ")",
                            InvoiceItemType.K_ANESTHESIST,
                            kValue.multiply(BigDecimal.valueOf(op.getkAnesthesistValue())),
                            BigDecimal.ONE, null
                    ));
                }
                if (op.getkBlocValue() > 0) {
                    invoice.addItem(new InvoiceItemEntity(
                            "CRO : Frais de Bloc Opératoire (K " + op.getkBlocValue() + " - " + op.getProcedureName() + ")",
                            InvoiceItemType.K_BLOC,
                            kValue.multiply(BigDecimal.valueOf(op.getkBlocValue())),
                            BigDecimal.ONE, null
                    ));
                }

                if (op.getImplants() != null) {
                    for (var implant : op.getImplants()) {
                        BigDecimal implantPrice = implant.getUnitPrice() != null && implant.getUnitPrice().compareTo(BigDecimal.ZERO) > 0
                                ? implant.getUnitPrice()
                                : conventionTariffService.getTariff(implant.getImplantName(), new BigDecimal("25000"));
                        String label = "Implant : " + implant.getImplantName()
                                + (implant.getLotNumber() != null && !implant.getLotNumber().isEmpty()
                                ? " (Lot: " + implant.getLotNumber() + ")" : "");
                        invoice.addItem(new InvoiceItemEntity(
                                label, InvoiceItemType.MEDICATION,
                                implantPrice, BigDecimal.valueOf(implant.getQuantity()), null
                        ));
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void addPrescriptionItems(InvoiceEntity invoice, UUID visitId) {
        try {
            Optional<ConsultationEntity> consultOpt = consultationRepository.findByVisitId(visitId);
            if (consultOpt.isEmpty()) return;
            Optional<PrescriptionEntity> presOpt = prescriptionRepository.findByConsultationId(consultOpt.get().getId());
            if (presOpt.isEmpty()) return;
            for (PrescriptionItemEntity item : presOpt.get().getItems()) {
                BigDecimal qty = BigDecimal.ONE;
                try {
                    String cleanQty = item.getQuantity().replaceAll("[^\\d.]", "");
                    if (!cleanQty.isEmpty()) qty = new BigDecimal(cleanQty);
                } catch (Exception ignored) {}

                BigDecimal drugPrice = conventionTariffService.getTariff(item.getDrugName(), new BigDecimal("2500"));
                invoice.addItem(new InvoiceItemEntity(
                        "Médicament : " + item.getDrugName() + " (" + item.getDosage() + ")",
                        InvoiceItemType.MEDICATION,
                        drugPrice,
                        qty,
                        null
                ));
            }
        } catch (Exception ignored) {}
    }
}
