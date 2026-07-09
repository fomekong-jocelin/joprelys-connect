package com.joprelys.backend.cash.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentReceiptRepository extends JpaRepository<PaymentReceiptEntity, UUID> {
    Optional<PaymentReceiptEntity> findByPaymentId(UUID paymentId);

    @Query(value = "SELECT nextval('receipt_number_seq')", nativeQuery = true)
    Long getNextReceiptNumberSequenceValue();
}
