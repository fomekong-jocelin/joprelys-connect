package com.joprelys.backend.ai.domain;

import java.util.List;
import java.util.Map;

/**
 * Interface stratégie pour les fournisseurs d'IA.
 *
 * <p>Définit le contrat commun pour la transcription audio (Speech-to-Text)
 * et la complétion de chat. Chaque fournisseur (OpenAI, Gemini, Claude)
 * implémente cette interface avec sa propre logique d'appel API.</p>
 *
 * <p>Les implémentations sont activées conditionnellement via
 * {@code @ConditionalOnProperty} selon la propriété {@code joprelys.ai.provider}.</p>
 */
public interface AiProvider {

    /**
     * Transcrit un fichier audio en texte.
     *
     * @param audioData les données audio brutes sous forme de tableau d'octets
     * @param mimeType  le type MIME du fichier audio (ex. "audio/webm", "audio/wav")
     * @param locale    la langue attendue de l'audio (ex. "fr")
     * @return le résultat de la transcription contenant le texte, la langue et la confiance
     * @throws UnsupportedOperationException si le fournisseur ne supporte pas la transcription
     */
    AiTranscription transcribeAudio(byte[] audioData, String mimeType, String locale);

    /**
     * Envoie une liste de messages à un modèle de chat et retourne sa réponse.
     *
     * @param messages     l'historique de conversation sous forme de liste de messages
     * @param systemPrompt le prompt système définissant le comportement de l'assistant
     * @return la réponse du modèle contenant le contenu, les tokens et le modèle utilisé
     */
    AiChatResponse chat(List<AiMessage> messages, String systemPrompt);

    /**
     * Demande une sortie strictement conforme à un JSON Schema.
     *
     * <p>Cette capacité est volontairement fail-closed : un fournisseur qui ne
     * garantit pas l'adhérence au schéma ne doit pas être utilisé pour extraire
     * des faits cliniques.</p>
     */
    default AiChatResponse chatStructured(
            List<AiMessage> messages,
            String systemPrompt,
            String schemaName,
            Map<String, Object> schema) {
        throw new UnsupportedOperationException("AI_STRUCTURED_OUTPUT_UNSUPPORTED");
    }
}
