package com.joprelys.backend.eco.config;

import com.joprelys.backend.eco.api.EcoApiPaths;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

/**
 * Chaîne de sécurité dédiée à {@code /api/eco/**} : uniquement des jetons Joprelys ID (resource server JWT).
 * Elle passe avant la chaîne maison ; les filtres maison s'abstiennent sur ces chemins.
 */
@Configuration
@EnableConfigurationProperties(EcoProperties.class)
public class EcoSecurityConfig {

    static final String SCOPE_PATIENT_LINK = "SCOPE_eco:patient-link";
    static final String SCOPE_CONSENT_REVOKE = "SCOPE_eco:consent.revoke";

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain ecoSecurityFilterChain(HttpSecurity http, JwtDecoder ecoJwtDecoder) throws Exception {
        return http
                .securityMatcher(EcoApiPaths.PATTERN)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
                        .addHeaderWriter(new StaticHeadersWriter("Content-Security-Policy",
                                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'")))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, EcoApiPaths.V1 + "/patient-links").hasAuthority(SCOPE_PATIENT_LINK)
                        .requestMatchers(HttpMethod.POST, EcoApiPaths.V1 + "/consents/*/revoke").hasAuthority(SCOPE_CONSENT_REVOKE)
                        .anyRequest().denyAll())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt.decoder(ecoJwtDecoder)))
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Insufficient scope")))
                .build();
    }

    /** Décodeur paresseux : les clés sont téléchargées au premier jeton, pas au démarrage. */
    @Bean
    JwtDecoder ecoJwtDecoder(EcoProperties properties) {
        EcoProperties.Identity identity = properties.identity();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(identity.jwkSetUri()).build();
        decoder.setJwtValidator(EcoJwtValidators.create(identity.issuerUri(), identity.audience()));
        return decoder;
    }
}
