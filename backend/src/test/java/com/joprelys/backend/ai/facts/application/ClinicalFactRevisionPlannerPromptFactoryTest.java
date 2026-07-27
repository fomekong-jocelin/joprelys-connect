package com.joprelys.backend.ai.facts.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactView;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Authority;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactStatus;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactType;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Laterality;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Polarity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

class ClinicalFactRevisionPlannerPromptFactoryTest {

    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final ClinicalFactRevisionPlannerPromptFactory factory =
            new ClinicalFactRevisionPlannerPromptFactory(objectMapper);

    @Test
    @SuppressWarnings("unchecked")
    void shouldSerializeOnlyStructuredFactsAndRequestedTranscriptItems() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("payload-json");
        FactView fact = fact();
        TranscriptItemView item = transcript("douleur abdominale", "PATIENT");

        String result = factory.payload("projection-v1", List.of(fact), List.of(item));

        assertThat(result).isEqualTo("payload-json");
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(objectMapper).writeValueAsString(payload.capture());
        Map<String, Object> root = (Map<String, Object>) payload.getValue();
        assertThat(root.get("baseProjectionVersion")).isEqualTo("projection-v1");
        assertThat((List<?>) root.get("effectiveFacts")).hasSize(1);
        assertThat((List<?>) root.get("transcriptItems")).hasSize(1);
    }

    @Test
    void shouldFailClosedWhenEffectiveFactContextExceedsBound() {
        List<FactView> facts = new ArrayList<>();
        for (int index = 0; index < 257; index++) facts.add(fact());

        assertThatThrownBy(() -> factory.payload(
                "projection-v1", facts, List.of(transcript("x", "PATIENT"))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_REVISION_PLAN_CONTEXT_TOO_LARGE");
    }

    @Test
    void shouldFailClosedWhenRequestedTranscriptPayloadIsTooLarge() {
        String large = "x".repeat(32_001);

        assertThatThrownBy(() -> factory.payload(
                "projection-v1", List.of(), List.of(transcript(large, "PATIENT"))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_REVISION_PLAN_TRANSCRIPT_TOO_LARGE");
    }

    private FactView fact() {
        return new FactView(
                UUID.randomUUID(),
                1,
                "fact-source",
                FactType.SYMPTOM.name(),
                Authority.PATIENT_REPORTED.name(),
                "ABDOMINAL_PAIN",
                "douleur abdominale",
                Polarity.POSITIVE.name(),
                null,
                null,
                null,
                null,
                Laterality.UNSPECIFIED.name(),
                null,
                null,
                FactStatus.ASSERTED.name(),
                null,
                Instant.now(),
                List.of());
    }

    private TranscriptItemView transcript(String text, String speaker) {
        return new TranscriptItemView(
                UUID.randomUUID(),
                1,
                "planner-prompt",
                "AMBIENT_DIARIZED",
                speaker,
                speaker.toLowerCase(),
                text,
                "fr",
                0,
                1_000,
                "FINAL",
                null,
                Instant.now());
    }
}
