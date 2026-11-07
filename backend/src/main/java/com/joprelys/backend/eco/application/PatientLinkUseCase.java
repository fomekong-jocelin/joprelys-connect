package com.joprelys.backend.eco.application;

/** Relier le profil d'une application de l'écosystème au DPU du patient connecté à Joprelys ID. */
public interface PatientLinkUseCase {

    /**
     * Crée la liaison et son consentement, ou renvoie la liaison active déjà existante pour ce profil et ce DPU.
     *
     * @throws EcoBusinessException si le DPU est absent ou inconnu, la relation non prise en charge,
     *                              la période invalide ou le profil déjà relié à un autre DPU
     */
    PatientLinkView link(CreatePatientLinkCommand command);
}
