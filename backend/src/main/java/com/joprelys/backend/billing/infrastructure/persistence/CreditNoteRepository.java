package com.joprelys.backend.billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CreditNoteRepository extends JpaRepository<CreditNoteEntity, UUID> {
    List<CreditNoteEntity> findByInvoiceId(UUID invoiceId);
}
