package com.joprelys.backend.spatial.api;

import java.util.UUID;

public record TransferRequest(
        UUID hospitalizationId,
        UUID newBedId
) {
}
