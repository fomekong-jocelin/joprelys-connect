package com.joprelys.backend.consultation.application;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Medical acts (signature, consultation writing, discharge decision, death declaration) are
 * reserved to an enabled physician, whatever the permissions granted to the role (AUDIT-20261104).
 */
@Service
public class MedicalSigningPolicy {

    private static final String PHYSICIAN_ROLE = "MEDECIN";

    private final UserAccountRepository users;

    public MedicalSigningPolicy(UserAccountRepository users) {
        this.users = users;
    }

    public UserAccountEntity requirePhysician(UUID actorId) {
        return requirePhysicianFor(actorId, "La signature médicale nécessite un médecin habilité.");
    }

    public UserAccountEntity requirePhysicianFor(UUID actorId, String refusalMessage) {
        UserAccountEntity actor = actorId == null ? null : users.findById(actorId).orElse(null);
        if (!isPhysician(actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, refusalMessage);
        }
        return actor;
    }

    public static boolean isPhysician(UserAccountEntity actor) {
        return actor != null && actor.isEnabled() && actor.getRole() != null
                && Arrays.stream(actor.getRole().split(",")).map(String::trim).anyMatch(PHYSICIAN_ROLE::equals);
    }
}
