package com.joprelys.backend.auth.rbac;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RbacAdministrationService {

    private static final Pattern ROLE_CODE_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]{2,63}");

    private final UserAccountRepository userAccountRepository;
    private final OrganizationRepository organizationRepository;
    private final RbacStore rbacStore;

    public RbacAdministrationService(
            UserAccountRepository userAccountRepository,
            OrganizationRepository organizationRepository,
            RbacStore rbacStore) {
        this.userAccountRepository = userAccountRepository;
        this.organizationRepository = organizationRepository;
        this.rbacStore = rbacStore;
    }

    @Transactional
    public RbacStore.EffectiveAccess myAccess(Authentication authentication) {
        UserAccountEntity actor = currentUser(authentication);
        return rbacStore.loadEffectiveAccess(actor.getId(), actor.getOrganizationId());
    }

    @Transactional(readOnly = true)
    public List<RbacStore.RoleView> listRoles(UUID requestedOrganizationId, Authentication authentication) {
        OrganizationScope scope = organizationScope(authentication, requestedOrganizationId);
        return rbacStore.listVisibleRoles(scope.organizationId()).stream()
                .filter(role -> !RbacCatalog.platformRoleCodes().contains(role.code()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RbacStore.PermissionView> listPermissions(Authentication authentication) {
        currentUser(authentication);
        return rbacStore.listPermissions().stream()
                .filter(permission -> !RbacCatalog.platformPermissionCodes().contains(permission.code()))
                .toList();
    }

    @Transactional
    public List<UserAccessView> listUsers(UUID requestedOrganizationId, Authentication authentication) {
        OrganizationScope scope = organizationScope(authentication, requestedOrganizationId);
        return userAccountRepository.findAllByOrganizationId(scope.organizationId()).stream()
                .sorted(Comparator.comparing(UserAccountEntity::getDisplayName))
                .map(user -> {
                    rbacStore.synchronizeLegacyAssignments(user);
                    RbacStore.EffectiveAccess access = rbacStore.loadEffectiveAccess(user.getId(), user.getOrganizationId());
                    return new UserAccessView(
                            user.getId(),
                            user.getEmail(),
                            user.getDisplayName(),
                            user.isEnabled(),
                            scope.actor().getId().equals(user.getId()),
                            access.roles(),
                            access.permissions());
                })
                .toList();
    }

    @Transactional
    public RbacStore.RoleView createRole(
            UUID requestedOrganizationId,
            String rawCode,
            String rawName,
            String description,
            boolean assignable,
            Set<String> permissionCodes,
            Authentication authentication) {
        OrganizationScope scope = organizationScope(authentication, requestedOrganizationId);
        UserAccountEntity actor = scope.actor();
        String code = normalizeCode(rawCode);
        String name = requireName(rawName);
        validateClinicPermissionCodes(permissionCodes);
        if (rbacStore.customRoleCodeExists(scope.organizationId(), code, null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un rôle portant ce code existe déjà.");
        }
        RbacStore.RoleView role = rbacStore.createCustomRole(
                scope.organizationId(), code, name, trimToNull(description), assignable,
                new LinkedHashSet<>(permissionCodes));
        rbacStore.audit(
                scope.organizationId(), actor.getId(), "ROLE_CREATED", "ROLE", role.id().toString(),
                "code=" + role.code() + "; permissions=" + String.join(",", role.permissions()));
        return role;
    }

    @Transactional
    public RbacStore.RoleView updateRole(
            UUID roleId,
            UUID requestedOrganizationId,
            String rawCode,
            String rawName,
            String description,
            boolean assignable,
            boolean enabled,
            Authentication authentication) {
        OrganizationScope scope = organizationScope(authentication, requestedOrganizationId);
        UserAccountEntity actor = scope.actor();
        RbacStore.RoleView current = requireCustomRole(roleId, scope.organizationId());
        assertActorDoesNotDependOnRole(current, actor);
        String code = normalizeCode(rawCode);
        if (rbacStore.customRoleCodeExists(scope.organizationId(), code, roleId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un rôle portant ce code existe déjà.");
        }
        RbacStore.RoleView updated;
        try {
            updated = rbacStore.updateCustomRole(
                    roleId,
                    scope.organizationId(),
                    code,
                    requireName(rawName),
                    trimToNull(description),
                    assignable,
                    enabled);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rôle personnalisé introuvable.", exception);
        }
        rbacStore.audit(
                scope.organizationId(), actor.getId(), "ROLE_UPDATED", "ROLE", roleId.toString(),
                "code=" + updated.code() + "; enabled=" + updated.enabled());
        return updated;
    }

    @Transactional
    public RbacStore.RoleView replaceRolePermissions(
            UUID roleId,
            UUID requestedOrganizationId,
            Set<String> permissionCodes,
            Authentication authentication) {
        OrganizationScope scope = organizationScope(authentication, requestedOrganizationId);
        UserAccountEntity actor = scope.actor();
        RbacStore.RoleView current = requireCustomRole(roleId, scope.organizationId());
        assertActorDoesNotDependOnRole(current, actor);
        validateClinicPermissionCodes(permissionCodes);
        RbacStore.RoleView updated;
        try {
            updated = rbacStore.replaceCustomRolePermissions(
                    roleId, scope.organizationId(), new LinkedHashSet<>(permissionCodes));
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rôle personnalisé introuvable.", exception);
        }
        rbacStore.audit(
                scope.organizationId(), actor.getId(), "ROLE_PERMISSIONS_REPLACED", "ROLE", roleId.toString(),
                "permissions=" + String.join(",", updated.permissions()));
        return updated;
    }

    @Transactional
    public UserAccessView replaceUserRoles(
            UUID targetUserId,
            UUID requestedOrganizationId,
            List<UUID> roleIds,
            Authentication authentication) {
        OrganizationScope scope = organizationScope(authentication, requestedOrganizationId);
        UserAccountEntity actor = scope.actor();
        if (actor.getId().equals(targetUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vous ne pouvez pas modifier vos propres privilèges.");
        }
        if (roleIds == null || roleIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Au moins un rôle doit être attribué.");
        }

        UserAccountEntity target = userAccountRepository.findByIdAndOrganizationId(targetUserId, scope.organizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        rbacStore.synchronizeLegacyAssignments(target);

        List<RbacStore.RoleView> roles = roleIds.stream()
                .distinct()
                .map(roleId -> rbacStore.findVisibleRole(roleId, scope.organizationId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle invalide ou inaccessible.")))
                .toList();
        if (roles.stream().map(RbacStore.RoleView::code).anyMatch(RbacCatalog.platformRoleCodes()::contains)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Les rôles plateforme ne peuvent pas être attribués depuis l’administration d’un établissement.");
        }
        if (roles.stream().anyMatch(role -> !role.enabled() || !role.assignable())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un rôle sélectionné est désactivé ou non attribuable.");
        }

        boolean targetWasAdmin = rbacStore.userHasAnyRole(target.getId(), RbacCatalog.adminRoleCodes());
        boolean targetWillRemainAdmin = roles.stream().map(RbacStore.RoleView::code)
                .anyMatch(RbacCatalog.adminRoleCodes()::contains);
        if (target.isEnabled() && targetWasAdmin && !targetWillRemainAdmin
                && rbacStore.countActiveAdministrators(scope.organizationId()) <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Impossible de retirer le dernier administrateur actif de l'établissement.");
        }

        rbacStore.replaceUserRoles(target.getId(), scope.organizationId(), actor.getId(), roles);
        String compatibilityRoles = roles.stream().map(RbacStore.RoleView::code).sorted()
                .collect(java.util.stream.Collectors.joining(","));
        target.setRole(compatibilityRoles);
        userAccountRepository.save(target);
        RbacStore.EffectiveAccess effective = rbacStore.loadEffectiveAccess(target.getId(), target.getOrganizationId());
        rbacStore.audit(
                scope.organizationId(), actor.getId(), "USER_ROLES_REPLACED", "USER", target.getId().toString(),
                "roles=" + String.join(",", effective.roles()));
        return new UserAccessView(
                target.getId(), target.getEmail(), target.getDisplayName(), target.isEnabled(), false,
                effective.roles(), effective.permissions());
    }

    @Transactional(readOnly = true)
    public List<RbacStore.AuditView> listAudit(UUID requestedOrganizationId, Authentication authentication) {
        OrganizationScope scope = organizationScope(authentication, requestedOrganizationId);
        return rbacStore.listAudit(scope.organizationId(), 100);
    }

    private OrganizationScope organizationScope(Authentication authentication, UUID requestedOrganizationId) {
        UserAccountEntity actor = currentUser(authentication);
        if (canManageOrganizations(authentication)) {
            if (requestedOrganizationId == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sélectionnez un établissement à administrer.");
            }
            if (!organizationRepository.existsById(requestedOrganizationId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable.");
            }
            return new OrganizationScope(actor, requestedOrganizationId);
        }

        UUID actorOrganizationId = actor.getOrganizationId();
        if (actorOrganizationId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Utilisateur non rattaché à un établissement.");
        }
        if (requestedOrganizationId != null && !actorOrganizationId.equals(requestedOrganizationId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Vous ne pouvez pas administrer les droits d’un autre établissement.");
        }
        return new OrganizationScope(actor, actorOrganizationId);
    }

    private RbacStore.RoleView requireCustomRole(UUID roleId, UUID organizationId) {
        RbacStore.RoleView role = rbacStore.findVisibleRole(roleId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rôle introuvable."));
        if (role.systemRole() || role.organizationId() == null || !role.organizationId().equals(organizationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les rôles système sont protégés.");
        }
        return role;
    }

    private void assertActorDoesNotDependOnRole(RbacStore.RoleView role, UserAccountEntity actor) {
        if (rbacStore.isRoleAssignedToUser(role.id(), actor.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vous ne pouvez pas modifier un rôle qui contribue à vos propres privilèges.");
        }
    }

    private void validateClinicPermissionCodes(Set<String> permissionCodes) {
        Set<String> requested = permissionCodes == null ? Set.of() : permissionCodes;
        Set<String> known = RbacCatalog.permissionCodes();
        List<String> unknown = requested.stream().filter(code -> !known.contains(code)).sorted().toList();
        if (!unknown.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permissions inconnues : " + String.join(", ", unknown));
        }
        List<String> forbidden = requested.stream()
                .filter(RbacCatalog.platformPermissionCodes()::contains)
                .sorted()
                .toList();
        if (!forbidden.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Permissions réservées à la plateforme : " + String.join(", ", forbidden));
        }
    }

    private boolean canManageOrganizations(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> RbacCatalog.PERMISSION_ORGANIZATION_MANAGE.equals(authority.getAuthority()));
    }

    private UserAccountEntity currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        UserAccountEntity user = userAccountRepository.findByEmail(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .filter(UserAccountEntity::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required"));
        rbacStore.synchronizeLegacyAssignments(user);
        return user;
    }

    private static String normalizeCode(String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (!ROLE_CODE_PATTERN.matcher(code).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le code du rôle doit contenir uniquement des lettres majuscules, chiffres et underscores.");
        }
        return code;
    }

    private static String requireName(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.length() < 3 || name.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom du rôle doit contenir entre 3 et 120 caractères.");
        }
        return name;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record OrganizationScope(UserAccountEntity actor, UUID organizationId) {
    }

    public record UserAccessView(
            UUID id,
            String email,
            String displayName,
            boolean enabled,
            boolean currentUser,
            Set<String> roles,
            Set<String> permissions) {
    }
}
