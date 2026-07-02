package com.joprelys.backend.consultation.api;

import com.joprelys.backend.consultation.application.ConsultationService;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
public class ConsultationHistoryController {

	private final ConsultationService consultationService;
	private final MedicalDocumentRepository medicalDocumentRepository;

	public ConsultationHistoryController(
			ConsultationService consultationService,
			MedicalDocumentRepository medicalDocumentRepository) {
		this.consultationService = consultationService;
		this.medicalDocumentRepository = medicalDocumentRepository;
	}

	@GetMapping("/{patientId}/consultations")
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE', 'INFIRMIER')")
	public List<ConsultationResponse> getPatientConsultations(@PathVariable UUID patientId) {
		return consultationService.getConsultationsByPatientId(patientId).stream()
				.map(c -> {
					var doc = medicalDocumentRepository.findByVisitId(c.getVisit().getId()).orElse(null);
					return ConsultationResponse.fromEntity(c, doc);
				})
				.toList();
	}
}
