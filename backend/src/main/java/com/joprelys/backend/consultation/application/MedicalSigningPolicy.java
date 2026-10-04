package com.joprelys.backend.consultation.application;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MedicalSigningPolicy {
    private final UserAccountRepository users;
    public MedicalSigningPolicy(UserAccountRepository users) { this.users = users; }
    public UserAccountEntity requirePhysician(UUID actorId) {
        UserAccountEntity actor = actorId == null ? null : users.findById(actorId).orElse(null);
        if (actor == null || !actor.isEnabled() || actor.getRole() == null
                || Arrays.stream(actor.getRole().split(",")).map(String::trim).noneMatch("MEDECIN"::equals)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La signature médicale nécessite un médecin habilité.");
        }
        return actor;
    }
}
