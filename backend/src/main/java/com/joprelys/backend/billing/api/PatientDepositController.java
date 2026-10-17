package com.joprelys.backend.billing.api;

import com.joprelys.backend.billing.application.PatientDepositService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deposits")
public class PatientDepositController {

    private final PatientDepositService depositService;

    public PatientDepositController(PatientDepositService depositService) {
        this.depositService = depositService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CASH_PAYMENT_COLLECT')")
    @Operation(summary = "Encaisser un acompte", description = "Encaisse un acompte patient dans la session de caisse ouverte.")
    public ResponseEntity<DepositResponse> collect(@Valid @RequestBody CreateDepositRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(depositService.collect(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('CASH_QUEUE_READ', 'BILLING_INVOICE_READ')")
    @Operation(summary = "Acomptes d'un patient", description = "Liste les acomptes et leur solde imputable.")
    public ResponseEntity<List<DepositResponse>> list(@RequestParam UUID patientId) {
        return ResponseEntity.ok(depositService.listByPatient(patientId));
    }
}
