package com.joprelys.backend.patient.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.patient.api.CreatePatientAllergyRequest;
import com.joprelys.backend.patient.api.CreatePatientMedicalHistoryRequest;
import com.joprelys.backend.patient.api.CreatePatientVaccinationRequest;
import com.joprelys.backend.patient.api.PatientAllergyResponse;
import com.joprelys.backend.patient.api.PatientMedicalHistoryResponse;
import com.joprelys.backend.patient.api.PatientVaccinationResponse;
import com.joprelys.backend.patient.infrastructure.persistence.PatientAllergyEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientAllergyRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientMedicalHistoryEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientMedicalHistoryRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientVaccinationEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientVaccinationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PatientMedicalInfoService {

    private final PatientAllergyRepository patientAllergyRepository;
    private final PatientMedicalHistoryRepository patientMedicalHistoryRepository;
    private final PatientVaccinationRepository patientVaccinationRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;
    private final PatientRepository patientRepository;

    public PatientMedicalInfoService(
            PatientAllergyRepository patientAllergyRepository,
            PatientMedicalHistoryRepository patientMedicalHistoryRepository,
            PatientVaccinationRepository patientVaccinationRepository,
            UserAccountRepository userAccountRepository,
            AuditService auditService,
            PatientRepository patientRepository) {
        this.patientAllergyRepository = patientAllergyRepository;
        this.patientMedicalHistoryRepository = patientMedicalHistoryRepository;
        this.patientVaccinationRepository = patientVaccinationRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
        this.patientRepository = patientRepository;
    }

    @Transactional(readOnly = true)
    public List<PatientAllergyResponse> listAllergies(UUID patientId) {
        requirePatient(patientId);
        return patientAllergyRepository.findAllByPatientIdAndDeletedAtIsNull(patientId).stream()
                .map(PatientAllergyResponse::fromEntity)
                .toList();
    }

    @Transactional
    public PatientAllergyResponse addAllergy(UUID patientId, CreatePatientAllergyRequest request) {
        PatientEntity patient = requirePatient(patientId);
        PatientAllergyEntity allergy = new PatientAllergyEntity(
                patientId,
                request.substance(),
                request.severity(),
                request.reaction(),
                request.status() != null ? request.status() : "ACTIVE",
                request.discoveredAt(),
                request.comment());

        PatientAllergyEntity saved = patientAllergyRepository.save(allergy);
        UserAccountEntity actor = getCurrentUser();
        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(), actor.getOrganizationId(), patientId,
                    "PATIENT_ALLERGY", saved.getId(), "ADD_ALLERGY",
                    "Ajout de l'allergie : " + saved.getSubstance()
                            + " (Gravité: " + saved.getSeverity() + ") pour " + patient.getFullName());
        }
        syncAllergies(patientId);
        return PatientAllergyResponse.fromEntity(saved);
    }

    @Transactional
    public PatientAllergyResponse updateAllergy(
            UUID patientId,
            UUID allergyId,
            CreatePatientAllergyRequest request) {
        PatientEntity patient = requirePatient(patientId);
        PatientAllergyEntity allergy = patientAllergyRepository
                .findByIdAndPatientIdAndDeletedAtIsNull(allergyId, patientId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Allergie introuvable pour ce patient"));

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
                    actor.getId(), actor.getOrganizationId(), patientId,
                    "PATIENT_ALLERGY", saved.getId(), "UPDATE_ALLERGY",
                    "Mise à jour de l'allergie : " + saved.getSubstance()
                            + " (Statut: " + saved.getStatus() + ") pour " + patient.getFullName());
        }
        syncAllergies(patientId);
        return PatientAllergyResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteAllergy(UUID patientId, UUID allergyId) {
        PatientEntity patient = requirePatient(patientId);
        PatientAllergyEntity allergy = patientAllergyRepository
                .findByIdAndPatientIdAndDeletedAtIsNull(allergyId, patientId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Allergie introuvable pour ce patient"));

        UserAccountEntity actor = getCurrentUser();
        allergy.setDeletedAt(java.time.Instant.now());
        if (actor != null) allergy.setDeletedBy(actor.getId());
        patientAllergyRepository.save(allergy);

        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(), actor.getOrganizationId(), patientId,
                    "PATIENT_ALLERGY", allergy.getId(), "DELETE_ALLERGY",
                    "Suppression logique de l'allergie : " + allergy.getSubstance()
                            + " pour " + patient.getFullName());
        }
        syncAllergies(patientId);
    }

    @Transactional(readOnly = true)
    public List<PatientMedicalHistoryResponse> listMedicalHistory(UUID patientId) {
        requirePatient(patientId);
        return patientMedicalHistoryRepository.findAllByPatientIdAndDeletedAtIsNull(patientId).stream()
                .map(PatientMedicalHistoryResponse::fromEntity)
                .toList();
    }

    @Transactional
    public PatientMedicalHistoryResponse addMedicalHistory(
            UUID patientId,
            CreatePatientMedicalHistoryRequest request) {
        PatientEntity patient = requirePatient(patientId);
        boolean important = request.important() != null ? request.important() : false;
        PatientMedicalHistoryEntity history = new PatientMedicalHistoryEntity(
                patientId,
                request.category(),
                request.description(),
                request.onsetDate(),
                request.isOngoing(),
                request.comment(),
                important);

        PatientMedicalHistoryEntity saved = patientMedicalHistoryRepository.save(history);
        UserAccountEntity actor = getCurrentUser();
        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(), actor.getOrganizationId(), patientId,
                    "PATIENT_HISTORY", saved.getId(), "ADD_HISTORY",
                    "Ajout de l'antécédent (" + saved.getCategory() + ") : "
                            + saved.getDescription() + " pour " + patient.getFullName());
        }
        syncMedicalHistory(patientId);
        return PatientMedicalHistoryResponse.fromEntity(saved);
    }

    @Transactional
    public PatientMedicalHistoryResponse updateMedicalHistory(
            UUID patientId,
            UUID historyId,
            CreatePatientMedicalHistoryRequest request) {
        PatientEntity patient = requirePatient(patientId);
        PatientMedicalHistoryEntity history = patientMedicalHistoryRepository
                .findByIdAndPatientIdAndDeletedAtIsNull(historyId, patientId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Antécédent introuvable pour ce patient"));

        history.setCategory(request.category());
        history.setDescription(request.description());
        history.setOnsetDate(request.onsetDate());
        history.setOngoing(request.isOngoing());
        history.setComment(request.comment());
        if (request.important() != null) history.setImportant(request.important());

        PatientMedicalHistoryEntity saved = patientMedicalHistoryRepository.save(history);
        UserAccountEntity actor = getCurrentUser();
        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(), actor.getOrganizationId(), patientId,
                    "PATIENT_HISTORY", saved.getId(), "UPDATE_HISTORY",
                    "Mise à jour de l'antécédent : " + saved.getDescription()
                            + " pour " + patient.getFullName());
        }
        syncMedicalHistory(patientId);
        return PatientMedicalHistoryResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteMedicalHistory(UUID patientId, UUID historyId) {
        PatientEntity patient = requirePatient(patientId);
        PatientMedicalHistoryEntity history = patientMedicalHistoryRepository
                .findByIdAndPatientIdAndDeletedAtIsNull(historyId, patientId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Antécédent introuvable pour ce patient"));

        UserAccountEntity actor = getCurrentUser();
        history.setDeletedAt(java.time.Instant.now());
        if (actor != null) history.setDeletedBy(actor.getId());
        patientMedicalHistoryRepository.save(history);

        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(), actor.getOrganizationId(), patientId,
                    "PATIENT_HISTORY", history.getId(), "DELETE_HISTORY",
                    "Suppression logique de l'antécédent : " + history.getDescription()
                            + " pour " + patient.getFullName());
        }
        syncMedicalHistory(patientId);
    }

    @Transactional(readOnly = true)
    public List<PatientVaccinationResponse> listVaccinations(UUID patientId) {
        requirePatient(patientId);
        return patientVaccinationRepository.findAllByPatientId(patientId).stream()
                .map(PatientVaccinationResponse::fromEntity)
                .toList();
    }

    @Transactional
    public PatientVaccinationResponse addVaccination(
            UUID patientId,
            CreatePatientVaccinationRequest request) {
        PatientEntity patient = requirePatient(patientId);
        PatientVaccinationEntity vaccination = new PatientVaccinationEntity(
                patientId,
                request.vaccineName(),
                request.batchNumber(),
                request.administeredAt(),
                request.administeredBy(),
                request.notes(),
                request.nextDoseAt());

        PatientVaccinationEntity saved = patientVaccinationRepository.save(vaccination);
        UserAccountEntity actor = getCurrentUser();
        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(), actor.getOrganizationId(), patientId,
                    "PATIENT_VACCINATION", saved.getId(), "ADD_VACCINATION",
                    "Ajout de la vaccination : " + saved.getVaccineName()
                            + " (Lot: " + saved.getBatchNumber() + ") pour " + patient.getFullName());
        }
        return PatientVaccinationResponse.fromEntity(saved);
    }

    @Transactional
    public PatientVaccinationResponse updateVaccination(
            UUID patientId,
            UUID vaccinationId,
            CreatePatientVaccinationRequest request) {
        PatientEntity patient = requirePatient(patientId);
        PatientVaccinationEntity vaccination = patientVaccinationRepository
                .findByIdAndPatientId(vaccinationId, patientId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Vaccination introuvable pour ce patient"));

        vaccination.setVaccineName(request.vaccineName());
        vaccination.setBatchNumber(request.batchNumber());
        vaccination.setAdministeredAt(request.administeredAt());
        vaccination.setAdministeredBy(request.administeredBy());
        vaccination.setNotes(request.notes());
        vaccination.setNextDoseAt(request.nextDoseAt());

        PatientVaccinationEntity saved = patientVaccinationRepository.save(vaccination);
        UserAccountEntity actor = getCurrentUser();
        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(), actor.getOrganizationId(), patientId,
                    "PATIENT_VACCINATION", saved.getId(), "UPDATE_VACCINATION",
                    "Mise à jour de la vaccination : " + saved.getVaccineName()
                            + " pour " + patient.getFullName());
        }
        return PatientVaccinationResponse.fromEntity(saved);
    }

    private void syncAllergies(UUID patientId) {
        List<PatientAllergyEntity> activeAllergies = patientAllergyRepository
                .findAllByPatientIdAndDeletedAtIsNull(patientId);
        String allergiesStr = activeAllergies.stream()
                .filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus()))
                .map(PatientAllergyEntity::getSubstance)
                .collect(java.util.stream.Collectors.joining(", "));

        patientRepository.findByIdGlobally(patientId).ifPresent(patient -> {
            patient.setAllergies(allergiesStr);
            patientRepository.save(patient);
        });
    }

    private void syncMedicalHistory(UUID patientId) {
        List<PatientMedicalHistoryEntity> activeHistories = patientMedicalHistoryRepository
                .findAllByPatientIdAndDeletedAtIsNull(patientId);
        String historyStr = activeHistories.stream()
                .filter(h -> h.isOngoing() || h.isImportant())
                .map(h -> {
                    String prefix = h.isImportant() ? "[⚠️ Important] " : "";
                    String suffix = h.isOngoing() ? " (En cours)" : "";
                    return prefix + h.getDescription() + suffix;
                })
                .collect(java.util.stream.Collectors.joining(", "));

        patientRepository.findByIdGlobally(patientId).ifPresent(patient -> {
            patient.setMedicalHistory(historyStr);
            patientRepository.save(patient);
        });
    }

    /** Access is validated by the controller using the precise allergies/history scope. */
    private PatientEntity requirePatient(UUID patientId) {
        return patientRepository.findByIdGlobally(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient non trouvé"));
    }

    private UserAccountEntity getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userAccountRepository.findByEmail(auth.getName().trim().toLowerCase()).orElse(null);
        }
        return null;
    }
}
