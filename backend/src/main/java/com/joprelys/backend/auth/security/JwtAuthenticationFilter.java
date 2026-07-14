package com.joprelys.backend.auth.security;

import com.joprelys.backend.auth.rbac.RbacAuthorityService;
import com.joprelys.backend.auth.session.application.AccessTokenSessionValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final BearerTokenResolver bearerTokenResolver;
    private final JwtService jwtService;
    private final AccessTokenSessionValidator accessTokenSessionValidator;
    private final RbacAuthorityService rbacAuthorityService;

    public JwtAuthenticationFilter(
            BearerTokenResolver bearerTokenResolver,
            JwtService jwtService,
            AccessTokenSessionValidator accessTokenSessionValidator,
            RbacAuthorityService rbacAuthorityService) {
        this.bearerTokenResolver = bearerTokenResolver;
        this.jwtService = jwtService;
        this.accessTokenSessionValidator = accessTokenSessionValidator;
        this.rbacAuthorityService = rbacAuthorityService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            if (bearerTokenResolver.resolve(request).map(token -> authenticate(token, response)).orElse(true)) {
                filterChain.doFilter(request, response);
            }
        } finally {
            TenantContext.clear();
        }
    }

    private boolean authenticate(String token, HttpServletResponse response) {
        try {
            JwtClaims claims = jwtService.parseAndValidate(token);
            if (!accessTokenSessionValidator.isValid(claims)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }

            Optional<UUID> userId = parseUuid(claims.subject());
            UUID claimedOrganizationId = parseUuid(claims.organizationId()).orElse(null);
            Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
            UUID effectiveOrganizationId = claimedOrganizationId;

            if (userId.isPresent()) {
                Optional<RbacAuthorityService.ResolvedAuthorities> resolved =
                        rbacAuthorityService.resolve(userId.get(), claimedOrganizationId);
                if (resolved.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return false;
                }
                RbacAuthorityService.ResolvedAuthorities access = resolved.get();
                access.roles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
                access.permissions().forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
                effectiveOrganizationId = access.organizationId();
            } else {
                Arrays.stream(claims.role().split(","))
                        .map(String::trim)
                        .filter(role -> !role.isBlank())
                        .forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            }

            var authentication = new UsernamePasswordAuthenticationToken(claims.email(), token, authorities);
            authentication.setDetails(claims);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            if (effectiveOrganizationId != null) {
                TenantContext.setTenantId(effectiveOrganizationId);
            }
            return true;
        } catch (InvalidTokenException exception) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }

    private static Optional<UUID> parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
