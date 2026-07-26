package com.joprelys.backend.ai.infrastructure;

import com.joprelys.backend.ai.domain.AiChatResponse;
import com.joprelys.backend.ai.domain.AiMessage;
import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.domain.AiTranscription;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Route les capacités IA vers des fournisseurs configurés indépendamment.
 *
 * <p>La transcription audio peut utiliser un fournisseur différent de celui qui
 * structure le brouillon clinique. Aucun fallback implicite n'est effectué par
 * ce routeur : le choix dépend exclusivement de la configuration.</p>
 */
public final class RoutingAiProvider implements AiProvider {

    private final AiProvider speechProvider;
    private final AiProvider draftProvider;

    public RoutingAiProvider(AiProvider speechProvider, AiProvider draftProvider) {
        this.speechProvider = Objects.requireNonNull(speechProvider, "speechProvider");
        this.draftProvider = Objects.requireNonNull(draftProvider, "draftProvider");
    }

    @Override
    public AiTranscription transcribeAudio(byte[] audioData, String mimeType, String locale) {
        return speechProvider.transcribeAudio(audioData, mimeType, locale);
    }

    @Override
    public AiChatResponse chat(List<AiMessage> messages, String systemPrompt) {
        return draftProvider.chat(messages, systemPrompt);
    }

    @Override
    public AiChatResponse chatStructured(
            List<AiMessage> messages,
            String systemPrompt,
            String schemaName,
            Map<String, Object> schema) {
        return draftProvider.chatStructured(messages, systemPrompt, schemaName, schema);
    }
}
