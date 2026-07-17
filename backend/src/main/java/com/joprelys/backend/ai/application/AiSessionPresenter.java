package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiConsultationContract.ConversationMessageView;
import com.joprelys.backend.ai.application.AiConsultationContract.MessageView;
import com.joprelys.backend.ai.application.AiConsultationContract.SessionView;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
final class AiSessionPresenter {

    private static final int MAX_VISIBLE_CONTENT_LENGTH = 12000;

    private final AiProperties properties;

    AiSessionPresenter(AiProperties properties) {
        this.properties = properties;
    }

    void appendVisibleMessage(
            AiConsultationSessionState state,
            String role,
            String content,
            String source,
            boolean needsClarification) {
        state.conversation.add(new ConversationMessageView(
                UUID.randomUUID(),
                role,
                limit(content == null ? "" : content.trim()),
                source,
                Instant.now(),
                needsClarification));
        int maximum = Math.max(5, properties.maxConversationTurns() * 2 + 1);
        while (state.conversation.size() > maximum) {
            state.conversation.removeFirst();
        }
    }

    MessageView toMessageView(
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

    SessionView toSessionView(
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

    private String limit(String value) {
        return value.length() <= MAX_VISIBLE_CONTENT_LENGTH
                ? value
                : value.substring(0, MAX_VISIBLE_CONTENT_LENGTH);
    }
}
