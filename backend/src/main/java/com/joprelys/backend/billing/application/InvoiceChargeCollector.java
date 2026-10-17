package com.joprelys.backend.billing.application;

import com.joprelys.backend.billing.infrastructure.persistence.InvoiceEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceItemEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceItemType;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationDailyCareEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationDailyCareRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.OperatingReportEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.OperatingReportRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.PatientConsumptionEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.PatientConsumptionRepository;
import com.joprelys.backend.lab.infrastructure.persistence.ExamType;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderItemEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderRepository;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderStatus;
import com.joprelys.backend.prescription.infrastructure.persistence.DispensationItemEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.DispensationItemRepository;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceProfileRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Collecte les charges facturables d'une visite à partir de leurs sources cliniques (ADR-20261017 D-11 à D-15).
 *
 * <p>Chaque ligne porte sa source ; une source déjà facturée sur une facture non annulée est ignorée et les
 * jours de séjour déjà facturés sont déduits. Le prix vient de la saisie clinique (soin, consommable, implant)
 * ou de la grille de l'établissement ; à défaut la ligne est créée « à tarifer », sans prix inventé.</p>
 */
@Component
class InvoiceChargeCollector {

    static final String CONSULTATION = "CONSULTATION";
    static final String STAY = "STAY";
    static final String DAILY_CARE = "DAILY_CARE";
    static final String CONSUMPTION = "CONSUMPTION";
    static final String SURGEON_FEE = "OPERATING_REPORT_SURGEON";
    static final String ANESTHESIA_FEE = "OPERATING_REPORT_ANESTHESIA";
    static final String THEATRE_FEE = "OPERATING_REPORT_THEATRE";
    static final String IMPLANT = "IMPLANT";
    static final String LAB_ORDER_ITEM = "LAB_ORDER_ITEM";
    static final String DISPENSATION_ITEM = "DISPENSATION_ITEM";

    private final ConventionTariffService tariffs;
    private final ConsultationRepository consultationRepository;
    private final HospitalizationDailyCareRepository dailyCareRepository;
    private final PatientConsumptionRepository consumptionRepository;
    private final OperatingReportRepository operatingReportRepository;
    private final InpatientSpaceProfileRepository inpatientSpaceProfileRepository;
    private final LabOrderRepository labOrderRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final DispensationItemRepository dispensationItemRepository;

    InvoiceChargeCollector(
            ConventionTariffService tariffs,
            ConsultationRepository consultationRepository,
            HospitalizationDailyCareRepository dailyCareRepository,
            PatientConsumptionRepository consumptionRepository,
            OperatingReportRepository operatingReportRepository,
            InpatientSpaceProfileRepository inpatientSpaceProfileRepository,
            LabOrderRepository labOrderRepository,
            PrescriptionRepository prescriptionRepository,
            DispensationItemRepository dispensationItemRepository) {
        this.tariffs = tariffs;
        this.consultationRepository = consultationRepository;
        this.dailyCareRepository = dailyCareRepository;
        this.consumptionRepository = consumptionRepository;
        this.operatingReportRepository = operatingReportRepository;
        this.inpatientSpaceProfileRepository = inpatientSpaceProfileRepository;
        this.labOrderRepository = labOrderRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.dispensationItemRepository = dispensationItemRepository;
    }

    void collect(InvoiceEntity invoice, VisitEntity visit, HospitalizationEntity stay, BilledSources billed) {
        consultationRepository.findByVisitId(visit.getId()).ifPresent(consultation -> {
            line(invoice, billed, CONSULTATION, consultation.getId(), "Consultation - " + visit.getReason(),
                    InvoiceItemType.CONSULTATION, "CS", null, BigDecimal.ONE);
            addDispensedMedications(invoice, billed, consultation, visit.getOrganizationId());
        });
        if (stay != null) {
            addStay(invoice, billed, stay);
            addDailyCares(invoice, billed, stay.getId());
            addConsumptions(invoice, billed, stay.getId());
            addOperatingReports(invoice, billed, stay.getId());
        }
        addLabExams(invoice, billed, visit);
    }

