package com.joprelys.backend.hospitalization.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Actes médicaux réservés au médecin et traçabilité des administrations (AUDIT-20261104 : CLIN-08, CLIN-09,
 * CLIN-13, FA-37). L'administrateur clinique garde ses permissions techniques mais ne pose aucun acte médical.
 */
class MedicalActAuthorityControllerTest extends HospitalizationControllerTestSupport {

    private static final String DECISION = """
            {"dischargeDiagnosis":"Amélioration clinique","dischargeInstructions":"Contrôle à J7"}
            """;

    @Test
    void clinicalAdministratorCannotTakeTheMedicalDischargeDecision() throws Exception {
        VisitEntity visit = createVisit(patientA, "VIS-AUTH-1", "Dyspnée", "MÉDECINE GÉNÉRALE");
        String stayId = admit(patientA, visit, PEDIATRICS_SERVICE, PEDIATRICS_SPACE, PEDIATRICS_BED, "Surveillance");

        mockMvc.perform(post("/api/hospitalizations/{id}/discharge", stayId)
                        .header("Authorization", bearer(clinicalAdministratorToken()))
                        .contentType(MediaType.APPLICATION_JSON).content(DECISION))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/hospitalizations/{id}/discharge", stayId)
                        .header("Authorization", bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON).content(DECISION))
                .andExpect(status().isOk());
    }

    @Test
    void clinicalAdministratorCannotWriteTheMedicalConsultation() throws Exception {
        VisitEntity visit = createVisit(patientA, "VIS-AUTH-2", "Céphalées", "MÉDECINE GÉNÉRALE");

        mockMvc.perform(post("/api/visits/{id}/consultation", visit.getId())
                        .header("Authorization", bearer(clinicalAdministratorToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptoms\":\"Céphalées\",\"diagnosis\":\"Migraine\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAdministrationCannotBeChartedInTheFuture() throws Exception {
        VisitEntity visit = createVisit(patientA, "VIS-AUTH-3", "Douleur", "MÉDECINE GÉNÉRALE");
        String stayId = admit(patientA, visit, PEDIATRICS_SERVICE, PEDIATRICS_SPACE, PEDIATRICS_BED, "Antalgie");
        UUID itemId = createPrescription(visit, "ACTIVE", null);

        mockMvc.perform(post("/api/hospitalizations/{id}/medication-administrations", stayId)
                        .header("Authorization", bearer(nurseToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(administrationAt(itemId, Instant.now().plus(2, ChronoUnit.HOURS))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void noAdministrationCanBeChartedAfterTheTimeOfDeath() throws Exception {
        VisitEntity visit = createVisit(patientA, "VIS-AUTH-4", "Choc septique", "MÉDECINE GÉNÉRALE");
        String stayId = admit(patientA, visit, PEDIATRICS_SERVICE, PEDIATRICS_SPACE, PEDIATRICS_BED, "Réanimation");
        UUID itemId = createPrescription(visit, "ACTIVE", null);
        Instant deceasedAt = Instant.now().minus(2, ChronoUnit.HOURS);
        jdbcTemplate.update("UPDATE patients SET status = 'DECEASED', deceased_at = ?, death_declared_at = ? WHERE id = ?",
                Timestamp.from(deceasedAt), Timestamp.from(deceasedAt), patientA.getId());

        mockMvc.perform(post("/api/hospitalizations/{id}/medication-administrations", stayId)
                        .header("Authorization", bearer(nurseToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(administrationAt(itemId, Instant.now().minus(1, ChronoUnit.HOURS))))
                .andExpect(status().isConflict());
        // Une administration antérieure au décès peut encore être tracée a posteriori.
        mockMvc.perform(post("/api/hospitalizations/{id}/medication-administrations", stayId)
                        .header("Authorization", bearer(nurseToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(administrationAt(itemId, deceasedAt.minus(30, ChronoUnit.MINUTES))))
                .andExpect(status().isCreated());
    }

    private String administrationAt(UUID itemId, Instant administeredAt) {
        return "{\"medicationName\":\"Paracétamol Injectable\",\"dose\":\"1g IV\",\"prescriptionItemId\":\"%s\",\"administeredAt\":\"%s\"}"
                .formatted(itemId, administeredAt);
    }

    private String clinicalAdministratorToken() {
        TenantContext.setTenantId(organization.getId());
        try {
            UserAccountEntity admin = new UserAccountEntity(
                    "admin.clinique." + UUID.randomUUID() + "@joprelys.local",
                    "Administrateur clinique",
                    "ADMIN_CLINIQUE",
                    "passhash");
            admin.setOrganizationId(organization.getId());
            return jwtService.createToken(userAccountRepository.save(admin)).value();
        } finally {
            TenantContext.clear();
        }
    }
}
