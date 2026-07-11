package com.joprelys.backend.emergency.medicolegal.api;

import com.joprelys.backend.emergency.medicolegal.domain.EmergencyBelongingTransferAction;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record TransferEmergencyBelongingRequest(
        @NotNull(message = "EMERGENCY_BELONGING_ACTION_REQUIRED")
        EmergencyBelongingTransferAction action,

        @Size(max = 160, message = "EMERGENCY_BELONGING_CUSTODIAN_TOO_LONG")
        String fromCustodian,

        @Size(max = 160, message = "EMERGENCY_BELONGING_RECIPIENT_TOO_LONG")
        String recipientName,

        @Size(max = 120, message = "EMERGENCY_BELONGING_RECIPIENT_ID_TOO_LONG")
        String recipientIdDocument,

        @Size(max = 1000, message = "EMERGENCY_BELONGING_NOTES_TOO_LONG")
        String notes,

        Instant occurredAt) {

    @AssertTrue(message = "EMERGENCY_BELONGING_RECIPIENT_IDENTITY_REQUIRED")
    public boolean isRecipientIdentityCompleteWhenCustodyEnds() {
        if (action != EmergencyBelongingTransferAction.RELEASED
                && action != EmergencyBelongingTransferAction.RETURNED) {
            return true;
        }
        return hasText(recipientName) && hasText(recipientIdDocument);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
