package com.joprelys.backend.emergency.application;

import com.joprelys.backend.emergency.api.AddResuscitationLogRequest;
import com.joprelys.backend.emergency.api.CreateEmergencyRequest;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyRepository;
import com.joprelys.backend.emergency.infrastructure.persistence.ResuscitationLogEntity;
import com.joprelys.backend.emergency.medicolegal.application.EmergencyArrivalThirdPartyService;
import com.joprelys.backend.emergency.triage.application.EmergencyTriageAssessmentUseCase;
import com.joprelys.backend.patient.application.PatientDeathService;
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.patient.reconciliation.application.PatientCanonicalResolver;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmergencyService {

    private final EmergencyRepository emergencyRepository;
    private final PatientRepository patientRepository;
    private final EmergencyArrivalThirdPartyService arrivalThirdPartyService;
    private final PatientCanonicalResolver canonicalResolver;
    private final EmergencyTriageAssessmentUseCase triageAssessmentUseCase;
    private final PatientDeathService deathService;

    private static final java.util.Set<String> ORIENTATIONS =
            java.util.Set.of("ADMISSION", "OR_DIRECT", "CONSULTATION", "TRANSFER", "DISCHARGE", "DEATH");
    /** Valeurs historiques envoyées par d'anciens clients. */
    private static final java.util.Map<String, String> LEGACY_ORIENTATIONS =
            java.util.Map.of("HOSPITALIZATION", "ADMISSION", "HOSPITALISATION", "ADMISSION");
    private static final java.util.Set<String> DOWNSTREAM_ORIENTATIONS =
            java.util.Set.of("ADMISSION", "OR_DIRECT", "CONSULTATION");

    public EmergencyService(
            EmergencyRepository emergencyRepository,
            PatientRepository patientRepository,
            EmergencyArrivalThirdPartyService arrivalThirdPartyService,
            PatientCanonicalResolver canonicalResolver,
            EmergencyTriageAssessmentUseCase triageAssessmentUseCase,
            PatientDeathService deathService) {
        this.deathService = deathService;
        this.emergencyRepository = emergencyRepository;
        this.patientRepository = patientRepository;
        this.arrivalThirdPartyService = arrivalThirdPartyService;
        this.canonicalResolver = canonicalResolver;
        this.triageAssessmentUseCase = triageAssessmentUseCase;
    }

    @Transactional
    public EmergencyEntity createEmergency(CreateEmergencyRequest request, UUID createdByUserId) {
        PatientEntity patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> notFound("PATIENT_NOT_FOUND"));
        if (patient.getIdentityStatus() == PatientIdentityStatus.MERGED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PATIENT_ALIAS_READ_ONLY");
        }
        PatientDeathService.rejectIfDeceased(canonicalResolver.resolve(patient.getId()).canonicalPatient());

        var patientContext = canonicalResolver.resolve(patient.getId());
        boolean alreadyInEmergency = emergencyRepository.findByStabilizedAtIsNull().stream()
                .map(EmergencyEntity::getPatient)
                .map(PatientEntity::getId)
                .anyMatch(patientContext.contributingPatientIds()::contains);
        if (alreadyInEmergency) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "EMERGENCY_ALREADY_ACTIVE");
        }

        EmergencyEntity emergency = emergencyRepository.save(new EmergencyEntity(
                patient,
                null,
                request.arrivalMode(),
                request.triageLevel(),
                request.hemodynamicStatus(),
                request.chiefComplaint(),
                request.initialBpSystolic(),
                request.initialBpDiastolic(),
                request.initialHr(),
                request.initialTemp(),
                normalize(request.thirdPartyName()),
                normalize(request.thirdPartyPhone()),
                normalize(request.thirdPartyRelationship()),
                normalize(request.thirdPartyIdDocument()),
                normalize(request.thirdPartyCircumstances()),
                request.contactConsentGranted(),
                createdByUserId));

        triageAssessmentUseCase.recordInitial(
                emergency,
                request.toInitialTriageCommand(),
                createdByUserId);
        arrivalThirdPartyService.capture(emergency, createdByUserId);
        return emergency;
    }

    @Transactional
    public ResuscitationLogEntity addResuscitationLog(
            UUID emergencyId,
            AddResuscitationLogRequest request,
            UUID administeredByUserId) {
        EmergencyEntity emergency = emergencyRepository.findByIdWithPatientAndLogs(emergencyId)
                .orElseThrow(() -> notFound("EMERGENCY_NOT_FOUND"));

        if (emergency.getStabilizedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "EMERGENCY_ALREADY_STABILIZED");
        }

        ResuscitationLogEntity log = new ResuscitationLogEntity(
                emergency,
                request.actionType(),
                request.description(),
                request.quantity(),
                request.unit(),
                request.administeredAt(),
                administeredByUserId);
        emergency.addResuscitationLog(log);
        emergencyRepository.save(emergency);
        return log;
    }

    /**
     * Décision d'orientation (ADR-20261017 D-06). Hospitalisation, bloc et consultation laissent le patient
     * sous la responsabilité des urgences jusqu'à son acceptation par l'aval ; sortie et transfert externe
     * clôturent l'épisode ; le décès déclenche sa déclaration.
     */
    @Transactional
    public EmergencyEntity stabilizeEmergency(UUID id, String orientation, UUID actorId, UUID actorOrganizationId) {
        EmergencyEntity emergency = emergencyRepository.findByIdWithPatientAndLogs(id)
                .orElseThrow(() -> notFound("EMERGENCY_NOT_FOUND"));
        if (!emergency.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "EMERGENCY_ALREADY_STABILIZED");
        }
        String decided = normalizeRequired(orientation, "EMERGENCY_ORIENTATION_REQUIRED").toUpperCase(java.util.Locale.ROOT);
        decided = LEGACY_ORIENTATIONS.getOrDefault(decided, decided);
        if (!ORIENTATIONS.contains(decided)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EMERGENCY_ORIENTATION_INVALID");
        }
        if (PatientDeathService.DEATH_ORIENTATION.equals(decided)) {
            deathService.declareDeath(emergency.getPatient().getId(),
                    new PatientDeathService.DeathDeclaration(Instant.now(), "Urgences", emergency.getChiefComplaint()),
                    actorId, actorOrganizationId);
            return emergency;
        }
        Instant now = Instant.now();
        emergency.decideOrientation(decided, actorId, now);
        if (!DOWNSTREAM_ORIENTATIONS.contains(decided)) {
            emergency.closeEpisode(actorId, now);
        }
        return emergencyRepository.save(emergency);
    }

    @Transactional(readOnly = true)
    public List<EmergencyEntity> getActiveEmergencies() {
        return emergencyRepository.findByStabilizedAtIsNull();
    }

    @Transactional(readOnly = true)
    public EmergencyEntity getEmergency(UUID id) {
        return emergencyRepository.findByIdWithPatientAndLogs(id)
                .orElseThrow(() -> notFound("EMERGENCY_NOT_FOUND"));
    }

    @Transactional(readOnly = true)
    public List<EmergencyEntity> getPatientEmergencies(UUID patientId) {
        var context = canonicalResolver.resolve(patientId);
        return emergencyRepository.findByPatientIdsWithLogs(context.contributingPatientIds());
    }

    private ResponseStatusException notFound(String code) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, code);
    }

    private static String normalizeRequired(String value, String code) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, code);
        }
        return normalized;
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}