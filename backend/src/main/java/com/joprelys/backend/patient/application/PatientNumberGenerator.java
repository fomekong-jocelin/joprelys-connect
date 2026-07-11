package com.joprelys.backend.patient.application;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

        String sql = "SELECT COUNT(*) FROM patients WHERE global_patient_number LIKE ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, dpuPrefix + "%");
        long nextSeq = (count != null ? count : 0L) + 1;

        String globalNumber = String.format("%s%06d", dpuPrefix, nextSeq);
        String localNumber = String.format("%s%06d", localPrefix, nextSeq);

        return new GeneratedNumbers(globalNumber, localNumber);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized String generateTemporaryPatientNumber() {
        String dateStr = LocalDate.now().format(DATE_FORMATTER);
        String prefix = "URG-TEMP-" + dateStr + "-";
        String sql = "SELECT COUNT(*) FROM patients WHERE temporary_patient_number LIKE ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, prefix + "%");
        long nextSeq = (count != null ? count : 0L) + 1;
        return String.format("%s%06d", prefix, nextSeq);
    }

    public record GeneratedNumbers(String globalNumber, String localNumber) {
    }
}
