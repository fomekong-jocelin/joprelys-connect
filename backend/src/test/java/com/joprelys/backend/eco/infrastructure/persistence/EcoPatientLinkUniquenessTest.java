package com.joprelys.backend.eco.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.eco.domain.LinkRelationship;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * S8 (revue sécurité) : la base garantit une seule liaison active par profil d'application (V125), même si deux
 * créations concurrentes passent la vérification applicative. Données fictives uniquement.
 */
@SpringBootTest
@ActiveProfiles("test")
class EcoPatientLinkUniquenessTest {

    @Autowired private EcoPatientLinkRepository linkRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    /** Base H2 partagée entre classes de test : ne rien laisser derrière soi. */
    @AfterEach
    void removeEcoRows() {
        TenantContext.clear();
        jdbcTemplate.update("DELETE FROM eco_outbox");
        jdbcTemplate.update("DELETE FROM eco_consents");
        jdbcTemplate.update("DELETE FROM eco_patient_links");
    }

    @Test
    void aSecondActiveLinkForTheSameProfileIsRefusedByTheDatabase() {
        UUID profile = UUID.randomUUID();
        linkRepository.saveAndFlush(link(savePatient(), profile));
        PatientEntity other = savePatient();

        assertThatThrownBy(() -> linkRepository.saveAndFlush(link(other, profile)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theProfileCanBeLinkedAgainOnceTheActiveLinkIsRevoked() {
        UUID profile = UUID.randomUUID();
        EcoPatientLinkEntity first = linkRepository.saveAndFlush(link(savePatient(), profile));
        first.revoke(Instant.now());
        linkRepository.saveAndFlush(first);

        PatientEntity other = savePatient();
        assertThatCode(() -> linkRepository.saveAndFlush(link(other, profile))).doesNotThrowAnyException();
    }

    private static EcoPatientLinkEntity link(PatientEntity patient, UUID profile) {
        return new EcoPatientLinkEntity(patient.getId(), patient.getGlobalPatientNumber(), "joprelys:clinique-demo",
                "allopharma", profile, LinkRelationship.SELF, "sub-" + patient.getGlobalPatientNumber(),
                "allopharma-mobile", Instant.now().truncatedTo(ChronoUnit.SECONDS));
    }

    private PatientEntity savePatient() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        OrganizationEntity organization = organizationRepository.saveAndFlush(new OrganizationEntity(
                "Clinique Demo " + suffix, "demo-" + suffix.toLowerCase() + "@test.local", "600000000", "Littoral",
                "Douala"));
        TenantContext.setTenantId(organization.getId());
        try {
            return patientRepository.saveAndFlush(new PatientEntity("DPU-TEST-" + suffix, "PAT-" + suffix,
                    "Patient Fictif", "FEMININ", LocalDate.of(1990, 1, 1), null, "Douala", null, null, null, null,
                    null, null));
        } finally {
            TenantContext.clear();
        }
    }
}
