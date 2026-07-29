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
import java.util.concurrent.TimeUnit;
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
    private final AiClinicalFactualityGuard factualityGuard;
    private final AiClinicalGroundingGuard groundingGuard;
    private final AiMedicationSafetyGuard medicationSafetyGuard;
    private final AiRepeatedClarificationGuard repeatedClarificationGuard = new AiRepeatedClarificationGuard();
    private final AiClinicalMemoryManager memoryManager = new AiClinicalMemoryManager();
    private final AiClinicalToolDispatcher toolDispatcher = new AiClinicalToolDispatcher();
    private final AiContinuousCaptureMerge continuousCaptureMerge;
    private final AiRevisionManager revisionManager;
    private final AiClarificationManager clarificationManager;
    private final AiTranscriptionWorkflow transcriptionWorkflow;
    private final ConcurrentMap<SessionKey, AiConsultationSessionState> sessions = new ConcurrentHashMap<>();

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
        this.factualityGuard = new AiClinicalFactualityGuard(objectMapper);
        this.groundingGuard = new AiClinicalGroundingGuard(objectMapper);
        this.medicationSafetyGuard = new AiMedicationSafetyGuard(objectMapper);
        this.continuousCaptureMerge = new AiContinuousCaptureMerge(objectMapper);
        this.revisionManager = revisionManager;
        this.clarificationManager = clarificationManager;
        this.transcriptionWorkflow = new AiTranscriptionWorkflow(aiProvider, properties);
    }

    @Autowired(required = false)
    void setMedicationReferenceDuplicateDetector(MedicationReferenceDuplicateDetector detector) {
        this.medicationSafetyGuard.setReferenceDuplicateDetector(detector);
    }

    public SessionView startSession(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            Map<String, String> initialDraft) {
        return startSession(visitId, userId, organizationId, initialDraft, properties.locale());
    }

    public SessionView startSession(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            Map<String, String> initialDraft,
            String requestedLocale) {
        ensureActiveVisit(visitId);
        String locale = messageBuilder.normalizeLocale(requestedLocale);
        AiConsultationSessionState state = new AiConsultationSessionState(
                UUID.randomUUID(), visitId, expiry(), locale);
        AiConsultationSessionSupport.mergeInitialDraft(state.baselineDraft, initialDraft);
        state.draft.putAll(state.baselineDraft);
        memoryManager.synchronizeAcceptedDraft(state);
        state.assistantMessage = messageBuilder.initialAssistantMessage(locale);
        appendVisibleMessage(state, "ASSISTANT", state.assistantMessage, "SYSTEM", false);
        sessions.put(sessionKey(visitId, userId, organizationId), state);
        return toSessionView(visitId, state);
    }

    public Optional<SessionView> getSession(UUID visitId, UUID userId, UUID organizationId) {
        ensureActiveVisit(visitId);
        SessionKey key = sessionKey(visitId, userId, organizationId);
        AiConsultationSessionState state = sessions.get(key);
        if (state == null) return Optional.empty();
        synchronized (state) {
            if (state.expiresAt.isBefore(Instant.now())) {
                sessions.remove(key, state);
                return Optional.empty();
            }
            return Optional.of(toSessionView(visitId, state));
        }
    }

    public String sessionLocale(UUID visitId, UUID userId, UUID organizationId) {
        return requireSession(visitId, userId, organizationId).locale;
    }

    /** Interactive/manual input keeps the explicit decision workflow for compatibility. */
    public MessageView processText(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            String text) {
        AiConsultationInputValidator.validateText(text);
        AiConsultationSessionState state = requireSession(visitId, userId, organizationId);
        synchronized (state) {
            ensureReadyForInteractiveInput(state);
            return processMessageLocked(
                    state, text.trim(), text.trim(), null, "TEXT", null, null, false);
        }
    }

    /**
     * Continuous reconstruction input never leaves a clarification or field decision
     * pending. The live browser capture no longer calls this method per phrase: it
     * persists first and rebuilds later from the durable transcript.
     */
    public MessageView processRealtimeTranscript(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            String transcript,
            Double confidence) {
        AiConsultationInputValidator.validateText(transcript);
        validateRealtimeConfidence(confidence);
        AiConsultationSessionState state = requireSession(visitId, userId, organizationId);
        synchronized (state) {
            return processMessageLocked(
                    state,
                    transcript.trim(),
                    transcript.trim(),
                    transcript.trim(),
                    "REALTIME",
                    null,
                    null,
                    true);
        }
    }

    /**
     * Finalization/recovery extraction for the durable clinician-reviewed transcript.
     * Uses a compact non-conversational prompt and strict structured output when the
     * configured provider supports it. This prevents a long consultation from paying
     * the cost of the full conversational prompt for every rebuild chunk.
     */
    public MessageView processCaptureTranscript(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            String transcript) {
        AiConsultationInputValidator.validateText(transcript);
        AiConsultationSessionState state = requireSession(visitId, userId, organizationId);
        synchronized (state) {
            return processMessageLocked(
                    state,
                    transcript.trim(),
                    transcript.trim(),
                    transcript.trim(),
                    "CAPTURE",
                    null,
                    null,
                    true,
                    true);
        }
    }

    public MessageView answerClarification(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            UUID clarificationId,
            String answer) {
        AiConsultationInputValidator.validateText(answer);
        AiConsultationSessionState state = requireSession(visitId, userId, organizationId);
        synchronized (state) {
            revisionManager.ensureNoPendingRevision(state);
            ClarificationView clarification = clarificationManager.findPending(state, clarificationId);
            String originalUtterance = latestUserUtterance(state);
            String modelText = "Original clinician utterance:\n"
                    + originalUtterance
                    + "\n\n"
                    + messageBuilder.clarificationModelText(state.locale, clarification, answer.trim());
            return processMessageLocked(
                    state,
                    modelText,
                    answer.trim(),
                    null,
                    "CLARIFICATION",
                    clarificationId,
                    answer.trim(),
                    false);
        }
    }

    public TranscriptionView transcribeAudio(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            byte[] audio,
            String contentType) {
        AiConsultationInputValidator.validateAudio(audio, contentType);
        AiConsultationSessionState state = requireSession(visitId, userId, organizationId);
        synchronized (state) {
            if (state.pendingTranscript != null) {
                throw conflict("AI_TRANSCRIPT_REVIEW_REQUIRED");
            }
            return transcriptionWorkflow.transcribe(state, audio, contentType);
        }
    }

    /** Legacy/manual staging endpoint; the new capture path does not depend on it. */
    public TranscriptionView stageRealtimeTranscript(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            String transcript) {
        AiConsultationInputValidator.validateText(transcript);
        AiConsultationSessionState state = requireSession(visitId, userId, organizationId);
        synchronized (state) {
            if (state.pendingTranscript != null) {
                throw conflict("AI_TRANSCRIPT_REVIEW_REQUIRED");
            }
            return transcriptionWorkflow.stage(state, transcript);
        }
    }

    /**
     * Legacy/manual transcription review remains explicitly decision-gated. The new
     * progressive dictation path persists directly into the durable capture ledger
     * and never uses this endpoint between recording segments.
     */
    public MessageView analyzeTranscript(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            String transcript) {
        AiConsultationInputValidator.validateText(transcript);
        AiConsultationSessionState state = requireSession(visitId, userId, organizationId);
        synchronized (state) {
            if (state.pendingTranscript == null) {
                throw conflict("AI_TRANSCRIPT_REVIEW_REQUIRED");
            }
            MessageView response = processMessageLocked(
                    state,
                    transcript.trim(),
                    transcript.trim(),
                    transcript.trim(),
                    "AUDIO",
                    null,
                    null,
                    false);
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
        return analyzeTranscript(visitId, userId, organizationId, transcription.transcript());
    }

    public void discardPendingTranscript(UUID visitId, UUID userId, UUID organizationId) {
        AiConsultationSessionState state = requireSession(visitId, userId, organizationId);
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
        AiConsultationSessionState state = requireSession(visitId, userId, organizationId);
        synchronized (state) {
            revisionManager.decideProposal(state, revisionId, proposalId, decision);
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
        AiConsultationSessionState state = requireSession(visitId, userId, organizationId);
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
            String clarificationAnswer,
            boolean continuousCapture) {
        return processMessageLocked(
                state,
                modelText,
                visibleText,
                transcript,
                source,
                resolvedClarificationId,
                clarificationAnswer,
                continuousCapture,
                false);
    }

    private MessageView processMessageLocked(
            AiConsultationSessionState state,
            String modelText,
            String visibleText,
            String transcript,
            String source,
            UUID resolvedClarificationId,
            String clarificationAnswer,
            boolean continuousCapture,
            boolean captureExtraction) {
        ClarificationView resolvedClarification = resolvedClarificationId == null
                ? null
                : clarificationManager.findPending(state, resolvedClarificationId);
        String resolvedClarificationField = resolvedClarification == null
                ? null
                : resolvedClarification.field();
        String factualSource = "CLARIFICATION".equals(source)
                ? joinSources(latestUserUtterance(state), visibleText)
                : visibleText;
        Map<String, Object> clinicalContext = loadClinicalContext(state.visitId);

        List<AiMessage> providerMessages = new ArrayList<>();
        if (!captureExtraction) {
            state.providerMessages.stream()
                    .filter(message -> message.role() == AiMessage.Role.SYSTEM)
                    .forEach(providerMessages::add);
        }
        providerMessages.add(AiMessage.system(AiClinicalFidelityContract.SYSTEM_INSTRUCTION));
        String userMessage = captureExtraction
                ? messageBuilder.buildCaptureExtractionMessage(modelText, state.draft, state.locale)
                : messageBuilder.buildUserMessage(
                        "Input source: " + source + "\n" + modelText,
                        state.draft,
                        state.locale,
                        clinicalContext);
        providerMessages.add(AiMessage.user(userMessage));

        long modelStarted = System.nanoTime();
        try {
            AiChatResponse response = captureExtraction
                    ? captureModelResponse(List.copyOf(providerMessages))
                    : aiProvider.chat(List.copyOf(providerMessages), SYSTEM_PROMPT);
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - modelStarted);
            log.info(
                    "AI_USAGE provider={} operation={} durationMs={} inputChars={}",
                    properties.provider(),
                    captureExtraction ? "capture_rebuild" : "consultation_chat",
                    durationMs,
                    modelText.length());
            if (response == null || response.content() == null) {
                throw new ResponseStatusException(
                        org.springframework.http.HttpStatusCode.valueOf(422), "AI_OUTPUT_INVALID");
            }
            ParsedResponse rawParsed = responseParser.parse(response.content());
            ParsedResponse factChecked = factualityGuard.enforce(
                    rawParsed, factualSource, state.draft, source, state.locale);
            ParsedResponse grounded = groundingGuard.enforce(
                    factChecked, factualSource, resolvedClarificationField, state.locale);
            ParsedResponse medicationChecked = "prescription".equals(resolvedClarificationField)
                    ? grounded
                    : medicationSafetyGuard.enforce(grounded, clinicalContext, state.locale);
            ParsedResponse parsed = repeatedClarificationGuard.enforce(
                    medicationChecked, state, state.locale);
            var toolPlan = toolDispatcher.dispatch(parsed);

            if (resolvedClarificationId != null) {
                clarificationManager.resolve(state, resolvedClarificationId, clarificationAnswer);
            }

            var revisionChanges = continuousCapture
                    ? continuousCaptureMerge.merge(state.draft, toolPlan.changes())
                    : toolPlan.changes();
            RevisionView revision = null;
            if (!continuousCapture && toolPlan.clarification() != null) {
                clarificationManager.append(state, toolPlan.clarification());
            } else {
                // Continuous capture never lets an ambiguous item erase independent safe
                // facts from the same chunk. Clarifications stay a human review concern;
                // grounded changes are accumulated immediately in the working draft.
                revision = revisionManager.createRevision(state, revisionChanges);
                if (continuousCapture && revision != null && "PENDING".equals(revision.status())) {
                    RevisionView createdRevision = revision;
                    UUID acceptedRevisionId = createdRevision.id();
                    revisionManager.decideRevision(state, acceptedRevisionId, "ACCEPT");
                    revision = state.revisions.stream()
                            .filter(item -> item.id().equals(acceptedRevisionId))
                            .findFirst()
                            .orElse(createdRevision);
                }
            }

            List<String> changedFields = revision == null
                    ? List.of()
                    : revision.proposals().stream()
                            .map(AiConsultationContract.FieldProposalView::field)
                            .toList();
            state.assistantMessage = continuousCapture
                    ? captureMessage(changedFields.size(), state.locale)
                    : parsed.assistantMessage();
            state.needsClarification = !continuousCapture && toolPlan.clarification() != null;
            if (transcript != null) state.transcript = transcript;
            state.expiresAt = expiry();
            if (resolvedClarification != null) {
                memoryManager.recordResolvedClarification(state, resolvedClarification, clarificationAnswer);
            }
            appendVisibleMessage(state, "USER", visibleText, source, false);
            if (!continuousCapture) {
                appendVisibleMessage(
                        state, "ASSISTANT", state.assistantMessage, "AI", state.needsClarification);
            }
            return AiConsultationSessionSupport.toMessageView(state, changedFields);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Échec génération brouillon IA provider={}", properties.provider());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE");
        }
    }

    private AiChatResponse captureModelResponse(List<AiMessage> providerMessages) {
        try {
            return aiProvider.chatStructured(
                    providerMessages,
                    AiClinicalCapturePrompt.SYSTEM_PROMPT,
                    AiClinicalCapturePrompt.SCHEMA_NAME,
                    AiClinicalCapturePrompt.schema());
        } catch (UnsupportedOperationException unsupported) {
            log.debug("Structured capture output unavailable for provider={}, falling back to strict JSON chat", properties.provider());
            return aiProvider.chat(providerMessages, AiClinicalCapturePrompt.SYSTEM_PROMPT);
        }
    }

    /**
     * Explicit direct realtime-analysis calls keep the legacy confidence contract.
     * The P0 capture path does not use this as a discard gate: low-confidence audio
     * is first stored in RealtimeClinicalIntakeService and later rebuilt with a null
     * confidence after clinician review.
     */
    private void validateRealtimeConfidence(Double confidence) {
        if (confidence == null) {
            return;
        }
        if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "AI_REALTIME_TRANSCRIPTION_UNVERIFIED");
        }
        double minimum = properties.minimumTranscriptionConfidence();
        if (minimum > 0.0 && confidence < minimum) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "AI_TRANSCRIPTION_LOW_CONFIDENCE");
        }
    }

    private String captureMessage(int changedFieldCount, String locale) {
        if ("en".equalsIgnoreCase(locale)) {
            return changedFieldCount == 0
                    ? "Transcript preserved. No safe structured change was added from this segment."
                    : "Transcript preserved and the working draft was updated.";
        }
        return changedFieldCount == 0
                ? "Transcription conservée. Aucun changement structuré sûr n'a été ajouté pour ce passage."
                : "Transcription conservée et brouillon de travail mis à jour.";
    }

    private String latestUserUtterance(AiConsultationSessionState state) {
        for (int index = state.conversation.size() - 1; index >= 0; index--) {
            var message = state.conversation.get(index);
            if ("USER".equals(message.role()) && message.content() != null && !message.content().isBlank()) {
                return message.content().trim();
            }
        }
        return "";
    }

    private String joinSources(String first, String second) {
        String left = first == null ? "" : first.trim();
        String right = second == null ? "" : second.trim();
        if (left.isBlank()) return right;
        if (right.isBlank()) return left;
        return left + "\n" + right;
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

    private AiConsultationSessionState requireSession(UUID visitId, UUID userId, UUID organizationId) {
        ensureActiveVisit(visitId);
        SessionKey key = sessionKey(visitId, userId, organizationId);
        AiConsultationSessionState state = sessions.get(key);
        if (state == null) throw conflict("AI_SESSION_EXPIRED");
        synchronized (state) {
            if (state.expiresAt.isBefore(Instant.now())) {
                sessions.remove(key, state);
                throw conflict("AI_SESSION_EXPIRED");
            }
        }
        return state;
    }

    private void ensureReadyForInteractiveInput(AiConsultationSessionState state) {
        if (state.pendingTranscript != null) throw conflict("AI_TRANSCRIPT_REVIEW_REQUIRED");
        clarificationManager.ensureNoPending(state);
        revisionManager.ensureNoPendingRevision(state);
    }

    private void ensureActiveVisit(UUID visitId) {
        var visit = visitService.getVisit(visitId);
        if (!"EN_COURS".equals(visit.getStatus())) throw conflict("VISIT_NOT_ACTIVE");
    }

    private void appendVisibleMessage(
            AiConsultationSessionState state,
            String role,
            String content,
            String source,
            boolean needsClarification) {
        AiConsultationSessionSupport.appendVisibleMessage(
                state, role, content, source, needsClarification, properties.maxConversationTurns());
    }

    private SessionView toSessionView(UUID visitId, AiConsultationSessionState state) {
        return AiConsultationSessionSupport.toSessionView(visitId, state);
    }

    private SessionKey sessionKey(UUID visitId, UUID userId, UUID organizationId) {
        return new SessionKey(visitId, userId, organizationKey(organizationId));
    }

    private Instant expiry() {
        long seconds = Math.max(5, properties.sessionTtlMinutes()) * 60L;
        return Instant.now().plusSeconds(seconds);
    }

    private String organizationKey(UUID organizationId) {
        return organizationId == null ? "GLOBAL" : organizationId.toString();
    }

    private ResponseStatusException conflict(String reason) {
        return new ResponseStatusException(HttpStatus.CONFLICT, reason);
    }

    private record SessionKey(UUID visitId, UUID userId, String organizationId) {
    }
}
