package com.joprelys.backend.patient.api;

import com.joprelys.backend.patient.application.PatientService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.context.SecurityContextHolder;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/patients")
@PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE')")
@Tag(name = "Patients", description = "Gestion du dossier patient unique (DPU)")
public class PatientController {

	private final PatientService patientService;
	private final UserAccountRepository userAccountRepository;
	private final com.joprelys.backend.patient.application.PatientSummaryService patientSummaryService;

	public PatientController(PatientService patientService, UserAccountRepository userAccountRepository, com.joprelys.backend.patient.application.PatientSummaryService patientSummaryService) {
		this.patientService = patientService;
		this.userAccountRepository = userAccountRepository;
		this.patientSummaryService = patientSummaryService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
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
	@Operation(summary = "Lister les patients", description = "Retourne la liste des patients avec recherche optionnelle par nom, téléphone ou DPU.", responses = {
		@ApiResponse(responseCode = "200", description = "Liste retournée avec succès")
	})
	public List<PatientResponse> list(@RequestParam(value = "q", required = false) @Parameter(description = "Terme de recherche") String query) {
		return patientService.searchPatients(query).stream()
				.map(this::mapToResponse)
				.toList();
	}

	// WT3 (DUPLICATES): Liste des candidats doublons
	@GetMapping("/duplicates")
	@PreAuthorize("hasRole('ADMIN_CLINIQUE')")
	public List<PatientDuplicateCandidateResponse> getDuplicates() {
		return patientService.getDuplicateCandidates().stream()
				.map(c -> new PatientDuplicateCandidateResponse(
						c.getId(),
						mapToResponse(c.getSourcePatient()),
						mapToResponse(c.getTargetPatient()),
						c.getSimilarityScore(),
						c.getStatus(),
						c.getCreatedAt()
				))
				.toList();
	}

	// WT3 (DUPLICATES): Ignorer un doublon candidat
	@PostMapping("/duplicates/{id}/ignore")
	@PreAuthorize("hasRole('ADMIN_CLINIQUE')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void ignoreDuplicate(@PathVariable UUID id) {
		patientService.ignoreDuplicateCandidate(id);
	}

	// WT3 (DUPLICATES): Fusionner deux dossiers patients
	@PostMapping("/merge")
	@PreAuthorize("hasRole('ADMIN_CLINIQUE')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void merge(@Valid @RequestBody MergePatientsRequest request) {
		var actorEmail = SecurityContextHolder.getContext().getAuthentication().getName();
		var actor = userAccountRepository.findByEmail(actorEmail.trim().toLowerCase())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non trouvé"));
		patientService.mergePatients(request.primaryId(), request.secondaryId(), actor.getId());
	}

	@PostMapping("/{id}/emergency-access")
	@ResponseStatus(HttpStatus.CREATED)
	public void triggerEmergencyAccess(
			@PathVariable UUID id,
			@RequestBody EmergencyAccessRequest request) {
		patientService.triggerEmergencyAccess(id, request.reason());
	}

	@GetMapping("/{id}")
	public PatientResponse getById(@PathVariable UUID id) {
		PatientEntity entity = patientService.getPatientById(id);
		return mapToResponse(entity);
	}

	// WT2 (PDF): Téléchargement du PDF de synthèse médicale patient
	@GetMapping("/{id}/summary-pdf")
	@PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE')")
	public org.springframework.http.ResponseEntity<byte[]> downloadSummaryPdf(@PathVariable UUID id) {
		byte[] pdfBytes = patientSummaryService.generatePatientSummaryPdf(id);
		return org.springframework.http.ResponseEntity.ok()
				.header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"patient-summary-" + id + ".pdf\"")
				.contentType(org.springframework.http.MediaType.APPLICATION_PDF)
				.body(pdfBytes);
	}

	@GetMapping("/{id}/medical-summary")
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
				emergencyActive
		);
	}
}

record EmergencyAccessRequest(String reason) {}
