package com.joprelys.backend.billing.api;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateInvoiceRequest(
        @NotNull(message = "L'identifiant du patient est obligatoire.")
        UUID patientId,
        
        UUID visitId,
        
        UUID insuranceConventionId,
        
        List<InvoiceItemDto> items
) {}
