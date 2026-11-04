package com.joprelys.backend.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.joprelys.backend.auth.rbac.RbacAuthorityService;
import com.joprelys.backend.auth.session.application.AccessTokenSessionValidator;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/** SEC-07 : l'identité d'un jeton professionnel se résout par son « sub », jamais par e-mail. */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterIdentityTest {

    private static final String TOKEN = "signed-token";

    @Mock private JwtService jwtService;
    @Mock private AccessTokenSessionValidator sessionValidator;
    @Mock private RbacAuthorityService rbacAuthorityService;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(new BearerTokenResolver(), jwtService, sessionValidator, rbacAuthorityService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tokenWhoseSubjectIsNotAnAccountIdIsRejectedWithoutEmailFallback() throws Exception {
        givenValidTokenWithSubject("medecin@joprelys.local");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(authenticatedRequest(), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).as("la requête ne doit pas atteindre l'application").isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(rbacAuthorityService);
    }

    @Test
    void tokenWhoseSubjectIsAnUnknownAccountIsRejected() throws Exception {
        UUID unknownUser = UUID.randomUUID();
        givenValidTokenWithSubject(unknownUser.toString());
        when(rbacAuthorityService.resolve(any(), any())).thenReturn(Optional.empty());

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(authenticatedRequest(), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    private void givenValidTokenWithSubject(String subject) {
        JwtClaims claims = new JwtClaims(subject, "medecin@joprelys.local", "Dr. Test", "MEDECIN",
                UUID.randomUUID().toString(), "jti-1", "sid-1", Instant.now().plusSeconds(600));
        when(jwtService.parseAndValidate(TOKEN)).thenReturn(claims);
        when(sessionValidator.isValid(claims)).thenReturn(true);
    }

    private static MockHttpServletRequest authenticatedRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/patients");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        return request;
    }
}
