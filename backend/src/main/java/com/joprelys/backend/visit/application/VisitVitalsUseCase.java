package com.joprelys.backend.visit.application;

import com.joprelys.backend.visit.api.SaveVitalsRequest;
import com.joprelys.backend.visit.infrastructure.persistence.VitalMeasurementEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Saisie et consultation des constantes d'une visite.
 */
public interface VisitVitalsUseCase {

	/**
	 * Enregistre une nouvelle mesure : met à jour la dernière mesure, l'ajoute à l'historique,
	 * fait passer la visite à l'étape « prêt pour le médecin » et trace l'auteur.
	 */
	VitalsEntity recordVitals(UUID visitId, SaveVitalsRequest request, UUID actorUserId, UUID actorOrganizationId);

	Optional<VitalsEntity> getLatestVitals(UUID visitId);

	/** Mesures de la visite, de la plus récente à la plus ancienne. */
	List<VitalMeasurementEntity> getVitalsHistory(UUID visitId);
}
