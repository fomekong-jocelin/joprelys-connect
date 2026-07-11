package com.joprelys.backend.auth.rbac;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
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
    private final RbacStore rbacStore;

    public RbacAdministrationService(UserAccountRepository userAccountRepository, RbacStore rbacStore) {
        this.userAccountRepository = userAccountRepository;
        this.rbacStore = rbacStore;
    }

    @Transactional
    public RbacStore.EffectiveAccess myAccess(Authentication authentication) {
        UserAccountEntity actor = currentUser(authentication, false);
        rbacStore.synchronizeLegacyAssignments(actor);
        return rbacStore.loadEffectiveAccess(actor.getId(), actor.getOrganizationId());
    }

    @Transactional(readOnly = true)
    public List<RbacStore.RoleView> listRoles(Authentication authentication) {
        UserAccountEntity actor = currentUser(authentication, true);
        return rbacStore.listVisibleRoles(actor.getOrganizationId());
    }

    @Transactional(readOnly = true)
    public List<RbacStore.PermissionView> listPermissions(Authentication authentication) {
        currentUser(authentication, true);
        return rbacStore.listPermissions();
    }

    @Transactional
    public List<UserAccessView> listUsers(Authentication authentication) {
        UserAccountEntity actor = currentUser(authentication, true);
        return userAccountRepository.findAllByOrganizationId(actor.getOrganizationId()).stream()
                .sorted(Comparator.comparing(UserAccountEntity::getDisplayName))
                .map(user -> {
                    rbacStore.synchronizeLegacyAssignments(user);
                    RbacStore.EffectiveAccess access = rbacStore.loadEffectiveAccess(user.getId(), user.getOrganizationId());
                    return new UserAccessView(
                            user.getId(),
                            user.getEmail(),
                            user.getDisplayName(),
                            user.isEnabled(),
                            actor.getId().equals(user.getId()),
                            access.roles(),
                            access.permissions());
                })
                .toList();
    }

    @Transactional
    public RbacStore.RoleView createRole(
            String rawCode,
            String rawName,
            String description,
            boolean assignable,
            Set<String> permissionCodes,
            Authentication authentication) {
        UserAccountEntity actor = currentUser(authentication, true);
        String code = normalizeCode(rawCode);
        String name = requireName(rawName);
        validatePermissionCodes(permissionCodes);
        if (rbacStore.customRoleCodeExists(actor.getOrganizationId(), code, null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un rôle portant ce code existe déjà.");
        }
        RbacStore.RoleView role = rbacStore.createCustomRole(
                actor.getOrganizationId(), code, name, trimToNull(description), assignable,
                new LinkedHashSet<>(permissionCodes));
        rbacStore.audit(
                actor.getOrganizationId(), actor.getId(), "ROLE_CREATED", "ROLE", role.id().toString(),
                "code=" + role.code() + "; permissions=" + String.join(",", role.permissions()));
        return role;
    }

    @Transactional
    public RbacStore.RoleView updateRole(
            UUID roleId,
            String rawCode,
            String rawName,
            String description,
            boolean assignable,
            boolean enabled,
            Authentication authentication) {
        UserAccountEntity actor = currentUser(authentication, true);
        RbacStore.RoleView current = requireCustomRole(roleId, actor.getOrganizationId());
        assertActorDoesNotDependOnRole(current, actor);
        String code = normalizeCode(rawCode);
        if (rbacStore.customRoleCodeExists(actor.getOrganizationId(), code, roleId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un rôle portant ce code existe déjà.");
        }
        RbacStore.RoleView updated;
        try {
            updated = rbacStore.updateCustomRole(
                    roleId,
                    actor.getOrganizationId(),
                    code,
                    requireName(rawName),
                    trimToNull(description),
                    assignable,
                    enabled);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rôle personnalisé introuvable.", exception);
        }
        rbacStore.audit(
                actor.getOrganizationId(), actor.getId(), "ROLE_UPDATED", "ROLE", roleId.toString(),
                "code=" + updated.code() + "; enabled=" + updated.enabled());
        return updated;
    }

    @Transactional
    public RbacStore.RoleView replaceRolePermissions(
            UUID roleId,
            Set<String> permissionCodes,
            Authentication authentication) {
        UserAccountEntity actor = currentUser(authentication, true);
        RbacStore.RoleView current = requireCustomRole(roleId, actor.getOrganizationId());
        assertActorDoesNotDependOnRole(current, actor);
        validatePermissionCodes(permissionCodes);
        RbacStore.RoleView updated;
        try {
            updated = rbacStore.replaceCustomRolePermissions(
                    roleId, actor.getOrganizationId(), new LinkedHashSet<>(permissionCodes));
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rôle personnalisé introuvable.", exception);
        }
        rbacStore.audit(
                actor.getOrganizationId(), actor.getId(), "ROLE_PERMISSIONS_REPLACED", "ROLE", roleId.toString(),
                "permissions=" + String.join(",", updated.permissions()));
        return updated;
    }

    @Transactional
    public UserAccessView replaceUserRoles(
            UUID targetUserId,
            List<UUID> roleIds,
            Authentication authentication) {
        UserAccountEntity actor = currentUser(authentication, true);
        if (actor.getId().equals(targetUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vous ne pouvez pas modifier vos propres privilèges.");
        }
        if (roleIds == null || roleIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Au moins un rôle doit être attribué.");
        }

        UserAccountEntity target = userAccountRepository.findByIdAndOrganizationId(targetUserId, actor.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        rbacStore.synchronizeLegacyAssignments(target);

        List<RbacStore.RoleView> roles = roleIds.stream()
                .distinct()
                .map(roleId -> rbacStore.findVisibleRole(roleId, actor.getOrganizationId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle invalide ou inaccessible.")))
                .toList();
        if (roles.stream().anyMatch(role -> !role.enabled() || !role.assignable())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un rôle sélectionné est désactivé ou non attribuable.");
        }

        boolean targetWasAdmin = rbacStore.userHasAnyRole(target.getId(), RbacCatalog.adminRoleCodes());
        boolean targetWillRemainAdmin = roles.stream().map(RbacStore.RoleView::code)
                .anyMatch(RbacCatalog.adminRoleCodes()::contains);
        if (target.isEnabled() && targetWasAdmin && !targetWillRemainAdmin
                && rbacStore.countActiveAdministrators(actor.getOrganizationId()) <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Impossible de retirer le dernier administrateur actif de l'établissement.");
        }

        rbacStore.replaceUserRoles(target.getId(), actor.getOrganizationId(), actor.getId(), roles);
        String compatibilityRoles = roles.stream().map(RbacStore.RoleView::code).sorted().collect(java.util.stream.Collectors.joining(","));
        target.setRole(compatibilityRoles);
        userAccountRepository.save(target);
        RbacStore.EffectiveAccess effective = rbacStore.loadEffectiveAccess(target.getId(), target.getOrganizationId());
        rbacStore.audit(
                actor.getOrganizationId(), actor.getId(), "USER_ROLES_REPLACED", "USER", target.getId().toString(),
                "roles=" + String.join(",", effective.roles()));
        return new UserAccessView(
                target.getId(), target.getEmail(), target.getDisplayName(), target.isEnabled(), false,
                effective.roles(), effective.permissions());
    }

    @Transactional(readOnly = true)
    public List<RbacStore.AuditView> listAudit(Authentication authentication) {
        UserAccountEntity actor = currentUser(authentication, true);
        return rbacStore.listAudit(actor.getOrganizationId(), 100);
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

    private void validatePermissionCodes(Set<String> permissionCodes) {
        Set<String> requested = permissionCodes == null ? Set.of() : permissionCodes;
        Set<String> known = RbacCatalog.permissionCodes();
        List<String> unknown = requested.stream().filter(code -> !known.contains(code)).sorted().toList();
        if (!unknown.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permissions inconnues : " + String.join(", ", unknown));
        }
    }

    private UserAccountEntity currentUser(Authentication authentication, boolean requireOrganization) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        UserAccountEntity user = userAccountRepository.findByEmail(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .filter(UserAccountEntity::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required"));
        if (requireOrganization && user.getOrganizationId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Utilisateur non rattaché à un établissement.");
        }
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
