package com.joprelys.backend.patient.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.patient.api.CreatePatientAllergyRequest;
import com.joprelys.backend.patient.api.CreatePatientMedicalHistoryRequest;
import com.joprelys.backend.patient.api.PatientAllergyResponse;
import com.joprelys.backend.patient.api.PatientMedicalHistoryResponse;
import com.joprelys.backend.patient.infrastructure.persistence.PatientAllergyEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientAllergyRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientMedicalHistoryEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientMedicalHistoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class PatientMedicalInfoService {

    private final PatientAllergyRepository patientAllergyRepository;
    private final PatientMedicalHistoryRepository patientMedicalHistoryRepository;
    private final PatientService patientService;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public PatientMedicalInfoService(PatientAllergyRepository patientAllergyRepository,
                                     PatientMedicalHistoryRepository patientMedicalHistoryRepository,
                                     PatientService patientService,
                                     UserAccountRepository userAccountRepository,
                                     AuditService auditService) {
        this.patientAllergyRepository = patientAllergyRepository;
        this.patientMedicalHistoryRepository = patientMedicalHistoryRepository;
        this.patientService = patientService;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PatientAllergyResponse> listAllergies(UUID patientId) {
        // Valide l'accès au patient (lève 403 CONSENT_REQUIRED ou 404 si inexistant)
        patientService.getPatientById(patientId);

        return patientAllergyRepository.findAllByPatientId(patientId).stream()
                .map(PatientAllergyResponse::fromEntity)
                .toList();
    }

    @Transactional
    public PatientAllergyResponse addAllergy(UUID patientId, CreatePatientAllergyRequest request) {
        PatientEntity patient = patientService.getPatientById(patientId);

        PatientAllergyEntity allergy = new PatientAllergyEntity(
                patientId,
                request.substance(),
                request.severity(),
                request.reaction(),
                request.status() != null ? request.status() : "ACTIVE",
                request.discoveredAt(),
                request.comment()
        );

        PatientAllergyEntity saved = patientAllergyRepository.save(allergy);

        UserAccountEntity actor = getCurrentUser();
        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    actor.getOrganizationId(),
                    patientId,
                    "PATIENT_ALLERGY",
                    saved.getId(),
                    "ADD_ALLERGY",
                    "Ajout de l'allergie : " + saved.getSubstance() + " (Gravité: " + saved.getSeverity() + ") pour " + patient.getFullName()
            );
        }

        return PatientAllergyResponse.fromEntity(saved);
    }

    @Transactional
    public PatientAllergyResponse updateAllergy(UUID patientId, UUID allergyId, CreatePatientAllergyRequest request) {
        PatientEntity patient = patientService.getPatientById(patientId);

        PatientAllergyEntity allergy = patientAllergyRepository.findByIdAndPatientId(allergyId, patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Allergie introuvable pour ce patient"));

        allergy.setSubstance(request.substance());
        allergy.setSeverity(request.severity());
        allergy.setReaction(request.reaction());
        allergy.setStatus(request.status() != null ? request.status() : allergy.getStatus());
        allergy.setDiscoveredAt(request.discoveredAt());
        allergy.setComment(request.comment());

        PatientAllergyEntity saved = patientAllergyRepository.save(allergy);

        UserAccountEntity actor = getCurrentUser();
        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    actor.getOrganizationId(),
                    patientId,
                    "PATIENT_ALLERGY",
                    saved.getId(),
                    "UPDATE_ALLERGY",
                    "Mise à jour de l'allergie : " + saved.getSubstance() + " (Statut: " + saved.getStatus() + ") pour " + patient.getFullName()
            );
        }

        return PatientAllergyResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<PatientMedicalHistoryResponse> listMedicalHistory(UUID patientId) {
        patientService.getPatientById(patientId);

        return patientMedicalHistoryRepository.findAllByPatientId(patientId).stream()
                .map(PatientMedicalHistoryResponse::fromEntity)
                .toList();
    }

    @Transactional
    public PatientMedicalHistoryResponse addMedicalHistory(UUID patientId, CreatePatientMedicalHistoryRequest request) {
        PatientEntity patient = patientService.getPatientById(patientId);

        PatientMedicalHistoryEntity history = new PatientMedicalHistoryEntity(
                patientId,
                request.category(),
                request.description(),
                request.onsetDate(),
                request.isOngoing(),
                request.comment()
        );

        PatientMedicalHistoryEntity saved = patientMedicalHistoryRepository.save(history);

        UserAccountEntity actor = getCurrentUser();
        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    actor.getOrganizationId(),
                    patientId,
                    "PATIENT_HISTORY",
                    saved.getId(),
                    "ADD_HISTORY",
                    "Ajout de l'antécédent (" + saved.getCategory() + ") : " + saved.getDescription() + " pour " + patient.getFullName()
            );
        }

        return PatientMedicalHistoryResponse.fromEntity(saved);
    }

    @Transactional
    public PatientMedicalHistoryResponse updateMedicalHistory(UUID patientId, UUID historyId, CreatePatientMedicalHistoryRequest request) {
        PatientEntity patient = patientService.getPatientById(patientId);

        PatientMedicalHistoryEntity history = patientMedicalHistoryRepository.findByIdAndPatientId(historyId, patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Antécédent introuvable pour ce patient"));

        history.setCategory(request.category());
        history.setDescription(request.description());
        history.setOnsetDate(request.onsetDate());
        history.setOngoing(request.isOngoing());
        history.setComment(request.comment());

        PatientMedicalHistoryEntity saved = patientMedicalHistoryRepository.save(history);

        UserAccountEntity actor = getCurrentUser();
        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    actor.getOrganizationId(),
                    patientId,
                    "PATIENT_HISTORY",
                    saved.getId(),
                    "UPDATE_HISTORY",
                    "Mise à jour de l'antécédent : " + saved.getDescription() + " pour " + patient.getFullName()
            );
        }

        return PatientMedicalHistoryResponse.fromEntity(saved);
    }

    private UserAccountEntity getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userAccountRepository.findByEmail(auth.getName().trim().toLowerCase()).orElse(null);
        }
        return null;
    }
}
