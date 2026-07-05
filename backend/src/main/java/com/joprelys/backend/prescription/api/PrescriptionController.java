package com.joprelys.backend.prescription.api;

import com.joprelys.backend.prescription.application.PrescriptionService;
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

	public PrescriptionController(PrescriptionService prescriptionService) {
		this.prescriptionService = prescriptionService;
	}

	@PostMapping("/consultations/{id}/prescription")
	@ResponseStatus(HttpStatus.OK)
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE')")
	public PrescriptionResponse savePrescription(
			@PathVariable UUID id,
			@Valid @RequestBody SavePrescriptionRequest request) {
		return PrescriptionResponse.fromEntity(prescriptionService.savePrescription(id, request));
	}

	@GetMapping("/consultations/{id}/prescription")
	@PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'PHARMACIEN')")
	public PrescriptionResponse getPrescription(@PathVariable UUID id) {
		return prescriptionService.getPrescription(id)
				.map(PrescriptionResponse::fromEntity)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Aucune prescription trouvée pour cette consultation."));
	}

	@PostMapping("/prescriptions/{id}/transmit")
	@ResponseStatus(HttpStatus.OK)
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE')")
	public PrescriptionResponse transmitPrescription(
			@PathVariable UUID id,
			Authentication authentication) {
		String actorEmail = authentication.getName();
		return PrescriptionResponse.fromEntity(prescriptionService.transmitPrescriptionForStaff(id, actorEmail));
	}
}
