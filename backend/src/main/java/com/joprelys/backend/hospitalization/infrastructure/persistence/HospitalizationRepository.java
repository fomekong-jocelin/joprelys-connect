package com.joprelys.backend.hospitalization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HospitalizationRepository extends JpaRepository<HospitalizationEntity, UUID> {

    List<HospitalizationEntity> findByPatientIdOrderByAdmittedAtDesc(UUID patientId);

    @Query("SELECT h FROM HospitalizationEntity h WHERE h.patientId = :patientId AND h.status = 'EN_COURS'")
    Optional<HospitalizationEntity> findActiveByPatientId(@Param("patientId") UUID patientId);

    @Query("SELECT h FROM HospitalizationEntity h WHERE h.status = 'EN_COURS'")
    List<HospitalizationEntity> findAllActive();

    @Query("SELECT h FROM HospitalizationEntity h WHERE h.roomNumber = :roomNumber AND h.bedNumber = :bedNumber AND h.status = 'EN_COURS'")
    Optional<HospitalizationEntity> findActiveByBed(@Param("roomNumber") String roomNumber, @Param("bedNumber") String bedNumber);
}
