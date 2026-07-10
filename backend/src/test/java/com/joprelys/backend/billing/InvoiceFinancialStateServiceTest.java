package com.joprelys.backend.billing;

import com.joprelys.backend.billing.api.InvoiceCollectionStatus;
import com.joprelys.backend.billing.application.InvoiceFinancialStateService;
import com.joprelys.backend.billing.infrastructure.persistence.InsuranceConventionEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceItemEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceItemType;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceRepository;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceStatus;
import com.joprelys.backend.billing.infrastructure.persistence.PaymentRepository;
import com.joprelys.backend.billing.infrastructure.persistence.ReceivableEntity;
import com.joprelys.backend.billing.infrastructure.persistence.ReceivableRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceFinancialStateServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private ReceivableRepository receivableRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private InvoiceFinancialStateService financialStateService;

    @Test
    void shouldSettlePatientOnlyInvoiceWhenPatientReceivableIsPaid() {
        InvoiceEntity invoice = createInvoice(null);
        ReceivableEntity patient = receivable(invoice, "PATIENT", invoice.getPatientId(), invoice.getPatientShare(), true);
        when(receivableRepository.findByInvoiceId(invoice.getId())).thenReturn(List.of(patient));

        InvoiceStatus status = financialStateService.synchronize(invoice);

        assertEquals(InvoiceStatus.SETTLED, status);
        assertEquals(InvoiceStatus.SETTLED, invoice.getStatus());
        assertEquals(InvoiceCollectionStatus.SETTLED, financialStateService.summarize(invoice).collectionStatus());
    }

    @Test
    void shouldKeepPaidWhileInsuranceIsOutstanding() {
        InsuranceConventionEntity convention = new InsuranceConventionEntity("Assurance", new BigDecimal("0.8000"));
        InvoiceEntity invoice = createInvoice(convention);
        ReceivableEntity patient = receivable(invoice, "PATIENT", invoice.getPatientId(), invoice.getPatientShare(), true);
        ReceivableEntity insurance = receivable(invoice, "INSURANCE", convention.getId(), invoice.getInsuranceShare(), false);
        when(receivableRepository.findByInvoiceId(invoice.getId())).thenReturn(List.of(patient, insurance));

        InvoiceStatus status = financialStateService.synchronize(invoice);

        assertEquals(InvoiceStatus.PAID, status);
        assertEquals(InvoiceCollectionStatus.INSURANCE_DUE, financialStateService.summarize(invoice).collectionStatus());
    }

    @Test
    void shouldSettleTiersPayantInvoiceWhenBothPartiesArePaid() {
        InsuranceConventionEntity convention = new InsuranceConventionEntity("Assurance", new BigDecimal("0.8000"));
        InvoiceEntity invoice = createInvoice(convention);
        ReceivableEntity patient = receivable(invoice, "PATIENT", invoice.getPatientId(), invoice.getPatientShare(), true);
        ReceivableEntity insurance = receivable(invoice, "INSURANCE", convention.getId(), invoice.getInsuranceShare(), true);
        when(receivableRepository.findByInvoiceId(invoice.getId())).thenReturn(List.of(patient, insurance));

        assertEquals(InvoiceStatus.SETTLED, financialStateService.synchronize(invoice));
    }

    @Test
    void shouldKeepValidatedWhenInsuranceIsPaidBeforePatient() {
        InsuranceConventionEntity convention = new InsuranceConventionEntity("Assurance", new BigDecimal("0.8000"));
        InvoiceEntity invoice = createInvoice(convention);
        ReceivableEntity patient = receivable(invoice, "PATIENT", invoice.getPatientId(), invoice.getPatientShare(), false);
        ReceivableEntity insurance = receivable(invoice, "INSURANCE", convention.getId(), invoice.getInsuranceShare(), true);
        when(receivableRepository.findByInvoiceId(invoice.getId())).thenReturn(List.of(patient, insurance));

        assertEquals(InvoiceStatus.VALIDATED, financialStateService.synchronize(invoice));
        assertEquals(InvoiceCollectionStatus.PATIENT_DUE, financialStateService.summarize(invoice).collectionStatus());
    }

    @Test
    void shouldExposeCancelledAsTerminalCollectionStatus() {
        InvoiceEntity invoice = createInvoice(null);
        invoice.setStatus(InvoiceStatus.CANCELLED);
        when(receivableRepository.findByInvoiceId(invoice.getId())).thenReturn(List.of());

        assertEquals(InvoiceCollectionStatus.CANCELLED, financialStateService.summarize(invoice).collectionStatus());
    }

    private InvoiceEntity createInvoice(InsuranceConventionEntity convention) {
        InvoiceEntity invoice = new InvoiceEntity(UUID.randomUUID(), UUID.randomUUID(), "FAC-TEST-STATE", convention);
        invoice.setOrganizationId(UUID.randomUUID());
        InvoiceItemEntity item = new InvoiceItemEntity(
                "Prestation",
                InvoiceItemType.CONSULTATION,
                new BigDecimal("100000.0000"),
                new BigDecimal("1.0000"),
                null);
        item.setOrganizationId(invoice.getOrganizationId());
        invoice.addItem(item);
        invoice.setStatus(InvoiceStatus.VALIDATED);
        return invoice;
    }

    private ReceivableEntity receivable(InvoiceEntity invoice,
                                         String debtorType,
                                         UUID debtorId,
                                         BigDecimal amount,
                                         boolean paid) {
        ReceivableEntity receivable = new ReceivableEntity(invoice.getId(), debtorType, debtorId, amount);
        receivable.setOrganizationId(invoice.getOrganizationId());
        if (paid) {
            receivable.setPaidAmount(amount);
        }
        return receivable;
    }
}
