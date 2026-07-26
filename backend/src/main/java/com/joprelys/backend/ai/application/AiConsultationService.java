package com.joprelys.backend.ai.application;

import static com.joprelys.backend.ai.application.AiConsultationPrompt.SYSTEM_PROMPT;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedResponse;
import com.joprelys.backend.ai.application.AiConsultationContract.ClarificationView;
import com.joprelys.backend.ai.application.AiConsultationContract.MessageView;
import com.joprelys.backend.ai.application.AiConsultationContract.RevisionView;
import com.joprelys.backend.ai.application.AiConsultationContract.SessionView;
import com.joprelys.backend.ai.application.AiConsultationContract.TranscriptionView;
import com.joprelys.backend.ai.domain.AiChatResponse;
import com.joprelys.backend.ai.domain.AiMessage;
import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import com.joprelys.backend.medication.reference.MedicationReferenceDuplicateDetector;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class AiConsultationService {

    private static final Logger log = LoggerFactory.getLogger(AiConsultationService.class);

    private final AiProvider aiProvider;
    private final AiProperties properties;
    private final VisitService visitService;
    private final AiConsultationMessageBuilder messageBuilder;
    private final AiClinicalResponseParser responseParser;
    private final ClinicalContextAssembler clinicalContextAssembler;
    private final AiClinicalGroundingGuard groundingGuard;
    private final AiMedicationSafetyGuard medicationSafetyGuard;
    private final AiRepeatedClarificationGuard repeatedClarificationGuard =
            new AiRepeatedClarificationGuard();
    private final AiClinicalMemoryManager memoryManager = new AiClinicalMemoryManager();
    private final AiClinicalToolDispatcher toolDispatcher = new AiClinicalToolDispatcher();
    private final AiRevisionManager revisionManager;
    private final AiClarificationManager clarificationManager;
    private final AiTranscriptionWorkflow transcriptionWorkflow;
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
        this.messageBuilder = new AiConsultationMessageBuilder(objectMapper);
        this.responseParser = responseParser;
        this.clinicalContextAssembler = clinicalContextAssembler;
        this.groundingGuard = new AiClinicalGroundingGuard(objectMapper);
        this.medicationSafetyGuard = new AiMedicationSafetyGuard(objectMapper);
        this.revisionManager = revisionManager;
        this.clarificationManager = clarificationManager;
        this.transcriptionWorkflow = new AiTranscriptionWorkflow(aiProvider, properties);
    }

    @Autowired(required = false)
    void setMedicationReferenceDuplicateDetector(
            MedicationReferenceDuplicateDetector referenceDuplicateDetector) {
        this.medicationSafetyGuard.setReferenceDuplicateDetector(referenceDuplicateDetector);
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
        String locale = messageBuilder.normalizeLocale(requestedLocale);
        SessionKey key = sessionKey(visitId, userId, organizationId);
        AiConsultationSessionState state = new AiConsultationSessionState(
                UUID.randomUUID(), visitId, expiry(), locale);
        AiConsultationSessionSupport.mergeInitialDraft(state.draft, initialDraft);
        memoryManager.synchronizeAcceptedDraft(state);
        state.assistantMessage = messageBuilder.initialAssistantMessage(locale);
        appendVisibleMessage(state, "ASSISTANT", state.assistantMessage, "SYSTEM", false);
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
        AiConsultationInputValidator.validateText(text);
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
        AiConsultationInputValidator.validateText(answer);
        AiConsultationSessionState state = requireSession(
                visitId, userId, organizationId);
        synchronized (state) {
            revisionManager.ensureNoPendingRevision(state);
            ClarificationView clarification = clarificationManager.findPending(
                    state, clarificationId);
            String modelText = messageBuilder.clarificationModelText(
                    state.locale, clarification, answer.trim());
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
        AiConsultationInputValidator.validateAudio(audio, contentType);
        AiConsultationSessionState state = requireSession(
                visitId, userId, organizationId);
        synchronized (state) {
            ensureReadyForNewInput(state);
            return transcriptionWorkflow.transcribe(state, audio, contentType);
        }
    }

    public TranscriptionView stageRealtimeTranscript(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            String transcript) {
        AiConsultationInputValidator.validateText(transcript);
        AiConsultationSessionState state = requireSession(
                visitId, userId, organizationId);
        synchronized (state) {
            ensureReadyForNewInput(state);
            return transcriptionWorkflow.stage(state, transcript);
        }
    }

    public MessageView analyzeTranscript(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            String transcript) {
        AiConsultationInputValidator.validateText(transcript);
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
        ClarificationView resolvedClarification = resolvedClarificationId == null
                ? null
                : clarificationManager.findPending(state, resolvedClarificationId);
        String resolvedClarificationField = resolvedClarification == null
                ? null
                : resolvedClarification.field();
        Map<String, Object> clinicalContext = loadClinicalContext(state.visitId);
        List<AiMessage> providerMessages = new ArrayList<>(state.providerMessages);
        providerMessages.add(AiMessage.user(messageBuilder.buildUserMessage(
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
            ParsedResponse medicationChecked = "prescription".equals(resolvedClarificationField)
                    ? grounded
                    : medicationSafetyGuard.enforce(grounded, clinicalContext, state.locale);
            ParsedResponse parsed = repeatedClarificationGuard.enforce(
                    medicationChecked, state, state.locale);
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
            if (resolvedClarification != null) {
                memoryManager.recordResolvedClarification(
                        state, resolvedClarification, clarificationAnswer);
            }
            appendVisibleMessage(state, "USER", visibleText, source, false);
            appendVisibleMessage(
                    state,
                    "ASSISTANT",
                    state.assistantMessage,
                    "AI",
                    state.needsClarification);
            return AiConsultationSessionSupport.toMessageView(state, changedFields);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Échec génération brouillon IA provider={}", properties.provider());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE");
        }
    }

    private Map<String, Object> loadClinicalContext(UUID visitId) {
        try {
            return clinicalContextAssembler.assemble(visitId);
        } catch (RuntimeException exception) {
            log.error("Contexte clinique indisponible visitId={}", visitId, exception);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "AI_CLINICAL_CONTEXT_UNAVAILABLE");
        }
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

    private void ensureActiveVisit(UUID visitId) {
        var visit = visitService.getVisit(visitId);
        if (!"EN_COURS".equals(visit.getStatus())) {
            throw conflict("VISIT_NOT_ACTIVE");
        }
    }

    private void trimProviderConversation(List<AiMessage> messages) {
        AiConsultationSessionSupport.trimProviderConversation(
                messages, properties.maxConversationTurns());
    }

    private void appendVisibleMessage(
            AiConsultationSessionState state,
            String role,
            String content,
            String source,
            boolean needsClarification) {
        AiConsultationSessionSupport.appendVisibleMessage(
                state,
                role,
                content,
                source,
                needsClarification,
                properties.maxConversationTurns());
    }

    private SessionView toSessionView(
            UUID visitId,
            AiConsultationSessionState state) {
        return AiConsultationSessionSupport.toSessionView(visitId, state);
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
