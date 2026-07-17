package com.joprelys.backend.visit.application;

import java.util.UUID;

/**
 * Generates a QR code that opens the consultation page of an active visit.
 */
public interface GenerateVisitConsultationQrCodeUseCase {

    /**
     * Generates a PNG QR code for a visit visible in the current tenant.
     *
     * @param visitId visit identifier
     * @return PNG bytes
     */
    byte[] generate(UUID visitId);
}
