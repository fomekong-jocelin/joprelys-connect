package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiConsultationContract.ClarificationView;
import com.joprelys.backend.ai.application.AiConsultationContract.ConversationMessageView;
import com.joprelys.backend.ai.application.AiConsultationContract.RevisionView;
import com.joprelys.backend.ai.domain.AiMessage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class AiConsultationSessionState {

    final UUID sessionId;
    final UUID visitId;
    final String locale;
    final Map<String, String> draft = new LinkedHashMap<>();
    final List<AiMessage> providerMessages = new ArrayList<>();
    final List<ConversationMessageView> conversation = new ArrayList<>();
    final List<ClarificationView> clarifications = new ArrayList<>();
    final List<RevisionView> revisions = new ArrayList<>();
    final AiClinicalConversationMemory clinicalMemory = new AiClinicalConversationMemory();

    Instant expiresAt;
    String transcript;
    String pendingTranscript;
    String transcriptStatus = "NONE";
    String assistantMessage;
    boolean needsClarification;
    int nextRevisionSequence = 1;

    AiConsultationSessionState(
            UUID sessionId,
            UUID visitId,
            Instant expiresAt,
            String locale) {
        this.sessionId = sessionId;
        this.visitId = visitId;
        this.expiresAt = expiresAt;
        this.locale = locale;
    }
}
