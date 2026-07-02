package com.joprelys.backend.lab.api;

import com.joprelys.backend.lab.application.LabOrderService;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lab-orders")
public class LabOrderController {

	private final LabOrderService labOrderService;

	public LabOrderController(LabOrderService labOrderService) {
		this.labOrderService = labOrderService;
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
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE', 'PATIENT')")
	public List<LabOrderResponse> getPatientLabOrders(@PathVariable UUID patientId) {
		return labOrderService.getPatientLabOrders(patientId);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE')")
	public LabOrderResponse getLabOrder(@PathVariable UUID id) {
		return labOrderService.getLabOrder(id);
	}
}
