package com.joprelys.backend.spatial.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.spatial.api.BedResponse;
import com.joprelys.backend.spatial.api.RoomConfigurationResponse;
import com.joprelys.backend.spatial.api.RoomResponse;
import com.joprelys.backend.spatial.api.SaveBedRequest;
import com.joprelys.backend.spatial.api.SaveRoomRequest;
import com.joprelys.backend.spatial.api.SaveWardRequest;
import com.joprelys.backend.spatial.api.SpatialConfigurationResponse;
import com.joprelys.backend.spatial.api.WardConfigurationResponse;
import com.joprelys.backend.spatial.api.WardResponse;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.RoomEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.RoomRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.WardEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.WardRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DefaultSpatialConfigurationService implements SpatialConfigurationUseCase {

    private final WardRepository wardRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final BedAssignmentRepository bedAssignmentRepository;
    private final UserAccountRepository userAccountRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;
    private final TransactionTemplate transactionTemplate;

    public DefaultSpatialConfigurationService(
            WardRepository wardRepository,
            RoomRepository roomRepository,
            BedRepository bedRepository,
            BedAssignmentRepository bedAssignmentRepository,
            UserAccountRepository userAccountRepository,
            OrganizationRepository organizationRepository,
            AuditService auditService,
            PlatformTransactionManager transactionManager) {
        this.wardRepository = wardRepository;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.bedAssignmentRepository = bedAssignmentRepository;
        this.userAccountRepository = userAccountRepository;
        this.organizationRepository = organizationRepository;
        this.auditService = auditService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public SpatialConfigurationResponse getConfiguration(UUID requestedOrganizationId) {
        return inOrganizationScope(requestedOrganizationId, scope -> getConfigurationInScope());
    }

    private SpatialConfigurationResponse getConfigurationInScope() {
        List<WardConfigurationResponse> wards = wardRepository.findAllByOrderByNameAsc().stream()
                .map(this::toConfiguration)
                .toList();
        return new SpatialConfigurationResponse(wards);
    }

    @Override
    public WardResponse createWard(UUID requestedOrganizationId, SaveWardRequest request) {
        return inOrganizationScope(requestedOrganizationId, scope -> createWardInScope(scope, request));
    }

    private WardResponse createWardInScope(OrganizationScope scope, SaveWardRequest request) {
        String name = normalize(request.name());
        reject(wardRepository.existsByNameIgnoreCase(name), "Un service portant ce nom existe déjà.");

        WardEntity ward = new WardEntity(name);
        ward.setOrganizationId(scope.organizationId());
        WardEntity saved = wardRepository.saveAndFlush(ward);
        audit(scope, "WARD", saved.getId(), "CREATE", "Création du service " + saved.getName());
        return WardResponse.fromEntity(saved);
    }

    @Override
    public WardResponse updateWard(UUID requestedOrganizationId, UUID id, SaveWardRequest request) {
        return inOrganizationScope(requestedOrganizationId, scope -> updateWardInScope(scope, id, request));
    }

    private WardResponse updateWardInScope(OrganizationScope scope, UUID id, SaveWardRequest request) {
        WardEntity ward = findWard(id);
        String name = normalize(request.name());
        reject(wardRepository.existsByNameIgnoreCaseAndIdNot(name, id), "Un service portant ce nom existe déjà.");

        ward.setName(name);
        WardEntity saved = wardRepository.saveAndFlush(ward);
        audit(scope, "WARD", saved.getId(), "UPDATE", "Modification du service " + saved.getName());
        return WardResponse.fromEntity(saved);
    }

    @Override
    public void deleteWard(UUID requestedOrganizationId, UUID id) {
        inOrganizationScope(requestedOrganizationId, scope -> {
            deleteWardInScope(scope, id);
            return null;
        });
    }

    private void deleteWardInScope(OrganizationScope scope, UUID id) {
        WardEntity ward = findWard(id);
        reject(roomRepository.existsByWardId(id), "Ce service contient encore des chambres.");
        wardRepository.delete(ward);
        wardRepository.flush();
        audit(scope, "WARD", id, "DELETE", "Suppression du service " + ward.getName());
    }

    @Override
    public RoomResponse createRoom(UUID requestedOrganizationId, SaveRoomRequest request) {
        return inOrganizationScope(requestedOrganizationId, scope -> createRoomInScope(scope, request));
    }

    private RoomResponse createRoomInScope(OrganizationScope scope, SaveRoomRequest request) {
        WardEntity ward = findWard(request.wardId());
        String number = normalize(request.roomNumber());
        rejectRoomDuplicate(ward.getId(), number, null);

        RoomEntity room = new RoomEntity(ward, number, request.capacity(), normalizeUpper(request.comfortLevel()));
        room.setOrganizationId(scope.organizationId());
        RoomEntity saved = roomRepository.saveAndFlush(room);
        audit(scope, "ROOM", saved.getId(), "CREATE", "Création de la chambre " + saved.getRoomNumber());
        return RoomResponse.fromEntity(saved);
    }

    @Override
    public RoomResponse updateRoom(UUID requestedOrganizationId, UUID id, SaveRoomRequest request) {
        return inOrganizationScope(requestedOrganizationId, scope -> updateRoomInScope(scope, id, request));
    }

    private RoomResponse updateRoomInScope(OrganizationScope scope, UUID id, SaveRoomRequest request) {
        RoomEntity room = findRoom(id);
        WardEntity ward = findWard(request.wardId());
        String number = normalize(request.roomNumber());
        rejectRoomDuplicate(ward.getId(), number, id);
        reject(request.capacity() < bedRepository.countByRoomId(id),
                "La capacité ne peut pas être inférieure au nombre de lits configurés.");

        room.setWard(ward);
        room.setRoomNumber(number);
        room.setCapacity(request.capacity());
        room.setComfortLevel(normalizeUpper(request.comfortLevel()));
        RoomEntity saved = roomRepository.saveAndFlush(room);
        audit(scope, "ROOM", saved.getId(), "UPDATE", "Modification de la chambre " + saved.getRoomNumber());
        return RoomResponse.fromEntity(saved);
    }

    @Override
    public void deleteRoom(UUID requestedOrganizationId, UUID id) {
        inOrganizationScope(requestedOrganizationId, scope -> {
            deleteRoomInScope(scope, id);
            return null;
        });
    }

    private void deleteRoomInScope(OrganizationScope scope, UUID id) {
        RoomEntity room = findRoom(id);
        reject(bedRepository.existsByRoomId(id), "Cette chambre contient encore des lits.");
        roomRepository.delete(room);
        roomRepository.flush();
        audit(scope, "ROOM", id, "DELETE", "Suppression de la chambre " + room.getRoomNumber());
    }

    @Override
    public BedResponse createBed(UUID requestedOrganizationId, SaveBedRequest request) {
        return inOrganizationScope(requestedOrganizationId, scope -> createBedInScope(scope, request));
    }

    private BedResponse createBedInScope(OrganizationScope scope, SaveBedRequest request) {
        RoomEntity room = findRoom(request.roomId());
        String number = normalize(request.bedNumber());
        rejectBedDuplicate(room.getId(), number, null);
        rejectCapacityExceeded(room, null);

        BedEntity bed = new BedEntity(room, number);
        bed.setOrganizationId(scope.organizationId());
        BedEntity saved = bedRepository.saveAndFlush(bed);
        audit(scope, "BED", saved.getId(), "CREATE", "Création du lit " + saved.getBedNumber());
        return BedResponse.fromEntity(saved);
    }

    @Override
    public BedResponse updateBed(UUID requestedOrganizationId, UUID id, SaveBedRequest request) {
        return inOrganizationScope(requestedOrganizationId, scope -> updateBedInScope(scope, id, request));
    }

    private BedResponse updateBedInScope(OrganizationScope scope, UUID id, SaveBedRequest request) {
        BedEntity bed = findBed(id);
        RoomEntity targetRoom = findRoom(request.roomId());
        String number = normalize(request.bedNumber());
        rejectBedMoveWhenUsed(bed, targetRoom);
        rejectBedDuplicate(targetRoom.getId(), number, id);
        rejectCapacityExceeded(targetRoom, bed);

        bed.setRoom(targetRoom);
        bed.setBedNumber(number);
        BedEntity saved = bedRepository.saveAndFlush(bed);
        audit(scope, "BED", saved.getId(), "UPDATE", "Modification du lit " + saved.getBedNumber());
        return BedResponse.fromEntity(saved);
    }

    @Override
    public void deleteBed(UUID requestedOrganizationId, UUID id) {
        inOrganizationScope(requestedOrganizationId, scope -> {
            deleteBedInScope(scope, id);
            return null;
        });
    }

    private void deleteBedInScope(OrganizationScope scope, UUID id) {
        BedEntity bed = findBed(id);
        reject(bed.getStatus() == BedStatus.OCCUPIED, "Un lit occupé ne peut pas être supprimé.");
        reject(bedAssignmentRepository.existsByBedId(id), "Ce lit possède un historique d'affectation.");
        bedRepository.delete(bed);
        bedRepository.flush();
        audit(scope, "BED", id, "DELETE", "Suppression du lit " + bed.getBedNumber());
    }

    private WardConfigurationResponse toConfiguration(WardEntity ward) {
        List<RoomConfigurationResponse> rooms = roomRepository.findByWardId(ward.getId()).stream()
                .sorted(Comparator.comparing(RoomEntity::getRoomNumber, String.CASE_INSENSITIVE_ORDER))
                .map(this::toConfiguration)
                .toList();
        return new WardConfigurationResponse(ward.getId(), ward.getName(), rooms);
    }

    private RoomConfigurationResponse toConfiguration(RoomEntity room) {
        List<BedResponse> beds = bedRepository.findByRoomId(room.getId()).stream()
                .sorted(Comparator.comparing(BedEntity::getBedNumber, String.CASE_INSENSITIVE_ORDER))
                .map(BedResponse::fromEntity)
                .toList();
        return new RoomConfigurationResponse(
                room.getId(), room.getWard().getId(), room.getRoomNumber(),
                room.getCapacity(), room.getComfortLevel(), beds);
    }

    private void rejectRoomDuplicate(UUID wardId, String number, UUID excludedId) {
        boolean duplicate = excludedId == null
                ? roomRepository.existsByWardIdAndRoomNumberIgnoreCase(wardId, number)
                : roomRepository.existsByWardIdAndRoomNumberIgnoreCaseAndIdNot(wardId, number, excludedId);
        reject(duplicate, "Une chambre portant ce numéro existe déjà dans ce service.");
    }

    private void rejectBedDuplicate(UUID roomId, String number, UUID excludedId) {
        boolean duplicate = excludedId == null
                ? bedRepository.existsByRoomIdAndBedNumberIgnoreCase(roomId, number)
                : bedRepository.existsByRoomIdAndBedNumberIgnoreCaseAndIdNot(roomId, number, excludedId);
        reject(duplicate, "Un lit portant ce numéro existe déjà dans cette chambre.");
    }

    private void rejectCapacityExceeded(RoomEntity room, BedEntity editedBed) {
        long currentBeds = bedRepository.countByRoomId(room.getId());
        boolean remainsInRoom = editedBed != null && editedBed.getRoom().getId().equals(room.getId());
        reject(!remainsInRoom && currentBeds >= room.getCapacity(), "La capacité de cette chambre est atteinte.");
    }

    private void rejectBedMoveWhenUsed(BedEntity bed, RoomEntity targetRoom) {
        if (bed.getRoom().getId().equals(targetRoom.getId())) {
            return;
        }
        reject(bed.getStatus() == BedStatus.OCCUPIED || bedAssignmentRepository.existsByBedId(bed.getId()),
                "Un lit utilisé ne peut pas être déplacé vers une autre chambre.");
    }

    private WardEntity findWard(UUID id) {
        return wardRepository.findById(id)
                .orElseThrow(() -> notFound("Service introuvable."));
    }

    private RoomEntity findRoom(UUID id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> notFound("Chambre introuvable."));
    }

    private BedEntity findBed(UUID id) {
        return bedRepository.findById(id)
                .orElseThrow(() -> notFound("Lit introuvable."));
    }

    private <T> T inOrganizationScope(UUID requestedOrganizationId, Function<OrganizationScope, T> action) {
        UserAccountEntity actor = currentActor();
        UUID organizationId = resolveOrganizationId(actor, requestedOrganizationId);
        UUID previousTenant = TenantContext.getTenantId();
        TenantContext.setTenantId(organizationId);
        try {
            return transactionTemplate.execute(status -> action.apply(new OrganizationScope(actor, organizationId)));
        } finally {
            if (previousTenant == null) {
                TenantContext.clear();
            } else {
                TenantContext.setTenantId(previousTenant);
            }
        }
    }

    private UUID resolveOrganizationId(UserAccountEntity actor, UUID requestedOrganizationId) {
        if (isPlatformAdministrator(actor)) {
            if (requestedOrganizationId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sélectionnez une clinique à administrer.");
            }
            if (!organizationRepository.existsById(requestedOrganizationId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Clinique introuvable.");
            }
            return requestedOrganizationId;
        }
        if (actor.getOrganizationId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Utilisateur non rattaché à une clinique.");
        }
        if (requestedOrganizationId != null && !actor.getOrganizationId().equals(requestedOrganizationId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous ne pouvez pas administrer une autre clinique.");
        }
        return actor.getOrganizationId();
    }

    private boolean isPlatformAdministrator(UserAccountEntity actor) {
        return List.of(actor.getRole().split(",")).stream()
                .map(String::trim)
                .anyMatch(role -> role.equals("ADMIN_JOPRELYS") || role.equals("SUPER_ADMIN"));
    }

    private UserAccountEntity currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise.");
        }
        return userAccountRepository.findByEmail(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .filter(UserAccountEntity::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise."));
    }

    private void audit(OrganizationScope scope, String type, UUID resourceId, String action, String reason) {
        auditService.logSuccess(
                scope.actor().getId(), scope.organizationId(), null, type, resourceId, action, reason);
    }

    private void reject(boolean condition, String message) {
        if (condition) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeUpper(String value) {
        return normalize(value).toUpperCase(Locale.ROOT);
    }

    private record OrganizationScope(UserAccountEntity actor, UUID organizationId) {
    }
}
