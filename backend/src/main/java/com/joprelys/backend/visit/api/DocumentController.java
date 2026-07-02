package com.joprelys.backend.visit.api;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.visit.application.DocumentService;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@RestController
public class DocumentController {

    private final DocumentService documentService;
    private final MedicalDocumentRepository medicalDocumentRepository;
    private final UserAccountRepository userAccountRepository;

    public DocumentController(
            DocumentService documentService,
            MedicalDocumentRepository medicalDocumentRepository,
            UserAccountRepository userAccountRepository) {
        this.documentService = documentService;
        this.medicalDocumentRepository = medicalDocumentRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping("/api/visits/{visitId}/document")
    @PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'PHARMACIEN')")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable UUID visitId) {
        MedicalDocumentEntity doc = medicalDocumentRepository.findByVisitId(visitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document introuvable pour cette visite."));

        byte[] pdfBytes = documentService.loadDocumentFile(doc);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getDocumentNumber() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/api/public/documents/{id}/verify")
    public DocumentVerificationResponse verifyAnonymously(@PathVariable UUID id) {
        return documentService.verifyDocument(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document introuvable."));
    }

    // -------------------------------------------------------------------------
    // STORY-0603 — Révocation et annulation de documents médicaux
    // -------------------------------------------------------------------------

    /**
     * Révoque un document médical valide.
     * Le QR code affichera ensuite DOCUMENT RÉVOQUÉ sur la page publique.
     * Rôles autorisés : MEDECIN, ADMIN_CLINIQUE
     */
    @PatchMapping("/api/documents/{id}/revoke")
    @PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE')")
    public DocumentStatusResponse revokeDocument(
            @PathVariable UUID id,
            @Valid @RequestBody RevokeDocumentRequest request,
            Authentication authentication) {

        UUID actorId = resolveActorId(authentication);
        return documentService.revokeDocument(id, request.reason(), actorId);
    }

    /**
     * Annule un document médical (VALID ou REVOQUE).
     * Cas d'usage : document généré par erreur système ou doublon.
     * Rôles autorisés : MEDECIN, ADMIN_CLINIQUE
     */
    @PatchMapping("/api/documents/{id}/cancel")
    @PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE')")
    public DocumentStatusResponse cancelDocument(
            @PathVariable UUID id,
            @Valid @RequestBody RevokeDocumentRequest request,
            Authentication authentication) {

        UUID actorId = resolveActorId(authentication);
        return documentService.cancelDocument(id, request.reason(), actorId);
    }

    // Extrait l'UUID de l'utilisateur connecté à partir du JWT (via UserAccountRepository)
    private UUID resolveActorId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        return userAccountRepository.findByEmail(authentication.getName())
                .map(u -> u.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Utilisateur introuvable en base."));
    }
}
