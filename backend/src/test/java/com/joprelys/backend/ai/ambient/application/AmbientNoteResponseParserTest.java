package com.joprelys.backend.ai.ambient.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.joprelys.backend.ai.ambient.domain.AmbientNoteTemplate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

class AmbientNoteResponseParserTest {

    private final AmbientNoteResponseParser parser = new AmbientNoteResponseParser(new ObjectMapper());

    @Test
    void shouldParseOnlyAllowedSourcedStatements() {
        UUID evidenceId = UUID.randomUUID();
        var parsed = parser.parse("""
                {
                  "statements": [{
                    "section": "SUBJECTIVE",
                    "text": "Element alpha depuis trois jours",
                    "evidenceItemIds": ["%s"]
                  }]
                }
                """.formatted(evidenceId), AmbientNoteTemplate.SOAP);

        assertThat(parsed.statements()).hasSize(1);
        assertThat(parsed.statements().getFirst().section()).isEqualTo("SUBJECTIVE");
        assertThat(parsed.statements().getFirst().evidenceItemIds()).containsExactly(evidenceId);
    }

    @Test
    void shouldRejectUnknownRootOrStatementFields() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> parser.parse("""
                {"statements": [], "summary": "extra"}
                """, AmbientNoteTemplate.SOAP))
                .isInstanceOf(ResponseStatusException.class);

        assertThatThrownBy(() -> parser.parse("""
                {"statements": [{
                  "section":"SUBJECTIVE",
                  "text":"Element alpha",
                  "evidenceItemIds":["%s"],
                  "confidence":0.99
                }]}
                """.formatted(id), AmbientNoteTemplate.SOAP))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldRejectMarkdownInvalidSectionOrMissingEvidence() {
        assertThatThrownBy(() -> parser.parse("```json\n{\"statements\":[]}\n```", AmbientNoteTemplate.SOAP))
                .isInstanceOf(ResponseStatusException.class);

        assertThatThrownBy(() -> parser.parse("""
                {"statements": [{"section":"MEDICATIONS","text":"Element beta","evidenceItemIds":[]}]}
                """, AmbientNoteTemplate.SOAP))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldRejectDuplicateEvidenceIds() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> parser.parse("""
                {"statements": [{
                  "section":"SUBJECTIVE",
                  "text":"Element alpha",
                  "evidenceItemIds":["%s","%s"]
                }]}
                """.formatted(id, id), AmbientNoteTemplate.SOAP))
                .isInstanceOf(ResponseStatusException.class);
    }
}
