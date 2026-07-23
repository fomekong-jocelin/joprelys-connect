package com.joprelys.backend.spatial.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.spatial.api.HospitalLocationDtos.InpatientProfileResponse;
import com.joprelys.backend.spatial.api.HospitalLocationDtos.SaveInpatientProfileRequest;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceProfileEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceProfileRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceTypeCatalogRepository;
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
public class InpatientSpaceProfileService {

    private final FacilitySpaceRepository spaceRepository;
    private final InpatientSpaceProfileRepository profileRepository;
    private final InpatientSpaceTypeCatalogRepository inpatientTypeRepository;
    private final UserAccountRepository userAccountRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;
    private final TransactionTemplate transactionTemplate;

    public InpatientSpaceProfileService(
            FacilitySpaceRepository spaceRepository,
            InpatientSpaceProfileRepository profileRepository,
            InpatientSpaceTypeCatalogRepository inpatientTypeRepository,
            UserAccountRepository userAccountRepository,
            OrganizationRepository organizationRepository,
            AuditService auditService,
            PlatformTransactionManager transactionManager) {
        this.spaceRepository = spaceRepository;
        this.profileRepository = profileRepository;
        this.inpatientTypeRepository = inpatientTypeRepository;
        this.userAccountRepository = userAccountRepository;
        this.organizationRepository = organizationRepository;
        this.auditService = auditService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public InpatientProfileResponse getProfile(UUID requestedOrganizationId, UUID spaceId) {
        return inOrganizationScope(requestedOrganizationId, scope -> InpatientProfileResponse.fromEntity(
                requireProfile(spaceId, scope.organizationId())));
    }

    public InpatientProfileResponse saveProfile(
            UUID requestedOrganizationId,
            UUID spaceId,
            SaveInpatientProfileRequest request) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            FacilitySpaceEntity space = spaceRepository.findByIdAndOrganizationId(spaceId, scope.organizationId())
                    .orElseThrow(() -> notFound("Espace introuvable."));
            if (!space.isActive()) {
                throw conflict("Un espace inactif ne peut pas recevoir un profil d'hébergement.");
            }
            if (!inpatientTypeRepository.existsById(space.getSpaceTypeCode())) {
                throw badRequest("Ce type d'espace ne peut pas recevoir de profil d'hébergement.");
            }
            InpatientSpaceProfileEntity profile = profileRepository
                    .findBySpaceIdAndOrganizationId(spaceId, scope.organizationId())
                    .orElseGet(() -> new InpatientSpaceProfileEntity(
                            spaceId,
                            scope.organizationId(),
                            space.getSpaceTypeCode(),
                            request.comfortLevel()));
            profile.updateComfortLevel(request.comfortLevel());
            InpatientSpaceProfileEntity saved = profileRepository.saveAndFlush(profile);
            audit(scope, spaceId, saved.getComfortLevel());
            return InpatientProfileResponse.fromEntity(saved);
        });
    }

    private InpatientSpaceProfileEntity requireProfile(UUID spaceId, UUID organizationId) {
        spaceRepository.findByIdAndOrganizationId(spaceId, organizationId)
                .orElseThrow(() -> notFound("Espace introuvable."));
        return profileRepository.findBySpaceIdAndOrganizationId(spaceId, organizationId)
                .orElseThrow(() -> notFound("Profil d'hébergement introuvable."));
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

    private void audit(OrganizationScope scope, UUID spaceId, String comfortLevel) {
        auditService.logSuccess(
                scope.actor().getId(),
                scope.organizationId(),
                null,
                "INPATIENT_SPACE_PROFILE",
                spaceId,
                "UPSERT",
                "Niveau de confort : " + comfortLevel);
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
