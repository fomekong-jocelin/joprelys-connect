package com.joprelys.backend.ai.facts.api;

import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactLedgerView;
import com.joprelys.backend.ai.facts.application.ClinicalFactExtractionService;
import com.joprelys.backend.ai.facts.application.ClinicalFactExtractionService.ExtractionReport;
import com.joprelys.backend.ai.facts.application.ClinicalFactLedgerService;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.ApplyRevisionRequest;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RevisionBatchView;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RevisionHistoryView;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionPlannerContract.PlanRevisionRequest;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionPlannerContract.RevisionPlanView;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionPlannerService;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionService;
import com.joprelys.backend.ai.facts.application.ClinicalNoteProjectionContract.NoteProjectionView;
import com.joprelys.backend.ai.facts.application.ClinicalNoteProjectionService;
import com.joprelys.backend.ai.facts.application.ClinicalNoteValidationContract.NoteValidationHistoryView;
import com.joprelys.backend.ai.facts.application.ClinicalNoteValidationContract.NoteValidationView;
import com.joprelys.backend.ai.facts.application.ClinicalNoteValidationContract.ValidateProjectionRequest;
import com.joprelys.backend.ai.facts.application.ClinicalNoteValidationService;
import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.auth.security.TenantContext;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai/consultations/{visitId}/facts")
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class ClinicalFactController {

    private final ClinicalFactLedgerService factLedgerService;
    private final ClinicalFactExtractionService extractionService;
    private final ClinicalNoteProjectionService noteProjectionService;
    private final ClinicalNoteValidationService noteValidationService;
    private final ClinicalFactRevisionService revisionService;
    private final ClinicalFactRevisionPlannerService revisionPlannerService;

    public ClinicalFactController(
            ClinicalFactLedgerService factLedgerService,
            ClinicalFactExtractionService extractionService,
            ClinicalNoteProjectionService noteProjectionService,
            ClinicalNoteValidationService noteValidationService,
            ClinicalFactRevisionService revisionService,
            ClinicalFactRevisionPlannerService revisionPlannerService) {
        this.factLedgerService = factLedgerService;
        this.extractionService = extractionService;
        this.noteProjectionService = noteProjectionService;
        this.noteValidationService = noteValidationService;
        this.revisionService = revisionService;
        this.revisionPlannerService = revisionPlannerService;
    }

    @PostMapping("/extract")
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    public ExtractionReport extract(
            @PathVariable UUID visitId,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return extractionService.extractNewFacts(
                visitId,
                identity.userId(),
                identity.organizationId());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CLINICAL_READ') or hasAuthority('CLINICAL_WRITE')")
    public FactLedgerView effective(
            @PathVariable UUID visitId,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return factLedgerService.listEffective(visitId, identity.organizationId());
    }

    @PostMapping("/revisions/plan")
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    public RevisionPlanView planRevision(
            @PathVariable UUID visitId,
            @RequestBody PlanRevisionRequest request,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return revisionPlannerService.plan(
                visitId,
                identity.organizationId(),
                request);
    }

    @PostMapping("/revisions")
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    public RevisionBatchView applyRevision(
            @PathVariable UUID visitId,
            @RequestBody ApplyRevisionRequest request,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return revisionService.apply(
                visitId,
                identity.userId(),
                identity.organizationId(),
                request);
    }

    @GetMapping("/revisions")
    @PreAuthorize("hasAuthority('CLINICAL_READ') or hasAuthority('CLINICAL_WRITE')")
    public RevisionHistoryView revisionHistory(
            @PathVariable UUID visitId,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return revisionService.history(visitId, identity.organizationId());
    }

    @GetMapping("/note-projection")
    @PreAuthorize("hasAuthority('CLINICAL_READ') or hasAuthority('CLINICAL_WRITE')")
    public NoteProjectionView noteProjection(
            @PathVariable UUID visitId,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return noteProjectionService.project(visitId, identity.organizationId());
    }

    @PostMapping("/note-projection/validations")
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    public NoteValidationView validateNoteProjection(
            @PathVariable UUID visitId,
            @Valid @RequestBody ValidateProjectionRequest request,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return noteValidationService.validate(
                visitId,
                identity.userId(),
                identity.organizationId(),
                request);
    }

    @GetMapping("/note-projection/validations")
    @PreAuthorize("hasAuthority('CLINICAL_READ') or hasAuthority('CLINICAL_WRITE')")
    public NoteValidationHistoryView noteValidationHistory(
            @PathVariable UUID visitId,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return noteValidationService.history(visitId, identity.organizationId());
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('CLINICAL_READ') or hasAuthority('CLINICAL_WRITE')")
    public FactLedgerView audit(
            @PathVariable UUID visitId,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return factLedgerService.listAudit(visitId, identity.organizationId());
    }

    private Identity identity(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof JwtClaims claims)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED");
        }
        UUID userId = parseUuid(claims.subject())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "AUTH_IDENTITY_UNRESOLVED"));
        UUID organizationId = Optional.ofNullable(TenantContext.getTenantId())
                .or(() -> parseUuid(claims.organizationId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "AUTH_TENANT_UNRESOLVED"));
        return new Identity(userId, organizationId);
    }

    private Optional<UUID> parseUuid(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private record Identity(UUID userId, UUID organizationId) {
    }
}
