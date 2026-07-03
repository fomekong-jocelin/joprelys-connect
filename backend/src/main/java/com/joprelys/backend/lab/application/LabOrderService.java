package com.joprelys.backend.lab.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.lab.api.CreateLabOrderRequest;
import com.joprelys.backend.lab.api.LabOrderResponse;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LabOrderService {

	private final LabOrderRepository labOrderRepository;
	private final PatientRepository patientRepository;
	private final VisitRepository visitRepository;
	private final UserAccountRepository userAccountRepository;
	private final AuditService auditService;
	private static final Set<String> ALLOWED_STATUSES = Set.of(
			"REQUESTED", "SAMPLE_COLLECTED", "IN_PROGRESS", "RESULT_AVAILABLE", "VALIDATED", "CANCELLED"
	);

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

		// Générer le numéro unique EXAM-REQ-YYYYMMDD-XXXXXX
		String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		String prefix = "EXAM-REQ-" + dateStr + "-";
		long countToday = labOrderRepository.countByExamRequestNumberStartingWith(prefix);
		String sequence = String.format("%06d", countToday + 1);
		String examRequestNumber = prefix + sequence;

		// Concaténer la liste d'examens sous forme de chaîne de caractères
		String examsJoined = String.join(",", request.exams());

		LabOrderEntity entity = new LabOrderEntity(
				examRequestNumber,
				patient,
				visit,
				practitioner,
				request.targetOrganizationId(),
				request.examType(),
				examsJoined,
				request.reason(),
				request.priority()
		);
		entity.setOrganizationId(organizationId);

		LabOrderEntity saved = labOrderRepository.save(entity);
		return mapToResponse(saved);
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
	public LabOrderResponse updateStatus(UUID orderId, String status) {
		if (!ALLOWED_STATUSES.contains(status)) {
			throw new IllegalArgumentException("Statut laboratoire non autorisé");
		}
		LabOrderEntity entity = labOrderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Demande d'examen introuvable"));
		entity.setStatus(status);
		LabOrderEntity saved = labOrderRepository.save(entity);
		auditService.logSuccess(
				null,
				saved.getOrganizationId(),
				saved.getPatient().getId(),
				"LAB_ORDER",
				saved.getId(),
				"UPDATE_LAB_ORDER_STATUS",
				"Changement de statut laboratoire : " + status
		);
		return mapToResponse(saved);
	}

	private LabOrderResponse mapToResponse(LabOrderEntity entity) {
		List<String> examsList = List.of(entity.getExams().split(",\\s*"));
		return new LabOrderResponse(
				entity.getId(),
				entity.getExamRequestNumber(),
				entity.getPatient().getId(),
				entity.getPatient().getFullName(),
				entity.getVisit() != null ? entity.getVisit().getId() : null,
				entity.getRequesterPractitioner().getId(),
				entity.getRequesterPractitioner().getDisplayName(),
				entity.getOrganizationId(),
				entity.getTargetOrganizationId(),
				entity.getExamType(),
				examsList,
				entity.getReason(),
				entity.getPriority(),
				entity.getStatus(),
				entity.getCreatedAt()
		);
	}
}
