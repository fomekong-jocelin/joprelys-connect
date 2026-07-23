package com.joprelys.backend.spatial.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.spatial.api.BedResponse;
import com.joprelys.backend.spatial.api.SaveBedRequest;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceProfileRepository;
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
public class HospitalBedConfigurationService {

    private final BedRepository bedRepository;
    private final BedAssignmentRepository bedAssignmentRepository;
    private final FacilitySpaceRepository spaceRepository;
    private final InpatientSpaceProfileRepository inpatientProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;
    private final TransactionTemplate transactionTemplate;

    public HospitalBedConfigurationService(
            BedRepository bedRepository,
            BedAssignmentRepository bedAssignmentRepository,
            FacilitySpaceRepository spaceRepository,
            InpatientSpaceProfileRepository inpatientProfileRepository,
            UserAccountRepository userAccountRepository,
            OrganizationRepository organizationRepository,
            AuditService auditService,
            PlatformTransactionManager transactionManager) {
        this.bedRepository = bedRepository;
        this.bedAssignmentRepository = bedAssignmentRepository;
        this.spaceRepository = spaceRepository;
        this.inpatientProfileRepository = inpatientProfileRepository;
        this.userAccountRepository = userAccountRepository;
        this.organizationRepository = organizationRepository;
        this.auditService = auditService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public List<BedResponse> listBeds(UUID requestedOrganizationId, UUID spaceId) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            FacilitySpaceEntity space = findSpace(spaceId, scope.organizationId());
            return bedRepository.findBySpaceId(space.getId()).stream()
                    .sorted(Comparator.comparing(BedEntity::getBedNumber, String.CASE_INSENSITIVE_ORDER))
                    .map(bed -> BedResponse.fromEntity(
                            bed,
                            bedAssignmentRepository.findActiveByBedId(bed.getId()).isPresent()))
                    .toList();
        });
    }

    public BedResponse createBed(UUID requestedOrganizationId, SaveBedRequest request) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            FacilitySpaceEntity space = requireInpatientSpace(request.spaceId(), scope.organizationId());
            String bedNumber = normalize(request.bedNumber());
            rejectConflict(
                    bedRepository.existsBySpaceIdAndBedNumberIgnoreCase(space.getId(), bedNumber),
                    "Un lit portant ce numéro existe déjà dans cet espace.");
            BedEntity bed = new BedEntity(space, bedNumber);
            BedEntity saved = bedRepository.saveAndFlush(bed);
            audit(scope, saved.getId(), "CREATE", "Création du lit " + saved.getBedNumber());
            return BedResponse.fromEntity(saved, false);
        });
    }

    public BedResponse updateBed(UUID requestedOrganizationId, UUID bedId, SaveBedRequest request) {
        return inOrganizationScope(requestedOrganizationId, scope -> {
            BedEntity bed = bedRepository.findByIdAndOrganizationId(bedId, scope.organizationId())
                    .orElseThrow(() -> notFound("Lit introuvable."));
            FacilitySpaceEntity targetSpace = requireInpatientSpace(request.spaceId(), scope.organizationId());
            String bedNumber = normalize(request.bedNumber());
            rejectConflict(
                    bedRepository.existsBySpaceIdAndBedNumberIgnoreCaseAndIdNot(
                            targetSpace.getId(), bedNumber, bedId),
                    "Un lit portant ce numéro existe déjà dans cet espace.");

            boolean moving = !bed.getSpace().getId().equals(targetSpace.getId());
            if (moving) {
                rejectConflict(
                        bed.getStatus() == BedStatus.OCCUPIED || bedAssignmentRepository.existsByBedId(bedId),
                        "Un lit utilisé ne peut pas être déplacé vers un autre espace.");
                bed.setSpace(targetSpace);
            }
            bed.setBedNumber(bedNumber);
            BedEntity saved = bedRepository.saveAndFlush(bed);
            audit(scope, saved.getId(), "UPDATE", "Modification du lit " + saved.getBedNumber());
            return BedResponse.fromEntity(
                    saved,
                    bedAssignmentRepository.findActiveByBedId(saved.getId()).isPresent());
        });
    }

    public void deleteBed(UUID requestedOrganizationId, UUID bedId) {
        inOrganizationScope(requestedOrganizationId, scope -> {
            BedEntity bed = bedRepository.findByIdAndOrganizationId(bedId, scope.organizationId())
                    .orElseThrow(() -> notFound("Lit introuvable."));
            rejectConflict(bed.getStatus() == BedStatus.OCCUPIED, "Un lit occupé ne peut pas être supprimé.");
            rejectConflict(bedAssignmentRepository.existsByBedId(bedId), "Ce lit possède un historique d'affectation.");
            bedRepository.delete(bed);
            bedRepository.flush();
            audit(scope, bedId, "DELETE", "Suppression du lit " + bed.getBedNumber());
            return null;
        });
    }

    private FacilitySpaceEntity requireInpatientSpace(UUID spaceId, UUID organizationId) {
        FacilitySpaceEntity space = findSpace(spaceId, organizationId);
        if (!space.isActive()) {
            throw conflict("L'espace sélectionné est inactif.");
        }
        if (!inpatientProfileRepository.existsBySpaceIdAndOrganizationId(spaceId, organizationId)) {
            throw badRequest("Seul un espace doté d'un profil d'hébergement peut recevoir des lits.");
        }
        return space;
    }

    private FacilitySpaceEntity findSpace(UUID spaceId, UUID organizationId) {
        return spaceRepository.findByIdAndOrganizationId(spaceId, organizationId)
                .orElseThrow(() -> notFound("Espace introuvable."));
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

    private void audit(OrganizationScope scope, UUID bedId, String action, String details) {
        auditService.logSuccess(
                scope.actor().getId(),
                scope.organizationId(),
                null,
                "BED",
                bedId,
                action,
                details);
    }

    private String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ");
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
