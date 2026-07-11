package com.joprelys.backend.emergency.medicolegal.api;

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
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmergencyMedicoLegalControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired UserAccountRepository userAccountRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired EmergencyRepository emergencyRepository;
    @Autowired JwtService jwtService;
    @Autowired JdbcTemplate jdbcTemplate;

    private OrganizationEntity organization;
    private EmergencyEntity emergency;
    private String doctorToken;
    private String receptionToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM emergency_admission_requests");
        jdbcTemplate.update("DELETE FROM resuscitation_logs");
        jdbcTemplate.update("DELETE FROM emergencies");
        jdbcTemplate.update("DELETE FROM patient_identity_declarations");
        jdbcTemplate.update("DELETE FROM patient_identity_status_history");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        organization = organizationRepository.save(
                new OrganizationEntity("Clinique A", "contact@clinic-a.local", "123", "Akwa", "Douala"));

        UserAccountEntity doctor = new UserAccountEntity(
                "doctor@clinic-a.local", "Doctor A", "MEDECIN", "passhash");
        doctor.setOrganizationId(organization.getId());
        doctor = userAccountRepository.save(doctor);
        doctorToken = jwtService.createToken(doctor).value();

        UserAccountEntity reception = new UserAccountEntity(
                "reception@clinic-a.local", "Reception A", "AGENT_ACCUEIL", "passhash");
        reception.setOrganizationId(organization.getId());
        reception = userAccountRepository.save(reception);
        receptionToken = jwtService.createToken(reception).value();

        TenantContext.setTenantId(organization.getId());
        PatientEntity patient = patientRepository.save(new PatientEntity(
                "DPU-ML-001",
                "PAT-ML-001",
                "Patient Medico Legal",
                "MASCULIN",
                LocalDate.of(1985, 5, 5),
                "+237699000000",
                "Douala",
                null,
                null,
                null,
                null,
                null,
                null));
        emergency = emergencyRepository.save(new EmergencyEntity(
                patient,
                null,
                "ACCOMPANIED",
                "RED",
                "SHOCK",
                "Patient inconscient après accident",
                90,
                55,
                120,
                BigDecimal.valueOf(36.8),
                doctor.getId()));
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldStoreMultipleThirdPartyQualitiesAndSourcedIdentityStatements() throws Exception {
        String body = """
                {
                  "fullName": "Paul Tamo",
                  "phone": "+237699000111",
                  "idDocument": "CNI 123456789",
                  "relationshipToPatient": "WITNESS",
                  "circumstances": "Patient trouvé sur la voie publique",
                  "consentToContact": true,
                  "legalRepresentativeClaimed": false,
                  "sourceType": "WITNESS",
                  "confidenceLevel": "LOW",
                  "qualities": ["ACCOMPANYING_PERSON", "WITNESS", "DECLARANT"],
                  "identityStatements": [
                    {
                      "fieldName": "fullName",
                      "value": "Jean Nkoa",
                      "confidenceLevel": "LOW",
                      "proofReference": "Déclaration orale"
                    }
                  ]
                }
                """;

        mockMvc.perform(post(endpoint("/third-parties"))
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.thirdParties[0].fullName").value("Paul Tamo"))
                .andExpect(jsonPath("$.thirdParties[0].qualities.length()").value(3))
                .andExpect(jsonPath("$.thirdParties[0].legalRepresentativeClaimed").value(false))
                .andExpect(jsonPath("$.identityStatements[0].fieldName").value("fullName"))
                .andExpect(jsonPath("$.identityStatements[0].value").value("Jean Nkoa"));
    }

    @Test
    void shouldCloseEmergencyLegalBasisWhenCapacityIsRestored() throws Exception {
        mockMvc.perform(post(endpoint("/capacity-events"))
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "INCAPABLE",
                                  "consciousnessLevel": "UNCONSCIOUS",
                                  "clinicalReason": "Glasgow 6/15"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentCapacity.status").value("INCAPABLE"));

        mockMvc.perform(post(endpoint("/legal-bases"))
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "basisType": "VITAL_EMERGENCY",
                                  "justification": "Détresse vitale et impossibilité de consentir",
                                  "coveredActs": ["AIRWAY_MANAGEMENT", "VASCULAR_ACCESS"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activeLegalBasis.basisType").value("VITAL_EMERGENCY"))
                .andExpect(jsonPath("$.activeLegalBasis.active").value(true));

        mockMvc.perform(post(endpoint("/capacity-events"))
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CAPABLE",
                                  "consciousnessLevel": "ALERT",
                                  "clinicalReason": "Patient réveillé, orienté et apte à consentir"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentCapacity.status").value("CAPABLE"))
                .andExpect(jsonPath("$.activeLegalBasis").doesNotExist())
                .andExpect(jsonPath("$.legalBases[0].closureReason").value("CAPACITY_RESTORED"));
    }

    @Test
    void shouldRequireRecipientIdentityAndKeepCompleteCustodyHistory() throws Exception {
        String createResponse = mockMvc.perform(post(endpoint("/belongings"))
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "PHONE",
                                  "description": "Téléphone noir écran fissuré",
                                  "quantity": 1,
                                  "itemCondition": "Écran fissuré",
                                  "sealNumber": "SC-0001",
                                  "depositedByName": "Paul Tamo"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.belongings[0].custodyStatus").value("IN_CUSTODY"))
                .andExpect(jsonPath("$.belongings[0].transfers[0].action").value("DEPOSITED"))
                .andReturn().getResponse().getContentAsString();

        String belongingId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.belongings[0].id");

        mockMvc.perform(post(endpoint("/belongings/" + belongingId + "/transfers"))
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "RELEASED",
                                  "recipientName": "Marie Nkoa"
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(endpoint("/belongings/" + belongingId + "/transfers"))
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "RELEASED",
                                  "fromCustodian": "Service des urgences",
                                  "recipientName": "Marie Nkoa",
                                  "recipientIdDocument": "CNI 99887766",
                                  "notes": "Remis contre signature"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.belongings[0].custodyStatus").value("RELEASED"))
                .andExpect(jsonPath("$.belongings[0].transfers.length()").value(2))
                .andExpect(jsonPath("$.belongings[0].transfers[1].recipientIdDocument").value("CNI 99887766"));
    }

    @Test
    void shouldDenyMedicoLegalDataToReceptionRole() throws Exception {
        mockMvc.perform(get(endpoint(""))
                        .header("Authorization", bearer(receptionToken)))
                .andExpect(status().isForbidden());
    }

    private String endpoint(String suffix) {
        return "/api/emergencies/" + emergency.getId() + "/medico-legal" + suffix;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
