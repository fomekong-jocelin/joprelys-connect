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
				jdbcTemplate.update("UPDATE prescriptions SET status = 'EXPIRED', updated_at = ? WHERE id = ?", Instant.now(), prescription.getId());
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

		PrescriptionDispensationEntity dispensation = new PrescriptionDispensationEntity(
				prescription,
				request.pharmacyName(),
				request.pharmacistLicense()
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

			if (qtyAlreadyDispensed + dispItem.quantityDispensed() > qtyPrescribed) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantité dispensée excède la quantité prescrite pour le médicament : " + item.getDrugName());
			}

			DispensationItemEntity itemEntity = new DispensationItemEntity(
					dispensation,
					item,
					dispItem.quantityDispensed(),
					dispItem.substitutedWith()
			);
			dispensation.getItems().add(itemEntity);

			if (qtyAlreadyDispensed + dispItem.quantityDispensed() < qtyPrescribed) {
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
				if (qtyAlreadyDispensed < qtyPrescribed) {
					allItemsFullyDispensed = false;
				}
			}
		}

		dispensationRepository.save(dispensation);

		if (anyItemDispensed) {
			String newStatus = allItemsFullyDispensed ? "FULLY_DISPENSED" : "PARTIALLY_DISPENSED";
			prescription.setStatus(newStatus);
			jdbcTemplate.update("UPDATE prescriptions SET status = ?, updated_at = ? WHERE id = ?", newStatus, Instant.now(), prescription.getId());
		}

		// Query patientId globally using raw SQL to bypass TenantId restrictions
		String patientIdSql = "SELECT v.patient_id " +
				"FROM prescriptions pr " +
				"JOIN consultations c ON pr.consultation_id = c.id " +
				"JOIN visits v ON c.visit_id = v.id " +
				"WHERE pr.id = ?";
		UUID patientId = jdbcTemplate.queryForObject(patientIdSql, UUID.class, prescription.getId());

		// Audit log
		auditService.logSuccess(
				null,
				prescription.getOrganizationId(),
				patientId,
				"PRESCRIPTION",
				prescription.getId(),
				"PHARMACY_DISPENSED",
				"Dispensation par pharmacie : " + request.pharmacyName() + " (Licence: " + request.pharmacistLicense() + ")"
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

		return dispensationRepository.findByPrescriptionIdOrderByDispensedAtDesc(prescription.getId()).stream()
				.map(dispensation -> new PharmacyDispensationHistoryResponse(
						dispensation.getId(),
						dispensation.getDispensedAt(),
						dispensation.getPharmacyName(),
						dispensation.getPharmacistLicense(),
						dispensation.getItems().stream()
								.map(item -> new PharmacyDispensationHistoryItemResponse(
										item.getPrescriptionItem().getId(),
										item.getPrescriptionItem().getDrugName(),
										item.getQuantityDispensed(),
										item.getSubstitutedWith()
								))
								.toList()
				))
				.toList();
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
