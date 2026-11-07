package com.joprelys.backend.patient.application;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Signe le QR d'accès patient (HMAC-SHA256, SEC P1) : un QR forgé ou dont la date de validité a été modifiée
 * est rejeté à la lecture. La clé vient de {@code JOPRELYS_PATIENT_QR_KEY}, distincte de celle des PIN.
 */
@Component
public class PatientAccessQrSigner {

    static final String TYPE = "JOPRELYS_PATIENT_ACCESS";
    private static final int MIN_SECRET_LENGTH = 32;
    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public PatientAccessQrSigner(@Value("${joprelys.security.patient-qr-key:}") String secret) {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JOPRELYS_PATIENT_QR_KEY doit être défini (au moins " + MIN_SECRET_LENGTH + " caractères).");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    /** Contenu JSON du QR, signature comprise. */
    public String payload(String globalPatientNumber, Instant validUntil) {
        return "{\"type\":\"" + TYPE + "\",\"globalPatientNumber\":\"" + globalPatientNumber
                + "\",\"validUntil\":\"" + validUntil + "\",\"sig\":\"" + sign(globalPatientNumber, validUntil) + "\"}";
    }

    /** Vrai seulement si la signature correspond et que la date de validité n'est pas dépassée. */
    public boolean isValid(String globalPatientNumber, Instant validUntil, String signature, Instant now) {
        if (globalPatientNumber == null || validUntil == null || signature == null || now.isAfter(validUntil)) {
            return false;
        }
        byte[] expected = sign(globalPatientNumber, validUntil).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.US_ASCII));
    }

    String sign(String globalPatientNumber, Instant validUntil) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            byte[] digest = mac.doFinal((TYPE + "|" + globalPatientNumber + "|" + validUntil)
                    .getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Signature du QR impossible", e);
        }
    }
}
