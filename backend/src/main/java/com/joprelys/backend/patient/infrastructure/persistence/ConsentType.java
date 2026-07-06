package com.joprelys.backend.patient.infrastructure.persistence;

/**
 * Types de consentement conformes au CDC — Module 12.
 *
 * - PONCTUEL       : accès unique ponctuel (consultation d'urgence, etc.)
 * - TEMPORAIRE     : accès limité dans le temps (soins de suite, etc.)
 * - ETABLISSEMENT  : accès par clinique/établissement (consentement courant)
 * - PROFESSIONNEL  : accès nominatif à un praticien particulier
 * - LIMITE         : accès restreint à un sous-ensemble de données (scopes)
 * - URGENCE        : accès d'urgence validé sans délai patient
 */
public enum ConsentType {
    PONCTUEL,
    TEMPORAIRE,
    ETABLISSEMENT,
    PROFESSIONNEL,
    LIMITE,
    URGENCE
}
