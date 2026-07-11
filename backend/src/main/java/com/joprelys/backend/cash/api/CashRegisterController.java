package com.joprelys.backend.cash.api;

import com.joprelys.backend.cash.application.CashRegisterService;
import com.joprelys.backend.cash.application.CashSessionCloseoutReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cash-registers")
@Tag(name = "Caisse", description = "Gestion des caisses, des sessions et des reçus")
public class CashRegisterController {

    private static final String LEGACY_CASH_ROLES =
            "hasAnyRole('ADMIN_CLINIQUE', 'AGENT_ACCUEIL', 'CAISSIER', 'DAF', 'ADMIN_JOPRELYS', 'SUPER_ADMIN')";
    private static final String LEGACY_SUPERVISION_ROLES =
            "hasAnyRole('ADMIN_CLINIQUE', 'DAF', 'ADMIN_JOPRELYS', 'SUPER_ADMIN')";

    private final CashRegisterService cashRegisterService;
    private final CashSessionCloseoutReportService closeoutReportService;

    public CashRegisterController(
            CashRegisterService cashRegisterService,
            CashSessionCloseoutReportService closeoutReportService) {
        this.cashRegisterService = cashRegisterService;
        this.closeoutReportService = closeoutReportService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('CASH_QUEUE_READ', 'CASH_SESSION_OPEN', 'CASH_HISTORY_READ') or " + LEGACY_CASH_ROLES)
    @Operation(summary = "Lister les caisses", description = "Retourne la liste des caisses physiques de la clinique")
    public ResponseEntity<List<CashRegisterResponse>> listRegisters() {
        return ResponseEntity.ok(cashRegisterService.listRegisters());
    }

    @PostMapping("/sessions/open")
    @PreAuthorize("hasAuthority('CASH_SESSION_OPEN') or " + LEGACY_CASH_ROLES)
    @Operation(summary = "Ouvrir une session de caisse", description = "Ouvre une session de caisse pour le caissier connecté")
    public ResponseEntity<CashSessionResponse> openSession(@Valid @RequestBody OpenSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cashRegisterService.openSession(request));
    }