    private void addStay(InvoiceEntity invoice, BilledSources billed, HospitalizationEntity stay) {
        LocalDate start = stay.getAdmittedAt().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate end = stay.getDischargedAt() != null
                ? stay.getDischargedAt().atZone(ZoneId.systemDefault()).toLocalDate()
                : LocalDate.now();
        long days = Math.max(1, ChronoUnit.DAYS.between(start, end));
        long remainingDays = days - billed.stayDays(stay.getId());
        if (remainingDays <= 0) {
            return;
        }
        String comfort = inpatientSpaceProfileRepository
                .findBySpaceIdAndOrganizationId(stay.getCurrentSpaceId(), stay.getOrganizationId())
                .map(profile -> profile.getComfortLevel())
                .orElse("STANDARD")
                .toUpperCase(Locale.ROOT);
        // Le séjour est une source cumulative : seuls les jours non encore facturés sont ajoutés.
        addPriced(invoice, STAY, stay.getId(),
                "Frais de séjour (" + comfort + ", espace " + stay.getSpaceName() + ", " + remainingDays + " nuit(s))",
                InvoiceItemType.STAY_FEE, "ROOM_" + comfort, null, BigDecimal.valueOf(remainingDays));
    }

    private void addDailyCares(InvoiceEntity invoice, BilledSources billed, UUID stayId) {
        for (HospitalizationDailyCareEntity care : dailyCareRepository.findByHospitalizationIdOrderByPerformedAtDesc(stayId)) {
            if (!care.isBillable()) {
                continue;
            }
            String label = "Soin : " + care.getCareType()
                    + (care.getDescription() != null && !care.getDescription().isEmpty() ? " (" + care.getDescription() + ")" : "");
            line(invoice, billed, DAILY_CARE, care.getId(), label, InvoiceItemType.AMI_CARE, care.getCareType(),
                    care.getPrice() != null ? BigDecimal.valueOf(care.getPrice()) : null, BigDecimal.ONE);
        }
    }

    private void addConsumptions(InvoiceEntity invoice, BilledSources billed, UUID stayId) {
        for (PatientConsumptionEntity consumption : consumptionRepository.findByHospitalizationIdOrderByConsumedAtDesc(stayId)) {
            BigDecimal price = consumption.getUnitPrice() > 0 ? BigDecimal.valueOf(consumption.getUnitPrice()) : null;
            line(invoice, billed, CONSUMPTION, consumption.getId(), "Consommation : " + consumption.getItemName(),
                    InvoiceItemType.MEDICATION, consumption.getItemName(), price, BigDecimal.valueOf(consumption.getQuantity()));
        }
    }

    private void addOperatingReports(InvoiceEntity invoice, BilledSources billed, UUID stayId) {
        for (OperatingReportEntity report : operatingReportRepository.findByHospitalizationIdOrderByOperationDateDesc(stayId)) {
            if (!report.isValidated()) {
                continue;
            }
            fee(invoice, billed, SURGEON_FEE, report, "Honoraires Chirurgien", InvoiceItemType.K_SURGEON, report.getkSurgeonValue());
            fee(invoice, billed, ANESTHESIA_FEE, report, "Honoraires Anesthésiste", InvoiceItemType.K_ANESTHESIST, report.getkAnesthesistValue());
            fee(invoice, billed, THEATRE_FEE, report, "Frais de Bloc Opératoire", InvoiceItemType.K_BLOC, report.getkBlocValue());
            if (report.getImplants() != null) {
                for (var implant : report.getImplants()) {
                    String label = "Implant : " + implant.getImplantName()
                            + (implant.getLotNumber() != null && !implant.getLotNumber().isEmpty() ? " (Lot: " + implant.getLotNumber() + ")" : "");
                    line(invoice, billed, IMPLANT, implant.getId(), label, InvoiceItemType.MEDICATION, implant.getImplantName(),
                            implant.getUnitPrice() > 0 ? BigDecimal.valueOf(implant.getUnitPrice()) : null,
                            BigDecimal.valueOf(implant.getQuantity()));
                }
            }
        }
    }

