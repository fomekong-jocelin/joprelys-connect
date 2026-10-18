package com.joprelys.backend.hospitalization.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Continuité des épisodes : fin de visite, sortie, désactivation d'unité et décès (ADR-20261017 D-07 à D-10). */
class JourneyContinuityControllerTest extends HospitalizationControllerTestSupport {

    private static final String DECISION = """
            {"dischargeDiagnosis":"Amélioration clinique","dischargeInstructions":"Contrôle à J7"}
            """;

    @Test
    void physicalDepartureClosesTheLinkedVisitSoThePatientCanReturn() throws Exception {
        VisitEntity visit = createVisit(patientA, "VIS-CONT-1", "Dyspnée", "MÉDECINE GÉNÉRALE");
        String stayId = admit(patientA, visit, PEDIATRICS_SERVICE, PEDIATRICS_SPACE, PEDIATRICS_BED, "Surveillance");

        mockMvc.perform(post("/api/visits/{id}/close", visit.getId()).header("Authorization", bearer(doctorToken)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/hospitalizations/{id}/discharge", stayId)
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON).content(DECISION))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/hospitalizations/{id}/discharge/cancel", stayId)
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Fièvre réapparue\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dischargeDecidedAt").isEmpty());
        mockMvc.perform(post("/api/hospitalizations/{id}/physical-departure", stayId)
                        .header("Authorization", bearer(hospitalizationManagerToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmed\":true}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/hospitalizations/{id}/discharge", stayId)
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON).content(DECISION))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/hospitalizations/{id}/physical-departure", stayId)
                        .header("Authorization", bearer(hospitalizationManagerToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmed\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/visits/{id}", visit.getId()).header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINEE"));
        mockMvc.perform(post("/api/visits")
                        .header("Authorization", bearer(billingAgentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"%s\",\"reason\":\"Contrôle J7\",\"orientation\":\"CONSULTATION\"}"
                                .formatted(patientA.getId())))
                .andExpect(status().isOk());
    }

    @Test
    void receptionAbandonsAVisitOnlyBeforeClinicalCare() throws Exception {
        VisitEntity visit = createVisit(patientB, "VIS-CONT-2", "Céphalées", "MÉDECINE GÉNÉRALE");

        mockMvc.perform(post("/api/visits/{id}/abandon", visit.getId())
                        .header("Authorization", bearer(billingAgentToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"PARTI\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/visits/{id}/abandon", visit.getId())
                        .header("Authorization", bearer(billingAgentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"LEFT_WITHOUT_BEING_SEEN\",\"note\":\"Parti après 2 h d'attente\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ANNULEE"))
                .andExpect(jsonPath("$.cancellationReason").value("LEFT_WITHOUT_BEING_SEEN"));

        VisitEntity seen = createVisit(patientB, "VIS-CONT-3", "Céphalées", "MÉDECINE GÉNÉRALE");
        mockMvc.perform(post("/api/visits/{id}/take-charge", seen.getId()).header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/visits/{id}/abandon", seen.getId())
                        .header("Authorization", bearer(billingAgentToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"LEFT_WITHOUT_BEING_SEEN\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void aUnitHostingAPatientCannotBeDeactivated() throws Exception {
        UserAccountEntity admin = new UserAccountEntity("admin.hosp@joprelys.local", "Admin clinique", "ADMIN_CLINIQUE", "passhash");
        admin.setOrganizationId(organization.getId());
        TenantContext.setTenantId(organization.getId());
        try {
            admin = userAccountRepository.save(admin);
        } finally {
            TenantContext.clear();
        }
        String adminToken = jwtService.createToken(admin).value();
        VisitEntity visit = createVisit(patientA, "VIS-CONT-4", "Surveillance", "MÉDECINE GÉNÉRALE");
        admit(patientA, visit, PEDIATRICS_SERVICE, PEDIATRICS_SPACE, PEDIATRICS_BED, "Surveillance");

        mockMvc.perform(post("/api/hospital-organization/units/{id}/deactivate",
                        placement(PEDIATRICS_SERVICE, PEDIATRICS_SPACE, PEDIATRICS_BED).unit().getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("1 séjour(s) en cours")));
    }

    @Test
    void deathOfAnInpatientKeepsTheBedUntilTheBodyLeavesAndBlocksNewVisits() throws Exception {
        VisitEntity visit = createVisit(patientA, "VIS-CONT-5", "Détresse respiratoire", "MÉDECINE GÉNÉRALE");
        String stayId = admit(patientA, visit, PEDIATRICS_SERVICE, PEDIATRICS_SPACE, PEDIATRICS_BED, "Réanimation");

        mockMvc.perform(post("/api/patients/{id}/death", patientA.getId())
                        .header("Authorization", bearer(nurseToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/patients/{id}/death", patientA.getId())
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"place\":\"Chambre 101\",\"circumstances\":\"Arrêt cardio-respiratoire\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECEASED"));

        mockMvc.perform(get("/api/hospitalizations/{id}", stayId).header("Authorization", bearer(doctorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EN_COURS"))
                .andExpect(jsonPath("$.dischargeDecidedAt").isNotEmpty());
        mockMvc.perform(post("/api/hospitalizations/{id}/physical-departure", stayId)
                        .header("Authorization", bearer(hospitalizationManagerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmed\":true,\"note\":\"Départ du corps vers la morgue\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECEDE"));
        mockMvc.perform(post("/api/visits")
                        .header("Authorization", bearer(billingAgentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"%s\",\"reason\":\"Test\",\"orientation\":\"CONSULTATION\"}"
                                .formatted(patientA.getId())))
                .andExpect(status().isConflict());
    }
}
