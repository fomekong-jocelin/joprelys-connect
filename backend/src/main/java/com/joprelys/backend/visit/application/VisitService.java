package com.joprelys.backend.visit.application;

import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VitalsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class VisitService {

	private final VisitRepository visitRepository;
	private final PatientRepository patientRepository;
	private final VisitNumberGenerator visitNumberGenerator;

	private final VitalsRepository vitalsRepository;
	private final DocumentService documentService;

	public VisitService(
			VisitRepository visitRepository,
			PatientRepository patientRepository,
			VisitNumberGenerator visitNumberGenerator,
			VitalsRepository vitalsRepository,
			@org.springframework.context.annotation.Lazy DocumentService documentService) {
		this.visitRepository = visitRepository;
		this.patientRepository = patientRepository;
		this.visitNumberGenerator = visitNumberGenerator;
		this.vitalsRepository = vitalsRepository;
		this.documentService = documentService;
	}

	@Transactional
	public VisitEntity createVisit(UUID patientId, String reason, String orientation) {
		PatientEntity patient = patientRepository.findById(patientId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable."));

		if (visitRepository.existsByPatientIdAndStatus(patientId, "EN_COURS")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce patient possède déjà une visite active en cours.");
		}

		String visitNumber = visitNumberGenerator.generateNextVisitNumber();
		VisitEntity visit = new VisitEntity(patient, visitNumber, reason, orientation);

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

	@Transactional(readOnly = true)
	public VisitEntity getVisit(UUID id) {
		return visitRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable."));
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
	public com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity saveVitals(UUID visitId, com.joprelys.backend.visit.api.SaveVitalsRequest request) {
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

		com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity vitals = vitalsRepository.findByVisitId(visitId)
				.orElseGet(() -> {
					var v = new com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity(visit, null, null, null, null, null, null, null, null, null, null);
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
		vitals.setBmi(bmi);

		return vitalsRepository.save(vitals);
	}

	@Transactional(readOnly = true)
	public java.util.Optional<com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity> getVitals(UUID visitId) {
		if (!visitRepository.existsById(visitId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable.");
		}
		return vitalsRepository.findByVisitId(visitId);
	}
}
