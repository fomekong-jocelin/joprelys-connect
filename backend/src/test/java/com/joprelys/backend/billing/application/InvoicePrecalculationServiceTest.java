package com.joprelys.backend.billing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joprelys.backend.billing.infrastructure.persistence.InsuranceConventionRepository;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceItemEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceItemType;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceRepository;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceStatus;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationDailyCareRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.OperatingReportRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.PatientConsumptionRepository;
import com.joprelys.backend.lab.infrastructure.persistence.ExamType;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderItemEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderRepository;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderStatus;
import com.joprelys.backend.prescription.infrastructure.persistence.DispensationItemEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.DispensationItemRepository;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionDispensationEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionItemEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceProfileRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;

class InvoicePrecalculationServiceTest {

    private final ConventionTariffService tariffs = mock(ConventionTariffService.class);
    private final VisitRepository visits = mock(VisitRepository.class);
    private final HospitalizationRepository stays = mock(HospitalizationRepository.class);
    private final HospitalizationDailyCareRepository cares = mock(HospitalizationDailyCareRepository.class);
    private final PatientConsumptionRepository consumptions = mock(PatientConsumptionRepository.class);
    private final OperatingReportRepository reports = mock(OperatingReportRepository.class);
    private final ConsultationRepository consultations = mock(ConsultationRepository.class);
    private final PrescriptionRepository prescriptions = mock(PrescriptionRepository.class);
    private final InpatientSpaceProfileRepository profiles = mock(InpatientSpaceProfileRepository.class);
    private final LabOrderRepository labOrders = mock(LabOrderRepository.class);
    private final DispensationItemRepository dispensations = mock(DispensationItemRepository.class);
    private final InvoiceRepository invoices = mock(InvoiceRepository.class);

    private final UUID organizationId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID visitId = UUID.randomUUID();
    private InvoicePrecalculationService service;
    private VisitEntity visit;

    @BeforeEach
    void setUp() {
        var collector = new InvoiceChargeCollector(tariffs, consultations, cares, consumptions, reports, profiles,
                labOrders, prescriptions, dispensations);
        service = new InvoicePrecalculationService(mock(InsuranceConventionRepository.class), visits, stays, invoices, collector);
        visit = mock(VisitEntity.class);
        when(visit.getId()).thenReturn(visitId);
        when(visit.getOrganizationId()).thenReturn(organizationId);
        when(visit.getReason()).thenReturn("Fièvre");
        when(visits.findById(visitId)).thenReturn(Optional.of(visit));
        when(stays.findByVisitId(visitId)).thenReturn(Optional.empty());
        when(invoices.findAllByVisitId(visitId)).thenReturn(List.of());
        when(tariffs.findTariff(anyString())).thenReturn(Optional.empty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"care", "consumption", "report", "prescription"})
    void aFailedClinicalSourceCannotProduceAPartialInvoice(String source) {
        UUID stayId = UUID.randomUUID();
        var stay = mock(HospitalizationEntity.class);
        when(stays.findByVisitId(visitId)).thenReturn(Optional.of(stay));
        when(stay.getId()).thenReturn(stayId);
        when(stay.getAdmittedAt()).thenReturn(Instant.now());
        var failure = new DataAccessResourceFailureException("Clinical source unavailable");
        switch (source) {
            case "care" -> when(cares.findByHospitalizationIdOrderByPerformedAtDesc(stayId)).thenThrow(failure);
            case "consumption" -> when(consumptions.findByHospitalizationIdOrderByConsumedAtDesc(stayId)).thenThrow(failure);
            case "report" -> when(reports.findByHospitalizationIdOrderByOperationDateDesc(stayId)).thenThrow(failure);
            case "prescription" -> {
                var consultation = consultation();
                when(prescriptions.findByConsultationId(consultation.getId())).thenThrow(failure);
            }
            default -> throw new IllegalArgumentException(source);
        }
        assertThrows(DataAccessResourceFailureException.class, () -> service.precalculate(patientId, visitId, null));
    }

