package com.joprelys.backend.spatial.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.spatial.api.BedAssignmentResponse;
import com.joprelys.backend.spatial.api.BedResponse;
import com.joprelys.backend.spatial.api.RoomOccupancyResponse;
import com.joprelys.backend.spatial.api.WardOccupancyResponse;
import com.joprelys.backend.spatial.api.WardResponse;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedCapacityStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedReadinessStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.RoomEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.RoomRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.WardEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.WardRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SpatialService {

    private final WardRepository wardRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final BedAssignmentRepository bedAssignmentRepository;
    private final ActiveBedAssignmentService activeBedAssignmentService;
    private final BedStatusTransitionPolicy bedStatusTransitionPolicy;
    private final HospitalizationRepository hospitalizationRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public SpatialService(
            WardRepository wardRepository,
            RoomRepository roomRepository,
            BedRepository bedRepository,
            BedAssignmentRepository bedAssignmentRepository,
            ActiveBedAssignmentService activeBedAssignmentService,
            BedStatusTransitionPolicy bedStatusTransitionPolicy,
            HospitalizationRepository hospitalizationRepository,
            UserAccountRepository userAccountRepository,
            AuditService auditService) {
        this.wardRepository = wardRepository;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.bedAssignmentRepository = bedAssignmentRepository;
        this.activeBedAssignmentService = activeBedAssignmentService;
        this.bedStatusTransitionPolicy = bedStatusTransitionPolicy;
        this.hospitalizationRepository = hospitalizationRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<WardResponse> listWards() {
        return wardRepository.findAll().stream()
                .map(WardResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public WardOccupancyResponse getWardOccupancy(UUID wardId) {
        WardEntity ward = wardRepository.findById(wardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service introuvable"));
        ward.requireRoomsAllowed();

        List<RoomEntity> rooms = roomRepository.findByWardId(wardId);
        List<BedEntity> wardBeds = bedRepository.findByWardId(wardId);
        Map<UUID, List<BedEntity>> bedsByRoom = wardBeds.stream()
                .collect(Collectors.groupingBy(bed -> bed.getRoom().getId()));
        Set<UUID> activeBedIds = wardBeds.isEmpty()
                ? Set.of()
                : new HashSet<>(bedAssignmentRepository.findActiveBedIds(
                        wardBeds.stream().map(BedEntity::getId).toList()));

        List<RoomOccupancyResponse> roomOccupancyResponses = new ArrayList<>();
        int installedBeds = 0;
        int openBeds = 0;
        int readyBeds = 0;
        int occupiedBeds = 0;
        int availableBeds = 0;

        for (RoomEntity room : rooms) {
            List<BedEntity> beds = bedsByRoom.getOrDefault(room.getId(), List.of());
            List<BedResponse> bedResponses = new ArrayList<>();

            for (BedEntity bed : beds) {
                boolean hasActiveAssignment = activeBedIds.contains(bed.getId());
                installedBeds++;
                if (bed.isOpen()) {
                    openBeds++;
                }
                if (bed.isOpen() && bed.isReady()) {
                    readyBeds++;
                }
                if (hasActiveAssignment) {
                    occupiedBeds++;
                }
                if (bed.isOperationallyAvailable(hasActiveAssignment)) {
                    availableBeds++;
                }
                bedResponses.add(BedResponse.fromEntity(bed, hasActiveAssignment));
            }

            roomOccupancyResponses.add(new RoomOccupancyResponse(
                    room.getId(),
                    room.getRoomNumber(),
                    room.getCapacity(),
                    room.getComfortLevel(),
                    bedResponses));
        }

        return new WardOccupancyResponse(
                ward.getId(),
                ward.getName(),
                roomOccupancyResponses,
                installedBeds,
                occupiedBeds,
                availableBeds,
                openBeds,
                readyBeds);
    }

    @Transactional
    public BedResponse updateBedStatus(UUID bedId, BedStatus newStatus) {
        BedEntity bed = requireBed(bedId);
        boolean hasActiveAssignment = bedAssignmentRepository.findActiveByBedId(bedId).isPresent();
        bedStatusTransitionPolicy.validateManualTransition(
                bed.getStatus(),
                newStatus,
                bed.getCapacityStatus(),
                hasActiveAssignment);

        return persistReadinessChange(
                bed,
                newStatus,
                hasActiveAssignment,
                "UPDATE_BED_READINESS",
                "État opérationnel supervisé");
    }

    @Transactional
    public BedResponse updateBedCleaningStatus(UUID bedId, BedReadinessStatus newReadinessStatus) {
        BedEntity bed = requireBed(bedId);
        boolean hasActiveAssignment = bedAssignmentRepository.findActiveByBedId(bedId).isPresent();
        bedStatusTransitionPolicy.validateCleaningTransition(
                bed.getStatus(),
                bed.getReadinessStatus(),
                newReadinessStatus,
                bed.getCapacityStatus(),
                hasActiveAssignment);

        return persistReadinessChange(
                bed,
                legacyStatus(newReadinessStatus),
                hasActiveAssignment,
                "UPDATE_BED_CLEANING",
                "Circuit de nettoyage");
    }

    @Transactional
    public BedResponse updateBedMaintenanceStatus(UUID bedId, BedReadinessStatus newReadinessStatus) {
        BedEntity bed = requireBed(bedId);
        boolean hasActiveAssignment = bedAssignmentRepository.findActiveByBedId(bedId).isPresent();
        bedStatusTransitionPolicy.validateMaintenanceTransition(
                bed.getStatus(),
                bed.getReadinessStatus(),
                newReadinessStatus,
                bed.getCapacityStatus(),
                hasActiveAssignment);

        return persistReadinessChange(
                bed,
                legacyStatus(newReadinessStatus),
                hasActiveAssignment,
                "UPDATE_BED_MAINTENANCE",
                "Circuit de maintenance");
    }

    @Transactional
    public BedResponse updateBedCapacityStatus(UUID bedId, BedCapacityStatus newCapacityStatus) {
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
        bed.setCapacityStatus(newCapacityStatus);
        BedEntity saved = bedRepository.save(bed);
        auditBedChange(
                saved,
                "UPDATE_BED_CAPACITY",
                "Capacité du lit " + saved.getBedNumber()
                        + " changée de " + previousStatus + " à " + newCapacityStatus);
        return BedResponse.fromEntity(saved, false);
    }

    @Transactional
    public BedAssignmentResponse transferPatient(UUID hospitalizationId, UUID newBedId) {
        HospitalizationEntity hospitalization = hospitalizationRepository.findById(hospitalizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospitalisation introuvable"));

        if (!"EN_COURS".equals(hospitalization.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'hospitalisation n'est pas active.");
        }

        BedEntity targetBed = bedRepository.findById(newBedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nouveau lit introuvable"));
        targetBed.getRoom().getWard().requireRoomsAllowed();
        if (!Objects.equals(hospitalization.getOrganizationId(), targetBed.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nouveau lit introuvable");
        }

        int claimed = bedRepository.claimIfAvailable(
                newBedId,
                BedStatus.FREE,
                BedStatus.OCCUPIED,
                BedCapacityStatus.OPEN,
                BedReadinessStatus.READY);
        if (claimed != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le lit demandé n'est pas ouvert, prêt et disponible.");
        }
        BedEntity occupiedBed = bedRepository.findById(newBedId)
                .orElseThrow(() -> new IllegalStateException(
                        "Le lit réservé a disparu pendant la transaction de transfert."));

        bedAssignmentRepository.findActiveByHospitalizationId(hospitalizationId).ifPresent(oldAssignment -> {
            oldAssignment.releaseAt(Instant.now());
            bedAssignmentRepository.saveAndFlush(oldAssignment);

            BedEntity oldBed = oldAssignment.getBed();
            oldBed.setStatus(BedStatus.CLEANING);
            bedRepository.save(oldBed);
        });

        hospitalization.setServiceName(occupiedBed.getRoom().getWard().getName());
        hospitalization.setRoomNumber(occupiedBed.getRoom().getRoomNumber());
        hospitalization.setBedNumber(occupiedBed.getBedNumber());
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
                            + " | Chambre : " + occupiedBed.getRoom().getRoomNumber()
                            + " | Service : " + occupiedBed.getRoom().getWard().getName());
        }

        return BedAssignmentResponse.fromEntity(savedAssignment);
    }

    private BedResponse persistReadinessChange(
            BedEntity bed,
            BedStatus newStatus,
            boolean hasActiveAssignment,
            String auditAction,
            String auditLabel) {
        if (bed.getStatus() == newStatus) {
            return BedResponse.fromEntity(bed, hasActiveAssignment);
        }

        BedStatus previousStatus = bed.getStatus();
        bed.setStatus(newStatus);
        BedEntity saved = bedRepository.save(bed);
        auditBedChange(
                saved,
                auditAction,
                auditLabel + " du lit " + saved.getBedNumber()
                        + " : " + previousStatus + " → " + newStatus);
        return BedResponse.fromEntity(saved, false);
    }

    private BedStatus legacyStatus(BedReadinessStatus readinessStatus) {
        return switch (readinessStatus) {
            case READY -> BedStatus.FREE;
            case CLEANING -> BedStatus.CLEANING;
            case MAINTENANCE -> BedStatus.MAINTENANCE;
        };
    }

    private BedEntity requireBed(UUID bedId) {
        BedEntity bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lit introuvable"));
        bed.getRoom().getWard().requireRoomsAllowed();
        return bed;
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
}
