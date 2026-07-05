package com.joprelys.backend.lab.api;

import com.joprelys.backend.lab.application.LabOrderService;
import com.joprelys.backend.lab.application.LabResultService;
import com.joprelys.backend.patient.application.PatientService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lab-orders")
public class LabOrderController {

	private final LabOrderService labOrderService;
	private final LabResultService labResultService;
	private final PatientService patientService;
	private final PatientRepository patientRepository;

	public LabOrderController(
			LabOrderService labOrderService,
			LabResultService labResultService,
			PatientService patientService,
			PatientRepository patientRepository) {
		this.labOrderService = labOrderService;
		this.labResultService = labResultService;
		this.patientService = patientService;
		this.patientRepository = patientRepository;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE')")
	public LabOrderResponse create(
			@Valid @RequestBody CreateLabOrderRequest request,
			Authentication authentication) {
		patientService.validateAccess(request.patientId(), "lab_results");
		var patient = patientRepository.findByIdGlobally(request.patientId()).orElseThrow();
		UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
		try {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
			String practitionerEmail = authentication.getName();
			return labOrderService.create(request, practitionerEmail);
		} finally {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
		}
	}

	@GetMapping("/patient/{patientId}")
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE', 'PATIENT', 'BIOLOGISTE')")
	public List<LabOrderResponse> getPatientLabOrders(@PathVariable UUID patientId) {
		patientService.validateAccess(patientId, "lab_results");
		var patient = patientRepository.findByIdGlobally(patientId).orElseThrow();
		UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
		try {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
			return labOrderService.getPatientLabOrders(patientId);
		} finally {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
		}
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('BIOLOGISTE', 'ADMIN_JOPRELYS')")
	public List<LabOrderResponse> getLabOrders() {
		return labOrderService.getLabOrders();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('BIOLOGISTE', 'ADMIN_JOPRELYS')")
	public LabOrderResponse getLabOrder(@PathVariable UUID id) {
		return labOrderService.getLabOrder(id);
	}

	@PatchMapping("/{id}/status")
	@PreAuthorize("hasAnyRole('BIOLOGISTE', 'ADMIN_JOPRELYS')")
	public LabOrderResponse updateStatus(
			@PathVariable UUID id,
			@Valid @RequestBody UpdateLabOrderStatusRequest request) {
		return labOrderService.updateStatus(id, request.status());
	}

	@GetMapping("/patient/{patientId}/results")
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE', 'PATIENT', 'BIOLOGISTE')")
	public List<LabResultResponse> getPatientResults(@PathVariable UUID patientId) {
		patientService.validateAccess(patientId, "lab_results");
		var patient = patientRepository.findByIdGlobally(patientId).orElseThrow();
		UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
		try {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
			return labResultService.getPatientResults(patientId);
		} finally {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
		}
	}
}
