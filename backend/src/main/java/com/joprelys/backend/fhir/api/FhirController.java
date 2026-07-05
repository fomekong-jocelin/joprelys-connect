package com.joprelys.backend.fhir.api;

import com.joprelys.backend.fhir.application.FhirService;
import com.joprelys.backend.fhir.model.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/fhir")
@PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'BIOLOGISTE')")
public class FhirController {

	private final FhirService fhirService;

	public FhirController(FhirService fhirService) {
		this.fhirService = fhirService;
	}

	@GetMapping("/Patient/{id}")
	public FhirPatientDto getPatient(@PathVariable UUID id) {
		return fhirService.getPatient(id);
	}

	@GetMapping("/Encounter/{id}")
	public FhirEncounterDto getEncounter(@PathVariable UUID id) {
		return fhirService.getEncounter(id);
	}

	@GetMapping("/Observation")
	public FhirBundleDto<FhirObservationDto> getObservations(@RequestParam("patient") UUID patientId) {
		return fhirService.getObservations(patientId);
	}
}
