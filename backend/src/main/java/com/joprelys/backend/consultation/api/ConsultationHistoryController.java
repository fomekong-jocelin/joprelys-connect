package com.joprelys.backend.consultation.api;

import com.joprelys.backend.consultation.application.ConsultationService;
import com.joprelys.backend.patient.application.PatientService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
public class ConsultationHistoryController {

	private final ConsultationService consultationService;
	private final PatientService patientService;
	private final PatientRepository patientRepository;

	public ConsultationHistoryController(
			ConsultationService consultationService,
			PatientService patientService,
			PatientRepository patientRepository) {
		this.consultationService = consultationService;
		this.patientService = patientService;
		this.patientRepository = patientRepository;
	}

	@GetMapping("/{patientId}/consultations")
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE', 'INFIRMIER')")
	public List<ConsultationResponse> getPatientConsultations(@PathVariable UUID patientId) {
		patientService.validateAccess(patientId, "medical_records");
		var patient = patientRepository.findByIdGlobally(patientId).orElseThrow();
		UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
		try {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
			return consultationService.getDetailedConsultationsByPatientId(patientId);
		} finally {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
		}
	}
}
