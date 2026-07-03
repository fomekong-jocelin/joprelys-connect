package com.joprelys.backend.prescription.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface DispensationItemRepository extends JpaRepository<DispensationItemEntity, UUID> {
	List<DispensationItemEntity> findByPrescriptionItemPrescriptionId(UUID prescriptionId);

	@Query("SELECT COALESCE(SUM(di.quantityDispensed), 0) FROM DispensationItemEntity di WHERE di.prescriptionItem.id = :prescriptionItemId")
	int sumQuantityDispensedByPrescriptionItemId(@Param("prescriptionItemId") UUID prescriptionItemId);
}
