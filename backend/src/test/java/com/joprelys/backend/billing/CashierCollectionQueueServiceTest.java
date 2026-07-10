package com.joprelys.backend.billing;

import com.joprelys.backend.billing.api.InvoiceCollectionStatus;
import com.joprelys.backend.billing.api.InvoiceSettlementSummaryResponse;
import com.joprelys.backend.billing.api.SettlementPartyResponse;
import com.joprelys.backend.billing.application.CashierCollectionQueueService;
import com.joprelys.backend.billing.application.InvoiceFinancialStateService;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceItemEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceItemType;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceRepository;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashierCollectionQueueServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceFinancialStateService financialStateService;

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private CashierCollectionQueueService queueService;

    @Test
    void shouldExposeOnlyPatientCollectableInvoicesWithMinimalPatientIdentity() {
        PatientEntity patient = patient("DPU-QUEUE-001", "Patient Queue");
        InvoiceEntity due = invoice(patient.getId(), "FAC-QUEUE-DUE", InvoiceStatus.VALIDATED);
        InvoiceEntity insuranceOnly = invoice(patient.getId(), "FAC-QUEUE-INS", InvoiceStatus.PARTIALLY_PAID);

        when(invoiceRepository.findByStatusInOrderByValidatedAtAscCreatedAtAsc(anyList()))
                .thenReturn(List.of(due, insuranceOnly));
        when(patientRepository.findAllById(anyList())).thenReturn(List.of(patient));
        when(financialStateService.summarize(due)).thenReturn(summary(
                due,
                InvoiceCollectionStatus.PATIENT_DUE,
                "20000.0000",
                "UNPAID"
        ));
        when(financialStateService.summarize(insuranceOnly)).thenReturn(summary(
                insuranceOnly,
                InvoiceCollectionStatus.INSURANCE_DUE,
                "0.0000",
                "PAID"
        ));

        var result = queueService.listQueue();

        assertEquals(1, result.size());
        assertEquals(due.getId(), result.getFirst().invoiceId());
        assertEquals("Patient Queue", result.getFirst().patientName());
        assertEquals("DPU-QUEUE-001", result.getFirst().globalPatientNumber());
        assertEquals(new BigDecimal("20000.0000"), result.getFirst().patientRemainingAmount());
        assertEquals(InvoiceCollectionStatus.PATIENT_DUE, result.getFirst().collectionStatus());

        ArgumentCaptor<List<InvoiceStatus>> statuses = ArgumentCaptor.forClass(List.class);
        verify(invoiceRepository).findByStatusInOrderByValidatedAtAscCreatedAtAsc(statuses.capture());
        assertEquals(List.of(InvoiceStatus.VALIDATED, InvoiceStatus.PARTIALLY_PAID), statuses.getValue());
    }

    @Test
    void shouldIgnoreMissingPatientAndAmountsWithinFinancialTolerance() {
        PatientEntity patient = patient("DPU-QUEUE-002", "Patient Tolérance");
        InvoiceEntity tinyRemainder = invoice(patient.getId(), "FAC-QUEUE-TINY", InvoiceStatus.PARTIALLY_PAID);
        InvoiceEntity orphan = invoice(UUID.randomUUID(), "FAC-QUEUE-ORPHAN", InvoiceStatus.VALIDATED);

        when(invoiceRepository.findByStatusInOrderByValidatedAtAscCreatedAtAsc(anyList()))
                .thenReturn(List.of(tinyRemainder, orphan));
        when(patientRepository.findAllById(anyList())).thenReturn(List.of(patient));
        when(financialStateService.summarize(tinyRemainder)).thenReturn(summary(
                tinyRemainder,
                InvoiceCollectionStatus.PATIENT_PARTIALLY_PAID,
                "0.0100",
                "PARTIALLY_PAID"
        ));

        assertTrue(queueService.listQueue().isEmpty());
    }

    private PatientEntity patient(String dpu, String name) {
        return new PatientEntity(
                dpu,
                "LOCAL-" + dpu,
                name,
                "MASCULIN",
                LocalDate.of(1990, 1, 1),
                "670000000",
                "Douala",
                "Akwa",
                "Adresse",
                "Contact",
                "671000000",
                "Aucune",
                "Aucun"
        );
    }

    private InvoiceEntity invoice(UUID patientId, String number, InvoiceStatus status) {
        InvoiceEntity invoice = new InvoiceEntity(patientId, UUID.randomUUID(), number, null);
        invoice.setOrganizationId(UUID.randomUUID());
        InvoiceItemEntity item = new InvoiceItemEntity(
                "Prestation",
                InvoiceItemType.CONSULTATION,
                new BigDecimal("20000.0000"),
                new BigDecimal("1.0000"),
                BigDecimal.ONE
        );
        item.setOrganizationId(invoice.getOrganizationId());
        invoice.addItem(item);
        invoice.setStatus(status);
        return invoice;
    }

    private InvoiceSettlementSummaryResponse summary(InvoiceEntity invoice,
                                                       InvoiceCollectionStatus status,
                                                       String remaining,
                                                       String partyStatus) {
        BigDecimal remainingAmount = new BigDecimal(remaining);
        BigDecimal total = invoice.getPatientShare();
        return new InvoiceSettlementSummaryResponse(
                invoice.getId(),
                status,
                new SettlementPartyResponse(
                        total,
                        total.subtract(remainingAmount).max(BigDecimal.ZERO),
                        remainingAmount,
                        partyStatus
                ),
                null
        );
    }
}
