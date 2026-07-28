package com.joprelys.backend.patient.api;

import com.joprelys.backend.patient.application.ExternalAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/external-access")
public class ExternalAccessController {

    private final ExternalAccessService externalAccessService;

    public ExternalAccessController(ExternalAccessService externalAccessService) {
        this.externalAccessService = externalAccessService;
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('PATIENT_READ', 'CLINICAL_READ', 'LAB_ORDER_READ', 'PHARMACY_PRESCRIPTION_READ')")
    public ExternalAccessResponse createRequest(@Valid @RequestBody CreateExternalAccessRequest request) {
        return externalAccessService.requestAccess(request);
    }
}
