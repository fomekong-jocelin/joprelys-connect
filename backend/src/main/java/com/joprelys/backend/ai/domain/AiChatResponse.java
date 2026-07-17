package com.joprelys.backend.ai.domain;

/**
 * Réponse d'un modèle d'IA à une requête de chat (complétion).
 *
 * <p>Contient le contenu textuel de la réponse, le nombre de tokens utilisés
 * et le modèle ayant généré la réponse.</p>
 *
 * @param content    le contenu textuel de la réponse du modèle
 * @param tokensUsed le nombre total de tokens consommés (prompt + réponse), peut être null
 * @param model      l'identifiant du modèle utilisé pour la génération
 */
public record AiChatResponse(
        String content,
        Integer tokensUsed,
        String model
) {
}
