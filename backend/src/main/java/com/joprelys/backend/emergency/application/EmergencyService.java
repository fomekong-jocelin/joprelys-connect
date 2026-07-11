package com.joprelys.backend.emergency.application;

import com.joprelys.backend.emergency.api.AddResuscitationLogRequest;
import com.joprelys.backend.emergency.api.CreateEmergencyRequest;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyRepository;
import com.joprelys.backend.emergency.infrastructure.persistence.ResuscitationLogEntity;
import com.joprelys.backend.emergency.infrastructure.persistence.ResuscitationLogRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class EmergencyService {

    private final EmergencyRepository emergencyRepository;
    private final ResuscitationLogRepository resuscitationLogRepository;
    private final PatientRepository patientRepository;

    public EmergencyService(
            EmergencyRepository emergencyRepository,
            ResuscitationLogRepository resuscitationLogRepository,
            PatientRepository patientRepository) {
        this.emergencyRepository = emergencyRepository;
        this.resuscitationLogRepository = resuscitationLogRepository;
        this.patientRepository = patientRepository;
    }

    @Transactional
    public EmergencyEntity createEmergency(CreateEmergencyRequest request, UUID createdByUserId) {
        PatientEntity patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable."));

        List<EmergencyEntity> active = emergencyRepository.findByStabilizedAtIsNull();
        boolean alreadyInEmergency = active.stream()
                .anyMatch(e -> e.getPatient().getId().equals(request.patientId()));

        if (alreadyInEmergency) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce patient est déjà enregistré dans les urgences actives.");
        }

        EmergencyEntity emergency = new EmergencyEntity(
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
                createdByUserId
        );

        return emergencyRepository.save(emergency);
    }

    @Transactional
    public ResuscitationLogEntity addResuscitationLog(
            UUID emergencyId,
            AddResuscitationLogRequest request,
            UUID administeredByUserId) {
        EmergencyEntity emergency = emergencyRepository.findByIdWithPatientAndLogs(emergencyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier d'urgence introuvable."));

        if (emergency.getStabilizedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible d'ajouter des soins à un dossier d'urgence déjà stabilisé.");
        }

        ResuscitationLogEntity log = new ResuscitationLogEntity(
                emergency,
                request.actionType(),
                request.description(),
                request.quantity(),
                request.unit(),
                request.administeredAt(),
                administeredByUserId
        );

        emergency.addResuscitationLog(log);
        return log;
    }

    @Transactional
    public EmergencyEntity stabilizeEmergency(UUID id, String orientation) {
        EmergencyEntity emergency = emergencyRepository.findByIdWithPatientAndLogs(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier d'urgence introuvable."));

        if (emergency.getStabilizedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce dossier d'urgence est déjà marqué comme stabilisé.");
        }

        emergency.setStabilizedAt(Instant.now());
        emergency.setOrientation(orientation);
        return emergencyRepository.save(emergency);
    }

    @Transactional(readOnly = true)
    public List<EmergencyEntity> getActiveEmergencies() {
        return emergencyRepository.findByStabilizedAtIsNull();
    }

    @Transactional(readOnly = true)
    public EmergencyEntity getEmergency(UUID id) {
        return emergencyRepository.findByIdWithPatientAndLogs(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier d'urgence introuvable."));
    }

    @Transactional(readOnly = true)
    public List<EmergencyEntity> getPatientEmergencies(UUID patientId) {
        return emergencyRepository.findByPatientIdWithLogs(patientId);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
