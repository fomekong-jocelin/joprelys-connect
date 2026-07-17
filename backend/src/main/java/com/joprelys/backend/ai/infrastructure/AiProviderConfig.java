package com.joprelys.backend.ai.infrastructure;

import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.infrastructure.claude.ClaudeProvider;
import com.joprelys.backend.ai.infrastructure.gemini.GeminiProvider;
import com.joprelys.backend.ai.infrastructure.openai.OpenAiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration Spring pour le module IA.
 *
 * <p>Crée le bean {@link AiProvider} approprié en fonction de la propriété
 * {@code joprelys.ai.provider}. L'ensemble du module n'est activé que si
 * {@code joprelys.ai.enabled} vaut {@code true}.</p>
 *
 * <p>Trois fournisseurs sont supportés :</p>
 * <ul>
 *   <li>{@code openai} – OpenAI (GPT + Whisper)</li>
 *   <li>{@code gemini} – Google Gemini (multimodal)</li>
 *   <li>{@code claude} – Anthropic Claude (avec fallback Whisper pour le STT)</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
@EnableConfigurationProperties(AiProperties.class)
public class AiProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(AiProviderConfig.class);

    /**
     * Crée le fournisseur OpenAI utilisant GPT pour le chat et Whisper pour le STT.
     *
     * @param properties la configuration AI
     * @return une instance de {@link OpenAiProvider}
     */
    @Bean
    @ConditionalOnProperty(name = "joprelys.ai.provider", havingValue = "openai")
    public AiProvider openAiProvider(AiProperties properties) {
        log.info("Initialisation du fournisseur IA : OpenAI (modèle chat={}, whisper={})",
                properties.openai().model(), properties.openai().whisperModel());
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.openai().baseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.openai().apiKey())
                .build();
        return new OpenAiProvider(restClient, properties.openai());
    }

    /**
     * Crée le fournisseur Gemini utilisant l'API Google Generative AI.
     *
     * @param properties la configuration AI
     * @return une instance de {@link GeminiProvider}
     */
    @Bean
    @ConditionalOnProperty(name = "joprelys.ai.provider", havingValue = "gemini")
    public AiProvider geminiProvider(AiProperties properties) {
        log.info("Initialisation du fournisseur IA : Gemini (modèle={})", properties.gemini().model());
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.gemini().baseUrl())
                .build();
        return new GeminiProvider(restClient, properties.gemini());
    }

    /**
     * Crée le fournisseur Claude utilisant l'API Anthropic Messages.
     *
     * <p>Si une clé API OpenAI est également configurée, Whisper sera utilisé
     * comme fallback pour la transcription audio (Claude ne possède pas de STT natif).</p>
     *
     * @param properties la configuration AI
     * @return une instance de {@link ClaudeProvider}
     */
    @Bean
    @ConditionalOnProperty(name = "joprelys.ai.provider", havingValue = "claude")
    public AiProvider claudeProvider(AiProperties properties) {
        log.info("Initialisation du fournisseur IA : Claude (modèle={})", properties.claude().model());
        RestClient claudeRestClient = RestClient.builder()
                .baseUrl(properties.claude().baseUrl())
                .defaultHeader("x-api-key", properties.claude().apiKey())
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("Content-Type", "application/json")
                .build();

        RestClient whisperFallbackClient = buildWhisperFallbackClient(properties);
        return new ClaudeProvider(claudeRestClient, properties.claude(), whisperFallbackClient, properties.openai());
    }

    /**
     * Construit un RestClient pour Whisper si une clé API OpenAI est disponible.
     */
    private RestClient buildWhisperFallbackClient(AiProperties properties) {
        if (properties.openai() != null
                && properties.openai().apiKey() != null
                && !properties.openai().apiKey().isBlank()) {
            log.info("Fallback Whisper activé pour la transcription audio avec Claude");
            return RestClient.builder()
                    .baseUrl(properties.openai().baseUrl())
                    .defaultHeader("Authorization", "Bearer " + properties.openai().apiKey())
                    .build();
        }
        log.warn("Aucune clé API OpenAI configurée : la transcription audio ne sera pas disponible avec Claude");
        return null;
    }
}
