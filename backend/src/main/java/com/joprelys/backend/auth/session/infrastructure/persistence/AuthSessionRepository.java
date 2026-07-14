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

    @Query("""
            select session
            from AuthSessionEntity session
            join fetch session.user
            where session.id = :sessionId
            """)
    Optional<AuthSessionEntity> findByIdWithUser(@Param("sessionId") UUID sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session
            from AuthSessionEntity session
            join fetch session.user
            where session.id = :sessionId
            """)
    Optional<AuthSessionEntity> findByIdForUpdate(@Param("sessionId") UUID sessionId);

    @Query("""
            select session
            from AuthSessionEntity session
            join fetch session.user
            where session.user.id = :userId
            order by session.createdAt desc
            """)
    List<AuthSessionEntity> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session
            from AuthSessionEntity session
            join fetch session.user
            where session.user.id = :userId
            order by session.createdAt asc
            """)
    List<AuthSessionEntity> findByUserIdForUpdate(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session
            from AuthSessionEntity session
            join fetch session.user
            where session.tokenFamilyId = :tokenFamilyId
            order by session.createdAt asc
            """)
    List<AuthSessionEntity> findByTokenFamilyIdForUpdate(@Param("tokenFamilyId") UUID tokenFamilyId);

    List<AuthSessionEntity> findByTokenFamilyIdOrderByCreatedAtAsc(UUID tokenFamilyId);

    @Modifying
    long deleteByAbsoluteExpiresAtBefore(Instant cutoff);
}
