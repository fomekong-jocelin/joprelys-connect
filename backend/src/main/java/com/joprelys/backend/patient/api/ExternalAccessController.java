package com.joprelys.backend.patient.api;

import com.joprelys.backend.patient.application.ExternalAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/external-access")
@PreAuthorize("hasAnyRole('INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE')")
public class ExternalAccessController {

    private final ExternalAccessService externalAccessService;

    public ExternalAccessController(ExternalAccessService externalAccessService) {
        this.externalAccessService = externalAccessService;
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ExternalAccessResponse createRequest(@Valid @RequestBody CreateExternalAccessRequest request) {
        return externalAccessService.requestAccess(request);
    }
}
