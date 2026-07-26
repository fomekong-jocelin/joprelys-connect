package com.joprelys.backend.ai.ambient.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.ambient.application.AmbientNoteFactualityGuard.GroundedNote;
import com.joprelys.backend.ai.ambient.application.AmbientNoteFactualityGuard.GroundedStatement;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.ambient.domain.AmbientNoteTemplate;
import com.joprelys.backend.ai.ambient.infrastructure.persistence.AmbientNoteRevisionEntity;
import com.joprelys.backend.ai.ambient.infrastructure.persistence.AmbientNoteRevisionRepository;
import com.joprelys.backend.ai.ambient.infrastructure.persistence.AmbientNoteStatementEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class AmbientNotePersistenceServiceTest {

    private final AmbientNoteRevisionRepository repository = mock(AmbientNoteRevisionRepository.class);
    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final AmbientTranscriptLedgerService ledger = mock(AmbientTranscriptLedgerService.class);
    private final AmbientNotePersistenceService service =
            new AmbientNotePersistenceService(repository, visitRepository, ledger);

    @Test
    void shouldRejectAcceptanceWhenTranscriptAdvancedAfterGeneration() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        VisitEntity visit = visit(organizationId);
        AmbientNoteRevisionEntity note = note(organizationId, visitId, userId, 3);
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(repository.findFirstByVisitIdOrderByRevisionNoDesc(visitId)).thenReturn(Optional.of(note));
        when(repository.findByIdAndVisitId(note.getId(), visitId)).thenReturn(Optional.of(note));
        when(ledger.listFinal(visitId, organizationId)).thenReturn(
                new AmbientTranscriptContract.TranscriptLedgerView(
                        visitId,
                        List.of(item(UUID.randomUUID(), 4, "DOCTOR", "Element alpha"))));

        assertThatThrownBy(() -> service.decide(
                visitId, note.getId(), userId, organizationId, "ACCEPT"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_NOTE_STALE");
    }

    @Test
    void shouldRejectAcceptanceWhenEvidenceWasSuperseded() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID originalEvidence = UUID.randomUUID();
        VisitEntity visit = visit(organizationId);
        AmbientNoteRevisionEntity note = note(organizationId, visitId, userId, 3);
        note.addStatement(new AmbientNoteStatementEntity(
                "PLAN", 1, "Element alpha", true, Set.of(originalEvidence)));
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(repository.findFirstByVisitIdOrderByRevisionNoDesc(visitId)).thenReturn(Optional.of(note));
        when(repository.findByIdAndVisitId(note.getId(), visitId)).thenReturn(Optional.of(note));
        when(ledger.listFinal(visitId, organizationId)).thenReturn(
                new AmbientTranscriptContract.TranscriptLedgerView(
                        visitId,
                        List.of(item(UUID.randomUUID(), 3, "DOCTOR", "Element corrected"))));

        assertThatThrownBy(() -> service.decide(
                visitId, note.getId(), userId, organizationId, "ACCEPT"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_NOTE_EVIDENCE_SUPERSEDED");
    }

    @Test
    void shouldRejectDecisionOnOlderRevision() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        VisitEntity visit = visit(organizationId);
        AmbientNoteRevisionEntity latest = note(organizationId, visitId, userId, 5);
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(repository.findFirstByVisitIdOrderByRevisionNoDesc(visitId)).thenReturn(Optional.of(latest));

        assertThatThrownBy(() -> service.decide(
                visitId, UUID.randomUUID(), userId, organizationId, "REJECT"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_NOTE_STALE");
    }

    private AmbientNoteRevisionEntity note(
            UUID organizationId,
            UUID visitId,
            UUID userId,
            long maxSequence) {
        return new AmbientNoteRevisionEntity(
                organizationId,
                visitId,
                1,
                AmbientNoteTemplate.SOAP,
                "fr",
                maxSequence,
                "test-model",
                10,
                userId,
                null);
    }

    private VisitEntity visit(UUID organizationId) {
        VisitEntity visit = mock(VisitEntity.class);
        when(visit.getOrganizationId()).thenReturn(organizationId);
        return visit;
    }

    private TranscriptItemView item(UUID id, long sequence, String speaker, String text) {
        return new TranscriptItemView(
                id,
                sequence,
                "source-" + sequence,
                "AMBIENT_DIARIZED",
                speaker,
                speaker,
                text,
                "fr",
                sequence * 1000,
                sequence * 1000 + 500,
                "FINAL",
                null,
                Instant.now());
    }
}
