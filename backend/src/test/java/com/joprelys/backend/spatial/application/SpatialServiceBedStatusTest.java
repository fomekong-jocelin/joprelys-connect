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
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.HospitalServiceCatalogRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedCapacityStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedReadinessStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStateAxis;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStateReasonCode;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceProfileRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.OrganizationalUnitSpaceAssignmentRepository;
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

    @Mock private BedRepository bedRepository;
    @Mock private BedAssignmentRepository bedAssignmentRepository;
    @Mock private ActiveBedAssignmentService activeBedAssignmentService;
    @Mock private BedStateChangeService bedStateChangeService;
    @Mock private HospitalizationRepository hospitalizationRepository;
    @Mock private FacilitySpaceRepository spaceRepository;
    @Mock private InpatientSpaceProfileRepository inpatientSpaceProfileRepository;
    @Mock private OrganizationalUnitSpaceAssignmentRepository unitSpaceAssignmentRepository;
    @Mock private OrganizationalUnitRepository organizationalUnitRepository;
    @Mock private HospitalServiceCatalogRepository serviceCatalogRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private AuditService auditService;

    private SpatialService spatialService;
    private UUID bedId;
    private BedEntity bed;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        spatialService = new SpatialService(
                bedRepository,
                bedAssignmentRepository,
                activeBedAssignmentService,
                new BedStatusTransitionPolicy(),
                bedStateChangeService,
                hospitalizationRepository,
                spaceRepository,
                inpatientSpaceProfileRepository,
                unitSpaceAssignmentRepository,
                organizationalUnitRepository,
                serviceCatalogRepository,
                userAccountRepository,
                auditService);

        FacilitySpaceEntity space = new FacilitySpaceEntity(
                UUID.randomUUID(), null, "SPACE_101", "Chambre 101", "HOSPITAL_ROOM");
        bed = new BedEntity(space, "101-A");
        bedId = UUID.randomUUID();
        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));
    }

    @Test
    void shouldRejectDirectOccupiedStatus() {
        when(bedAssignmentRepository.findActiveByBedId(bedId)).thenReturn(Optional.empty());
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> spatialService.updateBedStatus(bedId, BedStatus.OCCUPIED, null));
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
                () -> spatialService.updateBedStatus(bedId, BedStatus.MAINTENANCE, null));
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
                () -> spatialService.updateBedStatus(bedId, BedStatus.FREE, null));
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
                () -> spatialService.updateBedStatus(bedId, BedStatus.FREE, null));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(BedStatus.OCCUPIED, bed.getStatus());
        verify(bedRepository, never()).save(any(BedEntity.class));
    }

    @Test
    void shouldAllowOperationalTransitionForUnassignedBed() {
        when(bedAssignmentRepository.findActiveByBedId(bedId)).thenReturn(Optional.empty());
        when(bedRepository.save(bed)).thenReturn(bed);
        spatialService.updateBedStatus(bedId, BedStatus.MAINTENANCE, "Supervision exceptionnelle");
        assertEquals(BedStatus.MAINTENANCE, bed.getStatus());
        assertEquals(BedReadinessStatus.MAINTENANCE, bed.getReadinessStatus());
        verify(bedRepository).save(bed);
        verify(bedStateChangeService).recordLegacySupervision(
                bed,
                BedStateAxis.READINESS,
                BedReadinessStatus.READY.name(),
                BedReadinessStatus.MAINTENANCE.name(),
                "Supervision exceptionnelle");
    }

    @Test
    void shouldCloseAndReopenAnUnassignedReadyBedWithoutLosingReadiness() {
        when(bedAssignmentRepository.findActiveByBedId(bedId)).thenReturn(Optional.empty());
        when(bedRepository.save(bed)).thenReturn(bed);
        spatialService.updateBedCapacityStatus(
                bedId, BedCapacityStatus.CLOSED,
                BedStateReasonCode.CAPACITY_TEMPORARY_CLOSURE, "Fermeture planifiée");
        assertEquals(BedCapacityStatus.CLOSED, bed.getCapacityStatus());
        assertEquals(BedReadinessStatus.READY, bed.getReadinessStatus());
        assertEquals(BedStatus.MAINTENANCE, bed.getStatus());
        spatialService.updateBedCapacityStatus(
                bedId, BedCapacityStatus.OPEN,
                BedStateReasonCode.CAPACITY_REOPENING, "Réouverture validée");
        assertEquals(BedCapacityStatus.OPEN, bed.getCapacityStatus());
        assertEquals(BedReadinessStatus.READY, bed.getReadinessStatus());
        assertEquals(BedStatus.FREE, bed.getStatus());
    }

    @Test
    void shouldRejectDeclaringAClosedBedFree() {
        when(bedAssignmentRepository.findActiveByBedId(bedId)).thenReturn(Optional.empty());
        when(bedRepository.save(bed)).thenReturn(bed);
        spatialService.updateBedCapacityStatus(
                bedId, BedCapacityStatus.CLOSED,
                BedStateReasonCode.CAPACITY_SAFETY, "Contrôle sécurité");
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> spatialService.updateBedStatus(bedId, BedStatus.FREE, null));
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
                () -> spatialService.updateBedCapacityStatus(
                        bedId, BedCapacityStatus.CLOSED,
                        BedStateReasonCode.CAPACITY_SAFETY, "Danger identifié"));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(BedCapacityStatus.OPEN, bed.getCapacityStatus());
        verify(bedRepository, never()).save(any(BedEntity.class));
    }

    @Test
    void cleaningCircuitShouldStartAndCompleteCleaning() {
        when(bedAssignmentRepository.findActiveByBedId(bedId)).thenReturn(Optional.empty());
        when(bedRepository.save(bed)).thenReturn(bed);
        spatialService.updateBedCleaningStatus(
                bedId, BedReadinessStatus.CLEANING,
                BedStateReasonCode.CLEANING_ROUTINE, "Nettoyage de routine");
        assertEquals(BedReadinessStatus.CLEANING, bed.getReadinessStatus());
        assertEquals(BedStatus.CLEANING, bed.getStatus());
        spatialService.updateBedCleaningStatus(
                bedId, BedReadinessStatus.READY,
                BedStateReasonCode.CLEANING_COMPLETED, "Contrôle visuel conforme");
        assertEquals(BedReadinessStatus.READY, bed.getReadinessStatus());
        assertEquals(BedStatus.FREE, bed.getStatus());
    }

    @Test
    void maintenanceCircuitShouldStartAndCompleteMaintenance() {
        when(bedAssignmentRepository.findActiveByBedId(bedId)).thenReturn(Optional.empty());
        when(bedRepository.save(bed)).thenReturn(bed);
        spatialService.updateBedMaintenanceStatus(
                bedId, BedReadinessStatus.MAINTENANCE,
                BedStateReasonCode.MAINTENANCE_CORRECTIVE, "Frein défectueux");
        assertEquals(BedReadinessStatus.MAINTENANCE, bed.getReadinessStatus());
        assertEquals(BedStatus.MAINTENANCE, bed.getStatus());
        spatialService.updateBedMaintenanceStatus(
                bedId, BedReadinessStatus.READY,
                BedStateReasonCode.MAINTENANCE_COMPLETED, "Essai fonctionnel conforme");
        assertEquals(BedReadinessStatus.READY, bed.getReadinessStatus());
        assertEquals(BedStatus.FREE, bed.getStatus());
    }

    @Test
    void cleaningCircuitShouldNotReleaseAMaintenanceBed() {
        bed.setReadinessStatus(BedReadinessStatus.MAINTENANCE);
        when(bedAssignmentRepository.findActiveByBedId(bedId)).thenReturn(Optional.empty());
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> spatialService.updateBedCleaningStatus(
                        bedId, BedReadinessStatus.READY,
                        BedStateReasonCode.CLEANING_COMPLETED, "Tentative invalide"));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(BedReadinessStatus.MAINTENANCE, bed.getReadinessStatus());
        verify(bedRepository, never()).save(any(BedEntity.class));
    }

    @Test
    void maintenanceCircuitShouldNotReleaseACleaningBed() {
        bed.setReadinessStatus(BedReadinessStatus.CLEANING);
        when(bedAssignmentRepository.findActiveByBedId(bedId)).thenReturn(Optional.empty());
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> spatialService.updateBedMaintenanceStatus(
                        bedId, BedReadinessStatus.READY,
                        BedStateReasonCode.MAINTENANCE_COMPLETED, "Tentative invalide"));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(BedReadinessStatus.CLEANING, bed.getReadinessStatus());
        verify(bedRepository, never()).save(any(BedEntity.class));
    }
}
