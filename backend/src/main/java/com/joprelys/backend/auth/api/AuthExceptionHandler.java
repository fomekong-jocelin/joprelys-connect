package com.joprelys.backend.auth.api;

import com.joprelys.backend.auth.security.InvalidTokenException;
import com.joprelys.backend.auth.session.application.AuthSessionAccessDeniedException;
import com.joprelys.backend.auth.session.application.AuthSessionNotFoundException;
import com.joprelys.backend.auth.session.application.InvalidAuthSessionException;
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

    @ExceptionHandler(InvalidAuthSessionException.class)
    ProblemDetail invalidSession() {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Invalid authentication session");
        problem.setDetail("AUTH_SESSION_INVALID");
        return problem;
    }

    @ExceptionHandler(AuthSessionNotFoundException.class)
    ProblemDetail sessionNotFound() {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Authentication session not found");
        problem.setDetail("AUTH_SESSION_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(AuthSessionAccessDeniedException.class)
    ProblemDetail sessionAccessDenied() {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Authentication session access denied");
        problem.setDetail("ACCESS_DENIED");
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