    private void fee(InvoiceEntity invoice, BilledSources billed, String sourceType, OperatingReportEntity report,
                     String label, InvoiceItemType type, double kValue) {
        if (kValue <= 0) {
            return;
        }
        // Valeur de la lettre-clé K multipliée par le coefficient du compte rendu opératoire.
        line(invoice, billed, sourceType, report.getId(),
                "CRO : " + label + " (K " + kValue + " - " + report.getProcedureName() + ")",
                type, "K", null, BigDecimal.valueOf(kValue));
    }

    private void addLabExams(InvoiceEntity invoice, BilledSources billed, VisitEntity visit) {
        for (LabOrderEntity order : labOrderRepository.findByVisitId(visit.getId())) {
            boolean internal = order.getTargetOrganizationId() == null
                    || Objects.equals(order.getTargetOrganizationId(), visit.getOrganizationId());
            if (!internal || order.getStatus() == LabOrderStatus.CANCELLED) {
                continue;
            }
            InvoiceItemType type = order.getExamType() == ExamType.IMAGERIE ? InvoiceItemType.IMAGING_EXAM : InvoiceItemType.LAB_EXAM;
            for (LabOrderItemEntity item : order.getItems()) {
                if (item.getStatus() == LabOrderStatus.CANCELLED) {
                    continue;
                }
                line(invoice, billed, LAB_ORDER_ITEM, item.getId(), "Examen : " + item.getExamName(), type,
                        examTariffKey(item.getExamName()), null, BigDecimal.ONE);
            }
        }
    }

    private void addDispensedMedications(InvoiceEntity invoice, BilledSources billed, ConsultationEntity consultation, UUID organizationId) {
        prescriptionRepository.findByConsultationId(consultation.getId()).ifPresent(prescription -> {
            for (DispensationItemEntity dispensed : dispensationItemRepository.findByPrescriptionItemPrescriptionId(prescription.getId())) {
                // Seule une délivrance par la pharmacie de l'établissement est facturée (ADR-20261017 D-12).
                if (!Objects.equals(dispensed.getDispensation().getDispensingOrganizationId(), organizationId)
                        || dispensed.getQuantityDispensed() == null || dispensed.getQuantityDispensed() <= 0) {
                    continue;
                }
                var prescribed = dispensed.getPrescriptionItem();
                line(invoice, billed, DISPENSATION_ITEM, dispensed.getId(),
                        "Médicament : " + prescribed.getDrugName() + " (" + prescribed.getDosage() + ")",
                        InvoiceItemType.MEDICATION, prescribed.getDrugName(), null,
                        BigDecimal.valueOf(dispensed.getQuantityDispensed()));
            }
        });
    }

    static String examTariffKey(String examName) {
        String key = "EXAM:" + examName.trim().toUpperCase(Locale.ROOT);
        return key.length() > 100 ? key.substring(0, 100) : key;
    }

    private void line(InvoiceEntity invoice, BilledSources billed, String sourceType, UUID sourceId, String label,
                      InvoiceItemType type, String tariffKey, BigDecimal knownPrice, BigDecimal quantity) {
        if (billed.contains(sourceType, sourceId)) {
            return;
        }
        addPriced(invoice, sourceType, sourceId, label, type, tariffKey, knownPrice, quantity);
    }

    private void addPriced(InvoiceEntity invoice, String sourceType, UUID sourceId, String label,
                           InvoiceItemType type, String tariffKey, BigDecimal knownPrice, BigDecimal quantity) {
        BigDecimal price = knownPrice != null ? knownPrice : tariffs.findTariff(tariffKey).orElse(null);
        boolean missing = price == null;
        String safeLabel = label.length() > 250 ? label.substring(0, 250) : label;
        String safeKey = tariffKey == null ? null : tariffKey.length() > 100 ? tariffKey.substring(0, 100) : tariffKey;
        invoice.addItem(new InvoiceItemEntity(safeLabel, type, missing ? BigDecimal.ZERO : price, quantity, null)
                .withSource(sourceType, sourceId, safeKey, missing));
    }
}
