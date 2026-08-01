package com.joprelys.backend.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class AiLiveTranscriptBufferServiceTest {

    private final UUID visitId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID organizationId = UUID.randomUUID();
    private AiLiveTranscriptBufferService service;

    @BeforeEach
    void setUp() {
        service = new AiLiveTranscriptBufferService(180);
    }

    @Test
    void storesTheLatestMonotonicTranscript() {
        service.upsert(
                visitId,
                userId,
                organizationId,
                "Bonjour docteur.",
                1,
                "event-1");

        var updated = service.upsert(
                visitId,
                userId,
                organizationId,
                "Bonjour docteur. J'ai de la fièvre.",
                2,
                "event-2");

        assertThat(updated.sequence()).isEqualTo(2);
        assertThat(updated.transcript())
                .isEqualTo("Bonjour docteur. J'ai de la fièvre.");
        assertThat(service.get(visitId, userId, organizationId))
                .contains(updated);
    }

    @Test
    void acceptsAnIdempotentRetryAndIgnoresAnOlderSequence() {
        var first = service.upsert(
                visitId,
                userId,
                organizationId,
                "Toux sèche.",
                4,
                "event-4");

        var retry = service.upsert(
                visitId,
                userId,
                organizationId,
                "Toux sèche.",
                4,
                "event-4");
        var stale = service.upsert(
                visitId,
                userId,
                organizationId,
                "Ancienne version.",
                3,
                "event-3");

        assertThat(retry).isEqualTo(first);
        assertThat(stale).isEqualTo(first);
    }

    @Test
    void refusesAContradictoryOrRewrittenCapture() {
        service.upsert(
                visitId,
                userId,
                organizationId,
                "Pas de douleur thoracique.",
                1,
                "event-1");

        assertThatThrownBy(() -> service.upsert(
                visitId,
                userId,
                organizationId,
                "Douleur thoracique.",
                2,
                "event-2"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_REALTIME_TRANSCRIPT_STALE");
    }

    @Test
    void clearRemovesTheRecoverableTranscript() {
        service.upsert(
                visitId,
                userId,
                organizationId,
                "Température trente-huit degrés.",
                1,
                "event-1");

        service.clear(visitId, userId, organizationId);

        assertThat(service.get(visitId, userId, organizationId)).isEmpty();
    }
}
