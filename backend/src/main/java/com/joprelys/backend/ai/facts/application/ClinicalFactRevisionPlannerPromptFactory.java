package com.joprelys.backend.ai.facts.application;

import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactView;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
class ClinicalFactRevisionPlannerPromptFactory {

    private static final int MAX_EFFECTIVE_FACTS = 256;
    private static final int MAX_TRANSCRIPT_TEXT_CHARS = 32_000;

    private final ObjectMapper objectMapper;

    ClinicalFactRevisionPlannerPromptFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    String payload(
            String baseProjectionVersion,
            List<FactView> effectiveFacts,
            List<TranscriptItemView> transcriptItems) {
        if (effectiveFacts.size() > MAX_EFFECTIVE_FACTS) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                    "AI_CLINICAL_FACT_REVISION_PLAN_CONTEXT_TOO_LARGE");
        }
        int transcriptChars = transcriptItems.stream()
                .map(TranscriptItemView::text)
                .mapToInt(text -> text == null ? 0 : text.length())
                .sum();
        if (transcriptChars > MAX_TRANSCRIPT_TEXT_CHARS) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                    "AI_CLINICAL_FACT_REVISION_PLAN_TRANSCRIPT_TOO_LARGE");
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("baseProjectionVersion", baseProjectionVersion);
        root.put("effectiveFacts", effectiveFacts.stream().map(this::fact).toList());
        root.put("transcriptItems", transcriptItems.stream().map(this::transcriptItem).toList());
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception exception) {
            throw new IllegalStateException("AI_CLINICAL_FACT_REVISION_PLAN_INPUT_SERIALIZATION_FAILED", exception);
        }
    }

    private Map<String, Object> fact(FactView fact) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", fact.id().toString());
        value.put("factType", fact.factType());
        value.put("authority", fact.authority());
        value.put("conceptCode", fact.conceptCode());
        value.put("conceptText", fact.conceptText());
        value.put("polarity", fact.polarity());
        value.put("valuePrimary", fact.valuePrimary());
        value.put("valueSecondary", fact.valueSecondary());
        value.put("unitCode", fact.unitCode());
        value.put("temporalityText", fact.temporalityText());
        value.put("laterality", fact.laterality());
        value.put("frequencyText", fact.frequencyText());
        value.put("routeText", fact.routeText());
        value.put("evidenceQuotes", fact.evidence().stream()
                .map(evidence -> evidence.quoteText())
                .limit(3)
                .toList());
        return value;
    }

    private Map<String, Object> transcriptItem(TranscriptItemView item) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", item.id().toString());
        value.put("sequence", item.sequence());
        value.put("speakerType", item.speakerType());
        value.put("speakerLabel", item.speakerLabel() == null ? "" : item.speakerLabel());
        value.put("locale", item.locale() == null ? "fr" : item.locale());
        value.put("text", item.text());
        return value;
    }
}
