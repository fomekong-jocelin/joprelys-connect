package com.joprelys.backend.auth.session.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RefreshTokenSecurityTest {

    private final SecureRandomRefreshTokenGenerator generator = new SecureRandomRefreshTokenGenerator();
    private final Sha256RefreshTokenHasher hasher = new Sha256RefreshTokenHasher();

    @Test
    void shouldGenerateDistinctBase64UrlTokensWithAtLeast256BitsOfEntropy() {
        String first = generator.generate();
        String second = generator.generate();

        assertNotEquals(first, second);
        assertTrue(first.length() >= 43);
        assertTrue(first.matches("[A-Za-z0-9_-]+"));
    }

    @Test
    void shouldPersistOnlyStableSha256HexDigest() {
        String firstHash = hasher.hash("opaque-token");
        String secondHash = hasher.hash("opaque-token");

        assertEquals(firstHash, secondHash);
        assertEquals(64, firstHash.length());
        assertTrue(firstHash.matches("[0-9a-f]{64}"));
        assertNotEquals("opaque-token", firstHash);
    }
}
