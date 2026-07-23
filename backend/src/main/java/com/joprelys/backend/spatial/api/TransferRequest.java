package com.joprelys.backend.spatial.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TransferRequest(
        @NotNull UUID hospitalizationId,
        @NotNull UUID targetServiceUnitId,
        @NotNull UUID targetSpaceId,
        @NotNull UUID targetBedId
) {
}
