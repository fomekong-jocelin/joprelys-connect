package com.joprelys.backend.ai.ambient.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface AmbientAudioChunkRepository extends JpaRepository<AmbientAudioChunkEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AmbientAudioChunkEntity> findByVisitIdAndChunkId(UUID visitId, String chunkId);
}
