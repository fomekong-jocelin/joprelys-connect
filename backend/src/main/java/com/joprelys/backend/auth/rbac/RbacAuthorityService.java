package com.joprelys.backend.auth.rbac;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RbacAuthorityService {

    private final UserAccountRepository userAccountRepository;
    private final RbacStore rbacStore;

    public RbacAuthorityService(UserAccountRepository userAccountRepository, RbacStore rbacStore) {
        this.userAccountRepository = userAccountRepository;
        this.rbacStore = rbacStore;
    }

    @Transactional
    public Optional<ResolvedAuthorities> resolve(UUID userId, UUID tokenOrganizationId) {
        return userAccountRepository.findById(userId)
                .filter(UserAccountEntity::isEnabled)
                .flatMap(user -> resolveUser(user, tokenOrganizationId));
    }

    @Transactional
    public Optional<ResolvedAuthorities> resolveByEmail(String email, UUID tokenOrganizationId) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userAccountRepository.findByEmailIgnoreCase(email.trim())
                .filter(UserAccountEntity::isEnabled)
                .flatMap(user -> resolveUser(user, tokenOrganizationId));
    }

    private Optional<ResolvedAuthorities> resolveUser(
            UserAccountEntity user,
            UUID tokenOrganizationId) {
        if (tokenOrganizationId != null && user.getOrganizationId() != null
                && !tokenOrganizationId.equals(user.getOrganizationId())) {
            return Optional.empty();
        }

        rbacStore.synchronizeLegacyAssignments(user);
        RbacStore.EffectiveAccess access = rbacStore.loadEffectiveAccess(
                user.getId(), user.getOrganizationId());
        if (access.roles().isEmpty()) {
            Set<String> legacyRoles = legacyRoleCodes(user.getRole());
            access = new RbacStore.EffectiveAccess(
                    user.getId(),
                    legacyRoles,
                    RbacCatalog.permissionsForLegacyRoles(legacyRoles));
        }

        return Optional.of(new ResolvedAuthorities(
                user.getId(),
                user.getEmail(),
                user.getOrganizationId(),
                access.roles(),
                access.permissions()));
    }

    private static Set<String> legacyRoleCodes(String rawRoles) {
        if (rawRoles == null || rawRoles.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(rawRoles.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(role -> role.toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    public record ResolvedAuthorities(
            UUID userId,
            String email,
            UUID organizationId,
            Set<String> roles,
            Set<String> permissions) {
    }
}
