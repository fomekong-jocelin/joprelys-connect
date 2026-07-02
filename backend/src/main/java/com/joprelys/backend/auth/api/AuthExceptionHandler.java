package com.joprelys.backend.auth.api;

import com.joprelys.backend.auth.security.InvalidTokenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

	@ExceptionHandler(BadCredentialsException.class)
	ProblemDetail badCredentials() {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
		problem.setTitle("Authentication failed");
		problem.setDetail("Invalid email or password");
		return problem;
	}

	@ExceptionHandler({InvalidTokenException.class, MissingBearerTokenException.class})
	ProblemDetail invalidToken() {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
		problem.setTitle("Invalid authentication token");
		problem.setDetail("Authentication is required");
		return problem;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail validationFailure(MethodArgumentNotValidException ex) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		problem.setTitle("Invalid request");
		String detail = ex.getBindingResult().getFieldErrors().stream()
				.map(org.springframework.validation.FieldError::getDefaultMessage)
				.collect(java.util.stream.Collectors.joining(", "));
		problem.setDetail(detail.isEmpty() ? "Request validation failed" : detail);
		return problem;
	}

	@ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
	ProblemDetail responseStatusFailure(org.springframework.web.server.ResponseStatusException ex) {
		ProblemDetail problem = ProblemDetail.forStatus(ex.getStatusCode());
		problem.setTitle(ex.getReason() != null ? ex.getReason() : "Error occurred");
		problem.setDetail(ex.getReason());
		return problem;
	}
}
