package com.joprelys.backend.ai.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AiConsultationContract {

    private AiConsultationContract() {
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

    public record FieldProposalView(
            UUID id,
            String field,
            String operation,
            String previousValue,
            String proposedValue,
            String reason,
            String uncertainty,
            String status,
            Instant createdAt,
            Instant decidedAt) {
    }

    public record RevisionView(
            UUID id,
            int sequence,
            String status,
            Instant createdAt,
            List<FieldProposalView> proposals) {
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
            List<RevisionView> revisions,
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
            List<RevisionView> revisions,
            Instant expiresAt) {
    }
}
