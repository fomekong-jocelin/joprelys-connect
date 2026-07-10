package com.joprelys.backend.billing.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.billing.api.*;
import com.joprelys.backend.billing.infrastructure.persistence.*;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionItemEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.RoomRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationDailyCareRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationDailyCareEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.PatientConsumptionRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.PatientConsumptionEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.OperatingReportRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.OperatingReportEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.SurgicalImplantEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InsuranceConventionRepository insuranceConventionRepository;
    private final TariffGridRepository tariffGridRepository;
    private final PatientRepository patientRepository;
    private final VisitRepository visitRepository;
    private final HospitalizationRepository hospitalizationRepository;
    private final ConsultationRepository consultationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final RoomRepository roomRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;
    private final HospitalizationDailyCareRepository dailyCareRepository;
    private final PatientConsumptionRepository patientConsumptionRepository;
    private final OperatingReportRepository operatingReportRepository;

    public BillingService(InvoiceRepository invoiceRepository,
                          InvoiceItemRepository invoiceItemRepository,
                          InsuranceConventionRepository insuranceConventionRepository,
                          TariffGridRepository tariffGridRepository,
                          PatientRepository patientRepository,
                          VisitRepository visitRepository,
                          HospitalizationRepository hospitalizationRepository,
                          ConsultationRepository consultationRepository,
                          PrescriptionRepository prescriptionRepository,
                          RoomRepository roomRepository,
                          UserAccountRepository userAccountRepository,
                          AuditService auditService,
                          HospitalizationDailyCareRepository dailyCareRepository,
                          PatientConsumptionRepository patientConsumptionRepository,
                          OperatingReportRepository operatingReportRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.insuranceConventionRepository = insuranceConventionRepository;
        this.tariffGridRepository = tariffGridRepository;
        this.patientRepository = patientRepository;
        this.visitRepository = visitRepository;
        this.hospitalizationRepository = hospitalizationRepository;
        this.consultationRepository = consultationRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.roomRepository = roomRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
        this.dailyCareRepository = dailyCareRepository;
        this.patientConsumptionRepository = patientConsumptionRepository;
        this.operatingReportRepository = operatingReportRepository;
    }

    // FIX-3: orElseThrow au lieu de orElse(null) pour éviter des données sans organisation
    private UserAccountEntity getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non connecté"));
    }

    private double getTariff(String keyLetter, double defaultValue) {
        return tariffGridRepository.findByKeyLetter(keyLetter)
                .map(TariffGridEntity::getUnitValue)
                .orElse(defaultValue);
    }

    @Transactional(readOnly = true)
    public List<InsuranceConventionDto> listConventions() {
        return insuranceConventionRepository.findAll().stream()
                .map(InsuranceConventionDto::fromEntity)
                .toList();
    }

    @Transactional
    public InsuranceConventionDto createConvention(String name, Double coveragePercentage) {
        UserAccountEntity actor = getCurrentUser();
        UUID orgId = actor.getOrganizationId();

        InsuranceConventionEntity entity = new InsuranceConventionEntity(name, coveragePercentage);
        entity.setOrganizationId(orgId);
        InsuranceConventionEntity saved = insuranceConventionRepository.save(entity);
        return InsuranceConventionDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<TariffGridEntity> listTariffs() {
        return tariffGridRepository.findAll();
    }

    @Transactional
    public TariffGridEntity createOrUpdateTariff(String keyLetter, Double unitValue) {
        UserAccountEntity actor = getCurrentUser();
        UUID orgId = actor.getOrganizationId();

        Optional<TariffGridEntity> existing = tariffGridRepository.findByKeyLetter(keyLetter);
        TariffGridEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setUnitValue(unitValue);
        } else {
            entity = new TariffGridEntity(keyLetter, unitValue);
            entity.setOrganizationId(orgId);
        }
        return tariffGridRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse precalculateInvoice(UUID patientId, UUID visitId, UUID insuranceConventionId) {
        patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable"));

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

        // 1. Consultation fee
        double consultationPrice = getTariff("CS", 15000.0);
        if (visit != null) {
            invoice.addItem(new InvoiceItemEntity(
                    "Consultation - " + visit.getReason(),
                    InvoiceItemType.CONSULTATION,
                    consultationPrice,
                    1.0,
                    null
            ));
        }

        // 2. Stay fees (Hospitalizations)
        if (visit != null) {
            Optional<HospitalizationEntity> hospOpt = hospitalizationRepository.findByVisitId(visitId);
            if (hospOpt.isPresent()) {
                HospitalizationEntity hosp = hospOpt.get();
                LocalDate start = hosp.getAdmittedAt().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate end = hosp.getDischargedAt() != null
                        ? hosp.getDischargedAt().atZone(ZoneId.systemDefault()).toLocalDate()
                        : LocalDate.now();
                long days = ChronoUnit.DAYS.between(start, end);
                if (days <= 0) days = 1;

                // FIX-1: requête ciblée au lieu de findAll() + stream().filter() en mémoire
                String comfort = "STANDARD";
                double stayPrice = getTariff("ROOM_STANDARD", 10000.0);
                if (hosp.getRoomNumber() != null) {
                    try {
                        Optional<String> comfortOpt = roomRepository
                                .findByRoomNumberIgnoreCase(hosp.getRoomNumber())
                                .map(r -> r.getComfortLevel());
                        if (comfortOpt.isPresent()) {
                            comfort = comfortOpt.get().toUpperCase();
                            stayPrice = getTariff("ROOM_" + comfort,
                                    comfort.equalsIgnoreCase("VIP") ? 25000.0 : 10000.0);
                        }
                    } catch (Exception e) {
                        // FIX-2: log warn au lieu d'avaler silencieusement l'exception
                        log.warn("[BillingService] Impossible de récupérer le niveau de confort pour la chambre {} : {}",
                                hosp.getRoomNumber(), e.getMessage());
                    }
                }

                invoice.addItem(new InvoiceItemEntity(
                        "Frais de séjour en chambre (" + comfort + ", N° " + hosp.getRoomNumber() + ", " + days + " nuits)",
                        InvoiceItemType.STAY_FEE,
                        stayPrice,
                        (double) days,
                        null
                ));

                // 2.1 Daily cares
                try {
                    List<HospitalizationDailyCareEntity> cares = dailyCareRepository.findByHospitalizationIdOrderByPerformedAtDesc(hosp.getId());
                    for (HospitalizationDailyCareEntity dc : cares) {
                        if (dc.isBillable()) {
                            double carePrice = dc.getPrice() != null ? dc.getPrice() : getTariff(dc.getCareType(), 5000.0);
                            invoice.addItem(new InvoiceItemEntity(
                                    "Soin : " + dc.getCareType() + (dc.getDescription() != null && !dc.getDescription().isEmpty() ? " (" + dc.getDescription() + ")" : ""),
                                    InvoiceItemType.AMI_CARE,
                                    carePrice,
                                    1.0,
                                    null
                            ));
                        }
                    }
                } catch (Exception e) {
                    log.warn("[BillingService] Erreur chargement soins journaliers pour hosp {} : {}", hosp.getId(), e.getMessage());
                }

                // 2.2 Patient consumptions
                try {
                    List<PatientConsumptionEntity> consumptions = patientConsumptionRepository.findByHospitalizationIdOrderByConsumedAtDesc(hosp.getId());
                    for (PatientConsumptionEntity pc : consumptions) {
                        double consPrice = pc.getUnitPrice() > 0 ? pc.getUnitPrice() : getTariff(pc.getItemName(), 1500.0);
                        invoice.addItem(new InvoiceItemEntity(
                                "Consommation : " + pc.getItemName(),
                                InvoiceItemType.MEDICATION,
                                consPrice,
                                (double) pc.getQuantity(),
                                null
                        ));
                    }
                } catch (Exception e) {
                    log.warn("[BillingService] Erreur chargement consommations patient pour hosp {} : {}", hosp.getId(), e.getMessage());
                }

                // 2.3 Operating reports & implants
                try {
                    List<OperatingReportEntity> reports = operatingReportRepository.findByHospitalizationIdOrderByOperationDateDesc(hosp.getId());
                    for (OperatingReportEntity op : reports) {
                        if (op.isValidated()) {
                            if (op.getkSurgeonValue() > 0) {
                                double surgeonPrice = op.getkSurgeonValue() * getTariff("K", 1000.0);
                                invoice.addItem(new InvoiceItemEntity(
                                        "CRO : Honoraires Chirurgien (K " + op.getkSurgeonValue() + " - " + op.getProcedureName() + ")",
                                        InvoiceItemType.K_SURGEON,
                                        surgeonPrice,
                                        1.0,
                                        null
                                ));
                            }
                            if (op.getkAnesthesistValue() > 0) {
                                double anesthetistPrice = op.getkAnesthesistValue() * getTariff("K", 1000.0);
                                invoice.addItem(new InvoiceItemEntity(
                                        "CRO : Honoraires Anesthésiste (K " + op.getkAnesthesistValue() + " - " + op.getProcedureName() + ")",
                                        InvoiceItemType.K_ANESTHESIST,
                                        anesthetistPrice,
                                        1.0,
                                        null
                                ));
                            }
                            if (op.getkBlocValue() > 0) {
                                double blocPrice = op.getkBlocValue() * getTariff("K", 1000.0);
                                invoice.addItem(new InvoiceItemEntity(
                                        "CRO : Frais de Bloc Opératoire (K " + op.getkBlocValue() + " - " + op.getProcedureName() + ")",
                                        InvoiceItemType.K_BLOC,
                                        blocPrice,
                                        1.0,
                                        null
                                ));
                            }
                            if (op.getImplants() != null) {
                                for (SurgicalImplantEntity implant : op.getImplants()) {
                                    double implantPrice = implant.getUnitPrice() > 0 ? implant.getUnitPrice() : getTariff(implant.getImplantName(), 25000.0);
                                    invoice.addItem(new InvoiceItemEntity(
                                            "Implant : " + implant.getImplantName() + (implant.getLotNumber() != null && !implant.getLotNumber().isEmpty() ? " (Lot: " + implant.getLotNumber() + ")" : ""),
                                            InvoiceItemType.MEDICATION,
                                            implantPrice,
                                            (double) implant.getQuantity(),
                                            null
                                    ));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("[BillingService] Erreur chargement CRO/implants pour hosp {} : {}", hosp.getId(), e.getMessage());
                }
            }
        }

        // 3. Medications (Prescriptions)
        if (visit != null) {
            try {
                Optional<ConsultationEntity> consultOpt = consultationRepository.findByVisitId(visitId);
                if (consultOpt.isPresent()) {
                    Optional<PrescriptionEntity> presOpt = prescriptionRepository.findByConsultationId(consultOpt.get().getId());
                    if (presOpt.isPresent()) {
                        for (PrescriptionItemEntity item : presOpt.get().getItems()) {
                            double qty = 1.0;
                            try {
                                String cleanQty = item.getQuantity().replaceAll("[^\\d.]", "");
                                if (!cleanQty.isEmpty()) {
                                    qty = Double.parseDouble(cleanQty);
                                }
                            } catch (Exception e) {
                                log.warn("[BillingService] Impossible de parser la quantité '{}' pour le médicament {} : {}",
                                        item.getQuantity(), item.getDrugName(), e.getMessage());
                            }

                            double drugPrice = getTariff(item.getDrugName(), 2500.0);
                            invoice.addItem(new InvoiceItemEntity(
                                    "Médicament : " + item.getDrugName() + " (" + item.getDosage() + ")",
                                    InvoiceItemType.MEDICATION,
                                    drugPrice,
                                    qty,
                                    null
                            ));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[BillingService] Erreur chargement prescriptions pour visite {} : {}", visitId, e.getMessage());
            }
        }

        return InvoiceResponse.fromEntity(invoice);
    }

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        UserAccountEntity actor = getCurrentUser();
        UUID orgId = actor.getOrganizationId();

        patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable"));

        if (request.visitId() != null) {
            visitRepository.findById(request.visitId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable"));

            invoiceRepository.findByVisitId(request.visitId()).ifPresent(inv -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Une facture existe déjà pour cette visite.");
            });
        }

        InsuranceConventionEntity convention = null;
        if (request.insuranceConventionId() != null) {
            convention = insuranceConventionRepository.findById(request.insuranceConventionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Convention d'assurance introuvable"));
        }

        Long seqVal = invoiceRepository.getNextInvoiceNumberSequenceValue();
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String invoiceNumber = String.format("FAC-%s-%06d", dateStr, seqVal);

        InvoiceEntity invoice = new InvoiceEntity(request.patientId(), request.visitId(), invoiceNumber, convention);
        invoice.setOrganizationId(orgId);

        if (request.items() != null && !request.items().isEmpty()) {
            for (InvoiceItemDto itemDto : request.items()) {
                InvoiceItemEntity item = new InvoiceItemEntity(
                        itemDto.label(),
                        itemDto.itemType(),
                        itemDto.unitPrice(),
                        itemDto.quantity(),
                        itemDto.coefficient()
                );
                item.setOrganizationId(orgId);
                invoice.addItem(item);
            }
        } else {
            InvoiceResponse precalc = precalculateInvoice(request.patientId(), request.visitId(), request.insuranceConventionId());
            for (InvoiceItemResponse itemResp : precalc.items()) {
                InvoiceItemEntity item = new InvoiceItemEntity(
                        itemResp.label(),
                        itemResp.itemType(),
                        itemResp.unitPrice(),
                        itemResp.quantity(),
                        itemResp.coefficient()
                );
                item.setOrganizationId(orgId);
                invoice.addItem(item);
            }
        }

        InvoiceEntity saved = invoiceRepository.save(invoice);

        auditService.logSuccess(
                actor.getId(),
                orgId,
                request.patientId(),
                "BILLING",
                saved.getId(),
                "CREATE_INVOICE",
                "Création de la facture N° " + saved.getInvoiceNumber() + " pour un montant total de " + saved.getTotalAmount() + " FCFA."
        );

        return InvoiceResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listInvoices(UUID patientId) {
        return invoiceRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(InvoiceResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID id) {
        InvoiceEntity entity = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable"));
        return InvoiceResponse.fromEntity(entity);
    }

}
