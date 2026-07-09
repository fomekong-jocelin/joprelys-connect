package com.joprelys.backend.visit.application;

import tools.jackson.databind.ObjectMapper;
import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.api.CorrectVisitRequest;
import com.joprelys.backend.visit.api.CreateVisitRequest;
import com.joprelys.backend.visit.api.SaveVitalsRequest;
import com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitCorrectionEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitCorrectionRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VitalsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;

@Service
public class VisitService {

	private final VisitRepository visitRepository;
	private final PatientRepository patientRepository;
	private final VisitNumberGenerator visitNumberGenerator;
	private final VitalsRepository vitalsRepository;
	private final DocumentService documentService;
	private final VisitCorrectionRepository visitCorrectionRepository;
	private final AuditService auditService;
	private final ObjectMapper objectMapper;
	private final ConsultationRepository consultationRepository;
	private final PrescriptionRepository prescriptionRepository;
	private final UserAccountRepository userAccountRepository;

	@SuppressWarnings("checkstyle:ParameterNumber")
	public VisitService(
			VisitRepository visitRepository,
			PatientRepository patientRepository,
			VisitNumberGenerator visitNumberGenerator,
			VitalsRepository vitalsRepository,
			@org.springframework.context.annotation.Lazy DocumentService documentService,
			VisitCorrectionRepository visitCorrectionRepository,
			AuditService auditService,
			ObjectMapper objectMapper,
			ConsultationRepository consultationRepository,
			PrescriptionRepository prescriptionRepository,
			UserAccountRepository userAccountRepository) {
		this.visitRepository = visitRepository;
		this.patientRepository = patientRepository;
		this.visitNumberGenerator = visitNumberGenerator;
		this.vitalsRepository = vitalsRepository;
		this.documentService = documentService;
		this.visitCorrectionRepository = visitCorrectionRepository;
		this.auditService = auditService;
		this.objectMapper = objectMapper;
		this.consultationRepository = consultationRepository;
		this.prescriptionRepository = prescriptionRepository;
		this.userAccountRepository = userAccountRepository;
	}

