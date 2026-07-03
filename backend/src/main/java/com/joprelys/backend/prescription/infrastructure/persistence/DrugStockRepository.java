package com.joprelys.backend.prescription.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository Spring Data JPA pour les stocks de médicaments (STORY-1103).
 * Le filtrage multi-tenant par organization_id est automatique via @TenantId Hibernate.
 */
public interface DrugStockRepository extends JpaRepository<DrugStockEntity, UUID> {

    List<DrugStockEntity> findAllByOrderByDrugNameAsc();

    @Query("SELECT s FROM DrugStockEntity s WHERE LOWER(s.drugName) = LOWER(:drugName)")
    Optional<DrugStockEntity> findByDrugNameIgnoreCase(@Param("drugName") String drugName);

    @Query("SELECT s FROM DrugStockEntity s WHERE s.quantityAvailable <= s.minimumThreshold ORDER BY s.quantityAvailable ASC")
    List<DrugStockEntity> findBelowThreshold();
}
