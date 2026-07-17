package com.joprelys.backend.ai.domain;

/**
 * Résultat d'une transcription audio (Speech-to-Text).
 *
 * <p>Contient le texte transcrit, la langue détectée et un indice de confiance
 * retourné par le fournisseur d'IA.</p>
 *
 * @param text       le texte transcrit à partir de l'audio
 * @param locale     la langue détectée ou utilisée pour la transcription (ex. "fr")
 * @param confidence l'indice de confiance de la transcription (0.0 à 1.0), peut être null
 *                   si le fournisseur ne le supporte pas
 */
public record AiTranscription(
        String text,
        String locale,
        Double confidence
) {
}