	@Transactional
	public VisitEntity createVisit(CreateVisitRequest request) {
		PatientEntity patient = patientRepository.findById(request.patientId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable."));

		if (visitRepository.existsByPatientIdAndStatus(request.patientId(), "EN_COURS")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce patient possède déjà une visite active en cours.");
		}

		String visitNumber = visitNumberGenerator.generateNextVisitNumber();
		Instant arrivalAt = request.arrivalAt() != null ? request.arrivalAt() : Instant.now();
		VisitEntity visit = new VisitEntity(
				patient,
				visitNumber,
				request.reason(),
				request.orientation(),
				request.service() != null ? request.service() : request.orientation(),
				request.mainPractitionerId(),
				arrivalAt);

		return visitRepository.save(visit);
	}

	@Transactional
	public VisitEntity closeVisit(UUID visitId) {
		VisitEntity visit = visitRepository.findById(visitId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable."));

		if (!"EN_COURS".equals(visit.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seule une visite active peut être clôturée.");
		}

		visit.setStatus("TERMINEE");
		visit.setClosedAt(Instant.now());

		// Initialize lazy proxies to prevent LazyInitializationException outside transaction
		if (visit.getPatient() != null) {
			visit.getPatient().getFullName();
		}
		if (visit.getVitals() != null) {
			visit.getVitals().getTemperature();
		}

		VisitEntity savedVisit = visitRepository.save(visit);

		// Finalize draft prescriptions associated with this visit's consultations
		consultationRepository.findByVisitId(visit.getId()).ifPresent(consultation -> {
			prescriptionRepository.findByConsultationId(consultation.getId()).ifPresent(prescription -> {
				if ("DRAFT".equals(prescription.getStatus())) {
					prescription.setStatus("ACTIVE");
					prescription.setIssuedAt(Instant.now());
					prescriptionRepository.save(prescription);

					// Get actor user ID from SecurityContextHolder
					UUID actorUserId = null;
					var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
					if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
						actorUserId = userAccountRepository.findByEmail(auth.getName().trim().toLowerCase())
								.map(UserAccountEntity::getId)
								.orElse(null);
					}

					documentService.generatePrescriptionDocument(prescription.getId(), actorUserId);
				}
			});
		});

		documentService.generateAndSaveDocument(savedVisit);

		return savedVisit;
	}

	@Transactional
	public VisitEntity cancelVisit(UUID visitId) {
		VisitEntity visit = visitRepository.findById(visitId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable."));

		if (!"EN_COURS".equals(visit.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seule une visite active peut être annulée.");
		}

		visit.setStatus("ANNULEE");
		visit.setClosedAt(Instant.now());

		if (visit.getPatient() != null) {
			visit.getPatient().getFullName();
		}

		return visitRepository.save(visit);
	}

	@Transactional
	public VisitEntity correctVisit(UUID visitId, CorrectVisitRequest request, UUID correctedByUserId, UUID actorOrganizationId) {
		VisitEntity visit = visitRepository.findById(visitId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable."));

		if (!"TERMINEE".equals(visit.getStatus()) && !"ANNULEE".equals(visit.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Seule une visite clôturée ou annulée peut faire l'objet d'une correction traçée.");
		}

		String previousValues;
		try {
			previousValues = objectMapper.writeValueAsString(Map.of(
					"reason", visit.getReason(),
					"orientation", visit.getOrientation(),
					"service", visit.getService(),
					"mainPractitionerId", visit.getMainPractitionerId(),
					"arrivalAt", visit.getArrivalAt()));
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"Impossible de sérialiser les valeurs précédentes pour la traçabilité.");
		}

		VisitCorrectionEntity correction = new VisitCorrectionEntity(
				visit,
				correctedByUserId,
				request.correctionReason(),
				previousValues);
		visitCorrectionRepository.save(correction);

		if (request.reason() != null) {
			visit.setReason(request.reason());
		}
		if (request.orientation() != null) {
			visit.setOrientation(request.orientation());
		}
		if (request.service() != null) {
			visit.setService(request.service());
		}
		if (request.mainPractitionerId() != null) {
			visit.setMainPractitionerId(request.mainPractitionerId());
		}
		if (request.arrivalAt() != null) {
			visit.setArrivalAt(request.arrivalAt());
		}

		VisitEntity saved = visitRepository.save(visit);

		auditService.logSuccess(
				correctedByUserId,
				actorOrganizationId,
				visit.getPatient().getId(),
				"VISIT",
				visitId,
				"VISIT_CORRECTION",
				request.correctionReason());

		return saved;
	}

	@Transactional(readOnly = true)
	public VisitEntity getVisit(UUID id) {
		VisitEntity visit = visitRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable."));
		if (visit.getPatient() != null) {
			visit.getPatient().getFullName();
			visit.getPatient().getGlobalPatientNumber();
		}
		if (visit.getVitals() != null) {
			visit.getVitals().getTemperature();
		}
		return visit;
	}

	@Transactional(readOnly = true)
	public VisitEntity getVisitGlobally(UUID id) {
		return visitRepository.findByIdGlobally(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable."));
	}


	@Transactional(readOnly = true)
	public List<VisitEntity> getActiveVisits() {
		return visitRepository.findActiveVisits();
	}

	@Transactional
	public VitalsEntity saveVitals(UUID visitId, SaveVitalsRequest request) {
		VisitEntity visit = visitRepository.findById(visitId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable."));

		if (!"EN_COURS".equals(visit.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les constantes ne peuvent être saisies que sur une visite active.");
		}

		java.math.BigDecimal bmi = null;
		if (request.weight() != null && request.height() != null && request.height() > 0) {
			double heightM = request.height() / 100.0;
			double rawBmi = request.weight().doubleValue() / (heightM * heightM);
			bmi = java.math.BigDecimal.valueOf(rawBmi).setScale(2, java.math.RoundingMode.HALF_UP);
		}

		VitalsEntity vitals = vitalsRepository.findByVisitId(visitId)
				.orElseGet(() -> {
					var v = new VitalsEntity(visit, null, null, null, null, null, null, null, null, null, null, null);
					return v;
				});

		vitals.setTemperature(request.temperature());
		vitals.setWeight(request.weight());
		vitals.setHeight(request.height());
		vitals.setPulse(request.pulse());
		vitals.setSystolic(request.systolic());
		vitals.setDiastolic(request.diastolic());
		vitals.setSpo2(request.spo2());
		vitals.setGlycemia(request.glycemia());
		vitals.setRespiratoryRate(request.respiratoryRate());
		vitals.setPainScale(request.painScale());
		vitals.setBmi(bmi);

		return vitalsRepository.save(vitals);
	}

	@Transactional(readOnly = true)
	public java.util.Optional<VitalsEntity> getVitals(UUID visitId) {
		if (!visitRepository.existsById(visitId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable.");
		}
		return vitalsRepository.findByVisitId(visitId);
	}

	@Transactional(readOnly = true)
	public List<VisitEntity> getPatientVisits(UUID patientId) {
		return visitRepository.findByPatientIdWithPatientAndVitals(patientId);
	}
}
