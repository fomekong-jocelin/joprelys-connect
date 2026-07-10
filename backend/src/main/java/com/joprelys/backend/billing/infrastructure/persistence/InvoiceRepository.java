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

    @Query("SELECT i FROM InvoiceEntity i " +
           "WHERE i.insuranceConvention.id = :conventionId " +
           "AND i.status IN :statuses " +
           "AND COALESCE(i.validatedAt, i.createdAt) >= :start " +
           "AND COALESCE(i.validatedAt, i.createdAt) <= :end " +
           "AND i.insuranceBordereauId IS NULL " +
           "AND i.organizationId = :organizationId " +
           "AND i.insuranceShare > 0")
    List<InvoiceEntity> findInvoicesForBordereau(
            @Param("conventionId") UUID conventionId,
            @Param("statuses") List<InvoiceStatus> statuses,
            @Param("start") java.time.Instant start,
            @Param("end") java.time.Instant end,
            @Param("organizationId") UUID organizationId
    );

    List<InvoiceEntity> findByInsuranceBordereauIdAndOrganizationId(UUID insuranceBordereauId, UUID organizationId);
    List<InvoiceEntity> findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(InvoiceStatus status, java.time.Instant start, java.time.Instant end);
}
