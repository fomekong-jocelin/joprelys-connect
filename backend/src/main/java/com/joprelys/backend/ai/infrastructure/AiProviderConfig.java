package com.joprelys.backend.ai.infrastructure;

import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.infrastructure.claude.ClaudeProvider;
import com.joprelys.backend.ai.infrastructure.gemini.GeminiProvider;
import com.joprelys.backend.ai.infrastructure.openai.OpenAiProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration Spring du module IA multi-provider.
 *
 * <p>{@code joprelys.ai.speech-provider} sélectionne la transcription audio et
 * {@code joprelys.ai.provider} sélectionne la génération du brouillon clinique.
 * Les fournisseurs OpenAI, Gemini et Claude restent disponibles par configuration.</p>
 */
@Configuration
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
@EnableConfigurationProperties(AiProperties.class)
public class AiProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(AiProviderConfig.class);

    @Bean
    public AiProvider aiProvider(AiProperties properties) {
        String speechProviderName = normalizeProvider(properties.speechProvider(), "speech-provider");
        String draftProviderName = normalizeProvider(properties.provider(), "provider");

        validateSpeechConfiguration(speechProviderName, properties);

        Map<String, AiProvider> providers = new HashMap<>();
        AiProvider speechProvider = providers.computeIfAbsent(
                speechProviderName,
                provider -> buildProvider(provider, properties));
        AiProvider draftProvider = providers.computeIfAbsent(
                draftProviderName,
                provider -> buildProvider(provider, properties));

        log.info("Initialisation du routage IA : transcription={}, brouillon={}",
                speechProviderName, draftProviderName);
        return new RoutingAiProvider(speechProvider, draftProvider);
    }

    private AiProvider buildProvider(String provider, AiProperties properties) {
        return switch (provider) {
            case "openai" -> buildOpenAiProvider(properties);
            case "gemini" -> buildGeminiProvider(properties);
            case "claude" -> buildClaudeProvider(properties);
            default -> throw new IllegalArgumentException(
                    "Fournisseur IA non supporté : " + provider
                            + ". Valeurs acceptées : openai, gemini, claude.");
        };
    }

    private AiProvider buildOpenAiProvider(AiProperties properties) {
        AiProperties.OpenAiProperties config = requireOpenAiConfiguration(properties.openai());
        log.info("Configuration OpenAI chargée : brouillon={}, transcription={}",
                config.model(), config.transcribeModel());
        RestClient restClient = RestClient.builder()
                .baseUrl(config.baseUrl())
                .defaultHeader("Authorization", "Bearer " + config.apiKey())
                .build();
        return new OpenAiProvider(restClient, config);
    }

    private AiProvider buildGeminiProvider(AiProperties properties) {
        AiProperties.GeminiProperties config = requireGeminiConfiguration(properties.gemini());
        log.info("Configuration Gemini chargée : modèle={}", config.model());
        RestClient restClient = RestClient.builder()
                .baseUrl(config.baseUrl())
                .build();
        return new GeminiProvider(restClient, config);
    }

    private AiProvider buildClaudeProvider(AiProperties properties) {
        AiProperties.ClaudeProperties config = requireClaudeConfiguration(properties.claude());
        log.info("Configuration Claude chargée : modèle={}", config.model());
        RestClient claudeRestClient = RestClient.builder()
                .baseUrl(config.baseUrl())
                .defaultHeader("x-api-key", config.apiKey())
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("Content-Type", "application/json")
                .build();

        RestClient speechFallbackClient = buildOpenAiSpeechClient(properties.openai());
        return new ClaudeProvider(claudeRestClient, config, speechFallbackClient, properties.openai());
    }

    private RestClient buildOpenAiSpeechClient(AiProperties.OpenAiProperties config) {
        if (config == null || isBlank(config.apiKey())) {
            return null;
        }
        return RestClient.builder()
                .baseUrl(config.baseUrl())
                .defaultHeader("Authorization", "Bearer " + config.apiKey())
                .build();
    }

    private void validateSpeechConfiguration(String provider, AiProperties properties) {
        if ("claude".equals(provider)
                && (properties.openai() == null || isBlank(properties.openai().apiKey()))) {
            throw new IllegalStateException(
                    "SPEECH_PROVIDER=claude utilise la transcription OpenAI de secours. "
                            + "OPENAI_API_KEY doit être configurée.");
        }
    }

    private AiProperties.OpenAiProperties requireOpenAiConfiguration(
            AiProperties.OpenAiProperties config) {
        if (config == null || isBlank(config.apiKey()) || isBlank(config.model())
                || isBlank(config.transcribeModel()) || isBlank(config.baseUrl())) {
            throw new IllegalStateException(
                    "Configuration OpenAI incomplète : OPENAI_API_KEY, OPENAI_MODEL, "
                            + "OPENAI_TRANSCRIBE_MODEL et OPENAI_BASE_URL sont requis.");
        }
        return config;
    }

    private AiProperties.GeminiProperties requireGeminiConfiguration(
            AiProperties.GeminiProperties config) {
        if (config == null || isBlank(config.apiKey()) || isBlank(config.model())
                || isBlank(config.baseUrl())) {
            throw new IllegalStateException(
                    "Configuration Gemini incomplète : GEMINI_API_KEY, GEMINI_MODEL "
                            + "et GEMINI_BASE_URL sont requis.");
        }
        return config;
    }

    private AiProperties.ClaudeProperties requireClaudeConfiguration(
            AiProperties.ClaudeProperties config) {
        if (config == null || isBlank(config.apiKey()) || isBlank(config.model())
                || isBlank(config.baseUrl())) {
            throw new IllegalStateException(
                    "Configuration Claude incomplète : ANTHROPIC_API_KEY, CLAUDE_MODEL "
                            + "et ANTHROPIC_BASE_URL sont requis.");
        }
        return config;
    }

    private String normalizeProvider(String provider, String propertyName) {
        if (isBlank(provider)) {
            throw new IllegalStateException("joprelys.ai." + propertyName + " doit être configuré.");
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
