package com.joprelys.backend.patient.application;

import com.joprelys.backend.patient.infrastructure.persistence.ExternalAccessRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class ExternalAccessExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExternalAccessExpirationScheduler.class);
    private final ExternalAccessRequestRepository externalAccessRequestRepository;

    public ExternalAccessExpirationScheduler(ExternalAccessRequestRepository externalAccessRequestRepository) {
        this.externalAccessRequestRepository = externalAccessRequestRepository;
    }

    @Scheduled(fixedDelayString = "${app.expiration-check-interval-ms:60000}")
    @Transactional
    public void expireAccessRequests() {
        log.debug("Checking for expired external access requests...");
        int updated = externalAccessRequestRepository.expireRequests(Instant.now());
        if (updated > 0) {
            log.info("Expired {} external access requests automatically.", updated);
        }
    }
}
