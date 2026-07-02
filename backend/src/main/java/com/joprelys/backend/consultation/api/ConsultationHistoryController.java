package com.joprelys.backend.consultation.api;

import com.joprelys.backend.consultation.application.ConsultationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
public class ConsultationHistoryController {

	private final ConsultationService consultationService;

	public ConsultationHistoryController(ConsultationService consultationService) {
		this.consultationService = consultationService;
	}

	@GetMapping("/{patientId}/consultations")
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE', 'INFIRMIER')")
	public List<ConsultationResponse> getPatientConsultations(@PathVariable UUID patientId) {
		return consultationService.getConsultationsByPatientId(patientId).stream()
				.map(ConsultationResponse::fromEntity)
				.toList();
	}
}
