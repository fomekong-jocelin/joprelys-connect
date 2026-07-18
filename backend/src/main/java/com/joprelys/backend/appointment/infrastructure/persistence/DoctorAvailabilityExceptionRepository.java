package com.joprelys.backend.appointment.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorAvailabilityExceptionRepository extends JpaRepository<DoctorAvailabilityExceptionEntity, UUID> {

	List<DoctorAvailabilityExceptionEntity> findByDoctorIdAndStartAtLessThanAndEndAtGreaterThan(
			UUID doctorId, Instant periodEnd, Instant periodStart);
}
