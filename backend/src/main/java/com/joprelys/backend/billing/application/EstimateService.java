package com.joprelys.backend.billing.application;

import com.joprelys.backend.billing.api.*;
import com.joprelys.backend.billing.infrastructure.persistence.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Service métier pour les devis/proformas, avoirs, créances et validation de factures.
 * Respecte les règles SOLID — délègue les actions spécialisées hors BillingService.
 */
@Service
public class EstimateService {

    private static final String ESTIMATE_PREFIX = "DEV-";
    private static final String CREDIT_NOTE_PREFIX = "AV-";

    private final EstimateRepository estimateRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final ReceivableRepository receivableRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceFinancialStateService financialStateService;

    public EstimateService(EstimateRepository estimateRepository,
                           CreditNoteRepository creditNoteRepository,
                           ReceivableRepository receivableRepository,
                           InvoiceRepository invoiceRepository,
                           InvoiceFinancialStateService financialStateService) {
        this.estimateRepository = estimateRepository;
        this.creditNoteRepository = creditNoteRepository;
        this.receivableRepository = receivableRepository;
        this.invoiceRepository = invoiceRepository;
        this.financialStateService = financialStateService;
    }

    // ─── Devis / Proforma ──────────────────────────────────────────────────────

    @Transactional
    public EstimateResponse createEstimate(CreateEstimateRequest request) {
        Long seqVal = estimateRepository.getNextEstimateNumberSequenceValue();
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String number = String.format("%s%s-%06d", ESTIMATE_PREFIX, dateStr, seqVal);
        EstimateEntity estimate = new EstimateEntity(request.patientId(), request.visitId(), number);

        if (request.items() != null) {
            for (EstimateItemRequest itemReq : request.items()) {
                EstimateItemEntity item = new EstimateItemEntity(
                    itemReq.label(), itemReq.itemType(), itemReq.unitPrice(), itemReq.quantity()
                );
                estimate.addItem(item);
            }
        }

        estimateRepository.save(estimate);
        return EstimateResponse.fromEntity(estimate);
    }

    @Transactional(readOnly = true)
    public List<EstimateResponse> getEstimatesByPatient(UUID patientId) {
        return estimateRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
            .stream().map(EstimateResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public EstimateResponse getEstimate(UUID estimateId) {
        return EstimateResponse.fromEntity(findEstimateOrThrow(estimateId));
    }

    @Transactional
    public EstimateResponse updateEstimateStatus(UUID estimateId, String newStatus) {
        EstimateEntity estimate = findEstimateOrThrow(estimateId);
        estimate.setStatus(newStatus);
        estimateRepository.save(estimate);
        return EstimateResponse.fromEntity(estimate);
    }

    // ─── Validation de facture (immuabilité) ───────────────────────────────────

    @Transactional
    public InvoiceResponse validateInvoice(UUID invoiceId, UUID validatorUserId) {
        InvoiceEntity invoice = findInvoiceOrThrow(invoiceId);
        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Seule une facture en attente peut être validée. Statut actuel : " + invoice.getStatus().name() + ".");
        }
        invoice.setStatus(InvoiceStatus.VALIDATED);
        invoice.setValidatedAt(Instant.now());
        invoice.setValidatedByUserId(validatorUserId);
        invoiceRepository.save(invoice);

        financialStateService.initializeReceivables(invoice);
        financialStateService.synchronize(invoice);
        return InvoiceResponse.fromEntity(invoice);
    }

    @Transactional
    public InvoiceResponse cancelInvoice(UUID invoiceId) {
        InvoiceEntity invoice = findInvoiceOrThrow(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.VALIDATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Une facture validée ne peut pas être annulée directement. Créez un avoir.");
        }
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.save(invoice);
        return InvoiceResponse.fromEntity(invoice);
    }

    @Transactional
    public InvoiceResponse applyDiscount(UUID invoiceId, ApplyDiscountRequest request) {
        InvoiceEntity invoice = findInvoiceOrThrow(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.VALIDATED || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Impossible d'appliquer une remise sur une facture " + invoice.getStatus().name().toLowerCase() + ".");
        }
        invoice.setDiscountAmount(BigDecimal.valueOf(request.discountAmount()));
        invoice.setDiscountReason(request.discountReason());
        invoiceRepository.save(invoice);
        return InvoiceResponse.fromEntity(invoice);
    }

    // ─── Avoirs (Credit Notes) ─────────────────────────────────────────────────

    @Transactional
    public CreditNoteResponse createCreditNote(UUID invoiceId, CreateCreditNoteRequest request) {
        InvoiceEntity invoice = findInvoiceOrThrow(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de créer un avoir sur une facture annulée.");
        }
        Long seqVal = creditNoteRepository.getNextCreditNoteNumberSequenceValue();
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String number = String.format("%s%s-%06d", CREDIT_NOTE_PREFIX, dateStr, seqVal);
        BigDecimal creditAmount = BigDecimal.valueOf(request.amount());
        CreditNoteEntity creditNote = new CreditNoteEntity(invoiceId, number, request.amount(), request.reason());
        creditNoteRepository.save(creditNote);

        adjustReceivablesForCreditNote(invoice, creditAmount);
        financialStateService.synchronize(invoice);
        return CreditNoteResponse.fromEntity(creditNote);
    }

    @Transactional(readOnly = true)
    public List<CreditNoteResponse> getCreditNotesByInvoice(UUID invoiceId) {
        return creditNoteRepository.findByInvoiceId(invoiceId)
            .stream().map(CreditNoteResponse::fromEntity).toList();
    }

    // ─── Créances ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ReceivableResponse> getReceivablesByDebtor(UUID debtorId) {
        return receivableRepository.findByDebtorIdOrderByCreatedAtDesc(debtorId)
            .stream().map(ReceivableResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<ReceivableResponse> getReceivablesByInvoice(UUID invoiceId) {
        return receivableRepository.findByInvoiceId(invoiceId)
            .stream().map(ReceivableResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<ReceivableResponse> getReceivablesByStatus(String status) {
        if ("ALL".equalsIgnoreCase(status)) {
            return receivableRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(ReceivableResponse::fromEntity).toList();
        }
        return receivableRepository.findByStatusOrderByCreatedAtDesc(status)
            .stream().map(ReceivableResponse::fromEntity).toList();
    }

    // ─── Helpers privés ────────────────────────────────────────────────────────

    private EstimateEntity findEstimateOrThrow(UUID id) {
        return estimateRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Devis introuvable: " + id));
    }

    private InvoiceEntity findInvoiceOrThrow(UUID id) {
        return invoiceRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable: " + id));
    }

    private void adjustReceivablesForCreditNote(InvoiceEntity invoice, BigDecimal creditAmount) {
        List<ReceivableEntity> receivables = receivableRepository.findByInvoiceId(invoice.getId());
        if (receivables.isEmpty()) return;

        BigDecimal remaining = creditAmount;
        for (ReceivableEntity receivable : receivables) {
            if (remaining.signum() <= 0) break;
            BigDecimal reducible = receivable.getTotalAmount()
                    .subtract(receivable.getPaidAmount())
                    .max(BigDecimal.ZERO);
            BigDecimal reduction = remaining.min(reducible);
            receivable.setPaidAmount(receivable.getPaidAmount().add(reduction));
            remaining = remaining.subtract(reduction);
            receivableRepository.save(receivable);
        }
    }
}
