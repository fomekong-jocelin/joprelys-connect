package com.joprelys.backend.patient.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class PatientNumberGenerator {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
	private final JdbcTemplate jdbcTemplate;

	public PatientNumberGenerator(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public synchronized GeneratedNumbers generateNextNumbers() {
		String dateStr = LocalDate.now().format(DATE_FORMATTER);
		String dpuPrefix = "DPU-JOP-" + dateStr + "-";
		String localPrefix = "PAT-" + dateStr + "-";

		// Utilisation de JdbcTemplate pour contourner le filtre @TenantId de Hibernate et compter au niveau global
		String sql = "SELECT COUNT(*) FROM patients WHERE global_patient_number LIKE ?";
		Long count = jdbcTemplate.queryForObject(sql, Long.class, dpuPrefix + "%");
		long nextSeq = (count != null ? count : 0L) + 1;

		String globalNumber = String.format("%s%06d", dpuPrefix, nextSeq);
		String localNumber = String.format("%s%06d", localPrefix, nextSeq);

		return new GeneratedNumbers(globalNumber, localNumber);
	}

	public record GeneratedNumbers(String globalNumber, String localNumber) {
	}
}
