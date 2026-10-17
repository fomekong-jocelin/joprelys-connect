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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmergencyControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired UserAccountRepository userAccountRepository;
    @Autowired PatientRepository patientRepository;
    @Autowired EmergencyRepository emergencyRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired JwtService jwtService;

    private OrganizationEntity organization;
    private UserAccountEntity doctor;
    private String doctorToken;
    private PatientEntity patient;
    private EmergencyEntity emergency;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM vitals");
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
                new OrganizationEntity("Clinique A", "contacta@joprelys.local", "123", "Street A", "Douala"));

        doctor = new UserAccountEntity("medecin.a@joprelys.local", "Médecin A", "MEDECIN", "passhash");
        doctor.setOrganizationId(organization.getId());
        doctor = userAccountRepository.save(doctor);
        doctorToken = jwtService.createToken(doctor).value();

        TenantContext.setTenantId(organization.getId());
        patient = patientRepository.save(new PatientEntity(
                "DPU-A", "PAT-A", "Patient A", "MASCULIN",
                LocalDate.of(1990, 1, 1), "+123", "Douala", "", "", "", "", "", ""));
        emergency = emergencyRepository.save(new EmergencyEntity(
                patient,
                null,
                "AMBULANCE",
                "RED",
                "SHOCK",
                "Accident moto",
                120,
                80,
                90,
                BigDecimal.valueOf(37.5),
                doctor.getId()));
        TenantContext.clear();
    }

    @Test
    void shouldPersistAccompanyingPersonButMaskSensitiveIdentityInGeneralResponse() throws Exception {
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
                """.formatted(patient.getId());

        mockMvc.perform(post("/api/emergencies")
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.arrivalMode").value("ACCOMPANIED"))
                .andExpect(jsonPath("$.thirdPartyRecorded").value(true))
                .andExpect(jsonPath("$.thirdPartyName").doesNotExist())
                .andExpect(jsonPath("$.thirdPartyPhone").doesNotExist())
                .andExpect(jsonPath("$.thirdPartyIdDocument").doesNotExist())
                .andExpect(jsonPath("$.thirdPartyCircumstances").doesNotExist());
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
                """.formatted(patient.getId());

        mockMvc.perform(post("/api/emergencies")
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldExposeProvisionalClinicalIdentityWithoutThirdPartyEvidence() throws Exception {
        TenantContext.setTenantId(organization.getId());
        PatientEntity provisionalPatient = patientRepository.save(PatientEntity.provisionalEmergency(
                "DPU-JOP-20260711-999999",
                "PAT-20260711-999999",
                "URG-TEMP-20260711-999999",
                "MASCULIN",
                "35-45",
                "Cicatrice au front, chemise bleue",
                Instant.parse("2026-07-11T13:00:00Z"),
                "Carrefour Akwa",
                IdentityConfidenceLevel.NONE));

        EmergencyEntity provisionalEmergency = emergencyRepository.save(new EmergencyEntity(
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
                doctor.getId()));
        TenantContext.clear();

        mockMvc.perform(get("/api/emergencies/" + provisionalEmergency.getId())
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientName").value("URG-TEMP-20260711-999999"))
                .andExpect(jsonPath("$.temporaryPatientNumber").value("URG-TEMP-20260711-999999"))
                .andExpect(jsonPath("$.identityStatus").value("PROVISIONAL_URGENCY"))
                .andExpect(jsonPath("$.physicalDescription").value("Cicatrice au front, chemise bleue"))
                .andExpect(jsonPath("$.foundLocation").value("Carrefour Akwa"))
                .andExpect(jsonPath("$.thirdPartyRecorded").value(true))
                .andExpect(jsonPath("$.thirdPartyName").doesNotExist());
    }

    @Test
    void shouldAddResuscitationLogAndListPatientEmergency() throws Exception {
        mockMvc.perform(post("/api/emergencies/" + emergency.getId() + "/resuscitation")
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "FLUID_BOLUS",
                                  "quantity": 45,
                                  "unit": "ml",
                                  "description": "Ringer lactate"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionType").value("FLUID_BOLUS"))
                .andExpect(jsonPath("$.quantity").value(45));

        mockMvc.perform(get("/api/emergencies/patient/" + patient.getId())
                        .header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(emergency.getId().toString()))
                .andExpect(jsonPath("$[0].patientName").value("Patient A"))
                .andExpect(jsonPath("$[0].chiefComplaint").value("Accident moto"));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void closedEmergencyVitalsAreNotCopiedIntoALaterUnrelatedVisit() throws Exception {
        mockMvc.perform(post("/api/emergencies/" + emergency.getId() + "/stabilize")
                        .header("Authorization", bearer(doctorToken))
                        .param("orientation", "DISCHARGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stabilizedAt").isNotEmpty())
                .andExpect(jsonPath("$.stabilizedBy").value(doctor.getId().toString()));

        mockMvc.perform(post("/api/visits")
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"%s\",\"reason\":\"Suivi diabète\",\"orientation\":\"CONSULTATION\"}"
                                .formatted(patient.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vitals").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.careStage").value("ATTENTE_CONSTANTES"));
    }

    @Test
    void consultationOrientationKeepsTheEmergencyOpenUntilTheVisitAcceptsThePatient() throws Exception {
        mockMvc.perform(post("/api/emergencies/" + emergency.getId() + "/stabilize")
                        .header("Authorization", bearer(doctorToken))
                        .param("orientation", "CONSULTATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.awaitingDownstream").value(true))
                .andExpect(jsonPath("$.stabilizedAt").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(get("/api/emergencies/active").header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(emergency.getId().toString()));

        mockMvc.perform(post("/api/visits")
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"%s\",\"reason\":\"Avis spécialisé\",\"orientation\":\"CONSULTATION\"}"
                                .formatted(patient.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vitals.pulse").value(90));

        mockMvc.perform(get("/api/emergencies/" + emergency.getId()).header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stabilizedAt").isNotEmpty())
                .andExpect(jsonPath("$.visitId").isNotEmpty());
    }

    @Test
    void deathOrientationDeclaresTheDeathAndBlocksNewEpisodes() throws Exception {
        mockMvc.perform(post("/api/emergencies/" + emergency.getId() + "/stabilize")
                        .header("Authorization", bearer(doctorToken))
                        .param("orientation", "DEATH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orientation").value("DEATH"))
                .andExpect(jsonPath("$.stabilizedAt").isNotEmpty());

        mockMvc.perform(get("/api/patients/" + patient.getId()).header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECEASED"))
                .andExpect(jsonPath("$.deceasedAt").isNotEmpty());

        mockMvc.perform(post("/api/visits")
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"%s\",\"reason\":\"Test\",\"orientation\":\"CONSULTATION\"}"
                                .formatted(patient.getId())))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/patients/" + patient.getId() + "/death/revoke")
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Erreur de dossier : homonyme\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
