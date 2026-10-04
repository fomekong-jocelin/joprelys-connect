package com.joprelys.backend.prescription.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Prescriptions", description = "Gestion des ordonnances et prescriptions")
public class PrescriptionController {

    private final com.joprelys.backend.prescription.application.PrescriptionUseCase prescriptions;
    public PrescriptionController(com.joprelys.backend.prescription.application.PrescriptionUseCase prescriptions) {
        this.prescriptions = prescriptions;
    }

    @PostMapping("/consultations/{id}/prescription")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('PRESCRIPTION_WRITE')")
    @Operation(summary = "Enregistrer une prescription", description = "Crée ou met à jour l'ordonnance associée à une consultation.", responses = {
            @ApiResponse(responseCode = "200", description = "Prescription enregistrée avec succès"),
            @ApiResponse(responseCode = "403", description = "Consentement ou périmètre prescriptions insuffisant"),
            @ApiResponse(responseCode = "404", description = "Consultation réellement introuvable")
    })
    public PrescriptionResponse savePrescription(
            @Parameter(description = "Identifiant de la consultation") @PathVariable UUID id,
            @Valid @RequestBody SavePrescriptionRequest request) {
        return prescriptions.save(id, request);
    }

    @GetMapping("/consultations/{id}/prescription")
    @PreAuthorize("hasAnyAuthority('CLINICAL_READ', 'PHARMACY_PRESCRIPTION_READ')")
    @Operation(summary = "Récupérer une prescription", description = "Retourne l'ordonnance associée à une consultation. Une consultation valide sans ordonnance retourne 204.", responses = {
            @ApiResponse(responseCode = "200", description = "Prescription trouvée"),
            @ApiResponse(responseCode = "204", description = "Consultation valide, aucune prescription enregistrée"),
            @ApiResponse(responseCode = "403", description = "Consentement ou périmètre prescriptions insuffisant"),
            @ApiResponse(responseCode = "404", description = "Consultation réellement introuvable")
    })
    public ResponseEntity<PrescriptionResponse> getPrescription(
            @Parameter(description = "Identifiant de la consultation") @PathVariable UUID id) {
        return prescriptions.get(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/prescriptions/{id}/transmit")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('PRESCRIPTION_WRITE')")
    @Operation(summary = "Transmettre une prescription", description = "Transmet une ordonnance au service concerné.")
    public PrescriptionResponse transmitPrescription(
            @Parameter(description = "Identifiant de la prescription") @PathVariable UUID id,
            Authentication authentication) {
        return prescriptions.transmit(id, authentication);
    }

    @PostMapping("/prescriptions/{id}/finalize")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('PRESCRIPTION_SIGN')")
    @Operation(summary = "Finaliser une prescription", description = "Valide et active une prescription au statut DRAFT, générant le PDF.")
    public PrescriptionResponse finalizePrescription(
            @Parameter(description = "Identifiant de la prescription") @PathVariable UUID id,
            Authentication authentication) {
        return prescriptions.finalizePrescription(id, authentication);
    }

    @PatchMapping("/prescriptions/{id}/cancel")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('PRESCRIPTION_SIGN')")
    @Operation(summary = "Annuler une prescription", description = "Annule une ordonnance existante.")
    public PrescriptionResponse cancelPrescription(
            @Parameter(description = "Identifiant de la prescription") @PathVariable UUID id,
            Authentication authentication) {
        return prescriptions.cancel(id, authentication);
    }

}
