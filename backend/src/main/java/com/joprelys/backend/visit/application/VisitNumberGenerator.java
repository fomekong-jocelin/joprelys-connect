package com.joprelys.backend.visit.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class VisitNumberGenerator {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
	private final JdbcTemplate jdbcTemplate;

	public VisitNumberGenerator(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public synchronized String generateNextVisitNumber() {
		String dateStr = LocalDate.now().format(DATE_FORMATTER);
		String visitPrefix = "VIS-" + dateStr + "-";

		// Utilisation de JdbcTemplate pour contourner le filtre @TenantId de Hibernate et compter au niveau global
		String sql = "SELECT COUNT(*) FROM visits WHERE visit_number LIKE ?";
		Long count = jdbcTemplate.queryForObject(sql, Long.class, visitPrefix + "%");
		long nextSeq = (count != null ? count : 0L) + 1;

		return String.format("%s%06d", visitPrefix, nextSeq);
	}
}
