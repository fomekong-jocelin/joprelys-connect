package com.joprelys.backend.patient.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.patient.api.CreateProvisionalPatientRequest;
import com.joprelys.backend.patient.api.IdentityDeclarationRequest;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
public class ProvisionalPatientService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_NUMBER_ATTEMPTS = 10;

    private final PatientRepository patientRepository;
    private final PatientIdentityDeclarationRepository declarationRepository;
    private final PatientIdentityStatusHistoryRepository statusHistoryRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public ProvisionalPatientService(
            PatientRepository patientRepository,
            PatientIdentityDeclarationRepository declarationRepository,
            PatientIdentityStatusHistoryRepository statusHistoryRepository,
            UserAccountRepository userAccountRepository,
            AuditService auditService) {
        this.patientRepository = patientRepository;
        this.declarationRepository = declarationRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    @Transactional
    public CreationResult create(CreateProvisionalPatientRequest request) {
        UserAccountEntity actor = requireCurrentActor();
        if (actor.getOrganizationId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Un établissement est requis");
        }

        IdentityConfidenceLevel confidenceLevel = request.confidenceLevel() == null
                ? IdentityConfidenceLevel.NONE
                : request.confidenceLevel();
        if (confidenceLevel == IdentityConfidenceLevel.VERIFIED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Un dossier URG-TEMP ne peut pas être déclaré vérifié à la création");
        }

        ProvisionalNumbers numbers = generateUniqueNumbers();
        PatientEntity patient = PatientEntity.provisionalEmergency(
                numbers.globalPatientNumber(),
                numbers.localPatientNumber(),
                numbers.temporaryPatientNumber(),
                request.apparentGender(),
                request.estimatedAgeRange(),
                request.physicalDescription(),
                request.foundAt() == null ? Instant.now() : request.foundAt(),
                request.foundLocation(),
                confidenceLevel);
        patient.setOrganizationId(actor.getOrganizationId());

        PatientEntity savedPatient = patientRepository.saveAndFlush(patient);
        List<PatientIdentityDeclarationEntity> declarations = saveDeclarations(
                request.identityDeclarations(), savedPatient, actor);

        statusHistoryRepository.save(new PatientIdentityStatusHistoryEntity(
                actor.getOrganizationId(),
                savedPatient.getId(),
                null,
                PatientIdentityStatus.PROVISIONAL_URGENCY,
                "Création du dossier provisoire d'urgence",
                actor.getId()));

        auditService.logSuccess(
                actor.getId(),
                actor.getOrganizationId(),
                savedPatient.getId(),
                "PATIENT_IDENTITY",
                savedPatient.getId(),
                "CREATE_PROVISIONAL_EMERGENCY_PATIENT",
                "Création du dossier " + numbers.temporaryPatientNumber());

        return new CreationResult(savedPatient, declarations);
    }

    @Transactional(readOnly = true)
    public List<PatientIdentityDeclarationEntity> getDeclarations(UUID patientId) {
        return declarationRepository.findAllByPatientIdOrderByDeclaredAtAsc(patientId);
    }

    private ProvisionalNumbers generateUniqueNumbers() {
        String date = LocalDate.now().format(DATE_FORMATTER);
        for (int attempt = 0; attempt < MAX_NUMBER_ATTEMPTS; attempt++) {
            String random = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
            String temporary = "URG-TEMP-" + date + "-" + random.substring(0, 6);
            String global = "DPU-JOP-" + date + "-" + random.substring(0, 12);
            String local = "PAT-" + date + "-" + random.substring(0, 12);
            if (!patientRepository.existsByTemporaryPatientNumber(temporary)
                    && !patientRepository.existsByGlobalPatientNumber(global)) {
                return new ProvisionalNumbers(global, local, temporary);
            }
        }
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Impossible de générer un identifiant URG-TEMP unique");
    }

    private List<PatientIdentityDeclarationEntity> saveDeclarations(
            List<IdentityDeclarationRequest> requests,
            PatientEntity patient,
            UserAccountEntity actor) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        List<PatientIdentityDeclarationEntity> declarations = new ArrayList<>();
        for (IdentityDeclarationRequest request : requests) {
            if (request.confidenceLevel() == IdentityConfidenceLevel.VERIFIED) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Une déclaration initiale URG-TEMP ne peut pas être marquée vérifiée");
            }
            declarations.add(new PatientIdentityDeclarationEntity(
                    actor.getOrganizationId(),
                    patient.getId(),
                    request.fieldName().trim(),
                    request.value().trim(),
                    request.sourceType(),
                    normalize(request.sourceDetails()),
                    request.confidenceLevel(),
                    IdentityVerificationStatus.DECLARED,
                    actor.getId(),
                    request.declaredAt() == null ? Instant.now() : request.declaredAt()));
        }
        return declarationRepository.saveAll(declarations);
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

    private record ProvisionalNumbers(
            String globalPatientNumber,
            String localPatientNumber,
            String temporaryPatientNumber) {
    }

    public record CreationResult(
            PatientEntity patient,
            List<PatientIdentityDeclarationEntity> declarations) {
    }
}
