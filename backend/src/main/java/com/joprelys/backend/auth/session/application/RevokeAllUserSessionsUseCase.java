package com.joprelys.backend.auth.session.application;

import com.joprelys.backend.auth.session.domain.AuthSessionRevocationReason;
import java.util.UUID;

/**
 * Révocation système de toutes les sessions persistantes (refresh tokens et jetons d'accès
 * associés) d'un utilisateur, par exemple après une réinitialisation de mot de passe (SEC-16).
 */
public interface RevokeAllUserSessionsUseCase {

    /**
     * @return le nombre de sessions effectivement révoquées
     */
    int revokeAllForUser(UUID userId, AuthSessionRevocationReason reason);
}
