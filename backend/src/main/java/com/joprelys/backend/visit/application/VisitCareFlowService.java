package com.joprelys.backend.visit.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VisitCareFlowService implements VisitCareFlowUseCase {

	private static final String ACTIVE_VISIT_STATUS = "EN_COURS";

	private final VisitRepository visitRepository;
	private final UserAccountRepository userAccountRepository;
	private final AuditService auditService;

	public VisitCareFlowService(
			VisitRepository visitRepository,
			UserAccountRepository userAccountRepository,
			AuditService auditService) {
		this.visitRepository = visitRepository;
		this.userAccountRepository = userAccountRepository;
		this.auditService = auditService;
	}

	@Override
	@Transactional
	public VisitEntity startConsultation(UUID visitId, String practitionerEmail, boolean takeOver) {
		VisitEntity visit = requireActiveVisit(visitId);
		UserAccountEntity practitioner = requirePractitioner(practitionerEmail);

		boolean heldByColleague = visit.isInConsultationWithAnotherPractitioner(practitioner.getId());
		if (heldByColleague && !takeOver) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Patient déjà en consultation chez " + visit.getConsultingPractitionerName() + ".");
		}
		String previousPractitioner = visit.getConsultingPractitionerName();
		visit.startConsultation(practitioner.getId(), practitioner.getDisplayName(), Instant.now());
		VisitEntity saved = visitRepository.save(visit);

		auditService.logSuccess(
				practitioner.getId(),
				practitioner.getOrganizationId(),
				visit.getPatient().getId(),
				"VISIT",
				visitId,
				heldByColleague ? "VISIT_CONSULTATION_TAKEN_OVER" : "VISIT_CONSULTATION_STARTED",
				heldByColleague ? "Reprise de la consultation de " + previousPractitioner : null);
		return initialize(saved);
	}

	@Override
	@Transactional
	public VisitEntity releaseConsultation(UUID visitId, String practitionerEmail) {
		VisitEntity visit = requireActiveVisit(visitId);
		UserAccountEntity practitioner = requirePractitioner(practitionerEmail);

		if (!practitioner.getId().equals(visit.getConsultingPractitionerId())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Seul le praticien qui a le patient en charge peut le remettre dans la file.");
		}
		visit.releaseConsultation();
		VisitEntity saved = visitRepository.save(visit);

		auditService.logSuccess(
				practitioner.getId(),
				practitioner.getOrganizationId(),
				visit.getPatient().getId(),
				"VISIT",
				visitId,
				"VISIT_CONSULTATION_RELEASED",
				null);
		return initialize(saved);
	}

	@Override
	public void ensureConsultationOwnership(VisitEntity visit, UUID practitionerId, String practitionerName) {
		if (visit.isInConsultationWithAnotherPractitioner(practitionerId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Patient en consultation chez " + visit.getConsultingPractitionerName()
							+ ". Reprenez la prise en charge depuis la file avant d'enregistrer.");
		}
		visit.startConsultation(practitionerId, practitionerName, Instant.now());
	}

	private VisitEntity requireActiveVisit(UUID visitId) {
		VisitEntity visit = visitRepository.findById(visitId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable."));
		if (!ACTIVE_VISIT_STATUS.equals(visit.getStatus())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "La visite n'est plus active.");
		}
		return visit;
	}

	private UserAccountEntity requirePractitioner(String practitionerEmail) {
		return userAccountRepository.findByEmail(practitionerEmail.trim().toLowerCase())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Praticien introuvable."));
	}

	/** Charge les associations lues par la réponse (open-in-view désactivé). */
	private VisitEntity initialize(VisitEntity visit) {
		visit.getPatient().getFullName();
		visit.getPatient().getGlobalPatientNumber();
		if (visit.getVitals() != null) {
			visit.getVitals().getTemperature();
		}
		return visit;
	}
}
