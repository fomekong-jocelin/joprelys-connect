package com.joprelys.backend.spatial.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitRepository;
import com.joprelys.backend.spatial.api.HospitalLocationDtos.FacilityLocationResponse;
import com.joprelys.backend.spatial.api.HospitalLocationDtos.FacilitySpaceResponse;
import com.joprelys.backend.spatial.api.HospitalLocationDtos.LocationTypeResponse;
import com.joprelys.backend.spatial.api.HospitalLocationDtos.SaveFacilityLocationRequest;
import com.joprelys.backend.spatial.api.HospitalLocationDtos.SaveFacilitySpaceRequest;
import com.joprelys.backend.spatial.api.HospitalLocationDtos.SaveUnitSpaceAssignmentRequest;
import com.joprelys.backend.spatial.api.HospitalLocationDtos.SpaceTypeResponse;
import com.joprelys.backend.spatial.api.HospitalLocationDtos.UnitSpaceAssignmentResponse;
import com.joprelys.backend.spatial.domain.FacilityLocationNodeType;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilityLocationNodeEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilityLocationNodeRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceProfileEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceProfileRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceTypeCatalogRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.OrganizationalUnitSpaceAssignmentEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.OrganizationalUnitSpaceAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.SpaceTypeCatalogEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.SpaceTypeCatalogRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HospitalLocationConfigurationService {

    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]{1,63}");

    private final FacilityLocationNodeRepository locationRepository;
    private final FacilitySpaceRepository spaceRepository;
    private final SpaceTypeCatalogRepository spaceTypeRepository;
    private final InpatientSpaceTypeCatalogRepository inpatientTypeRepository;
    private final InpatientSpaceProfileRepository inpatientProfileRepository;
    private final OrganizationalUnitSpaceAssignmentRepository assignmentRepository;
    private final OrganizationalUnitRepository unitRepository;
    private final UserAccountRepository userAccountRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;
    private final TransactionTemplate transactionTemplate;

    public HospitalLocationConfigurationService(
            FacilityLocationNodeRepository locationRepository,
            FacilitySpaceRepository spaceRepository,
            SpaceTypeCatalogRepository spaceTypeRepository,
            InpatientSpaceTypeCatalogRepository inpatientTypeRepository,
            InpatientSpaceProfileRepository inpatientProfileRepository,
            OrganizationalUnitSpaceAssignmentRepository assignmentRepository,
            OrganizationalUnitRepository unitRepository,
            UserAccountRepository userAccountRepository,
            OrganizationRepository organizationRepository,
            AuditService auditService,
            PlatformTransactionManager transactionManager) {
        this.locationRepository = locationRepository;
        this.spaceRepository = spaceRepository;
        this.spaceTypeRepository = spaceTypeRepository;
        this.inpatientTypeRepository = inpatientTypeRepository;
        this.inpatientProfileRepository = inpatientProfileRepository;
        this.assignmentRepository = assignmentRepository;
        this.unitRepository = unitRepository;
        this.userAccountRepository = userAccountRepository;
        this.organizationRepository = organizationRepository;
        this.auditService = auditService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public List<LocationTypeResponse> listLocationTypes() {
        currentActor();
        return Arrays.stream(FacilityLocationNodeType.values())
                .map(type -> new LocationTypeResponse(type.name(), type.rank()))
                .toList();
    }

    public List<FacilityLocationResponse> listLocations(UUID requestedOrganizationId, boolean includeInactive) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            List<FacilityLocationNodeEntity> entities = includeInactive
                    ? locationRepository.findAllByOrganizationIdOrderByCodeAsc(scope.organizationId())
                    : locationRepository.findAllByOrganizationIdAndActiveTrueOrderByCodeAsc(scope.organizationId());
            return entities.stream().map(FacilityLocationResponse::fromEntity).toList();
        });
    }

    public FacilityLocationResponse createLocation(
            UUID requestedOrganizationId,
            SaveFacilityLocationRequest request) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            String code = normalizeCode(request.code());
            rejectConflict(
                    locationRepository.existsByOrganizationIdAndCodeIgnoreCase(scope.organizationId(), code),
                    "Ce code de localisation est déjà utilisé.");
            FacilityLocationNodeEntity parent = resolveParent(
                    scope.organizationId(), request.parentId(), request.nodeType(), null);
            FacilityLocationNodeEntity entity = new FacilityLocationNodeEntity(
                    scope.organizationId(),
                    parent == null ? null : parent.getId(),
                    code,
                    requireName(request.name()),
                    request.nodeType());
            FacilityLocationNodeEntity saved = locationRepository.saveAndFlush(entity);
            audit(scope, "FACILITY_LOCATION", saved.getId(), "CREATE", saved.getCode());
            return FacilityLocationResponse.fromEntity(saved);
        });
    }

    public FacilityLocationResponse updateLocation(
            UUID requestedOrganizationId,
            UUID locationId,
            SaveFacilityLocationRequest request) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            FacilityLocationNodeEntity entity = findLocation(locationId, scope.organizationId());
            if (entity.getNodeType() != request.nodeType()) {
                throw badRequest("Le type d'une localisation existante ne peut pas être modifié.");
            }
            String code = normalizeCode(request.code());
            rejectConflict(
                    locationRepository.existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(
                            scope.organizationId(), code, locationId),
                    "Ce code de localisation est déjà utilisé.");
            FacilityLocationNodeEntity parent = resolveParent(
                    scope.organizationId(), request.parentId(), request.nodeType(), locationId);
            entity.update(parent == null ? null : parent.getId(), code, requireName(request.name()));
            FacilityLocationNodeEntity saved = locationRepository.saveAndFlush(entity);
            audit(scope, "FACILITY_LOCATION", saved.getId(), "UPDATE", saved.getCode());
            return FacilityLocationResponse.fromEntity(saved);
        });
    }

    public FacilityLocationResponse setLocationActive(
            UUID requestedOrganizationId,
            UUID locationId,
            boolean active) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            FacilityLocationNodeEntity entity = findLocation(locationId, scope.organizationId());
            if (active) {
                validateLocationActivation(entity, scope.organizationId());
                entity.activate();
            } else {
                rejectConflict(
                        locationRepository.existsByOrganizationIdAndParentIdAndActiveTrue(
                                scope.organizationId(), locationId),
                        "Désactivez d'abord les localisations enfants actives.");
                rejectConflict(
                        spaceRepository.existsByOrganizationIdAndLocationNodeIdAndActiveTrue(
                                scope.organizationId(), locationId),
                        "Désactivez ou déplacez d'abord les espaces actifs de cette localisation.");
                entity.deactivate();
            }
            FacilityLocationNodeEntity saved = locationRepository.saveAndFlush(entity);
            audit(scope, "FACILITY_LOCATION", saved.getId(), active ? "ACTIVATE" : "DEACTIVATE", saved.getCode());
            return FacilityLocationResponse.fromEntity(saved);
        });
    }

    public List<SpaceTypeResponse> listSpaceTypes() {
        currentActor();
        return spaceTypeRepository.findAllByActiveTrueOrderByNameFrAsc().stream()
                .map(type -> new SpaceTypeResponse(
                        type.getCode(),
                        type.getNameFr(),
                        type.getNameEn(),
                        inpatientTypeRepository.existsById(type.getCode())))
                .toList();
    }

    public List<FacilitySpaceResponse> listSpaces(
            UUID requestedOrganizationId,
            UUID locationNodeId,
            boolean includeInactive) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            if (locationNodeId != null) {
                findLocation(locationNodeId, scope.organizationId());
            }
            List<FacilitySpaceEntity> spaces = locationNodeId == null
                    ? (includeInactive
                            ? spaceRepository.findAllByOrganizationIdOrderByCodeAsc(scope.organizationId())
                            : spaceRepository.findAllByOrganizationIdAndActiveTrueOrderByCodeAsc(scope.organizationId()))
                    : spaceRepository.findAllByOrganizationIdAndLocationNodeIdOrderByCodeAsc(
                            scope.organizationId(), locationNodeId).stream()
                            .filter(space -> includeInactive || space.isActive())
                            .toList();
            return spaces.stream().map(this::toSpaceResponse).toList();
        });
    }

    public FacilitySpaceResponse createSpace(
            UUID requestedOrganizationId,
            SaveFacilitySpaceRequest request) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            String code = normalizeCode(request.code());
            rejectConflict(
                    spaceRepository.existsByOrganizationIdAndCodeIgnoreCase(scope.organizationId(), code),
                    "Ce code d'espace est déjà utilisé.");
            FacilityLocationNodeEntity location = resolveLocation(
                    scope.organizationId(), request.locationNodeId(), true);
            SpaceTypeCatalogEntity spaceType = requireActiveSpaceType(request.spaceTypeCode());
            if (request.enableInpatientProfile()) {
                requireInpatientCompatible(spaceType.getCode());
            }
            FacilitySpaceEntity entity = new FacilitySpaceEntity(
                    scope.organizationId(),
                    location == null ? null : location.getId(),
                    code,
                    requireName(request.name()),
                    spaceType.getCode());
            FacilitySpaceEntity saved = spaceRepository.saveAndFlush(entity);
            if (request.enableInpatientProfile()) {
                inpatientProfileRepository.saveAndFlush(new InpatientSpaceProfileEntity(
                        saved.getId(), scope.organizationId(), saved.getSpaceTypeCode()));
            }
            audit(scope, "FACILITY_SPACE", saved.getId(), "CREATE", saved.getCode());
            return toSpaceResponse(saved);
        });
    }

    public FacilitySpaceResponse updateSpace(
            UUID requestedOrganizationId,
            UUID spaceId,
            SaveFacilitySpaceRequest request) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            FacilitySpaceEntity entity = findSpace(spaceId, scope.organizationId());
            String code = normalizeCode(request.code());
            rejectConflict(
                    spaceRepository.existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(
                            scope.organizationId(), code, spaceId),
                    "Ce code d'espace est déjà utilisé.");
            FacilityLocationNodeEntity location = resolveLocation(
                    scope.organizationId(), request.locationNodeId(), true);
            SpaceTypeCatalogEntity spaceType = requireActiveSpaceType(request.spaceTypeCode());
            boolean hasProfile = inpatientProfileRepository.existsBySpaceIdAndOrganizationId(
                    spaceId, scope.organizationId());
            if (hasProfile && !entity.getSpaceTypeCode().equals(spaceType.getCode())) {
                throw conflict("Le type d'un espace d'hébergement ne peut pas être changé tant que son profil existe.");
            }
            if (request.enableInpatientProfile()) {
                requireInpatientCompatible(spaceType.getCode());
            } else if (hasProfile) {
                throw conflict("Le retrait du profil d'hébergement sera autorisé après le cutover des lits vers spaceId.");
            }
            entity.update(
                    location == null ? null : location.getId(),
                    code,
                    requireName(request.name()),
                    spaceType.getCode());
            FacilitySpaceEntity saved = spaceRepository.saveAndFlush(entity);
            if (request.enableInpatientProfile() && !hasProfile) {
                inpatientProfileRepository.saveAndFlush(new InpatientSpaceProfileEntity(
                        saved.getId(), scope.organizationId(), saved.getSpaceTypeCode()));
            }
            audit(scope, "FACILITY_SPACE", saved.getId(), "UPDATE", saved.getCode());
            return toSpaceResponse(saved);
        });
    }

    public FacilitySpaceResponse setSpaceActive(
            UUID requestedOrganizationId,
            UUID spaceId,
            boolean active) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            FacilitySpaceEntity entity = findSpace(spaceId, scope.organizationId());
            if (active) {
                resolveLocation(scope.organizationId(), entity.getLocationNodeId(), true);
                requireActiveSpaceType(entity.getSpaceTypeCode());
                entity.activate();
            } else {
                boolean hasActiveAssignment = assignmentRepository
                        .findAllByOrganizationIdAndSpaceIdOrderByValidFromDesc(scope.organizationId(), spaceId)
                        .stream()
                        .anyMatch(assignment -> assignment.isActiveAt(Instant.now()));
                rejectConflict(hasActiveAssignment, "Clôturez d'abord les rattachements actifs de cet espace.");
                entity.deactivate();
            }
            FacilitySpaceEntity saved = spaceRepository.saveAndFlush(entity);
            audit(scope, "FACILITY_SPACE", saved.getId(), active ? "ACTIVATE" : "DEACTIVATE", saved.getCode());
            return toSpaceResponse(saved);
        });
    }

    public FacilitySpaceResponse enableInpatientProfile(UUID requestedOrganizationId, UUID spaceId) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            FacilitySpaceEntity space = findSpace(spaceId, scope.organizationId());
            if (!space.isActive()) {
                throw conflict("Un espace inactif ne peut pas recevoir un profil d'hébergement.");
            }
            requireInpatientCompatible(space.getSpaceTypeCode());
            if (!inpatientProfileRepository.existsBySpaceIdAndOrganizationId(spaceId, scope.organizationId())) {
                inpatientProfileRepository.saveAndFlush(new InpatientSpaceProfileEntity(
                        spaceId, scope.organizationId(), space.getSpaceTypeCode()));
                audit(scope, "FACILITY_SPACE", spaceId, "ENABLE_INPATIENT_PROFILE", space.getCode());
            }
            return toSpaceResponse(space);
        });
    }

    public List<UnitSpaceAssignmentResponse> listAssignments(
            UUID requestedOrganizationId,
            UUID spaceId,
            UUID unitId,
            Instant activeAt) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            List<OrganizationalUnitSpaceAssignmentEntity> assignments;
            if (spaceId != null) {
                findSpace(spaceId, scope.organizationId());
                assignments = assignmentRepository.findAllByOrganizationIdAndSpaceIdOrderByValidFromDesc(
                        scope.organizationId(), spaceId);
            } else if (unitId != null) {
                findUnit(unitId, scope.organizationId());
                assignments = assignmentRepository.findAllByOrganizationIdAndOrganizationalUnitIdOrderByValidFromDesc(
                        scope.organizationId(), unitId);
            } else {
                assignments = assignmentRepository.findAllByOrganizationIdOrderByValidFromDesc(scope.organizationId());
            }
            return assignments.stream()
                    .filter(assignment -> unitId == null || unitId.equals(assignment.getOrganizationalUnitId()))
                    .filter(assignment -> activeAt == null || assignment.isActiveAt(activeAt))
                    .map(UnitSpaceAssignmentResponse::fromEntity)
                    .toList();
        });
    }

    public UnitSpaceAssignmentResponse createAssignment(
            UUID requestedOrganizationId,
            SaveUnitSpaceAssignmentRequest request) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            OrganizationalUnitEntity unit = findUnit(request.organizationalUnitId(), scope.organizationId());
            FacilitySpaceEntity space = findSpace(request.spaceId(), scope.organizationId());
            if (!unit.isActive() || !space.isActive()) {
                throw conflict("L'unité et l'espace doivent être actifs pour créer un rattachement.");
            }
            validatePeriod(request.validFrom(), request.validTo());
            rejectOverlap(scope.organizationId(), request, null);
            OrganizationalUnitSpaceAssignmentEntity entity = new OrganizationalUnitSpaceAssignmentEntity(
                    scope.organizationId(),
                    unit.getId(),
                    space.getId(),
                    request.validFrom(),
                    request.validTo());
            OrganizationalUnitSpaceAssignmentEntity saved = assignmentRepository.saveAndFlush(entity);
            audit(scope, "UNIT_SPACE_ASSIGNMENT", saved.getId(), "CREATE", unit.getCode() + " -> " + space.getCode());
            return UnitSpaceAssignmentResponse.fromEntity(saved);
        });
    }

    public UnitSpaceAssignmentResponse updateAssignment(
            UUID requestedOrganizationId,
            UUID assignmentId,
            SaveUnitSpaceAssignmentRequest request) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            OrganizationalUnitSpaceAssignmentEntity entity = assignmentRepository
                    .findByIdAndOrganizationId(assignmentId, scope.organizationId())
                    .orElseThrow(() -> notFound("Rattachement unité-espace introuvable."));
            if (!entity.getOrganizationalUnitId().equals(request.organizationalUnitId())
                    || !entity.getSpaceId().equals(request.spaceId())) {
                throw badRequest("L'unité et l'espace d'un rattachement existant ne peuvent pas être remplacés.");
            }
            validatePeriod(request.validFrom(), request.validTo());
            rejectOverlap(scope.organizationId(), request, assignmentId);
            entity.updatePeriod(request.validFrom(), request.validTo());
            OrganizationalUnitSpaceAssignmentEntity saved = assignmentRepository.saveAndFlush(entity);
            audit(scope, "UNIT_SPACE_ASSIGNMENT", saved.getId(), "UPDATE", "Période mise à jour");
            return UnitSpaceAssignmentResponse.fromEntity(saved);
        });
    }

    private FacilityLocationNodeEntity resolveParent(
            UUID organizationId,
            UUID parentId,
            FacilityLocationNodeType childType,
            UUID editedId) {
        if (parentId == null) {
            return null;
        }
        if (parentId.equals(editedId)) {
            throw badRequest("Une localisation ne peut pas être son propre parent.");
        }
        FacilityLocationNodeEntity parent = findLocation(parentId, organizationId);
        if (!parent.isActive()) {
            throw badRequest("La localisation parente est inactive.");
        }
        if (!parent.getNodeType().canContain(childType)) {
            throw badRequest("Hiérarchie géographique invalide.");
        }
        if (editedId != null) {
            FacilityLocationNodeEntity current = parent;
            while (current != null) {
                if (editedId.equals(current.getId())) {
                    throw badRequest("Cette modification créerait un cycle géographique.");
                }
                current = current.getParentId() == null
                        ? null
                        : findLocation(current.getParentId(), organizationId);
            }
        }
        return parent;
    }

    private void validateLocationActivation(FacilityLocationNodeEntity entity, UUID organizationId) {
        if (entity.getParentId() != null) {
            FacilityLocationNodeEntity parent = findLocation(entity.getParentId(), organizationId);
            if (!parent.isActive()) {
                throw conflict("La localisation parente est inactive.");
            }
        }
    }

    private FacilityLocationNodeEntity resolveLocation(UUID organizationId, UUID locationId, boolean requireActive) {
        if (locationId == null) {
            return null;
        }
        FacilityLocationNodeEntity location = findLocation(locationId, organizationId);
        if (requireActive && !location.isActive()) {
            throw conflict("La localisation sélectionnée est inactive.");
        }
        return location;
    }

    private SpaceTypeCatalogEntity requireActiveSpaceType(String rawCode) {
        String code = normalizeCode(rawCode);
        return spaceTypeRepository.findById(code)
                .filter(SpaceTypeCatalogEntity::isActive)
                .orElseThrow(() -> badRequest("Type d'espace inconnu ou inactif."));
    }

    private void requireInpatientCompatible(String spaceTypeCode) {
        if (!inpatientTypeRepository.existsById(spaceTypeCode)) {
            throw badRequest("Ce type d'espace ne peut pas recevoir de lits.");
        }
    }

    private FacilitySpaceResponse toSpaceResponse(FacilitySpaceEntity entity) {
        return FacilitySpaceResponse.fromEntity(
                entity,
                inpatientProfileRepository.existsBySpaceIdAndOrganizationId(entity.getId(), entity.getOrganizationId()));
    }

    private void validatePeriod(Instant validFrom, Instant validTo) {
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw badRequest("La date de fin doit être strictement postérieure à la date de début.");
        }
    }

    private void rejectOverlap(
            UUID organizationId,
            SaveUnitSpaceAssignmentRequest request,
            UUID excludedId) {
        boolean overlap;
        if (excludedId == null) {
            overlap = request.validTo() == null
                    ? assignmentRepository.hasOverlapOpenEnded(
                            organizationId,
                            request.organizationalUnitId(),
                            request.spaceId(),
                            request.validFrom())
                    : assignmentRepository.hasOverlapBounded(
                            organizationId,
                            request.organizationalUnitId(),
                            request.spaceId(),
                            request.validFrom(),
                            request.validTo());
        } else {
            overlap = request.validTo() == null
                    ? assignmentRepository.hasOverlapOpenEndedExcluding(
                            organizationId,
                            request.organizationalUnitId(),
                            request.spaceId(),
                            excludedId,
                            request.validFrom())
                    : assignmentRepository.hasOverlapBoundedExcluding(
                            organizationId,
                            request.organizationalUnitId(),
                            request.spaceId(),
                            excludedId,
                            request.validFrom(),
                            request.validTo());
        }
        rejectConflict(overlap, "Cette période chevauche déjà un rattachement pour la même unité et le même espace.");
    }

    private FacilityLocationNodeEntity findLocation(UUID id, UUID organizationId) {
        return locationRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> notFound("Localisation introuvable."));
    }

    private FacilitySpaceEntity findSpace(UUID id, UUID organizationId) {
        return spaceRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> notFound("Espace introuvable."));
    }

    private OrganizationalUnitEntity findUnit(UUID id, UUID organizationId) {
        return unitRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> notFound("Unité organisationnelle introuvable."));
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
                throw badRequest("Sélectionnez un établissement à administrer.");
            }
            if (!organizationRepository.existsById(requestedOrganizationId)) {
                throw notFound("Établissement introuvable.");
            }
            return requestedOrganizationId;
        }
        if (actor.getOrganizationId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Utilisateur non rattaché à un établissement.");
        }
        if (requestedOrganizationId != null && !actor.getOrganizationId().equals(requestedOrganizationId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous ne pouvez pas administrer un autre établissement.");
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
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise.");
        }
        return userAccountRepository.findByEmail(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .filter(UserAccountEntity::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise."));
    }

    private void audit(OrganizationScope scope, String entityType, UUID entityId, String action, String details) {
        auditService.logSuccess(
                scope.actor().getId(),
                scope.organizationId(),
                null,
                entityType,
                entityId,
                action,
                details);
    }

    private String normalizeCode(String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw badRequest("Le code doit contenir 2 à 64 caractères A-Z, 0-9 ou _, et commencer par une lettre.");
        }
        return code;
    }

    private String requireName(String value) {
        if (value == null || value.isBlank()) {
            throw badRequest("Le nom est obligatoire.");
        }
        return value.trim();
    }

    private void rejectConflict(boolean condition, String message) {
        if (condition) {
            throw conflict(message);
        }
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

    private record OrganizationScope(UserAccountEntity actor, UUID organizationId) {
    }
}
