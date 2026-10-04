package com.joprelys.backend.patient.application;

/**
 * Generates the QR code displayed at reception to open the public pre-registration form.
 */
public interface GenerateAdmissionQrCodeUseCase {

    /**
     * Generates a PNG QR code pointing to the pre-registration form of the current tenant.
     *
     * @return PNG bytes
     */
    byte[] generate();
}
