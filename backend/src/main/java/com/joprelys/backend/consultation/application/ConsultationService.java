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

	public ConsultationService(
			ConsultationRepository consultationRepository,
			VisitRepository visitRepository,
			UserAccountRepository userAccountRepository,
			MedicalDocumentRepository medicalDocumentRepository,
			PrescriptionRepository prescriptionRepository) {
		this.consultationRepository = consultationRepository;
		this.visitRepository = visitRepository;
		this.userAccountRepository = userAccountRepository;
		this.medicalDocumentRepository = medicalDocumentRepository;
		this.prescriptionRepository = prescriptionRepository;
	}

	@Transactional
	public ConsultationEntity saveConsultation(UUID visitId, String doctorEmail, SaveConsultationRequest request) {
		VisitEntity visit = visitRepository.findById(visitId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable."));

		if (!"EN_COURS".equals(visit.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Une consultation ne peut être saisie que sur une visite active (EN_COURS).");
		}

		UserAccountEntity doctor = userAccountRepository.findByEmail(doctorEmail)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Médecin introuvable."));

		ConsultationEntity consultation = consultationRepository.findByVisitId(visitId)
				.orElseGet(() -> {
					String docNumber = generateDocumentNumber();
					return new ConsultationEntity(visit, doctor, docNumber,
							request.symptoms(), request.clinicalExam(),
							request.suspectedDiagnosis(), request.diagnosis(),
							request.finalDiagnosis(), request.conclusion(),
							request.advice(), request.followUp());
				});

		consultation.setSymptoms(request.symptoms());
		consultation.setClinicalExam(request.clinicalExam());
		consultation.setSuspectedDiagnosis(request.suspectedDiagnosis());
		consultation.setDiagnosis(request.diagnosis());
		consultation.setFinalDiagnosis(request.finalDiagnosis());
		consultation.setConclusion(request.conclusion());
		consultation.setAdvice(request.advice());
		consultation.setFollowUp(request.followUp());
		consultation.setDoctor(doctor);

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
