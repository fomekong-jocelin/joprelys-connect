package com.joprelys.backend.consultation.api;

import com.joprelys.backend.consultation.application.ConsultationService;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentRepository;
import com.joprelys.backend.patient.application.PatientService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@RestController
@RequestMapping("/api/visits")
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
	public ConsultationResponse saveConsultation(
			@PathVariable UUID id,
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
	public ConsultationResponse getConsultation(@PathVariable UUID id) {
		UUID patientId = PatientService.convertToUuid(
				visitRepository.findPatientIdByVisitId(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable."))
		);
		patientService.validateAccessForSubResource(patientId, "medical_records", "Visite introuvable.");
		var patient = patientRepository.findByIdGlobally(patientId).orElseThrow();
		UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
		try {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
			var consultation = consultationService.getConsultationByVisitId(id)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
							"Aucune consultation trouvée pour cette visite."));
			var doc = medicalDocumentRepository.findByVisitId(id).orElse(null);
			return ConsultationResponse.fromEntity(consultation, doc);
		} finally {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
		}
	}
}
