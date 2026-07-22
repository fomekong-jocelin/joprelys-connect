package com.joprelys.backend.spatial.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BedStateChangeRepository extends JpaRepository<BedStateChangeEntity, UUID> {

    List<BedStateChangeEntity> findByBedIdOrderByOccurredAtDesc(UUID bedId);
}
