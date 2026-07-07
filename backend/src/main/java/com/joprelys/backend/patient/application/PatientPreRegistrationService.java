package com.joprelys.backend.patient.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.patient.api.CreatePatientRequest;
import com.joprelys.backend.patient.api.PatientPreRegistrationRequest;
import com.joprelys.backend.patient.api.PatientPreRegistrationResponse;
import com.joprelys.backend.patient.api.PreRegistrationValidationRequest;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientPreRegistrationEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientPreRegistrationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PreRegistrationStatus;
import com.joprelys.backend.auth.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PatientPreRegistrationService {

    private final PatientPreRegistrationRepository preRegistrationRepository;
    private final PatientRepository patientRepository;
    private final PatientService patientService;
    private final MedicalCaptchaService captchaService;
    private final PatientSimilarityService similarityService;
    private final AuditService auditService;
    private final UserAccountRepository userAccountRepository;

    public PatientPreRegistrationService(
            PatientPreRegistrationRepository preRegistrationRepository,
            PatientRepository patientRepository,
            PatientService patientService,
            MedicalCaptchaService captchaService,
            PatientSimilarityService similarityService,
            AuditService auditService,
            UserAccountRepository userAccountRepository) {
        this.preRegistrationRepository = preRegistrationRepository;
        this.patientRepository = patientRepository;
        this.patientService = patientService;
        this.captchaService = captchaService;
        this.similarityService = similarityService;
        this.auditService = auditService;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public PatientPreRegistrationEntity submitPreRegistration(PatientPreRegistrationRequest request) {
        boolean isCaptchaValid = captchaService.validateCaptcha(request.captchaId(), request.captchaAnswer());
        if (!isCaptchaValid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Réponse au captcha incorrecte ou expirée");
        }

        PatientPreRegistrationEntity entity = new PatientPreRegistrationEntity(
                request.organizationId(),
                request.firstName().trim(),
                request.lastName().trim(),
                request.gender().trim().toUpperCase(),
                request.birthDate(),
                request.bloodGroup() != null ? request.bloodGroup().trim().toUpperCase() : null,
                request.phone() != null ? request.phone().trim() : null,
                request.email() != null ? request.email().trim().toLowerCase() : null,
                request.address() != null ? request.address().trim() : null,
                request.emergencyContactName() != null ? request.emergencyContactName().trim() : null,
                request.emergencyContactPhone() != null ? request.emergencyContactPhone().trim() : null,
                request.emergencyContactRelation() != null ? request.emergencyContactRelation().trim().toUpperCase() : null
        );

        return preRegistrationRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public Page<PatientPreRegistrationResponse> getPendingPreRegistrations(Pageable pageable) {
        return preRegistrationRepository.findAllByStatus(PreRegistrationStatus.AWAITING_VALIDATION, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public PatientPreRegistrationResponse getPreRegistrationById(UUID id) {
        PatientPreRegistrationEntity entity = preRegistrationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pré-enregistrement non trouvé"));
        return mapToResponse(entity);
    }

    @Transactional
    public UUID validatePreRegistration(UUID id, PreRegistrationValidationRequest request, UUID actorId) {
        PatientPreRegistrationEntity preRegistration = preRegistrationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pré-enregistrement non trouvé"));

        if (preRegistration.getStatus() != PreRegistrationStatus.AWAITING_VALIDATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce pré-enregistrement a déjà été traité");
        }

        UUID patientId;
        UserAccountEntity actor = userAccountRepository.findById(actorId).orElse(null);
        UUID actorOrgId = actor != null ? actor.getOrganizationId() : null;

        if (request.reconcileWithPatientId() != null) {
            // Cas A : Réconciliation (fusion/liaison avec un patient existant)
            PatientEntity existing = patientRepository.findById(request.reconcileWithPatientId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient existant non trouvé"));

            // Mise à jour des coordonnées
            existing.setFullName(request.lastName().trim().toUpperCase() + " " + request.firstName().trim());
            existing.setGender(request.gender().trim().toUpperCase());
            existing.setBirthDate(request.birthDate());
            if (request.phone() != null) existing.setPhone(request.phone().trim());
            if (request.email() != null) existing.setEmail(request.email().trim().toLowerCase());
            if (request.address() != null) existing.setAddress(request.address().trim());
            if (request.emergencyContactName() != null) existing.setEmergencyContactName(request.emergencyContactName().trim());
            if (request.emergencyContactPhone() != null) existing.setEmergencyContactPhone(request.emergencyContactPhone().trim());
            if (request.bloodGroup() != null) existing.setBloodGroup(request.bloodGroup().trim().toUpperCase());

            patientRepository.save(existing);
            patientId = existing.getId();

            if (actor != null) {
                auditService.logSuccess(
                        actorId,
                        actorOrgId,
                        patientId,
                        "Patient",
                        patientId,
                        "PATIENT_PRE_REGISTRATION_RECONCILED",
                        "Pré-enregistrement " + id + " réconcilié avec le patient existant ID " + patientId
                );
            }
        } else {
            // Cas B : Création d'un nouveau patient officiel
            String fullName = request.lastName().trim().toUpperCase() + " " + request.firstName().trim();
            CreatePatientRequest createRequest = new CreatePatientRequest(
                    fullName,
                    request.gender().trim().toUpperCase(),
                    request.birthDate(),
                    request.phone() != null ? request.phone().trim() : null,
                    "Non spécifiée", // City obligatoire dans CreatePatientRequest, fallback
                    null, // District
                    request.address() != null ? request.address().trim() : null,
                    request.emergencyContactName() != null ? request.emergencyContactName().trim() : null,
                    request.emergencyContactPhone() != null ? request.emergencyContactPhone().trim() : null,
                    null, // Allergies initiales en texte
                    null, // Antécédents initiaux en texte
                    request.bloodGroup() != null ? request.bloodGroup().trim().toUpperCase() : null,
                    request.email() != null ? request.email().trim().toLowerCase() : null
            );

            PatientEntity newPatient = patientService.createPatient(createRequest);
            patientId = newPatient.getId();

            if (actor != null) {
                auditService.logSuccess(
                        actorId,
                        actorOrgId,
                        patientId,
                        "Patient",
                        patientId,
                        "PATIENT_PRE_REGISTRATION_VALIDATED",
                        "Pré-enregistrement " + id + " validé et nouveau patient créé ID " + patientId
                );
            }
        }

        preRegistration.setStatus(PreRegistrationStatus.VALIDATED);
        preRegistration.setValidatedAt(Instant.now());
        preRegistration.setValidatedBy(actorId);
        preRegistrationRepository.save(preRegistration);

        return patientId;
    }

    @Transactional
    public void rejectPreRegistration(UUID id, UUID actorId) {
        PatientPreRegistrationEntity preRegistration = preRegistrationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pré-enregistrement non trouvé"));

        if (preRegistration.getStatus() != PreRegistrationStatus.AWAITING_VALIDATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce pré-enregistrement a déjà été traité");
        }

        preRegistration.setStatus(PreRegistrationStatus.REJECTED);
        preRegistration.setValidatedAt(Instant.now());
        preRegistration.setValidatedBy(actorId);
        preRegistrationRepository.save(preRegistration);

        UserAccountEntity actor = userAccountRepository.findById(actorId).orElse(null);
        UUID actorOrgId = actor != null ? actor.getOrganizationId() : null;

        if (actor != null) {
            auditService.logSuccess(
                    actorId,
                    actorOrgId,
                    null,
                    "PatientPreRegistration",
                    id,
                    "PATIENT_PRE_REGISTRATION_REJECTED",
                    "Pré-enregistrement " + id + " rejeté par l'agent"
            );
        }
    }

    @Scheduled(cron = "0 0 2 * * ?") // Tous les jours à 2h du matin
    @Transactional
    public void cleanupOldPreRegistrations() {
        Instant limit = Instant.now().minusSeconds(24 * 3600); // 24 heures
        preRegistrationRepository.deleteByStatusAndCreatedAtBefore(PreRegistrationStatus.AWAITING_VALIDATION, limit);
    }

    private PatientPreRegistrationResponse mapToResponse(PatientPreRegistrationEntity entity) {
        Double bestScore = null;
        UUID similarPatientId = null;
        String similarPatientName = null;

        String fullName = entity.getLastName().trim().toUpperCase() + " " + entity.getFirstName().trim();
        List<PatientEntity> matches = patientRepository.findByBirthDate(entity.getBirthDate());

        for (PatientEntity match : matches) {
            if ("ACTIVE".equals(match.getStatus())) {
                double score = similarityService.getSimilarityScore(fullName, match.getFullName());
                if (score >= 70.0) {
                    if (bestScore == null || score > bestScore) {
                        bestScore = score;
                        similarPatientId = match.getId();
                        similarPatientName = match.getFullName();
                    }
                }
            }
        }

        return new PatientPreRegistrationResponse(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getGender(),
                entity.getBirthDate(),
                entity.getBloodGroup(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getAddress(),
                entity.getEmergencyContactName(),
                entity.getEmergencyContactPhone(),
                entity.getEmergencyContactRelation(),
                entity.getStatus(),
                entity.getCreatedAt(),
                bestScore,
                similarPatientId,
                similarPatientName
        );
    }
}
