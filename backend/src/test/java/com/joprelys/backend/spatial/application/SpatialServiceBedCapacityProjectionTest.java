package com.joprelys.backend.spatial.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.spatial.api.BedResponse;
import com.joprelys.backend.spatial.api.WardOccupancyResponse;
import com.joprelys.backend.spatial.domain.HospitalServiceType;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedCapacityStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.RoomEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.RoomRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.WardEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.WardRepository;
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

    private SpatialService service;
    private WardEntity ward;
    private RoomEntity room;

    @BeforeEach
    void setUp() {
        service = new SpatialService(
                wardRepository,
                roomRepository,
                bedRepository,
                bedAssignmentRepository,
                activeBedAssignmentService,
                new BedStatusTransitionPolicy(),
                hospitalizationRepository,
                userAccountRepository,
                auditService);
        ward = new WardEntity("Médecine", HospitalServiceType.HOSPITALIZATION);
        room = new RoomEntity(ward, "101", 4, "STANDARD");
    }

    @Test
    void shouldSeparateInstalledOpenReadyOccupiedAndAvailableCounts() throws Exception {
        UUID wardId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        setId(ward, wardId);
        setId(room, roomId);

        BedEntity available = bed("101-A");
        BedEntity occupied = bed("101-B");
        occupied.setStatus(BedStatus.OCCUPIED);
        BedEntity closed = bed("101-C");
        closed.setCapacityStatus(BedCapacityStatus.CLOSED);
        BedEntity cleaning = bed("101-D");
        cleaning.setStatus(BedStatus.CLEANING);

        List<BedEntity> beds = List.of(available, occupied, closed, cleaning);
        when(wardRepository.findById(wardId)).thenReturn(java.util.Optional.of(ward));
        when(roomRepository.findByWardId(wardId)).thenReturn(List.of(room));
        when(bedRepository.findByWardId(wardId)).thenReturn(beds);
        when(bedAssignmentRepository.findActiveBedIds(beds.stream().map(BedEntity::getId).toList()))
                .thenReturn(List.of(occupied.getId()));

        WardOccupancyResponse response = service.getWardOccupancy(wardId);

        assertEquals(4, response.totalBedsCount());
        assertEquals(3, response.openBedsCount());
        assertEquals(2, response.readyBedsCount());
        assertEquals(1, response.occupiedBedsCount());
        assertEquals(1, response.availableBedsCount());

        BedResponse closedResponse = response.rooms().getFirst().beds().stream()
                .filter(bed -> bed.id().equals(closed.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals("CLOSED", closedResponse.capacityStatus());
        assertEquals("READY", closedResponse.readinessStatus());
        assertEquals("UNASSIGNED", closedResponse.usageStatus());
        assertEquals(false, closedResponse.available());
    }

    private BedEntity bed(String number) throws Exception {
        BedEntity bed = new BedEntity(room, number);
        setId(bed, UUID.randomUUID());
        return bed;
    }

    private void setId(Object target, UUID id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
