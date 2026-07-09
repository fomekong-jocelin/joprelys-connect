package com.joprelys.backend.billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface CreditNoteRepository extends JpaRepository<CreditNoteEntity, UUID> {
    List<CreditNoteEntity> findByInvoiceId(UUID invoiceId);

    @Query(value = "SELECT nextval('credit_note_number_seq')", nativeQuery = true)
    Long getNextCreditNoteNumberSequenceValue();
}
