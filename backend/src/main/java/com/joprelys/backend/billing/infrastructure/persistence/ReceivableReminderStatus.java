package com.joprelys.backend.billing.infrastructure.persistence;

public enum ReceivableReminderStatus {
    PENDING,
    PROMISED_PAYMENT,
    DISPUTE,
    UNREACHABLE
}
