package com.joprelys.backend.billing.api;

import com.joprelys.backend.billing.infrastructure.persistence.ReceivableReminderActionType;
import com.joprelys.backend.billing.infrastructure.persistence.ReceivableReminderStatus;

public record CreateReminderRequest(
    ReceivableReminderActionType actionType,
    ReceivableReminderStatus status,
    String notes
) {
}
