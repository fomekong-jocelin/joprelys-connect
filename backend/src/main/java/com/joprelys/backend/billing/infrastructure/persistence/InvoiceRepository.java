package com.joprelys.backend.billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, UUID> {
    List<InvoiceEntity> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
    Optional<InvoiceEntity> findByVisitId(UUID visitId);

    @Query("SELECT i FROM InvoiceEntity i LEFT JOIN FETCH i.insuranceConvention LEFT JOIN FETCH i.items WHERE i.id = :id")
    Optional<InvoiceEntity> findByIdWithDetails(@Param("id") UUID id);

    @Query(value = "SELECT nextval('invoice_number_seq')", nativeQuery = true)
    Long getNextInvoiceNumberSequenceValue();
}
