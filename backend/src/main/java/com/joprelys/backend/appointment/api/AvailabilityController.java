package com.joprelys.backend.appointment.api;

import com.joprelys.backend.appointment.application.AvailabilityService;
import com.joprelys.backend.auth.security.JwtClaims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de gestion des disponibilités médecin (STORY-2602, contrat §2).
 * Aucune logique métier : tout est délégué à {@link AvailabilityService} avec l'identité
 * extraite du jeton JWT.
 */
@RestController
@RequestMapping("/api/availabilities")
@Tag(name = "Disponibilités médecins", description = "Gestion des plages de disponibilité récurrentes et des indisponibilités ponctuelles des médecins")
public class AvailabilityController {

	private final AvailabilityService availabilityService;

	public AvailabilityController(AvailabilityService availabilityService) {
		this.availabilityService = availabilityService;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('MEDECIN','ADMIN_CLINIQUE')")
	@Operation(summary = "Lister les règles de disponibilité",
			description = "Retourne les règles récurrentes du médecin connecté, ou du médecin indiqué pour un administrateur de clinique.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Liste des règles retournée"),
					@ApiResponse(responseCode = "401", description = "Non authentifié"),
					@ApiResponse(responseCode = "403", description = "Accès refusé")
			})
	public List<AvailabilityRuleResponse> listRules(
			@Parameter(description = "Identifiant du médecin visé (médecin connecté si omis)")
			@RequestParam(required = false) UUID doctorId,
			Authentication authentication) {
		return availabilityService.listRules(doctorId, callerId(authentication), isClinicAdmin(authentication));
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('MEDECIN','ADMIN_CLINIQUE')")
	@Operation(summary = "Créer une règle de disponibilité",
			description = "Crée une plage hebdomadaire récurrente. Un médecin ne peut créer que ses propres règles.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Règle créée"),
					@ApiResponse(responseCode = "400", description = "Plage horaire ou fenêtre de validité invalide"),
					@ApiResponse(responseCode = "401", description = "Non authentifié"),
					@ApiResponse(responseCode = "403", description = "Accès refusé"),
					@ApiResponse(responseCode = "409", description = "Chevauchement avec une règle active existante")
			})
	public AvailabilityRuleResponse createRule(
			@Valid @RequestBody UpsertAvailabilityRuleRequest request,
			Authentication authentication) {
		return availabilityService.createRule(
				request, callerId(authentication), organizationId(authentication), isClinicAdmin(authentication));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('MEDECIN','ADMIN_CLINIQUE')")
	@Operation(summary = "Modifier une règle de disponibilité",
			description = "Modifie une règle existante. Échoue si des rendez-vous actifs occupent les créneaux retirés (RM-07).",
			responses = {
					@ApiResponse(responseCode = "200", description = "Règle modifiée"),
					@ApiResponse(responseCode = "400", description = "Plage horaire ou fenêtre de validité invalide"),
					@ApiResponse(responseCode = "401", description = "Non authentifié"),
					@ApiResponse(responseCode = "403", description = "Accès refusé"),
					@ApiResponse(responseCode = "404", description = "Règle introuvable"),
					@ApiResponse(responseCode = "409", description = "Chevauchement ou rendez-vous actifs sur la plage")
			})
	public AvailabilityRuleResponse updateRule(
			@Parameter(description = "Identifiant de la règle") @PathVariable UUID id,
			@Valid @RequestBody UpsertAvailabilityRuleRequest request,
			Authentication authentication) {
		return availabilityService.updateRule(
				id, request, callerId(authentication), organizationId(authentication), isClinicAdmin(authentication));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyRole('MEDECIN','ADMIN_CLINIQUE')")
	@Operation(summary = "Désactiver une règle de disponibilité",
			description = "Désactivation logique (active = false), jamais de suppression silencieuse : échoue si des rendez-vous actifs occupent les créneaux de la règle (RM-07).",
			responses = {
					@ApiResponse(responseCode = "200", description = "Règle désactivée"),
					@ApiResponse(responseCode = "401", description = "Non authentifié"),
					@ApiResponse(responseCode = "403", description = "Accès refusé"),
					@ApiResponse(responseCode = "404", description = "Règle introuvable"),
					@ApiResponse(responseCode = "409", description = "Rendez-vous actifs sur la plage")
			})
	public AvailabilityRuleResponse deactivateRule(
			@Parameter(description = "Identifiant de la règle") @PathVariable UUID id,
			Authentication authentication) {
		return availabilityService.deactivateRule(id, callerId(authentication), isClinicAdmin(authentication));
	}

	@GetMapping("/exceptions")
	@PreAuthorize("hasAnyRole('MEDECIN','ADMIN_CLINIQUE')")
	@Operation(summary = "Lister les indisponibilités",
			description = "Retourne les indisponibilités du médecin connecté, ou du médecin indiqué pour un administrateur de clinique, éventuellement bornées par une période.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Liste des indisponibilités retournée"),
					@ApiResponse(responseCode = "400", description = "Période invalide"),
					@ApiResponse(responseCode = "401", description = "Non authentifié"),
					@ApiResponse(responseCode = "403", description = "Accès refusé")
			})
	public List<AvailabilityExceptionResponse> listExceptions(
			@Parameter(description = "Identifiant du médecin visé (médecin connecté si omis)")
			@RequestParam(required = false) UUID doctorId,
			@Parameter(description = "Début de période (Instant UTC, optionnel)")
			@RequestParam(required = false) Instant from,
			@Parameter(description = "Fin de période (Instant UTC, optionnel)")
			@RequestParam(required = false) Instant to,
			Authentication authentication) {
		return availabilityService.listExceptions(doctorId, from, to, callerId(authentication), isClinicAdmin(authentication));
	}

	@PostMapping("/exceptions")
	@PreAuthorize("hasAnyRole('MEDECIN','ADMIN_CLINIQUE')")
	@Operation(summary = "Créer une indisponibilité",
			description = "Déclare une indisponibilité ponctuelle (congé, absence) qui masque les créneaux correspondants.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Indisponibilité créée"),
					@ApiResponse(responseCode = "400", description = "Plage invalide"),
					@ApiResponse(responseCode = "401", description = "Non authentifié"),
					@ApiResponse(responseCode = "403", description = "Accès refusé")
			})
	public AvailabilityExceptionResponse createException(
			@Valid @RequestBody CreateAvailabilityExceptionRequest request,
			Authentication authentication) {
		return availabilityService.createException(
				request, callerId(authentication), organizationId(authentication), isClinicAdmin(authentication));
	}

	@DeleteMapping("/exceptions/{id}")
	@PreAuthorize("hasAnyRole('MEDECIN','ADMIN_CLINIQUE')")
	@Operation(summary = "Supprimer une indisponibilité",
			description = "Suppression physique d'une indisponibilité. Échoue si des rendez-vous actifs recouvrent sa plage (RM-07).",
			responses = {
					@ApiResponse(responseCode = "200", description = "Indisponibilité supprimée"),
					@ApiResponse(responseCode = "401", description = "Non authentifié"),
					@ApiResponse(responseCode = "403", description = "Accès refusé"),
					@ApiResponse(responseCode = "404", description = "Indisponibilité introuvable"),
					@ApiResponse(responseCode = "409", description = "Rendez-vous actifs sur la plage")
			})
	public void deleteException(
			@Parameter(description = "Identifiant de l'indisponibilité") @PathVariable UUID id,
			Authentication authentication) {
		availabilityService.deleteException(id, callerId(authentication), isClinicAdmin(authentication));
	}

	private static UUID callerId(Authentication authentication) {
		JwtClaims claims = (JwtClaims) authentication.getDetails();
		return UUID.fromString(claims.subject());
	}

	private static UUID organizationId(Authentication authentication) {
		JwtClaims claims = (JwtClaims) authentication.getDetails();
		return claims.organizationId() != null && !claims.organizationId().isBlank()
				? UUID.fromString(claims.organizationId())
				: null;
	}

	private static boolean isClinicAdmin(Authentication authentication) {
		return authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.anyMatch("ROLE_ADMIN_CLINIQUE"::equals);
	}
}
