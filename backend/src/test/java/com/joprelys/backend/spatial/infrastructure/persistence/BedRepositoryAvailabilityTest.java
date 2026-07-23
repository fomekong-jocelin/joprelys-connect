package com.joprelys.backend.spatial.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BedRepositoryAvailabilityTest {

    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private FacilitySpaceRepository spaceRepository;
    @Autowired
    private InpatientSpaceProfileRepository inpatientSpaceProfileRepository;
    @Autowired
    private BedRepository bedRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID organizationId;

    @AfterEach
    void cleanTestData() {
        TenantContext.clear();
        if (organizationId == null) {
            return;
        }
        jdbcTemplate.update("DELETE FROM beds WHERE organization_id = ?", organizationId);
        jdbcTemplate.update("DELETE FROM inpatient_space_profiles WHERE organization_id = ?", organizationId);
        jdbcTemplate.update("DELETE FROM facility_spaces WHERE organization_id = ?", organizationId);
        jdbcTemplate.update("DELETE FROM organizations WHERE id = ?", organizationId);
    }

    @Test
    void shouldClaimOnlyAnOpenReadyFreeBed() {
        OrganizationEntity organization = organizationRepository.save(new OrganizationEntity(
                "Clinique disponibilité",
                "bed-capacity-" + UUID.randomUUID() + "@joprelys.local",
                "000",
                "Adresse",
                "Douala"));
        organizationId = organization.getId();
        TenantContext.setTenantId(organizationId);

        FacilitySpaceEntity space = spaceRepository.saveAndFlush(new FacilitySpaceEntity(
                organizationId,
                null,
                "ROOM_401",
                "Chambre 401",
                "HOSPITAL_ROOM"));
        inpatientSpaceProfileRepository.saveAndFlush(new InpatientSpaceProfileEntity(
                space.getId(), organizationId, "HOSPITAL_ROOM", "STANDARD"));

        BedEntity available = saveBed(space, "401-A");
        BedEntity closed = saveBed(space, "401-B");
        closed.setCapacityStatus(BedCapacityStatus.CLOSED);
        bedRepository.saveAndFlush(closed);
        BedEntity maintenance = saveBed(space, "401-C");
        maintenance.setStatus(BedStatus.MAINTENANCE);
        bedRepository.saveAndFlush(maintenance);

        assertEquals(1, claim(available.getId()));
        assertEquals(0, claim(closed.getId()));
        assertEquals(0, claim(maintenance.getId()));
    }

    private BedEntity saveBed(FacilitySpaceEntity space, String number) {
        return bedRepository.saveAndFlush(new BedEntity(space, number));
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
