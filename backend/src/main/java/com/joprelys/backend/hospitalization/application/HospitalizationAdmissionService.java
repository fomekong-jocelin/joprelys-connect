package com.joprelys.backend.hospitalization.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.hospitalization.api.CreateHospitalizationRequest;
import com.joprelys.backend.hospitalization.api.HospitalizationResponse;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.patient.application.PatientService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStatus;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HospitalizationAdmissionService {

    private static final DateTimeFormatter HOSPITALIZATION_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final HospitalizationRepository hospitalizationRepository;
    private final PatientService patientService;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;
    private final BedRepository bedRepository;
    private final BedAssignmentRepository bedAssignmentRepository;

    public HospitalizationAdmissionService(
            HospitalizationRepository hospitalizationRepository,
            PatientService patientService,
            UserAccountRepository userAccountRepository,
            AuditService auditService,
            BedRepository bedRepository,
            BedAssignmentRepository bedAssignmentRepository) {
        this.hospitalizationRepository = hospitalizationRepository;
        this.patientService = patientService;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
        this.bedRepository = bedRepository;
        this.bedAssignmentRepository = bedAssignmentRepository;
    }

    @Transactional
    public HospitalizationResponse admitPatient(CreateHospitalizationRequest request) {
        PatientEntity patient = patientService.getPatientById(request.patientId());
        rejectActiveHospitalization(request);
        rejectActiveBedAssignment(request);

        BedEntity bed = requireConfiguredBed(patient, request);
        bed.getRoom().getWard().requireRoomsAllowed();
        if (bed.getStatus() != BedStatus.FREE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le lit demandé n'est pas libre.");
        }

        bed.setStatus(BedStatus.OCCUPIED);
        bedRepository.saveAndFlush(bed);

        HospitalizationEntity saved = hospitalizationRepository.save(createHospitalization(request));
        BedAssignmentEntity assignment = new BedAssignmentEntity(saved.getId(), bed);
        assignment.setOrganizationId(saved.getOrganizationId());
        bedAssignmentRepository.save(assignment);

        auditAdmission(request, saved);
        return HospitalizationResponse.fromEntity(saved);
    }

    private void rejectActiveHospitalization(CreateHospitalizationRequest request) {
        hospitalizationRepository.findActiveByPatientId(request.patientId()).ifPresent(hospitalization -> {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le patient est déjà admis en hospitalisation active.");
        });
    }

    private void rejectActiveBedAssignment(CreateHospitalizationRequest request) {
        hospitalizationRepository.findActiveByBed(request.roomNumber(), request.bedNumber())
                .ifPresent(hospitalization -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Le lit demandé est déjà occupé par un autre séjour.");
                });
    }

    private BedEntity requireConfiguredBed(PatientEntity patient, CreateHospitalizationRequest request) {
        return bedRepository.findConfiguredBed(
                        patient.getOrganizationId(),
                        request.serviceName(),
                        request.roomNumber(),
                        request.bedNumber())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Le lit sélectionné n'existe pas dans la structure configurée."));
    }

    private HospitalizationEntity createHospitalization(CreateHospitalizationRequest request) {
        Long sequence = hospitalizationRepository.getNextHospitalizationNumberSequenceValue();
        String number = String.format(
                "HOSP-%s-%06d",
                LocalDate.now().format(HOSPITALIZATION_DATE),
                sequence);
        return new HospitalizationEntity(
                request.patientId(),
                request.serviceName(),
                request.roomNumber(),
                request.bedNumber(),
                request.admissionReason(),
                number,
                request.visitId(),
                request.responsiblePractitionerId());
    }

    private void auditAdmission(CreateHospitalizationRequest request, HospitalizationEntity hospitalization) {
        UserAccountEntity actor = currentUser();
        if (actor == null) {
            return;
        }
        auditService.logSuccess(
                actor.getId(),
                actor.getOrganizationId(),
                request.patientId(),
                "HOSPITALIZATION",
                hospitalization.getId(),
                "ADMISSION",
                "Admission en hospitalisation. Service : " + hospitalization.getServiceName()
                        + " | Chambre : " + hospitalization.getRoomNumber()
                        + " | Lit : " + hospitalization.getBedNumber());
    }

    private UserAccountEntity currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return userAccountRepository.findByEmail(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .orElse(null);
    }
}
