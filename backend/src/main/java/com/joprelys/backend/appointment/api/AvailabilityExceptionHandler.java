package com.joprelys.backend.appointment.api;

import com.joprelys.backend.appointment.application.AvailabilityApiException;
import com.joprelys.backend.common.api.ApiErrorResponse;
import com.joprelys.backend.common.api.TraceIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Gestionnaire dédié aux erreurs métier des disponibilités médecin.
 *
 * <p>Prioritaire sur les advices globaux ({@link com.joprelys.backend.common.api.GlobalExceptionHandler}
 * et {@code AuthExceptionHandler}) afin de garantir les codes d'erreur du contrat §2 : le format
 * {@code ProblemDetail} l'emporterait sinon pour certaines exceptions.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AvailabilityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(AvailabilityExceptionHandler.class);

	@ExceptionHandler(AvailabilityApiException.class)
	ResponseEntity<ApiErrorResponse> handleAvailabilityApiException(AvailabilityApiException ex) {
		String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
		if (traceId == null) {
			traceId = "trc_unknown";
		}
		log.warn("[trace_id={}] Availability error: {} - {}", traceId, ex.getCode(), ex.getMessage());
		return ResponseEntity.status(ex.getStatus())
				.body(new ApiErrorResponse(ex.getCode(), ex.getMessage(), traceId));
	}
}
