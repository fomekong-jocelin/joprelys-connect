package com.joprelys.backend.lab.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.lab.api.CreateLabOrderRequest;
import com.joprelys.backend.lab.api.LabOrderItemResponse;
import com.joprelys.backend.lab.api.LabOrderResponse;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderItemEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderRepository;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LabOrderService {

	private final LabOrderRepository labOrderRepository;
	private final PatientRepository patientRepository;
	private final VisitRepository visitRepository;
	private final UserAccountRepository userAccountRepository;
	private final AuditService auditService;

	public LabOrderService(
			LabOrderRepository labOrderRepository,
			PatientRepository patientRepository,
			VisitRepository visitRepository,
			UserAccountRepository userAccountRepository,
			AuditService auditService) {
		this.labOrderRepository = labOrderRepository;
		this.patientRepository = patientRepository;
		this.visitRepository = visitRepository;
		this.userAccountRepository = userAccountRepository;
		this.auditService = auditService;
	}

	@Transactional
	public LabOrderResponse create(CreateLabOrderRequest request, String practitionerEmail) {
		PatientEntity patient = patientRepository.findById(request.patientId())
				.orElseThrow(() -> new IllegalArgumentException("Patient introuvable"));

		VisitEntity visit = null;
		if (request.visitId() != null) {
			visit = visitRepository.findById(request.visitId())
					.orElseThrow(() -> new IllegalArgumentException("Visite introuvable"));
		}

		UserAccountEntity practitioner = userAccountRepository.findByEmail(practitionerEmail)
				.orElseThrow(() -> new IllegalArgumentException("Praticien introuvable"));
		UUID organizationId = practitioner.getOrganizationId();

		String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		String prefix = "EXAM-REQ-" + dateStr + "-";
		long countToday = labOrderRepository.countByExamRequestNumberStartingWithGlobally(prefix);
		String sequence = String.format("%06d", countToday + 1);
		String examRequestNumber = prefix + sequence;

		LabOrderEntity entity = new LabOrderEntity(
				examRequestNumber,
				patient,
				visit,
				practitioner,
				request.targetOrganizationId(),
				request.examType(),
				request.exams(),
				request.reason(),
				request.priority(),
				organizationId);
		entity.setOrganizationId(organizationId);

		return mapToResponse(labOrderRepository.save(entity));
	}

	public List<LabOrderResponse> getPatientLabOrders(UUID patientId) {
		return labOrderRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
				.stream()
				.map(this::mapToResponse)
				.collect(Collectors.toList());
	}

	public List<LabOrderResponse> getLabOrders() {
		return labOrderRepository.findAllByOrderByCreatedAtDesc()
				.stream()
				.map(this::mapToResponse)
				.collect(Collectors.toList());
	}

	public LabOrderResponse getLabOrder(UUID orderId) {
		LabOrderEntity entity = labOrderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Demande d'examen introuvable"));
		return mapToResponse(entity);
	}

	@Transactional
	public LabOrderResponse updateStatus(UUID orderId, LabOrderStatus status, String practitionerEmail) {
		LabOrderEntity entity = findGlobal(orderId);
		UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
		try {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(entity.getOrganizationId());
			UserAccountEntity operator = resolveOperator(practitionerEmail);
			validateTargetOrganization(entity, operator);

			entity.setStatus(status);
			propagateLegacyOrderStatus(entity, status);
			LabOrderEntity saved = labOrderRepository.save(entity);
			logStatusChange(saved, "UPDATE_LAB_ORDER_STATUS", "Changement de statut laboratoire : " + status.name());
			return mapToResponse(saved);
		} finally {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
		}
	}

	@Transactional
	public LabOrderResponse updateItemStatus(
			UUID orderId,
			UUID itemId,
			LabOrderStatus status,
			String practitionerEmail) {
		LabOrderEntity entity = findGlobal(orderId);
		UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
		try {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(entity.getOrganizationId());
			UserAccountEntity operator = resolveOperator(practitionerEmail);
			validateTargetOrganization(entity, operator);

			LabOrderItemEntity item = entity.getItems().stream()
					.filter(candidate -> candidate.getId().equals(itemId))
					.findFirst()
					.orElseThrow(() -> new IllegalArgumentException("Examen introuvable dans cette demande"));

			validatePaymentGate(entity.getStatus(), status);
			validateItemTransition(item.getStatus(), status);
			item.applyStatus(status, Instant.now());
			recalculateOrderStatus(entity);
			LabOrderEntity saved = labOrderRepository.save(entity);
			logStatusChange(
					saved,
					"UPDATE_LAB_ORDER_ITEM_STATUS",
					"Changement de statut examen : " + item.getExamName() + " -> " + status.name());
			return mapToResponse(saved);
		} finally {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
		}
	}

	public void recalculateOrderStatus(LabOrderEntity entity) {
		List<LabOrderItemEntity> items = entity.getItems();
		if (items.isEmpty()) {
			return;
		}

		boolean allCancelled = items.stream().allMatch(item -> item.getStatus() == LabOrderStatus.CANCELLED);
		if (allCancelled) {
			entity.setStatus(LabOrderStatus.CANCELLED);
			return;
		}

		boolean allTerminal = items.stream().allMatch(item ->
				item.getStatus() == LabOrderStatus.VALIDATED || item.getStatus() == LabOrderStatus.CANCELLED);
		if (allTerminal) {
			entity.setStatus(LabOrderStatus.VALIDATED);
			return;
		}

		if (items.stream().anyMatch(item ->
				item.getStatus() == LabOrderStatus.RESULT_AVAILABLE || item.getStatus() == LabOrderStatus.VALIDATED)) {
			entity.setStatus(LabOrderStatus.RESULT_AVAILABLE);
			return;
		}
		if (items.stream().anyMatch(item -> item.getStatus() == LabOrderStatus.IN_PROGRESS)) {
			entity.setStatus(LabOrderStatus.IN_PROGRESS);
			return;
		}
		if (items.stream().anyMatch(item -> item.getStatus() == LabOrderStatus.SAMPLE_COLLECTED)) {
			entity.setStatus(LabOrderStatus.SAMPLE_COLLECTED);
			return;
		}

		if (entity.getStatus() != LabOrderStatus.AWAITING_PAYMENT && entity.getStatus() != LabOrderStatus.PAID) {
			entity.setStatus(LabOrderStatus.REQUESTED);
		}
	}

	void validatePaymentGate(LabOrderStatus orderStatus, LabOrderStatus next) {
		if (orderStatus == LabOrderStatus.AWAITING_PAYMENT && next != LabOrderStatus.CANCELLED) {
			throw new IllegalArgumentException("Le paiement doit être confirmé avant de traiter les examens");
		}
	}

	private void validateItemTransition(LabOrderStatus current, LabOrderStatus next) {
		if (current == next) {
			return;
		}
		boolean allowed = switch (current) {
			case REQUESTED -> next == LabOrderStatus.SAMPLE_COLLECTED || next == LabOrderStatus.CANCELLED;
			case SAMPLE_COLLECTED -> next == LabOrderStatus.IN_PROGRESS || next == LabOrderStatus.CANCELLED;
			case IN_PROGRESS -> next == LabOrderStatus.RESULT_AVAILABLE || next == LabOrderStatus.CANCELLED;
			case RESULT_AVAILABLE -> next == LabOrderStatus.VALIDATED;
			case VALIDATED, CANCELLED -> false;
			case AWAITING_PAYMENT, PAID -> next == LabOrderStatus.SAMPLE_COLLECTED || next == LabOrderStatus.CANCELLED;
		};
		if (!allowed) {
			throw new IllegalArgumentException(
					"Transition de statut examen invalide : " + current.name() + " -> " + next.name());
		}
	}

	private void propagateLegacyOrderStatus(LabOrderEntity entity, LabOrderStatus status) {
		if (status == LabOrderStatus.AWAITING_PAYMENT || status == LabOrderStatus.PAID) {
			return;
		}
		Instant now = Instant.now();
		for (LabOrderItemEntity item : entity.getItems()) {
			if (item.getStatus() == LabOrderStatus.CANCELLED && status != LabOrderStatus.CANCELLED) {
				continue;
			}
			item.applyStatus(status, now);
		}
	}

	private LabOrderEntity findGlobal(UUID orderId) {
		return labOrderRepository.findByIdGlobally(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Demande d'examen introuvable"));
	}

	private UserAccountEntity resolveOperator(String practitionerEmail) {
		return userAccountRepository.findByEmail(practitionerEmail)
				.orElseThrow(() -> new IllegalArgumentException("Praticien introuvable"));
	}

	private void validateTargetOrganization(LabOrderEntity entity, UserAccountEntity operator) {
		if (entity.getTargetOrganizationId() != null
				&& !entity.getTargetOrganizationId().equals(operator.getOrganizationId())) {
			throw new org.springframework.web.server.ResponseStatusException(
					org.springframework.http.HttpStatus.FORBIDDEN,
					"Seul le laboratoire cible peut modifier le statut de cette demande");
		}
	}

	private void logStatusChange(LabOrderEntity entity, String action, String details) {
		auditService.logSuccess(
				null,
				entity.getOrganizationId(),
				entity.getPatient().getId(),
				"LAB_ORDER",
				entity.getId(),
				action,
				details);
	}

	private LabOrderResponse mapToResponse(LabOrderEntity entity) {
		List<String> examsList = entity.getItems().stream()
				.map(LabOrderItemEntity::getExamName)
				.collect(Collectors.toList());
		List<LabOrderItemResponse> itemResponses = entity.getItems().stream()
				.map(item -> new LabOrderItemResponse(
						item.getId(),
						item.getExamName(),
						item.getStatus(),
						item.getSampleCollectedAt(),
						item.getResultAt(),
						item.getValidatedAt()))
				.toList();
		return new LabOrderResponse(
				entity.getId(),
				entity.getExamRequestNumber(),
				entity.getPatient().getId(),
				entity.getPatient().getFullName(),
				entity.getVisit() != null ? entity.getVisit().getId() : null,
				entity.getRequesterPractitioner().getId(),
				entity.getRequesterPractitioner().getDisplayName(),
				entity.getSourceOrganizationId(),
				entity.getTargetOrganizationId(),
				entity.getExamType(),
				examsList,
				itemResponses,
				entity.getReason(),
				entity.getPriority(),
				entity.getStatus(),
				entity.getCreatedAt());
	}
}
