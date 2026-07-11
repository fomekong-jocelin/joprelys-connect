package com.joprelys.backend.auth.api;

import com.joprelys.backend.auth.application.AuthenticationService;
import com.joprelys.backend.auth.security.BearerTokenResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthenticationService authenticationService;
	private final BearerTokenResolver bearerTokenResolver;

	public AuthController(AuthenticationService authenticationService, BearerTokenResolver bearerTokenResolver) {
		this.authenticationService = authenticationService;
		this.bearerTokenResolver = bearerTokenResolver;
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
		return authenticationService.login(request, clientIp(servletRequest));
	}

	@PostMapping("/verify-otp")
	public LoginResponse verifyOtp(@Valid @RequestBody VerifyStaffOtpRequest request, HttpServletRequest servletRequest) {
		return authenticationService.verifyStaffOtp(request, clientIp(servletRequest));
	}

	@GetMapping("/me")
	public CurrentSessionResponse currentSession(Authentication authentication) {
		return authenticationService.currentSession(authentication);
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpServletRequest request) {
		String token = bearerTokenResolver.resolve(request)
				.orElseThrow(() -> new MissingBearerTokenException("Missing bearer token"));
		authenticationService.logout(token);
	}

	private static String clientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
