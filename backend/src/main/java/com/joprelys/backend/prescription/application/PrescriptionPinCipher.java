package com.joprelys.backend.prescription.application;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Chiffre le PIN d'ordonnance au repos (AES-256-GCM, SEC P0-3).
 *
 * <p>Le PIN reste nécessaire en clair à trois moments légitimes : l'impression de l'ordonnance, sa lecture par le
 * patient lui-même et la vérification par la pharmacie. Il est donc chiffré (réversible avec la clé) plutôt que
 * haché ; une copie de la base sans la clé ne révèle aucun PIN. La clé vient de {@code JOPRELYS_PRESCRIPTION_PIN_KEY}.
 */
@Component
public class PrescriptionPinCipher {

    static final String FORMAT_PREFIX = "v1:";
    private static final int MIN_SECRET_LENGTH = 32;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public PrescriptionPinCipher(@Value("${joprelys.security.prescription-pin-key:}") String secret) {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JOPRELYS_PRESCRIPTION_PIN_KEY doit être défini (au moins " + MIN_SECRET_LENGTH + " caractères).");
        }
        this.key = new SecretKeySpec(sha256(secret), "AES");
    }

    public String encrypt(String pin) {
        if (pin == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(pin.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
            return FORMAT_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Chiffrement du PIN impossible", e);
        }
    }

    /**
     * @throws IllegalStateException si la valeur a été altérée ou chiffrée avec une autre clé
     */
    public String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        if (!stored.startsWith(FORMAT_PREFIX)) {
            throw new IllegalStateException("Format de PIN chiffré inconnu");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(stored.substring(FORMAT_PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, payload, 0, IV_BYTES));
            byte[] plain = cipher.doFinal(payload, IV_BYTES, payload.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Déchiffrement du PIN impossible (clé différente ou donnée altérée)", e);
        }
    }

    private static byte[] sha256(String secret) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
