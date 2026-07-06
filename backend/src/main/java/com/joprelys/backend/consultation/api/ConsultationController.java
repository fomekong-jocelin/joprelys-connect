package com.joprelys.backend.consultation.api;

import com.joprelys.backend.consultation.application.ConsultationService;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentRepository;
import com.joprelys.backend.patient.application.PatientService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@RestController
@RequestMapping("/api/visits")
@Tag(name = "Consultations", description = "Saisie et gestion des consultations médicales")
public class ConsultationController {

	private final ConsultationService consultationService;
	private final MedicalDocumentRepository medicalDocumentRepository;
	private final PatientService patientService;
	private final PatientRepository patientRepository;
	private final VisitRepository visitRepository;

	public ConsultationController(
			ConsultationService consultationService,
			MedicalDocumentRepository medicalDocumentRepository,
			PatientService patientService,
			PatientRepository patientRepository,
			VisitRepository visitRepository) {
		this.consultationService = consultationService;
		this.medicalDocumentRepository = medicalDocumentRepository;
		this.patientService = patientService;
		this.patientRepository = patientRepository;
		this.visitRepository = visitRepository;
	}

	@PostMapping("/{id}/consultation")
	@ResponseStatus(HttpStatus.OK)
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE')")
	@Operation(summary = "Enregistrer une consultation", description = "Sauvegarde ou met à jour la consultation médicale d'une visite.", responses = {
			@ApiResponse(responseCode = "200", description = "Consultation enregistrée avec succès"),
			@ApiResponse(responseCode = "404", description = "Introuvable")
	})
	public ConsultationResponse saveConsultation(
			@Parameter(description = "Identifiant de la visite") @PathVariable UUID id,
			@Valid @RequestBody SaveConsultationRequest request,
			Authentication authentication) {
		UUID patientId = PatientService.convertToUuid(
				visitRepository.findPatientIdByVisitId(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable."))
		);
		patientService.validateAccessForSubResource(patientId, "medical_records", "Visite introuvable.");
		var patient = patientRepository.findByIdGlobally(patientId).orElseThrow();
		UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
		try {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
			String doctorEmail = authentication.getName();
			var consultation = consultationService.saveConsultation(id, doctorEmail, request);
			return ConsultationResponse.fromEntity(consultation);
		} finally {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
		}
	}

	@GetMapping("/{id}/consultation")
	@PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'AGENT_ACCUEIL', 'ADMIN_CLINIQUE')")
	@Operation(summary = "Récupérer une consultation", description = "Retourne la consultation médicale associée à une visite.", responses = {
			@ApiResponse(responseCode = "200", description = "Consultation trouvée"),
			@ApiResponse(responseCode = "404", description = "Introuvable")
	})
	public ConsultationResponse getConsultation(@Parameter(description = "Identifiant de la visite") @PathVariable UUID id) {
		UUID patientId = PatientService.convertToUuid(
				visitRepository.findPatientIdByVisitId(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable."))
		);
		patientService.validateAccessForSubResource(patientId, "medical_records", "Visite introuvable.");
		var patient = patientRepository.findByIdGlobally(patientId).orElseThrow();
		UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
		try {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
			return consultationService.getDetailedConsultationByVisitId(id);
		} finally {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
		}
	}
}
