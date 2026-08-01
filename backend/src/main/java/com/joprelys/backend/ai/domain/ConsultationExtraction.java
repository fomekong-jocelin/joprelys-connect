package com.joprelys.backend.ai.domain;

import java.util.Map;

/**
 * Extraction structurée des champs de consultation à partir d'une réponse IA.
 *
 * <p>Cette structure contient les champs médicaux extraits par le modèle d'IA,
 * un message en langage naturel pour le médecin, et des indicateurs de clarification
 * lorsque l'information fournie est ambiguë ou incomplète.</p>
 *
 * @param extractedFields       les champs extraits sous forme de paires clé-valeur,
 *                              les clés possibles sont : symptoms, clinicalExam,
 *                              diagnosis, conclusion, advice, followUp
 * @param message               le message en langage naturel destiné au médecin
 * @param needsClarification    indique si l'IA a besoin de précisions supplémentaires
 * @param clarificationQuestion la question de clarification posée par l'IA, null si non applicable
 */
public record ConsultationExtraction(
        Map<String, String> extractedFields,
        String message,
        boolean needsClarification,
        String clarificationQuestion
) {

    /**
     * Crée une extraction vide avec un message d'erreur.
     *
     * @param errorMessage le message d'erreur à afficher
     * @return une extraction vide avec le message fourni
     */
    public static ConsultationExtraction error(String errorMessage) {
        return new ConsultationExtraction(Map.of(), errorMessage, false, null);
    }
}
