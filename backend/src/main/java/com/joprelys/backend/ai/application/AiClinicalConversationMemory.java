package com.joprelys.backend.ai.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Structured memory kept inside a governed consultation session.
 *
 * <p>Only accepted draft values and clarifications explicitly answered by the
 * clinician are retained. Pending/rejected model proposals never enter this
 * memory.</p>
 */
final class AiClinicalConversationMemory {

    final Map<String, String> acceptedFacts = new LinkedHashMap<>();
    final List<AnsweredClarification> answeredClarifications = new ArrayList<>();

    record AnsweredClarification(
            String field,
            String question,
            String answer,
            Instant resolvedAt) {
    }
}
