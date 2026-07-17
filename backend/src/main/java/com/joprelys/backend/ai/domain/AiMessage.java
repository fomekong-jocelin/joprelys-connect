package com.joprelys.backend.ai.domain;

/**
 * Représente un message dans une conversation avec un modèle d'IA.
 *
 * <p>Chaque message possède un rôle (système, utilisateur ou assistant)
 * et un contenu textuel. Cette structure est commune à tous les fournisseurs d'IA.</p>
 *
 * @param role    le rôle de l'émetteur du message
 * @param content le contenu textuel du message
 */
public record AiMessage(
        Role role,
        String content
) {

    /**
     * Rôles possibles dans une conversation IA.
     */
    public enum Role {

        /** Message système définissant le comportement de l'assistant. */
        SYSTEM,

        /** Message envoyé par l'utilisateur (médecin). */
        USER,

        /** Message généré par l'assistant IA. */
        ASSISTANT
    }

    /**
     * Crée un message système.
     *
     * @param content le contenu du prompt système
     * @return un nouveau message avec le rôle SYSTEM
     */
    public static AiMessage system(String content) {
        return new AiMessage(Role.SYSTEM, content);
    }

    /**
     * Crée un message utilisateur.
     *
     * @param content le contenu du message utilisateur
     * @return un nouveau message avec le rôle USER
     */
    public static AiMessage user(String content) {
        return new AiMessage(Role.USER, content);
    }

    /**
     * Crée un message assistant.
     *
     * @param content le contenu de la réponse de l'assistant
     * @return un nouveau message avec le rôle ASSISTANT
     */
    public static AiMessage assistant(String content) {
        return new AiMessage(Role.ASSISTANT, content);
    }
}
