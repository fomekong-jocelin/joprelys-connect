package com.joprelys.backend.auth.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class JwtRevocationService {

	private final Map<String, Instant> revokedTokenIds = new ConcurrentHashMap<>();
	private final Clock clock;

	public JwtRevocationService(Clock clock) {
		this.clock = clock;
	}

	public void revoke(String tokenId, Instant expiresAt) {
		purgeExpiredTokens();
		if (expiresAt.isAfter(clock.instant())) {
			revokedTokenIds.put(tokenId, expiresAt);
		}
	}

	public boolean isRevoked(String tokenId) {
		purgeExpiredTokens();
		return revokedTokenIds.containsKey(tokenId);
	}

	private void purgeExpiredTokens() {
		Instant now = clock.instant();
		revokedTokenIds.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
	}
}
