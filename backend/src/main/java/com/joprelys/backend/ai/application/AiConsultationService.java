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
import java.util.Objects;
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
    private static final int MAX_TRANSCRIPT_LENGTH = 12000;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "audio/webm", "audio/mp4", "audio/mpeg", "audio/wav");
    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "symptoms", "clinicalExam", "suspectedDiagnosis", "diagnosis",
            "finalDiagnosis", "conclusion", "advice", "followUp");
    private static final String SYSTEM_PROMPT = """
            Tu es un assistant de saisie clinique. Tu transformes uniquement les faits dictés
            par le médecin en brouillon structuré. Tu n'inventes aucun symptôme, diagnostic,
            traitement ou conseil. Tu conserves les négations, l'incertitude et les nuances.
            Tu ne prescris rien. Pour tout médicament mentionné, tu ne modifies jamais
            silencieusement le nom, le dosage, l'unité, la fréquence, la durée ou la voie
            d'administration. Si un élément est ambigu ou incertain (par exemple 15 mg ou
            50 mg), tu conserves la forme entendue et tu la marques [À CONFIRMER] au lieu
            de choisir arbitrairement. Retourne uniquement un objet JSON sans bloc Markdown :
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
        SessionKey key = sessionKey(visitId, userId, organizationId);
        SessionState state = new SessionState(UUID.randomUUID(), expiry());
        mergeAllowedDraft(state.draft, initialDraft);
        state.assistantMessage = "Décrivez les symptômes et l'examen clinique.";
        sessions.put(key, state);
        return toSessionView(visitId, state);
    }

    public Optional<SessionView> getSession(
            UUID visitId,
            UUID userId,
            UUID organizationId) {
        ensureActiveVisit(visitId);
        SessionKey key = sessionKey(visitId, userId, organizationId);
        SessionState state = sessions.get(key);
        if (state == null) {
            return Optional.empty();
        }
        synchronized (state) {
            if (state.expiresAt.isBefore(Instant.now())) {
                sessions.remove(key, state);
                return Optional.empty();
            }
            return Optional.of(toSessionView(visitId, state));
        }
    }

    public MessageView processText(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            String text) {
        validateTranscript(text);
        SessionState state = requireSession(visitId, userId, organizationId);
        return processMessage(state, text.trim(), null);
    }

    /**
     * Transcrit l'audio sans lancer l'analyse clinique. Le texte reste en attente
     * afin que le médecin puisse le relire et le corriger avant tout appel au modèle de chat.
     */
    public TranscriptionView transcribeAudio(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            byte[] audio,
            String contentType) {
        validateAudio(audio, contentType);
        SessionState state = requireSession(visitId, userId, organizationId);
        String normalizedMime = normalizeMimeType(contentType);
        try {
            AiTranscription transcription = aiProvider.transcribeAudio(
                    audio, normalizedMime, properties.locale());
            if (transcription == null || transcription.text() == null
                    || transcription.text().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "AI_OUTPUT_INVALID");
            }
            String transcript = limit(transcription.text().trim(), MAX_TRANSCRIPT_LENGTH);
            synchronized (state) {
                state.pendingTranscript = transcript;
                state.transcriptStatus = "PENDING_REVIEW";
                state.expiresAt = expiry();
                return new TranscriptionView(
                        state.sessionId,
                        transcript,
                        state.transcriptStatus,
                        state.expiresAt);
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Échec transcription IA provider={}, octets={}",
                    properties.speechProvider(), audio.length);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE");
        }
    }

    /**
     * Analyse une transcription explicitement confirmée ou corrigée par le médecin.
     */
    public MessageView analyzeTranscript(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            String transcript) {
        validateTranscript(transcript);
        SessionState state = requireSession(visitId, userId, organizationId);
        synchronized (state) {
            state.pendingTranscript = null;
            state.transcriptStatus = "ANALYZED";
        }
        return processMessage(state, transcript.trim(), transcript.trim());
    }

    /**
     * Contrat historique conservé pour compatibilité : transcription puis analyse immédiate.
     * Les nouveaux clients doivent utiliser transcribeAudio puis analyzeTranscript.
     */
    public MessageView processAudio(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            byte[] audio,
            String contentType) {
        TranscriptionView transcription = transcribeAudio(
                visitId, userId, organizationId, audio, contentType);
        return analyzeTranscript(
                visitId, userId, organizationId, transcription.transcript());
    }

    public void discardPendingTranscript(
            UUID visitId,
            UUID userId,
            UUID organizationId) {
        SessionState state = requireSession(visitId, userId, organizationId);
        synchronized (state) {
            state.pendingTranscript = null;
            state.transcriptStatus = "NONE";
            state.expiresAt = expiry();
        }
    }

    public void deleteSession(UUID visitId, UUID userId, UUID organizationId) {
        sessions.remove(sessionKey(visitId, userId, organizationId));
    }

    private MessageView processMessage(
            SessionState state,
            String text,
            String transcript) {
        synchronized (state) {
            state.messages.add(AiMessage.user(buildUserMessage(text, state.draft)));
            trimConversation(state.messages);
            try {
                AiChatResponse response = aiProvider.chat(
                        List.copyOf(state.messages), SYSTEM_PROMPT);
                if (response == null || response.content() == null) {
                    throw new ResponseStatusException(
                            HttpStatus.UNPROCESSABLE_ENTITY, "AI_OUTPUT_INVALID");
                }
                ParsedResponse parsed = parseProviderResponse(response.content());
                Map<String, String> previousDraft = new LinkedHashMap<>(state.draft);
                mergeAllowedDraft(state.draft, parsed.draft());
                List<String> changedFields = ALLOWED_FIELDS.stream()
                        .filter(field -> !Objects.equals(
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
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE");
            }
        }
    }

    private SessionState requireSession(
            UUID visitId,
            UUID userId,
            UUID organizationId) {
        ensureActiveVisit(visitId);
        SessionKey key = sessionKey(visitId, userId, organizationId);
        SessionState state = sessions.get(key);
        if (state == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "AI_SESSION_EXPIRED");
        }
        synchronized (state) {
            if (state.expiresAt.isBefore(Instant.now())) {
                sessions.remove(key, state);
                throw new ResponseStatusException(HttpStatus.CONFLICT, "AI_SESSION_EXPIRED");
            }
        }
        return state;
    }

    private void validateAudio(byte[] audio, String contentType) {
        if (audio == null || audio.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_MESSAGE_INVALID");
        }
        if (audio.length > MAX_AUDIO_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, "AI_AUDIO_TOO_LARGE");
        }
        if (!ALLOWED_MIME_TYPES.contains(normalizeMimeType(contentType))) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE, "AI_AUDIO_TYPE_UNSUPPORTED");
        }
    }

    private void validateTranscript(String transcript) {
        if (transcript == null || transcript.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_MESSAGE_INVALID");
        }
        if (transcript.length() > MAX_TRANSCRIPT_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, "AI_TRANSCRIPT_TOO_LARGE");
        }
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
                    + "\nNouvelle dictée ou correction du médecin: " + text;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "AI_OUTPUT_INVALID");
        }
    }

    @SuppressWarnings("unchecked")
    private ParsedResponse parseProviderResponse(String content) {
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "AI_OUTPUT_INVALID");
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
                    ? value
                    : "Brouillon mis à jour. Vérifiez les champs avant de les appliquer.";
            boolean needsClarification = root.get("needsClarification") instanceof Boolean value
                    && value;
            return new ParsedResponse(draft, assistantMessage, needsClarification);
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "AI_OUTPUT_INVALID");
        }
    }

    private void mergeAllowedDraft(
            Map<String, String> target,
            Map<String, String> source) {
        if (source == null) {
            return;
        }
        source.forEach((field, value) -> {
            if (ALLOWED_FIELDS.contains(field) && value != null && !value.isBlank()) {
                int maximum = field.equals("followUp") ? 1000 : 5000;
                target.put(field, limit(value.trim(), maximum));
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
                state.pendingTranscript,
                state.transcriptStatus,
                state.assistantMessage,
                state.needsClarification);
    }

    private SessionKey sessionKey(
            UUID visitId,
            UUID userId,
            UUID organizationId) {
        return new SessionKey(visitId, userId, organizationKey(organizationId));
    }

    private Instant expiry() {
        long seconds = Math.max(5, properties.sessionTtlMinutes()) * 60L;
        return Instant.now().plusSeconds(seconds);
    }

    private String normalizeMimeType(String contentType) {
        return contentType == null
                ? ""
                : contentType.split(";", 2)[0].trim().toLowerCase();
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
        private String pendingTranscript;
        private String transcriptStatus = "NONE";
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
            String pendingTranscript,
            String transcriptStatus,
            String assistantMessage,
            boolean needsClarification) {
    }

    public record TranscriptionView(
            UUID sessionId,
            String transcript,
            String status,
            Instant expiresAt) {
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
