package com.joprelys.backend.lab.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabOrderRepository extends JpaRepository<LabOrderEntity, UUID> {

	Optional<LabOrderEntity> findByExamRequestNumber(String examRequestNumber);

	List<LabOrderEntity> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

	List<LabOrderEntity> findAllByOrderByCreatedAtDesc();

	List<LabOrderEntity> findByVisitId(UUID visitId);

	@org.springframework.data.jpa.repository.Query("SELECT DISTINCT o FROM LabOrderEntity o JOIN o.items i WHERE i.id IN :itemIds")
	List<LabOrderEntity> findByItemIds(@org.springframework.data.repository.query.Param("itemIds") java.util.Collection<UUID> itemIds);

	Optional<LabOrderEntity> findFirstByVisitIdAndExamTypeAndStatusNotOrderByCreatedAtDesc(
			UUID visitId, ExamType examType, LabOrderStatus status);

	long countByExamRequestNumberStartingWith(String prefix);

	@org.springframework.data.jpa.repository.Query(value = "SELECT COUNT(*) FROM lab_orders WHERE exam_request_number LIKE CONCAT(:prefix, '%')", nativeQuery = true)
	long countByExamRequestNumberStartingWithGlobally(@org.springframework.data.repository.query.Param("prefix") String prefix);

	@org.springframework.data.jpa.repository.Query(value = "SELECT * FROM lab_orders WHERE id = :id", nativeQuery = true)
	Optional<LabOrderEntity> findByIdGlobally(@org.springframework.data.repository.query.Param("id") UUID id);
}
