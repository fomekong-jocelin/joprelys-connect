package com.joprelys.backend.prescription.api;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.patient.application.PatientAccessPolicyService;
import com.joprelys.backend.patient.application.PatientService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.prescription.application.PrescriptionService;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@Tag(name = "Prescriptions", description = "Gestion des ordonnances et prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final PatientAccessPolicyService accessPolicy;
    private final PatientRepository patientRepository;
    private final ConsultationRepository consultationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final UserAccountRepository userAccountRepository;

    public PrescriptionController(
            PrescriptionService prescriptionService,
            PatientAccessPolicyService accessPolicy,
            PatientRepository patientRepository,
            ConsultationRepository consultationRepository,
            PrescriptionRepository prescriptionRepository,
            UserAccountRepository userAccountRepository) {
        this.prescriptionService = prescriptionService;
        this.accessPolicy = accessPolicy;
        this.patientRepository = patientRepository;
        this.consultationRepository = consultationRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @PostMapping("/consultations/{id}/prescription")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    @Operation(summary = "Enregistrer une prescription", description = "Crée ou met à jour l'ordonnance associée à une consultation.", responses = {
            @ApiResponse(responseCode = "200", description = "Prescription enregistrée avec succès"),
            @ApiResponse(responseCode = "403", description = "Consentement ou périmètre prescriptions insuffisant"),
            @ApiResponse(responseCode = "404", description = "Consultation réellement introuvable")
    })
    public PrescriptionResponse savePrescription(
            @Parameter(description = "Identifiant de la consultation") @PathVariable UUID id,
            @Valid @RequestBody SavePrescriptionRequest request) {
        UUID patientId = resolveConsultationPatientId(id);
        accessPolicy.validateAccess(patientId, "prescriptions");
        var patient = resolvePatient(patientId);
        UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
        try {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
            return PrescriptionResponse.fromEntity(prescriptionService.savePrescription(id, request));
        } finally {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
        }
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
        UUID patientId = resolveConsultationPatientId(id);
        accessPolicy.validateAccess(patientId, "prescriptions");
        var patient = resolvePatient(patientId);
        UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
        try {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
            return prescriptionService.getPrescription(id)
                    .map(PrescriptionResponse::fromEntity)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.noContent().build());
        } finally {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
        }
    }

    @PostMapping("/prescriptions/{id}/transmit")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    @Operation(summary = "Transmettre une prescription", description = "Transmet une ordonnance au service concerné.")
    public PrescriptionResponse transmitPrescription(
            @Parameter(description = "Identifiant de la prescription") @PathVariable UUID id,
            Authentication authentication) {
        UUID patientId = resolvePrescriptionPatientId(id);
        accessPolicy.validateAccess(patientId, "prescriptions");
        var patient = resolvePatient(patientId);
        UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
        try {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
            return PrescriptionResponse.fromEntity(
                    prescriptionService.transmitPrescriptionForStaff(id, authentication.getName()));
        } finally {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
        }
    }

    @PostMapping("/prescriptions/{id}/finalize")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    @Operation(summary = "Finaliser une prescription", description = "Valide et active une prescription au statut DRAFT, générant le PDF.")
    public PrescriptionResponse finalizePrescription(
            @Parameter(description = "Identifiant de la prescription") @PathVariable UUID id,
            Authentication authentication) {
        UUID patientId = resolvePrescriptionPatientId(id);
        accessPolicy.validateAccess(patientId, "prescriptions");
        var patient = resolvePatient(patientId);
        UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
        try {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
            return PrescriptionResponse.fromEntity(
                    prescriptionService.finalizePrescription(id, resolveActorId(authentication)));
        } finally {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
        }
    }

    @PatchMapping("/prescriptions/{id}/cancel")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    @Operation(summary = "Annuler une prescription", description = "Annule une ordonnance existante.")
    public PrescriptionResponse cancelPrescription(
            @Parameter(description = "Identifiant de la prescription") @PathVariable UUID id,
            Authentication authentication) {
        UUID patientId = resolvePrescriptionPatientId(id);
        accessPolicy.validateAccess(patientId, "prescriptions");
        var patient = resolvePatient(patientId);
        UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
        try {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
            return PrescriptionResponse.fromEntity(
                    prescriptionService.cancelPrescription(id, resolveActorId(authentication)));
        } finally {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
        }
    }

    private UUID resolveConsultationPatientId(UUID consultationId) {
        return PatientService.convertToUuid(
                consultationRepository.findPatientIdByConsultationId(consultationId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation introuvable.")));
    }

    private UUID resolvePrescriptionPatientId(UUID prescriptionId) {
        return PatientService.convertToUuid(
                prescriptionRepository.findPatientIdByPrescriptionId(prescriptionId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordonnance introuvable.")));
    }

    private com.joprelys.backend.patient.infrastructure.persistence.PatientEntity resolvePatient(UUID patientId) {
        return patientRepository.findByIdGlobally(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient non trouvé."));
    }

    private UUID resolveActorId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        var user = userAccountRepository.findByEmail(authentication.getName().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable."));
        return user.getId();
    }
}
