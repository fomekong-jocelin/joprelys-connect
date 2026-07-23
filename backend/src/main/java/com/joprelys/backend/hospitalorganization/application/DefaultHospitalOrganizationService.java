package com.joprelys.backend.hospitalorganization.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.hospitalorganization.api.HospitalServiceCatalogResponse;
import com.joprelys.backend.hospitalorganization.api.MedicalSpecialtyCatalogResponse;
import com.joprelys.backend.hospitalorganization.api.OrganizationalUnitResponse;
import com.joprelys.backend.hospitalorganization.api.SaveOrganizationalUnitRequest;
import com.joprelys.backend.hospitalorganization.domain.OrganizationalUnitType;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.HospitalServiceCatalogEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.HospitalServiceCatalogRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.MedicalSpecialtyCatalogRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
public class DefaultHospitalOrganizationService implements HospitalOrganizationUseCase {

    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]{2,63}");
    private static final Set<OrganizationalUnitType> ROOT_TYPES = EnumSet.of(
            OrganizationalUnitType.POLE,
            OrganizationalUnitType.DEPARTMENT,
            OrganizationalUnitType.SERVICE);

    private final HospitalServiceCatalogRepository serviceCatalogRepository;
    private final MedicalSpecialtyCatalogRepository specialtyCatalogRepository;
    private final OrganizationalUnitRepository unitRepository;
    private final UserAccountRepository userAccountRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;
    private final TransactionTemplate transactionTemplate;

    public DefaultHospitalOrganizationService(
            HospitalServiceCatalogRepository serviceCatalogRepository,
            MedicalSpecialtyCatalogRepository specialtyCatalogRepository,
            OrganizationalUnitRepository unitRepository,
            UserAccountRepository userAccountRepository,
            OrganizationRepository organizationRepository,
            AuditService auditService,
            PlatformTransactionManager transactionManager) {
        this.serviceCatalogRepository = serviceCatalogRepository;
        this.specialtyCatalogRepository = specialtyCatalogRepository;
        this.unitRepository = unitRepository;
        this.userAccountRepository = userAccountRepository;
        this.organizationRepository = organizationRepository;
        this.auditService = auditService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public List<HospitalServiceCatalogResponse> listServiceCatalog() {
        currentActor();
        return serviceCatalogRepository.findAllByActiveTrueOrderByNameFrAsc().stream()
                .map(HospitalServiceCatalogResponse::fromEntity)
                .toList();
    }

    @Override
    public List<MedicalSpecialtyCatalogResponse> listSpecialtyCatalog() {
        currentActor();
        return specialtyCatalogRepository.findAllByActiveTrueOrderByNameFrAsc().stream()
                .map(MedicalSpecialtyCatalogResponse::fromEntity)
                .toList();
    }

    @Override
    public List<OrganizationalUnitResponse> listUnits(UUID requestedOrganizationId, boolean includeInactive) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            List<OrganizationalUnitEntity> units = includeInactive
                    ? unitRepository.findAllByOrganizationIdOrderByCodeAsc(scope.organizationId())
                    : unitRepository.findAllByOrganizationIdAndActiveTrueOrderByCodeAsc(scope.organizationId());
            return units.stream().map(OrganizationalUnitResponse::fromEntity).toList();
        });
    }

    @Override
    public OrganizationalUnitResponse createUnit(
            UUID requestedOrganizationId,
            SaveOrganizationalUnitRequest request) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            String code = normalizeCode(request.code());
            rejectConflict(
                    unitRepository.existsByOrganizationIdAndCodeIgnoreCase(scope.organizationId(), code),
                    "Ce code d'unité est déjà utilisé.");
            OrganizationalUnitEntity parent = resolveParent(
                    scope.organizationId(), request.parentId(), request.unitType(), null);
            ResolvedUnitIdentity identity = resolveIdentity(request);
            OrganizationalUnitEntity unit = new OrganizationalUnitEntity(
                    scope.organizationId(),
                    parent == null ? null : parent.getId(),
                    code,
                    identity.name(),
                    request.unitType(),
                    identity.serviceCatalogCode());
            OrganizationalUnitEntity saved = unitRepository.saveAndFlush(unit);
            audit(scope, saved, "CREATE");
            return OrganizationalUnitResponse.fromEntity(saved);
        });
    }

    @Override
    public OrganizationalUnitResponse updateUnit(
            UUID requestedOrganizationId,
            UUID unitId,
            SaveOrganizationalUnitRequest request) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            OrganizationalUnitEntity unit = findUnit(unitId, scope.organizationId());
            if (unit.getUnitType() != request.unitType()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Le type d'une unité existante ne peut pas être modifié.");
            }
            String code = normalizeCode(request.code());
            rejectConflict(
                    unitRepository.existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(
                            scope.organizationId(), code, unitId),
                    "Ce code d'unité est déjà utilisé.");
            OrganizationalUnitEntity parent = resolveParent(
                    scope.organizationId(), request.parentId(), request.unitType(), unitId);
            ResolvedUnitIdentity identity = resolveIdentity(request);
            unit.update(
                    parent == null ? null : parent.getId(),
                    code,
                    identity.name(),
                    identity.serviceCatalogCode());
            OrganizationalUnitEntity saved = unitRepository.saveAndFlush(unit);
            audit(scope, saved, "UPDATE");
            return OrganizationalUnitResponse.fromEntity(saved);
        });
    }

    @Override
    public OrganizationalUnitResponse setUnitActive(
            UUID requestedOrganizationId,
            UUID unitId,
            boolean active) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            OrganizationalUnitEntity unit = findUnit(unitId, scope.organizationId());
            if (active) {
                validateActivation(unit, scope.organizationId());
                unit.activate();
            } else {
                rejectConflict(
                        unitRepository.existsByOrganizationIdAndParentIdAndActiveTrue(
                                scope.organizationId(), unitId),
                        "Désactivez d'abord les unités enfants actives.");
                unit.deactivate();
            }
            OrganizationalUnitEntity saved = unitRepository.saveAndFlush(unit);
            audit(scope, saved, active ? "ACTIVATE" : "DEACTIVATE");
            return OrganizationalUnitResponse.fromEntity(saved);
        });
    }

    private ResolvedUnitIdentity resolveIdentity(SaveOrganizationalUnitRequest request) {
        if (request.unitType() == OrganizationalUnitType.SERVICE) {
            if (request.name() != null && !request.name().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Le nom d'un service est défini par le catalogue et ne doit pas être saisi librement.");
            }
            String catalogCode = normalizeCatalogCode(request.serviceCatalogCode());
            HospitalServiceCatalogEntity catalog = serviceCatalogRepository.findById(catalogCode)
                    .filter(HospitalServiceCatalogEntity::isActive)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Type de service inconnu ou inactif."));
            return new ResolvedUnitIdentity(catalog.getNameFr(), catalog.getCode());
        }

        if (request.serviceCatalogCode() != null && !request.serviceCatalogCode().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Seules les unités de type SERVICE peuvent référencer le catalogue des services.");
        }
        return new ResolvedUnitIdentity(requireName(request.name()), null);
    }

    private OrganizationalUnitEntity resolveParent(
            UUID organizationId,
            UUID parentId,
            OrganizationalUnitType childType,
            UUID editedUnitId) {
        if (parentId == null) {
            if (!ROOT_TYPES.contains(childType)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Une unité de soins doit appartenir à un service.");
            }
            return null;
        }
        if (parentId.equals(editedUnitId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une unité ne peut pas être son propre parent.");
        }
        OrganizationalUnitEntity parent = findUnit(parentId, organizationId);
        if (!parent.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le parent sélectionné est inactif.");
        }
        if (!isAllowedParent(parent.getUnitType(), childType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hiérarchie organisationnelle invalide.");
        }
        if (editedUnitId != null) {
            assertNoCycle(parent, editedUnitId, organizationId);
        }
        return parent;
    }

    private void assertNoCycle(OrganizationalUnitEntity parent, UUID editedUnitId, UUID organizationId) {
        OrganizationalUnitEntity current = parent;
        while (current != null) {
            if (editedUnitId.equals(current.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette modification créerait un cycle.");
            }
            UUID nextParentId = current.getParentId();
            current = nextParentId == null ? null : findUnit(nextParentId, organizationId);
        }
    }

    private boolean isAllowedParent(OrganizationalUnitType parent, OrganizationalUnitType child) {
        return switch (parent) {
            case POLE -> child == OrganizationalUnitType.DEPARTMENT || child == OrganizationalUnitType.SERVICE;
            case DEPARTMENT -> child == OrganizationalUnitType.SERVICE;
            case SERVICE -> child == OrganizationalUnitType.CARE_UNIT;
            case CARE_UNIT -> false;
        };
    }

    private void validateActivation(OrganizationalUnitEntity unit, UUID organizationId) {
        if (unit.getParentId() != null) {
            OrganizationalUnitEntity parent = findUnit(unit.getParentId(), organizationId);
            if (!parent.isActive()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Le parent de cette unité est inactif.");
            }
        }
        if (unit.getUnitType() == OrganizationalUnitType.SERVICE) {
            serviceCatalogRepository.findById(unit.getServiceCatalogCode())
                    .filter(HospitalServiceCatalogEntity::isActive)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Le type de service associé est inactif."));
        }
    }

    private OrganizationalUnitEntity findUnit(UUID unitId, UUID organizationId) {
        return unitRepository.findByIdAndOrganizationId(unitId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unité introuvable."));
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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sélectionnez un établissement à administrer.");
            }
            if (!organizationRepository.existsById(requestedOrganizationId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable.");
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

    private void audit(OrganizationScope scope, OrganizationalUnitEntity unit, String action) {
        auditService.logSuccess(
                scope.actor().getId(),
                scope.organizationId(),
                null,
                "ORGANIZATIONAL_UNIT",
                unit.getId(),
                action,
                unit.getCode() + " [" + unit.getUnitType() + "]");
    }

    private String normalizeCode(String rawCode) {
        String code = rawCode.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le code doit contenir uniquement des lettres majuscules, chiffres et underscores.");
        }
        return code;
    }

    private String normalizeCatalogCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sélectionnez un type de service.");
        }
        return rawCode.trim().toUpperCase(Locale.ROOT);
    }

    private String requireName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom de l'unité est obligatoire.");
        }
        String name = rawName.trim().replaceAll("\\s+", " ");
        if (name.length() < 2 || name.length() > 120) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le nom de l'unité doit contenir entre 2 et 120 caractères.");
        }
        return name;
    }

    private void rejectConflict(boolean condition, String message) {
        if (condition) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    private record OrganizationScope(UserAccountEntity actor, UUID organizationId) {
    }

    private record ResolvedUnitIdentity(String name, String serviceCatalogCode) {
    }
}
