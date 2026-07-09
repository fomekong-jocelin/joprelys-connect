package com.joprelys.backend.reception.application;

import com.joprelys.backend.reception.api.CreateReceptionLogRequest;
import com.joprelys.backend.reception.infrastructure.persistence.ReceptionLogEntity;
import com.joprelys.backend.reception.infrastructure.persistence.ReceptionLogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ReceptionLogService {

	private final ReceptionLogRepository receptionLogRepository;

	public ReceptionLogService(ReceptionLogRepository receptionLogRepository) {
		this.receptionLogRepository = receptionLogRepository;
	}

	@Transactional
	public ReceptionLogEntity createReceptionLog(CreateReceptionLogRequest request, UUID createdByUserId) {
		ReceptionLogEntity log = new ReceptionLogEntity(
				request.logType(),
				request.firstName(),
				request.lastName(),
				request.idDocumentType(),
				request.idDocumentNumber(),
				request.targetPatientId(),
				request.targetStaffId(),
				request.reason(),
				request.arrivalAt(),
				createdByUserId
		);
		return receptionLogRepository.save(log);
	}

	@Transactional
	public ReceptionLogEntity markDeparture(UUID id) {
		ReceptionLogEntity log = receptionLogRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enregistrement d'accueil introuvable."));

		log.setDepartureAt(Instant.now());
		return receptionLogRepository.save(log);
	}

	@Transactional(readOnly = true)
	public List<ReceptionLogEntity> getAllReceptionLogs() {
		return receptionLogRepository.findAll();
	}

	@Transactional(readOnly = true)
	public ReceptionLogEntity getReceptionLog(UUID id) {
		return receptionLogRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enregistrement d'accueil introuvable."));
	}
}
