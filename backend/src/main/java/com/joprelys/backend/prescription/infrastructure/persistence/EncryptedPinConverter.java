package com.joprelys.backend.prescription.infrastructure.persistence;

import com.joprelys.backend.prescription.application.PrescriptionPinCipher;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/** Chiffre le PIN d'ordonnance à l'écriture en base et le déchiffre à la lecture. */
@Component
@Converter
public class EncryptedPinConverter implements AttributeConverter<String, String> {

    private final PrescriptionPinCipher cipher;

    public EncryptedPinConverter(PrescriptionPinCipher cipher) {
        this.cipher = cipher;
    }

    @Override
    public String convertToDatabaseColumn(String pin) {
        return cipher.encrypt(pin);
    }

    @Override
    public String convertToEntityAttribute(String stored) {
        return cipher.decrypt(stored);
    }
}
