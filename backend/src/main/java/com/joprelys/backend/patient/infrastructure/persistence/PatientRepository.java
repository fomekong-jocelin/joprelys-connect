package com.joprelys.backend.patient.infrastructure.persistence;

import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepository extends JpaRepository<PatientEntity, UUID> {

    @Query("SELECT p FROM PatientEntity p WHERE "
            + "LOWER(COALESCE(p.fullName, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR "
            + "COALESCE(p.phone, '') LIKE CONCAT('%', :query, '%') OR "
            + "LOWER(p.globalPatientNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR "
            + "LOWER(COALESCE(p.temporaryPatientNumber, '')) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<PatientEntity> searchPatients(@Param("query") String query);

    @Query(value = "SELECT * FROM patients WHERE identity_status <> 'PROVISIONAL_URGENCY' AND ("
            + "LOWER(COALESCE(full_name, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR "
            + "COALESCE(phone, '') LIKE CONCAT('%', :query, '%') OR "
            + "LOWER(global_patient_number) LIKE LOWER(CONCAT('%', :query, '%'))) ", nativeQuery = true)
    List<PatientEntity> searchPatientsGlobally(@Param("query") String query);

    boolean existsByGlobalPatientNumber(String globalPatientNumber);

    boolean existsByTemporaryPatientNumber(String temporaryPatientNumber);

    Optional<PatientEntity> findByTemporaryPatientNumber(String temporaryPatientNumber);

    @Query(value = "SELECT * FROM patients WHERE global_patient_number = :globalPatientNumber", nativeQuery = true)
    Optional<PatientEntity> findByGlobalPatientNumber(@Param("globalPatientNumber") String globalPatientNumber);

    @Query(value = "SELECT * FROM patients WHERE id = :id", nativeQuery = true)
    Optional<PatientEntity> findByIdGlobally(@Param("id") UUID id);

    List<PatientEntity> findByBirthDate(LocalDate birthDate);

    List<PatientEntity> findAllByIdentityStatus(PatientIdentityStatus identityStatus);
}
