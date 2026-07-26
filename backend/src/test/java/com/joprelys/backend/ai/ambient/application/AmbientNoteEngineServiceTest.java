package com.joprelys.backend.ai.ambient.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.ambient.application.AmbientNoteContract.NoteRevisionView;
import com.joprelys.backend.ai.ambient.application.AmbientNoteContract.NoteStatementView;
import com.joprelys.backend.ai.ambient.application.AmbientNoteFactualityGuard.GroundedNote;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.ambient.domain.AmbientNoteTemplate;
import com.joprelys.backend.ai.domain.AiChatResponse;
import com.joprelys.backend.ai.domain.AiMessage;
import com.joprelys.backend.ai.domain.AiProvider;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

class AmbientNoteEngineServiceTest {

    private final AiProvider provider = mock(AiProvider.class);
    private final AmbientTranscriptLedgerService ledger = mock(AmbientTranscriptLedgerService.class);
    private final AmbientNotePersistenceService persistence = mock(AmbientNotePersistenceService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AmbientNoteEngineService service = new AmbientNoteEngineService(
            provider,
            ledger,
            persistence,
            new AmbientNoteResponseParser(objectMapper),
            new AmbientNoteFactualityGuard(),
            objectMapper);

    @Test
    void shouldGenerateFromEffectiveTranscriptInAudioChronology() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        TranscriptItemView lateArrivalEarlyAudio = item("PATIENT", "Element premier", 8, 1_000);
        TranscriptItemView earlyArrivalLateAudio = item("DOCTOR", "Element second", 2, 8_000);
        when(ledger.listFinal(visitId, organizationId)).thenReturn(
                new AmbientTranscriptContract.TranscriptLedgerView(
                        visitId, List.of(earlyArrivalLateAudio, lateArrivalEarlyAudio)));
        when(persistence.latest(visitId, organizationId)).thenReturn(Optional.empty());
        when(provider.chat(anyList(), anyString())).thenReturn(new AiChatResponse(
                jsonStatement("SUBJECTIVE", "Element premier", lateArrivalEarlyAudio.id()),
                30,
                "test-model"));
        NoteRevisionView persisted = noteView(visitId, 1, 8, "GENERATED", List.of());
        when(persistence.persistGenerated(
                eq(visitId), eq(userId), eq(organizationId), eq(AmbientNoteTemplate.SOAP), eq("fr"),
                eq(8L), eq("test-model"), eq(30), any(GroundedNote.class)))
                .thenReturn(persisted);

        NoteRevisionView result = service.generate(visitId, userId, organizationId, "SOAP", "fr");

        assertThat(result).isEqualTo(persisted);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AiMessage>> messages = ArgumentCaptor.forClass(List.class);
        verify(provider).chat(messages.capture(), anyString());
        String payload = messages.getValue().getFirst().content();
        assertThat(payload.indexOf("Element premier")).isLessThan(payload.indexOf("Element second"));
    }

    @Test
    void shouldReuseLatestGroundedNoteWhenTranscriptDidNotChange() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        TranscriptItemView source = item("PATIENT", "Element alpha", 4, 1_000);
        NoteRevisionView latest = noteView(
                visitId,
                2,
                4,
                "ACCEPTED",
                List.of(new NoteStatementView(
                        UUID.randomUUID(), "SUBJECTIVE", 1, "Element alpha", false, List.of(source.id()))));
        when(ledger.listFinal(visitId, organizationId)).thenReturn(
                new AmbientTranscriptContract.TranscriptLedgerView(visitId, List.of(source)));
        when(persistence.latest(visitId, organizationId)).thenReturn(Optional.of(latest));

        NoteRevisionView result = service.generate(
                visitId, UUID.randomUUID(), organizationId, "SOAP", "fr");

