package com.joprelys.backend.ai.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés de configuration pour le module IA.
 *
 * <p>Liées au préfixe {@code joprelys.ai} dans {@code application.yml}.
 * Permet de configurer le fournisseur actif, les clés API, les modèles
 * et les paramètres de session.</p>
 *
 * @param enabled              active ou désactive le module IA
 * @param provider             le fournisseur à utiliser : "openai", "gemini" ou "claude"
 * @param sessionTtlMinutes    durée de vie des sessions de conversation en minutes
 * @param maxConversationTurns nombre maximum de tours de conversation par session
 * @param locale               la langue par défaut pour les transcriptions et réponses
 * @param openai               configuration spécifique à OpenAI
 * @param gemini               configuration spécifique à Gemini
 * @param claude               configuration spécifique à Claude/Anthropic
 */
@ConfigurationProperties(prefix = "joprelys.ai")
public record AiProperties(
        boolean enabled,
        String provider,
        int sessionTtlMinutes,
        int maxConversationTurns,
        String locale,
        OpenAiProperties openai,
        GeminiProperties gemini,
        ClaudeProperties claude
) {

    /**
     * Configuration spécifique au fournisseur OpenAI.
     *
     * @param apiKey       la clé API OpenAI
     * @param model        le modèle de chat (ex. "gpt-4o")
     * @param whisperModel le modèle Whisper pour la transcription (ex. "whisper-1")
     * @param baseUrl      l'URL de base de l'API OpenAI
     */
    public record OpenAiProperties(
            String apiKey,
            String model,
            String whisperModel,
            String baseUrl
    ) {
    }

    /**
     * Configuration spécifique au fournisseur Gemini.
     *
     * @param apiKey  la clé API Gemini
     * @param model   le modèle Gemini à utiliser (ex. "gemini-2.5-flash")
     * @param baseUrl l'URL de base de l'API Gemini
     */
    public record GeminiProperties(
            String apiKey,
            String model,
            String baseUrl
    ) {
    }

    /**
     * Configuration spécifique au fournisseur Claude/Anthropic.
     *
     * @param apiKey  la clé API Anthropic
     * @param model   le modèle Claude à utiliser (ex. "claude-sonnet-4-20250514")
     * @param baseUrl l'URL de base de l'API Anthropic
     */
    public record ClaudeProperties(
            String apiKey,
            String model,
            String baseUrl
    ) {
    }
}
