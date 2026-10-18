package com.joprelys.backend.auth.application;

import org.springframework.security.authentication.BadCredentialsException;

/**
 * Rejet d'un code de connexion du personnel, avec sa raison. L'étape OTP n'est atteinte qu'après un mot de passe
 * valide : la raison (code incorrect, expiré, trop de tentatives, aucune demande) ne révèle pas l'existence du compte.
 */
public class StaffOtpRejectedException extends BadCredentialsException {

    public enum Reason { OTP_INCORRECT, OTP_EXPIRED, OTP_TOO_MANY_ATTEMPTS, OTP_NO_ACTIVE_REQUEST }

    private final Reason reason;
    private final int remainingAttempts;

    public StaffOtpRejectedException(Reason reason, String message, int remainingAttempts) {
        super(message);
        this.reason = reason;
        this.remainingAttempts = remainingAttempts;
    }

    public Reason getReason() {
        return reason;
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }
}
