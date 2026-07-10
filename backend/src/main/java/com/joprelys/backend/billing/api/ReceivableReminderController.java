package com.joprelys.backend.billing.api;

import com.joprelys.backend.billing.application.ReceivableReminderService;
import com.joprelys.backend.billing.infrastructure.persistence.ReceivableReminderEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@Tag(name = "Billing - Relances Créances", description = "Gestion des relances et historique pour le recouvrement")
public class ReceivableReminderController {

    private final ReceivableReminderService reminderService;

    public ReceivableReminderController(ReceivableReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping("/api/receivables/{receivableId}/reminders")
    @PreAuthorize("hasAnyRole('DAF', 'SECRETAIRE_COMPTABLE', 'ADMIN_CLINIQUE')")
    @Operation(summary = "Consigner une relance", description = "Ajoute une action de relance dans l'historique d'une créance.")
    public ResponseEntity<ReceivableReminderResponse> recordReminder(
            @PathVariable("receivableId") UUID receivableId,
            @Valid @RequestBody CreateReminderRequest request) {
        
        ReceivableReminderEntity entity = reminderService.recordReminder(
                receivableId,
                request.actionType(),
                request.status(),
                request.notes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ReceivableReminderResponse.fromEntity(entity));
    }

    @GetMapping("/api/receivables/{receivableId}/reminders")
    @PreAuthorize("hasAnyRole('DAF', 'SECRETAIRE_COMPTABLE', 'ADMIN_CLINIQUE')")
    @Operation(summary = "Lister les relances d'une créance", description = "Récupère l'historique des relances chronologiques d'une créance.")
    public ResponseEntity<List<ReceivableReminderResponse>> getReminders(
            @PathVariable("receivableId") UUID receivableId) {
        
        List<ReceivableReminderEntity> reminders = reminderService.getReminders(receivableId);
        List<ReceivableReminderResponse> response = reminders.stream()
                .map(ReceivableReminderResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}
