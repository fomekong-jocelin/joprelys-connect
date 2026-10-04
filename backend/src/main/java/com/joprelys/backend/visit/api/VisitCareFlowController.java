package com.joprelys.backend.visit.api;

import com.joprelys.backend.visit.application.VisitCareFlowUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/visits/{id}")
@Tag(name = "Visites", description = "Gestion des visites et constantes vitales")
public class VisitCareFlowController {

	private final VisitCareFlowUseCase careFlow;

	public VisitCareFlowController(VisitCareFlowUseCase careFlow) {
		this.careFlow = careFlow;
	}

	@PostMapping("/take-charge")
	@PreAuthorize("hasAuthority('CLINICAL_WRITE')")
	@Operation(summary = "Prendre le patient en consultation", responses = {
			@ApiResponse(responseCode = "200", description = "Patient en consultation chez le praticien connecté"),
			@ApiResponse(responseCode = "409", description = "Patient déjà en consultation chez un confrère")
	})
	public VisitResponse takeCharge(
			@Parameter(description = "Identifiant de la visite") @PathVariable UUID id,
			@Parameter(description = "Reprendre explicitement un patient pris en charge par un confrère")
			@RequestParam(name = "takeOver", defaultValue = "false") boolean takeOver,
			Authentication authentication) {
		return VisitResponse.fromEntity(careFlow.startConsultation(id, authentication.getName(), takeOver));
	}

	@PostMapping("/release")
	@PreAuthorize("hasAuthority('CLINICAL_WRITE')")
	@Operation(summary = "Remettre le patient dans la file", description = "Libère la prise en charge sans clôturer la visite.")
	public VisitResponse release(
			@Parameter(description = "Identifiant de la visite") @PathVariable UUID id,
			Authentication authentication) {
		return VisitResponse.fromEntity(careFlow.releaseConsultation(id, authentication.getName()));
	}
}
