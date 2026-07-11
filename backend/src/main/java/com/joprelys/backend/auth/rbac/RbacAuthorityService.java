package com.joprelys.backend.auth.rbac;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
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
        Optional<UserAccountEntity> userOptional = userAccountRepository.findById(userId)
                .filter(UserAccountEntity::isEnabled);
        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        UserAccountEntity user = userOptional.get();
        if (tokenOrganizationId != null && user.getOrganizationId() != null
                && !tokenOrganizationId.equals(user.getOrganizationId())) {
            return Optional.empty();
        }

        rbacStore.synchronizeLegacyAssignments(user);
        RbacStore.EffectiveAccess access = rbacStore.loadEffectiveAccess(user.getId(), user.getOrganizationId());
        return Optional.of(new ResolvedAuthorities(
                user.getId(),
                user.getEmail(),
                user.getOrganizationId(),
                access.roles(),
                access.permissions()));
    }

    public record ResolvedAuthorities(
            UUID userId,
            String email,
            UUID organizationId,
            Set<String> roles,
            Set<String> permissions) {
    }
}
