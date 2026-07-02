package com.joprelys.backend.prescription.api;

import com.joprelys.backend.prescription.application.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@RestController
@RequestMapping("/api/consultations")
public class PrescriptionController {

	private final PrescriptionService prescriptionService;

	public PrescriptionController(PrescriptionService prescriptionService) {
		this.prescriptionService = prescriptionService;
	}

	@PostMapping("/{id}/prescription")
	@ResponseStatus(HttpStatus.OK)
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE')")
	public PrescriptionResponse savePrescription(
			@PathVariable UUID id,
			@Valid @RequestBody SavePrescriptionRequest request) {
		return PrescriptionResponse.fromEntity(prescriptionService.savePrescription(id, request));
	}

	@GetMapping("/{id}/prescription")
	@PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'PHARMACIEN')")
	public PrescriptionResponse getPrescription(@PathVariable UUID id) {
		return prescriptionService.getPrescription(id)
				.map(PrescriptionResponse::fromEntity)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Aucune prescription trouvée pour cette consultation."));
	}
}
