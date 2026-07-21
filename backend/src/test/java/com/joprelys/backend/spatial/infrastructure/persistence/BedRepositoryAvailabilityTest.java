package com.joprelys.backend.spatial.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.spatial.domain.HospitalServiceType;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BedRepositoryAvailabilityTest {

    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private WardRepository wardRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private BedRepository bedRepository;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void shouldClaimOnlyAnOpenReadyFreeBed() {
        OrganizationEntity organization = organizationRepository.save(new OrganizationEntity(
                "Clinique disponibilité",
                "bed-capacity-" + UUID.randomUUID() + "@joprelys.local",
                "000",
                "Adresse",
                "Douala"));
        TenantContext.setTenantId(organization.getId());

        WardEntity ward = new WardEntity("Hospitalisation", HospitalServiceType.HOSPITALIZATION);
        ward.setOrganizationId(organization.getId());
        ward = wardRepository.save(ward);

        RoomEntity room = new RoomEntity(ward, "401", 3, "STANDARD");
        room.setOrganizationId(organization.getId());
        room = roomRepository.save(room);

        BedEntity available = saveBed(room, organization.getId(), "401-A");
        BedEntity closed = saveBed(room, organization.getId(), "401-B");
        closed.setCapacityStatus(BedCapacityStatus.CLOSED);
        bedRepository.saveAndFlush(closed);
        BedEntity maintenance = saveBed(room, organization.getId(), "401-C");
        maintenance.setStatus(BedStatus.MAINTENANCE);
        bedRepository.saveAndFlush(maintenance);

        assertEquals(1, claim(available.getId()));
        assertEquals(0, claim(closed.getId()));
        assertEquals(0, claim(maintenance.getId()));
    }

    private BedEntity saveBed(RoomEntity room, UUID organizationId, String number) {
        BedEntity bed = new BedEntity(room, number);
        bed.setOrganizationId(organizationId);
        return bedRepository.saveAndFlush(bed);
    }

    private int claim(UUID bedId) {
        return bedRepository.claimIfAvailable(
                bedId,
                BedStatus.FREE,
                BedStatus.OCCUPIED,
                BedCapacityStatus.OPEN,
                BedReadinessStatus.READY);
    }
}
