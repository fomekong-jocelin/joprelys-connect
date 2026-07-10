package com.joprelys.backend.billing.application;

import com.joprelys.backend.billing.api.InvoiceSettlementSummaryResponse;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Provides a debtor-level view of invoice settlement for the billing workspace. */
@Service
public class InvoiceSettlementQueryService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceFinancialStateService financialStateService;

    public InvoiceSettlementQueryService(InvoiceRepository invoiceRepository,
                                         InvoiceFinancialStateService financialStateService) {
        this.invoiceRepository = invoiceRepository;
        this.financialStateService = financialStateService;
    }

    @Transactional(readOnly = true)
    public List<InvoiceSettlementSummaryResponse> listByPatient(UUID patientId) {
        return invoiceRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(financialStateService::summarize)
                .toList();
    }
}
