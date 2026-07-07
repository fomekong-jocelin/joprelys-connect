package com.joprelys.backend.prescription.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.prescription.api.PharmacyDispensationHistoryItemResponse;
import com.joprelys.backend.prescription.api.PharmacyDispensationHistoryResponse;
import com.joprelys.backend.prescription.api.PharmacyDispenseRequest;
import com.joprelys.backend.prescription.api.PharmacyDispensedItem;
import com.joprelys.backend.prescription.api.PharmacyVerifyItem;
import com.joprelys.backend.prescription.api.PharmacyVerifyResponse;
import com.joprelys.backend.prescription.infrastructure.persistence.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PharmacyService {

	private final PrescriptionRepository prescriptionRepository;
	private final PrescriptionDispensationRepository dispensationRepository;
	private final DispensationItemRepository dispensationItemRepository;
	private final AuditService auditService;
	private final JdbcTemplate jdbcTemplate;

	private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();
	private final Map<String, Instant> lockouts = new ConcurrentHashMap<>();

	public void resetLockouts() {
		failedAttempts.clear();
		lockouts.clear();
	}

	public PharmacyService(
			PrescriptionRepository prescriptionRepository,
			PrescriptionDispensationRepository dispensationRepository,
			DispensationItemRepository dispensationItemRepository,
			AuditService auditService,
			JdbcTemplate jdbcTemplate) {
		this.prescriptionRepository = prescriptionRepository;
		this.dispensationRepository = dispensationRepository;
		this.dispensationItemRepository = dispensationItemRepository;
		this.auditService = auditService;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional
	public PharmacyVerifyResponse verifyPrescription(String prescriptionNumber, String pinCode) {
		checkLockout(prescriptionNumber);

		PrescriptionEntity prescription = prescriptionRepository.findByPrescriptionNumberGlobally(prescriptionNumber)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordonnance introuvable."));

		// PIN verify
		if (prescription.getPinCode() == null || !prescription.getPinCode().equalsIgnoreCase(pinCode)) {
			handleFailedAttempt(prescriptionNumber);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Code PIN incorrect.");
		}

		// Clear failed attempts on success
		failedAttempts.remove(prescriptionNumber);

		// Expiration check
		if (prescription.getExpiresAt() != null && prescription.getExpiresAt().isBefore(Instant.now())) {
			if (!"EXPIRED".equals(prescription.getStatus())) {
				prescription.setStatus("EXPIRED");
				jdbcTemplate.update("UPDATE prescriptions SET status = 'EXPIRED', updated_at = ? WHERE id = ?", Timestamp.from(Instant.now()), prescription.getId());
			}
		}

		// Query patient and doctor names globally using raw SQL to bypass TenantId restrictions on public endpoints
		String namesSql = "SELECT p.full_name as patient_name, u.display_name as doctor_name, p.id as patient_id " +
				"FROM prescriptions pr " +
				"JOIN consultations c ON pr.consultation_id = c.id " +
				"JOIN visits v ON c.visit_id = v.id " +
				"JOIN patients p ON v.patient_id = p.id " +
				"LEFT JOIN users u ON c.doctor_id = u.id " +
				"WHERE pr.id = ?";
		Map<String, Object> names = jdbcTemplate.queryForMap(namesSql, prescription.getId());
		String patientName = (String) names.get("patient_name");
		String doctorName = names.get("doctor_name") != null ? (String) names.get("doctor_name") : "Non spécifié";
		UUID patientId = names.get("patient_id") instanceof UUID ? (UUID) names.get("patient_id") : UUID.fromString(names.get("patient_id").toString());

		List<PharmacyVerifyItem> items = prescription.getItems().stream().map(item -> {
			int quantityAlreadyDispensed = dispensationItemRepository.sumQuantityDispensedByPrescriptionItemId(item.getId());
			return new PharmacyVerifyItem(
					item.getId(),
					item.getDrugName(),
					item.getDosage(),
					"Comprimé", // fallback or value from dosage
					item.getQuantity(),
					quantityAlreadyDispensed,
					true, // substitutionAllowed
					item.getInstructions()
			);
		}).toList();

		// Audit log
		auditService.logSuccess(
				null,
				prescription.getOrganizationId(),
				patientId,
				"PRESCRIPTION",
				prescription.getId(),
				"PHARMACY_VERIFIED",
				"Vérification par pharmacie : " + prescriptionNumber
		);

		return new PharmacyVerifyResponse(
				prescription.getId(),
				prescription.getPrescriptionNumber(),
				prescription.getStatus(),
				patientName,
				doctorName,
				prescription.getCreatedAt(),
				prescription.getExpiresAt(),
				items
		);
	}

	@Transactional
	public void dispensePrescription(PharmacyDispenseRequest request) {
		checkLockout(request.prescriptionNumber());

		PrescriptionEntity prescription = prescriptionRepository.findByPrescriptionNumberGlobally(request.prescriptionNumber())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordonnance introuvable."));

		if (prescription.getPinCode() == null || !prescription.getPinCode().equalsIgnoreCase(request.pinCode())) {
			handleFailedAttempt(request.prescriptionNumber());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Code PIN incorrect.");
		}

		failedAttempts.remove(request.prescriptionNumber());

		// Status verification
		if ("FULLY_DISPENSED".equals(prescription.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette ordonnance a déjà été entièrement dispensée.");
		}
		if ("CANCELLED".equals(prescription.getStatus()) || "REVOQUE".equals(prescription.getStatus()) || "ANNULE".equals(prescription.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette ordonnance est annulée ou révoquée.");
		}
		if ("EXPIRED".equals(prescription.getStatus()) || (prescription.getExpiresAt() != null && prescription.getExpiresAt().isBefore(Instant.now()))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette ordonnance a expiré.");
		}

		String pharmacyName = request.pharmacyName();
		if (pharmacyName != null && pharmacyName.length() > 200) {
			pharmacyName = pharmacyName.substring(0, 200);
		}
		String pharmacistLicense = request.pharmacistLicense();
		if (pharmacistLicense != null && pharmacistLicense.length() > 50) {
			pharmacistLicense = pharmacistLicense.substring(0, 50);
		}

		PrescriptionDispensationEntity dispensation = new PrescriptionDispensationEntity(
				prescription,
				pharmacyName,
				pharmacistLicense
		);

		boolean allItemsFullyDispensed = true;
		boolean anyItemDispensed = false;

		for (PharmacyDispensedItem dispItem : request.dispensedItems()) {
			PrescriptionItemEntity item = prescription.getItems().stream()
					.filter(i -> i.getId().equals(dispItem.prescriptionItemId()))
					.findFirst()
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ligne d'ordonnance introuvable: " + dispItem.prescriptionItemId()));

			int qtyAlreadyDispensed = dispensationItemRepository.sumQuantityDispensedByPrescriptionItemId(item.getId());
			int qtyPrescribed = parseQuantity(item.getQuantity());

			// Si qtyPrescribed <= 0, cela signifie que la quantité n'est pas chiffrée de manière stricte (ex: "Selon besoin"), la limite n'est pas bloquante.
			if (qtyPrescribed > 0 && (qtyAlreadyDispensed + dispItem.quantityDispensed() > qtyPrescribed)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantité dispensée excède la quantité prescrite pour le médicament : " + item.getDrugName());
			}

			DispensationItemEntity itemEntity = new DispensationItemEntity(
					dispensation,
					item,
					dispItem.quantityDispensed(),
					dispItem.substitutedWith()
			);
			dispensation.getItems().add(itemEntity);

			if (qtyPrescribed > 0 && (qtyAlreadyDispensed + dispItem.quantityDispensed() < qtyPrescribed)) {
				allItemsFullyDispensed = false;
			}
			if (dispItem.quantityDispensed() > 0) {
				anyItemDispensed = true;
			}
		}

		// Also check other items not mentioned in the request
		for (PrescriptionItemEntity item : prescription.getItems()) {
			boolean requestContains = request.dispensedItems().stream()
					.anyMatch(di -> di.prescriptionItemId().equals(item.getId()));
			if (!requestContains) {
				int qtyAlreadyDispensed = dispensationItemRepository.sumQuantityDispensedByPrescriptionItemId(item.getId());
				int qtyPrescribed = parseQuantity(item.getQuantity());
				if (qtyPrescribed > 0 && qtyAlreadyDispensed < qtyPrescribed) {
					allItemsFullyDispensed = false;
				}
			}
		}

		dispensationRepository.save(dispensation);

		if (anyItemDispensed) {
			String newStatus = allItemsFullyDispensed ? "FULLY_DISPENSED" : "PARTIALLY_DISPENSED";
			prescription.setStatus(newStatus);
			jdbcTemplate.update("UPDATE prescriptions SET status = ?, updated_at = ? WHERE id = ?", newStatus, Timestamp.from(Instant.now()), prescription.getId());
		}

		// Query patientId globally using raw SQL to bypass TenantId restrictions, handling potential EmptyResultDataAccessException
		UUID patientId = null;
		try {
			String patientIdSql = "SELECT v.patient_id " +
					"FROM prescriptions pr " +
					"JOIN consultations c ON pr.consultation_id = c.id " +
					"JOIN visits v ON c.visit_id = v.id " +
					"WHERE pr.id = ?";
			patientId = jdbcTemplate.queryForObject(patientIdSql, UUID.class, prescription.getId());
		} catch (org.springframework.dao.EmptyResultDataAccessException e) {
			// Fallback: essayer de récupérer le patientId via visit_id directement
			if (prescription.getVisitId() != null) {
				try {
					patientId = jdbcTemplate.queryForObject("SELECT patient_id FROM visits WHERE id = ?", UUID.class, prescription.getVisitId());
				} catch (org.springframework.dao.EmptyResultDataAccessException ex) {
					// Ignorer
				}
			}
		}

		// Audit log
		auditService.logSuccess(
				null,
				prescription.getOrganizationId(),
				patientId,
				"PRESCRIPTION",
				prescription.getId(),
				"PHARMACY_DISPENSED",
				"Dispensation par pharmacie : " + pharmacyName + " (Licence: " + pharmacistLicense + ")"
		);
	}

	@Transactional(readOnly = true)
	public List<PharmacyDispensationHistoryResponse> getDispensationHistory(String prescriptionNumber, String pinCode) {
		checkLockout(prescriptionNumber);

		PrescriptionEntity prescription = prescriptionRepository.findByPrescriptionNumberGlobally(prescriptionNumber)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordonnance introuvable."));

		if (prescription.getPinCode() == null || !prescription.getPinCode().equalsIgnoreCase(pinCode)) {
			handleFailedAttempt(prescriptionNumber);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Code PIN incorrect.");
		}

		failedAttempts.remove(prescriptionNumber);

		// Utilise du SQL natif via JdbcTemplate pour contourner le filtre @TenantId de Hibernate.
		// Sur un endpoint public, il n'y a pas de contexte de tenant actif, ce qui rend
		// les requêtes Spring Data JPA traversant des relations @TenantId (@ManyToOne vers PrescriptionEntity)
		// invisibles (retournent une liste vide). Le pattern SQL natif est identique à verifyPrescription().
		String dispensationsSql =
				"SELECT d.id AS disp_id, d.dispensed_at, d.pharmacy_name, d.pharmacist_license " +
				"FROM prescription_dispensations d " +
				"WHERE d.prescription_id = ? " +
				"ORDER BY d.dispensed_at DESC";

		List<Map<String, Object>> dispensationRows = jdbcTemplate.queryForList(dispensationsSql, prescription.getId());

		return dispensationRows.stream().map(row -> {
			UUID dispensationId = row.get("disp_id") instanceof UUID uuid ? uuid : UUID.fromString(row.get("disp_id").toString());
			Instant dispensedAt = row.get("dispensed_at") instanceof Timestamp ts ? ts.toInstant()
					: row.get("dispensed_at") instanceof java.sql.Timestamp ts2 ? ts2.toInstant()
					: Instant.parse(row.get("dispensed_at").toString());
			String pharmacyName = (String) row.get("pharmacy_name");
			String pharmacistLicense = (String) row.get("pharmacist_license");

			String itemsSql =
					"SELECT di.id AS item_id, pi.id AS prescription_item_id, pi.drug_name, " +
					"di.quantity_dispensed, di.substituted_with " +
					"FROM dispensation_items di " +
					"JOIN prescription_items pi ON di.prescription_item_id = pi.id " +
					"WHERE di.dispensation_id = ?";

			List<PharmacyDispensationHistoryItemResponse> items = jdbcTemplate.queryForList(itemsSql, dispensationId)
					.stream()
					.map(itemRow -> {
						UUID prescriptionItemId = itemRow.get("prescription_item_id") instanceof UUID uid
								? uid : UUID.fromString(itemRow.get("prescription_item_id").toString());
						String drugName = (String) itemRow.get("drug_name");
						Integer quantityDispensed = itemRow.get("quantity_dispensed") instanceof Number n
								? n.intValue() : 0;
						String substitutedWith = (String) itemRow.get("substituted_with");
						return new PharmacyDispensationHistoryItemResponse(prescriptionItemId, drugName, quantityDispensed, substitutedWith);
					})
					.toList();

			return new PharmacyDispensationHistoryResponse(dispensationId, dispensedAt, pharmacyName, pharmacistLicense, items);
		}).toList();
	}

	private void checkLockout(String prescriptionNumber) {
		Instant lockoutTime = lockouts.get(prescriptionNumber);
		if (lockoutTime != null) {
			if (lockoutTime.isAfter(Instant.now())) {
				throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Cette ordonnance est temporairement bloquée suite à plusieurs essais erronés.");
			} else {
				lockouts.remove(prescriptionNumber);
			}
		}
	}

	private void handleFailedAttempt(String prescriptionNumber) {
		int count = failedAttempts.getOrDefault(prescriptionNumber, 0) + 1;
		if (count >= 3) {
			lockouts.put(prescriptionNumber, Instant.now().plus(5, ChronoUnit.MINUTES));
			failedAttempts.remove(prescriptionNumber);
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Code PIN incorrect. L'ordonnance est bloquée pour 5 minutes.");
		} else {
			failedAttempts.put(prescriptionNumber, count);
		}
	}

	private int parseQuantity(String qtyStr) {
		if (qtyStr == null) return 0;
		String digits = qtyStr.replaceAll("[^0-9]", "");
		if (digits.isEmpty()) return 0;
		try {
			return java.lang.Integer.parseInt(digits);
		} catch (NumberFormatException e) {
			return 0;
		}
	}


}
