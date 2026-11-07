package com.joprelys.backend.eco.application;

import com.joprelys.backend.eco.domain.RevokedBy;
import java.util.Optional;
import java.util.UUID;

public record RevokeConsentCommand(EcoCaller caller, UUID consentId, RevokedBy revokedBy, Optional<String> reason) {
}
