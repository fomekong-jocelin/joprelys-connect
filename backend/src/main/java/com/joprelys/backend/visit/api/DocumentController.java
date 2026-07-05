package com.joprelys.backend.visit.api;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.visit.application.DocumentService;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Documents", description = "Documents médicaux vérifiables")
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
    @Operation(summary = "Télécharger un document", description = "Télécharge le document médical PDF associé à une visite.", responses = {
            @ApiResponse(responseCode = "200", description = "Document PDF retourné"),
            @ApiResponse(responseCode = "404", description = "Introuvable")
    })
    public ResponseEntity<byte[]> downloadDocument(@Parameter(description = "Identifiant de la visite") @PathVariable UUID visitId) {
        MedicalDocumentEntity doc = medicalDocumentRepository.findByVisitIdWithVisitAndPatient(visitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document introuvable pour cette visite."));

        byte[] pdfBytes = documentService.loadDocumentFile(doc);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getDocumentNumber() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/api/public/documents/{id}/verify")
    @Operation(summary = "Vérifier un document (anonyme)", description = "Vérifie publiquement la validité d'un document médical via son identifiant.", responses = {
            @ApiResponse(responseCode = "200", description = "Document vérifié"),
            @ApiResponse(responseCode = "404", description = "Introuvable")
    })
    public DocumentVerificationResponse verifyAnonymously(@Parameter(description = "Identifiant du document") @PathVariable UUID id) {
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
    @Operation(summary = "Révoquer un document", description = "Révoque un document médical valide. Le QR code affichera ensuite DOCUMENT RÉVOQUÉ.", responses = {
            @ApiResponse(responseCode = "200", description = "Document révoqué avec succès"),
            @ApiResponse(responseCode = "404", description = "Introuvable")
    })
    public DocumentStatusResponse revokeDocument(
            @Parameter(description = "Identifiant du document") @PathVariable UUID id,
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
    @Operation(summary = "Annuler un document", description = "Annule un document médical (VALID ou REVOQUE). Cas d'usage : document généré par erreur système ou doublon.", responses = {
            @ApiResponse(responseCode = "200", description = "Document annulé avec succès"),
            @ApiResponse(responseCode = "404", description = "Introuvable")
    })
    public DocumentStatusResponse cancelDocument(
            @Parameter(description = "Identifiant du document") @PathVariable UUID id,
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
