package com.joprelys.backend.billing.application;

import com.joprelys.backend.billing.api.CashierCollectionQueueItemResponse;
import com.joprelys.backend.billing.api.InvoiceCollectionStatus;
import com.joprelys.backend.billing.api.InvoiceSettlementSummaryResponse;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceRepository;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Builds the tenant-scoped list of invoices that still require a patient payment. */
@Service
public class CashierCollectionQueueService {

    private static final BigDecimal DUE_TOLERANCE = new BigDecimal("0.01");
    private static final List<InvoiceStatus> CANDIDATE_STATUSES = List.of(
            InvoiceStatus.VALIDATED,
            InvoiceStatus.PARTIALLY_PAID
    );

    private final InvoiceRepository invoiceRepository;
    private final InvoiceFinancialStateService financialStateService;
    private final PatientRepository patientRepository;

    public CashierCollectionQueueService(InvoiceRepository invoiceRepository,
                                         InvoiceFinancialStateService financialStateService,
                                         PatientRepository patientRepository) {
        this.invoiceRepository = invoiceRepository;
        this.financialStateService = financialStateService;
        this.patientRepository = patientRepository;
    }

    @Transactional(readOnly = true)
    public List<CashierCollectionQueueItemResponse> listQueue() {
        List<InvoiceEntity> candidates = invoiceRepository
                .findByStatusInOrderByValidatedAtAscCreatedAtAsc(CANDIDATE_STATUSES);

        Map<UUID, PatientEntity> patientsById = patientRepository.findAllById(
                        candidates.stream().map(InvoiceEntity::getPatientId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(PatientEntity::getId, Function.identity()));

        return candidates.stream()
                .map(invoice -> toQueueItem(invoice, patientsById.get(invoice.getPatientId())))
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    private java.util.Optional<CashierCollectionQueueItemResponse> toQueueItem(InvoiceEntity invoice,
                                                                               PatientEntity patient) {
        if (patient == null) {
            return java.util.Optional.empty();
        }

        InvoiceSettlementSummaryResponse summary = financialStateService.summarize(invoice);
        if (!isPatientCollectable(summary)) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(new CashierCollectionQueueItemResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                patient.getId(),
                patient.getFullName(),
                patient.getGlobalPatientNumber(),
                patient.getPhone(),
                invoice.getTotalAmount(),
                summary.patient().remainingAmount(),
                summary.collectionStatus(),
                invoice.getCreatedAt(),
                invoice.getValidatedAt()
        ));
    }

    private boolean isPatientCollectable(InvoiceSettlementSummaryResponse summary) {
        InvoiceCollectionStatus status = summary.collectionStatus();
        return (status == InvoiceCollectionStatus.PATIENT_DUE
                || status == InvoiceCollectionStatus.PATIENT_PARTIALLY_PAID)
                && summary.patient().remainingAmount().compareTo(DUE_TOLERANCE) > 0;
    }
}
