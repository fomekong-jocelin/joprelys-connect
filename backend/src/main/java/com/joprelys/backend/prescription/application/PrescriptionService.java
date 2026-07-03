package com.joprelys.backend.prescription.application;

import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.prescription.api.PrescriptionItemRequest;
import com.joprelys.backend.prescription.api.SavePrescriptionRequest;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionItemEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PrescriptionService {

	private final PrescriptionRepository prescriptionRepository;
	private final ConsultationRepository consultationRepository;
	private final PrescriptionNumberGenerator prescriptionNumberGenerator;

	public PrescriptionService(PrescriptionRepository prescriptionRepository,
			ConsultationRepository consultationRepository,
			PrescriptionNumberGenerator prescriptionNumberGenerator) {
		this.prescriptionRepository = prescriptionRepository;
		this.consultationRepository = consultationRepository;
		this.prescriptionNumberGenerator = prescriptionNumberGenerator;
	}

	@Transactional
	public PrescriptionEntity savePrescription(UUID consultationId, SavePrescriptionRequest request) {
		var consultation = consultationRepository.findById(consultationId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation introuvable."));

		PrescriptionEntity prescription = prescriptionRepository.findByConsultationId(consultationId)
				.orElseGet(() -> {
					PrescriptionEntity newPresc = new PrescriptionEntity(consultation);
					newPresc.setPrescriptionNumber(prescriptionNumberGenerator.generateNextPrescriptionNumber());
					newPresc.setPinCode(generateRandomPin());
					newPresc.setExpiresAt(java.time.Instant.now().plus(90, java.time.temporal.ChronoUnit.DAYS));
					newPresc.setStatus("ACTIVE");
					return newPresc;
				});

		// Remplacer les items (orphanRemoval)
		prescription.getItems().clear();
		List<PrescriptionItemRequest> items = request.items();
		for (int i = 0; i < items.size(); i++) {
			PrescriptionItemRequest req = items.get(i);
			prescription.getItems().add(new PrescriptionItemEntity(
					prescription, req.drugName(), req.dosage(),
					req.posology(), req.duration(), req.quantity(),
					req.instructions(), i
			));
		}

		return prescriptionRepository.save(prescription);
	}

	@Transactional(readOnly = true)
	public Optional<PrescriptionEntity> getPrescription(UUID consultationId) {
		if (!consultationRepository.existsById(consultationId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation introuvable.");
		}
		return prescriptionRepository.findByConsultationId(consultationId);
	}

	private String generateRandomPin() {
		String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		java.security.SecureRandom random = new java.security.SecureRandom();
		StringBuilder sb = new StringBuilder(4);
		for (int i = 0; i < 4; i++) {
			sb.append(chars.charAt(random.nextInt(chars.length())));
		}
		return sb.toString();
	}
}
