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

    private final EstimateRepository estimateRepository;
    private final ReceivableRepository receivableRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceFinancialStateService financialStateService;
    private final LabPaymentReleaseService labPaymentReleaseService;

    public EstimateService(EstimateRepository estimateRepository,
                           ReceivableRepository receivableRepository,
                           InvoiceRepository invoiceRepository,
                           InvoiceFinancialStateService financialStateService,
                           LabPaymentReleaseService labPaymentReleaseService) {
        this.labPaymentReleaseService = labPaymentReleaseService;
        this.estimateRepository = estimateRepository;
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
        List<String> missingTariffs = invoice.getItems().stream()
            .filter(InvoiceItemEntity::isTariffMissing)
            .map(item -> item.getTariffKey() == null ? item.getLabel() : item.getTariffKey())
            .distinct()
            .toList();
        if (!missingTariffs.isEmpty()) {
            // ADR-20261017 D-11 : aucun prix inventé ; la facture attend le paramétrage puis un recalcul.
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Tarifs manquants : " + String.join(", ", missingTariffs)
                    + ". Paramétrez la grille tarifaire puis recalculez la facture.");
        }
        invoice.setStatus(InvoiceStatus.VALIDATED);
        invoice.setValidatedAt(Instant.now());
        invoice.setValidatedByUserId(validatorUserId);
        invoiceRepository.save(invoice);

        financialStateService.initializeReceivables(invoice);
        financialStateService.synchronize(invoice);
        labPaymentReleaseService.releaseIfPatientShareSettled(invoice);
        return InvoiceResponse.fromEntity(invoice);
    }

    @Transactional
    public InvoiceResponse cancelInvoice(UUID invoiceId) {
        InvoiceEntity invoice = findInvoiceOrThrow(invoiceId);
        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Seule une facture en attente peut être annulée directement. Créez un avoir pour une facture validée ou réglée.");
        }
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.save(invoice);
        return InvoiceResponse.fromEntity(invoice);
    }

    @Transactional
    public InvoiceResponse applyDiscount(UUID invoiceId, ApplyDiscountRequest request) {
        InvoiceEntity invoice = findInvoiceOrThrow(invoiceId);
        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Une remise ne peut être appliquée que sur une facture en attente. Statut actuel : " + invoice.getStatus().name() + ".");
        }
        invoice.setDiscountAmount(BigDecimal.valueOf(request.discountAmount()));
        invoice.setDiscountReason(request.discountReason());
        invoiceRepository.save(invoice);
        return InvoiceResponse.fromEntity(invoice);
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
}
