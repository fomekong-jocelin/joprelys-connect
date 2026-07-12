package com.joprelys.backend.patient.reconciliation.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PatientPairLockServiceTest {

    @Test
    void shouldAlwaysLockPatientsInUuidOrder() {
        PatientRepository patientRepository = mock(PatientRepository.class);
        PatientPairLockService lockService = new PatientPairLockService(patientRepository);

        UUID lowId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID highId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        PatientEntity lowPatient = mock(PatientEntity.class);
        PatientEntity highPatient = mock(PatientEntity.class);
        when(patientRepository.findByIdForUpdate(lowId)).thenReturn(Optional.of(lowPatient));
        when(patientRepository.findByIdForUpdate(highId)).thenReturn(Optional.of(highPatient));

        PatientPairLockService.LockedPatientPair pair = lockService.lock(highId, lowId);

        InOrder inOrder = inOrder(patientRepository);
        inOrder.verify(patientRepository).findByIdForUpdate(lowId);
        inOrder.verify(patientRepository).findByIdForUpdate(highId);
        assertSame(highPatient, pair.left());
        assertSame(lowPatient, pair.right());
    }

    @Test
    void shouldRejectTheSamePatientOnBothSides() {
        PatientRepository patientRepository = mock(PatientRepository.class);
        PatientPairLockService lockService = new PatientPairLockService(patientRepository);
        UUID patientId = UUID.randomUUID();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> lockService.lock(patientId, patientId));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("PATIENT_PAIR_MUST_BE_DISTINCT", exception.getReason());
    }
}
