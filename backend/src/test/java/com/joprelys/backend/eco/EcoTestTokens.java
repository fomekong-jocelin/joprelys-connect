package com.joprelys.backend.eco;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Émet de vrais jetons RS256 signés, au format de Joprelys ID, avec une clé générée pour les tests.
 * Le décodeur de test valide signature, expiration, émetteur et audience comme en production.
 */
public final class EcoTestTokens {

    public static final String ISSUER = "https://joprelys-id.test/realms/joprelys-id";
    public static final String AUDIENCE = "joprelys-api";

    private static final KeyPair KEYS = generate();
    private static final KeyPair OTHER_KEYS = generate();

    private EcoTestTokens() {
    }

    public static RSAPublicKey publicKey() {
        return (RSAPublicKey) KEYS.getPublic();
    }

    /** Jeton patient émis pour le mobile AllôPharma après consentement (scope eco:patient-link). */
    public static String patient(String dpu) {
        return token(claims -> claims
                .claim("azp", "allopharma-mobile")
                .claim("scope", "openid eco:patient-link")
                .claim("dpu", dpu));
    }

    /** Jeton du compte de service AllôPharma (client credentials, scope eco:consent.revoke). */
    public static String server() {
        return token(claims -> claims
                .subject(UUID.randomUUID().toString())
                .claim("azp", "allopharma-server")
                .claim("scope", "eco:consent.revoke"));
    }

    public static String token(Consumer<JwtClaimsSet.Builder> customizer) {
        return sign(KEYS, customizer);
    }

    /** Même contenu, mais signé par une clé que le serveur ne connaît pas. */
    public static String signedByUnknownKey(Consumer<JwtClaimsSet.Builder> customizer) {
        return sign(OTHER_KEYS, customizer);
    }

    private static String sign(KeyPair keys, Consumer<JwtClaimsSet.Builder> customizer) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .audience(List.of(AUDIENCE))
                .subject(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300));
        customizer.accept(claims);
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keys.getPublic())
                .privateKey((RSAPrivateKey) keys.getPrivate())
                .keyID(keys == KEYS ? "test-key" : "unknown-key")
                .build();
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(rsaKey.getKeyID()).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    private static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
