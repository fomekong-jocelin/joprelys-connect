package com.joprelys.backend.appointment.infrastructure.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<AppointmentEntity, UUID> {

	List<AppointmentEntity> findByDoctorIdAndStartAtBetweenOrderByStartAtAsc(UUID doctorId, Instant from, Instant to);

	List<AppointmentEntity> findByStartAtBetweenOrderByStartAtAsc(Instant from, Instant to);

	List<AppointmentEntity> findByPatientIdOrderByStartAtDesc(UUID patientId);

	boolean existsByDoctorIdAndStartAtAndStatusIn(UUID doctorId, Instant startAt, Collection<AppointmentStatus> statuses);

	List<AppointmentEntity> findByDoctorIdAndStartAtBetweenAndStatusIn(
			UUID doctorId, Instant from, Instant to, Collection<AppointmentStatus> statuses);

	List<AppointmentEntity> findByDoctorIdAndStartAtGreaterThanEqualAndStatusIn(
			UUID doctorId, Instant from, Collection<AppointmentStatus> statuses);
}
