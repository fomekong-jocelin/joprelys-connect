package com.joprelys.backend.appointment.api;

import com.joprelys.backend.appointment.application.DoctorAppointmentApiException;
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

/** Garantit les erreurs structurées du contrat d'agenda médecin. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DoctorAppointmentExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(DoctorAppointmentExceptionHandler.class);

	@ExceptionHandler(DoctorAppointmentApiException.class)
	ResponseEntity<ApiErrorResponse> handleDoctorAppointmentApiException(DoctorAppointmentApiException ex) {
		String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
		if (traceId == null) {
			traceId = "trc_unknown";
		}
		log.warn("[trace_id={}] Doctor appointment error: {}", traceId, ex.getCode());
		return ResponseEntity.status(ex.getStatus())
				.body(new ApiErrorResponse(ex.getCode(), ex.getMessage(), traceId));
	}
}
