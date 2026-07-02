package com.joprelys.backend.visit.application;

import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
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

	public VisitService(
			VisitRepository visitRepository,
			PatientRepository patientRepository,
			VisitNumberGenerator visitNumberGenerator) {
		this.visitRepository = visitRepository;
		this.patientRepository = patientRepository;
		this.visitNumberGenerator = visitNumberGenerator;
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

		return visitRepository.save(visit);
	}

	@Transactional(readOnly = true)
	public List<VisitEntity> getActiveVisits() {
		return visitRepository.findActiveVisits();
	}
}
