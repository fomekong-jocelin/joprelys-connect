package com.joprelys.backend.emergency.application;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.emergency.api.CreateProvisionalEmergencyAdmissionRequest;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyAdmissionRequestEntity;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyAdmissionRequestRepository;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyRepository;
import com.joprelys.backend.patient.application.ProvisionalPatientService;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProvisionalEmergencyAdmissionService {

    private final EmergencyAdmissionRequestRepository admissionRequestRepository;
    private final EmergencyRepository emergencyRepository;
    private final ProvisionalPatientService provisionalPatientService;
    private final EmergencyService emergencyService;
    private final UserAccountRepository userAccountRepository;
    private final TransactionTemplate transactionTemplate;

    public ProvisionalEmergencyAdmissionService(
            EmergencyAdmissionRequestRepository admissionRequestRepository,
            EmergencyRepository emergencyRepository,
            ProvisionalPatientService provisionalPatientService,
            EmergencyService emergencyService,
            UserAccountRepository userAccountRepository,
            PlatformTransactionManager transactionManager) {
        this.admissionRequestRepository = admissionRequestRepository;
        this.emergencyRepository = emergencyRepository;
        this.provisionalPatientService = provisionalPatientService;
        this.emergencyService = emergencyService;
        this.userAccountRepository = userAccountRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public EmergencyEntity create(CreateProvisionalEmergencyAdmissionRequest request, UUID userId) {
        try {
            return executeCreation(request, userId);
        } catch (DataIntegrityViolationException conflict) {
            EmergencyEntity existing = transactionTemplate.execute(status -> findCompleted(request.requestId()));
            if (existing != null) {
                return existing;
            }
            throw conflict;
        }
    }

    private EmergencyEntity executeCreation(
            CreateProvisionalEmergencyAdmissionRequest request,
            UUID userId) {
        EmergencyEntity result = transactionTemplate.execute(status -> {
            EmergencyEntity existing = findCompleted(request.requestId());
            if (existing != null) {
                return existing;
            }

            var actor = userAccountRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "AUTHENTICATION_REQUIRED"));
            if (actor.getOrganizationId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "ORGANIZATION_REQUIRED");
            }

            var reservation = new EmergencyAdmissionRequestEntity(
                    request.requestId(),
                    actor.getOrganizationId());
            admissionRequestRepository.saveAndFlush(reservation);

            var patient = provisionalPatientService.create(request.patient()).patient();
            var emergency = emergencyService.createEmergency(
                    request.emergency().forPatient(patient.getId()),
                    userId);

            reservation.complete(emergency.getId());
            admissionRequestRepository.save(reservation);
            return emergency;
        });

        if (result == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "EMERGENCY_ADMISSION_FAILED");
        }
        return result;
    }

    private EmergencyEntity findCompleted(UUID requestId) {
        return admissionRequestRepository.findById(requestId)
                .filter(item -> EmergencyAdmissionRequestEntity.COMPLETED.equals(item.getStatus()))
                .map(EmergencyAdmissionRequestEntity::getEmergencyId)
                .flatMap(emergencyRepository::findByIdWithPatientAndLogs)
                .orElse(null);
    }
}
