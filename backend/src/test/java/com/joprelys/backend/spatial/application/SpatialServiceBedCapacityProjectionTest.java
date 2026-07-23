package com.joprelys.backend.spatial.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.HospitalServiceCatalogRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitRepository;
import com.joprelys.backend.spatial.api.BedResponse;
import com.joprelys.backend.spatial.api.SpaceOccupancyResponse;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedCapacityStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceProfileRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.OrganizationalUnitSpaceAssignmentRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpatialServiceBedCapacityProjectionTest {

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

    private SpatialService service;
    private FacilitySpaceEntity space;

    @BeforeEach
    void setUp() {
        service = new SpatialService(
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
        space = new FacilitySpaceEntity(UUID.randomUUID(), null, "SPACE_101", "Chambre 101", "HOSPITAL_ROOM");
    }

    @Test
    void shouldSeparateInstalledOpenReadyOccupiedAndAvailableCounts() throws Exception {
        UUID spaceId = UUID.randomUUID();
        setId(space, spaceId);

        BedEntity available = bed("101-A");
        BedEntity occupied = bed("101-B");
        occupied.setStatus(BedStatus.OCCUPIED);
        BedEntity closed = bed("101-C");
        closed.setCapacityStatus(BedCapacityStatus.CLOSED);
        BedEntity cleaning = bed("101-D");
        cleaning.setStatus(BedStatus.CLEANING);

        List<BedEntity> beds = List.of(available, occupied, closed, cleaning);
        when(spaceRepository.findById(spaceId)).thenReturn(java.util.Optional.of(space));
        when(bedRepository.findBySpaceId(spaceId)).thenReturn(beds);
        when(bedAssignmentRepository.findActiveBedIds(beds.stream().map(BedEntity::getId).toList()))
                .thenReturn(List.of(occupied.getId()));

        SpaceOccupancyResponse response = service.getSpaceOccupancy(spaceId);

        assertEquals(4, response.installedBeds());
        assertEquals(3, response.openBeds());
        assertEquals(2, response.readyBeds());
        assertEquals(1, response.occupiedBeds());
        assertEquals(1, response.availableBeds());

        BedResponse closedResponse = response.beds().stream()
                .filter(bed -> bed.id().equals(closed.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals("CLOSED", closedResponse.capacityStatus());
        assertEquals("READY", closedResponse.readinessStatus());
        assertEquals("UNASSIGNED", closedResponse.usageStatus());
        assertEquals(false, closedResponse.available());
    }

    private BedEntity bed(String number) throws Exception {
        BedEntity bed = new BedEntity(space, number);
        setId(bed, UUID.randomUUID());
        return bed;
    }

    private void setId(Object target, UUID id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
