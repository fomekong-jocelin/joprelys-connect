package com.joprelys.backend.lab.api;

import com.joprelys.backend.lab.application.LabOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lab-orders")
public class LabOrderController {

	private final LabOrderService labOrderService;
	private final com.joprelys.backend.lab.application.LabResultService labResultService;

	public LabOrderController(
			LabOrderService labOrderService,
			com.joprelys.backend.lab.application.LabResultService labResultService) {
		this.labOrderService = labOrderService;
		this.labResultService = labResultService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE')")
	public LabOrderResponse create(
			@Valid @RequestBody CreateLabOrderRequest request,
			Authentication authentication) {
		String practitionerEmail = authentication.getName();
		return labOrderService.create(request, practitionerEmail);
	}

	@GetMapping("/patient/{patientId}")
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE', 'PATIENT', 'BIOLOGISTE')")
	public List<LabOrderResponse> getPatientLabOrders(@PathVariable UUID patientId) {
		return labOrderService.getPatientLabOrders(patientId);
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
		return labResultService.getPatientResults(patientId);
	}
}
