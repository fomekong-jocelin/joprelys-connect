package com.joprelys.backend.billing.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.billing.infrastructure.persistence.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ReceivableReminderService {

    private final ReceivableReminderRepository reminderRepository;
    private final ReceivableRepository receivableRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public ReceivableReminderService(ReceivableReminderRepository reminderRepository,
                                     ReceivableRepository receivableRepository,
                                     UserAccountRepository userAccountRepository,
                                     AuditService auditService) {
        this.reminderRepository = reminderRepository;
        this.receivableRepository = receivableRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    private UserAccountEntity getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userAccountRepository.findByEmail(email).orElse(null);
    }

    @Transactional
    public ReceivableReminderEntity recordReminder(UUID receivableId, ReceivableReminderActionType actionType, ReceivableReminderStatus status, String notes) {
        UserAccountEntity actor = getCurrentUser();
        if (actor == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non connecté");
        }

        ReceivableEntity receivable = receivableRepository.findById(receivableId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Créance introuvable"));

        ReceivableReminderEntity reminder = new ReceivableReminderEntity(
                receivableId,
                actionType,
                status,
                notes,
                actor.getId()
        );
        reminder.setOrganizationId(actor.getOrganizationId());

        ReceivableReminderEntity saved = reminderRepository.save(reminder);

        auditService.logSuccess(
                actor.getId(),
                actor.getOrganizationId(),
                null,
                "BILLING",
                saved.getId(),
                "RECORD_REMINDER",
                "Consignation d'une action de relance (" + actionType + ", statut: " + status + ") pour la créance ID " + receivableId
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public List<ReceivableReminderEntity> getReminders(UUID receivableId) {
        // Vérifier que la créance existe
        if (!receivableRepository.existsById(receivableId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Créance introuvable");
        }
        return reminderRepository.findByReceivableIdOrderByCreatedAtDesc(receivableId);
    }
}
