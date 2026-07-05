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

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PrescriptionService {

	private final PrescriptionRepository prescriptionRepository;
	private final ConsultationRepository consultationRepository;
	private final PrescriptionNumberGenerator prescriptionNumberGenerator;
	private final AlloPharmaClient alloPharmaClient;
	private final com.joprelys.backend.audit.application.AuditService auditService;
	private final UserAccountRepository userAccountRepository;
	private final PatientRepository patientRepository;

	public PrescriptionService(PrescriptionRepository prescriptionRepository,
			ConsultationRepository consultationRepository,
			PrescriptionNumberGenerator prescriptionNumberGenerator,
			AlloPharmaClient alloPharmaClient,
			com.joprelys.backend.audit.application.AuditService auditService,
			UserAccountRepository userAccountRepository,
			PatientRepository patientRepository) {
		this.prescriptionRepository = prescriptionRepository;
		this.consultationRepository = consultationRepository;
		this.prescriptionNumberGenerator = prescriptionNumberGenerator;
		this.alloPharmaClient = alloPharmaClient;
		this.auditService = auditService;
		this.userAccountRepository = userAccountRepository;
		this.patientRepository = patientRepository;
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

	@Transactional
	public PrescriptionEntity transmitPrescription(UUID id, UUID actorUserId, UUID actorOrganizationId, String actorEmail, boolean isPatient) {
		PrescriptionEntity prescription = prescriptionRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordonnance introuvable."));

		// RM-1601-1: Seule une ordonnance au statut ACTIVE peut être télétransmise.
		if (!"ACTIVE".equalsIgnoreCase(prescription.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seule une ordonnance active peut être télétransmise.");
		}

		// RM-1601-2: Une ordonnance déjà transmise ne peut pas être transmise à nouveau (bouton désactivé).
		if ("TRANSMITTED".equalsIgnoreCase(prescription.getTransmissionStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette ordonnance a déjà été télétransmise.");
		}

		prescription.setTransmissionStatus("PENDING");
		prescription = prescriptionRepository.save(prescription);

		boolean success = alloPharmaClient.transmit(prescription);
		if (success) {
			prescription.setTransmissionStatus("TRANSMITTED");
			prescription.setTransmittedAt(java.time.Instant.now());
			auditService.logSuccess(
					actorUserId,
					actorOrganizationId,
					prescription.getConsultation().getVisit().getPatient().getId(),
					"PRESCRIPTION",
					prescription.getId(),
					"TRANSMIT_PRESCRIPTION",
					"Télétransmission de l'ordonnance " + prescription.getPrescriptionNumber() + " à AllôPharma réussie"
			);
		} else {
			prescription.setTransmissionStatus("FAILED");
			auditService.log(
					actorUserId,
					actorOrganizationId,
					prescription.getConsultation().getVisit().getPatient().getId(),
					"PRESCRIPTION",
					prescription.getId(),
					"TRANSMIT_PRESCRIPTION",
					"Échec de la télétransmission de l'ordonnance " + prescription.getPrescriptionNumber() + " à AllôPharma",
					null,
					null,
					"FAILED"
			);
		}

		return prescriptionRepository.save(prescription);
	}

	@Transactional
	public PrescriptionEntity transmitPrescriptionForStaff(UUID id, String actorEmail) {
		UserAccountEntity actor = userAccountRepository.findByEmail(actorEmail)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable."));

		return transmitPrescription(id, actor.getId(), actor.getOrganizationId(), actor.getEmail(), false);
	}

	@Transactional
	public PrescriptionEntity transmitPrescriptionForPatient(UUID id, String globalPatientNumber) {
		com.joprelys.backend.patient.infrastructure.persistence.PatientEntity patient = patientRepository.findByGlobalPatientNumber(globalPatientNumber)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Patient introuvable."));

		return transmitPrescription(id, patient.getId(), null, globalPatientNumber, true);
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