    @GetMapping("/sessions/active")
    @PreAuthorize("hasAnyAuthority('CASH_SESSION_OPEN', 'CASH_SESSION_CLOSE', 'CASH_MOVEMENT_WRITE', 'CASH_HISTORY_READ') or " + LEGACY_CASH_ROLES)
    @Operation(summary = "Session de caisse active", description = "Retourne la session de caisse active du caissier connecté")
    public ResponseEntity<CashSessionResponse> getActiveSession() {
        CashSessionResponse active = cashRegisterService.getActiveSession();
        if (active == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(active);
    }

    @GetMapping("/sessions/active/summary")
    @PreAuthorize("hasAnyAuthority('CASH_SESSION_CLOSE', 'CASH_MOVEMENT_WRITE', 'CASH_HISTORY_READ') or " + LEGACY_CASH_ROLES)
    @Operation(summary = "Récapitulatif de session active", description = "Retourne le rapprochement des espèces, chèques et virements de la session du caissier connecté")
    public ResponseEntity<CashSessionSummaryResponse> getActiveSessionSummary() {
        return ResponseEntity.ok(cashRegisterService.getActiveSessionSummary());
    }

    @GetMapping("/sessions/mine")
    @PreAuthorize("hasAuthority('CASH_HISTORY_READ') or " + LEGACY_CASH_ROLES)
    @Operation(summary = "Historique personnel de caisse", description = "Retourne les vingt dernières sessions ouvertes par l'utilisateur connecté avec leur rapprochement financier")
    public ResponseEntity<List<CashSessionHistoryResponse>> listMySessions() {
        return ResponseEntity.ok(cashRegisterService.listMySessions());
    }

    @GetMapping(value = "/sessions/{sessionId}/closeout-report", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('CASH_HISTORY_READ') or " + LEGACY_CASH_ROLES)
    @Operation(summary = "Bordereau PDF de clôture", description = "Génère le bordereau récapitulatif d'une session clôturée accessible au caissier propriétaire ou à la supervision")
    public ResponseEntity<byte[]> downloadCloseoutReport(@PathVariable UUID sessionId) {
        CashSessionHistoryResponse history = cashRegisterService.getSessionHistory(sessionId);
        byte[] report = closeoutReportService.generate(sessionId);
        String filename = "BORDEREAU_CLOTURE_" + history.reportNumber() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(report);
    }

    @PostMapping("/movements")
    @PreAuthorize("hasAuthority('CASH_MOVEMENT_WRITE') or " + LEGACY_CASH_ROLES)
    @Operation(summary = "Enregistrer un mouvement de caisse", description = "Enregistre une entrée, une sortie ou un versement banque dans la session active")
    public ResponseEntity<CashMovementResponse> addMovement(@Valid @RequestBody CashMovementRequest request) {
        return ResponseEntity.ok(cashRegisterService.addMovement(request));
    }

    @GetMapping("/sessions/{sessionId}/movements")
    @PreAuthorize("hasAuthority('CASH_HISTORY_READ') or " + LEGACY_CASH_ROLES)
    @Operation(summary = "Mouvements de caisse d'une session", description = "Retourne tous les mouvements d'une session de caisse spécifique")
    public ResponseEntity<List<CashMovementResponse>> getSessionMovements(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(cashRegisterService.getSessionMovements(sessionId));
    }

    @PostMapping("/sessions/close")
    @PreAuthorize("hasAuthority('CASH_SESSION_CLOSE') or " + LEGACY_CASH_ROLES)
    @Operation(summary = "Clôturer la session de caisse", description = "Clôture la session de caisse active avec déclaration du solde physique")
    public ResponseEntity<CashSessionResponse> closeSession(@Valid @RequestBody CloseSessionRequest request) {
        return ResponseEntity.ok(cashRegisterService.closeSession(request));
    }

    @GetMapping("/payments/{paymentId}/receipt")
    @PreAuthorize("hasAnyAuthority('CASH_PAYMENT_COLLECT', 'CASH_HISTORY_READ') or " + LEGACY_CASH_ROLES)
    @Operation(summary = "Obtenir le reçu de paiement", description = "Retourne les détails du reçu numéroté associé à un paiement")
    public ResponseEntity<PaymentReceiptResponse> getReceiptByPayment(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(cashRegisterService.getReceiptByPayment(paymentId));
    }

    @GetMapping("/{registerId}/sessions")
    @PreAuthorize("hasAuthority('CASH_HISTORY_READ') or " + LEGACY_SUPERVISION_ROLES)
    @Operation(summary = "Lister l'historique des sessions d'une caisse", description = "Retourne l'historique de toutes les sessions d'une caisse (DAF/Admin)")
    public ResponseEntity<List<CashSessionResponse>> listSessionsByRegister(@PathVariable UUID registerId) {
        return ResponseEntity.ok(cashRegisterService.listSessionsByRegister(registerId));
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasAuthority('CASH_HISTORY_READ') or " + LEGACY_SUPERVISION_ROLES)
    @Operation(summary = "Lister toutes les sessions de caisse", description = "Retourne l'historique global de toutes les sessions de caisse de l'établissement (DAF/Admin)")
    public ResponseEntity<List<CashSessionResponse>> listAllSessions() {
        return ResponseEntity.ok(cashRegisterService.listAllSessions());
    }

    @PostMapping("/sessions/{sessionId}/resolve-discrepancy")
    @PreAuthorize("hasAuthority('CASH_DISCREPANCY_RESOLVE') or " + LEGACY_SUPERVISION_ROLES)
    @Operation(summary = "Résoudre un écart de caisse", description = "Permet au DAF d'enregistrer la résolution d'un écart sur une session clôturée")
    public ResponseEntity<CashSessionResponse> resolveDiscrepancy(
            @PathVariable UUID sessionId,
            @Valid @RequestBody ResolveDiscrepancyRequest request) {
        return ResponseEntity.ok(cashRegisterService.resolveDiscrepancy(sessionId, request));
    }
}
