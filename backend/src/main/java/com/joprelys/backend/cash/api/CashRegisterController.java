package com.joprelys.backend.cash.api;

import com.joprelys.backend.cash.application.CashRegisterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cash-registers")
@Tag(name = "Caisse", description = "Gestion des caisses, des sessions et des reçus")
public class CashRegisterController {

    private final CashRegisterService cashRegisterService;

    public CashRegisterController(CashRegisterService cashRegisterService) {
        this.cashRegisterService = cashRegisterService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_CLINIQUE', 'AGENT_ACCUEIL', 'CAISSIER', 'DAF')")
    @Operation(summary = "Lister les caisses", description = "Retourne la liste des caisses physiques de la clinique")
    public ResponseEntity<List<CashRegisterResponse>> listRegisters() {
        return ResponseEntity.ok(cashRegisterService.listRegisters());
    }

    @PostMapping("/sessions/open")
    @PreAuthorize("hasAnyRole('ADMIN_CLINIQUE', 'AGENT_ACCUEIL', 'CAISSIER', 'DAF')")
    @Operation(summary = "Ouvrir une session de caisse", description = "Ouvre une session de caisse pour le caissier connecté")
    public ResponseEntity<CashSessionResponse> openSession(@Valid @RequestBody OpenSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cashRegisterService.openSession(request));
    }

    @GetMapping("/sessions/active")
    @PreAuthorize("hasAnyRole('ADMIN_CLINIQUE', 'AGENT_ACCUEIL', 'CAISSIER', 'DAF')")
    @Operation(summary = "Session de caisse active", description = "Retourne la session de caisse active du caissier connecté")
    public ResponseEntity<CashSessionResponse> getActiveSession() {
        CashSessionResponse active = cashRegisterService.getActiveSession();
        if (active == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(active);
    }

    @PostMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMIN_CLINIQUE', 'AGENT_ACCUEIL', 'CAISSIER', 'DAF')")
    @Operation(summary = "Enregistrer un mouvement de caisse", description = "Enregistre une entrée, une sortie ou un versement banque dans la session active")
    public ResponseEntity<CashMovementResponse> addMovement(@Valid @RequestBody CashMovementRequest request) {
        return ResponseEntity.ok(cashRegisterService.addMovement(request));
    }

    @GetMapping("/sessions/{sessionId}/movements")
    @PreAuthorize("hasAnyRole('ADMIN_CLINIQUE', 'AGENT_ACCUEIL', 'CAISSIER', 'DAF')")
    @Operation(summary = "Mouvements de caisse d'une session", description = "Retourne tous les mouvements d'une session de caisse spécifique")
    public ResponseEntity<List<CashMovementResponse>> getSessionMovements(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(cashRegisterService.getSessionMovements(sessionId));
    }

    @PostMapping("/sessions/close")
    @PreAuthorize("hasAnyRole('ADMIN_CLINIQUE', 'AGENT_ACCUEIL', 'CAISSIER', 'DAF')")
    @Operation(summary = "Clôturer la session de caisse", description = "Clôture la session de caisse active avec déclaration du solde physique")
    public ResponseEntity<CashSessionResponse> closeSession(@Valid @RequestBody CloseSessionRequest request) {
        return ResponseEntity.ok(cashRegisterService.closeSession(request));
    }

    @GetMapping("/payments/{paymentId}/receipt")
    @PreAuthorize("hasAnyRole('ADMIN_CLINIQUE', 'AGENT_ACCUEIL', 'CAISSIER', 'DAF')")
    @Operation(summary = "Obtenir le reçu de paiement", description = "Retourne les détails du reçu numéroté associé à un paiement")
    public ResponseEntity<PaymentReceiptResponse> getReceiptByPayment(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(cashRegisterService.getReceiptByPayment(paymentId));
    }

    @GetMapping("/{registerId}/sessions")
    @PreAuthorize("hasAnyRole('ADMIN_CLINIQUE', 'DAF')")
    @Operation(summary = "Lister l'historique des sessions d'une caisse", description = "Retourne l'historique de toutes les sessions d'une caisse (DAF/Admin)")
    public ResponseEntity<List<CashSessionResponse>> listSessionsByRegister(@PathVariable UUID registerId) {
        return ResponseEntity.ok(cashRegisterService.listSessionsByRegister(registerId));
    }
}
