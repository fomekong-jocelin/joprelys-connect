package com.joprelys.backend.emergency.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ResuscitationLogRepository extends JpaRepository<ResuscitationLogEntity, UUID> {
	List<ResuscitationLogEntity> findByEmergencyId(UUID emergencyId);
}
