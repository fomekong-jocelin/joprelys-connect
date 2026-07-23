package com.joprelys.backend.spatial.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.spatial.api.RoomConfigurationResponse;
import com.joprelys.backend.spatial.api.RoomResponse;
import com.joprelys.backend.spatial.api.SaveRoomRequest;
import com.joprelys.backend.spatial.api.SaveWardRequest;
import com.joprelys.backend.spatial.api.SpatialConfigurationResponse;
import com.joprelys.backend.spatial.api.WardConfigurationResponse;
import com.joprelys.backend.spatial.api.WardResponse;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DefaultSpatialConfigurationService implements SpatialConfigurationUseCase {

    private final WardRepository wardRepository;
    private final RoomRepository roomRepository;
    private final UserAccountRepository userAccountRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;
    private final TransactionTemplate transactionTemplate;

    public DefaultSpatialConfigurationService(
            WardRepository wardRepository,
            RoomRepository roomRepository,
            UserAccountRepository userAccountRepository,
            OrganizationRepository organizationRepository,
            AuditService auditService,
            PlatformTransactionManager transactionManager) {
        this.wardRepository = wardRepository;
        this.roomRepository = roomRepository;
        this.userAccountRepository = userAccountRepository;
        this.organizationRepository = organizationRepository;
        this.auditService = auditService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public SpatialConfigurationResponse getConfiguration(UUID requestedOrganizationId) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            List<WardConfigurationResponse> wards = wardRepository.findAllByOrderByNameAsc().stream()
                    .map(this::toConfiguration)
                    .toList();
            return new SpatialConfigurationResponse(wards);
        });
    }

    @Override
    public WardResponse createWard(UUID requestedOrganizationId, SaveWardRequest request) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            String name = normalize(request.name());
            reject(wardRepository.existsByNameIgnoreCase(name), "Un service portant ce nom existe déjà.");
            WardEntity ward = new WardEntity(name, request.serviceType());
            ward.setOrganizationId(scope.organizationId());
            WardEntity saved = wardRepository.saveAndFlush(ward);
            audit(scope, "WARD", saved.getId(), "CREATE", serviceAuditReason("Création", saved));
            return WardResponse.fromEntity(saved);
        });
    }

    @Override
    public WardResponse updateWard(UUID requestedOrganizationId, UUID id, SaveWardRequest request) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            WardEntity ward = findWard(id);
            String name = normalize(request.name());
            reject(
                    wardRepository.existsByNameIgnoreCaseAndIdNot(name, id),
                    "Un service portant ce nom existe déjà.");
            ward.changeServiceType(request.serviceType(), roomRepository.existsByWardId(id));
            ward.setName(name);
            WardEntity saved = wardRepository.saveAndFlush(ward);
            audit(scope, "WARD", saved.getId(), "UPDATE", serviceAuditReason("Modification", saved));
            return WardResponse.fromEntity(saved);
        });
    }

    @Override
    public void deleteWard(UUID requestedOrganizationId, UUID id) {
        inOrganizationScope(requestedOrganizationId, scope -> {
            WardEntity ward = findWard(id);
            reject(roomRepository.existsByWardId(id), "Ce service contient encore des chambres.");
            wardRepository.delete(ward);
            wardRepository.flush();
            audit(scope, "WARD", id, "DELETE", "Suppression du service " + ward.getName());
            return null;
        });
    }

    @Override
    public RoomResponse createRoom(UUID requestedOrganizationId, SaveRoomRequest request) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            WardEntity ward = findWard(request.wardId());
            ward.requireRoomsAllowed();
            String number = normalize(request.roomNumber());
            rejectRoomDuplicate(ward.getId(), number, null);
            RoomEntity room = new RoomEntity(ward, number, request.capacity(), normalizeUpper(request.comfortLevel()));
            room.setOrganizationId(scope.organizationId());
            RoomEntity saved = roomRepository.saveAndFlush(room);
            audit(scope, "ROOM", saved.getId(), "CREATE", "Création de la chambre " + saved.getRoomNumber());
            return RoomResponse.fromEntity(saved);
        });
    }

    @Override
    public RoomResponse updateRoom(UUID requestedOrganizationId, UUID id, SaveRoomRequest request) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            RoomEntity room = findRoom(id);
            WardEntity ward = findWard(request.wardId());
            ward.requireRoomsAllowed();
            String number = normalize(request.roomNumber());
            rejectRoomDuplicate(ward.getId(), number, id);
            room.setWard(ward);
            room.setRoomNumber(number);
            room.setCapacity(request.capacity());
            room.setComfortLevel(normalizeUpper(request.comfortLevel()));
            RoomEntity saved = roomRepository.saveAndFlush(room);
            audit(scope, "ROOM", saved.getId(), "UPDATE", "Modification de la chambre " + saved.getRoomNumber());
            return RoomResponse.fromEntity(saved);
        });
    }

    @Override
    public void deleteRoom(UUID requestedOrganizationId, UUID id) {
        inOrganizationScope(requestedOrganizationId, scope -> {
            RoomEntity room = findRoom(id);
            roomRepository.delete(room);
            roomRepository.flush();
            audit(scope, "ROOM", id, "DELETE", "Suppression de la chambre " + room.getRoomNumber());
            return null;
        });
    }

    private WardConfigurationResponse toConfiguration(WardEntity ward) {
        List<RoomConfigurationResponse> rooms = roomRepository.findByWardId(ward.getId()).stream()
                .sorted(Comparator.comparing(RoomEntity::getRoomNumber, String.CASE_INSENSITIVE_ORDER))
                .map(this::toConfiguration)
                .toList();
        return new WardConfigurationResponse(
                ward.getId(), ward.getName(), ward.getServiceType(), ward.allowsRooms(), rooms);
    }

    private RoomConfigurationResponse toConfiguration(RoomEntity room) {
        return new RoomConfigurationResponse(
                room.getId(),
                room.getWard().getId(),
                room.getRoomNumber(),
                room.getCapacity(),
                room.getComfortLevel(),
                List.of());
    }

    private void rejectRoomDuplicate(UUID wardId, String number, UUID excludedId) {
        boolean duplicate = excludedId == null
                ? roomRepository.existsByWardIdAndRoomNumberIgnoreCase(wardId, number)
                : roomRepository.existsByWardIdAndRoomNumberIgnoreCaseAndIdNot(wardId, number, excludedId);
        reject(duplicate, "Une chambre portant ce numéro existe déjà dans ce service.");
    }

    private WardEntity findWard(UUID id) {
        return wardRepository.findById(id)
                .orElseThrow(() -> notFound("Service introuvable."));
    }

    private RoomEntity findRoom(UUID id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> notFound("Chambre introuvable."));
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
        if (isPlatformAdministrator()) {
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

    private boolean isPlatformAdministrator() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ORGANIZATION_MANAGE".equals(authority.getAuthority()));
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

    private String serviceAuditReason(String action, WardEntity ward) {
        return action + " du service " + ward.getName() + " [" + ward.getServiceType() + "]";
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
