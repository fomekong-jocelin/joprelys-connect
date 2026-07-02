package com.joprelys.backend.consultation.api;

import com.joprelys.backend.consultation.application.ConsultationService;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@RestController
@RequestMapping("/api/visits")
public class ConsultationController {

	private final ConsultationService consultationService;
	private final MedicalDocumentRepository medicalDocumentRepository;

	public ConsultationController(
			ConsultationService consultationService,
			MedicalDocumentRepository medicalDocumentRepository) {
		this.consultationService = consultationService;
		this.medicalDocumentRepository = medicalDocumentRepository;
	}

	/**
	 * POST /api/visits/{id}/consultation
	 * Creates or updates the consultation for the given visit.
	 * Accessible to MEDECIN and ADMIN_CLINIQUE only.
	 */
	@PostMapping("/{id}/consultation")
	@ResponseStatus(HttpStatus.OK)
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE')")
	public ConsultationResponse saveConsultation(
			@PathVariable UUID id,
			@Valid @RequestBody SaveConsultationRequest request,
			Authentication authentication) {
		String doctorEmail = authentication.getName();
		var consultation = consultationService.saveConsultation(id, doctorEmail, request);
		return ConsultationResponse.fromEntity(consultation);
	}

	/**
	 * GET /api/visits/{id}/consultation
	 * Returns the consultation linked to a visit, or 404 if none.
	 * Accessible to all clinical staff.
	 */
	@GetMapping("/{id}/consultation")
	@PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'AGENT_ACCUEIL', 'ADMIN_CLINIQUE')")
	public ConsultationResponse getConsultation(@PathVariable UUID id) {
		var consultation = consultationService.getConsultationByVisitId(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Aucune consultation trouvée pour cette visite."));
		var doc = medicalDocumentRepository.findByVisitId(id).orElse(null);
		return ConsultationResponse.fromEntity(consultation, doc);
	}
}
