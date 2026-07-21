package com.joprelys.backend.spatial.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ActiveBedAssignmentServiceTest {

    @Mock
    private BedAssignmentRepository bedAssignmentRepository;

    private ActiveBedAssignmentService service;

    @BeforeEach
    void setUp() {
        service = new ActiveBedAssignmentService(bedAssignmentRepository);
    }

    @Test
    void shouldCreateAndFlushAnActiveAssignment() {
        UUID organizationId = UUID.randomUUID();
        BedEntity bed = bed(organizationId);
        when(bedAssignmentRepository.saveAndFlush(any(BedAssignmentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BedAssignmentEntity saved = service.assign(UUID.randomUUID(), bed, organizationId);

        assertSame(bed, saved.getBed());
        assertEquals(bed.getId(), saved.getActiveBedId());
        assertEquals(saved.getHospitalizationId(), saved.getActiveHospitalizationId());
    }

    @Test
    void shouldTranslateIntegrityCollisionToConflict() {
        UUID organizationId = UUID.randomUUID();
        when(bedAssignmentRepository.saveAndFlush(any(BedAssignmentEntity.class)))
                .thenThrow(new DataIntegrityViolationException("uq_bed_assignments_active_bed"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.assign(UUID.randomUUID(), bed(organizationId), organizationId));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Une affectation active existe déjà pour ce lit ou cette hospitalisation.",
                exception.getReason());
    }

    @Test
    void shouldRejectBedFromAnotherOrganizationBeforePersistence() {
        UUID hospitalizationOrganizationId = UUID.randomUUID();
        BedEntity bed = mock(BedEntity.class);
        when(bed.getOrganizationId()).thenReturn(UUID.randomUUID());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.assign(
                        UUID.randomUUID(),
                        bed,
                        hospitalizationOrganizationId));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Le lit sélectionné n'appartient pas à l'établissement du séjour.",
                exception.getReason());
        verifyNoInteractions(bedAssignmentRepository);
    }

    private BedEntity bed(UUID organizationId) {
        BedEntity bed = mock(BedEntity.class);
        when(bed.getId()).thenReturn(UUID.randomUUID());
        when(bed.getOrganizationId()).thenReturn(organizationId);
        return bed;
    }
}
