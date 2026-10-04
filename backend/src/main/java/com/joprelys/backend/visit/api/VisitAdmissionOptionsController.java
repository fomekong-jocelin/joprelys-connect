package com.joprelys.backend.visit.api;

import com.joprelys.backend.visit.application.VisitAdmissionOptionsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Visites", description = "Gestion des visites et constantes vitales")
public class VisitAdmissionOptionsController {

	private final VisitAdmissionOptionsUseCase admissionOptions;

	public VisitAdmissionOptionsController(VisitAdmissionOptionsUseCase admissionOptions) {
		this.admissionOptions = admissionOptions;
	}

	@GetMapping("/api/visits/admission-options")
	@PreAuthorize("hasAuthority('VISIT_CREATE')")
	@Operation(summary = "Référentiels d'ouverture de visite",
			description = "Services de l'établissement et cliniciens pouvant être praticien principal.")
	public VisitAdmissionOptionsUseCase.AdmissionOptions getAdmissionOptions(Authentication authentication) {
		return admissionOptions.getAdmissionOptions(authentication);
	}
}
