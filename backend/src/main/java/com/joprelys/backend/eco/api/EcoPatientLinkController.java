package com.joprelys.backend.eco.api;

import com.joprelys.backend.eco.api.EcoDtos.CreatePatientLinkRequest;
import com.joprelys.backend.eco.api.EcoDtos.PatientLinkResponse;
import com.joprelys.backend.eco.application.CreatePatientLinkCommand;
import com.joprelys.backend.eco.application.PatientLinkUseCase;
import com.joprelys.backend.eco.application.PatientLinkView;
import com.joprelys.backend.eco.domain.ConsentScope;
import com.joprelys.backend.eco.domain.LinkRelationship;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Liaison patient ↔ DPU, au nom du patient connecté à Joprelys ID (scope {@code eco:patient-link}). */
@RestController
@RequestMapping(EcoApiPaths.V1 + "/patient-links")
public class EcoPatientLinkController {

    private final PatientLinkUseCase patientLinkUseCase;
    private final EcoCallerResolver callerResolver;
    private final EcoResourceMapper mapper;

    public EcoPatientLinkController(PatientLinkUseCase patientLinkUseCase, EcoCallerResolver callerResolver,
                                    EcoResourceMapper mapper) {
        this.patientLinkUseCase = patientLinkUseCase;
        this.callerResolver = callerResolver;
        this.mapper = mapper;
    }

    /** 201 à la création, 200 si la même liaison active existait déjà (appel rejouable). */
    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_eco:patient-link')")
    public ResponseEntity<PatientLinkResponse> link(@AuthenticationPrincipal Jwt jwt,
                                                    @Valid @RequestBody CreatePatientLinkRequest request) {
        PatientLinkView view = patientLinkUseCase.link(new CreatePatientLinkCommand(
                callerResolver.resolve(jwt),
                request.allopharmaProfileId(),
                LinkRelationship.fromContract(request.relationship()).orElseThrow(),
                request.scope().stream().map(value -> ConsentScope.fromContract(value).orElseThrow())
                        .collect(Collectors.toSet()),
                Optional.ofNullable(request.periodEnd())));
        return ResponseEntity.status(view.created() ? HttpStatus.CREATED : HttpStatus.OK).body(mapper.toResponse(view));
    }
}
