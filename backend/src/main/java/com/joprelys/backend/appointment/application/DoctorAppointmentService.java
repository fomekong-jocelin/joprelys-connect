package com.joprelys.backend.appointment.application;

import com.joprelys.backend.appointment.api.DoctorAppointmentResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DoctorAppointmentService {

	List<DoctorAppointmentResponse> listOwn(
			UUID doctorId,
			UUID organizationId,
			Instant from,
			Instant to);
}
