package com.joprelys.backend.prescription.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class PrescriptionNumberGenerator {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
	private final JdbcTemplate jdbcTemplate;

	public PrescriptionNumberGenerator(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public synchronized String generateNextPrescriptionNumber() {
		String dateStr = LocalDate.now().format(DATE_FORMATTER);
		String prefix = "ORD-" + dateStr + "-";

		String sql = "SELECT COUNT(*) FROM prescriptions WHERE prescription_number LIKE ?";
		Long count = jdbcTemplate.queryForObject(sql, Long.class, prefix + "%");
		long nextSeq = (count != null ? count : 0L) + 1;

		return String.format("%s%06d", prefix, nextSeq);
	}
}
