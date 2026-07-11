package com.joprelys.backend.emergency.medicolegal.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmergencyBelongingRepository extends JpaRepository<EmergencyBelongingEntity, UUID> {
    List<EmergencyBelongingEntity> findByEmergencyIdOrderByCreatedAtAsc(UUID emergencyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from EmergencyBelongingEntity b where b.id = :id")
    Optional<EmergencyBelongingEntity> findByIdForUpdate(@Param("id") UUID id);
}
