package com.joprelys.backend.ai.facts.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptLedgerView;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptLedgerService;
import com.joprelys.backend.ai.domain.AiChatResponse;
import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactLedgerView;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.OperationType;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RevisionOperationRequest;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionPlannerContract.PlanRevisionRequest;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionPlannerModelContract.PlannerEnvelope;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionPlannerModelContract.PlannerOperation;
import com.joprelys.backend.ai.facts.application.ClinicalNoteProjectionContract.NoteProjectionView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

class ClinicalFactRevisionPlannerServiceTest {

    private final AmbientTranscriptLedgerService transcriptLedger = mock(AmbientTranscriptLedgerService.class);
    private final ClinicalFactLedgerService factLedger = mock(ClinicalFactLedgerService.class);
    private final ClinicalNoteProjectionService projectionService = mock(ClinicalNoteProjectionService.class);
    private final ClinicalFactRevisionPlannerPromptFactory promptFactory = mock(ClinicalFactRevisionPlannerPromptFactory.class);
    private final ClinicalFactRevisionPlanNormalizer normalizer = mock(ClinicalFactRevisionPlanNormalizer.class);
    private final AiProvider aiProvider = mock(AiProvider.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final ClinicalFactRevisionPlannerService service = new ClinicalFactRevisionPlannerService(
            transcriptLedger,
            factLedger,
            projectionService,
            promptFactory,
            normalizer,
            aiProvider,
            objectMapper);

    private UUID visitId;
    private UUID organizationId;
    private TranscriptItemView transcriptItem;

    @BeforeEach
    void setUp() {
        visitId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        transcriptItem = transcript("douleur abdominale", "PATIENT");
    }

    @Test
    void shouldReturnNormalizedProposalWithoutExecutingRevision() throws Exception {
        prepareStableRead("projection-v1", transcriptItem);
        PlannerEnvelope envelope = new PlannerEnvelope(List.of(new PlannerOperation(
                "ADD", null, null, null)));
        when(promptFactory.payload(eq("projection-v1"), any(), any())).thenReturn("input-json");
        when(aiProvider.chatStructured(any(), any(), any(), any()))
                .thenReturn(new AiChatResponse("model-json", 120, "gpt-test"));
        when(objectMapper.readValue("model-json", PlannerEnvelope.class)).thenReturn(envelope);
        RevisionOperationRequest add = new RevisionOperationRequest(
                UUID.randomUUID(), OperationType.ADD, null, null, null);
        when(normalizer.normalize(
                eq(visitId), eq(organizationId), any(UUID.class), any(), any(), eq(envelope)))
                .thenReturn(List.of(add));

        var result = service.plan(
                visitId,
                organizationId,
                new PlanRevisionRequest("projection-v1", List.of(transcriptItem.id())));

        assertThat(result.visitId()).isEqualTo(visitId);
        assertThat(result.baseProjectionVersion()).isEqualTo("projection-v1");
        assertThat(result.model()).isEqualTo("gpt-test");
        assertThat(result.modelOperationCount()).isEqualTo(1);
        assertThat(result.normalizedOperationCount()).isEqualTo(1);
        assertThat(result.applyRequired()).isTrue();
        assertThat(result.proposedRevision().revisionId()).isEqualTo(result.planId());
        assertThat(result.proposedRevision().operations()).containsExactly(add);
        verify(factLedger).listEffective(visitId, organizationId);
    }

    @Test
    void shouldMarkKeepOnlyPlanAsNoApplyRequired() throws Exception {
        prepareStableRead("projection-v1", transcriptItem);
        PlannerEnvelope envelope = new PlannerEnvelope(List.of(new PlannerOperation(
                "KEEP", UUID.randomUUID().toString(), null, null)));
        when(promptFactory.payload(eq("projection-v1"), any(), any())).thenReturn("input-json");
        when(aiProvider.chatStructured(any(), any(), any(), any()))
                .thenReturn(new AiChatResponse("model-json", 90, "gpt-test"));
        when(objectMapper.readValue("model-json", PlannerEnvelope.class)).thenReturn(envelope);
        when(normalizer.normalize(any(), any(), any(), any(), any(), eq(envelope)))
                .thenReturn(List.of(new RevisionOperationRequest(
                        UUID.randomUUID(), OperationType.KEEP, UUID.randomUUID(), null, null)));

        var result = service.plan(
                visitId,
                organizationId,
                new PlanRevisionRequest("projection-v1", List.of(transcriptItem.id())));

        assertThat(result.applyRequired()).isFalse();
    }

    @Test
    void shouldRejectStaleBaseBeforeCallingModel() {
        when(projectionService.project(visitId, organizationId))
                .thenReturn(projection("projection-v2"));

        assertThatThrownBy(() -> service.plan(
                visitId,
                organizationId,
                new PlanRevisionRequest("projection-v1", List.of(transcriptItem.id()))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_REVISION_PLAN_STALE");

        verify(aiProvider, never()).chatStructured(any(), any(), any(), any());
        verify(transcriptLedger, never()).listFinal(visitId, organizationId);
    }

    @Test
    void shouldRejectTranscriptItemOutsideCurrentFinalSnapshot() {
        when(projectionService.project(visitId, organizationId))
                .thenReturn(projection("projection-v1"));
        when(transcriptLedger.listFinal(visitId, organizationId))
                .thenReturn(new TranscriptLedgerView(visitId, List.of()));

        assertThatThrownBy(() -> service.plan(
                visitId,
                organizationId,
                new PlanRevisionRequest("projection-v1", List.of(transcriptItem.id()))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_REVISION_PLAN_TRANSCRIPT_NOT_EFFECTIVE");

        verify(aiProvider, never()).chatStructured(any(), any(), any(), any());
    }

    @Test
    void shouldRejectUnresolvedSpeakerBeforeCallingModel() {
        TranscriptItemView unresolved = transcript("douleur abdominale", "UNSPECIFIED");
        when(projectionService.project(visitId, organizationId))
                .thenReturn(projection("projection-v1"));
        when(transcriptLedger.listFinal(visitId, organizationId))
                .thenReturn(new TranscriptLedgerView(visitId, List.of(unresolved)));

        assertThatThrownBy(() -> service.plan(
                visitId,
                organizationId,
                new PlanRevisionRequest("projection-v1", List.of(unresolved.id()))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_REVISION_PLAN_SPEAKER_UNRESOLVED");

        verify(aiProvider, never()).chatStructured(any(), any(), any(), any());
    }

    @Test
    void shouldFailClosedWhenStructuredOutputIsUnavailable() {
        when(projectionService.project(visitId, organizationId))
                .thenReturn(projection("projection-v1"));
        when(transcriptLedger.listFinal(visitId, organizationId))
                .thenReturn(new TranscriptLedgerView(visitId, List.of(transcriptItem)));
        when(factLedger.listEffective(visitId, organizationId))
                .thenReturn(new FactLedgerView(visitId, List.of()));
        when(promptFactory.payload(any(), any(), any())).thenReturn("input-json");
        when(aiProvider.chatStructured(any(), any(), any(), any()))
                .thenThrow(new UnsupportedOperationException("unsupported"));

        assertThatThrownBy(() -> service.plan(
                visitId,
                organizationId,
                new PlanRevisionRequest("projection-v1", List.of(transcriptItem.id()))))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void shouldRejectMalformedStructuredOutput() throws Exception {
        when(projectionService.project(visitId, organizationId))
                .thenReturn(projection("projection-v1"));
        when(transcriptLedger.listFinal(visitId, organizationId))
                .thenReturn(new TranscriptLedgerView(visitId, List.of(transcriptItem)));
        when(factLedger.listEffective(visitId, organizationId))
                .thenReturn(new FactLedgerView(visitId, List.of()));
        when(promptFactory.payload(any(), any(), any())).thenReturn("input-json");
        when(aiProvider.chatStructured(any(), any(), any(), any()))
                .thenReturn(new AiChatResponse("bad", 20, "gpt-test"));
        when(objectMapper.readValue("bad", PlannerEnvelope.class))
                .thenThrow(new IllegalArgumentException("bad json"));

        assertThatThrownBy(() -> service.plan(
                visitId,
                organizationId,
                new PlanRevisionRequest("projection-v1", List.of(transcriptItem.id()))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_REVISION_PLAN_STRUCTURED_OUTPUT_INVALID");
    }

    @Test
    void shouldRejectPlanWhenProjectionChangesDuringModelCall() throws Exception {
        when(projectionService.project(visitId, organizationId))
                .thenReturn(projection("projection-v1"), projection("projection-v2"));
        when(transcriptLedger.listFinal(visitId, organizationId))
                .thenReturn(new TranscriptLedgerView(visitId, List.of(transcriptItem)));
        when(factLedger.listEffective(visitId, organizationId))
                .thenReturn(new FactLedgerView(visitId, List.of()));
        when(promptFactory.payload(any(), any(), any())).thenReturn("input-json");
        when(aiProvider.chatStructured(any(), any(), any(), any()))
                .thenReturn(new AiChatResponse("model-json", 20, "gpt-test"));
        PlannerEnvelope envelope = new PlannerEnvelope(List.of());
        when(objectMapper.readValue("model-json", PlannerEnvelope.class)).thenReturn(envelope);
        when(normalizer.normalize(any(), any(), any(), any(), any(), eq(envelope)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.plan(
                visitId,
                organizationId,
                new PlanRevisionRequest("projection-v1", List.of(transcriptItem.id()))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_REVISION_PLAN_STALE");
    }

    @Test
    void shouldRejectPlanWhenRequestedTranscriptChangesDuringModelCall() throws Exception {
        TranscriptItemView corrected = transcriptItemWithSameId(
                transcriptItem,
                "correction douleur thoracique");
        when(projectionService.project(visitId, organizationId))
                .thenReturn(projection("projection-v1"), projection("projection-v1"));
        when(transcriptLedger.listFinal(visitId, organizationId))
                .thenReturn(
                        new TranscriptLedgerView(visitId, List.of(transcriptItem)),
                        new TranscriptLedgerView(visitId, List.of(corrected)));
        when(factLedger.listEffective(visitId, organizationId))
                .thenReturn(new FactLedgerView(visitId, List.of()));
        when(promptFactory.payload(any(), any(), any())).thenReturn("input-json");
        when(aiProvider.chatStructured(any(), any(), any(), any()))
                .thenReturn(new AiChatResponse("model-json", 20, "gpt-test"));
        PlannerEnvelope envelope = new PlannerEnvelope(List.of());
        when(objectMapper.readValue("model-json", PlannerEnvelope.class)).thenReturn(envelope);
        when(normalizer.normalize(any(), any(), any(), any(), any(), eq(envelope)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.plan(
                visitId,
                organizationId,
                new PlanRevisionRequest("projection-v1", List.of(transcriptItem.id()))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_REVISION_PLAN_TRANSCRIPT_STALE");
    }

    private void prepareStableRead(String version, TranscriptItemView item) {
        when(projectionService.project(visitId, organizationId))
                .thenReturn(projection(version), projection(version));
        when(transcriptLedger.listFinal(visitId, organizationId))
                .thenReturn(new TranscriptLedgerView(visitId, List.of(item)));
        when(factLedger.listEffective(visitId, organizationId))
                .thenReturn(new FactLedgerView(visitId, List.of()));
    }

    private NoteProjectionView projection(String version) {
        return new NoteProjectionView(visitId, version, 0, List.of());
    }

    private TranscriptItemView transcript(String text, String speakerType) {
        return new TranscriptItemView(
                UUID.randomUUID(),
                7,
                "planner-source",
                "AMBIENT_DIARIZED",
                speakerType,
                speakerType.toLowerCase(),
                text,
                "fr",
                5_000,
                8_000,
                "FINAL",
                null,
                Instant.now());
    }

    private TranscriptItemView transcriptItemWithSameId(TranscriptItemView source, String text) {
        return new TranscriptItemView(
                source.id(),
                source.sequence(),
                source.sourceEventId(),
                source.source(),
                source.speakerType(),
                source.speakerLabel(),
                text,
                source.locale(),
                source.startOffsetMs(),
                source.endOffsetMs(),
                source.status(),
                source.supersedesItemId(),
                Instant.now());
    }
}
