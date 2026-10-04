package com.joprelys.backend.spatial.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.hospitalorganization.domain.OrganizationalUnitType;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitRepository;
import com.joprelys.backend.spatial.api.HospitalLocationDtos.SaveFacilitySpaceRequest;
import com.joprelys.backend.spatial.api.HospitalLocationDtos.SaveUnitSpaceAssignmentRequest;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.OrganizationalUnitSpaceAssignmentRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HospitalLocationConfigurationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private OrganizationalUnitRepository unitRepository;
    @Autowired private FacilitySpaceRepository spaceRepository;
    @Autowired private OrganizationalUnitSpaceAssignmentRepository assignmentRepository;
    @Autowired private JwtService jwtService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JsonMapper jsonMapper;

    private OrganizationEntity organization;
    private UserAccountEntity admin;
    private String adminToken;
    private OrganizationalUnitEntity unit;
    private FacilitySpaceEntity space;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        organization = organizationRepository.save(new OrganizationEntity(
                "Clinique Spatial Test",
                "org-spatial-" + UUID.randomUUID() + "@joprelys.local",
                "123456",
                "Adresse",
                "Douala"));

        TenantContext.setTenantId(organization.getId());
        try {
            admin = new UserAccountEntity(
                    "admin-spatial-" + UUID.randomUUID() + "@joprelys.local",
                    "Admin Spatial",
                    "ADMIN_CLINIQUE",
                    "passhash");
            admin.setOrganizationId(organization.getId());
            userAccountRepository.save(admin);
            adminToken = jwtService.createToken(admin).value();

            unit = new OrganizationalUnitEntity(
                    organization.getId(),
                    null,
                    "UHCD_TEST",
                    "UHCD Test",
                    OrganizationalUnitType.CARE_UNIT,
                    null);
            unitRepository.save(unit);

            space = new FacilitySpaceEntity(
                    organization.getId(),
                    null,
                    "CH_TEST",
                    "Chambre Test",
                    "HOSPITAL_ROOM");
            spaceRepository.save(space);
        } finally {
            TenantContext.clear();
        }
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        if (organization == null || organization.getId() == null) {
            return;
        }
        UUID orgId = organization.getId();
        jdbcTemplate.update("DELETE FROM organizational_unit_space_assignments WHERE organization_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM beds WHERE organization_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM inpatient_space_profiles WHERE organization_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM facility_spaces WHERE organization_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM facility_location_nodes WHERE organization_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM organizational_units WHERE organization_id = ?", orgId);
        if (admin != null && admin.getId() != null) {
            jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", admin.getId());
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", admin.getId());
        }
        jdbcTemplate.update("DELETE FROM organizations WHERE id = ?", orgId);
    }

    @Test
    void shouldCreateUnitSpaceAssignmentWithNullValidTo() throws Exception {
        SaveUnitSpaceAssignmentRequest request = new SaveUnitSpaceAssignmentRequest(
                unit.getId(),
                space.getId(),
                Instant.parse("2026-10-04T07:00:00Z"),
                null);

        mockMvc.perform(post("/api/spatial/configuration/unit-space-assignments")
                        .param("organizationId", organization.getId().toString())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.organizationalUnitId").value(unit.getId().toString()))
                .andExpect(jsonPath("$.spaceId").value(space.getId().toString()));
    }

    @Test
    void shouldRejectOverlappingUnitSpaceAssignment() throws Exception {
        SaveUnitSpaceAssignmentRequest initial = new SaveUnitSpaceAssignmentRequest(
                unit.getId(),
                space.getId(),
                Instant.parse("2026-10-04T07:00:00Z"),
                null);

        mockMvc.perform(post("/api/spatial/configuration/unit-space-assignments")
                        .param("organizationId", organization.getId().toString())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(initial)))
                .andExpect(status().isCreated());

        SaveUnitSpaceAssignmentRequest conflict = new SaveUnitSpaceAssignmentRequest(
                unit.getId(),
                space.getId(),
                Instant.parse("2026-10-04T08:00:00Z"),
                null);

        mockMvc.perform(post("/api/spatial/configuration/unit-space-assignments")
                        .param("organizationId", organization.getId().toString())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(conflict)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldCreateBoundedAndNonOverlappingAssignment() throws Exception {
        SaveUnitSpaceAssignmentRequest first = new SaveUnitSpaceAssignmentRequest(
                unit.getId(),
                space.getId(),
                Instant.parse("2026-10-04T07:00:00Z"),
                Instant.parse("2026-10-04T12:00:00Z"));

        mockMvc.perform(post("/api/spatial/configuration/unit-space-assignments")
                        .param("organizationId", organization.getId().toString())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        SaveUnitSpaceAssignmentRequest second = new SaveUnitSpaceAssignmentRequest(
                unit.getId(),
                space.getId(),
                Instant.parse("2026-10-04T12:00:00Z"),
                Instant.parse("2026-10-04T18:00:00Z"));

        mockMvc.perform(post("/api/spatial/configuration/unit-space-assignments")
                        .param("organizationId", organization.getId().toString())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(second)))
                .andExpect(status().isCreated());
    }
}
