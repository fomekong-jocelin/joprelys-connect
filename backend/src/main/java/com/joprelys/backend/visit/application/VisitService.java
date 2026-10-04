package com.joprelys.backend.visit.application;

import tools.jackson.databind.ObjectMapper;
import com.joprelys.backend.ai.realtime.application.RealtimeClinicalIntakeService;
import com.joprelys.backend.ai.realtime.application.RealtimeIntakeSource;
import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.api.CorrectVisitRequest;
import com.joprelys.backend.visit.api.CreateVisitRequest;
import com.joprelys.backend.visit.infrastructure.persistence.VisitCorrectionEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitCorrectionRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyRepository;
import com.joprelys.backend.emergency.triage.infrastructure.persistence.EmergencyTriageAssessmentEntity;
import com.joprelys.backend.emergency.triage.infrastructure.persistence.EmergencyTriageAssessmentRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VisitService {

	private final VisitRepository visitRepository;
	private final PatientRepository patientRepository;
	private final VisitNumberGenerator visitNumberGenerator;
	private final DocumentService documentService;
	private final VisitCorrectionRepository visitCorrectionRepository;
	private final AuditService auditService;
	private final ObjectMapper objectMapper;
	private final RealtimeClinicalIntakeService realtimeClinicalIntakeService;
    private final VisitClinicalClosureService clinicalClosure;
	private final EmergencyRepository emergencyRepository;
	private final EmergencyTriageAssessmentRepository triageAssessmentRepository;

	@SuppressWarnings("checkstyle:ParameterNumber")
	public VisitService(
			VisitRepository visitRepository,
			PatientRepository patientRepository,
			VisitNumberGenerator visitNumberGenerator,
			@org.springframework.context.annotation.Lazy DocumentService documentService,
			VisitCorrectionRepository visitCorrectionRepository,
			AuditService auditService,
			ObjectMapper objectMapper,
			RealtimeClinicalIntakeService realtimeClinicalIntakeService,
            VisitClinicalClosureService clinicalClosure,
			@org.springframework.beans.factory.annotation.Autowired(required = false) EmergencyRepository emergencyRepository,
			@org.springframework.beans.factory.annotation.Autowired(required = false) EmergencyTriageAssessmentRepository triageAssessmentRepository) {
		this.visitRepository = visitRepository;
		this.patientRepository = patientRepository;
		this.visitNumberGenerator = visitNumberGenerator;
		this.documentService = documentService;
		this.visitCorrectionRepository = visitCorrectionRepository;
		this.auditService = auditService;
		this.objectMapper = objectMapper;
		this.realtimeClinicalIntakeService = realtimeClinicalIntakeService;
        this.clinicalClosure = clinicalClosure;
		this.emergencyRepository = emergencyRepository;
		this.triageAssessmentRepository = triageAssessmentRepository;
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

		propagateEmergencyVitalsIfApplicable(visit, patient);

		return visitRepository.save(visit);
	}

	@Transactional
	public VisitEntity closeVisit(UUID visitId) {
		VisitEntity visit = visitRepository.findByIdForUpdate(visitId)
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

        clinicalClosure.signClinicalActs(visit);

		documentService.generateAndSaveDocument(savedVisit);

		// The voice transcript remains recoverable for every draft save. It is removed
		// from the recovery queue only when the whole visit closure has succeeded.
		if (savedVisit.getOrganizationId() != null) {
			realtimeClinicalIntakeService.consume(
					visitId,
					savedVisit.getOrganizationId(),
					RealtimeIntakeSource.CONSULTATION);
		}

		return savedVisit;
	}

	@Transactional
	public VisitEntity cancelVisit(UUID visitId) {
		VisitEntity visit = visitRepository.findByIdForUpdate(visitId)
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
		VisitEntity visit = visitRepository.findByIdForUpdate(visitId)
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

	@Transactional(readOnly = true)
	public List<VisitEntity> getPatientVisits(UUID patientId) {
		return visitRepository.findByPatientIdWithPatientAndVitals(patientId);
	}

	private void propagateEmergencyVitalsIfApplicable(VisitEntity visit, PatientEntity patient) {
		if (emergencyRepository == null) return;
		List<EmergencyEntity> emergencies = emergencyRepository.findByPatientIdWithLogs(patient.getId());
		if (emergencies.isEmpty()) return;

		Instant threshold = Instant.now().minus(java.time.Duration.ofHours(24));
		EmergencyEntity latest = emergencies.stream()
				.filter(e -> e.getCreatedAt().isAfter(threshold) || e.getStabilizedAt() == null || e.getVisitId() == null)
				.findFirst()
				.orElse(null);

		if (latest == null) return;

		if (latest.getVisitId() == null) {
			latest.setVisitId(visit.getId());
			emergencyRepository.save(latest);
		}

		EmergencyTriageAssessmentEntity triage = null;
		if (triageAssessmentRepository != null) {
			List<EmergencyTriageAssessmentEntity> assessments =
					triageAssessmentRepository.findByEmergency_IdOrderBySequenceNumberAsc(latest.getId());
			if (!assessments.isEmpty()) {
				triage = assessments.get(assessments.size() - 1);
			}
		}

		Integer systolic = triage != null && triage.getBpSystolic() != null ? triage.getBpSystolic() : latest.getInitialBpSystolic();
		Integer diastolic = triage != null && triage.getBpDiastolic() != null ? triage.getBpDiastolic() : latest.getInitialBpDiastolic();
		Integer pulse = triage != null && triage.getHeartRate() != null ? triage.getHeartRate() : latest.getInitialHr();
		java.math.BigDecimal temp = triage != null && triage.getTemperature() != null ? triage.getTemperature() : latest.getInitialTemp();
		Integer spo2 = triage != null ? triage.getOxygenSaturation() : null;
		Integer rr = triage != null ? triage.getRespiratoryRate() : null;
		Integer pain = triage != null ? triage.getPainScore() : null;

		boolean hasAnyVital = systolic != null || diastolic != null || pulse != null || temp != null || spo2 != null || rr != null || pain != null;
		if (hasAnyVital) {
			VitalsEntity vitals = new VitalsEntity(
					visit,
					temp,
					null,
					null,
					pulse,
					systolic,
					diastolic,
					spo2,
					null,
					rr,
					pain,
					null);
			visit.setVitals(vitals);
			visit.markVitalsRecorded();
		}
	}
}
