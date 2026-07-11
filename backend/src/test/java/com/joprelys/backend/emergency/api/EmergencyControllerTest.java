package com.joprelys.backend.emergency.api;

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
import com.joprelys.backend.patient.domain.IdentityConfidenceLevel;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EmergencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private EmergencyRepository emergencyRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    private OrganizationEntity orgA;
    private UserAccountEntity userMedecinA;
    private String tokenMedecinA;
    private PatientEntity patientA;
    private EmergencyEntity emergencyA;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM resuscitation_logs");
        jdbcTemplate.update("DELETE FROM emergencies");
        jdbcTemplate.update("DELETE FROM patient_identity_declarations");
        jdbcTemplate.update("DELETE FROM patient_identity_status_history");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        orgA = new OrganizationEntity("Clinique A", "contacta@joprelys.local", "123", "Street A", "Douala");
        orgA = organizationRepository.save(orgA);

        userMedecinA = new UserAccountEntity("medecin.a@joprelys.local", "Médecin A", "MEDECIN", "passhash");
        userMedecinA.setOrganizationId(orgA.getId());
        userMedecinA = userAccountRepository.save(userMedecinA);

        tokenMedecinA = jwtService.createToken(userMedecinA).value();

        TenantContext.setTenantId(orgA.getId());

        patientA = new PatientEntity("DPU-A", "PAT-A", "Patient A", "MASCULIN", LocalDate.of(1990, 1, 1), "+123", "Douala", "", "", "", "", "", "");
        patientA = patientRepository.save(patientA);

        emergencyA = new EmergencyEntity(
                patientA,
                null,
                "AMBULANCE",
                "RED",
                "SHOCK",
                "Accident moto",
                120,
                80,
                90,
                BigDecimal.valueOf(37.5),
                userMedecinA.getId()
        );
        emergencyA = emergencyRepository.save(emergencyA);

        TenantContext.clear();
    }

    @Test
    void shouldPersistArrivalThirdPartyWhenPatientIsAccompanied() throws Exception {
        jdbcTemplate.update("DELETE FROM emergencies");

        String body = """
                {
                  "patientId": "%s",
                  "arrivalMode": "ACCOMPANIED",
                  "triageLevel": "ORANGE",
                  "hemodynamicStatus": "UNSTABLE",
                  "chiefComplaint": "Traumatisme après accident",
                  "thirdPartyName": "Paul Tamo",
                  "thirdPartyPhone": "+237699000111",
                  "thirdPartyRelationship": "WITNESS",
                  "thirdPartyIdDocument": "CNI 123456789",
                  "thirdPartyCircumstances": "A trouvé le patient sur la voie publique.",
                  "thirdPartyConsentToContact": true
                }
                """.formatted(patientA.getId());

        mockMvc.perform(post("/api/emergencies")
                        .header("Authorization", "Bearer " + tokenMedecinA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.arrivalMode").value("ACCOMPANIED"))
                .andExpect(jsonPath("$.thirdPartyName").value("Paul Tamo"))
                .andExpect(jsonPath("$.thirdPartyPhone").value("+237699000111"))
                .andExpect(jsonPath("$.thirdPartyRelationship").value("WITNESS"))
                .andExpect(jsonPath("$.thirdPartyConsentToContact").value(true));
    }

    @Test
    void shouldRejectAccompaniedArrivalWithoutRequiredThirdPartyDetails() throws Exception {
        jdbcTemplate.update("DELETE FROM emergencies");

        String body = """
                {
                  "patientId": "%s",
                  "arrivalMode": "ACCOMPANIED",
                  "triageLevel": "ORANGE",
                  "hemodynamicStatus": "UNSTABLE",
                  "chiefComplaint": "Traumatisme"
                }
                """.formatted(patientA.getId());

        mockMvc.perform(post("/api/emergencies")
                        .header("Authorization", "Bearer " + tokenMedecinA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldExposeCompleteProvisionalIdentityInEmergencyDetails() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        PatientEntity provisionalPatient = PatientEntity.provisionalEmergency(
                "DPU-JOP-20260711-999999",
                "PAT-20260711-999999",
                "URG-TEMP-20260711-999999",
                "MASCULIN",
                "35-45",
                "Cicatrice au front, chemise bleue",
                Instant.parse("2026-07-11T13:00:00Z"),
                "Carrefour Akwa",
                IdentityConfidenceLevel.NONE
        );
        provisionalPatient = patientRepository.save(provisionalPatient);

        EmergencyEntity provisionalEmergency = new EmergencyEntity(
                provisionalPatient,
                null,
                "ACCOMPANIED",
                "RED",
                "SHOCK",
                "Traumatisme crânien, patient inconscient",
                90,
                55,
                52,
                BigDecimal.valueOf(37.0),
                "Paul Tamo",
                "+237699000111",
                "WITNESS",
                "CNI 123456789",
                "Patient trouvé sur la voie publique.",
                true,
                userMedecinA.getId()
        );
        provisionalEmergency = emergencyRepository.save(provisionalEmergency);
        TenantContext.clear();

        mockMvc.perform(get("/api/emergencies/" + provisionalEmergency.getId())
                        .header("Authorization", "Bearer " + tokenMedecinA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientName").value("URG-TEMP-20260711-999999"))
                .andExpect(jsonPath("$.temporaryPatientNumber").value("URG-TEMP-20260711-999999"))
                .andExpect(jsonPath("$.globalPatientNumber").value("DPU-JOP-20260711-999999"))
                .andExpect(jsonPath("$.identityStatus").value("PROVISIONAL_URGENCY"))
                .andExpect(jsonPath("$.apparentGender").value("MASCULIN"))
                .andExpect(jsonPath("$.estimatedAgeRange").value("35-45"))
                .andExpect(jsonPath("$.physicalDescription").value("Cicatrice au front, chemise bleue"))
                .andExpect(jsonPath("$.foundLocation").value("Carrefour Akwa"))
                .andExpect(jsonPath("$.thirdPartyName").value("Paul Tamo"));
    }

    @Test
    void testAddResuscitationLog() throws Exception {
        String body = """
                {
                  "actionType": "FLUID_BOLUS",
                  "quantity": 45,
                  "unit": "ml",
                  "description": "Ringer lactate"
                }
                """;

        mockMvc.perform(post("/api/emergencies/" + emergencyA.getId() + "/resuscitation")
                        .header("Authorization", "Bearer " + tokenMedecinA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionType").value("FLUID_BOLUS"))
                .andExpect(jsonPath("$.quantity").value(45))
                .andExpect(jsonPath("$.unit").value("ml"))
                .andExpect(jsonPath("$.description").value("Ringer lactate"));
    }

    @Test
    void testGetPatientEmergencies() throws Exception {
        mockMvc.perform(get("/api/emergencies/patient/" + patientA.getId())
                        .header("Authorization", "Bearer " + tokenMedecinA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(emergencyA.getId().toString()))
                .andExpect(jsonPath("$[0].patientName").value("Patient A"))
                .andExpect(jsonPath("$[0].globalPatientNumber").value("DPU-A"))
                .andExpect(jsonPath("$[0].chiefComplaint").value("Accident moto"));
    }
}
