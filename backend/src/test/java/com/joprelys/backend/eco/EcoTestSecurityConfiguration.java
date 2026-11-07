package com.joprelys.backend.eco;

import com.joprelys.backend.eco.config.EcoJwtValidators;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** Remplace le téléchargement des clés Joprelys ID par la clé de test ; mêmes validateurs qu'en production. */
@TestConfiguration
public class EcoTestSecurityConfiguration {

    @Bean
    @Primary
    JwtDecoder testEcoJwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(EcoTestTokens.publicKey()).build();
        decoder.setJwtValidator(EcoJwtValidators.create(EcoTestTokens.ISSUER, EcoTestTokens.AUDIENCE));
        return decoder;
    }
}
