package com.joprelys.backend.spatial.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.hospitalorganization.domain.OrganizationalUnitType;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.HospitalServiceCatalogRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitRepository;
import com.joprelys.backend.spatial.api.BedAssignmentResponse;
import com.joprelys.backend.spatial.api.BedResponse;
import com.joprelys.backend.spatial.api.BedStateChangeResponse;
import com.joprelys.backend.spatial.api.OrganizationalUnitOccupancyResponse;
import com.joprelys.backend.spatial.api.SpaceOccupancyResponse;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedCapacityStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedReadinessStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStateAxis;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStateChangeSource;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStateReasonCode;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceProfileRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.OrganizationalUnitSpaceAssignmentRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SpatialService {

    private static final Set<OrganizationalUnitType> HOSPITALIZATION_UNIT_TYPES = Set.of(
            OrganizationalUnitType.SERVICE,
            OrganizationalUnitType.CARE_UNIT);

    private final BedRepository bedRepository;
    private final BedAssignmentRepository bedAssignmentRepository;
    private final ActiveBedAssignmentService activeBedAssignmentService;
    private final BedStatusTransitionPolicy bedStatusTransitionPolicy;
    private final BedStateChangeService bedStateChangeService;
    private final HospitalizationRepository hospitalizationRepository;
    private final FacilitySpaceRepository spaceRepository;
    private final InpatientSpaceProfileRepository inpatientProfileRepository;
    private final OrganizationalUnitSpaceAssignmentRepository unitSpaceAssignmentRepository;
    private final OrganizationalUnitRepository unitRepository;
    private final HospitalServiceCatalogRepository serviceCatalogRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public SpatialService(
            BedRepository bedRepository,
            BedAssignmentRepository bedAssignmentRepository,
            ActiveBedAssignmentService activeBedAssignmentService,
            BedStatusTransitionPolicy bedStatusTransitionPolicy,
            BedStateChangeService bedStateChangeService,
            HospitalizationRepository hospitalizationRepository,
            FacilitySpaceRepository spaceRepository,
            InpatientSpaceProfileRepository inpatientProfileRepository,
            OrganizationalUnitSpaceAssignmentRepository unitSpaceAssignmentRepository,
            OrganizationalUnitRepository unitRepository,
            HospitalServiceCatalogRepository serviceCatalogRepository,
            UserAccountRepository userAccountRepository,
            AuditService auditService) {
        this.bedRepository = bedRepository;
        this.bedAssignmentRepository = bedAssignmentRepository;
        this.activeBedAssignmentService = activeBedAssignmentService;
        this.bedStatusTransitionPolicy = bedStatusTransitionPolicy;
        this.bedStateChangeService = bedStateChangeService;
        this.hospitalizationRepository = hospitalizationRepository;
        this.spaceRepository = spaceRepository;
        this.inpatientProfileRepository = inpatientProfileRepository;
        this.unitSpaceAssignmentRepository = unitSpaceAssignmentRepository;
        this.unitRepository = unitRepository;
        this.serviceCatalogRepository = serviceCatalogRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public SpaceOccupancyResponse getSpaceOccupancy(UUID spaceId) {
        FacilitySpaceEntity space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> notFound("Espace introuvable."));
        List<BedEntity> beds = bedRepository.findBySpaceId(spaceId);
        BedMetrics metrics = calculateMetrics(beds);
        return new SpaceOccupancyResponse(
                space.getId(),
                space.getCode(),
                space.getName(),
                metrics.installedBeds(),
                metrics.occupiedBeds(),
                metrics.availableBeds(),
                metrics.openBeds(),
                metrics.readyBeds(),
                metrics.bedResponses());
    }

    @Transactional(readOnly = true)
    public OrganizationalUnitOccupancyResponse getOrganizationalUnitOccupancy(UUID unitId) {
        OrganizationalUnitEntity unit = unitRepository.findById(unitId)
                .orElseThrow(() -> notFound("Unité organisationnelle introuvable."));
        Instant now = Instant.now();
        List<UUID> spaceIds = unitSpaceAssignmentRepository
                .findAllByOrganizationIdAndOrganizationalUnitIdOrderByValidFromDesc(
                        unit.getOrganizationId(), unitId)
                .stream()
                .filter(assignment -> assignment.isActiveAt(now))
                .map(assignment -> assignment.getSpaceId())
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf));

        Map<UUID, BedEntity> distinctBeds = new LinkedHashMap<>();
        for (UUID spaceId : spaceIds) {
            bedRepository.findBySpaceId(spaceId).forEach(bed -> distinctBeds.putIfAbsent(bed.getId(), bed));
        }
        BedMetrics metrics = calculateMetrics(new ArrayList<>(distinctBeds.values()));
        return new OrganizationalUnitOccupancyResponse(
                unit.getId(),
                unit.getCode(),
                resolveUnitLabel(unit),
                spaceIds,
                metrics.installedBeds(),
                metrics.occupiedBeds(),
                metrics.availableBeds(),
                metrics.openBeds(),
                metrics.readyBeds());
    }

    @Transactional(readOnly = true)
    public List<BedStateChangeResponse> getBedStateHistory(UUID bedId) {
        requireBed(bedId);
        return bedStateChangeService.listHistory(bedId);
    }

    public BedStateReasonCode parseBedStateReasonCode(String value) {
        return bedStateChangeService.parseReasonCode(value);
    }

    @Transactional
    public BedResponse updateBedStatus(UUID bedId, BedStatus newStatus, String note) {
        BedEntity bed = requireBed(bedId);
        boolean hasActiveAssignment = bedAssignmentRepository.findActiveByBedId(bedId).isPresent();
        bedStatusTransitionPolicy.validateManualTransition(
                bed.getStatus(),
                newStatus,
                bed.getCapacityStatus(),
                hasActiveAssignment);
        return persistLegacyStatusChange(bed, newStatus, hasActiveAssignment, note);
    }

    @Transactional
    public BedResponse updateBedCleaningStatus(
            UUID bedId,
            BedReadinessStatus newReadinessStatus,
            BedStateReasonCode reasonCode,
            String note) {
        BedEntity bed = requireBed(bedId);
        boolean hasActiveAssignment = bedAssignmentRepository.findActiveByBedId(bedId).isPresent();
        bedStatusTransitionPolicy.validateCleaningTransition(
                bed.getStatus(),
                bed.getReadinessStatus(),
                newReadinessStatus,
                bed.getCapacityStatus(),
                hasActiveAssignment);
        return persistReadinessAxisChange(
                bed,
                newReadinessStatus,
                hasActiveAssignment,
                reasonCode,
                note,
                "UPDATE_BED_CLEANING",
                "Circuit de nettoyage");
    }

    @Transactional
    public BedResponse updateBedMaintenanceStatus(
            UUID bedId,
            BedReadinessStatus newReadinessStatus,
            BedStateReasonCode reasonCode,
            String note) {
        BedEntity bed = requireBed(bedId);
        boolean hasActiveAssignment = bedAssignmentRepository.findActiveByBedId(bedId).isPresent();
        bedStatusTransitionPolicy.validateMaintenanceTransition(
                bed.getStatus(),
                bed.getReadinessStatus(),
                newReadinessStatus,
                bed.getCapacityStatus(),
                hasActiveAssignment);
        return persistReadinessAxisChange(
                bed,
                newReadinessStatus,
                hasActiveAssignment,
                reasonCode,
                note,
                "UPDATE_BED_MAINTENANCE",
                "Circuit de maintenance");
    }

    @Transactional
    public BedResponse updateBedCapacityStatus(
            UUID bedId,
            BedCapacityStatus newCapacityStatus,
            BedStateReasonCode reasonCode,
            String note) {
        BedEntity bed = requireBed(bedId);
        boolean hasActiveAssignment = bedAssignmentRepository.findActiveByBedId(bedId).isPresent();
        bedStatusTransitionPolicy.validateCapacityTransition(
                bed.getStatus(),
                newCapacityStatus,
                hasActiveAssignment);

        if (bed.getCapacityStatus() == newCapacityStatus) {
            return BedResponse.fromEntity(bed, hasActiveAssignment);
        }

        BedCapacityStatus previousStatus = bed.getCapacityStatus();
        bedStateChangeService.recordManual(
                bed,
                BedStateAxis.CAPACITY,
                previousStatus.name(),
                newCapacityStatus.name(),
                reasonCode,
                note);
        bed.setCapacityStatus(newCapacityStatus);
        BedEntity saved = bedRepository.save(bed);
        auditBedChange(
                saved,
                "UPDATE_BED_CAPACITY",
                "Capacité du lit " + saved.getBedNumber()
                        + " changée de " + previousStatus + " à " + newCapacityStatus
                        + " | motif=" + reasonCode);
        return BedResponse.fromEntity(saved, false);
    }

    @Transactional
    public BedAssignmentResponse transferPatient(
            UUID hospitalizationId,
            UUID targetServiceUnitId,
            UUID targetSpaceId,
            UUID targetBedId) {
        HospitalizationEntity hospitalization = hospitalizationRepository.findById(hospitalizationId)
                .orElseThrow(() -> notFound("Hospitalisation introuvable."));

        if (!"EN_COURS".equals(hospitalization.getStatus())) {
            throw conflict("L'hospitalisation n'est pas active.");
        }
        if (hospitalization.hasDischargeDecision()) {
            throw conflict(
                    "Le transfert est interdit après la décision médicale de sortie. "
                            + "Annulez formellement la décision ou confirmez le départ physique.");
        }

        UUID organizationId = hospitalization.getOrganizationId();
        OrganizationalUnitEntity targetUnit = unitRepository
                .findByIdAndOrganizationId(targetServiceUnitId, organizationId)
                .orElseThrow(() -> notFound("Unité de service cible introuvable."));
        if (!targetUnit.isActive() || !HOSPITALIZATION_UNIT_TYPES.contains(targetUnit.getUnitType())) {
            throw conflict("L'unité de service cible n'est pas disponible pour une hospitalisation.");
        }

        FacilitySpaceEntity targetSpace = spaceRepository
                .findByIdAndOrganizationId(targetSpaceId, organizationId)
                .orElseThrow(() -> notFound("Espace cible introuvable."));
        if (!targetSpace.isActive()) {
            throw conflict("L'espace cible est inactif.");
        }
        if (!inpatientProfileRepository.existsBySpaceIdAndOrganizationId(targetSpaceId, organizationId)) {
            throw conflict("L'espace cible n'est pas configuré pour l'hébergement.");
        }
        if (!unitSpaceAssignmentRepository.existsActiveAt(
                organizationId, targetServiceUnitId, targetSpaceId, Instant.now())) {
            throw conflict("L'unité cible n'utilise pas cet espace à la date du transfert.");
        }

        BedEntity targetBed = bedRepository.findByIdAndOrganizationId(targetBedId, organizationId)
                .orElseThrow(() -> notFound("Lit cible introuvable."));
        if (!targetBed.getSpace().getId().equals(targetSpaceId)) {
            throw conflict("Le lit cible n'appartient pas à l'espace demandé.");
        }

        int claimed = bedRepository.claimIfAvailable(
                targetBedId,
                BedStatus.FREE,
                BedStatus.OCCUPIED,
                BedCapacityStatus.OPEN,
                BedReadinessStatus.READY);
        if (claimed != 1) {
            throw conflict("Le lit demandé n'est pas ouvert, prêt et disponible.");
        }
        BedEntity occupiedBed = bedRepository.findById(targetBedId)
                .orElseThrow(() -> new IllegalStateException(
                        "Le lit réservé a disparu pendant la transaction de transfert."));

        bedAssignmentRepository.findActiveByHospitalizationId(hospitalizationId).ifPresent(oldAssignment -> {
            BedEntity oldBed = oldAssignment.getBed();
            BedReadinessStatus previousReadiness = oldBed.getReadinessStatus();
            if (previousReadiness != BedReadinessStatus.READY) {
                throw conflict("Le lit source présente un état de préparation incohérent pour un transfert.");
            }
            oldAssignment.releaseAt(Instant.now());
            bedAssignmentRepository.saveAndFlush(oldAssignment);
            bedStateChangeService.recordSystem(
                    oldBed,
                    BedStateAxis.READINESS,
                    previousReadiness.name(),
                    BedReadinessStatus.CLEANING.name(),
                    BedStateReasonCode.CLEANING_AFTER_TRANSFER,
                    "Nettoyage déclenché après transfert du séjour " + hospitalizationId,
                    BedStateChangeSource.SYSTEM_TRANSFER);
            oldBed.setStatus(BedStatus.CLEANING);
            bedRepository.save(oldBed);
        });

        String serviceSnapshot = resolveUnitLabel(targetUnit);
        hospitalization.relocate(
                targetUnit.getId(),
                targetSpace.getId(),
                occupiedBed.getId(),
                serviceSnapshot,
                targetSpace.getName(),
                occupiedBed.getBedNumber());
        hospitalizationRepository.save(hospitalization);

        var savedAssignment = activeBedAssignmentService.assign(
                hospitalizationId,
                occupiedBed,
                hospitalization.getOrganizationId());

        UserAccountEntity actor = getCurrentUser();
        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    actor.getOrganizationId(),
                    hospitalization.getPatientId(),
                    "HOSPITALIZATION",
                    hospitalizationId,
                    "TRANSFER",
                    "Patient transféré vers Lit : " + occupiedBed.getBedNumber()
                            + " | Espace : " + targetSpace.getName()
                            + " | Service : " + serviceSnapshot);
        }

        return BedAssignmentResponse.fromEntity(savedAssignment);
    }

    private BedMetrics calculateMetrics(List<BedEntity> beds) {
        Set<UUID> activeBedIds = beds.isEmpty()
                ? Set.of()
                : new LinkedHashSet<>(bedAssignmentRepository.findActiveBedIds(
                        beds.stream().map(BedEntity::getId).toList()));
        int open = 0;
        int ready = 0;
        int occupied = 0;
        int available = 0;
        List<BedResponse> responses = new ArrayList<>();
        for (BedEntity bed : beds) {
            boolean hasActiveAssignment = activeBedIds.contains(bed.getId());
            if (bed.isOpen()) {
                open++;
            }
            if (bed.isOpen() && bed.isReady()) {
                ready++;
            }
            if (hasActiveAssignment) {
                occupied++;
            }
            if (bed.isOperationallyAvailable(hasActiveAssignment)) {
                available++;
            }
            responses.add(BedResponse.fromEntity(bed, hasActiveAssignment));
        }
        return new BedMetrics(beds.size(), occupied, available, open, ready, responses);
    }

    private String resolveUnitLabel(OrganizationalUnitEntity unit) {
        if (unit.getUnitType() == OrganizationalUnitType.SERVICE) {
            return serviceCatalogRepository.findById(unit.getServiceCatalogCode())
                    .map(catalog -> catalog.getNameFr())
                    .orElse(unit.getCode());
        }
        return unit.getName() == null || unit.getName().isBlank() ? unit.getCode() : unit.getName();
    }

    private BedResponse persistLegacyStatusChange(
            BedEntity bed,
            BedStatus newStatus,
            boolean hasActiveAssignment,
            String note) {
        if (bed.getStatus() == newStatus) {
            return BedResponse.fromEntity(bed, hasActiveAssignment);
        }

        BedStatus previousStatus = bed.getStatus();
        BedCapacityStatus previousCapacity = bed.getCapacityStatus();
        BedReadinessStatus previousReadiness = bed.getReadinessStatus();
        bed.setStatus(newStatus);

        if (previousCapacity != bed.getCapacityStatus()) {
            bedStateChangeService.recordLegacySupervision(
                    bed,
                    BedStateAxis.CAPACITY,
                    previousCapacity.name(),
                    bed.getCapacityStatus().name(),
                    note);
        }
        if (previousReadiness != bed.getReadinessStatus()) {
            bedStateChangeService.recordLegacySupervision(
                    bed,
                    BedStateAxis.READINESS,
                    previousReadiness.name(),
                    bed.getReadinessStatus().name(),
                    note);
        }

        BedEntity saved = bedRepository.save(bed);
        auditBedChange(
                saved,
                "UPDATE_BED_READINESS",
                "État opérationnel supervisé du lit " + saved.getBedNumber()
                        + " : " + previousStatus + " → " + newStatus
                        + " | motif=LEGACY_SUPERVISION");
        return BedResponse.fromEntity(saved, false);
    }

    private BedResponse persistReadinessAxisChange(
            BedEntity bed,
            BedReadinessStatus newReadinessStatus,
            boolean hasActiveAssignment,
            BedStateReasonCode reasonCode,
            String note,
            String auditAction,
            String auditLabel) {
        if (bed.getReadinessStatus() == newReadinessStatus) {
            return BedResponse.fromEntity(bed, hasActiveAssignment);
        }

        BedReadinessStatus previousStatus = bed.getReadinessStatus();
        bedStateChangeService.recordManual(
                bed,
                BedStateAxis.READINESS,
                previousStatus.name(),
                newReadinessStatus.name(),
                reasonCode,
                note);
        bed.setReadinessStatus(newReadinessStatus);
        BedEntity saved = bedRepository.save(bed);
        auditBedChange(
                saved,
                auditAction,
                auditLabel + " du lit " + saved.getBedNumber()
                        + " : " + previousStatus + " → " + newReadinessStatus
                        + " | motif=" + reasonCode);
        return BedResponse.fromEntity(saved, false);
    }

    private BedEntity requireBed(UUID bedId) {
        return bedRepository.findById(bedId)
                .orElseThrow(() -> notFound("Lit introuvable."));
    }

    private void auditBedChange(BedEntity bed, String action, String reason) {
        UserAccountEntity actor = getCurrentUser();
        if (actor == null) {
            return;
        }
        auditService.logSuccess(
                actor.getId(),
                actor.getOrganizationId(),
                null,
                "SPATIAL",
                bed.getId(),
                action,
                reason);
    }

    private UserAccountEntity getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userAccountRepository.findByEmail(auth.getName().trim().toLowerCase()).orElse(null);
        }
        return null;
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private record BedMetrics(
            int installedBeds,
            int occupiedBeds,
            int availableBeds,
            int openBeds,
            int readyBeds,
            List<BedResponse> bedResponses) {
    }
}
