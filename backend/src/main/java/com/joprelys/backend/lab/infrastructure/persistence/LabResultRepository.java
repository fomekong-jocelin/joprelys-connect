package com.joprelys.backend.lab.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface LabResultRepository extends JpaRepository<LabResultEntity, UUID> {

	List<LabResultEntity> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

	List<LabResultEntity> findByLabOrderId(UUID labOrderId);

	List<LabResultEntity> findByLabOrderItemId(UUID labOrderItemId);

	List<LabResultEntity> findByResultNumber(String resultNumber);

	long countByResultNumberStartingWith(String prefix);

	List<LabResultEntity> findByPatientIdAndInterpretationOrderByCreatedAtDesc(UUID patientId, String interpretation);

	@org.springframework.data.jpa.repository.Query(value = "SELECT nextval('lab_result_number_seq')", nativeQuery = true)
	Long getNextResultNumberSequenceValue();
}
