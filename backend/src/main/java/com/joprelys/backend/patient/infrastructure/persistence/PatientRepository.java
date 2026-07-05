package com.joprelys.backend.patient.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<PatientEntity, UUID> {

	@Query("SELECT p FROM PatientEntity p WHERE " +
			"LOWER(p.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
			"p.phone LIKE CONCAT('%', :query, '%') OR " +
			"LOWER(p.globalPatientNumber) LIKE LOWER(CONCAT('%', :query, '%'))")
	List<PatientEntity> searchPatients(@Param("query") String query);

	@Query(value = "SELECT * FROM patients WHERE " +
			"LOWER(full_name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
			"phone LIKE CONCAT('%', :query, '%') OR " +
			"LOWER(global_patient_number) LIKE LOWER(CONCAT('%', :query, '%'))", nativeQuery = true)
	List<PatientEntity> searchPatientsGlobally(@Param("query") String query);

	boolean existsByGlobalPatientNumber(String globalPatientNumber);

	@Query(value = "SELECT * FROM patients WHERE global_patient_number = :globalPatientNumber", nativeQuery = true)
	java.util.Optional<PatientEntity> findByGlobalPatientNumber(@Param("globalPatientNumber") String globalPatientNumber);

	@Query(value = "SELECT * FROM patients WHERE id = :id", nativeQuery = true)
	java.util.Optional<PatientEntity> findByIdGlobally(@Param("id") UUID id);

	List<PatientEntity> findByBirthDate(java.time.LocalDate birthDate);
}