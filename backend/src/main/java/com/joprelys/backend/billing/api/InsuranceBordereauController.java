package com.joprelys.backend.billing.api;

import com.joprelys.backend.billing.application.InsuranceBordereauService;
import com.joprelys.backend.billing.infrastructure.persistence.InsuranceBordereauEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Billing - Bordereaux d'assurance", description = "Gestion des bordereaux de tiers-payant d'assurance")
public class InsuranceBordereauController {

    private static final String CAN_READ =
            "hasAuthority('INSURANCE_BORDEREAU_READ') or hasAnyRole('SECRETAIRE_COMPTABLE', 'DAF', 'ADMIN_CLINIQUE', 'ADMIN_JOPRELYS', 'SUPER_ADMIN')";
    private static final String CAN_PROGRESS =
            "hasAuthority('INSURANCE_BORDEREAU_PROGRESS') or hasAnyRole('SECRETAIRE_COMPTABLE', 'DAF', 'ADMIN_CLINIQUE', 'ADMIN_JOPRELYS', 'SUPER_ADMIN')";
    private static final String CAN_SETTLE =
            "hasAuthority('INSURANCE_BORDEREAU_SETTLE') or hasAnyRole('DAF', 'ADMIN_CLINIQUE', 'ADMIN_JOPRELYS', 'SUPER_ADMIN')";

    private final InsuranceBordereauService bordereauService;

    public InsuranceBordereauController(InsuranceBordereauService bordereauService) {
        this.bordereauService = bordereauService;
    }

