package com.joprelys.backend.billing.api;

import com.joprelys.backend.billing.application.BillingSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing/settings")
public class BillingSettingsController {

    private final BillingSettingsService settingsService;

    public BillingSettingsController(BillingSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('BILLING_SETTINGS_MANAGE', 'BILLING_INVOICE_READ')")
    @Operation(summary = "Paramètres financiers", description = "Paiement avant prélèvement et seuil d'approbation des avoirs.")
    public BillingSettingsDto get() {
        return settingsService.get();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('BILLING_SETTINGS_MANAGE')")
    @Operation(summary = "Modifier les paramètres financiers", description = "Réservé à la direction financière et à l'administration.")
    public BillingSettingsDto update(@Valid @RequestBody BillingSettingsDto request) {
        return settingsService.update(request);
    }
}
