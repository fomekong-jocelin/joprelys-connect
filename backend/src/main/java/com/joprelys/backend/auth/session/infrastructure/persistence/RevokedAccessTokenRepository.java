package com.joprelys.backend.auth.session.infrastructure.persistence;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessTokenEntity, String> {

    boolean existsByTokenIdAndExpiresAtAfter(String tokenId, Instant now);

    @Modifying
    long deleteByExpiresAtBefore(Instant cutoff);
}
