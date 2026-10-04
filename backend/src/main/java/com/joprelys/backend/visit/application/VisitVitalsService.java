package com.joprelys.backend.visit.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.visit.api.SaveVitalsRequest;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VitalMeasurementEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VitalMeasurementRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VitalsRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
public class VisitVitalsService implements VisitVitalsUseCase {

	private static final String ACTIVE_VISIT_STATUS = "EN_COURS";

	private final VisitRepository visitRepository;
	private final VitalsRepository vitalsRepository;
	private final VitalMeasurementRepository vitalMeasurementRepository;
	private final UserAccountRepository userAccountRepository;
	private final AuditService auditService;
	private final ObjectMapper objectMapper;

	public VisitVitalsService(
			VisitRepository visitRepository,
			VitalsRepository vitalsRepository,
			VitalMeasurementRepository vitalMeasurementRepository,
			UserAccountRepository userAccountRepository,
			AuditService auditService,
			ObjectMapper objectMapper) {
		this.visitRepository = visitRepository;
		this.vitalsRepository = vitalsRepository;
		this.vitalMeasurementRepository = vitalMeasurementRepository;
		this.userAccountRepository = userAccountRepository;
		this.auditService = auditService;
		this.objectMapper = objectMapper;
	}

	@Override
	@Transactional
	public VitalsEntity recordVitals(UUID visitId, SaveVitalsRequest request, UUID actorUserId, UUID actorOrganizationId) {
		VisitEntity visit = visitRepository.findById(visitId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable."));

		if (!ACTIVE_VISIT_STATUS.equals(visit.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les constantes ne peuvent être saisies que sur une visite active.");
		}
		if (request.systolic() != null && request.diastolic() != null && request.systolic() <= request.diastolic()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"La tension systolique doit être supérieure à la tension diastolique.");
		}

		VitalsEntity vitals = vitalsRepository.findByVisitId(visitId)
				.orElseGet(() -> new VitalsEntity(visit, null, null, null, null, null, null, null, null, null, null, null));
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
		vitals.setBmi(computeBmi(request));
		VitalsEntity saved = vitalsRepository.save(vitals);

		String actorName = actorUserId == null ? null : userAccountRepository.findById(actorUserId)
				.map(UserAccountEntity::getDisplayName)
				.orElse(null);
		vitalMeasurementRepository.save(new VitalMeasurementEntity(visitId, actorUserId, actorName, Instant.now(), saved));

		visit.markVitalsRecorded();
		visitRepository.save(visit);

		auditService.logSuccess(
				actorUserId,
				actorOrganizationId,
				visit.getPatient().getId(),
				"VISIT",
				visitId,
				"VISIT_VITALS_RECORDED",
				auditSummary(request));

		return saved;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<VitalsEntity> getLatestVitals(UUID visitId) {
		requireVisit(visitId);
		return vitalsRepository.findByVisitId(visitId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<VitalMeasurementEntity> getVitalsHistory(UUID visitId) {
		requireVisit(visitId);
		return vitalMeasurementRepository.findByVisitIdOrderByRecordedAtDesc(visitId);
	}

	private void requireVisit(UUID visitId) {
		if (!visitRepository.existsById(visitId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable.");
		}
	}

	private BigDecimal computeBmi(SaveVitalsRequest request) {
		if (request.weight() == null || request.height() == null || request.height() <= 0) {
			return null;
		}
		double heightM = request.height() / 100.0;
		double rawBmi = request.weight().doubleValue() / (heightM * heightM);
		return BigDecimal.valueOf(rawBmi).setScale(2, RoundingMode.HALF_UP);
	}

	private String auditSummary(SaveVitalsRequest request) {
		try {
			Map<String, Object> values = new LinkedHashMap<>();
			values.put("temperature", request.temperature());
			values.put("weight", request.weight());
			values.put("height", request.height());
			values.put("pulse", request.pulse());
			values.put("systolic", request.systolic());
			values.put("diastolic", request.diastolic());
			values.put("spo2", request.spo2());
			values.put("glycemia", request.glycemia());
			values.put("respiratoryRate", request.respiratoryRate());
			values.put("painScale", request.painScale());
			values.values().removeIf(Objects::isNull);
			return objectMapper.writeValueAsString(values);
		} catch (Exception e) {
			return "Constantes enregistrées";
		}
	}
}