        assertThat(result).isSameAs(latest);
        verify(provider, never()).chat(anyList(), anyString());
    }

    @Test
    void shouldIncrementFromLatestGroundedNoteUsingOnlyNewTranscriptItems() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        TranscriptItemView oldItem = item("PATIENT", "Element alpha", 1, 1_000);
        TranscriptItemView newItem = item("DOCTOR", "Element beta", 2, 2_000);
        NoteRevisionView latest = noteView(
                visitId,
                1,
                1,
                "ACCEPTED",
                List.of(new NoteStatementView(
                        UUID.randomUUID(), "SUBJECTIVE", 1, "Element alpha", false, List.of(oldItem.id()))));
        when(ledger.listFinal(visitId, organizationId)).thenReturn(
                new AmbientTranscriptContract.TranscriptLedgerView(visitId, List.of(oldItem, newItem)));
        when(persistence.latest(visitId, organizationId)).thenReturn(Optional.of(latest));
        when(provider.chat(anyList(), anyString())).thenReturn(new AiChatResponse(
                """
                {"statements":[
                  {"section":"SUBJECTIVE","text":"Element alpha","evidenceItemIds":["%s"]},
                  {"section":"PLAN","text":"Element beta","evidenceItemIds":["%s"]}
                ]}
                """.formatted(oldItem.id(), newItem.id()),
                22,
                "test-model"));
        when(persistence.persistGenerated(any(), any(), any(), any(), anyString(),
                eq(2L), anyString(), eq(22), any(GroundedNote.class)))
                .thenReturn(noteView(visitId, 2, 2, "GENERATED", List.of()));

        service.generate(visitId, userId, organizationId, "SOAP", "fr");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AiMessage>> messages = ArgumentCaptor.forClass(List.class);
        verify(provider).chat(messages.capture(), anyString());
        String payload = messages.getValue().getFirst().content();
        assertThat(payload).contains("\"currentNote\"").contains("Element alpha");
        assertThat(payload).contains("Element beta");
        assertThat(payload).doesNotContain("\"newTranscriptItems\":[{\"id\":\"" + oldItem.id());
    }

    @Test
    void shouldRegenerateFromEffectiveLedgerWhenPriorEvidenceWasSuperseded() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID removedEvidence = UUID.randomUUID();
        TranscriptItemView replacement = item("PATIENT", "Element corrected", 5, 500);
        NoteRevisionView latest = noteView(
                visitId,
                3,
                3,
                "ACCEPTED",
                List.of(new NoteStatementView(
                        UUID.randomUUID(), "SUBJECTIVE", 1, "Element old", false, List.of(removedEvidence))));
        when(ledger.listFinal(visitId, organizationId)).thenReturn(
                new AmbientTranscriptContract.TranscriptLedgerView(visitId, List.of(replacement)));
        when(persistence.latest(visitId, organizationId)).thenReturn(Optional.of(latest));
        when(provider.chat(anyList(), anyString())).thenReturn(new AiChatResponse(
                jsonStatement("SUBJECTIVE", "Element corrected", replacement.id()),
                18,
                "test-model"));
        when(persistence.persistGenerated(any(), any(), any(), any(), anyString(),
                eq(5L), anyString(), eq(18), any(GroundedNote.class)))
                .thenReturn(noteView(visitId, 4, 5, "GENERATED", List.of()));

        service.generate(visitId, userId, organizationId, "SOAP", "fr");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AiMessage>> messages = ArgumentCaptor.forClass(List.class);
        verify(provider).chat(messages.capture(), anyString());
        assertThat(messages.getValue().getFirst().content()).contains("\"currentNote\":[]");
        assertThat(messages.getValue().getFirst().content()).contains("Element corrected");
        assertThat(messages.getValue().getFirst().content()).doesNotContain("Element old");
    }

    @Test
    void shouldProcessLongTranscriptInBoundedRollingBatches() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        List<TranscriptItemView> items = new ArrayList<>();
        for (int index = 1; index <= 41; index++) {
            items.add(item(index == 41 ? "DOCTOR" : "PATIENT", "Element item" + index, index, index * 1_000L));
        }
        TranscriptItemView first = items.getFirst();
        TranscriptItemView last = items.getLast();
        when(ledger.listFinal(visitId, organizationId)).thenReturn(
                new AmbientTranscriptContract.TranscriptLedgerView(visitId, items));
        when(persistence.latest(visitId, organizationId)).thenReturn(Optional.empty());
        when(provider.chat(anyList(), anyString()))
                .thenReturn(new AiChatResponse(
                        jsonStatement("SUBJECTIVE", first.text(), first.id()), 10, "model-1"))
                .thenReturn(new AiChatResponse(
                        """
                        {"statements":[
                          {"section":"SUBJECTIVE","text":"%s","evidenceItemIds":["%s"]},
                          {"section":"PLAN","text":"%s","evidenceItemIds":["%s"]}
                        ]}
                        """.formatted(first.text(), first.id(), last.text(), last.id()),
                        11,
                        "model-1"));
        when(persistence.persistGenerated(any(), any(), any(), any(), anyString(),
                eq(41L), eq("model-1"), eq(21), any(GroundedNote.class)))
                .thenReturn(noteView(visitId, 1, 41, "GENERATED", List.of()));

        service.generate(visitId, userId, organizationId, "SOAP", "fr");

        verify(provider, org.mockito.Mockito.times(2)).chat(anyList(), anyString());
    }

    private String jsonStatement(String section, String text, UUID evidenceId) {
        return """
                {"statements":[{"section":"%s","text":"%s","evidenceItemIds":["%s"]}]}
                """.formatted(section, text, evidenceId);
    }

    private TranscriptItemView item(String speaker, String text, long sequence, long startMs) {
        return new TranscriptItemView(
                UUID.randomUUID(),
                sequence,
                "source-" + sequence,
                "AMBIENT_DIARIZED",
                speaker,
                speaker,
                text,
                "fr",
                startMs,
                startMs + 500,
                "FINAL",
                null,
                Instant.now());
    }

    private NoteRevisionView noteView(
            UUID visitId,
            long revision,
            long maxSequence,
            String status,
            List<NoteStatementView> statements) {
        return new NoteRevisionView(
                UUID.randomUUID(),
                visitId,
                revision,
                "SOAP",
                "fr",
                status,
                maxSequence,
                "test-model",
                10,
                null,
                Instant.now(),
                null,
                null,
                statements);
    }
}
