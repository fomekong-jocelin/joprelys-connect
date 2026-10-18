package com.joprelys.backend.billing.application;

import com.joprelys.backend.billing.infrastructure.persistence.InvoiceEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceItemEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceStatus;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderRepository;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Libère les examens en attente de paiement dès que la part patient de la facture qui les porte est réglée
 * (ADR-20261017 D-13). Une facture sans part patient (prise en charge totale) libère dès sa validation.
 */
@Service
public class LabPaymentReleaseService {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");
    private static final Set<InvoiceStatus> PATIENT_SHARE_SETTLED = Set.of(InvoiceStatus.PAID, InvoiceStatus.SETTLED);

    private final LabOrderRepository labOrderRepository;

    public LabPaymentReleaseService(LabOrderRepository labOrderRepository) {
        this.labOrderRepository = labOrderRepository;
    }

    @Transactional
    public int releaseIfPatientShareSettled(InvoiceEntity invoice) {
        boolean settled = PATIENT_SHARE_SETTLED.contains(invoice.getStatus())
                || (invoice.getStatus() == InvoiceStatus.VALIDATED
                    && invoice.getPatientShare() != null && invoice.getPatientShare().compareTo(TOLERANCE) < 0);
        if (!settled) {
            return 0;
        }
        List<UUID> examItemIds = invoice.getItems().stream()
                .filter(item -> InvoiceChargeCollector.LAB_ORDER_ITEM.equals(item.getSourceType()))
                .map(InvoiceItemEntity::getSourceId)
                .toList();
        if (examItemIds.isEmpty()) {
            return 0;
        }
        int released = 0;
        for (LabOrderEntity order : labOrderRepository.findByItemIds(examItemIds)) {
            if (order.getStatus() == LabOrderStatus.AWAITING_PAYMENT) {
                order.setStatus(LabOrderStatus.PAID);
                labOrderRepository.save(order);
                released++;
            }
        }
        return released;
    }
}
