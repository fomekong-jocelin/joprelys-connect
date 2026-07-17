package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.domain.AiChatResponse;
import com.joprelys.backend.ai.domain.AiMessage;
import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.domain.AiTranscription;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import com.joprelys.backend.visit.application.VisitService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class AiConsultationService {

    private static final Logger log = LoggerFactory.getLogger(AiConsultationService.class);
    private static final int MAX_AUDIO_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "audio/webm", "audio/mp4", "audio/mpeg", "audio/wav");
    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "symptoms", "clinicalExam", "suspectedDiagnosis", "diagnosis",
            "finalDiagnosis", "conclusion", "advice", "followUp");
    private static final String SYSTEM_PROMPT = """
            Tu es un assistant de saisie clinique. Tu transformes uniquement les faits dictés
            par le médecin en brouillon structuré. Tu n'inventes aucun symptôme, diagnostic,
            traitement ou conseil. Tu conserves les négations, l'incertitude et les nuances.
            Tu ne prescris rien. Retourne uniquement un objet JSON sans bloc Markdown :
            {
              "draft": {
                "symptoms": "...",
                "clinicalExam": "...",
                "suspectedDiagnosis": "...",
                "diagnosis": "...",
                "finalDiagnosis": "...",
                "conclusion": "...",
                "advice": "...",
                "followUp": "..."
              },
              "assistantMessage": "message court destiné au médecin",
              "needsClarification": false
            }
            Omets les champs non concernés. Le brouillon fourni est la base à corriger ou enrichir.
            """;

    private final AiProvider aiProvider;
    private final AiProperties properties;
    private final VisitService visitService;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<SessionKey, SessionState> sessions = new ConcurrentHashMap<>();

    public AiConsultationService(
            AiProvider aiProvider,
            AiProperties properties,
            VisitService visitService,
            ObjectMapper objectMapper) {
        this.aiProvider = aiProvider;
        this.properties = properties;
        this.visitService = visitService;
        this.objectMapper = objectMapper;
    }

    public SessionView startSession(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            Map<String, String> initialDraft) {
        ensureActiveVisit(visitId);
        SessionKey key = new SessionKey(visitId, userId, organizationKey(organizationId));
        SessionState state = new SessionState(UUID.randomUUID(), expiry());
        mergeAllowedDraft(state.draft, initialDraft);
        state.assistantMessage = "Décrivez les symptômes et l'examen clinique.";
        sessions.put(key, state);
        return toSessionView(visitId, state);
    }

    public Optional<SessionView> getSession(UUID visitId, UUID userId, UUID organizationId) {
        SessionKey key = new SessionKey(visitId, userId, organizationKey(organizationId));
        SessionState state = sessions.get(key);
        if (state == null) {
            return Optional.empty();
        }
        if (state.expiresAt.isBefore(Instant.now())) {
            sessions.remove(key);
            return Optional.empty();
        }
        return Optional.of(toSessionView(visitId, state));
    }

    public MessageView processText(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            String text) {
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_MESSAGE_INVALID");
        }
        SessionState state = requireSession(visitId, userId, organizationId);
        return processMessage(visitId, state, text.trim(), null);
    }

    public MessageView processAudio(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            byte[] audio,
            String contentType) {
        if (audio == null || audio.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_MESSAGE_INVALID");
        }
        if (audio.length > MAX_AUDIO_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "AI_AUDIO_TOO_LARGE");
        }
        String normalizedMime = normalizeMimeType(contentType);
        if (!ALLOWED_MIME_TYPES.contains(normalizedMime)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "AI_AUDIO_TYPE_UNSUPPORTED");
        }
        SessionState state = requireSession(visitId, userId, organizationId);
        try {
            AiTranscription transcription = aiProvider.transcribeAudio(
                    audio, normalizedMime, properties.locale());
            if (transcription == null || transcription.text() == null
                    || transcription.text().isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_OUTPUT_INVALID");
            }
            return processMessage(visitId, state, transcription.text().trim(), transcription.text().trim());
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Échec transcription IA provider={}, octets={}",
                    properties.speechProvider(), audio.length);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE");
        }
    }

    public void deleteSession(UUID visitId, UUID userId, UUID organizationId) {
        sessions.remove(new SessionKey(visitId, userId, organizationKey(organizationId)));
    }

    private MessageView processMessage(
            UUID visitId,
            SessionState state,
            String text,
            String transcript) {
        state.messages.add(AiMessage.user(buildUserMessage(text, state.draft)));
        trimConversation(state.messages);
        try {
            AiChatResponse response = aiProvider.chat(List.copyOf(state.messages), SYSTEM_PROMPT);
            ParsedResponse parsed = parseProviderResponse(response.content());
            Map<String, String> previousDraft = new LinkedHashMap<>(state.draft);
            mergeAllowedDraft(state.draft, parsed.draft());
            List<String> changedFields = ALLOWED_FIELDS.stream()
                    .filter(field -> !java.util.Objects.equals(
                            previousDraft.get(field), state.draft.get(field)))
                    .sorted()
                    .toList();
            state.assistantMessage = parsed.assistantMessage();
            state.needsClarification = parsed.needsClarification();
            state.transcript = transcript;
            state.expiresAt = expiry();
            state.messages.add(AiMessage.assistant(response.content()));
            trimConversation(state.messages);
            return new MessageView(
                    state.sessionId,
                    transcript,
                    Map.copyOf(state.draft),
                    changedFields,
                    state.assistantMessage,
                    state.needsClarification,
                    state.expiresAt);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Échec génération brouillon IA provider={}", properties.provider());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE");
        }
    }

    private SessionState requireSession(UUID visitId, UUID userId, UUID organizationId) {
        ensureActiveVisit(visitId);
        SessionKey key = new SessionKey(visitId, userId, organizationKey(organizationId));
        SessionState state = sessions.get(key);
        if (state == null || state.expiresAt.isBefore(Instant.now())) {
            sessions.remove(key);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "AI_SESSION_EXPIRED");
        }
        return state;
    }

    private void ensureActiveVisit(UUID visitId) {
        var visit = visitService.getVisit(visitId);
        if (!"EN_COURS".equals(visit.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "VISIT_NOT_ACTIVE");
        }
    }

    private String buildUserMessage(String text, Map<String, String> draft) {
        try {
            return "Brouillon actuel: " + objectMapper.writeValueAsString(draft)
                    + "\nNouvelle dictée du médecin: " + text;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_OUTPUT_INVALID");
        }
    }

    @SuppressWarnings("unchecked")
    private ParsedResponse parseProviderResponse(String content) {
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_OUTPUT_INVALID");
        }
        String cleaned = content.trim()
                .replaceFirst("^```(?:json)?\\s*", "")
                .replaceFirst("\\s*```$", "");
        try {
            Map<String, Object> root = objectMapper.readValue(cleaned, Map.class);
            Map<String, String> draft = new LinkedHashMap<>();
            Object draftValue = root.get("draft");
            if (draftValue instanceof Map<?, ?> draftMap) {
                draftMap.forEach((key, value) -> {
                    if (key != null && value instanceof String stringValue
                            && ALLOWED_FIELDS.contains(key.toString())) {
                        draft.put(key.toString(), stringValue);
                    }
                });
            }
            String assistantMessage = root.get("assistantMessage") instanceof String value
                    ? value : "Brouillon mis à jour. Vérifiez les champs avant de les appliquer.";
            boolean needsClarification = root.get("needsClarification") instanceof Boolean value
                    && value;
            return new ParsedResponse(draft, assistantMessage, needsClarification);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_OUTPUT_INVALID");
        }
    }

    private void mergeAllowedDraft(Map<String, String> target, Map<String, String> source) {
        if (source == null) {
            return;
        }
        source.forEach((field, value) -> {
            if (ALLOWED_FIELDS.contains(field) && value != null && !value.isBlank()) {
                target.put(field, limit(value.trim(), field.equals("followUp") ? 1000 : 5000));
            }
        });
    }

    private void trimConversation(List<AiMessage> messages) {
        int maximum = Math.max(2, properties.maxConversationTurns() * 2);
        while (messages.size() > maximum) {
            messages.removeFirst();
        }
    }

    private SessionView toSessionView(UUID visitId, SessionState state) {
        return new SessionView(
                state.sessionId,
                visitId,
                "ACTIVE",
                state.expiresAt,
                Map.copyOf(state.draft),
                state.transcript,
                state.assistantMessage,
                state.needsClarification);
    }

    private Instant expiry() {
        return Instant.now().plusSeconds(Math.max(5, properties.sessionTtlMinutes()) * 60L);
    }

    private String normalizeMimeType(String contentType) {
        return contentType == null ? "" : contentType.split(";", 2)[0].trim().toLowerCase();
    }

    private String organizationKey(UUID organizationId) {
        return organizationId == null ? "GLOBAL" : organizationId.toString();
    }

    private String limit(String value, int maximum) {
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }

    private record SessionKey(UUID visitId, UUID userId, String organizationId) {
    }

    private record ParsedResponse(
            Map<String, String> draft,
            String assistantMessage,
            boolean needsClarification) {
    }

    private static final class SessionState {
        private final UUID sessionId;
        private final Map<String, String> draft = new LinkedHashMap<>();
        private final List<AiMessage> messages = new ArrayList<>();
        private Instant expiresAt;
        private String transcript;
        private String assistantMessage;
        private boolean needsClarification;

        private SessionState(UUID sessionId, Instant expiresAt) {
            this.sessionId = sessionId;
            this.expiresAt = expiresAt;
        }
    }

    public record SessionView(
            UUID sessionId,
            UUID visitId,
            String status,
            Instant expiresAt,
            Map<String, String> draft,
            String transcript,
            String assistantMessage,
            boolean needsClarification) {
    }

    public record MessageView(
            UUID sessionId,
            String transcript,
            Map<String, String> draft,
            List<String> changedFields,
            String assistantMessage,
            boolean needsClarification,
            Instant expiresAt) {
    }
}
