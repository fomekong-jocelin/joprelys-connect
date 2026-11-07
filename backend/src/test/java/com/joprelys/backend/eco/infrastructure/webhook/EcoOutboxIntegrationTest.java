package com.joprelys.backend.eco.infrastructure.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.JsonPath;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.eco.EcoContractSchemas;
import com.joprelys.backend.eco.application.EcoBusinessException;
import com.joprelys.backend.eco.application.PatientConsentRevocationUseCase;
import com.joprelys.backend.eco.application.event.ConsentRevokedEvent;
import com.joprelys.backend.eco.application.event.EcoEventPublisher;
import com.joprelys.backend.eco.config.EcoProperties;
import com.joprelys.backend.eco.domain.ConsentScope;
import com.joprelys.backend.eco.domain.EcoStatus;
import com.joprelys.backend.eco.domain.GranteeType;
import com.joprelys.backend.eco.domain.LinkRelationship;
import com.joprelys.backend.eco.domain.RevokedBy;
import com.joprelys.backend.eco.infrastructure.persistence.EcoConsentEntity;
import com.joprelys.backend.eco.infrastructure.persistence.EcoConsentRepository;
import com.joprelys.backend.eco.infrastructure.persistence.EcoOutboxEntity;
import com.joprelys.backend.eco.infrastructure.persistence.EcoOutboxRepository;
import com.joprelys.backend.eco.infrastructure.persistence.EcoPatientLinkEntity;
import com.joprelys.backend.eco.infrastructure.persistence.EcoPatientLinkRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * Outbox transactionnelle et relais (S4) : révocation depuis le portail patient → {@code consent.revoked}
 * conforme aux contrats, atomicité avec la révocation, rejeu puis file d'erreurs. Données fictives uniquement.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EcoOutboxIntegrationTest {

    @Autowired private PatientConsentRevocationUseCase portalRevocation;
    @Autowired private EcoEventPublisher publisher;
    @Autowired private EcoOutboxRepository outboxRepository;
    @Autowired private EcoPatientLinkRepository linkRepository;
    @Autowired private EcoConsentRepository consentRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private EcoProperties properties;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private ObjectMapper objectMapper;

    private UUID patientId;
    private EcoPatientLinkEntity link;
    private EcoConsentEntity consent;

    @BeforeEach
    void createFictitiousLink() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        patientId = savePatient("DPU-TEST-" + suffix, suffix).getId();
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        link = linkRepository.saveAndFlush(new EcoPatientLinkEntity(patientId, "DPU-TEST-" + suffix,
                "joprelys:clinique-demo", "allopharma", UUID.randomUUID(), LinkRelationship.SELF,
                "sub-" + suffix, "allopharma-mobile", now));
        consent = consentRepository.saveAndFlush(new EcoConsentEntity(link.getId(),
                Set.of(ConsentScope.PRESCRIPTIONS), GranteeType.APPLICATION, "allopharma", now,
                now.plus(Duration.ofDays(365))));
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    // --- Révocation portail → outbox ---

    @Test
    void portalRevocationQueuesAContractCompliantConsentRevokedEvent() {
        portalRevocation.revokeByPatient(patientId, link.getId());

        EcoOutboxEntity entry = single(rowsForConsent());
        assertThat(entry.getEventType()).isEqualTo("consent.revoked");
        assertThat(entry.getSubscriber()).isEqualTo("allopharma");
        assertThat(entry.getStatus()).isEqualTo(EcoOutboxEntity.Status.PENDING);
        String payload = entry.getPayload();
        assertThat(EcoContractSchemas.validate("event-envelope.schema.json", payload)).isEmpty();
        String data = objectMapper.readTree(payload).get("data").toString();
        assertThat(EcoContractSchemas.validate("consent-revoked.data.schema.json", data)).isEmpty();
        assertThat(JsonPath.<String>read(payload, "$.id")).isEqualTo(entry.getEventId().toString());
        assertThat(JsonPath.<String>read(payload, "$.source")).isEqualTo("joprelys:clinique-demo");
        assertThat(JsonPath.<String>read(payload, "$.subject")).isEqualTo("Consent/" + consent.getId());
        assertThat(JsonPath.<String>read(payload, "$.data.patientLinkId")).isEqualTo(link.getId().toString());
        assertThat(JsonPath.<String>read(payload, "$.data.revokedBy")).isEqualTo("patient");
        assertThat(payload).doesNotContain(link.getDpu()).doesNotContain("reason");

        assertThat(consentRepository.findById(consent.getId()).orElseThrow().getStatus()).isEqualTo(EcoStatus.REVOKED);
        assertThat(linkRepository.findById(link.getId()).orElseThrow().getStatus()).isEqualTo(EcoStatus.REVOKED);
    }

    @Test
    void secondPortalRevocationIsIdempotentAndQueuesNothingMore() {
        portalRevocation.revokeByPatient(patientId, link.getId());
        portalRevocation.revokeByPatient(patientId, link.getId());

        assertThat(rowsForConsent()).hasSize(1);
    }

    @Test
    void anotherPatientsLinkAnswersNotFoundAndQueuesNothing() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        UUID otherPatient = savePatient("DPU-TEST-" + suffix, suffix).getId();

        assertThatThrownBy(() -> portalRevocation.revokeByPatient(otherPatient, link.getId()))
                .isInstanceOf(EcoBusinessException.class)
                .extracting(e -> ((EcoBusinessException) e).error())
                .isEqualTo(EcoBusinessException.EcoError.PATIENT_LINK_NOT_FOUND);
        assertThat(rowsForConsent()).isEmpty();
        assertThat(consentRepository.findById(consent.getId()).orElseThrow().getStatus()).isEqualTo(EcoStatus.ACTIVE);
    }

    @Test
    void rolledBackRevocationLeavesNoOutboxRow() {
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            portalRevocation.revokeByPatient(patientId, link.getId());
            throw new IllegalStateException("échec simulé après la révocation");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(rowsForConsent()).isEmpty();
        assertThat(consentRepository.findById(consent.getId()).orElseThrow().getStatus()).isEqualTo(EcoStatus.ACTIVE);
    }

    @Test
    void publisherRefusesToRunOutsideABusinessTransaction() {
        ConsentRevokedEvent event = new ConsentRevokedEvent("allopharma", consent.getId(), link.getId(),
                Instant.now(), RevokedBy.PATIENT);

        assertThatThrownBy(() -> publisher.publishConsentRevoked(event))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void applicationWithoutSubscriberQueuesNothing() {
        ConsentRevokedEvent event = new ConsentRevokedEvent("application-sans-abonne", consent.getId(),
                link.getId(), Instant.now(), RevokedBy.PATIENT);

        transactionTemplate.executeWithoutResult(status -> publisher.publishConsentRevoked(event));

        assertThat(rowsForConsent()).isEmpty();
    }

    // --- Relais ---

    @Test
    void relayMarksSuccessfulDeliveryAsDelivered() {
        EcoOutboxEntity entry = queued(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        Instant now = entry.getCreatedAt().plusSeconds(1);

        relay((subscriber, eventId, body) -> EcoWebhookSender.DeliveryResult.ok()).relayDue(now);

        EcoOutboxEntity reloaded = outboxRepository.findById(entry.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(EcoOutboxEntity.Status.DELIVERED);
        assertThat(reloaded.getDeliveredAt()).isEqualTo(now);
        assertThat(reloaded.getAttempts()).isEqualTo(1);
    }

    @Test
    void relayReschedulesServerErrorWithBackoff() {
        EcoOutboxEntity entry = queued(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        Instant now = entry.getCreatedAt().plusSeconds(1);

        relay((subscriber, eventId, body) -> EcoWebhookSender.DeliveryResult.failed("HTTP 503")).relayDue(now);

        EcoOutboxEntity reloaded = outboxRepository.findById(entry.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(EcoOutboxEntity.Status.PENDING);
        assertThat(reloaded.getNextAttemptAt()).isEqualTo(now.plusSeconds(30));
        assertThat(reloaded.getLastError()).isEqualTo("HTTP 503");
        assertThat(reloaded.getAttempts()).isEqualTo(1);
    }

    @Test
    void relayMovesEventOlderThanTwentyFourHoursToDead() {
        EcoOutboxEntity entry = queued(Instant.now().minus(Duration.ofHours(24)).truncatedTo(ChronoUnit.SECONDS));

        relay((subscriber, eventId, body) -> EcoWebhookSender.DeliveryResult.failed("HTTP 503"))
                .relayDue(Instant.now());

        EcoOutboxEntity reloaded = outboxRepository.findById(entry.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(EcoOutboxEntity.Status.DEAD);
        assertThat(reloaded.getLastError()).isEqualTo("HTTP 503");
    }

    @Test
    void relaySendsTheStoredBodyWithTheEventIdAsIdempotencyKey() {
        EcoOutboxEntity entry = queued(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        List<UUID> sentIds = new ArrayList<>();
        List<String> sentBodies = new ArrayList<>();

        relay((subscriber, eventId, body) -> {
            sentIds.add(eventId);
            sentBodies.add(body);
            return EcoWebhookSender.DeliveryResult.ok();
        }).relayDue(entry.getCreatedAt());

        assertThat(sentIds).contains(entry.getEventId());
        assertThat(sentBodies).contains(entry.getPayload());
    }

    private EcoOutboxRelay relay(EcoWebhookSender sender) {
        return new EcoOutboxRelay(outboxRepository, sender, properties);
    }

    private EcoOutboxEntity queued(Instant createdAt) {
        UUID eventId = UUID.randomUUID();
        return outboxRepository.saveAndFlush(new EcoOutboxEntity(eventId, "consent.revoked", "allopharma",
                "{\"id\":\"" + eventId + "\",\"subject\":\"Consent/" + consent.getId() + "\"}", createdAt));
    }

    private List<EcoOutboxEntity> rowsForConsent() {
        return outboxRepository.findAll().stream()
                .filter(row -> row.getPayload().contains(consent.getId().toString()))
                .toList();
    }

    private static EcoOutboxEntity single(List<EcoOutboxEntity> rows) {
        assertThat(rows).hasSize(1);
        return rows.getFirst();
    }

    private PatientEntity savePatient(String dpu, String suffix) {
        OrganizationEntity organization = organizationRepository.saveAndFlush(new OrganizationEntity(
                "Clinique Demo " + suffix, "demo-" + suffix.toLowerCase() + "@test.local", "600000000", "Littoral",
                "Douala"));
        TenantContext.setTenantId(organization.getId());
        try {
            return patientRepository.saveAndFlush(new PatientEntity(dpu, "PAT-" + suffix, "Patient Fictif",
                    "FEMININ", LocalDate.of(1990, 1, 1), null, "Douala", null, null, null, null, null, null));
        } finally {
            TenantContext.clear();
        }
    }
}
