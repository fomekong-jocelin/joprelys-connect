package com.joprelys.backend.billing.application;

import com.joprelys.backend.billing.api.InvoiceSettlementSummaryResponse;
import com.joprelys.backend.billing.api.SettlementPartyResponse;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceRepository;
import com.joprelys.backend.billing.infrastructure.persistence.ReceivableEntity;
import com.joprelys.backend.billing.infrastructure.persistence.ReceivableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Provides a debtor-level view of invoice settlement for the billing workspace. */
@Service
public class InvoiceSettlementQueryService {

    private static final BigDecimal SETTLED_TOLERANCE = new BigDecimal("0.01");

    private final InvoiceRepository invoiceRepository;
    private final ReceivableRepository receivableRepository;

    public InvoiceSettlementQueryService(InvoiceRepository invoiceRepository,
                                         ReceivableRepository receivableRepository) {
        this.invoiceRepository = invoiceRepository;
        this.receivableRepository = receivableRepository;
    }

    @Transactional(readOnly = true)
    public List<InvoiceSettlementSummaryResponse> listByPatient(UUID patientId) {
        return invoiceRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(this::summarize)
                .toList();
    }

    private InvoiceSettlementSummaryResponse summarize(InvoiceEntity invoice) {
        List<ReceivableEntity> receivables = receivableRepository.findByInvoiceId(invoice.getId());
        SettlementPartyResponse patient = partySummary(invoice.getPatientShare(), receivables, "PATIENT");
        SettlementPartyResponse insurance = invoice.getInsuranceShare().compareTo(SETTLED_TOLERANCE) > 0
                ? partySummary(invoice.getInsuranceShare(), receivables, "INSURANCE")
                : null;
        return new InvoiceSettlementSummaryResponse(
                invoice.getId(),
                collectionStatus(patient, insurance, receivables.isEmpty()),
                patient,
                insurance
        );
    }

    private SettlementPartyResponse partySummary(BigDecimal invoiceShare,
                                                  List<ReceivableEntity> receivables,
                                                  String debtorType) {
        return receivables.stream()
                .filter(receivable -> debtorType.equalsIgnoreCase(receivable.getDebtorType()))
                .findFirst()
                .map(receivable -> new SettlementPartyResponse(
                        receivable.getTotalAmount(),
                        receivable.getPaidAmount(),
                        receivable.getTotalAmount().subtract(receivable.getPaidAmount()).max(BigDecimal.ZERO),
                        receivable.getStatus()))
                .orElseGet(() -> new SettlementPartyResponse(invoiceShare, BigDecimal.ZERO, invoiceShare, "NOT_DUE"));
    }

    private String collectionStatus(SettlementPartyResponse patient,
                                    SettlementPartyResponse insurance,
                                    boolean noReceivableExists) {
        if (noReceivableExists) {
            return "NOT_YET_DUE";
        }
        if (patient.remainingAmount().compareTo(SETTLED_TOLERANCE) > 0) {
            return patient.paidAmount().compareTo(SETTLED_TOLERANCE) > 0 ? "PATIENT_PARTIALLY_PAID" : "PATIENT_DUE";
        }
        if (insurance != null && insurance.remainingAmount().compareTo(SETTLED_TOLERANCE) > 0) {
            return "INSURANCE_DUE";
        }
        return "SETTLED";
    }
}
