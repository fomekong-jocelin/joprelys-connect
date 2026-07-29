package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiConsultationContract.ClarificationView;
import java.util.Map;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

final class AiConsultationMessageBuilder {

    private final ObjectMapper objectMapper;

    AiConsultationMessageBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    String buildUserMessage(
            String text,
            Map<String, String> draft,
            String locale,
            Map<String, Object> clinicalContext) {
        try {
            String languageInstruction = "en".equals(locale)
                    ? "Reply in English."
                    : "Réponds en français.";
            String contextInstruction = "en".equals(locale)
                    ? "The secure clinical context below is READ-ONLY background. Use it to understand risk, detect conflicts, "
                            + "and ask a targeted safety clarification when useful. Never turn a background fact into a proposed "
                            + "consultation change unless the clinician explicitly states or confirms it in the current turn."
                    : "Le contexte clinique sécurisé ci-dessous est un arrière-plan EN LECTURE SEULE. Utilise-le pour comprendre "
                            + "les risques, détecter les incohérences et demander une clarification de sécurité ciblée si utile. "
                            + "Ne transforme jamais un fait de contexte en modification proposée de la consultation tant que le "
                            + "professionnel ne l'a pas explicitement énoncé ou confirmé dans le tour courant.";
            return "Session locale: " + locale
                    + "\n" + languageInstruction
                    + "\n" + contextInstruction
                    + "\nContexte clinique sécurisé: "
                    + objectMapper.writeValueAsString(clinicalContext)
                    + "\nBrouillon accepté (contexte uniquement, ne pas le recopier spontanément): "
                    + objectMapper.writeValueAsString(draft)
                    + "\nNouvelle dictée, correction ou réponse du médecin: " + text;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatusCode.valueOf(422), "AI_OUTPUT_INVALID");
        }
    }

    String buildCaptureExtractionMessage(
            String transcript,
            Map<String, String> draft,
            String locale) {
        try {
            return "LOCALE: " + normalizeLocale(locale)
                    + "\nACCEPTED DRAFT (deduplication context only): "
                    + objectMapper.writeValueAsString(draft == null ? Map.of() : draft)
                    + "\nCURRENT TRANSCRIPT:\n"
                    + transcript;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatusCode.valueOf(422), "AI_OUTPUT_INVALID");
        }
    }

    String clarificationModelText(
            String locale,
            ClarificationView clarification,
            String answer) {
        if ("en".equals(locale)) {
            return "Clinician answer to a structured clarification. Field: "
                    + clarification.field()
                    + ". Question: " + clarification.question()
                    + ". Answer: " + answer;
        }
        return "Réponse du médecin à une clarification structurée. Champ: "
                + clarification.field()
                + ". Question: " + clarification.question()
                + ". Réponse: " + answer;
    }

    String normalizeLocale(String locale) {
        return "en".equalsIgnoreCase(locale) ? "en" : "fr";
    }

    String initialAssistantMessage(String locale) {
        return "en".equals(locale)
                ? "Hello doctor. I’m listening."
                : "Bonjour docteur. Je vous écoute.";
    }
}
