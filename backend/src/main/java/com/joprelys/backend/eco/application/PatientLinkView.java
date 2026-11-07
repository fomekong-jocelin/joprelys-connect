package com.joprelys.backend.eco.application;

import com.joprelys.backend.eco.infrastructure.persistence.EcoConsentEntity;
import com.joprelys.backend.eco.infrastructure.persistence.EcoPatientLinkEntity;

/**
 * Liaison et consentement associés.
 *
 * @param created {@code true} si l'appel a créé la liaison, {@code false} s'il a renvoyé une liaison existante
 */
public record PatientLinkView(EcoPatientLinkEntity link, EcoConsentEntity consent, boolean created) {
}
