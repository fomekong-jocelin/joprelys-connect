package com.joprelys.backend.billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ReceivableRepository extends JpaRepository<ReceivableEntity, UUID> {
    List<ReceivableEntity> findByDebtorIdOrderByCreatedAtDesc(UUID debtorId);
    List<ReceivableEntity> findByInvoiceId(UUID invoiceId);
    List<ReceivableEntity> findByInvoiceIdAndDebtorTypeIgnoreCase(UUID invoiceId, String debtorType);
    List<ReceivableEntity> findByStatusOrderByCreatedAtDesc(String status);
}
