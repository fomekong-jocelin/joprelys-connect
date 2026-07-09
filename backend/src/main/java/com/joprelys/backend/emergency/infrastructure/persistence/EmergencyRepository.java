package com.joprelys.backend.emergency.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmergencyRepository extends JpaRepository<EmergencyEntity, UUID> {
	@Query("SELECT e FROM EmergencyEntity e JOIN FETCH e.patient LEFT JOIN FETCH e.resuscitationLogs WHERE e.organizationId = :organizationId")
	List<EmergencyEntity> findByOrganizationId(@Param("organizationId") UUID organizationId);

	@Query("SELECT e FROM EmergencyEntity e JOIN FETCH e.patient LEFT JOIN FETCH e.resuscitationLogs WHERE e.stabilizedAt IS NULL")
	List<EmergencyEntity> findByStabilizedAtIsNull();

	@Query("SELECT e FROM EmergencyEntity e JOIN FETCH e.patient LEFT JOIN FETCH e.resuscitationLogs WHERE e.id = :id")
	Optional<EmergencyEntity> findByIdWithPatientAndLogs(@Param("id") UUID id);

	Optional<EmergencyEntity> findByVisitId(UUID visitId);

	@Query("SELECT e FROM EmergencyEntity e JOIN FETCH e.patient LEFT JOIN FETCH e.resuscitationLogs WHERE e.patient.id = :patientId ORDER BY e.createdAt DESC")
	List<EmergencyEntity> findByPatientIdWithLogs(@Param("patientId") UUID patientId);
}
