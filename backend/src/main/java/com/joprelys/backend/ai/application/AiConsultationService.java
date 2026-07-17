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
    private static final int MAX_CLARIFICATION_OPTIONS = 5;
    private static final String INITIAL_ASSISTANT_MESSAGE =
            "Décrivez les symptômes et l'examen clinique.";
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
            50 mg), tu conserves la forme entendue et tu demandes une clarification au lieu
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
              "needsClarification": false,
              "clarification": null
            }
            Quand needsClarification vaut true, clarification est obligatoire et respecte :
            {
              "field": "un champ exact du brouillon",
              "question": "question précise destinée au médecin",
              "options": ["option facultative 1", "option facultative 2"]
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
        state.assistantMessage = INITIAL_ASSISTANT_MESSAGE;
        appendVisibleMessage(
                state,
                "ASSISTANT",
                INITIAL_ASSISTANT_MESSAGE,
                "SYSTEM",
                false);
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
        return processMessage(
                state,
                text.trim(),
                text.trim(),
                null,
                "TEXT",
                null,
                null);
    }

    public MessageView answerClarification(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            UUID clarificationId,
            String answer) {
        validateTranscript(answer);
        SessionState state = requireSession(visitId, userId, organizationId);
        synchronized (state) {
            ClarificationView clarification = findPendingClarification(state, clarificationId);
            String modelText = "Réponse du médecin à une clarification structurée. Champ: "
                    + clarification.field()
                    + ". Question: " + clarification.question()
                    + ". Réponse: " + answer.trim();
            return processMessage(
                    state,
                    modelText,
                    answer.trim(),
                    null,
                    "CLARIFICATION",
                    clarificationId,
                    answer.trim());
        }
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
        MessageView response = processMessage(
                state,
                transcript.trim(),
                transcript.trim(),
                transcript.trim(),
                "AUDIO",
                null,
                null);
        synchronized (state) {
            state.pendingTranscript = null;
            state.transcriptStatus = "ANALYZED";
            state.expiresAt = expiry();
        }
        return response;
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
            String modelText,
            String visibleText,
            String transcript,
            String source,
            UUID resolvedClarificationId,
            String clarificationAnswer) {
        synchronized (state) {
            List<AiMessage> providerMessages = new ArrayList<>(state.messages);
            providerMessages.add(AiMessage.user(buildUserMessage(modelText, state.draft)));
            trimConversation(providerMessages);
            try {
                AiChatResponse response = aiProvider.chat(
                        List.copyOf(providerMessages), SYSTEM_PROMPT);
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
                if (resolvedClarificationId != null) {
                    resolveClarification(
                            state, resolvedClarificationId, clarificationAnswer);
                }
                if (parsed.needsClarification()) {
                    appendClarification(state, parsed.clarification());
                }
                state.assistantMessage = parsed.assistantMessage();
                state.needsClarification = parsed.needsClarification();
                if (transcript != null) {
                    state.transcript = transcript;
                }
                state.expiresAt = expiry();
                state.messages.clear();
                state.messages.addAll(providerMessages);
                state.messages.add(AiMessage.assistant(response.content()));
                trimConversation(state.messages);
                appendVisibleMessage(state, "USER", visibleText, source, false);
                appendVisibleMessage(
                        state,
                        "ASSISTANT",
                        state.assistantMessage,
                        "AI",
                        state.needsClarification);
                return toMessageView(state, changedFields);
            } catch (ResponseStatusException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                log.warn("Échec génération brouillon IA provider={}", properties.provider());
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE");
            }
        }
    }

    private MessageView toMessageView(
            SessionState state,
            List<String> changedFields) {
        return new MessageView(
                state.sessionId,
                state.transcript,
                Map.copyOf(state.draft),
                changedFields,
                state.assistantMessage,
                state.needsClarification,
                List.copyOf(state.conversation),
                List.copyOf(state.clarifications),
                state.expiresAt);
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

    private ClarificationView findPendingClarification(
            SessionState state,
            UUID clarificationId) {
        return state.clarifications.stream()
                .filter(clarification -> clarification.id().equals(clarificationId))
                .filter(clarification -> "PENDING".equals(clarification.status()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "AI_CLARIFICATION_NOT_PENDING"));
    }

    private void resolveClarification(
            SessionState state,
            UUID clarificationId,
            String answer) {
        for (int index = 0; index < state.clarifications.size(); index++) {
            ClarificationView clarification = state.clarifications.get(index);
            if (clarification.id().equals(clarificationId)
                    && "PENDING".equals(clarification.status())) {
                state.clarifications.set(index, new ClarificationView(
                        clarification.id(),
                        clarification.field(),
                        clarification.question(),
                        "RESOLVED",
                        clarification.options(),
                        clarification.createdAt(),
                        answer,
                        Instant.now()));
                return;
            }
        }
        throw new ResponseStatusException(
                HttpStatus.CONFLICT, "AI_CLARIFICATION_NOT_PENDING");
    }

    private void appendClarification(
            SessionState state,
            ParsedClarification clarification) {
        state.clarifications.add(new ClarificationView(
                UUID.randomUUID(),
                clarification.field(),
                clarification.question(),
                "PENDING",
                List.copyOf(clarification.options()),
                Instant.now(),
                null,
                null));
        int maximum = Math.max(5, properties.maxConversationTurns());
        while (state.clarifications.size() > maximum) {
            int removableIndex = firstResolvedClarificationIndex(state.clarifications);
            state.clarifications.remove(removableIndex >= 0 ? removableIndex : 0);
        }
    }

    private int firstResolvedClarificationIndex(
            List<ClarificationView> clarifications) {
        for (int index = 0; index < clarifications.size(); index++) {
            if ("RESOLVED".equals(clarifications.get(index).status())) {
                return index;
            }
        }
        return -1;
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
                    + "\nNouvelle dictée, correction ou réponse du médecin: " + text;
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
                    && !value.isBlank()
                    ? limit(value.trim(), 2000)
                    : "Brouillon mis à jour. Vérifiez les champs avant de les appliquer.";
            boolean needsClarification = root.get("needsClarification") instanceof Boolean value
                    && value;
            ParsedClarification clarification = needsClarification
                    ? parseClarification(root.get("clarification"))
                    : null;
            return new ParsedResponse(
                    draft,
                    assistantMessage,
                    needsClarification,
                    clarification);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "AI_OUTPUT_INVALID");
        }
    }

    private ParsedClarification parseClarification(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "AI_CLARIFICATION_INVALID");
        }
        String field = map.get("field") instanceof String stringValue
                ? stringValue.trim() : "";
        String question = map.get("question") instanceof String stringValue
                ? stringValue.trim() : "";
        if (!ALLOWED_FIELDS.contains(field) || question.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "AI_CLARIFICATION_INVALID");
        }
        List<String> options = new ArrayList<>();
        Object optionValue = map.get("options");
        if (optionValue instanceof List<?> optionList) {
            optionList.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(String::trim)
                    .filter(option -> !option.isBlank())
                    .limit(MAX_CLARIFICATION_OPTIONS)
                    .map(option -> limit(option, 200))
                    .forEach(options::add);
        }
        return new ParsedClarification(
                field,
                limit(question, 1000),
                options);
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

    private void appendVisibleMessage(
            SessionState state,
            String role,
            String content,
            String source,
            boolean needsClarification) {
        state.conversation.add(new ConversationMessageView(
                UUID.randomUUID(),
                role,
                limit(content == null ? "" : content.trim(), MAX_TRANSCRIPT_LENGTH),
                source,
                Instant.now(),
                needsClarification));
        int maximum = Math.max(5, properties.maxConversationTurns() * 2 + 1);
        while (state.conversation.size() > maximum) {
            state.conversation.removeFirst();
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
                List.copyOf(state.conversation),
                List.copyOf(state.clarifications),
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

    private record ParsedClarification(
            String field,
            String question,
            List<String> options) {
    }

    private record ParsedResponse(
            Map<String, String> draft,
            String assistantMessage,
            boolean needsClarification,
            ParsedClarification clarification) {
    }

    private static final class SessionState {
        private final UUID sessionId;
        private final Map<String, String> draft = new LinkedHashMap<>();
        private final List<AiMessage> messages = new ArrayList<>();
        private final List<ConversationMessageView> conversation = new ArrayList<>();
        private final List<ClarificationView> clarifications = new ArrayList<>();
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

    public record ConversationMessageView(
            UUID id,
            String role,
            String content,
            String source,
            Instant createdAt,
            boolean needsClarification) {
    }

    public record ClarificationView(
            UUID id,
            String field,
            String question,
            String status,
            List<String> options,
            Instant createdAt,
            String answer,
            Instant resolvedAt) {
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
            List<ConversationMessageView> conversation,
            List<ClarificationView> clarifications,
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
            List<ConversationMessageView> conversation,
            List<ClarificationView> clarifications,
            Instant expiresAt) {
    }
}
