package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedResponse;

final class AiRepeatedClarificationGuard {

    private final AiClinicalMemoryManager memoryManager = new AiClinicalMemoryManager();

    ParsedResponse enforce(
            ParsedResponse parsed,
            AiConsultationSessionState state,
            String locale) {
        if (parsed == null
                || !parsed.needsClarification()
                || parsed.clarification() == null) {
            return parsed;
        }
        if (!memoryManager.wasAlreadyAnswered(
                state,
                parsed.clarification().field(),
                parsed.clarification().question())) {
            return parsed;
        }
        String message = "en".equalsIgnoreCase(locale)
                ? "This information is already recorded. Please continue."
                : "Cette précision est déjà enregistrée. Vous pouvez poursuivre.";
        return new ParsedResponse(
                parsed.changes(),
                message,
                false,
                null);
    }
}
