package com.joprelys.backend.billing.api;

import java.math.BigDecimal;

import com.joprelys.backend.billing.application.InsuranceBordereauService;
import com.joprelys.backend.billing.infrastructure.persistence.InsuranceBordereauEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Billing - Bordereaux d'assurance", description = "Gestion des bordereaux de tiers-payant d'assurance")
public class InsuranceBordereauController {

    private final InsuranceBordereauService bordereauService;

    public InsuranceBordereauController(InsuranceBordereauService bordereauService) {
        this.bordereauService = bordereauService;
    }

    @PostMapping("/api/billing/insurance-bordereaux")
    @PreAuthorize("hasAnyRole('SECRETAIRE_COMPTABLE', 'DAF', 'ADMIN_CLINIQUE')")
    @Operation(summary = "Générer un bordereau d'assurance", description = "Génère un bordereau récapitulatif pour les factures tiers-payant d'une convention sur une période donnée.")
    public ResponseEntity<BordereauResponse> generateBordereau(@Valid @RequestBody GenerateBordereauRequest request) {
        InsuranceBordereauEntity entity = bordereauService.generateBordereau(
                request.insuranceConventionId(),
                request.startDate(),
                request.endDate()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(BordereauResponse.fromEntity(entity));
    }

    @GetMapping("/api/billing/insurance-bordereaux")
    @PreAuthorize("hasAnyRole('SECRETAIRE_COMPTABLE', 'DAF', 'ADMIN_CLINIQUE')")
    @Operation(summary = "Lister les bordereaux", description = "Lister tous les bordereaux d'assurance générés.")
    public ResponseEntity<List<BordereauResponse>> listBordereaux() {
        List<BordereauResponse> list = bordereauService.listBordereaux().stream()
                .map(BordereauResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/api/billing/insurance-bordereaux/{id}")
    @PreAuthorize("hasAnyRole('SECRETAIRE_COMPTABLE', 'DAF', 'ADMIN_CLINIQUE')")
    @Operation(summary = "Détails d'un bordereau", description = "Retourne les détails d'un bordereau et la liste de ses factures.")
    public ResponseEntity<BordereauDetailsResponse> getBordereauDetails(@PathVariable UUID id) {
        InsuranceBordereauEntity entity = bordereauService.getBordereau(id);
        List<InvoiceEntity> invoices = bordereauService.getBordereauInvoices(id);
        return ResponseEntity.ok(BordereauDetailsResponse.fromEntity(entity, invoices));
    }

    @PostMapping("/api/billing/insurance-bordereaux/{id}/send")
    @PreAuthorize("hasAnyRole('SECRETAIRE_COMPTABLE', 'DAF', 'ADMIN_CLINIQUE')")
    @Operation(summary = "Marquer comme envoyé", description = "Passe le statut du bordereau à envoyé (SENT).")
    public ResponseEntity<BordereauResponse> markAsSent(@PathVariable UUID id) {
        InsuranceBordereauEntity entity = bordereauService.markAsSent(id);
        return ResponseEntity.ok(BordereauResponse.fromEntity(entity));
    }

    @PostMapping("/api/billing/insurance-bordereaux/{id}/pay")
    @PreAuthorize("hasAnyRole('DAF', 'ADMIN_CLINIQUE')")
    @Operation(summary = "Enregistrer le règlement", description = "Enregistre le règlement du bordereau et solde les factures correspondantes.")
    public ResponseEntity<BordereauResponse> recordPayment(@PathVariable UUID id, @Valid @RequestBody BordereauPaymentRequest request) {
        InsuranceBordereauEntity entity = bordereauService.recordPayment(id, request.amount(), request.referenceNumber());
        return ResponseEntity.ok(BordereauResponse.fromEntity(entity));
    }

    // ─── DTO Records ──────────────────────────────────────────────────────────

    public record GenerateBordereauRequest(
            @NotNull UUID insuranceConventionId,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate
    ) {}

    public record BordereauPaymentRequest(
            @NotNull Double amount,
            @NotNull String referenceNumber
    ) {}

    public record BordereauResponse(
            UUID id,
            String bordereauNumber,
            UUID insuranceConventionId,
            String insuranceConventionName,
            LocalDate startDate,
            LocalDate endDate,
            Double totalAmount,
            String status,
            String createdAt
    ) {
        public static BordereauResponse fromEntity(InsuranceBordereauEntity entity) {
            return new BordereauResponse(
                    entity.getId(),
                    entity.getBordereauNumber(),
                    entity.getInsuranceConvention().getId(),
                    entity.getInsuranceConvention().getName(),
                    entity.getStartDate(),
                    entity.getEndDate(),
                    entity.getTotalAmount(),
                    entity.getStatus().name(),
                    entity.getCreatedAt().toString()
            );
        }
    }

    public record BordereauDetailsResponse(
            UUID id,
            String bordereauNumber,
            UUID insuranceConventionId,
            String insuranceConventionName,
            LocalDate startDate,
            LocalDate endDate,
            Double totalAmount,
            String status,
            String createdAt,
            List<BordereauInvoiceDto> invoices
    ) {
        public static BordereauDetailsResponse fromEntity(InsuranceBordereauEntity entity, List<InvoiceEntity> invoices) {
            List<BordereauInvoiceDto> invoiceDtos = invoices.stream()
                    .map(BordereauInvoiceDto::fromEntity)
                    .toList();
            return new BordereauDetailsResponse(
                    entity.getId(),
                    entity.getBordereauNumber(),
                    entity.getInsuranceConvention().getId(),
                    entity.getInsuranceConvention().getName(),
                    entity.getStartDate(),
                    entity.getEndDate(),
                    entity.getTotalAmount(),
                    entity.getStatus().name(),
                    entity.getCreatedAt().toString(),
                    invoiceDtos
            );
        }
    }

    public record BordereauInvoiceDto(
            UUID id,
            String invoiceNumber,
            BigDecimal totalAmount,
            BigDecimal insuranceShare,
            BigDecimal patientShare,
            String status
    ) {
        public static BordereauInvoiceDto fromEntity(InvoiceEntity entity) {
            return new BordereauInvoiceDto(
                    entity.getId(),
                    entity.getInvoiceNumber(),
                    entity.getTotalAmount(),
                    entity.getInsuranceShare(),
                    entity.getPatientShare(),
                    entity.getStatus().name()
            );
        }
    }
}
