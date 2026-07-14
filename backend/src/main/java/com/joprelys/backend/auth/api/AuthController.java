package com.joprelys.backend.auth.api;

import com.joprelys.backend.auth.application.AuthenticationOutcome;
import com.joprelys.backend.auth.application.AuthenticationService;
import com.joprelys.backend.auth.security.BearerTokenResolver;
import com.joprelys.backend.auth.session.application.InvalidAuthSessionException;
import com.joprelys.backend.auth.session.application.IssuedAuthSession;
import com.joprelys.backend.auth.session.application.RefreshAuthSessionUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final RefreshAuthSessionUseCase refreshAuthSessionUseCase;
    private final BearerTokenResolver bearerTokenResolver;
    private final SessionClientMetadataFactory metadataFactory;
    private final RefreshTokenCookieManager cookieManager;
    private final AuthResponseMapper responseMapper;

    public AuthController(
            AuthenticationService authenticationService,
            RefreshAuthSessionUseCase refreshAuthSessionUseCase,
            BearerTokenResolver bearerTokenResolver,
            SessionClientMetadataFactory metadataFactory,
            RefreshTokenCookieManager cookieManager,
            AuthResponseMapper responseMapper) {
        this.authenticationService = authenticationService;
        this.refreshAuthSessionUseCase = refreshAuthSessionUseCase;
        this.bearerTokenResolver = bearerTokenResolver;
        this.metadataFactory = metadataFactory;
        this.cookieManager = cookieManager;
        this.responseMapper = responseMapper;
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        AuthenticationOutcome outcome = authenticationService.login(
                request,
                clientIp(servletRequest),
                metadataFactory.from(servletRequest));
        writeCookieWhenAuthenticated(outcome, servletResponse);
        return responseMapper.toResponse(outcome);
    }

    @PostMapping("/verify-otp")
    public LoginResponse verifyOtp(
            @Valid @RequestBody VerifyStaffOtpRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        AuthenticationOutcome outcome = authenticationService.verifyStaffOtp(
                request,
                clientIp(servletRequest),
                metadataFactory.from(servletRequest));
        writeCookieWhenAuthenticated(outcome, servletResponse);
        return responseMapper.toResponse(outcome);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        String refreshToken = cookieManager.read(servletRequest)
                .orElseThrow(InvalidAuthSessionException::new);
        try {
            IssuedAuthSession session = refreshAuthSessionUseCase.refresh(
                    refreshToken,
                    metadataFactory.from(servletRequest));
            cookieManager.write(servletResponse, session);
            return responseMapper.toResponse(session);
        } catch (InvalidAuthSessionException exception) {
            cookieManager.clear(servletResponse);
            throw exception;
        }
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String token = bearerTokenResolver.resolve(request)
                .orElseThrow(() -> new MissingBearerTokenException("Missing bearer token"));
        authenticationService.logout(token);
        cookieManager.clear(response);
    }

    private void writeCookieWhenAuthenticated(
            AuthenticationOutcome outcome,
            HttpServletResponse response) {
        if (outcome.session() != null) {
            cookieManager.write(response, outcome.session());
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
