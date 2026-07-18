package com.joprelys.backend.fhir.api;

import com.joprelys.backend.fhir.application.FhirService;
import com.joprelys.backend.fhir.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/fhir")
@Tag(name = "HL7 FHIR R4", description = "Endpoints d'interopérabilité conformes aux standards HL7 FHIR R4")
public class FhirController {

	private final FhirService fhirService;

	public FhirController(FhirService fhirService) {
		this.fhirService = fhirService;
	}

	@GetMapping("/Patient/{id}")
	@PreAuthorize("hasAuthority('PATIENT_READ')")
	@Operation(
			summary = "Récupérer un Patient au format FHIR",
			description = "Retourne la ressource Patient correspondante si l'utilisateur est authentifié et possède le consentement d'accès.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Ressource Patient retournée avec succès"),
					@ApiResponse(responseCode = "401", description = "Utilisateur non authentifié"),
					@ApiResponse(responseCode = "403", description = "Accès interdit (rôle insuffisant ou consentement requis)"),
					@ApiResponse(responseCode = "404", description = "Patient introuvable")
			}
	)
	public FhirPatientDto getPatient(
			@PathVariable @Parameter(description = "Identifiant unique du patient (UUID)") UUID id) {
		return fhirService.getPatient(id);
	}

	@GetMapping("/Encounter/{id}")
	@PreAuthorize("hasAuthority('CLINICAL_READ')")
	@Operation(
			summary = "Récupérer une Rencontre (visite) au format FHIR",
			description = "Retourne la ressource Encounter correspondante après vérification des consentements d'accès du patient associé.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Ressource Encounter retournée avec succès"),
					@ApiResponse(responseCode = "401", description = "Utilisateur non authentifié"),
					@ApiResponse(responseCode = "403", description = "Accès interdit (rôle insuffisant ou consentement requis)"),
					@ApiResponse(responseCode = "404", description = "Visite introuvable")
			}
	)
	public FhirEncounterDto getEncounter(
			@PathVariable @Parameter(description = "Identifiant unique de la visite (UUID)") UUID id) {
		return fhirService.getEncounter(id);
	}

	@GetMapping("/Observation")
	@PreAuthorize("hasAuthority('CLINICAL_READ')")
	@Operation(
			summary = "Récupérer les Constantes d'un Patient (Observations FHIR)",
			description = "Retourne un Bundle FHIR contenant l'ensemble des observations (constantes vitales) d'un patient.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Bundle d'observations retourné avec succès"),
					@ApiResponse(responseCode = "401", description = "Utilisateur non authentifié"),
					@ApiResponse(responseCode = "403", description = "Accès interdit (rôle insuffisant ou consentement requis)"),
					@ApiResponse(responseCode = "404", description = "Patient introuvable")
			}
	)
	public FhirBundleDto<FhirObservationDto> getObservations(
			@RequestParam("patient") @Parameter(description = "Identifiant unique du patient (UUID)") UUID patientId) {
		return fhirService.getObservations(patientId);
	}

	@GetMapping("/DiagnosticReport")
	@PreAuthorize("hasAuthority('LAB_ORDER_READ')")
	@Operation(
			summary = "Récupérer les Comptes-rendus d'examens d'un Patient (DiagnosticReport FHIR)",
			description = "Retourne un Bundle FHIR contenant l'ensemble des comptes-rendus de laboratoire d'un patient.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Bundle de rapports retourné avec succès"),
					@ApiResponse(responseCode = "401", description = "Utilisateur non authentifié"),
					@ApiResponse(responseCode = "403", description = "Accès interdit (rôle insuffisant ou consentement requis)"),
					@ApiResponse(responseCode = "404", description = "Patient introuvable")
			}
	)
	public FhirBundleDto<FhirDiagnosticReportDto> getDiagnosticReports(
			@RequestParam("patient") @Parameter(description = "Identifiant unique du patient (UUID)") UUID patientId) {
		return fhirService.getDiagnosticReports(patientId);
	}
}
