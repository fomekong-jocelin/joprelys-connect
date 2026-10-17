package com.joprelys.backend.billing.application;

import com.joprelys.backend.billing.infrastructure.persistence.InvoiceEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceItemEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceStatus;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Sources cliniques déjà portées par des factures non annulées d'un épisode (ADR-20261017 D-15). */
final class BilledSources {

    private final Set<String> keys = new HashSet<>();
    private final Map<UUID, Long> stayDays = new HashMap<>();

    static BilledSources of(List<InvoiceEntity> invoices, UUID excludedInvoiceId) {
        BilledSources billed = new BilledSources();
        for (InvoiceEntity invoice : invoices) {
            if (invoice.getStatus() == InvoiceStatus.CANCELLED || Objects.equals(invoice.getId(), excludedInvoiceId)) {
                continue;
            }
            for (InvoiceItemEntity item : invoice.getItems()) {
                if (item.getSourceType() == null || item.getSourceId() == null) {
                    continue;
                }
                if (InvoiceChargeCollector.STAY.equals(item.getSourceType())) {
                    billed.stayDays.merge(item.getSourceId(), quantity(item.getQuantity()), Long::sum);
                } else {
                    billed.keys.add(key(item.getSourceType(), item.getSourceId()));
                }
            }
        }
        return billed;
    }

    boolean contains(String sourceType, UUID sourceId) {
        return keys.contains(key(sourceType, sourceId));
    }

    long stayDays(UUID stayId) {
        return stayDays.getOrDefault(stayId, 0L);
    }

    private static long quantity(BigDecimal value) {
        return value == null ? 0 : value.longValue();
    }

    private static String key(String sourceType, UUID sourceId) {
        return sourceType + ":" + sourceId;
    }
}
