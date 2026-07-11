package com.joprelys.backend.patient.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.patient.api.RegularizeProvisionalPatientRequest;
import com.joprelys.backend.patient.domain.IdentityConfidenceLevel;
import com.joprelys.backend.patient.domain.IdentityVerificationStatus;
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientIdentityDeclarationEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientIdentityDeclarationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientIdentityStatusHistoryEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientIdentityStatusHistoryRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProvisionalPatientRegularizationService {

    private final PatientRepository patientRepository;
    private final PatientIdentityDeclarationRepository declarationRepository;
    private final PatientIdentityStatusHistoryRepository statusHistoryRepository;
    private final UserAccountRepository userAccountRepository;
    private final PatientSimilarityService similarityService;
    private final AuditService auditService;

    public ProvisionalPatientRegularizationService(
            PatientRepository patientRepository,
            PatientIdentityDeclarationRepository declarationRepository,
            PatientIdentityStatusHistoryRepository statusHistoryRepository,
            UserAccountRepository userAccountRepository,
            PatientSimilarityService similarityService,
            AuditService auditService) {
        this.patientRepository = patientRepository;
        this.declarationRepository = declarationRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.userAccountRepository = userAccountRepository;
        this.similarityService = similarityService;
        this.auditService = auditService;
    }

    @Transactional
    public PatientEntity regularize(UUID patientId, RegularizeProvisionalPatientRequest request) {
        UserAccountEntity actor = requireCurrentActor();
        PatientEntity patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable"));

        if (actor.getOrganizationId() == null || !actor.getOrganizationId().equals(patient.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable");
        }

        PatientIdentityStatus previousStatus = patient.getIdentityStatus();
        if (previousStatus != PatientIdentityStatus.PROVISIONAL_URGENCY
                && previousStatus != PatientIdentityStatus.DECLARED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Seul un dossier provisoire ou déclaré peut être régularisé");
        }

        patient.setFullName(request.fullName());
        patient.setGender(request.gender());
        patient.setBirthDate(request.birthDate());
        patient.setPhone(normalize(request.phone()));
        patient.setCity(request.city());
        patient.setDistrict(normalize(request.district()));
        patient.setAddress(normalize(request.address()));
        patient.setEmail(normalize(request.email()));
        patient.setEmergencyContactName(normalize(request.emergencyContactName()));
        patient.setEmergencyContactPhone(normalize(request.emergencyContactPhone()));
        patient.transitionIdentityStatus(PatientIdentityStatus.VERIFIED);

        PatientEntity saved = patientRepository.saveAndFlush(patient);
        declarationRepository.saveAll(buildVerifiedDeclarations(saved, actor, request));
        statusHistoryRepository.save(new PatientIdentityStatusHistoryEntity(
                actor.getOrganizationId(),
                saved.getId(),
                previousStatus,
                PatientIdentityStatus.VERIFIED,
                request.reason().trim(),
                actor.getId()));

        similarityService.checkForDuplicates(saved);
        auditService.logSuccess(
                actor.getId(),
                actor.getOrganizationId(),
                saved.getId(),
                "PATIENT_IDENTITY",
                saved.getId(),
                "REGULARIZE_PROVISIONAL_PATIENT",
                "Régularisation du dossier " + saved.getTemporaryPatientNumber()
                        + " en identité vérifiée : " + saved.getFullName());
        return saved;
    }

    private List<PatientIdentityDeclarationEntity> buildVerifiedDeclarations(
            PatientEntity patient,
            UserAccountEntity actor,
            RegularizeProvisionalPatientRequest request) {
        List<PatientIdentityDeclarationEntity> declarations = new ArrayList<>();
        Instant now = Instant.now();
        addDeclaration(declarations, patient, actor, request, "fullName", request.fullName(), now);
        addDeclaration(declarations, patient, actor, request, "gender", request.gender(), now);
        addDeclaration(declarations, patient, actor, request, "birthDate", request.birthDate().toString(), now);
        addDeclaration(declarations, patient, actor, request, "city", request.city(), now);
        addDeclaration(declarations, patient, actor, request, "phone", request.phone(), now);
        addDeclaration(declarations, patient, actor, request, "email", request.email(), now);
        return declarations;
    }

    private void addDeclaration(
            List<PatientIdentityDeclarationEntity> declarations,
            PatientEntity patient,
            UserAccountEntity actor,
            RegularizeProvisionalPatientRequest request,
            String field,
            String value,
            Instant declaredAt) {
        String normalized = normalize(value);
        if (normalized == null) {
            return;
        }
        declarations.add(new PatientIdentityDeclarationEntity(
                actor.getOrganizationId(),
                patient.getId(),
                field,
                normalized,
                request.sourceType(),
                request.sourceDetails().trim(),
                IdentityConfidenceLevel.VERIFIED,
                IdentityVerificationStatus.VERIFIED,
                actor.getId(),
                declaredAt));
    }

    private UserAccountEntity requireCurrentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
        }
        return userAccountRepository.findByEmail(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non trouvé"));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
