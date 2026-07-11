package com.joprelys.backend.patient.reconciliation.application;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PatientReconciliationActorProvider {

    private final UserAccountRepository userAccountRepository;

    public PatientReconciliationActorProvider(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public UserAccountEntity requireCurrentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED");
        }

        String email = authentication.getName().trim().toLowerCase(Locale.ROOT);
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "AUTHENTICATION_REQUIRED"));
    }
}
