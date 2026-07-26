package com.joprelys.backend.ai.application;

import java.util.Map;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

final class AiClinicalMemoryPromptDecorator {

    private final ObjectMapper objectMapper;

    AiClinicalMemoryPromptDecorator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    String decorate(String currentTurn, String locale, Map<String, Object> memory) {
        try {
            String instruction = "en".equals(locale)
                    ? "The following governed session memory contains only accepted facts and explicitly answered questions. "
                            + "Do not repeat an answered question unless the current turn contradicts its stored answer."
                    : "La mémoire de session gouvernée suivante contient uniquement les faits acceptés et les questions "
                            + "explicitement résolues. Ne repose pas une question déjà résolue, sauf contradiction dans le tour courant.";
            return instruction
                    + "\nGoverned session memory: "
                    + objectMapper.writeValueAsString(memory)
                    + "\nCurrent clinician turn: "
                    + currentTurn;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatusCode.valueOf(422), "AI_OUTPUT_INVALID");
        }
    }
}
