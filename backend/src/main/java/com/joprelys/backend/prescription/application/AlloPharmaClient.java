package com.joprelys.backend.prescription.application;

import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Simulated AllôPharma teletransmission. Only the prescription number is logged: the prescription content
 * (patient, medicines, dosages) is health data and never goes to the logs (SEC-11).
 */
@Component
public class AlloPharmaClient {

	private static final Logger log = LoggerFactory.getLogger(AlloPharmaClient.class);

	public boolean transmit(PrescriptionEntity prescription) {
		log.info("Simulating teletransmission to AllôPharma for prescription: {}", prescription.getPrescriptionNumber());

		// Simulate success / failure
		if (prescription.getPrescriptionNumber() != null && prescription.getPrescriptionNumber().endsWith("999")) {
			log.warn("Simulated network/API failure for AllôPharma transmission");
			return false;
		}

		log.info("AllôPharma Teletransmission SUCCESS");
		return true;
	}
}
