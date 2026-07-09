package com.joprelys.backend.billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItemEntity, UUID> {
    List<InvoiceItemEntity> findByInvoiceId(UUID invoiceId);
}
