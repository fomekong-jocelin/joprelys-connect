package com.joprelys.backend.spatial.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.spatial.domain.HospitalServiceType;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedCapacityStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedReadinessStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.RoomEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.RoomRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.WardEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.WardRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SpatialServiceBedStatusTest {

    @Mock
    private WardRepository wardRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BedRepository bedRepository;

    @Mock
    private BedAssignmentRepository bedAssignmentRepository;

    @Mock
    private ActiveBedAssignmentService activeBedAssignmentService;

    @Mock
    private HospitalizationRepository hospitalizationRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private AuditService auditService;

    private SpatialService spatialService;
    private UUID bedId;
    private BedEntity bed;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        spatialService = new SpatialService(
                wardRepository,
                roomRepository,
                bedRepository,
                bedAssignmentRepository,
                activeBedAssignmentService,
                new BedStatusTransitionPolicy(),
                hospitalizationRepository,
                userAccountRepository,
                auditService);

        WardEntity ward = new WardEntity("Médecine", HospitalServiceType.HOSPITALIZATION);
        RoomEntity room = new RoomEntity(ward, "101", 1, "STANDARD");
        bed = new BedEntity(room, "101-A");
        bedId = UUID.randomUUID();
        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));
    }

    @Test
    void shouldRejectDirectOccupiedStatus() {
        when(bedAssignmentRepository.findActiveByBedId(bedId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> spatialService.updateBedStatus(bedId, BedStatus.OCCUPIED));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(BedStatus.FREE, bed.getStatus());
        verify(bedRepository, never()).save(any(BedEntity.class));
    }

    @Test
    void shouldRejectAnyManualTransitionWhileAssignmentIsActive() {
        bed.setStatus(BedStatus.OCCUPIED);
        BedAssignmentEntity assignment = new BedAssignmentEntity(UUID.randomUUID(), bed);
        when(bedAssignmentRepository.findActiveByBedId(bedId)).thenReturn(Optional.of(assignment));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> spatialService.updateBedStatus(bedId, BedStatus.MAINTENANCE));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(BedStatus.OCCUPIED, bed.getStatus());
        verify(bedRepository, never()).save(any(BedEntity.class));
        verify(bedAssignmentRepository, never()).save(any(BedAssignmentEntity.class));
    }

    @Test
    void shouldNotReleaseAssignmentWhenFreeIsRequestedManually() {
        bed.setStatus(BedStatus.CLEANING);
        BedAssignmentEntity assignment = new BedAssignmentEntity(UUID.randomUUID(), bed);
        when(bedAssignmentRepository.findActiveByBedId(bedId)).thenReturn(Optional.of(assignment));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> spatialService.updateBedStatus(bedId, BedStatus.FREE));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(BedStatus.CLEANING, bed.getStatus());
        assertNull(assignment.getReleasedAt());
        verify(bedAssignmentRepository, never()).save(any(BedAssignmentEntity.class));
    }

    @Test
    void shouldRequireReconciliationForOccupiedBedWithoutAssignment() {
        bed.setStatus(BedStatus.OCCUPIED);
        when(bedAssignmentRepository.findActiveByBedId(bedId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> spatialService.updateBedStatus(bedId, BedStatus.FREE));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(BedStatus.OCCUPIED, bed.getStatus());
        verify(bedRepository, never()).save(any(BedEntity.class));
    }

    @Test
    void shouldAllowOperationalTransitionForUnassignedBed() {
        when(bedAssignmentRepository.findActiveByBedId(bedId)).thenReturn(Optional.empty());
        when(bedRepository.save(bed)).thenReturn(bed);

        spatialService.updateBedStatus(bedId, BedStatus.MAINTENANCE);

        assertEquals(BedStatus.MAINTENANCE, bed.getStatus());
        assertEquals(BedReadinessStatus.MAINTENANCE, bed.getReadinessStatus());
        verify(bedRepository).save(bed);
    }

    @Test
    void shouldCloseAndReopenAnUnassignedReadyBedWithoutLosingReadiness() {
        when(bedAssignmentRepository.findActiveByBedId(bedId)).thenReturn(Optional.empty());
        when(bedRepository.save(bed)).thenReturn(bed);

        spatialService.updateBedCapacityStatus(bedId, BedCapacityStatus.CLOSED);

        assertEquals(BedCapacityStatus.CLOSED, bed.getCapacityStatus());
        assertEquals(BedReadinessStatus.READY, bed.getReadinessStatus());
        assertEquals(BedStatus.MAINTENANCE, bed.getStatus());

        spatialService.updateBedCapacityStatus(bedId, BedCapacityStatus.OPEN);

        assertEquals(BedCapacityStatus.OPEN, bed.getCapacityStatus());
        assertEquals(BedReadinessStatus.READY, bed.getReadinessStatus());
        assertEquals(BedStatus.FREE, bed.getStatus());
    }

    @Test
    void shouldRejectDeclaringAClosedBedFree() {
        when(bedAssignmentRepository.findActiveByBedId(bedId)).thenReturn(Optional.empty());
        when(bedRepository.save(bed)).thenReturn(bed);
        spatialService.updateBedCapacityStatus(bedId, BedCapacityStatus.CLOSED);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> spatialService.updateBedStatus(bedId, BedStatus.FREE));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(BedCapacityStatus.CLOSED, bed.getCapacityStatus());
        assertEquals(BedStatus.MAINTENANCE, bed.getStatus());
    }

    @Test
    void shouldRejectClosingAnAssignedBed() {
        bed.setStatus(BedStatus.OCCUPIED);
        BedAssignmentEntity assignment = new BedAssignmentEntity(UUID.randomUUID(), bed);
        when(bedAssignmentRepository.findActiveByBedId(bedId)).thenReturn(Optional.of(assignment));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> spatialService.updateBedCapacityStatus(bedId, BedCapacityStatus.CLOSED));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(BedCapacityStatus.OPEN, bed.getCapacityStatus());
        verify(bedRepository, never()).save(any(BedEntity.class));
    }
}
