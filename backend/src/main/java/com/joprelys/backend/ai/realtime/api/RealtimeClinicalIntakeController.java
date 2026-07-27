package com.joprelys.backend.ai.realtime.api;

import com.joprelys.backend.ai.realtime.application.RealtimeClinicalIntakeService;
import com.joprelys.backend.ai.realtime.application.RealtimeClinicalIntakeService.IntakeView;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/consultations/{visitId}/realtime-intake")
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
@PreAuthorize("hasAuthority('CLINICAL_WRITE')")
public class RealtimeClinicalIntakeController {

    private final RealtimeClinicalIntakeService service;
    private final RealtimeIntakeIdentityResolver identityResolver;

    public RealtimeClinicalIntakeController(
            RealtimeClinicalIntakeService service,
            RealtimeIntakeIdentityResolver identityResolver) {
        this.service = service;
        this.identityResolver = identityResolver;
    }

    @PostMapping
    public IntakeView ingest(
            @PathVariable UUID visitId,
            @Valid @RequestBody RealtimeIntakeRequest request,
            Authentication authentication) {
        var identity = identityResolver.resolve(authentication);
        return service.ingest(
                visitId,
                identity.userId(),
                identity.organizationId(),
                request.eventId(),
                request.itemId(),
                request.transcript(),
                request.confidence());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CLINICAL_READ') or hasAuthority('CLINICAL_WRITE')")
    public List<IntakeView> list(
            @PathVariable UUID visitId,
            Authentication authentication) {
        var identity = identityResolver.resolve(authentication);
        return service.list(visitId, identity.organizationId());
    }
}
