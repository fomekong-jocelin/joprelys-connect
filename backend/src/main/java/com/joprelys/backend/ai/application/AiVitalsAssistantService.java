package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiVitalsResponseParser.ParsedVitals;
import com.joprelys.backend.ai.domain.AiChatResponse;
import com.joprelys.backend.ai.domain.AiMessage;
import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.domain.AiTranscription;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import com.joprelys.backend.visit.application.VisitService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class AiVitalsAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiVitalsAssistantService.class);
    private static final int MAX_TEXT_LENGTH = 3000;
    private static final String SYSTEM_PROMPT = """
            You are Joprelys Vitals Copilot, a voice-first clinical data-entry assistant used by healthcare professionals.

            Your task is strictly limited to vital signs and physical measurements explicitly stated by the healthcare professional.
            You never diagnose, prescribe, infer a missing value, convert an ambiguous shorthand silently, or save data.
            The human user remains responsible for final validation and persistence.

            Supported fields and units:
            - temperature: Celsius, accepted application range 30..45
            - weight: kilograms, 1..500
            - height: centimeters, 30..250
            - pulse: beats/minute, 20..250
            - systolic: mmHg, 40..250
            - diastolic: mmHg, 30..150
            - spo2: percent, 50..100
            - glycemia: g/L, 0.1..10
            - respiratoryRate: breaths/minute, 5..100
            - painScale: 0..10

            Safety rules:
            1. Extract only values that were explicitly spoken or typed.
            2. Never manufacture a unit or transform an ambiguous blood-pressure shorthand. For example, "tension 12/8" must trigger confirmation; do not silently output 120/80.
            3. If a value is ambiguous, lacks the required unit/context, conflicts with another value, or falls outside the accepted application range, omit that field from vitals and set needsConfirmation=true.
            4. A correction such as "non, température 37,2" replaces only the explicitly corrected field. Never clear other existing values.
            5. Preserve decimals exactly when clinically meaningful.
            6. Understand natural French and English medical speech, including common Francophone/Cameroonian phrasing, while keeping numeric values literal.
            7. Ask at most one concise clarification at a time.
            8. assistantMessage must be short, natural and suitable for speech. Use the requested locale (fr or en).
            9. Return JSON only, without Markdown.

            Exact JSON schema:
            {
              "assistantMessage": "string",
              "needsConfirmation": true|false,
              "confirmationReason": "string, required when needsConfirmation=true",
              "vitals": {
                "temperature": number,
                "weight": number,
                "height": number,
                "pulse": number,
                "systolic": number,
                "diastolic": number,
                "spo2": number,
                "glycemia": number,
                "respiratoryRate": number,
                "painScale": number
              }
            }

            Include only detected and safe-to-propose keys inside vitals.
            """;

    private final AiProvider aiProvider;
    private final AiProperties properties;
    private final AiVitalsResponseParser parser;
    private final VisitService visitService;
    private final ObjectMapper objectMapper;

    public AiVitalsAssistantService(
            AiProvider aiProvider,
            AiProperties properties,
            AiVitalsResponseParser parser,
            VisitService visitService,
            ObjectMapper objectMapper) {
        this.aiProvider = aiProvider;
        this.properties = properties;
        this.parser = parser;
        this.visitService = visitService;
        this.objectMapper = objectMapper;
    }

    public VitalsAssistantResponse analyzeText(
            UUID visitId,
            String text,
            String locale,
            Map<String, Double> currentVitals) {
        ensureActiveVisit(visitId);
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_MESSAGE_INVALID");
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "AI_TRANSCRIPT_TOO_LARGE");
        }
        String normalizedLocale = normalizeLocale(locale);
        return analyze(visitId, text.trim(), text.trim(), normalizedLocale, currentVitals);
    }

    public VitalsAssistantResponse analyzeAudio(
            UUID visitId,
            byte[] audio,
            String mimeType,
            String locale,
            Map<String, Double> currentVitals) {
        ensureActiveVisit(visitId);
        String normalizedLocale = normalizeLocale(locale);
        AiTranscription transcription;
        try {
            transcription = aiProvider.transcribeAudio(audio, mimeType, normalizedLocale);
        } catch (RuntimeException exception) {
            log.warn("Vitals audio transcription failed provider={}, bytes={}",
                    properties.speechProvider(), audio == null ? 0 : audio.length);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE");
        }
        if (transcription == null || transcription.text() == null || transcription.text().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_OUTPUT_INVALID");
        }
        String transcript = transcription.text().trim();
        if (transcript.length() > MAX_TEXT_LENGTH) {
            transcript = transcript.substring(0, MAX_TEXT_LENGTH);
        }
        return analyze(visitId, transcript, transcript, normalizedLocale, currentVitals);
    }

    private VitalsAssistantResponse analyze(
            UUID visitId,
            String modelText,
            String transcript,
            String locale,
            Map<String, Double> currentVitals) {
        try {
            Map<String, Double> normalizedCurrent = normalizeCurrentVitals(currentVitals);
            String userMessage = "Requested locale: " + locale
                    + "\nVisit ID: " + visitId
                    + "\nCurrent validated/input values: " + objectMapper.writeValueAsString(normalizedCurrent)
                    + "\nNew utterance: " + modelText;
            AiChatResponse response = aiProvider.chat(List.of(AiMessage.user(userMessage)), SYSTEM_PROMPT);
            if (response == null || response.content() == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_OUTPUT_INVALID");
            }
            ParsedVitals parsed = parser.parse(response.content());
            return new VitalsAssistantResponse(
                    transcript,
                    parsed.vitals(),
                    parsed.assistantMessage(),
                    parsed.needsConfirmation(),
                    parsed.confirmationReason());
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Vitals extraction failed provider={}", properties.provider());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE");
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_OUTPUT_INVALID");
        }
    }

    private Map<String, Double> normalizeCurrentVitals(Map<String, Double> currentVitals) {
        if (currentVitals == null || currentVitals.isEmpty()) {
            return Map.of();
        }
        Map<String, Double> normalized = new LinkedHashMap<>();
        currentVitals.forEach((field, value) -> {
            if (AiVitalsResponseParser.VITAL_FIELDS.contains(field)
                    && value != null
                    && Double.isFinite(value)) {
                normalized.put(field, value);
            }
        });
        return normalized;
    }

    private void ensureActiveVisit(UUID visitId) {
        var visit = visitService.getVisit(visitId);
        if (!"EN_COURS".equals(visit.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "VISIT_NOT_ACTIVE");
        }
    }

    private String normalizeLocale(String locale) {
        return "en".equalsIgnoreCase(locale) ? "en" : "fr";
    }

    public record VitalsAssistantResponse(
            String transcript,
            Map<String, Double> vitals,
            String assistantMessage,
            boolean needsConfirmation,
            String confirmationReason) {
    }
}
