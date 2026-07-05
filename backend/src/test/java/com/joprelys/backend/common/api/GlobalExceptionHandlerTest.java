package com.joprelys.backend.common.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void givenResponseStatusException_whenHandle_thenReturnsErrorWithTraceId() {
        org.slf4j.MDC.put(TraceIdFilter.TRACE_ID_MDC_KEY, "trc_test_123");
        try {
            ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable.");

            ResponseEntity<ApiErrorResponse> response = handler.handleResponseStatus(ex);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("NOT_FOUND", response.getBody().error().code());
            assertEquals("Patient introuvable.", response.getBody().error().message());
            assertEquals("trc_test_123", response.getBody().error().trace_id());
        } finally {
            org.slf4j.MDC.remove(TraceIdFilter.TRACE_ID_MDC_KEY);
        }
    }

    @Test
    void givenBadRequest_whenHandle_thenReturnsBadRequestCode() {
        org.slf4j.MDC.put(TraceIdFilter.TRACE_ID_MDC_KEY, "trc_test_456");
        try {
            ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paramètre invalide.");

            ResponseEntity<ApiErrorResponse> response = handler.handleResponseStatus(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("BAD_REQUEST", response.getBody().error().code());
            assertEquals("trc_test_456", response.getBody().error().trace_id());
        } finally {
            org.slf4j.MDC.remove(TraceIdFilter.TRACE_ID_MDC_KEY);
        }
    }

    @Test
    void givenValidationError_whenHandle_thenReturnsValidationErrorCode() {
        org.slf4j.MDC.put(TraceIdFilter.TRACE_ID_MDC_KEY, "trc_test_789");
        try {
            var target = new Object();
            var bindingResult = new BeanPropertyBindingResult(target, "target");
            bindingResult.addError(new FieldError("target", "email", "Email invalide"));
            bindingResult.addError(new FieldError("target", "name", "Nom requis"));
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

            ResponseEntity<ApiErrorResponse> response = handler.handleValidation(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("VALIDATION_ERROR", response.getBody().error().code());
            assertTrue(response.getBody().error().message().contains("Email invalide"));
            assertTrue(response.getBody().error().message().contains("Nom requis"));
            assertEquals("trc_test_789", response.getBody().error().trace_id());
        } finally {
            org.slf4j.MDC.remove(TraceIdFilter.TRACE_ID_MDC_KEY);
        }
    }

    @Test
    void givenAccessDenied_whenHandle_thenReturnsAccessDeniedCode() {
        org.slf4j.MDC.put(TraceIdFilter.TRACE_ID_MDC_KEY, "trc_test_abc");
        try {
            AccessDeniedException ex = new AccessDeniedException("Accès refusé");

            ResponseEntity<ApiErrorResponse> response = handler.handleAccessDenied(ex);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals("ACCESS_DENIED", response.getBody().error().code());
            assertEquals("Vous n'êtes pas autorisé à effectuer cette action.", response.getBody().error().message());
            assertEquals("trc_test_abc", response.getBody().error().trace_id());
        } finally {
            org.slf4j.MDC.remove(TraceIdFilter.TRACE_ID_MDC_KEY);
        }
    }

    @Test
    void givenNotFound_whenHandle_thenReturnsNotFoundCode() {
        org.slf4j.MDC.put(TraceIdFilter.TRACE_ID_MDC_KEY, "trc_test_def");
        try {
            NoSuchElementException ex = new NoSuchElementException("Élément manquant");

            ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(ex);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("NOT_FOUND", response.getBody().error().code());
            assertEquals("Élément manquant", response.getBody().error().message());
            assertEquals("trc_test_def", response.getBody().error().trace_id());
        } finally {
            org.slf4j.MDC.remove(TraceIdFilter.TRACE_ID_MDC_KEY);
        }
    }

    @Test
    void givenGenericException_whenHandle_thenReturnsInternalErrorCode() {
        org.slf4j.MDC.put(TraceIdFilter.TRACE_ID_MDC_KEY, "trc_test_ghi");
        try {
            RuntimeException ex = new RuntimeException("Boom");

            ResponseEntity<ApiErrorResponse> response = handler.handleGeneric(ex);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("INTERNAL_ERROR", response.getBody().error().code());
            assertEquals("Une erreur interne est survenue. Veuillez réessayer plus tard.", response.getBody().error().message());
            assertEquals("trc_test_ghi", response.getBody().error().trace_id());
        } finally {
            org.slf4j.MDC.remove(TraceIdFilter.TRACE_ID_MDC_KEY);
        }
    }

    @Test
    void givenNoTraceIdInMdc_whenHandle_thenReturnsUnknownTraceId() {
        org.slf4j.MDC.remove(TraceIdFilter.TRACE_ID_MDC_KEY);
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.CONFLICT, "Conflit.");

        ResponseEntity<ApiErrorResponse> response = handler.handleResponseStatus(ex);

        assertEquals("trc_unknown", response.getBody().error().trace_id());
    }
}
