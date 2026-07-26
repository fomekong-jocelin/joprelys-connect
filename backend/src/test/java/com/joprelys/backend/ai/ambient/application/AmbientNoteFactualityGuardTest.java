package com.joprelys.backend.ai.ambient.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.joprelys.backend.ai.ambient.application.AmbientNoteResponseParser.ParsedNote;
import com.joprelys.backend.ai.ambient.application.AmbientNoteResponseParser.ParsedStatement;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.ambient.domain.AmbientNoteTemplate;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class AmbientNoteFactualityGuardTest {

    private final AmbientNoteFactualityGuard guard = new AmbientNoteFactualityGuard();

    @Test
    void shouldAllowPatientEvidenceForSubjectiveStatement() {
        TranscriptItemView item = item("PATIENT", "Element alpha depuis trois jours", 1);
        var grounded = guard.enforce(
                note("SUBJECTIVE", "Element alpha depuis trois jours", item.id()),
                AmbientNoteTemplate.SOAP,
                List.of(item));

        assertThat(grounded.statements()).hasSize(1);
        assertThat(grounded.statements().getFirst().critical()).isFalse();
    }

    @Test
    void shouldRejectStatementSupportedOnlyByUnspecifiedSpeaker() {
        TranscriptItemView item = item("UNSPECIFIED", "Element alpha", 1);

        assertThatThrownBy(() -> guard.enforce(
                note("SUBJECTIVE", "Element alpha", item.id()),
                AmbientNoteTemplate.SOAP,
                List.of(item)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_NOTE_UNGROUNDED");
    }

    @Test
    void shouldRequireDoctorEvidenceForCriticalSection() {
        TranscriptItemView patient = item("PATIENT", "Element gamma", 1);

        assertThatThrownBy(() -> guard.enforce(
                note("ASSESSMENT", "Element gamma", patient.id()),
                AmbientNoteTemplate.SOAP,
                List.of(patient)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_NOTE_CRITICAL_REQUIRES_DOCTOR_EVIDENCE");
    }

    @Test
    void shouldRejectUnknownEvidenceId() {
        TranscriptItemView item = item("DOCTOR", "Element alpha", 1);

        assertThatThrownBy(() -> guard.enforce(
                note("PLAN", "Element alpha", UUID.randomUUID()),
                AmbientNoteTemplate.SOAP,
                List.of(item)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_NOTE_UNGROUNDED");
    }

    @Test
    void shouldRejectUnsupportedNewClinicalToken() {
        TranscriptItemView item = item("DOCTOR", "Element alpha", 1);

        assertThatThrownBy(() -> guard.enforce(
                note("PLAN", "Element alpha beta", item.id()),
                AmbientNoteTemplate.SOAP,
                List.of(item)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_NOTE_UNGROUNDED");
    }

    @Test
    void shouldRejectChangedNumber() {
        TranscriptItemView item = item("DOCTOR", "Mesure 72", 1);

        assertThatThrownBy(() -> guard.enforce(
                note("OBJECTIVE", "Mesure 73", item.id()),
                AmbientNoteTemplate.SOAP,
                List.of(item)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_NOTE_UNGROUNDED");
    }

    @Test
    void shouldRejectNegationReversal() {
        TranscriptItemView item = item("PATIENT", "Sans element beta", 1);

        assertThatThrownBy(() -> guard.enforce(
                note("SUBJECTIVE", "Element beta", item.id()),
                AmbientNoteTemplate.SOAP,
                List.of(item)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_NOTE_UNGROUNDED");
    }

    private ParsedNote note(String section, String text, UUID evidenceId) {
        return new ParsedNote(List.of(new ParsedStatement(section, text, Set.of(evidenceId))));
    }

    private TranscriptItemView item(String speaker, String text, long sequence) {
        return new TranscriptItemView(
                UUID.randomUUID(),
                sequence,
                "source-" + sequence,
                "AMBIENT_DIARIZED",
                speaker,
                speaker,
                text,
                "fr",
                sequence * 1_000,
                sequence * 1_000 + 500,
                "FINAL",
                null,
                Instant.now());
    }
}
