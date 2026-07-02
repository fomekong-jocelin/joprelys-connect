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

@RestController
@RequestMapping("/api/patients")
@PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE')")
public class PatientController {

	private final PatientService patientService;

	public PatientController(PatientService patientService) {
		this.patientService = patientService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PatientResponse create(@Valid @RequestBody CreatePatientRequest request) {
		PatientEntity entity = patientService.createPatient(request);
		return mapToResponse(entity);
	}

	@GetMapping
	public List<PatientResponse> list(@RequestParam(value = "q", required = false) String query) {
		return patientService.searchPatients(query).stream()
				.map(this::mapToResponse)
				.toList();
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
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				emergencyActive
		);
	}
}

record EmergencyAccessRequest(String reason) {}
