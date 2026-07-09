package com.joprelys.backend.reception.api;

import com.joprelys.backend.reception.application.ReceptionLogService;
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
@RequestMapping("/api/reception")
@Tag(name = "Registre d'accueil", description = "Gestion des entrées/sorties des visiteurs et audiences")
public class ReceptionLogController {

	private final ReceptionLogService receptionLogService;

	public ReceptionLogController(ReceptionLogService receptionLogService) {
		this.receptionLogService = receptionLogService;
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE')")
	@Operation(summary = "Créer une entrée d'accueil", description = "Enregistre une nouvelle arrivée (visiteur, audience, patient) à l'accueil.", responses = {
			@ApiResponse(responseCode = "200", description = "Entrée enregistrée avec succès")
	})
	public ReceptionLogResponse create(
			@Valid @RequestBody CreateReceptionLogRequest request,
			Authentication authentication) {
		var claims = (com.joprelys.backend.auth.security.JwtClaims) authentication.getDetails();
		UUID userId = UUID.fromString(claims.subject());
		var log = receptionLogService.createReceptionLog(request, userId);
		return ReceptionLogResponse.fromEntity(log);
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE')")
	@Operation(summary = "Lister le registre d'accueil", description = "Retourne tous les enregistrements du registre d'accueil pour la clinique.", responses = {
			@ApiResponse(responseCode = "200", description = "Registre récupéré avec succès")
	})
	public List<ReceptionLogResponse> getAll() {
		return receptionLogService.getAllReceptionLogs().stream()
				.map(ReceptionLogResponse::fromEntity)
				.toList();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE')")
	@Operation(summary = "Récupérer un enregistrement", description = "Retourne un enregistrement par son identifiant.", responses = {
			@ApiResponse(responseCode = "200", description = "Enregistrement trouvé"),
			@ApiResponse(responseCode = "404", description = "Introuvable")
	})
	public ReceptionLogResponse getById(@Parameter(description = "Identifiant de l'enregistrement") @PathVariable UUID id) {
		var log = receptionLogService.getReceptionLog(id);
		return ReceptionLogResponse.fromEntity(log);
	}

	@PostMapping("/{id}/departure")
	@PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE')")
	@Operation(summary = "Enregistrer un départ", description = "Marque l'heure de départ pour un visiteur ou une audience.", responses = {
			@ApiResponse(responseCode = "200", description = "Départ enregistré avec succès"),
			@ApiResponse(responseCode = "404", description = "Introuvable")
	})
	public ReceptionLogResponse markDeparture(@Parameter(description = "Identifiant de l'enregistrement") @PathVariable UUID id) {
		var log = receptionLogService.markDeparture(id);
		return ReceptionLogResponse.fromEntity(log);
	}
}
