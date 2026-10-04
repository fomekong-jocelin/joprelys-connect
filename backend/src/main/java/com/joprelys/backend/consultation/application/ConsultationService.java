package com.joprelys.backend.consultation.application;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.consultation.api.SaveConsultationRequest;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.consultation.api.ConsultationResponse;
import com.joprelys.backend.prescription.api.PrescriptionItemResponse;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
import com.joprelys.backend.visit.api.VitalsResponse;
import com.joprelys.backend.visit.application.VisitCareFlowUseCase;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConsultationService {

	private final ConsultationRepository consultationRepository;
	private final VisitRepository visitRepository;
	private final UserAccountRepository userAccountRepository;
	private final MedicalDocumentRepository medicalDocumentRepository;
	private final PrescriptionRepository prescriptionRepository;
	private final VisitCareFlowUseCase visitCareFlow;

	public ConsultationService(
			ConsultationRepository consultationRepository,
			VisitRepository visitRepository,
			UserAccountRepository userAccountRepository,
			MedicalDocumentRepository medicalDocumentRepository,
			PrescriptionRepository prescriptionRepository,
			VisitCareFlowUseCase visitCareFlow) {
		this.consultationRepository = consultationRepository;
		this.visitRepository = visitRepository;
		this.userAccountRepository = userAccountRepository;
		this.medicalDocumentRepository = medicalDocumentRepository;
		this.prescriptionRepository = prescriptionRepository;
		this.visitCareFlow = visitCareFlow;
	}

	@Transactional
	public ConsultationEntity saveConsultation(UUID visitId, String doctorEmail, SaveConsultationRequest request) {
		VisitEntity visit = visitRepository.findByIdForUpdate(visitId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable."));

		if (!"EN_COURS".equals(visit.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Une consultation ne peut être saisie que sur une visite active (EN_COURS).");
		}

		UserAccountEntity doctor = userAccountRepository.findByEmail(doctorEmail)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Médecin introuvable."));

		visitCareFlow.ensureConsultationOwnership(visit, doctor.getId(), doctor.getDisplayName());
		visitRepository.save(visit);

		String symptoms = textOrEmpty(request.symptoms());
		String diagnosis = textOrEmpty(request.diagnosis());
		ConsultationEntity consultation = consultationRepository.findByVisitId(visitId)
				.orElseGet(() -> {
					String docNumber = generateDocumentNumber();
					return new ConsultationEntity(visit, doctor, docNumber,
							symptoms, request.clinicalExam(),
							diagnosis, request.conclusion(),
							request.advice(), request.followUp());
				});

        if (consultation.getSignedAt() != null || !"BROUILLON".equals(consultation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une consultation signée ne peut plus être modifiée.");
        }

		if (request.expectedUpdatedAt() != null && consultation.getUpdatedAt() != null
				&& !consultation.getUpdatedAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
						.equals(request.expectedUpdatedAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS))) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Cette consultation a été modifiée entre-temps par un autre poste. Rechargez-la avant d'enregistrer.");
		}

		// L'auteur de la consultation reste le praticien qui l'a créée.
		consultation.setSymptoms(symptoms);
		consultation.setClinicalExam(request.clinicalExam());
		consultation.setDiagnosis(diagnosis);
		consultation.setConclusion(request.conclusion());
		consultation.setAdvice(request.advice());
		consultation.setFollowUp(request.followUp());

		return consultationRepository.save(consultation);
	}

	@Transactional(readOnly = true)
	public Optional<ConsultationEntity> getConsultationByVisitId(UUID visitId) {
		if (!visitRepository.existsById(visitId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable.");
		}
		return consultationRepository.findByVisitId(visitId);
	}

	private String generateDocumentNumber() {
		String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		long count = consultationRepository.countGlobally() + 1;
		return String.format("DOC-CONS-%s-%06d", date, count);
	}

	private String textOrEmpty(String value) {
		return value == null ? "" : value;
	}

	@Transactional(readOnly = true)
	public List<ConsultationEntity> getConsultationsByPatientId(UUID patientId) {
		return consultationRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
	}

	@Transactional(readOnly = true)
	public List<ConsultationResponse> getDetailedConsultationsByPatientId(UUID patientId) {
		return getConsultationsByPatientId(patientId).stream()
				.map(c -> {
					var doc = medicalDocumentRepository.findByVisitId(c.getVisit().getId()).orElse(null);
					var vitals = VitalsResponse.fromEntity(c.getVisit().getVitals());
					var prescriptionOpt = prescriptionRepository.findByConsultationId(c.getId());
					var prescriptionItems = prescriptionOpt
							.map(p -> p.getItems().stream()
									.map(PrescriptionItemResponse::fromEntity)
									.toList())
							.orElse(List.of());
					return ConsultationResponse.fromEntity(c, doc, vitals, prescriptionItems, prescriptionOpt.orElse(null));
				})
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<ConsultationResponse> findDetailedConsultationByVisitId(UUID visitId) {
		return consultationRepository.findByVisitId(visitId)
				.map(c -> {
					var doc = medicalDocumentRepository.findByVisitId(visitId).orElse(null);
					var vitals = VitalsResponse.fromEntity(c.getVisit().getVitals());
					var prescriptionOpt = prescriptionRepository.findByConsultationId(c.getId());
					var prescriptionItems = prescriptionOpt
							.map(p -> p.getItems().stream()
									.map(PrescriptionItemResponse::fromEntity)
									.toList())
							.orElse(List.of());
					return ConsultationResponse.fromEntity(
							c, doc, vitals, prescriptionItems, prescriptionOpt.orElse(null));
				});
	}

	@Transactional(readOnly = true)
	public ConsultationResponse getDetailedConsultationByVisitId(UUID visitId) {
		return findDetailedConsultationByVisitId(visitId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Aucune consultation trouvée pour cette visite."));
	}
}
