package com.joprelys.backend.accounting.api;

import com.joprelys.backend.accounting.application.AccountingExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/accounting")
@Tag(name = "Comptabilité", description = "Exports comptables et intégration OHADA")
public class AccountingController {

    private final AccountingExportService accountingExportService;

    public AccountingController(AccountingExportService accountingExportService) {
        this.accountingExportService = accountingExportService;
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN_CLINIQUE', 'DAF')")
    @Operation(summary = "Exporter les écritures comptables OHADA", description = "Génère un export CSV d'import pour Sage 100")
    public ResponseEntity<byte[]> exportSage100(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Instant start;
        Instant end;

        if (startDate != null && !startDate.isBlank()) {
            start = LocalDate.parse(startDate).atStartOfDay(ZoneOffset.UTC).toInstant();
        } else {
            start = LocalDate.now().minusDays(30).atStartOfDay(ZoneOffset.UTC).toInstant();
        }

        if (endDate != null && !endDate.isBlank()) {
            end = LocalDate.parse(endDate).atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
        } else {
            end = Instant.now();
        }

        String csvContent = accountingExportService.generateSage100Export(start, end);
        byte[] csvBytes = csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        String filename = "export_sage_100_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(csvBytes.length)
                .body(csvBytes);
    }
}
