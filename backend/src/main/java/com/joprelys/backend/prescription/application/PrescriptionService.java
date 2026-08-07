package com.joprelys.backend.prescription.application;

import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.prescription.api.PrescriptionItemRequest;
import com.joprelys.backend.prescription.api.SavePrescriptionRequest;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionItemEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
import com.joprelys.backend.visit.application.DocumentService;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.time.Instant;
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
	private final DocumentService documentService;

	public PrescriptionService(PrescriptionRepository prescriptionRepository,
			ConsultationRepository consultationRepository,
			PrescriptionNumberGenerator prescriptionNumberGenerator,
			AlloPharmaClient alloPharmaClient,
			com.joprelys.backend.audit.application.AuditService auditService,
			UserAccountRepository userAccountRepository,
			PatientRepository patientRepository,
			@Lazy DocumentService documentService) {
		this.prescriptionRepository = prescriptionRepository;
		this.consultationRepository = consultationRepository;
		this.prescriptionNumberGenerator = prescriptionNumberGenerator;
		this.alloPharmaClient = alloPharmaClient;
		this.auditService = auditService;
		this.userAccountRepository = userAccountRepository;
		this.patientRepository = patientRepository;
		this.documentService = documentService;
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
					newPresc.setExpiresAt(Instant.now().plus(90, java.time.temporal.ChronoUnit.DAYS));
					newPresc.setStatus("DRAFT");
					return newPresc;
				});

		// Si l'ordonnance existe déjà, interdire sa modification si elle n'est pas en DRAFT
		if (prescription.getId() != null && !"DRAFT".equals(prescription.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une ordonnance validée ou active ne peut plus être modifiée.");
		}

		// Remplacer les items (orphanRemoval)
		prescription.getItems().clear();
		List<PrescriptionItemRequest> items = request.items();
		for (int i = 0; i < items.size(); i++) {
			PrescriptionItemRequest req = items.get(i);
			prescription.getItems().add(new PrescriptionItemEntity(
					prescription, req.drugName(), req.dosage(),
					req.posology(), req.duration(), req.quantity(),
					req.instructions(), i,
					req.form(), req.route(), req.frequency(), req.substitutionAllowed()
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
	public PrescriptionEntity finalizePrescription(UUID prescriptionId, UUID actorUserId) {
		PrescriptionEntity prescription = prescriptionRepository.findByIdWithConsultationAndItems(prescriptionId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordonnance introuvable."));

		if (!"DRAFT".equals(prescription.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seule une ordonnance au statut DRAFT peut être finalisée.");
		}

		validateDraftCompleteness(prescription);
		prescription.setStatus("ACTIVE");
		prescription.setIssuedAt(Instant.now());
		prescription = prescriptionRepository.save(prescription);

		// Générer le document PDF associé
		documentService.generatePrescriptionDocument(prescription.getId(), actorUserId);

		return prescription;
	}

	@Transactional
	public PrescriptionEntity cancelPrescription(UUID prescriptionId, UUID actorUserId) {
		PrescriptionEntity prescription = prescriptionRepository.findByIdWithConsultationAndItems(prescriptionId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordonnance introuvable."));

		prescription.setStatus("CANCELLED");
		prescription = prescriptionRepository.save(prescription);

		if (prescription.getDocumentId() != null) {
			try {
				documentService.cancelDocument(prescription.getDocumentId(), "Prescription annulée par le médecin.", actorUserId);
			} catch (Exception e) {
				// Ignorer si le document est déjà annulé/révoqué
			}
		}

		auditService.logSuccess(
				actorUserId,
				prescription.getOrganizationId(),
				prescription.getConsultation().getVisit().getPatient().getId(),
				"PRESCRIPTION",
				prescription.getId(),
				"CANCEL_PRESCRIPTION",
				"Annulation réussie de l'ordonnance " + prescription.getPrescriptionNumber()
		);

		return prescription;
	}

	@Scheduled(cron = "0 0 0 * * ?") // Tous les jours à minuit
	@Transactional
	public void expireOutdatedPrescriptions() {
		List<PrescriptionEntity> activePrescriptions = prescriptionRepository.findAllActivePrescriptions();
		Instant now = Instant.now();
		for (PrescriptionEntity prescription : activePrescriptions) {
			if (prescription.getExpiresAt() != null && prescription.getExpiresAt().isBefore(now)) {
				prescription.setStatus("EXPIRED");
				prescriptionRepository.save(prescription);
			}
		}
	}

	@Transactional
	public PrescriptionEntity transmitPrescription(UUID id, UUID actorUserId, UUID actorOrganizationId, String actorEmail, boolean isPatient) {
		PrescriptionEntity prescription = prescriptionRepository.findByIdWithConsultationAndItems(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordonnance introuvable."));

		// Sécurité : IDOR check pour le patient
		if (isPatient) {
			UUID patientId = prescription.getConsultation().getVisit().getPatient().getId();
			if (!patientId.equals(actorUserId)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas autorisé à télétransmettre cette ordonnance.");
			}
		}

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
			prescription.setTransmittedAt(Instant.now());
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

	private void validateDraftCompleteness(PrescriptionEntity prescription) {
		boolean missingDosage = prescription.getItems().stream()
				.anyMatch(item -> item.getDosage() == null || item.getDosage().isBlank());
		if (missingDosage) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Complétez le dosage de chaque médicament avant de finaliser l’ordonnance.");
		}
	}

	private String generateRandomPin() {
		int pin = (int) (Math.random() * 9000) + 1000;
		return String.valueOf(pin);
	}
}