    @Test
    void aMissingTariffNeverInventsAPrice() {
        consultation();

        InvoiceEntity draft = service.draft(patientId, visitId, null, null);

        assertThat(draft.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getItemType()).isEqualTo(InvoiceItemType.CONSULTATION);
            assertThat(item.isTariffMissing()).isTrue();
            assertThat(item.getTariffKey()).isEqualTo("CS");
            assertThat(item.getUnitPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        });
    }

    @Test
    void internalExamsAndInternalDispensationsAreBilledOnTheirRealQuantities() {
        when(tariffs.findTariff("EXAM:NFS")).thenReturn(Optional.of(new BigDecimal("6000")));
        when(tariffs.findTariff("Artemether-Lumefantrine")).thenReturn(Optional.of(new BigDecimal("4500")));
        var consultation = consultation();
        LabOrderEntity order = labOrder(organizationId, "NFS");
        LabOrderEntity external = labOrder(UUID.randomUUID(), "Sérologie");
        when(labOrders.findByVisitId(visitId)).thenReturn(List.of(order, external));
        PrescriptionEntity prescription = mock(PrescriptionEntity.class);
        UUID prescriptionId = UUID.randomUUID();
        when(prescription.getId()).thenReturn(prescriptionId);
        when(prescriptions.findByConsultationId(consultation.getId())).thenReturn(Optional.of(prescription));
        var internal = dispensed(organizationId, 1);
        var externalPharmacy = dispensed(UUID.randomUUID(), 3);
        when(dispensations.findByPrescriptionItemPrescriptionId(prescriptionId)).thenReturn(List.of(internal, externalPharmacy));

        InvoiceEntity draft = service.draft(patientId, visitId, null, null);

        assertThat(draft.getItems()).extracting(InvoiceItemEntity::getItemType)
                .containsExactlyInAnyOrder(InvoiceItemType.CONSULTATION, InvoiceItemType.MEDICATION, InvoiceItemType.LAB_EXAM);
        assertThat(draft.getItems()).filteredOn(item -> item.getItemType() == InvoiceItemType.MEDICATION)
                .singleElement().satisfies(item -> {
                    assertThat(item.getQuantity()).isEqualByComparingTo(BigDecimal.ONE);
                    assertThat(item.getTotalItemAmount()).isEqualByComparingTo("4500");
                });
    }

    @Test
    void chargesAlreadyBilledAreNotBilledTwiceAndStayDaysAreDeducted() {
        var consultation = consultation();
        UUID stayId = UUID.randomUUID();
        var stay = mock(HospitalizationEntity.class);
        when(stay.getId()).thenReturn(stayId);
        when(stay.getAdmittedAt()).thenReturn(Instant.now().minus(Duration.ofDays(10)));
        when(stay.getSpaceName()).thenReturn("Chambre 101");
        when(stays.findByVisitId(visitId)).thenReturn(Optional.of(stay));
        InvoiceEntity interim = new InvoiceEntity(patientId, visitId, "FAC-1", null);
        interim.setStatus(InvoiceStatus.VALIDATED);
        interim.addItem(new InvoiceItemEntity("Consultation", InvoiceItemType.CONSULTATION, BigDecimal.TEN, BigDecimal.ONE, null)
                .withSource(InvoiceChargeCollector.CONSULTATION, consultation.getId(), "CS", false));
        interim.addItem(new InvoiceItemEntity("Séjour", InvoiceItemType.STAY_FEE, BigDecimal.TEN, BigDecimal.valueOf(7), null)
                .withSource(InvoiceChargeCollector.STAY, stayId, "ROOM_STANDARD", false));
        when(invoices.findAllByVisitId(visitId)).thenReturn(List.of(interim));

        InvoiceEntity draft = service.draft(patientId, visitId, null, null);

        assertThat(draft.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getItemType()).isEqualTo(InvoiceItemType.STAY_FEE);
            assertThat(item.getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(3));
        });
    }

    private ConsultationEntity consultation() {
        var consultation = mock(ConsultationEntity.class);
        when(consultation.getId()).thenReturn(UUID.randomUUID());
        when(consultations.findByVisitId(visitId)).thenReturn(Optional.of(consultation));
        return consultation;
    }

    private LabOrderEntity labOrder(UUID target, String examName) {
        LabOrderEntity order = mock(LabOrderEntity.class);
        LabOrderItemEntity item = mock(LabOrderItemEntity.class);
        when(item.getId()).thenReturn(UUID.randomUUID());
        when(item.getExamName()).thenReturn(examName);
        when(item.getStatus()).thenReturn(LabOrderStatus.REQUESTED);
        when(order.getTargetOrganizationId()).thenReturn(target);
        when(order.getStatus()).thenReturn(LabOrderStatus.REQUESTED);
        when(order.getExamType()).thenReturn(ExamType.LABORATOIRE);
        when(order.getItems()).thenReturn(List.of(item));
        return order;
    }

    private DispensationItemEntity dispensed(UUID dispensingOrganizationId, int quantity) {
        DispensationItemEntity item = mock(DispensationItemEntity.class);
        PrescriptionDispensationEntity dispensation = mock(PrescriptionDispensationEntity.class);
        PrescriptionItemEntity prescribed = mock(PrescriptionItemEntity.class);
        when(item.getId()).thenReturn(UUID.randomUUID());
        when(item.getDispensation()).thenReturn(dispensation);
        when(item.getPrescriptionItem()).thenReturn(prescribed);
        when(item.getQuantityDispensed()).thenReturn(quantity);
        when(dispensation.getDispensingOrganizationId()).thenReturn(dispensingOrganizationId);
        when(prescribed.getDrugName()).thenReturn("Artemether-Lumefantrine");
        when(prescribed.getDosage()).thenReturn("20/120 mg");
        return item;
    }
}
