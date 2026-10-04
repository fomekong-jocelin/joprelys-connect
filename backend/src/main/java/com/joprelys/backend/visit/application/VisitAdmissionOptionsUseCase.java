package com.joprelys.backend.visit.application;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;

/**
 * Référentiels nécessaires pour ouvrir une visite, accessibles à l'accueil sans droit
 * de gestion du personnel : services de l'établissement et cliniciens (noms et unités uniquement).
 */
public interface VisitAdmissionOptionsUseCase {

	record PractitionerOption(UUID id, String displayName, String role, List<String> unitNames) {
	}

	record AdmissionOptions(List<String> services, List<PractitionerOption> practitioners) {
	}

	AdmissionOptions getAdmissionOptions(Authentication authentication);
}
