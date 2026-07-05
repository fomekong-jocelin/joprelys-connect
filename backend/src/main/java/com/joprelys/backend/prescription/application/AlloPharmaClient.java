package com.joprelys.backend.prescription.application;

import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class AlloPharmaClient {

	private static final Logger log = LoggerFactory.getLogger(AlloPharmaClient.class);

	public boolean transmit(PrescriptionEntity prescription) {
		log.info("Simulating teletransmission to AllôPharma for prescription: {}", prescription.getPrescriptionNumber());

		// Building simulated JSON payload
		String drugItemsJson = prescription.getItems().stream()
				.map(item -> String.format("{\"drugName\":\"%s\",\"dosage\":\"%s\",\"posology\":\"%s\",\"quantity\":\"%s\"}",
						escapeJson(item.getDrugName()),
						escapeJson(item.getDosage()),
						escapeJson(item.getPosology()),
						escapeJson(item.getQuantity())))
				.collect(Collectors.joining(","));

		String payload = String.format(
				"{\"prescriptionNumber\":\"%s\",\"doctorName\":\"%s\",\"organizationId\":\"%s\",\"items\":[%s]}",
				escapeJson(prescription.getPrescriptionNumber()),
				escapeJson(prescription.getConsultation().getDoctor().getDisplayName()),
				prescription.getOrganizationId(),
				drugItemsJson
		);

		log.info("AllôPharma Payload Sent: {}", payload);

		// Simulate success / failure
		if (prescription.getPrescriptionNumber() != null && prescription.getPrescriptionNumber().endsWith("999")) {
			log.warn("Simulated network/API failure for AllôPharma transmission");
			return false;
		}

		log.info("AllôPharma Teletransmission SUCCESS");
		return true;
	}

	private String escapeJson(String value) {
		if (value == null) return "";
		return value.replace("\"", "\\\"");
	}
}
