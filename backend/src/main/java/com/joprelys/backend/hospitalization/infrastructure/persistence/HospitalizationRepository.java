package com.joprelys.backend.hospitalization.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HospitalizationRepository extends JpaRepository<HospitalizationEntity, UUID> {

    Optional<HospitalizationEntity> findByVisitId(UUID visitId);

    Optional<HospitalizationEntity> findByEmergencyId(UUID emergencyId);

    List<HospitalizationEntity> findByPatientIdOrderByAdmittedAtDesc(UUID patientId);

    @Query("SELECT h FROM HospitalizationEntity h WHERE h.patientId IN :patientIds ORDER BY h.admittedAt DESC")
    List<HospitalizationEntity> findByPatientIdsOrderByAdmittedAtDesc(
            @Param("patientIds") Collection<UUID> patientIds);

    @Query("SELECT h FROM HospitalizationEntity h WHERE h.patientId = :patientId AND h.status = 'EN_COURS'")
    Optional<HospitalizationEntity> findActiveByPatientId(@Param("patientId") UUID patientId);

    @Query("SELECT h FROM HospitalizationEntity h WHERE h.patientId IN :patientIds AND h.status = 'EN_COURS'")
    Optional<HospitalizationEntity> findActiveByPatientIds(@Param("patientIds") Collection<UUID> patientIds);

    @Query("SELECT h FROM HospitalizationEntity h WHERE h.status = 'EN_COURS'")
    List<HospitalizationEntity> findAllActive();

    @Query("SELECT h FROM HospitalizationEntity h WHERE h.currentBedId = :bedId AND h.status = 'EN_COURS'")
    Optional<HospitalizationEntity> findActiveByBedId(@Param("bedId") UUID bedId);

    @Query(value = "SELECT nextval('hospitalization_number_seq')", nativeQuery = true)
    Long getNextHospitalizationNumberSequenceValue();
}
