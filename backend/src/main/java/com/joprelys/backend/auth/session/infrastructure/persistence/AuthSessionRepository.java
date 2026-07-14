package com.joprelys.backend.auth.session.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthSessionRepository extends JpaRepository<AuthSessionEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session
            from AuthSessionEntity session
            join fetch session.user
            where session.refreshTokenHash = :refreshTokenHash
            """)
    Optional<AuthSessionEntity> findByRefreshTokenHashForUpdate(
            @Param("refreshTokenHash") String refreshTokenHash);

    List<AuthSessionEntity> findByTokenFamilyIdOrderByCreatedAtAsc(UUID tokenFamilyId);

    @Modifying
    long deleteByAbsoluteExpiresAtBefore(Instant cutoff);
}
