package com.joprelys.backend.visit.infrastructure.persistence;

/**
 * Étape de prise en charge d'une visite active (le statut EN_COURS/TERMINEE/ANNULEE reste le cycle administratif).
 */
public enum VisitCareStage {
	ATTENTE_CONSTANTES,
	PRET_MEDECIN,
	EN_CONSULTATION
}
