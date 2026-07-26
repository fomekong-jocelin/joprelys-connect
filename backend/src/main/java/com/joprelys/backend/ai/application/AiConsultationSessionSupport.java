package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiConsultationContract.ConversationMessageView;
import com.joprelys.backend.ai.application.AiConsultationContract.MessageView;
import com.joprelys.backend.ai.application.AiConsultationContract.SessionView;
import com.joprelys.backend.ai.domain.AiMessage;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class AiConsultationSessionSupport {

    private static final int MAX_TRANSCRIPT_LENGTH = 12000;

    private AiConsultationSessionSupport() {
    }

    static void mergeInitialDraft(
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

    static void trimProviderConversation(
            List<AiMessage> messages,
            int maxConversationTurns) {
        int maximum = Math.max(2, maxConversationTurns * 2);
        while (messages.size() > maximum) {
            messages.removeFirst();
        }
    }

    static void appendVisibleMessage(
            AiConsultationSessionState state,
            String role,
            String content,
            String source,
            boolean needsClarification,
            int maxConversationTurns) {
        state.conversation.add(new ConversationMessageView(
                UUID.randomUUID(),
                role,
                limit(content == null ? "" : content.trim(), MAX_TRANSCRIPT_LENGTH),
                source,
                Instant.now(),
                needsClarification));
        int maximum = Math.max(5, maxConversationTurns * 2 + 1);
        while (state.conversation.size() > maximum) {
            state.conversation.removeFirst();
        }
    }

    static SessionView toSessionView(
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

    static MessageView toMessageView(
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

    private static String limit(String value, int maximum) {
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }
}
