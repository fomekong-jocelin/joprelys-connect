package com.joprelys.backend.hospitalorganization.api;

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
class HospitalOrganizationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private JwtService jwtService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JsonMapper jsonMapper;

    private OrganizationEntity organization;
    private String adminToken;
    private String doctorToken;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        organization = organizationRepository.save(new OrganizationEntity(
                "Clinique Organisation",
                "org-" + UUID.randomUUID() + "@joprelys.local",
                "123456",
                "Adresse",
                "Douala"));

        TenantContext.setTenantId(organization.getId());
        try {
            UserAccountEntity admin = new UserAccountEntity(
                    "admin-org-" + UUID.randomUUID() + "@joprelys.local",
                    "Admin Organisation",
                    "ADMIN_CLINIQUE",
                    "passhash");
            admin.setOrganizationId(organization.getId());
            admin = userAccountRepository.save(admin);
            adminToken = jwtService.createToken(admin).value();

            UserAccountEntity doctor = new UserAccountEntity(
                    "doctor-org-" + UUID.randomUUID() + "@joprelys.local",
                    "Dr Organisation",
                    "MEDECIN",
                    "passhash");
            doctor.setOrganizationId(organization.getId());
            doctor = userAccountRepository.save(doctor);
            doctorToken = jwtService.createToken(doctor).value();
        } finally {
            TenantContext.clear();
        }
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        if (organization != null && organization.getId() != null) {
            // Supprimer uniquement les unités de cette fixture, des feuilles vers la racine,
            // afin de respecter la FK hiérarchique (parent_id, organization_id).
            int deleted;
            do {
                deleted = jdbcTemplate.update(
                        """
                        DELETE FROM organizational_units
                        WHERE organization_id = ?
                          AND id NOT IN (
                              SELECT parent_id
                              FROM organizational_units
                              WHERE organization_id = ?
                                AND parent_id IS NOT NULL
                          )
                        """,
                        organization.getId(),
                        organization.getId());
            } while (deleted > 0);
        }
    }

    @Test
    void adminShouldCreateDirectServiceFromCatalogWithoutFreeTextName() throws Exception {
        mockMvc.perform(get("/api/hospital-organization/catalogs/services")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'GENERAL_MEDICINE')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'GENERAL_MEDICINE')].nameFr").value("Médecine générale"))
                .andExpect(jsonPath("$[?(@.code == 'GENERAL_MEDICINE')].nameEn").value("General medicine"));

        mockMvc.perform(post("/api/hospital-organization/units")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"SVC_GEN_MED",
                                  "unitType":"SERVICE",
                                  "serviceCatalogCode":"GENERAL_MEDICINE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SVC_GEN_MED"))
                .andExpect(jsonPath("$.name").isEmpty())
                .andExpect(jsonPath("$.unitType").value("SERVICE"))
                .andExpect(jsonPath("$.serviceCatalogCode").value("GENERAL_MEDICINE"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void adminShouldCreateCompleteOptionalHierarchy() throws Exception {
        String poleId = createNamedUnit("POLE_MED", "POLE", null, "Pôle médecine");
        String departmentId = createNamedUnit("DEP_MED", "DEPARTMENT", poleId, "Département médecine");
        String serviceId = createService("SVC_INTERNAL", departmentId, "INTERNAL_MEDICINE");

        mockMvc.perform(post("/api/hospital-organization/units")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"UNIT_DIAB",
                                  "unitType":"CARE_UNIT",
                                  "parentId":"%s",
                                  "name":"Unité diabétologie"
                                }
                                """.formatted(serviceId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentId").value(serviceId))
                .andExpect(jsonPath("$.unitType").value("CARE_UNIT"));
    }

    @Test
    void serviceShouldAcceptLocalNameAndRejectInvalidHierarchy() throws Exception {
        mockMvc.perform(post("/api/hospital-organization/units")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"SVC_FREE",
                                  "unitType":"SERVICE",
                                  "name":"Service tapé à la main",
                                  "serviceCatalogCode":"GENERAL_MEDICINE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Service tapé à la main"));

        mockMvc.perform(post("/api/hospital-organization/units")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"UNIT_ROOT",
                                  "unitType":"CARE_UNIT",
                                  "name":"Unité orpheline"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void doctorMustNotManageHospitalOrganization() throws Exception {
        mockMvc.perform(post("/api/hospital-organization/units")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"SVC_DENIED",
                                  "unitType":"SERVICE",
                                  "serviceCatalogCode":"GENERAL_MEDICINE"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivationShouldFailWhileAnActiveChildExists() throws Exception {
        String poleId = createNamedUnit("POLE_ACTIVE", "POLE", null, "Pôle actif");
        createService("SVC_CHILD", poleId, "GENERAL_MEDICINE");

        mockMvc.perform(post("/api/hospital-organization/units/" + poleId + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    private String createNamedUnit(String code, String type, String parentId, String name) throws Exception {
        String parentJson = parentId == null ? "null" : "\"" + parentId + "\"";
        String response = mockMvc.perform(post("/api/hospital-organization/units")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"%s",
                                  "unitType":"%s",
                                  "parentId":%s,
                                  "name":"%s"
                                }
                                """.formatted(code, type, parentJson, name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(response).get("id").asString();
    }

    private String createService(String code, String parentId, String catalogCode) throws Exception {
        String parentJson = parentId == null ? "null" : "\"" + parentId + "\"";
        String response = mockMvc.perform(post("/api/hospital-organization/units")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"%s",
                                  "unitType":"SERVICE",
                                  "parentId":%s,
                                  "serviceCatalogCode":"%s"
                                }
                                """.formatted(code, parentJson, catalogCode)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(response).get("id").asString();
    }
    @Test
    void cataloguesIncludeEveryDisciplineRaisedByTheClinicalAudit() throws Exception {
        String services = mockMvc.perform(get("/api/hospital-organization/catalogs/services")
                .header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String specialties = mockMvc.perform(get("/api/hospital-organization/catalogs/specialties")
                .header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (String code : java.util.List.of("ORTHOPEDICS_TRAUMATOLOGY", "NEONATOLOGY", "INFECTIOUS_DISEASES",
                "GASTROENTEROLOGY", "OPHTHALMOLOGY", "ENT", "STOMATOLOGY", "NEPHROLOGY", "HEMODIALYSIS")) {
            org.assertj.core.api.Assertions.assertThat(services).contains("\"code\":\"" + code + "\"");
            org.assertj.core.api.Assertions.assertThat(specialties).contains("\"code\":\"" + code + "\"");
        }
        org.assertj.core.api.Assertions.assertThat(specialties).contains("EMERGENCY_MEDICINE");
    }

}
