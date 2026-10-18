package com.joprelys.backend.billing.api;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.billing.application.EstimateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST pour : devis/proformas, validation de factures, remises, avoirs et créances.
 * Respecte SOLID — séparé d'InvoiceController pour limiter la taille et les responsabilités.
 */
@RestController
@Tag(name = "Billing - Devis & Avoirs", description = "Devis, validation de factures, remises, avoirs et créances")
public class EstimateController {

    private final EstimateService estimateService;
    private final UserAccountRepository userAccountRepository;
    private final com.joprelys.backend.billing.application.CreditNoteService creditNoteService;

    public EstimateController(EstimateService estimateService, UserAccountRepository userAccountRepository,
                              com.joprelys.backend.billing.application.CreditNoteService creditNoteService) {
        this.creditNoteService = creditNoteService;
        this.estimateService = estimateService;
        this.userAccountRepository = userAccountRepository;
    }

    // ─── Devis / Proformas ─────────────────────────────────────────────────────

    @PostMapping("/api/estimates")
    @PreAuthorize("hasAuthority('BILLING_INVOICE_WRITE')")
    @Operation(summary = "Créer un devis", description = "Crée un devis/proforma pour un patient.")
    public ResponseEntity<EstimateResponse> createEstimate(@Valid @RequestBody CreateEstimateRequest request) {
        EstimateResponse response = estimateService.createEstimate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/estimates")
    @PreAuthorize("hasAuthority('BILLING_INVOICE_READ')")
    @Operation(summary = "Lister les devis d'un patient", description = "Récupère tous les devis d'un patient.")
    public ResponseEntity<List<EstimateResponse>> listEstimates(@RequestParam UUID patientId) {
        return ResponseEntity.ok(estimateService.getEstimatesByPatient(patientId));
    }

    @GetMapping("/api/estimates/{id}")
    @PreAuthorize("hasAuthority('BILLING_INVOICE_READ')")
    @Operation(summary = "Détails d'un devis", description = "Récupère les détails d'un devis par son identifiant.")
    public ResponseEntity<EstimateResponse> getEstimate(@PathVariable UUID id) {
        return ResponseEntity.ok(estimateService.getEstimate(id));
    }

    @PatchMapping("/api/estimates/{id}/status")
    @PreAuthorize("hasAuthority('BILLING_INVOICE_WRITE')")
    @Operation(summary = "Changer le statut d'un devis", description = "Met à jour le statut d'un devis (DRAFT, ACCEPTED, REJECTED, INVOICED).")
    public ResponseEntity<EstimateResponse> updateEstimateStatus(@PathVariable UUID id,
                                                                  @RequestParam String status) {
        return ResponseEntity.ok(estimateService.updateEstimateStatus(id, status));
    }

    // ─── Validation de facture ─────────────────────────────────────────────────

    @PostMapping("/api/invoices/{id}/validate")
    @PreAuthorize("hasAuthority('BILLING_INVOICE_WRITE')")
    @Operation(summary = "Valider une facture", description = "Valide une facture et la rend immuable. Génère les créances patient et assurance.")
    public ResponseEntity<InvoiceResponse> validateInvoice(@PathVariable UUID id, Authentication authentication) {
        String email = authentication.getName();
        UserAccountEntity user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        InvoiceResponse response = estimateService.validateInvoice(id, user.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/invoices/{id}/cancel")
    @PreAuthorize("hasAuthority('BILLING_INVOICE_CANCEL')")
    @Operation(summary = "Annuler une facture", description = "Annule une facture non encore validée.")
    public ResponseEntity<InvoiceResponse> cancelInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(estimateService.cancelInvoice(id));
    }

    @PostMapping("/api/invoices/{id}/discount")
    @PreAuthorize("hasAuthority('BILLING_INVOICE_WRITE')")
    @Operation(summary = "Appliquer une remise", description = "Applique une remise sur une facture en attente.")
    public ResponseEntity<InvoiceResponse> applyDiscount(@PathVariable UUID id,
                                                          @Valid @RequestBody ApplyDiscountRequest request) {
        return ResponseEntity.ok(estimateService.applyDiscount(id, request));
    }

    // ─── Avoirs ────────────────────────────────────────────────────────────────

    @PostMapping("/api/invoices/{id}/credit-notes")
    @PreAuthorize("hasAnyAuthority('BILLING_CREDIT_NOTE_ISSUE', 'BILLING_INVOICE_CANCEL')")
    @Operation(summary = "Émettre un avoir", description = "Émet un avoir sur une facture validée. Au-delà du seuil de l'établissement, l'avoir attend l'approbation de la DAF.")
    public ResponseEntity<CreditNoteResponse> createCreditNote(@PathVariable UUID id,
                                                                @Valid @RequestBody CreateCreditNoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(creditNoteService.issue(id, request));
    }

    @PostMapping("/api/credit-notes/{id}/approve")
    @PreAuthorize("hasAuthority('BILLING_CREDIT_NOTE_APPROVE')")
    @Operation(summary = "Approuver un avoir", description = "Approuve un avoir en attente ; l'émetteur ne peut pas approuver son propre avoir.")
    public ResponseEntity<CreditNoteResponse> approveCreditNote(@PathVariable UUID id) {
        return ResponseEntity.ok(creditNoteService.approve(id));
    }

    @PostMapping("/api/credit-notes/{id}/reject")
    @PreAuthorize("hasAuthority('BILLING_CREDIT_NOTE_APPROVE')")
    @Operation(summary = "Refuser un avoir", description = "Refuse un avoir en attente d'approbation.")
    public ResponseEntity<CreditNoteResponse> rejectCreditNote(@PathVariable UUID id) {
        return ResponseEntity.ok(creditNoteService.reject(id));
    }

    @PostMapping("/api/credit-notes/{id}/refund")
    @PreAuthorize("hasAuthority('CASH_PAYMENT_COLLECT')")
    @Operation(summary = "Rembourser un avoir", description = "Rembourse en caisse tout ou partie d'un avoir actif, dans la limite des sommes encaissées.")
    public ResponseEntity<CreditNoteResponse> refundCreditNote(@PathVariable UUID id,
                                                                @Valid @RequestBody RefundCreditNoteRequest request) {
        return ResponseEntity.ok(creditNoteService.refund(id, request));
    }

    @GetMapping("/api/invoices/{id}/credit-notes")
    @PreAuthorize("hasAuthority('BILLING_INVOICE_READ')")
    @Operation(summary = "Lister les avoirs d'une facture", description = "Récupère tous les avoirs associés à une facture.")
    public ResponseEntity<List<CreditNoteResponse>> listCreditNotes(@PathVariable UUID id) {
        return ResponseEntity.ok(creditNoteService.listByInvoice(id));
    }

    // ─── Créances ──────────────────────────────────────────────────────────────

    @GetMapping("/api/receivables")
    @PreAuthorize("hasAuthority('BILLING_INVOICE_READ')")
    @Operation(summary = "Créances par débiteur", description = "Récupère les créances d'un patient ou d'une assurance.")
    public ResponseEntity<List<ReceivableResponse>> getReceivablesByDebtor(@RequestParam UUID debtorId) {
        return ResponseEntity.ok(estimateService.getReceivablesByDebtor(debtorId));
    }

    @GetMapping("/api/invoices/{id}/receivables")
    @PreAuthorize("hasAuthority('BILLING_INVOICE_READ')")
    @Operation(summary = "Créances d'une facture", description = "Récupère les créances (patient + assurance) liées à une facture validée.")
    public ResponseEntity<List<ReceivableResponse>> getReceivablesByInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(estimateService.getReceivablesByInvoice(id));
    }

    @GetMapping("/api/receivables/by-status")
    @PreAuthorize("hasAuthority('BILLING_INVOICE_READ')")
    @Operation(summary = "Créances par statut", description = "Filtre les créances par statut : UNPAID, PARTIALLY_PAID, PAID.")
    public ResponseEntity<List<ReceivableResponse>> getReceivablesByStatus(@RequestParam String status) {
        return ResponseEntity.ok(estimateService.getReceivablesByStatus(status));
    }
}
