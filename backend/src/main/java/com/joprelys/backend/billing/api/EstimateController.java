package com.joprelys.backend.billing.api;

import com.joprelys.backend.billing.application.EstimateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    public EstimateController(EstimateService estimateService) {
        this.estimateService = estimateService;
    }

    // ─── Devis / Proformas ─────────────────────────────────────────────────────

    @PostMapping("/api/estimates")
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER')")
    @Operation(summary = "Créer un devis", description = "Crée un devis/proforma pour un patient.")
    public ResponseEntity<EstimateResponse> createEstimate(@Valid @RequestBody CreateEstimateRequest request) {
        EstimateResponse response = estimateService.createEstimate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/estimates")
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER')")
    @Operation(summary = "Lister les devis d'un patient", description = "Récupère tous les devis d'un patient.")
    public ResponseEntity<List<EstimateResponse>> listEstimates(@RequestParam UUID patientId) {
        return ResponseEntity.ok(estimateService.getEstimatesByPatient(patientId));
    }

    @GetMapping("/api/estimates/{id}")
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER')")
    @Operation(summary = "Détails d'un devis", description = "Récupère les détails d'un devis par son identifiant.")
    public ResponseEntity<EstimateResponse> getEstimate(@PathVariable UUID id) {
        return ResponseEntity.ok(estimateService.getEstimate(id));
    }

    @PatchMapping("/api/estimates/{id}/status")
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER')")
    @Operation(summary = "Changer le statut d'un devis", description = "Met à jour le statut d'un devis (DRAFT, ACCEPTED, REJECTED, INVOICED).")
    public ResponseEntity<EstimateResponse> updateEstimateStatus(@PathVariable UUID id,
                                                                  @RequestParam String status) {
        return ResponseEntity.ok(estimateService.updateEstimateStatus(id, status));
    }

    // ─── Validation de facture ─────────────────────────────────────────────────

    @PostMapping("/api/invoices/{id}/validate")
    @PreAuthorize("hasAnyRole('ADMIN_CLINIQUE', 'MEDECIN')")
    @Operation(summary = "Valider une facture", description = "Valide une facture et la rend immuable. Génère les créances patient et assurance.")
    public ResponseEntity<InvoiceResponse> validateInvoice(@PathVariable UUID id, Authentication authentication) {
        UUID validatorId = UUID.fromString(authentication.getName());
        InvoiceResponse response = estimateService.validateInvoice(id, validatorId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/invoices/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN_CLINIQUE', 'MEDECIN')")
    @Operation(summary = "Annuler une facture", description = "Annule une facture non encore validée.")
    public ResponseEntity<InvoiceResponse> cancelInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(estimateService.cancelInvoice(id));
    }

    @PostMapping("/api/invoices/{id}/discount")
    @PreAuthorize("hasAnyRole('ADMIN_CLINIQUE', 'MEDECIN', 'AGENT_ACCUEIL')")
    @Operation(summary = "Appliquer une remise", description = "Applique une remise sur une facture en attente.")
    public ResponseEntity<InvoiceResponse> applyDiscount(@PathVariable UUID id,
                                                          @Valid @RequestBody ApplyDiscountRequest request) {
        return ResponseEntity.ok(estimateService.applyDiscount(id, request));
    }

    // ─── Avoirs ────────────────────────────────────────────────────────────────

    @PostMapping("/api/invoices/{id}/credit-notes")
    @PreAuthorize("hasAnyRole('ADMIN_CLINIQUE', 'MEDECIN')")
    @Operation(summary = "Créer un avoir", description = "Crée un avoir (annulation partielle ou totale) sur une facture.")
    public ResponseEntity<CreditNoteResponse> createCreditNote(@PathVariable UUID id,
                                                                @Valid @RequestBody CreateCreditNoteRequest request) {
        CreditNoteResponse response = estimateService.createCreditNote(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/invoices/{id}/credit-notes")
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER')")
    @Operation(summary = "Lister les avoirs d'une facture", description = "Récupère tous les avoirs associés à une facture.")
    public ResponseEntity<List<CreditNoteResponse>> listCreditNotes(@PathVariable UUID id) {
        return ResponseEntity.ok(estimateService.getCreditNotesByInvoice(id));
    }

    // ─── Créances ──────────────────────────────────────────────────────────────

    @GetMapping("/api/receivables")
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER')")
    @Operation(summary = "Créances par débiteur", description = "Récupère les créances d'un patient ou d'une assurance.")
    public ResponseEntity<List<ReceivableResponse>> getReceivablesByDebtor(@RequestParam UUID debtorId) {
        return ResponseEntity.ok(estimateService.getReceivablesByDebtor(debtorId));
    }

    @GetMapping("/api/invoices/{id}/receivables")
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER')")
    @Operation(summary = "Créances d'une facture", description = "Récupère les créances (patient + assurance) liées à une facture validée.")
    public ResponseEntity<List<ReceivableResponse>> getReceivablesByInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(estimateService.getReceivablesByInvoice(id));
    }

    @GetMapping("/api/receivables/by-status")
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER')")
    @Operation(summary = "Créances par statut", description = "Filtre les créances par statut : UNPAID, PARTIALLY_PAID, PAID.")
    public ResponseEntity<List<ReceivableResponse>> getReceivablesByStatus(@RequestParam String status) {
        return ResponseEntity.ok(estimateService.getReceivablesByStatus(status));
    }
}
