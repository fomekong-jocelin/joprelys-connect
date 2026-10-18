package com.joprelys.backend.billing.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientDepositRepository extends JpaRepository<PatientDepositEntity, UUID> {

    List<PatientDepositEntity> findByPatientIdInOrderByCreatedAtDesc(Collection<UUID> patientIds);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM PatientDepositEntity d WHERE d.id = :id")
    Optional<PatientDepositEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query(value = "SELECT nextval('patient_deposit_number_seq')", nativeQuery = true)
    Long nextDepositNumber();
}
