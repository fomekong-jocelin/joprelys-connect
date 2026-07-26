package com.joprelys.backend.ai.application;

import static com.joprelys.backend.ai.application.AiConsultationPrompt.SYSTEM_PROMPT;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedResponse;
import com.joprelys.backend.ai.application.AiConsultationContract.ClarificationView;
import com.joprelys.backend.ai.application.AiConsultationContract.ConversationMessageView;
import com.joprelys.backend.ai.application.AiConsultationContract.MessageView;
import com.joprelys.backend.ai.application.AiConsultationContract.RevisionView;
import com.joprelys.backend.ai.application.AiConsultationContract.SessionView;
import com.joprelys.backend.ai.application.AiConsultationContract.TranscriptionView;
import com.joprelys.backend.ai.domain.AiChatResponse;
import com.joprelys.backend.ai.domain.AiMessage;
import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.domain.AiTranscription;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import com.joprelys.backend.visit.application.VisitService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "audio/webm", "audio/mp4", "audio/mpeg", "audio/wav");

    private final AiProvider aiProvider;
    private final AiProperties properties;
    private final VisitService visitService;
    private final ObjectMapper objectMapper;
    private final AiClinicalResponseParser responseParser;
    private final ClinicalContextAssembler clinicalContextAssembler;
    private final AiClinicalGroundingGuard groundingGuard;
    private final AiMedicationSafetyGuard medicationSafetyGuard;
    private final AiClinicalToolDispatcher toolDispatcher = new AiClinicalToolDispatcher();
    private final AiRevisionManager revisionManager;
    private final AiClarificationManager clarificationManager;
    private final ConcurrentMap<SessionKey, AiConsultationSessionState> sessions =
            new ConcurrentHashMap<>();

    public AiConsultationService(
            AiProvider aiProvider,
            AiProperties properties,
            VisitService visitService,
            ObjectMapper objectMapper,
            AiClinicalResponseParser responseParser,
            ClinicalContextAssembler clinicalContextAssembler,
            AiRevisionManager revisionManager,
            AiClarificationManager clarificationManager) {
        this.aiProvider = aiProvider;
        this.properties = properties;
        this.visitService = visitService;
        this.objectMapper = objectMapper;
        this.responseParser = responseParser;
        this.clinicalContextAssembler = clinicalContextAssembler;
        this.groundingGuard = new AiClinicalGroundingGuard(objectMapper);
        this.medicationSafetyGuard = new AiMedicationSafetyGuard(objectMapper);
        this.revisionManager = revisionManager;
        this.clarificationManager = clarificationManager;
    }

    public SessionView startSession(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            Map<String, String> initialDraft) {
        return startSession(
                visitId,
                userId,
                organizationId,
                initialDraft,
                properties.locale());
    }

    public SessionView startSession(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            Map<String, String> initialDraft,
            String requestedLocale) {
        ensureActiveVisit(visitId);
        String locale = normalizeLocale(requestedLocale);
        SessionKey key = sessionKey(visitId, userId, organizationId);
        AiConsultationSessionState state = new AiConsultationSessionState(
                UUID.randomUUID(), visitId, expiry(), locale);
        mergeInitialDraft(state.draft, initialDraft);
        state.assistantMessage = initialAssistantMessage(locale);
        appendVisibleMessage(
                state,
                "ASSISTANT",
                state.assistantMessage,
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
        AiConsultationSessionState state = sessions.get(key);
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

    public String sessionLocale(
            UUID visitId,
            UUID userId,
            UUID organizationId) {
        return requireSession(visitId, userId, organizationId).locale;
    }

    public MessageView processText(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            String text) {
        validateText(text);
        AiConsultationSessionState state = requireSession(
                visitId, userId, organizationId);
        synchronized (state) {
            ensureReadyForNewInput(state);
            return processMessageLocked(
                    state,
                    text.trim(),
                    text.trim(),
                    null,
                    "TEXT",
                    null,
                    null);
        }
    }

    public MessageView answerClarification(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            UUID clarificationId,
            String answer) {
        validateText(answer);
        AiConsultationSessionState state = requireSession(
                visitId, userId, organizationId);
        synchronized (state) {
            revisionManager.ensureNoPendingRevision(state);
            ClarificationView clarification = clarificationManager.findPending(
                    state, clarificationId);
            String modelText = clarificationModelText(state.locale, clarification, answer.trim());
            return processMessageLocked(
                    state,
                    modelText,
                    answer.trim(),
                    null,
                    "CLARIFICATION",
                    clarificationId,
                    answer.trim());
        }
    }

    public TranscriptionView transcribeAudio(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            byte[] audio,
            String contentType) {
        validateAudio(audio, contentType);
        AiConsultationSessionState state = requireSession(
                visitId, userId, organizationId);
        synchronized (state) {
            ensureReadyForNewInput(state);
            String normalizedMime = normalizeMimeType(contentType);
            try {
                AiTranscription transcription = aiProvider.transcribeAudio(
                        audio, normalizedMime, state.locale);
                if (transcription == null
                        || transcription.text() == null
                        || transcription.text().isBlank()) {
                    throw new ResponseStatusException(
                            org.springframework.http.HttpStatusCode.valueOf(422), "AI_OUTPUT_INVALID");
                }
                String transcript = limit(
                        transcription.text().trim(), MAX_TRANSCRIPT_LENGTH);
                state.pendingTranscript = transcript;
                state.transcriptStatus = "PENDING_REVIEW";
                state.expiresAt = expiry();
                return new TranscriptionView(
                        state.sessionId,
                        transcript,
                        state.transcriptStatus,
                        state.expiresAt);
            } catch (ResponseStatusException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                log.warn("Échec transcription IA provider={}, octets={}",
                        properties.speechProvider(), audio.length);
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE");
            }
        }
    }

    public MessageView analyzeTranscript(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            String transcript) {
        validateText(transcript);
        AiConsultationSessionState state = requireSession(
                visitId, userId, organizationId);
        synchronized (state) {
            if (state.pendingTranscript == null) {
                throw conflict("AI_TRANSCRIPT_REVIEW_REQUIRED");
            }
            clarificationManager.ensureNoPending(state);
            revisionManager.ensureNoPendingRevision(state);
            MessageView response = processMessageLocked(
                    state,
                    transcript.trim(),
                    transcript.trim(),
                    transcript.trim(),
                    "AUDIO",
                    null,
                    null);
            state.pendingTranscript = null;
            state.transcriptStatus = "ANALYZED";
            state.expiresAt = expiry();
            return response;
        }
    }

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
        AiConsultationSessionState state = requireSession(
                visitId, userId, organizationId);
        synchronized (state) {
            state.pendingTranscript = null;
            state.transcriptStatus = "NONE";
            state.expiresAt = expiry();
        }
    }

    public SessionView decideProposal(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            UUID revisionId,
            UUID proposalId,
            String decision) {
        AiConsultationSessionState state = requireSession(
                visitId, userId, organizationId);
        synchronized (state) {
            revisionManager.decideProposal(
                    state, revisionId, proposalId, decision);
            state.expiresAt = expiry();
            return toSessionView(visitId, state);
        }
    }

    public SessionView decideRevision(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            UUID revisionId,
            String decision) {
        AiConsultationSessionState state = requireSession(
                visitId, userId, organizationId);
        synchronized (state) {
            revisionManager.decideRevision(state, revisionId, decision);
            state.expiresAt = expiry();
            return toSessionView(visitId, state);
        }
    }

    public void deleteSession(UUID visitId, UUID userId, UUID organizationId) {
        sessions.remove(sessionKey(visitId, userId, organizationId));
    }

    private MessageView processMessageLocked(
            AiConsultationSessionState state,
            String modelText,
            String visibleText,
            String transcript,
            String source,
            UUID resolvedClarificationId,
            String clarificationAnswer) {
        String resolvedClarificationField = resolvedClarificationId == null
                ? null
                : clarificationManager.findPending(state, resolvedClarificationId).field();

        Map<String, Object> clinicalContext;
        try {
            clinicalContext = clinicalContextAssembler.assemble(state.visitId);
        } catch (RuntimeException exception) {
            log.error("Contexte clinique indisponible visitId={}", state.visitId, exception);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "AI_CLINICAL_CONTEXT_UNAVAILABLE");
        }

        List<AiMessage> providerMessages = new ArrayList<>(state.providerMessages);
        providerMessages.add(AiMessage.user(buildUserMessage(
                modelText, state.draft, state.locale, clinicalContext)));
        trimProviderConversation(providerMessages);
        try {
            AiChatResponse response = aiProvider.chat(
                    List.copyOf(providerMessages), SYSTEM_PROMPT);
            if (response == null || response.content() == null) {
                throw new ResponseStatusException(
                        org.springframework.http.HttpStatusCode.valueOf(422), "AI_OUTPUT_INVALID");
            }
            ParsedResponse rawParsed = responseParser.parse(response.content());
            ParsedResponse grounded = groundingGuard.enforce(
                    rawParsed, visibleText, resolvedClarificationField, state.locale);
            ParsedResponse parsed = "prescription".equals(resolvedClarificationField)
                    ? grounded
                    : medicationSafetyGuard.enforce(grounded, clinicalContext, state.locale);
            var toolPlan = toolDispatcher.dispatch(parsed);
            boolean adjustedOutput = parsed != rawParsed;

            if (resolvedClarificationId != null) {
                clarificationManager.resolve(
                        state, resolvedClarificationId, clarificationAnswer);
            }
            if (toolPlan.clarification() != null) {
                clarificationManager.append(state, toolPlan.clarification());
            }
            RevisionView revision = toolPlan.clarification() != null
                    ? null
                    : revisionManager.createRevision(state, toolPlan.changes());
            List<String> changedFields = revision == null
                    ? List.of()
                    : revision.proposals().stream()
                            .map(AiConsultationContract.FieldProposalView::field)
                            .toList();
            state.assistantMessage = parsed.assistantMessage();
            state.needsClarification = toolPlan.clarification() != null;
            if (transcript != null) {
                state.transcript = transcript;
            }
            state.expiresAt = expiry();
            state.providerMessages.clear();
            state.providerMessages.addAll(providerMessages);
            state.providerMessages.add(AiMessage.assistant(
                    adjustedOutput ? parsed.assistantMessage() : response.content()));
            trimProviderConversation(state.providerMessages);
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

    private MessageView toMessageView(
            AiConsultationSessionState state,
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
                List.copyOf(state.revisions),
                state.expiresAt);
    }

    private AiConsultationSessionState requireSession(
            UUID visitId,
            UUID userId,
            UUID organizationId) {
        ensureActiveVisit(visitId);
        SessionKey key = sessionKey(visitId, userId, organizationId);
        AiConsultationSessionState state = sessions.get(key);
        if (state == null) {
            throw conflict("AI_SESSION_EXPIRED");
        }
        synchronized (state) {
            if (state.expiresAt.isBefore(Instant.now())) {
                sessions.remove(key, state);
                throw conflict("AI_SESSION_EXPIRED");
            }
        }
        return state;
    }

    private void ensureReadyForNewInput(AiConsultationSessionState state) {
        if (state.pendingTranscript != null) {
            throw conflict("AI_TRANSCRIPT_REVIEW_REQUIRED");
        }
        clarificationManager.ensureNoPending(state);
        revisionManager.ensureNoPendingRevision(state);
    }

    private void validateAudio(byte[] audio, String contentType) {
        if (audio == null || audio.length == 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "AI_MESSAGE_INVALID");
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

    private void validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "AI_MESSAGE_INVALID");
        }
        if (text.length() > MAX_TRANSCRIPT_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, "AI_TRANSCRIPT_TOO_LARGE");
        }
    }

    private void ensureActiveVisit(UUID visitId) {
        var visit = visitService.getVisit(visitId);
        if (!"EN_COURS".equals(visit.getStatus())) {
            throw conflict("VISIT_NOT_ACTIVE");
        }
    }

    private String buildUserMessage(
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
                    org.springframework.http.HttpStatusCode.valueOf(422), "AI_OUTPUT_INVALID");
        }
    }

    private String clarificationModelText(
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

    private void mergeInitialDraft(
            Map<String, String> target,
            Map<String, String> source) {
        if (source == null) {
            return;
        }
        source.forEach((field, value) -> {
            if (AiClinicalResponseParser.ALLOWED_FIELDS.contains(field)
                    && value != null
                    && !value.isBlank()) {
                int maximum = field.equals("followUp") ? 1000 : 5000;
                target.put(field, limit(value.trim(), maximum));
            }
        });
    }

    private void trimProviderConversation(List<AiMessage> messages) {
        int maximum = Math.max(2, properties.maxConversationTurns() * 2);
        while (messages.size() > maximum) {
            messages.removeFirst();
        }
    }

    private void appendVisibleMessage(
            AiConsultationSessionState state,
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

    private SessionView toSessionView(
            UUID visitId,
            AiConsultationSessionState state) {
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
                List.copyOf(state.revisions),
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

    private String normalizeLocale(String locale) {
        return "en".equalsIgnoreCase(locale) ? "en" : "fr";
    }

    private String initialAssistantMessage(String locale) {
        return "en".equals(locale)
                ? "Hello doctor. I’m listening."
                : "Bonjour docteur. Je vous écoute.";
    }

    private String organizationKey(UUID organizationId) {
        return organizationId == null ? "GLOBAL" : organizationId.toString();
    }

    private String limit(String value, int maximum) {
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }

    private ResponseStatusException conflict(String reason) {
        return new ResponseStatusException(HttpStatus.CONFLICT, reason);
    }

    private record SessionKey(UUID visitId, UUID userId, String organizationId) {
    }
}
