package com.joprelys.backend.billing.application;

import com.joprelys.backend.billing.api.InvoiceResponse;
import com.joprelys.backend.billing.infrastructure.persistence.InsuranceConventionEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InsuranceConventionRepository;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceItemEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceItemType;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationDailyCareEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationDailyCareRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.OperatingReportEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.OperatingReportRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.PatientConsumptionEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.PatientConsumptionRepository;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionItemEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceProfileRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    private final InpatientSpaceProfileRepository inpatientSpaceProfileRepository;

    public InvoicePrecalculationService(
            ConventionTariffService conventionTariffService,
            InsuranceConventionRepository insuranceConventionRepository,
            VisitRepository visitRepository,
            HospitalizationRepository hospitalizationRepository,
            HospitalizationDailyCareRepository dailyCareRepository,
            PatientConsumptionRepository patientConsumptionRepository,
            OperatingReportRepository operatingReportRepository,
            ConsultationRepository consultationRepository,
            PrescriptionRepository prescriptionRepository,
            InpatientSpaceProfileRepository inpatientSpaceProfileRepository) {
        this.conventionTariffService = conventionTariffService;
        this.insuranceConventionRepository = insuranceConventionRepository;
        this.visitRepository = visitRepository;
        this.hospitalizationRepository = hospitalizationRepository;
        this.dailyCareRepository = dailyCareRepository;
        this.patientConsumptionRepository = patientConsumptionRepository;
        this.operatingReportRepository = operatingReportRepository;
        this.consultationRepository = consultationRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.inpatientSpaceProfileRepository = inpatientSpaceProfileRepository;
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

        BigDecimal consultationPrice = conventionTariffService.getTariff("CS", new BigDecimal("15000"));
        if (visit != null) {
            invoice.addItem(new InvoiceItemEntity(
                    "Consultation - " + visit.getReason(),
                    InvoiceItemType.CONSULTATION,
                    consultationPrice,
                    BigDecimal.ONE,
                    null));
        }

        if (visit != null) {
            hospitalizationRepository.findByVisitId(visitId)
                    .ifPresent(hospitalization -> addHospitalizationItems(invoice, hospitalization));
            addPrescriptionItems(invoice, visitId);
        }

        return InvoiceResponse.fromEntity(invoice);
    }

    private void addHospitalizationItems(InvoiceEntity invoice, HospitalizationEntity hospitalization) {
        LocalDate start = hospitalization.getAdmittedAt().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate end = hospitalization.getDischargedAt() != null
                ? hospitalization.getDischargedAt().atZone(ZoneId.systemDefault()).toLocalDate()
                : LocalDate.now();
        long days = Math.max(1, ChronoUnit.DAYS.between(start, end));

        String comfort = inpatientSpaceProfileRepository
                .findBySpaceIdAndOrganizationId(
                        hospitalization.getCurrentSpaceId(),
                        hospitalization.getOrganizationId())
                .map(profile -> profile.getComfortLevel())
                .orElse("STANDARD")
                .toUpperCase();
        BigDecimal stayPrice = conventionTariffService.getTariff(
                "ROOM_" + comfort,
                comfort.equals("VIP") ? new BigDecimal("25000") : new BigDecimal("10000"));

        invoice.addItem(new InvoiceItemEntity(
                "Frais de séjour (" + comfort + ", espace " + hospitalization.getSpaceName()
                        + ", " + days + " nuits)",
                InvoiceItemType.STAY_FEE,
                stayPrice,
                BigDecimal.valueOf(days),
                null));

        addDailyCares(invoice, hospitalization.getId());
        addConsumptions(invoice, hospitalization.getId());
        addOperatingReports(invoice, hospitalization.getId());
    }

    private void addDailyCares(InvoiceEntity invoice, UUID hospitalizationId) {
        try {
            List<HospitalizationDailyCareEntity> cares =
                    dailyCareRepository.findByHospitalizationIdOrderByPerformedAtDesc(hospitalizationId);
            for (HospitalizationDailyCareEntity care : cares) {
                if (care.isBillable()) {
                    BigDecimal carePrice = care.getPrice() != null
                            ? BigDecimal.valueOf(care.getPrice())
                            : conventionTariffService.getTariff(care.getCareType(), new BigDecimal("5000"));
                    String label = "Soin : " + care.getCareType()
                            + (care.getDescription() != null && !care.getDescription().isEmpty()
                                    ? " (" + care.getDescription() + ")"
                                    : "");
                    invoice.addItem(new InvoiceItemEntity(
                            label,
                            InvoiceItemType.AMI_CARE,
                            carePrice,
                            BigDecimal.ONE,
                            null));
                }
            }
        } catch (Exception ignored) {
            // La pré-facturation reste tolérante aux modules optionnels non configurés.
        }
    }

    private void addConsumptions(InvoiceEntity invoice, UUID hospitalizationId) {
        try {
            List<PatientConsumptionEntity> consumptions =
                    patientConsumptionRepository.findByHospitalizationIdOrderByConsumedAtDesc(hospitalizationId);
            for (PatientConsumptionEntity consumption : consumptions) {
                Double unitPrice = consumption.getUnitPrice();
                BigDecimal consumptionPrice = unitPrice != null && unitPrice > 0
                        ? BigDecimal.valueOf(unitPrice)
                        : conventionTariffService.getTariff(consumption.getItemName(), new BigDecimal("1500"));
                invoice.addItem(new InvoiceItemEntity(
                        "Consommation : " + consumption.getItemName(),
                        InvoiceItemType.MEDICATION,
                        consumptionPrice,
                        BigDecimal.valueOf(consumption.getQuantity()),
                        null));
            }
        } catch (Exception ignored) {
            // La pré-facturation reste tolérante aux modules optionnels non configurés.
        }
    }

    private void addOperatingReports(InvoiceEntity invoice, UUID hospitalizationId) {
        try {
            List<OperatingReportEntity> reports =
                    operatingReportRepository.findByHospitalizationIdOrderByOperationDateDesc(hospitalizationId);
            for (OperatingReportEntity report : reports) {
                if (!report.isValidated()) {
                    continue;
                }
                BigDecimal kValue = conventionTariffService.getTariff("K", new BigDecimal("1000"));

                if (report.getkSurgeonValue() > 0) {
                    invoice.addItem(new InvoiceItemEntity(
                            "CRO : Honoraires Chirurgien (K " + report.getkSurgeonValue()
                                    + " - " + report.getProcedureName() + ")",
                            InvoiceItemType.K_SURGEON,
                            kValue.multiply(BigDecimal.valueOf(report.getkSurgeonValue())),
                            BigDecimal.ONE,
                            null));
                }
                if (report.getkAnesthesistValue() > 0) {
                    invoice.addItem(new InvoiceItemEntity(
                            "CRO : Honoraires Anesthésiste (K " + report.getkAnesthesistValue()
                                    + " - " + report.getProcedureName() + ")",
                            InvoiceItemType.K_ANESTHESIST,
                            kValue.multiply(BigDecimal.valueOf(report.getkAnesthesistValue())),
                            BigDecimal.ONE,
                            null));
                }
                if (report.getkBlocValue() > 0) {
                    invoice.addItem(new InvoiceItemEntity(
                            "CRO : Frais de Bloc Opératoire (K " + report.getkBlocValue()
                                    + " - " + report.getProcedureName() + ")",
                            InvoiceItemType.K_BLOC,
                            kValue.multiply(BigDecimal.valueOf(report.getkBlocValue())),
                            BigDecimal.ONE,
                            null));
                }

                if (report.getImplants() != null) {
                    for (var implant : report.getImplants()) {
                        Double implantUnitPrice = implant.getUnitPrice();
                        BigDecimal implantPrice = implantUnitPrice != null && implantUnitPrice > 0
                                ? BigDecimal.valueOf(implantUnitPrice)
                                : conventionTariffService.getTariff(
                                        implant.getImplantName(),
                                        new BigDecimal("25000"));
                        String label = "Implant : " + implant.getImplantName()
                                + (implant.getLotNumber() != null && !implant.getLotNumber().isEmpty()
                                        ? " (Lot: " + implant.getLotNumber() + ")"
                                        : "");
                        invoice.addItem(new InvoiceItemEntity(
                                label,
                                InvoiceItemType.MEDICATION,
                                implantPrice,
                                BigDecimal.valueOf(implant.getQuantity()),
                                null));
                    }
                }
            }
        } catch (Exception ignored) {
            // La pré-facturation reste tolérante aux modules optionnels non configurés.
        }
    }

    private void addPrescriptionItems(InvoiceEntity invoice, UUID visitId) {
        try {
            Optional<ConsultationEntity> consultation = consultationRepository.findByVisitId(visitId);
            if (consultation.isEmpty()) {
                return;
            }
            Optional<PrescriptionEntity> prescription =
                    prescriptionRepository.findByConsultationId(consultation.get().getId());
            if (prescription.isEmpty()) {
                return;
            }
            for (PrescriptionItemEntity item : prescription.get().getItems()) {
                BigDecimal quantity = BigDecimal.ONE;
                try {
                    String cleanQuantity = item.getQuantity().replaceAll("[^\\d.]", "");
                    if (!cleanQuantity.isEmpty()) {
                        quantity = new BigDecimal(cleanQuantity);
                    }
                } catch (Exception ignored) {
                    // Quantité non numérique : conserver une unité.
                }

                BigDecimal drugPrice = conventionTariffService.getTariff(
                        item.getDrugName(),
                        new BigDecimal("2500"));
                invoice.addItem(new InvoiceItemEntity(
                        "Médicament : " + item.getDrugName() + " (" + item.getDosage() + ")",
                        InvoiceItemType.MEDICATION,
                        drugPrice,
                        quantity,
                        null));
            }
        } catch (Exception ignored) {
            // La pré-facturation reste tolérante aux modules optionnels non configurés.
        }
    }
}
