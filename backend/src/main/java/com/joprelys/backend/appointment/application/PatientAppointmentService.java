package com.joprelys.backend.appointment.application;

import com.joprelys.backend.appointment.api.AppointmentResponse;
import com.joprelys.backend.appointment.api.CancelAppointmentRequest;
import com.joprelys.backend.appointment.api.DoctorDirectoryEntry;
import com.joprelys.backend.appointment.api.PatientBookAppointmentRequest;
import com.joprelys.backend.appointment.api.SlotResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;

/** Cas d'usage du portail patient pour les rendez-vous. */
public interface PatientAppointmentService {

	List<DoctorDirectoryEntry> listDoctors(Authentication authentication, String specialty, String department);

	List<SlotResponse> listSlots(
			Authentication authentication,
			UUID doctorId,
			Instant from,
			Instant to);

	AppointmentResponse book(Authentication authentication, PatientBookAppointmentRequest request);

	List<AppointmentResponse> listOwn(Authentication authentication, Instant from, Instant to);

	AppointmentResponse cancel(
			Authentication authentication,
			UUID appointmentId,
			CancelAppointmentRequest request);
}
