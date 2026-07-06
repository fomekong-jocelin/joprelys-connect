package com.joprelys.backend.visit.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class DocumentNumberGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final JdbcTemplate jdbcTemplate;

    public DocumentNumberGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized String generateNextDocumentNumber() {
        String dateStr = LocalDate.now().format(DATE_FORMATTER);
        String docPrefix = "DOC-" + dateStr + "-";

        Long nextSeq = jdbcTemplate.queryForObject("SELECT nextval('medical_document_number_seq')", Long.class);
        long nextVal = nextSeq != null ? nextSeq : 1L;

        return String.format("%s%06d", docPrefix, nextVal);
    }
}
