package com.joprelys.backend.hospitalization.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyRepository;
import com.joprelys.backend.hospitalization.api.CreateHospitalizationRequest;
import com.joprelys.backend.hospitalization.api.HospitalizationResponse;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.reconciliation.application.PatientCanonicalResolver;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStatus;
import com.joprelys.backend.visit.application.VisitNumberGenerator;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HospitalizationAdmissionService {

    private static final DateTimeFormatter HOSPITALIZATION_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String ACTIVE_VISIT_STATUS = "EN_COURS";

    private final HospitalizationRepository hospitalizationRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;
    private final BedRepository bedRepository;
    private final BedAssignmentRepository bedAssignmentRepository;
    private final EmergencyRepository emergencyRepository;
    private final PatientCanonicalResolver canonicalResolver;
    private final VisitRepository visitRepository;
    private final VisitNumberGenerator visitNumberGenerator;

    public HospitalizationAdmissionService(
            HospitalizationRepository hospitalizationRepository,
            UserAccountRepository userAccountRepository,
            AuditService auditService,
            BedRepository bedRepository,
            BedAssignmentRepository bedAssignmentRepository,
            EmergencyRepository emergencyRepository,
            PatientCanonicalResolver canonicalResolver,
            VisitRepository visitRepository,
            VisitNumberGenerator visitNumberGenerator) {
        this.hospitalizationRepository = hospitalizationRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
        this.bedRepository = bedRepository;
        this.bedAssignmentRepository = bedAssignmentRepository;
        this.emergencyRepository = emergencyRepository;
        this.canonicalResolver = canonicalResolver;
        this.visitRepository = visitRepository;
        this.visitNumberGenerator = visitNumberGenerator;
    }

    @Transactional
    public HospitalizationResponse admitPatient(CreateHospitalizationRequest request) {
        var patientContext = canonicalResolver.resolve(request.patientId());
        EmergencyEntity emergency = resolveEmergency(request, patientContext);
        VisitEntity visit = resolveVisit(request, patientContext, emergency);
        PatientEntity carePatient = visit.getPatient();

        rejectActiveHospitalization(patientContext);
        rejectDuplicateEmergencyHospitalization(emergency);
        rejectActiveBedAssignment(request);

        BedEntity configuredBed = requireConfiguredBed(carePatient, request);
        configuredBed.getRoom().getWard().requireRoomsAllowed();
        BedEntity occupiedBed = claimConfiguredBed(configuredBed);

        HospitalizationEntity saved = hospitalizationRepository.save(createHospitalization(
                request,
                visit,
                emergency));
        BedAssignmentEntity assignment = new BedAssignmentEntity(saved.getId(), occupiedBed);
        assignment.setOrganizationId(saved.getOrganizationId());
        bedAssignmentRepository.save(assignment);

        auditAdmission(saved, emergency);
        return HospitalizationResponse.fromEntity(saved);
    }

    private EmergencyEntity resolveEmergency(
            CreateHospitalizationRequest request,
            PatientCanonicalResolver.CanonicalPatientContext requestedContext) {
        if (request.emergencyId() == null) {
            return null;
        }

        EmergencyEntity emergency = emergencyRepository.findByIdWithPatientAndLogs(request.emergencyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Urgence introuvable."));
        var emergencyContext = canonicalResolver.resolve(emergency.getPatient().getId());
        if (!emergencyContext.canonicalPatient().getId().equals(requestedContext.canonicalPatient().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "L'urgence ne correspond pas au dossier patient sélectionné.");
        }
        if (!emergency.getOrganizationId().equals(requestedContext.canonicalPatient().getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Urgence introuvable.");
        }
        return emergency;
    }

    private VisitEntity resolveVisit(
            CreateHospitalizationRequest request,
            PatientCanonicalResolver.CanonicalPatientContext patientContext,
            EmergencyEntity emergency) {
        if (request.visitId() != null) {
            VisitEntity visit = requireVisit(request.visitId());
            validateVisitPatient(visit, patientContext);
            linkEmergencyToVisitIfNeeded(emergency, visit);
            return visit;
        }

        if (emergency == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Une visite ou une urgence associée est obligatoire.");
        }

        if (emergency.getVisitId() != null) {
            VisitEntity visit = requireVisit(emergency.getVisitId());
            validateVisitPatient(visit, patientContext);
            return visit;
        }

        PatientEntity visitPatient = patientContext.requestedPatient().getIdentityStatus() == PatientIdentityStatus.MERGED
                ? patientContext.canonicalPatient()
                : patientContext.requestedPatient();
        VisitEntity visit = visitRepository
                .findFirstByPatientIdAndStatusOrderByCreatedAtDesc(visitPatient.getId(), ACTIVE_VISIT_STATUS)
                .orElseGet(() -> visitRepository.save(new VisitEntity(
                        visitPatient,
                        visitNumberGenerator.generateNextVisitNumber(),
                        "Hospitalisation après urgence : " + emergency.getChiefComplaint(),
                        "HOSPITALISATION",
                        request.serviceName(),
                        request.responsiblePractitionerId(),
                        emergency.getCreatedAt())));
        emergency.setVisitId(visit.getId());
        emergencyRepository.save(emergency);
        return visit;
    }

    private void linkEmergencyToVisitIfNeeded(EmergencyEntity emergency, VisitEntity visit) {
        if (emergency == null) {
            return;
        }
        if (emergency.getVisitId() != null && !emergency.getVisitId().equals(visit.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "L'urgence est déjà associée à une autre visite.");
        }
        if (emergency.getVisitId() == null) {
            emergency.setVisitId(visit.getId());
            emergencyRepository.save(emergency);
        }
    }

    private VisitEntity requireVisit(UUID visitId) {
        return visitRepository.findById(visitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable."));
    }

    private void validateVisitPatient(
            VisitEntity visit,
            PatientCanonicalResolver.CanonicalPatientContext context) {
        if (!context.contributingPatientIds().contains(visit.getPatient().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La visite ne correspond pas au patient ou à l'une de ses identités sources.");
        }
    }

    private void rejectActiveHospitalization(PatientCanonicalResolver.CanonicalPatientContext context) {
        hospitalizationRepository.findActiveByPatientIds(context.contributingPatientIds()).ifPresent(hospitalization -> {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le patient possède déjà une hospitalisation active, y compris sous une identité source.");
        });
    }

    private void rejectDuplicateEmergencyHospitalization(EmergencyEntity emergency) {
        if (emergency == null) {
            return;
        }
        hospitalizationRepository.findByEmergencyId(emergency.getId()).ifPresent(hospitalization -> {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cette urgence est déjà liée à une hospitalisation.");
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

    private BedEntity claimConfiguredBed(BedEntity configuredBed) {
        int claimed = bedRepository.claimIfFree(configuredBed.getId(), BedStatus.FREE, BedStatus.OCCUPIED);
        if (claimed != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le lit demandé n'est pas libre.");
        }
        return bedRepository.findById(configuredBed.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Le lit réservé a disparu pendant la transaction d'admission."));
    }

    private HospitalizationEntity createHospitalization(
            CreateHospitalizationRequest request,
            VisitEntity visit,
            EmergencyEntity emergency) {
        Long sequence = hospitalizationRepository.getNextHospitalizationNumberSequenceValue();
        String number = String.format(
                "HOSP-%s-%06d",
                LocalDate.now().format(HOSPITALIZATION_DATE),
                sequence);
        return new HospitalizationEntity(
                visit.getPatient().getId(),
                request.serviceName(),
                request.roomNumber(),
                request.bedNumber(),
                request.admissionReason(),
                number,
                visit.getId(),
                emergency == null ? null : emergency.getId(),
                request.responsiblePractitionerId());
    }

    private void auditAdmission(HospitalizationEntity hospitalization, EmergencyEntity emergency) {
        UserAccountEntity actor = currentUser();
        if (actor == null) {
            return;
        }
        auditService.logSuccess(
                actor.getId(),
                actor.getOrganizationId(),
                hospitalization.getPatientId(),
                "HOSPITALIZATION",
                hospitalization.getId(),
                "ADMISSION",
                "Admission en hospitalisation. Service : " + hospitalization.getServiceName()
                        + " | Chambre : " + hospitalization.getRoomNumber()
                        + " | Lit : " + hospitalization.getBedNumber()
                        + (emergency == null ? "" : " | Urgence source : " + emergency.getId()));
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
