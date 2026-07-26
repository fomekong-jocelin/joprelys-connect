package com.joprelys.backend.visit.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitRepository extends JpaRepository<VisitEntity, UUID> {

    @Query("SELECT v FROM VisitEntity v JOIN FETCH v.patient LEFT JOIN FETCH v.vitals WHERE v.status = 'EN_COURS' ORDER BY v.createdAt ASC")
    List<VisitEntity> findActiveVisits();

    boolean existsByPatientIdAndStatus(UUID patientId, String status);

    Optional<VisitEntity> findFirstByPatientIdAndStatusOrderByCreatedAtDesc(UUID patientId, String status);

    // WT3 (DUPLICATES): Find all visits for a patient (used during merge)
    List<VisitEntity> findByPatientId(UUID patientId);

    @Query("SELECT v FROM VisitEntity v JOIN FETCH v.patient LEFT JOIN FETCH v.vitals WHERE v.patient.id = :patientId ORDER BY v.createdAt DESC")
    List<VisitEntity> findByPatientIdWithPatientAndVitals(@Param("patientId") UUID patientId);

    @Query(value = "SELECT * FROM visits WHERE id = :id", nativeQuery = true)
    Optional<VisitEntity> findByIdGlobally(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM VisitEntity v WHERE v.id = :id")
    Optional<VisitEntity> findByIdForUpdate(@Param("id") UUID id);

    // WT1 (SCOPES): Find patient ID from a visit ID (used for scope validation)
    @Query(value = "SELECT patient_id FROM visits WHERE id = :visitId", nativeQuery = true)
    Optional<Object> findPatientIdByVisitId(@Param("visitId") UUID visitId);
}
