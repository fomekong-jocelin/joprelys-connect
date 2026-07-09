package com.joprelys.backend.spatial.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface WardRepository extends JpaRepository<WardEntity, UUID> {
}
