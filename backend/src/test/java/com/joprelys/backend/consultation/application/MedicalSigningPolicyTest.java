package com.joprelys.backend.consultation.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class MedicalSigningPolicyTest {
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final MedicalSigningPolicy policy = new MedicalSigningPolicy(users);

    @Test
    void requiresActivePhysicianQualificationIndependentlyOfGrantedPermissions() {
        UUID actorId = UUID.randomUUID();
        UserAccountEntity nurse = new UserAccountEntity("nurse@test.local", "Nurse", "INFIRMIER", "hash");
        when(users.findById(actorId)).thenReturn(Optional.of(nurse));
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> policy.requirePhysician(actorId)).getStatusCode());
        UserAccountEntity physician = new UserAccountEntity("doctor@test.local", "Doctor", "MEDECIN", "hash");
        physician.setEnabled(false);
        when(users.findById(actorId)).thenReturn(Optional.of(physician));
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> policy.requirePhysician(actorId)).getStatusCode());
        physician.setEnabled(true);
        assertEquals(physician, policy.requirePhysician(actorId));
    }

    @Test
    void missingActorCannotSign() {
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> policy.requirePhysician(null)).getStatusCode());
    }
}