    @PostMapping("/api/billing/insurance-bordereaux")
    @PreAuthorize(CAN_PROGRESS)
    @Operation(summary = "Générer un bordereau d'assurance")
    public ResponseEntity<BordereauResponse> generateBordereau(@Valid @RequestBody GenerateBordereauRequest request) {
        InsuranceBordereauEntity entity = bordereauService.generateBordereau(
                request.insuranceConventionId(), request.startDate(), request.endDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(BordereauResponse.fromEntity(entity));
    }

    @GetMapping("/api/billing/insurance-bordereaux")
    @PreAuthorize(CAN_READ)
    @Operation(summary = "Lister les bordereaux")
    public ResponseEntity<List<BordereauResponse>> listBordereaux() {
        return ResponseEntity.ok(bordereauService.listBordereaux().stream()
                .map(BordereauResponse::fromEntity)
                .toList());
    }

    @GetMapping("/api/billing/insurance-bordereaux/{id}")
    @PreAuthorize(CAN_READ)
    @Operation(summary = "Détails d'un bordereau")
    public ResponseEntity<BordereauDetailsResponse> getBordereauDetails(@PathVariable UUID id) {
        InsuranceBordereauEntity entity = bordereauService.getBordereau(id);
        List<InvoiceEntity> invoices = bordereauService.getBordereauInvoices(id);
        return ResponseEntity.ok(BordereauDetailsResponse.fromEntity(entity, invoices));
    }

    @PostMapping("/api/billing/insurance-bordereaux/{id}/send")
    @PreAuthorize(CAN_PROGRESS)
    public ResponseEntity<BordereauResponse> markAsSent(@PathVariable UUID id) {
        return ResponseEntity.ok(BordereauResponse.fromEntity(bordereauService.markAsSent(id)));
    }

    @PostMapping("/api/billing/insurance-bordereaux/{id}/receive")
    @PreAuthorize(CAN_PROGRESS)
    public ResponseEntity<BordereauResponse> markAsReceived(
            @PathVariable UUID id,
            @Valid @RequestBody ReceiveBordereauRequest request) {
        return ResponseEntity.ok(BordereauResponse.fromEntity(
                bordereauService.markAsReceived(id, request.insurerReference())));
    }

    @PostMapping("/api/billing/insurance-bordereaux/{id}/accept")
    @PreAuthorize(CAN_SETTLE)
    public ResponseEntity<BordereauResponse> accept(
            @PathVariable UUID id,
            @Valid @RequestBody AcceptBordereauRequest request) {
        return ResponseEntity.ok(BordereauResponse.fromEntity(
                bordereauService.accept(id, request.acceptedAmount(), request.insurerReference())));
    }

    @PostMapping("/api/billing/insurance-bordereaux/{id}/reject")
    @PreAuthorize(CAN_SETTLE)
    public ResponseEntity<BordereauResponse> reject(
            @PathVariable UUID id,
            @Valid @RequestBody RejectBordereauRequest request) {
        return ResponseEntity.ok(BordereauResponse.fromEntity(
                bordereauService.reject(id, request.rejectionReason(), request.insurerReference())));
    }

    @PostMapping("/api/billing/insurance-bordereaux/{id}/pay")
    @PreAuthorize(CAN_SETTLE)
    public ResponseEntity<BordereauResponse> recordPayment(
            @PathVariable UUID id,
            @Valid @RequestBody BordereauPaymentRequest request) {
        return ResponseEntity.ok(BordereauResponse.fromEntity(
                bordereauService.recordPayment(id, request.amount(), request.referenceNumber())));
    }

    public record GenerateBordereauRequest(
            @NotNull UUID insuranceConventionId,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate
    ) {}

    public record ReceiveBordereauRequest(@NotBlank String insurerReference) {}

    public record AcceptBordereauRequest(
            @NotNull BigDecimal acceptedAmount,
            String insurerReference
    ) {}

    public record RejectBordereauRequest(
            @NotBlank String rejectionReason,
            String insurerReference
    ) {}

    public record BordereauPaymentRequest(
            @NotNull BigDecimal amount,
            @NotBlank String referenceNumber
    ) {}

    public record BordereauResponse(
            UUID id,
            String bordereauNumber,
            UUID insuranceConventionId,
            String insuranceConventionName,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal totalAmount,
            BigDecimal acceptedAmount,
            BigDecimal paidAmount,
            BigDecimal remainingAmount,
            BigDecimal disputedAmount,
            String insurerReference,
            String paymentReference,
            String rejectionReason,
            String status,
            Instant sentAt,
            Instant receivedAt,
            Instant acceptedAt,
            Instant rejectedAt,
            Instant settledAt,
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
                    entity.getAcceptedAmount(),
                    entity.getPaidAmount(),
                    entity.getRemainingAmount(),
                    entity.getDisputedAmount(),
                    entity.getInsurerReference(),
                    entity.getPaymentReference(),
                    entity.getRejectionReason(),
                    entity.getStatus().name(),
                    entity.getSentAt(),
                    entity.getReceivedAt(),
                    entity.getAcceptedAt(),
                    entity.getRejectedAt(),
                    entity.getSettledAt(),
                    entity.getCreatedAt().toString());
        }
    }

    public record BordereauDetailsResponse(
            UUID id,
            String bordereauNumber,
            UUID insuranceConventionId,
            String insuranceConventionName,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal totalAmount,
            BigDecimal acceptedAmount,
            BigDecimal paidAmount,
            BigDecimal remainingAmount,
            BigDecimal disputedAmount,
            String insurerReference,
            String paymentReference,
            String rejectionReason,
            String status,
            Instant sentAt,
            Instant receivedAt,
            Instant acceptedAt,
            Instant rejectedAt,
            Instant settledAt,
            String createdAt,
            List<BordereauInvoiceDto> invoices
    ) {
        public static BordereauDetailsResponse fromEntity(
                InsuranceBordereauEntity entity,
                List<InvoiceEntity> invoices) {
            return new BordereauDetailsResponse(
                    entity.getId(),
                    entity.getBordereauNumber(),
                    entity.getInsuranceConvention().getId(),
                    entity.getInsuranceConvention().getName(),
                    entity.getStartDate(),
                    entity.getEndDate(),
                    entity.getTotalAmount(),
                    entity.getAcceptedAmount(),
                    entity.getPaidAmount(),
                    entity.getRemainingAmount(),
                    entity.getDisputedAmount(),
                    entity.getInsurerReference(),
                    entity.getPaymentReference(),
                    entity.getRejectionReason(),
                    entity.getStatus().name(),
                    entity.getSentAt(),
                    entity.getReceivedAt(),
                    entity.getAcceptedAt(),
                    entity.getRejectedAt(),
                    entity.getSettledAt(),
                    entity.getCreatedAt().toString(),
                    invoices.stream().map(BordereauInvoiceDto::fromEntity).toList());
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
                    entity.getStatus().name());
        }
    }
}
