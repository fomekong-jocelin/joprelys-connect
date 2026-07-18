package com.joprelys.backend.appointment.application;

import org.springframework.http.HttpStatus;

/**
 * Exception métier du module disponibilités médecin, transportant le statut HTTP et le code
 * d'erreur normalisé du contrat API (format {@code ApiErrorResponse}).
 */
public class AvailabilityApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	public AvailabilityApiException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}
}
