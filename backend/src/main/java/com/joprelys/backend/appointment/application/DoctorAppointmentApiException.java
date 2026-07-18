package com.joprelys.backend.appointment.application;

import org.springframework.http.HttpStatus;

/** Exception métier stable du parcours d'agenda médecin. */
public class DoctorAppointmentApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	public DoctorAppointmentApiException(HttpStatus status, String code, String message) {
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
