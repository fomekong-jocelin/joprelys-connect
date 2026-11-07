package com.joprelys.backend.eco.api;

import com.joprelys.backend.eco.api.EcoDtos.PatientLinkResponse;
import com.joprelys.backend.eco.api.EcoDtos.RevokeConsentRequest;
import com.joprelys.backend.eco.application.ConsentRevocationUseCase;
import com.joprelys.backend.eco.application.RevokeConsentCommand;
import com.joprelys.backend.eco.domain.RevokedBy;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Révocation d'un consentement par l'application bénéficiaire (client credentials, scope {@code eco:consent.revoke}). */
@RestController
@RequestMapping(EcoApiPaths.V1 + "/consents")
public class EcoConsentController {

    private final ConsentRevocationUseCase revocationUseCase;
    private final EcoCallerResolver callerResolver;
    private final EcoResourceMapper mapper;

    public EcoConsentController(ConsentRevocationUseCase revocationUseCase, EcoCallerResolver callerResolver,
                                EcoResourceMapper mapper) {
        this.revocationUseCase = revocationUseCase;
        this.callerResolver = callerResolver;
        this.mapper = mapper;
    }

    /** Idempotent : une révocation rejouée renvoie l'état révoqué sans nouvel effet. */
    @PostMapping("/{consentId}/revoke")
    @PreAuthorize("hasAuthority('SCOPE_eco:consent.revoke')")
    public PatientLinkResponse revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID consentId,
                                      @Valid @RequestBody RevokeConsentRequest request) {
        return mapper.toResponse(revocationUseCase.revoke(new RevokeConsentCommand(
                callerResolver.resolve(jwt),
                consentId,
                RevokedBy.fromContract(request.revokedBy()).orElseThrow(),
                Optional.ofNullable(request.reason()).filter(reason -> !reason.isBlank()))));
    }
}
