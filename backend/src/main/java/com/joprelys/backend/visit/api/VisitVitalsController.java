package com.joprelys.backend.visit.api;

import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.visit.application.VisitVitalsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/visits/{id}/vitals")
@Tag(name = "Visites", description = "Gestion des visites et constantes vitales")
public class VisitVitalsController {

	private final VisitVitalsUseCase visitVitals;

	public VisitVitalsController(VisitVitalsUseCase visitVitals) {
		this.visitVitals = visitVitals;
	}

	@PostMapping
	@PreAuthorize("hasAuthority('VISIT_VITALS_WRITE')")
	@Operation(summary = "Enregistrer une mesure de constantes", description = "Ajoute une mesure horodatée et signée à la visite.", responses = {
			@ApiResponse(responseCode = "200", description = "Constantes vitales enregistrées"),
			@ApiResponse(responseCode = "404", description = "Introuvable")
	})
	public VitalsResponse saveVitals(
			@Parameter(description = "Identifiant de la visite") @PathVariable UUID id,
			@Valid @RequestBody SaveVitalsRequest request,
			Authentication authentication) {
		var claims = (JwtClaims) authentication.getDetails();
		UUID userId = UUID.fromString(claims.subject());
		UUID organizationId = claims.organizationId() != null && !claims.organizationId().isBlank()
				? UUID.fromString(claims.organizationId())
				: null;
		return VitalsResponse.fromEntity(visitVitals.recordVitals(id, request, userId, organizationId));
	}

	@GetMapping
	@PreAuthorize("hasAuthority('VISIT_READ')")
	@Operation(summary = "Récupérer la dernière mesure de constantes", responses = {
			@ApiResponse(responseCode = "200", description = "Constantes vitales retournées"),
			@ApiResponse(responseCode = "404", description = "Introuvable")
	})
	public VitalsResponse getVitals(@Parameter(description = "Identifiant de la visite") @PathVariable UUID id) {
		return visitVitals.getLatestVitals(id)
				.map(VitalsResponse::fromEntity)
				.orElse(null);
	}

	@GetMapping("/history")
	@PreAuthorize("hasAuthority('VISIT_READ')")
	@Operation(summary = "Historique des mesures de constantes", description = "Mesures de la visite, de la plus récente à la plus ancienne.")
	public List<VitalMeasurementResponse> getVitalsHistory(@Parameter(description = "Identifiant de la visite") @PathVariable UUID id) {
		return visitVitals.getVitalsHistory(id).stream()
				.map(VitalMeasurementResponse::fromEntity)
				.toList();
	}
}
