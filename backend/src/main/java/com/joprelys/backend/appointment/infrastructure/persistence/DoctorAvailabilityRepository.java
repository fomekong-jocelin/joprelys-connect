package com.joprelys.backend.appointment.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailabilityEntity, UUID> {

	List<DoctorAvailabilityEntity> findByDoctorIdAndWeekdayAndActiveTrue(UUID doctorId, Integer weekday);

	List<DoctorAvailabilityEntity> findByDoctorIdAndActiveTrue(UUID doctorId);

	List<DoctorAvailabilityEntity> findByDoctorIdOrderByWeekdayAscStartTimeAsc(UUID doctorId);
}
