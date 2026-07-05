package com.joprelys.backend.prescription.api;

import com.joprelys.backend.prescription.application.PrescriptionService;
import com.joprelys.backend.patient.application.PatientService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class PrescriptionController {

	private final PrescriptionService prescriptionService;
	private final PatientService patientService;
	private final PatientRepository patientRepository;
	private final ConsultationRepository consultationRepository;
	private final PrescriptionRepository prescriptionRepository;

	public PrescriptionController(
			PrescriptionService prescriptionService,
			PatientService patientService,
			PatientRepository patientRepository,
			ConsultationRepository consultationRepository,
			PrescriptionRepository prescriptionRepository) {
		this.prescriptionService = prescriptionService;
		this.patientService = patientService;
		this.patientRepository = patientRepository;
		this.consultationRepository = consultationRepository;
		this.prescriptionRepository = prescriptionRepository;
	}

	@PostMapping("/consultations/{id}/prescription")
	@ResponseStatus(HttpStatus.OK)
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE')")
	public PrescriptionResponse savePrescription(
			@PathVariable UUID id,
			@Valid @RequestBody SavePrescriptionRequest request) {
		UUID patientId = PatientService.convertToUuid(
				consultationRepository.findPatientIdByConsultationId(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation introuvable."))
		);
		patientService.validateAccess(patientId, "prescriptions");
		var patient = patientRepository.findByIdGlobally(patientId).orElseThrow();
		UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
		try {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
			return PrescriptionResponse.fromEntity(prescriptionService.savePrescription(id, request));
		} finally {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
		}
	}

	@GetMapping("/consultations/{id}/prescription")
	@PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'PHARMACIEN')")
	public PrescriptionResponse getPrescription(@PathVariable UUID id) {
		UUID patientId = PatientService.convertToUuid(
				consultationRepository.findPatientIdByConsultationId(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation introuvable."))
		);
		patientService.validateAccess(patientId, "prescriptions");
		var patient = patientRepository.findByIdGlobally(patientId).orElseThrow();
		UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
		try {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
			return prescriptionService.getPrescription(id)
					.map(PrescriptionResponse::fromEntity)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
							"Aucune prescription trouvée pour cette consultation."));
		} finally {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
		}
	}

	@PostMapping("/prescriptions/{id}/transmit")
	@ResponseStatus(HttpStatus.OK)
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE')")
	public PrescriptionResponse transmitPrescription(
			@PathVariable UUID id,
			Authentication authentication) {
		UUID patientId = PatientService.convertToUuid(
				prescriptionRepository.findPatientIdByPrescriptionId(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordonnance introuvable."))
		);
		patientService.validateAccess(patientId, "prescriptions");
		var patient = patientRepository.findByIdGlobally(patientId).orElseThrow();
		UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
		try {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
			String actorEmail = authentication.getName();
			return PrescriptionResponse.fromEntity(prescriptionService.transmitPrescriptionForStaff(id, actorEmail));
		} finally {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
		}
	}
}
