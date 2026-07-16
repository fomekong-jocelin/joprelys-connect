package com.joprelys.backend.notification.application;

public interface AccountMailService {
    void sendTemporaryPassword(String recipient, String displayName, String temporaryPassword);

    void sendLoginCode(String recipient, String displayName, String code);

    void sendPasswordRecoveryCode(String recipient, String displayName, String code);

    void sendPatientLoginCode(String recipient, String displayName, String code);
}
