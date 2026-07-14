package com.joprelys.backend.auth.api;

import com.joprelys.backend.auth.session.application.IssuedAuthSession;
import com.joprelys.backend.auth.session.config.AuthSessionProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieManager {

    private final AuthSessionProperties properties;
    private final Clock clock;

    public RefreshTokenCookieManager(AuthSessionProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        if ("None".equals(properties.refreshCookieSameSite()) && !properties.refreshCookieSecure()) {
            throw new IllegalStateException("SameSite=None requires a Secure refresh cookie");
        }
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> properties.refreshCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    public void write(HttpServletResponse response, IssuedAuthSession session) {
        long maxAgeSeconds = Math.max(
                0,
                Duration.between(clock.instant(), session.sessionExpiresAt()).getSeconds());
        ResponseCookie cookie = baseCookie(session.refreshToken())
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clear(HttpServletResponse response) {
        ResponseCookie cookie = baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(properties.refreshCookieName(), value)
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite(properties.refreshCookieSameSite())
                .path(properties.refreshCookiePath());
    }
}
