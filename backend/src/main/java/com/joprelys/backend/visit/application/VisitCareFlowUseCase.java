package com.joprelys.backend.visit.application;

import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import java.util.UUID;

/**
 * Prise en charge médicale d'une visite active : un seul praticien à la fois.
 */
public interface VisitCareFlowUseCase {

	/**
	 * Le praticien prend le patient en consultation.
	 *
	 * @param takeOver {@code true} pour reprendre explicitement un patient déjà en consultation chez un confrère
	 * @throws org.springframework.web.server.ResponseStatusException 409 si un confrère l'a déjà en charge sans reprise
	 */
	VisitEntity startConsultation(UUID visitId, String practitionerEmail, boolean takeOver);

	/** Remet le patient dans la file sans clôturer la visite ; réservé au praticien qui l'a en charge. */
	VisitEntity releaseConsultation(UUID visitId, String practitionerEmail);

	/**
	 * Garantit que le praticien qui enregistre la consultation l'a bien en charge (prise en charge implicite si personne).
	 */
	void ensureConsultationOwnership(VisitEntity visit, UUID practitionerId, String practitionerName);
}
