package com.joprelys.backend.ai.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés de configuration du module IA.
 *
 * <p>Le fournisseur de transcription et le fournisseur de génération du brouillon
 * clinique sont sélectionnés indépendamment. Cela permet, par exemple, d'utiliser
 * OpenAI pour la transcription audio et Claude pour la structuration clinique,
 * tout en conservant Gemini comme alternative configurable.</p>
 *
 * @param enabled              active ou désactive le module IA
 * @param provider             fournisseur du brouillon clinique : openai, gemini ou claude
 * @param speechProvider       fournisseur de transcription : openai, gemini ou claude
 * @param sessionTtlMinutes    durée de vie des sessions de conversation en minutes
 * @param maxConversationTurns nombre maximum de tours de conversation par session
 * @param locale               langue par défaut de transcription et de réponse
 * @param openai               configuration spécifique à OpenAI
 * @param gemini               configuration spécifique à Gemini
 * @param claude               configuration spécifique à Claude/Anthropic
 */
@ConfigurationProperties(prefix = "joprelys.ai")
public record AiProperties(
        boolean enabled,
        String provider,
        String speechProvider,
        int sessionTtlMinutes,
        int maxConversationTurns,
        String locale,
        OpenAiProperties openai,
        GeminiProperties gemini,
        ClaudeProperties claude
) {

    /**
     * Configuration spécifique à OpenAI.
     *
     * @param apiKey           clé API OpenAI
     * @param model            modèle de génération du brouillon clinique
     * @param transcribeModel  modèle de transcription audio
     * @param transcribePrompt prompt de contexte transmis à la transcription
     *                         (améliore le vocabulaire médical français)
     * @param baseUrl          URL de base de l'API OpenAI
     */
    public record OpenAiProperties(
            String apiKey,
            String model,
            String transcribeModel,
            String transcribePrompt,
            String baseUrl
    ) {
    }

    /**
     * Configuration spécifique à Gemini.
     *
     * @param apiKey  clé API Gemini
     * @param model   modèle Gemini multimodal
     * @param baseUrl URL de base de l'API Gemini
     */
    public record GeminiProperties(
            String apiKey,
            String model,
            String baseUrl
    ) {
    }

    /**
     * Configuration spécifique à Claude/Anthropic.
     *
     * @param apiKey  clé API Anthropic
     * @param model   modèle Claude utilisé pour le brouillon clinique
     * @param baseUrl URL de base de l'API Anthropic
     */
    public record ClaudeProperties(
            String apiKey,
            String model,
            String baseUrl
    ) {
    }
}
