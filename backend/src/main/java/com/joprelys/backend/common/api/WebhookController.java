package com.joprelys.backend.common.api;

import com.joprelys.backend.common.application.WebhookService;
import com.joprelys.backend.common.infrastructure.persistence.WebhookEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST pour la gestion des webhooks.
 * Réservé aux ADMIN_CLINIQUE et ADMIN_JOPRELYS.
 */
@RestController
@RequestMapping("/api/webhooks")
@PreAuthorize("hasAnyRole('ADMIN_CLINIQUE', 'ADMIN_JOPRELYS')")
public class WebhookController {

    private final WebhookService webhookService;
    private final com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository userAccountRepository;

    public WebhookController(WebhookService webhookService,
                             com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository userAccountRepository) {
        this.webhookService = webhookService;
        this.userAccountRepository = userAccountRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WebhookEntity create(@Valid @RequestBody CreateWebhookRequest request, Authentication authentication) {
        UUID orgId = resolveOrganizationId(authentication);
        return webhookService.createWebhook(orgId, request.url(), request.secret(), request.eventTypes());
    }

    @GetMapping
    public List<WebhookEntity> list(Authentication authentication) {
        UUID orgId = resolveOrganizationId(authentication);
        return webhookService.getOrganizationWebhooks(orgId);
    }

    @PutMapping("/{id}")
    public WebhookEntity update(@PathVariable UUID id,
                                @Valid @RequestBody UpdateWebhookRequest request,
                                Authentication authentication) {
        UUID orgId = resolveOrganizationId(authentication);
        return webhookService.updateWebhook(id, orgId, request.url(), request.secret(), request.eventTypes());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication) {
        UUID orgId = resolveOrganizationId(authentication);
        webhookService.deleteWebhook(id, orgId);
    }

    private UUID resolveOrganizationId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié.");
        }
        return userAccountRepository.findByEmail(authentication.getName().trim().toLowerCase())
                .map(u -> u.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable."));
    }

    public record CreateWebhookRequest(
            @NotBlank String url,
            @NotBlank String secret,
            @NotBlank String eventTypes
    ) {}

    public record UpdateWebhookRequest(
            String url,
            String secret,
            String eventTypes
    ) {}
}
