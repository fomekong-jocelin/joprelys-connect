package com.joprelys.backend.audit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

	List<AuditLogEntity> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

	List<AuditLogEntity> findByActorOrganizationIdOrderByCreatedAtDesc(UUID actorOrganizationId);
}
