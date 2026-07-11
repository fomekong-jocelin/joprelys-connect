package com.joprelys.backend.emergency.application;

import com.joprelys.backend.emergency.api.AddResuscitationLogRequest;
import com.joprelys.backend.emergency.api.CreateEmergencyRequest;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyRepository;
import com.joprelys.backend.emergency.infrastructure.persistence.ResuscitationLogEntity;
import com.joprelys.backend.emergency.medicolegal.application.EmergencyArrivalThirdPartyService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
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

    public EmergencyService(
            EmergencyRepository emergencyRepository,
            PatientRepository patientRepository,
            EmergencyArrivalThirdPartyService arrivalThirdPartyService) {
        this.emergencyRepository = emergencyRepository;
        this.patientRepository = patientRepository;
        this.arrivalThirdPartyService = arrivalThirdPartyService;
    }

    @Transactional
    public EmergencyEntity createEmergency(CreateEmergencyRequest request, UUID createdByUserId) {
        PatientEntity patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> notFound("PATIENT_NOT_FOUND"));

        boolean alreadyInEmergency = emergencyRepository.findByStabilizedAtIsNull().stream()
                .anyMatch(emergency -> emergency.getPatient().getId().equals(request.patientId()));
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

    @Transactional
    public EmergencyEntity stabilizeEmergency(UUID id, String orientation) {
        EmergencyEntity emergency = emergencyRepository.findByIdWithPatientAndLogs(id)
                .orElseThrow(() -> notFound("EMERGENCY_NOT_FOUND"));
        if (emergency.getStabilizedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "EMERGENCY_ALREADY_STABILIZED");
        }
        emergency.setStabilizedAt(Instant.now());
        emergency.setOrientation(normalizeRequired(orientation, "EMERGENCY_ORIENTATION_REQUIRED"));
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
        return emergencyRepository.findByPatientIdWithLogs(patientId);
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
