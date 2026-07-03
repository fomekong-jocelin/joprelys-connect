package com.joprelys.backend.patient.application;

import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Centralise les contrôles d'accès IDOR pour le portail patient.
 * OWASP API Security Top 10 : A01:2023 - Broken Object Level Authorization.
 */
@Service
public class PatientAccessGuardService {

    private final PatientRepository patientRepository;

    public PatientAccessGuardService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    /**
     * Résout le patient depuis le token JWT.
     * Lance 401 si non authentifié, 404 si patient inconnu.
     */
    public PatientEntity resolve(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié.");
        }
        return patientRepository.findByGlobalPatientNumber(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier patient introuvable."));
    }

    /**
     * Résout le patient et vérifie que l'ID demandé correspond bien au patient authentifié.
     * Lance 403 si tentative d'accès à un autre DPU (IDOR attempt).
     */
    public PatientEntity resolveAndGuard(Authentication authentication, UUID requestedPatientId) {
        PatientEntity patient = resolve(authentication);
        if (requestedPatientId != null && !patient.getId().equals(requestedPatientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Accès refusé : vous ne pouvez pas accéder à ce dossier patient.");
        }
        return patient;
    }
}
