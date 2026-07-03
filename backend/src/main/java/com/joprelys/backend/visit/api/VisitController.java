package com.joprelys.backend.visit.api;

import com.joprelys.backend.visit.application.VisitService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class VisitController {

	private final VisitService visitService;

	public VisitController(VisitService visitService) {
		this.visitService = visitService;
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE')")
	public VisitResponse create(@Valid @RequestBody CreateVisitRequest request) {
		var visit = visitService.createVisit(request.patientId(), request.reason(), request.orientation());
		return VisitResponse.fromEntity(visit);
	}

	@GetMapping("/active")
	@PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE')")
	public List<VisitResponse> getActiveVisits() {
		return visitService.getActiveVisits().stream()
				.map(VisitResponse::fromEntity)
				.toList();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE')")
	public VisitResponse getVisit(@PathVariable UUID id) {
		return VisitResponse.fromEntity(visitService.getVisit(id));
	}

	@PostMapping("/{id}/close")
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE')")
	public VisitResponse close(@PathVariable UUID id) {
		var visit = visitService.closeVisit(id);
		return VisitResponse.fromEntity(visit);
	}

	@PostMapping("/{id}/cancel")
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE')")
	public VisitResponse cancel(@PathVariable UUID id) {
		var visit = visitService.cancelVisit(id);
		return VisitResponse.fromEntity(visit);
	}

	@PostMapping("/{id}/vitals")
	@PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE')")
	public VitalsResponse saveVitals(@PathVariable UUID id, @Valid @RequestBody SaveVitalsRequest request) {
		var vitals = visitService.saveVitals(id, request);
		return VitalsResponse.fromEntity(vitals);
	}

	@GetMapping("/{id}/vitals")
	@PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE')")
	public VitalsResponse getVitals(@PathVariable UUID id) {
		return visitService.getVitals(id)
				.map(VitalsResponse::fromEntity)
				.orElse(null);
	}
}
