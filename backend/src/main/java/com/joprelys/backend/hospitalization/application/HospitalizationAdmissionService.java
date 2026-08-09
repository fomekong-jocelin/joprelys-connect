package com.joprelys.backend.hospitalization.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.StaffOrganizationalUnitAssignmentEntity;
import com.joprelys.backend.auth.infrastructure.persistence.StaffOrganizationalUnitAssignmentRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyRepository;
import com.joprelys.backend.hospitalization.api.CreateHospitalizationRequest;
import com.joprelys.backend.hospitalization.api.HospitalizationResponse;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.hospitalorganization.domain.OrganizationalUnitType;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.HospitalServiceCatalogRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitRepository;
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.reconciliation.application.PatientCanonicalResolver;
import com.joprelys.backend.spatial.application.ActiveBedAssignmentService;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedCapacityStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedReadinessStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceProfileRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.OrganizationalUnitSpaceAssignmentRepository;
import com.joprelys.backend.visit.application.VisitNumberGenerator;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
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
    private static final Set<OrganizationalUnitType> ADMISSION_UNIT_TYPES = Set.of(
            OrganizationalUnitType.SERVICE,
            OrganizationalUnitType.CARE_UNIT);

    private final HospitalizationRepository hospitalizationRepository;
    private final UserAccountRepository userAccountRepository;
    private final StaffOrganizationalUnitAssignmentRepository staffUnitAssignmentRepository;
    private final AuditService auditService;
    private final BedRepository bedRepository;
    private final BedAssignmentRepository bedAssignmentRepository;
    private final ActiveBedAssignmentService activeBedAssignmentService;
    private final EmergencyRepository emergencyRepository;
    private final PatientCanonicalResolver canonicalResolver;
    private final VisitRepository visitRepository;
    private final VisitNumberGenerator visitNumberGenerator;
    private final OrganizationalUnitRepository unitRepository;
    private final HospitalServiceCatalogRepository serviceCatalogRepository;
    private final FacilitySpaceRepository spaceRepository;
    private final InpatientSpaceProfileRepository inpatientProfileRepository;
    private final OrganizationalUnitSpaceAssignmentRepository unitSpaceAssignmentRepository;

    public HospitalizationAdmissionService(
            HospitalizationRepository hospitalizationRepository,
            UserAccountRepository userAccountRepository,
            StaffOrganizationalUnitAssignmentRepository staffUnitAssignmentRepository,
            AuditService auditService,
            BedRepository bedRepository,
            BedAssignmentRepository bedAssignmentRepository,
            ActiveBedAssignmentService activeBedAssignmentService,
            EmergencyRepository emergencyRepository,
            PatientCanonicalResolver canonicalResolver,
            VisitRepository visitRepository,
            VisitNumberGenerator visitNumberGenerator,
            OrganizationalUnitRepository unitRepository,
            HospitalServiceCatalogRepository serviceCatalogRepository,
            FacilitySpaceRepository spaceRepository,
            InpatientSpaceProfileRepository inpatientProfileRepository,
            OrganizationalUnitSpaceAssignmentRepository unitSpaceAssignmentRepository) {
        this.hospitalizationRepository = hospitalizationRepository;
        this.userAccountRepository = userAccountRepository;
        this.staffUnitAssignmentRepository = staffUnitAssignmentRepository;
        this.auditService = auditService;
        this.bedRepository = bedRepository;
        this.bedAssignmentRepository = bedAssignmentRepository;
        this.activeBedAssignmentService = activeBedAssignmentService;
        this.emergencyRepository = emergencyRepository;
        this.canonicalResolver = canonicalResolver;
        this.visitRepository = visitRepository;
        this.visitNumberGenerator = visitNumberGenerator;
        this.unitRepository = unitRepository;
        this.serviceCatalogRepository = serviceCatalogRepository;
        this.spaceRepository = spaceRepository;
        this.inpatientProfileRepository = inpatientProfileRepository;
        this.unitSpaceAssignmentRepository = unitSpaceAssignmentRepository;
    }

    @Transactional
    public HospitalizationResponse admitPatient(CreateHospitalizationRequest request) {
        var patientContext = canonicalResolver.resolve(request.patientId());
        UUID organizationId = patientContext.canonicalPatient().getOrganizationId();
        AdmissionPlacement placement = resolvePlacement(request, organizationId);
        EmergencyEntity emergency = resolveEmergency(request, patientContext);
        VisitEntity visit = resolveVisit(request, patientContext, emergency, placement.serviceNameSnapshot());

        rejectActiveHospitalization(patientContext);
        rejectDuplicateEmergencyHospitalization(emergency);
        validateResponsiblePractitioner(
                request.responsiblePractitionerId(),
                organizationId,
                placement.unit().getId());
        rejectActiveBedAssignment(placement.bed().getId());

        BedEntity occupiedBed = claimConfiguredBed(placement.bed());
        HospitalizationEntity saved = hospitalizationRepository.save(createHospitalization(
                request,
                visit,
                emergency,
                placement,
                occupiedBed));
        activeBedAssignmentService.assign(saved.getId(), occupiedBed, saved.getOrganizationId());

        auditAdmission(saved, emergency);
        return HospitalizationResponse.fromEntity(saved);
    }

    private AdmissionPlacement resolvePlacement(CreateHospitalizationRequest request, UUID organizationId) {
        OrganizationalUnitEntity unit = unitRepository
                .findByIdAndOrganizationId(request.serviceUnitId(), organizationId)
                .orElseThrow(() -> notFound("Unité de service introuvable."));
        if (!unit.isActive()) {
            throw conflict("L'unité de service sélectionnée est inactive.");
        }
        if (!ADMISSION_UNIT_TYPES.contains(unit.getUnitType())) {
            throw badRequest("L'unité sélectionnée ne peut pas porter une admission hospitalière.");
        }

        FacilitySpaceEntity space = spaceRepository
                .findByIdAndOrganizationId(request.spaceId(), organizationId)
                .orElseThrow(() -> notFound("Espace d'hébergement introuvable."));
        if (!space.isActive()) {
            throw conflict("L'espace d'hébergement sélectionné est inactif.");
        }
        if (!inpatientProfileRepository.existsBySpaceIdAndOrganizationId(space.getId(), organizationId)) {
            throw conflict("L'espace sélectionné n'est pas configuré pour l'hébergement.");
        }
        if (!unitSpaceAssignmentRepository.existsActiveAt(
                organizationId, unit.getId(), space.getId(), Instant.now())) {
            throw conflict("L'unité sélectionnée n'utilise pas cet espace à la date de l'admission.");
        }

        BedEntity bed = bedRepository.findByIdAndOrganizationId(request.bedId(), organizationId)
                .orElseThrow(() -> notFound("Lit introuvable."));
        if (!bed.getSpace().getId().equals(space.getId())) {
            throw conflict("Le lit sélectionné n'appartient pas à l'espace demandé.");
        }

        return new AdmissionPlacement(
                unit,
                space,
                bed,
                resolveUnitLabel(unit),
                space.getName());
    }

    private String resolveUnitLabel(OrganizationalUnitEntity unit) {
        if (unit.getUnitType() == OrganizationalUnitType.SERVICE) {
            return serviceCatalogRepository.findById(unit.getServiceCatalogCode())
                    .map(catalog -> catalog.getNameFr())
                    .orElseThrow(() -> conflict("Le catalogue du service sélectionné est introuvable."));
        }
        return unit.getName() == null || unit.getName().isBlank() ? unit.getCode() : unit.getName();
    }

    private EmergencyEntity resolveEmergency(
            CreateHospitalizationRequest request,
            PatientCanonicalResolver.CanonicalPatientContext requestedContext) {
        if (request.emergencyId() == null) {
            return null;
        }

        EmergencyEntity emergency = emergencyRepository.findByIdWithPatientAndLogs(request.emergencyId())
                .orElseThrow(() -> notFound("Urgence introuvable."));
        var emergencyContext = canonicalResolver.resolve(emergency.getPatient().getId());
        if (!emergencyContext.canonicalPatient().getId().equals(requestedContext.canonicalPatient().getId())) {
            throw conflict("L'urgence ne correspond pas au dossier patient sélectionné.");
        }
        if (!emergency.getOrganizationId().equals(requestedContext.canonicalPatient().getOrganizationId())) {
            throw notFound("Urgence introuvable.");
        }
        return emergency;
    }

    private VisitEntity resolveVisit(
            CreateHospitalizationRequest request,
            PatientCanonicalResolver.CanonicalPatientContext patientContext,
            EmergencyEntity emergency,
            String serviceSnapshot) {
        if (request.visitId() != null) {
            VisitEntity visit = requireVisit(request.visitId());
            validateVisitPatient(visit, patientContext);
            linkEmergencyToVisitIfNeeded(emergency, visit);
            return visit;
        }

        if (emergency == null) {
            throw badRequest("Une visite ou une urgence associée est obligatoire.");
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
                        serviceSnapshot,
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
            throw conflict("L'urgence est déjà associée à une autre visite.");
        }
        if (emergency.getVisitId() == null) {
            emergency.setVisitId(visit.getId());
            emergencyRepository.save(emergency);
        }
    }

    private VisitEntity requireVisit(UUID visitId) {
        return visitRepository.findById(visitId)
                .orElseThrow(() -> notFound("Visite introuvable."));
    }

    private void validateVisitPatient(
            VisitEntity visit,
            PatientCanonicalResolver.CanonicalPatientContext context) {
        if (!context.contributingPatientIds().contains(visit.getPatient().getId())) {
            throw conflict("La visite ne correspond pas au patient ou à l'une de ses identités sources.");
        }
    }

    private void rejectActiveHospitalization(PatientCanonicalResolver.CanonicalPatientContext context) {
        hospitalizationRepository.findActiveByPatientIds(context.contributingPatientIds()).ifPresent(hospitalization -> {
            throw conflict("Le patient possède déjà une hospitalisation active, y compris sous une identité source.");
        });
    }

    private void rejectDuplicateEmergencyHospitalization(EmergencyEntity emergency) {
        if (emergency == null) {
            return;
        }
        hospitalizationRepository.findByEmergencyId(emergency.getId()).ifPresent(hospitalization -> {
            throw conflict("Cette urgence est déjà liée à une hospitalisation.");
        });
    }

    private void rejectActiveBedAssignment(UUID bedId) {
        if (bedAssignmentRepository.findActiveByBedId(bedId).isPresent()
                || hospitalizationRepository.findActiveByBedId(bedId).isPresent()) {
            throw conflict("Le lit demandé est déjà occupé par un autre séjour.");
        }
    }

    private void validateResponsiblePractitioner(UUID practitionerId, UUID organizationId, UUID unitId) {
        UserAccountEntity practitioner = userAccountRepository
                .findByIdAndOrganizationId(practitionerId, organizationId)
                .filter(UserAccountEntity::isEnabled)
                .orElseThrow(() -> conflict("Le praticien responsable n'est pas actif dans cet établissement."));
        if (!practitioner.hasRole("MEDECIN")) {
            throw conflict("Le praticien responsable doit être un médecin habilité.");
        }

        Instant now = Instant.now();
        boolean assignedToUnit = staffUnitAssignmentRepository
                .findAllByOrganizationIdAndStaffIdOrderByValidFromDesc(organizationId, practitionerId)
                .stream()
                .anyMatch(assignment -> isActiveAssignmentForUnit(assignment, unitId, now));
        if (!assignedToUnit) {
            throw conflict("Le praticien responsable n'est pas affecté à l'unité sélectionnée.");
        }
    }

    private boolean isActiveAssignmentForUnit(
            StaffOrganizationalUnitAssignmentEntity assignment,
            UUID unitId,
            Instant at) {
        return unitId.equals(assignment.getOrganizationalUnitId()) && assignment.activeAt(at);
    }

    private BedEntity claimConfiguredBed(BedEntity configuredBed) {
        int claimed = bedRepository.claimIfAvailable(
                configuredBed.getId(),
                BedStatus.FREE,
                BedStatus.OCCUPIED,
                BedCapacityStatus.OPEN,
                BedReadinessStatus.READY);
        if (claimed != 1) {
            throw conflict("Le lit demandé n'est pas ouvert, prêt et disponible.");
        }
        return bedRepository.findById(configuredBed.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Le lit réservé a disparu pendant la transaction d'admission."));
    }

    private HospitalizationEntity createHospitalization(
            CreateHospitalizationRequest request,
            VisitEntity visit,
            EmergencyEntity emergency,
            AdmissionPlacement placement,
            BedEntity occupiedBed) {
        Long sequence = hospitalizationRepository.getNextHospitalizationNumberSequenceValue();
        String number = String.format(
                "HOSP-%s-%06d",
                LocalDate.now().format(HOSPITALIZATION_DATE),
                sequence);
        HospitalizationEntity hospitalization = new HospitalizationEntity(
                visit.getPatient().getId(),
                placement.unit().getId(),
                placement.space().getId(),
                occupiedBed.getId(),
                placement.serviceNameSnapshot(),
                placement.spaceNameSnapshot(),
                occupiedBed.getBedNumber(),
                request.admissionReason(),
                number,
                visit.getId(),
                emergency == null ? null : emergency.getId(),
                request.responsiblePractitionerId());
        hospitalization.setOrganizationId(visit.getPatient().getOrganizationId());
        return hospitalization;
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
                        + " | Espace : " + hospitalization.getSpaceName()
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

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private record AdmissionPlacement(
            OrganizationalUnitEntity unit,
            FacilitySpaceEntity space,
            BedEntity bed,
            String serviceNameSnapshot,
            String spaceNameSnapshot) {
    }
}
