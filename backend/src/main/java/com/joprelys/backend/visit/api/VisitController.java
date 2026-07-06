package com.joprelys.backend.visit.api;

import com.joprelys.backend.visit.application.VisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/visits")
@Tag(name = "Visites", description = "Gestion des visites et constantes vitales")
public class VisitController {

	private final VisitService visitService;

	public VisitController(VisitService visitService) {
		this.visitService = visitService;
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE')")
	@Operation(summary = "Créer une visite", description = "Crée une nouvelle visite pour un patient avec motif, orientation, service, praticien et date d'arrivée.", responses = {
			@ApiResponse(responseCode = "200", description = "Visite créée avec succès"),
			@ApiResponse(responseCode = "404", description = "Introuvable")
	})
	public VisitResponse create(@Valid @RequestBody CreateVisitRequest request) {
		var visit = visitService.createVisit(request);
		return VisitResponse.fromEntity(visit);
	}

	@PostMapping("/{id}/correct")
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE')")
	@Operation(summary = "Corriger une visite clôturée", description = "Corrige une visite terminée ou annulée avec traçabilité de la correction.", responses = {
			@ApiResponse(responseCode = "200", description = "Visite corrigée avec succès"),
			@ApiResponse(responseCode = "404", description = "Introuvable")
	})
	public VisitResponse correct(
			@Parameter(description = "Identifiant de la visite") @PathVariable UUID id,
			@Valid @RequestBody CorrectVisitRequest request,
			Authentication authentication) {
		var claims = (com.joprelys.backend.auth.security.JwtClaims) authentication.getDetails();
		UUID userId = UUID.fromString(claims.subject());
		UUID organizationId = claims.organizationId() != null && !claims.organizationId().isBlank()
				? UUID.fromString(claims.organizationId())
				: null;
		var visit = visitService.correctVisit(id, request, userId, organizationId);
		return VisitResponse.fromEntity(visit);
	}

	@GetMapping("/active")
	@PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE')")
	@Operation(summary = "Lister les visites actives", description = "Retourne la liste des visites actuellement actives.", responses = {
			@ApiResponse(responseCode = "200", description = "Liste des visites actives retournée"),
			@ApiResponse(responseCode = "404", description = "Introuvable")
	})
	public List<VisitResponse> getActiveVisits() {
		return visitService.getActiveVisits().stream()
				.map(VisitResponse::fromEntity)
				.toList();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE')")
	@Operation(summary = "Récupérer une visite", description = "Retourne les détails d'une visite par son identifiant.", responses = {
			@ApiResponse(responseCode = "200", description = "Visite trouvée"),
			@ApiResponse(responseCode = "404", description = "Introuvable")
	})
	public VisitResponse getVisit(@Parameter(description = "Identifiant de la visite") @PathVariable UUID id) {
		return VisitResponse.fromEntity(visitService.getVisit(id));
	}

	@PostMapping("/{id}/close")
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE')")
	@Operation(summary = "Clôturer une visite", description = "Clôture une visite en cours.", responses = {
			@ApiResponse(responseCode = "200", description = "Visite clôturée avec succès"),
			@ApiResponse(responseCode = "404", description = "Introuvable")
	})
	public VisitResponse close(@Parameter(description = "Identifiant de la visite") @PathVariable UUID id) {
		var visit = visitService.closeVisit(id);
		return VisitResponse.fromEntity(visit);
	}

	@PostMapping("/{id}/cancel")
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE')")
	@Operation(summary = "Annuler une visite", description = "Annule une visite existante.", responses = {
			@ApiResponse(responseCode = "200", description = "Visite annulée avec succès"),
			@ApiResponse(responseCode = "404", description = "Introuvable")
	})
	public VisitResponse cancel(@Parameter(description = "Identifiant de la visite") @PathVariable UUID id) {
		var visit = visitService.cancelVisit(id);
		return VisitResponse.fromEntity(visit);
	}

	@PostMapping("/{id}/vitals")
	@PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE')")
	@Operation(summary = "Enregistrer les constantes vitales", description = "Sauvegarde les constantes vitales associées à une visite.", responses = {
			@ApiResponse(responseCode = "200", description = "Constantes vitales enregistrées"),
			@ApiResponse(responseCode = "404", description = "Introuvable")
	})
	public VitalsResponse saveVitals(@Parameter(description = "Identifiant de la visite") @PathVariable UUID id, @Valid @RequestBody SaveVitalsRequest request) {
		var vitals = visitService.saveVitals(id, request);
		return VitalsResponse.fromEntity(vitals);
	}

	@GetMapping("/{id}/vitals")
	@PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE')")
	@Operation(summary = "Récupérer les constantes vitales", description = "Retourne les constantes vitales d'une visite.", responses = {
			@ApiResponse(responseCode = "200", description = "Constantes vitales retournées"),
			@ApiResponse(responseCode = "404", description = "Introuvable")
	})
	public VitalsResponse getVitals(@Parameter(description = "Identifiant de la visite") @PathVariable UUID id) {
		return visitService.getVitals(id)
				.map(VitalsResponse::fromEntity)
				.orElse(null);
	}

	@GetMapping("/patient/{patientId}")
	@PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE')")
	@Operation(summary = "Lister les visites d'un patient", description = "Retourne la liste complète des visites d'un patient.", responses = {
			@ApiResponse(responseCode = "200", description = "Liste des visites retournée")
	})
	public List<VisitResponse> getPatientVisits(@PathVariable UUID patientId) {
		return visitService.getPatientVisits(patientId).stream()
				.map(VisitResponse::fromEntity)
				.toList();
	}
}
