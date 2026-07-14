package com.joprelys.backend.auth.session.api;

import com.joprelys.backend.auth.api.RefreshTokenCookieManager;
import com.joprelys.backend.auth.session.application.ListAuthSessionsUseCase;
import com.joprelys.backend.auth.session.application.LogoutAllSessionsUseCase;
import com.joprelys.backend.auth.session.application.RevokeAuthSessionUseCase;
import com.joprelys.backend.auth.session.application.SessionActor;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthSessionController {

    private final ListAuthSessionsUseCase listUseCase;
    private final RevokeAuthSessionUseCase revokeUseCase;
    private final LogoutAllSessionsUseCase logoutAllUseCase;
    private final SessionActorFactory actorFactory;
    private final AuthSessionResponseMapper responseMapper;
    private final RefreshTokenCookieManager cookieManager;

    public AuthSessionController(
            ListAuthSessionsUseCase listUseCase,
            RevokeAuthSessionUseCase revokeUseCase,
            LogoutAllSessionsUseCase logoutAllUseCase,
            SessionActorFactory actorFactory,
            AuthSessionResponseMapper responseMapper,
            RefreshTokenCookieManager cookieManager) {
        this.listUseCase = listUseCase;
        this.revokeUseCase = revokeUseCase;
        this.logoutAllUseCase = logoutAllUseCase;
        this.actorFactory = actorFactory;
        this.responseMapper = responseMapper;
        this.cookieManager = cookieManager;
    }

    @GetMapping("/sessions")
    public List<AuthSessionResponse> listOwn(Authentication authentication) {
        SessionActor actor = actorFactory.from(authentication);
        return responseMapper.toResponses(listUseCase.listOwn(actor));
    }

    @GetMapping("/users/{userId}/sessions")
    @PreAuthorize("hasAuthority('AUTH_SESSION_MANAGE')")
    public List<AuthSessionResponse> listUser(
            @PathVariable UUID userId,
            Authentication authentication) {
        SessionActor actor = actorFactory.from(authentication);
        return responseMapper.toResponses(listUseCase.listUser(actor, userId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(
            @PathVariable UUID sessionId,
            Authentication authentication,
            HttpServletResponse response) {
        SessionActor actor = actorFactory.from(authentication);
        revokeUseCase.revoke(actor, sessionId);
        if (Objects.equals(actor.currentSessionId(), sessionId)) {
            cookieManager.clear(response);
        }
    }

    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(Authentication authentication, HttpServletResponse response) {
        logoutAllUseCase.logoutAll(actorFactory.from(authentication));
        cookieManager.clear(response);
    }
}
