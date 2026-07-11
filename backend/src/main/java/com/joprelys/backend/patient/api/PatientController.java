package com.joprelys.backend.patient.api;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.patient.application.PatientService;
import com.joprelys.backend.patient.application.PatientSummaryService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/patients")
@Tag(name = "Patients", description = "Gestion du dossier patient unique (DPU)")
public class PatientController {

    private static final String LEGACY_PATIENT_ROLES =
            "hasAnyRole('AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE')";
    private static final String LEGACY_CLINICAL_ROLES =
            "hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE')";
    private static final String LEGACY_ADMIN_ROLES =
            "hasRole('ADMIN_CLINIQUE')";

    private final PatientService patientService;
    private final UserAccountRepository userAccountRepository;
    private final PatientSummaryService patientSummaryService;

    public PatientController(
            PatientService patientService,
            UserAccountRepository userAccountRepository,
            PatientSummaryService patientSummaryService) {
        this.patientService = patientService;
        this.userAccountRepository = userAccountRepository;
        this.patientSummaryService = patientSummaryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PATIENT_WRITE') or " + LEGACY_PATIENT_ROLES)
    @Operation(summary = "Créer un patient", description = "Enregistre un nouveau patient et génère son numéro DPU.", responses = {
            @ApiResponse(responseCode = "201", description = "Patient créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "409", description = "Patient en doublon détecté")
    })
    public PatientResponse create(@Valid @RequestBody CreatePatientRequest request) {
        PatientEntity entity = patientService.createPatient(request);
        return mapToResponse(entity);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PATIENT_READ') or " + LEGACY_PATIENT_ROLES)
    @Operation(summary = "Lister les patients", description = "Retourne la liste des patients avec recherche optionnelle par nom, téléphone, DPU ou numéro URG-TEMP.", responses = {
            @ApiResponse(responseCode = "200", description = "Liste retournée avec succès")
    })
    public List<PatientResponse> list(
            @RequestParam(value = "q", required = false)
            @Parameter(description = "Terme de recherche") String query) {
        return patientService.searchPatients(query).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @GetMapping("/duplicates")
    @PreAuthorize("hasAuthority('PATIENT_MERGE') or " + LEGACY_ADMIN_ROLES)
    public List<PatientDuplicateCandidateResponse> getDuplicates() {
        return patientService.getDuplicateCandidates().stream()
                .map(candidate -> new PatientDuplicateCandidateResponse(
                        candidate.getId(),
                        mapToResponse(candidate.getSourcePatient()),
                        mapToResponse(candidate.getTargetPatient()),
                        candidate.getSimilarityScore(),
                        candidate.getStatus(),
                        candidate.getCreatedAt()))
                .toList();
    }

    @PostMapping("/duplicates/{id}/ignore")
    @PreAuthorize("hasAuthority('PATIENT_MERGE') or " + LEGACY_ADMIN_ROLES)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ignoreDuplicate(@PathVariable UUID id) {
        patientService.ignoreDuplicateCandidate(id);
    }

    @PostMapping("/merge")
    @PreAuthorize("hasAuthority('PATIENT_MERGE') or " + LEGACY_ADMIN_ROLES)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void merge(@Valid @RequestBody MergePatientsRequest request) {
        String actorEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        var actor = userAccountRepository.findByEmail(actorEmail.trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non trouvé"));
        patientService.mergePatients(request.primaryId(), request.secondaryId(), actor.getId());
    }

    @PostMapping("/{id}/emergency-access")
    @PreAuthorize("hasAuthority('PATIENT_EMERGENCY_ACCESS') or " + LEGACY_CLINICAL_ROLES)
    @ResponseStatus(HttpStatus.CREATED)
    public void triggerEmergencyAccess(
            @PathVariable UUID id,
            @RequestBody EmergencyAccessRequest request) {
        patientService.triggerEmergencyAccess(id, request.reason());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PATIENT_READ') or " + LEGACY_PATIENT_ROLES)
    public PatientResponse getById(@PathVariable UUID id) {
        PatientEntity entity = patientService.getPatientById(id);
        return mapToResponse(entity);
    }

    @GetMapping("/{id}/summary-pdf")
    @PreAuthorize("hasAuthority('CLINICAL_READ') or " + LEGACY_CLINICAL_ROLES)
    public org.springframework.http.ResponseEntity<byte[]> downloadSummaryPdf(@PathVariable UUID id) {
        byte[] pdfBytes = patientSummaryService.generatePatientSummaryPdf(id);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"patient-summary-" + id + ".pdf\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/{id}/medical-summary")
    @PreAuthorize("hasAuthority('CLINICAL_READ') or " + LEGACY_CLINICAL_ROLES)
    @Operation(summary = "Obtenir la synthèse médicale d'un patient", description = "Retourne la synthèse médicale structurée d'un patient.")
    public MedicalSummaryResponse getMedicalSummary(@PathVariable UUID id) {
        return patientSummaryService.getMedicalSummary(id);
    }

    private PatientResponse mapToResponse(PatientEntity entity) {
        boolean emergencyActive = patientService.isEmergencyAccessActiveForCurrentActor(entity.getId());
        return mapToResponse(entity, emergencyActive);
    }

    private PatientResponse mapToResponse(PatientEntity entity, boolean emergencyActive) {
        return new PatientResponse(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getGlobalPatientNumber(),
                entity.getLocalPatientNumber(),
                entity.getFullName(),
                entity.getGender(),
                entity.getBirthDate(),
                entity.getPhone(),
                entity.getCity(),
                entity.getDistrict(),
                entity.getAddress(),
                entity.getEmergencyContactName(),
                entity.getEmergencyContactPhone(),
                entity.getAllergies(),
                entity.getMedicalHistory(),
                entity.getStatus(),
                entity.getBloodGroup(),
                entity.getEmail(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                emergencyActive,
                entity.getIdentityStatus(),
                entity.getTemporaryPatientNumber(),
                entity.getDisplayName());
    }
}

record EmergencyAccessRequest(String reason) {
}
