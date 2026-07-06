package com.joprelys.backend.lab.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.lab.api.LabResultItem;
import com.joprelys.backend.lab.api.LabResultResponse;
import com.joprelys.backend.lab.api.LabResultUploadRequest;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderRepository;
import com.joprelys.backend.lab.infrastructure.persistence.LabResultEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabResultRepository;
import com.joprelys.backend.visit.application.DocumentNumberGenerator;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentRepository;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity;
import com.joprelys.backend.visit.infrastructure.persistence.DocumentType;
import com.joprelys.backend.visit.infrastructure.persistence.DocumentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class LabResultService {

	private final LabOrderRepository labOrderRepository;
	private final LabResultRepository labResultRepository;
	private final AuditService auditService;
	private final com.joprelys.backend.notification.application.NotificationService notificationService;
	private final com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository userAccountRepository;
	private final DocumentNumberGenerator documentNumberGenerator;
	private final MedicalDocumentRepository medicalDocumentRepository;

	@Value("${joprelys.documents.storage-dir:./storage/documents}")
	private String storageDir;

	@Value("${joprelys.documents.verification-base-url:http://localhost:4200/verify}")
	private String verificationBaseUrl;

	public LabResultService(
			LabOrderRepository labOrderRepository,
			LabResultRepository labResultRepository,
			AuditService auditService,
			com.joprelys.backend.notification.application.NotificationService notificationService,
			com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository userAccountRepository,
			DocumentNumberGenerator documentNumberGenerator,
			MedicalDocumentRepository medicalDocumentRepository) {
		this.labOrderRepository = labOrderRepository;
		this.labResultRepository = labResultRepository;
		this.auditService = auditService;
		this.notificationService = notificationService;
		this.userAccountRepository = userAccountRepository;
		this.documentNumberGenerator = documentNumberGenerator;
		this.medicalDocumentRepository = medicalDocumentRepository;
	}

	@Transactional(readOnly = true)
	public java.util.List<LabResultResponse> getPatientResults(UUID patientId) {
		java.util.List<LabResultEntity> allResults = labResultRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
		
		java.util.Map<String, LabResultEntity> latestMap = new java.util.HashMap<>();
		for (LabResultEntity r : allResults) {
			String key = r.getResultNumber() + "_" + r.getAnalyteName();
			LabResultEntity existing = latestMap.get(key);
			if (existing == null || r.getVersion() > existing.getVersion()) {
				latestMap.put(key, r);
			}
		}
		
		return latestMap.values().stream()
				.map(LabResultResponse::fromEntity)
				.sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
				.toList();
	}

	@Transactional
	public void uploadResults(LabResultUploadRequest request) {
		LabOrderEntity labOrder = labOrderRepository.findByExamRequestNumber(request.examRequestNumber())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande d'examen introuvable."));

		if (labOrder.getStatus() == com.joprelys.backend.lab.infrastructure.persistence.LabOrderStatus.CANCELLED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de téléverser des résultats sur une demande annulée.");
		}

		String pdfFilePath = null;
		if (request.pdfBase64() != null && !request.pdfBase64().isBlank()) {
			try {
				String base64Data = request.pdfBase64().trim();
				if (base64Data.startsWith("data:")) {
					int commaIndex = base64Data.indexOf(',');
					if (commaIndex == -1) {
						throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format Base64 invalide.");
					}
					String metadata = base64Data.substring(0, commaIndex);
					if (!metadata.startsWith("data:application/pdf;base64")) {
						throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le type de fichier doit être strictement application/pdf.");
					}
					base64Data = base64Data.substring(commaIndex + 1);
				}

				byte[] pdfBytes = Base64.getDecoder().decode(base64Data);

				if (pdfBytes.length > 5 * 1024 * 1024) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier PDF dépasse la taille maximale autorisée de 5 Mo.");
				}

				if (pdfBytes.length < 4 || pdfBytes[0] != 0x25 || pdfBytes[1] != 0x50 || pdfBytes[2] != 0x44 || pdfBytes[3] != 0x46) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier n'est pas un document PDF valide (signature magique manquante).");
				}

				Path targetDir = Paths.get(storageDir).toAbsolutePath().normalize();
				if (!Files.exists(targetDir)) {
					Files.createDirectories(targetDir);
				}

				String fileName = "lab-result-" + labOrder.getExamRequestNumber() + "-" + System.currentTimeMillis() + ".pdf";
				Path filePath = targetDir.resolve(fileName);
				Files.write(filePath, pdfBytes);
				pdfFilePath = filePath.toString();
			} catch (IllegalArgumentException e) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format PDF Base64 invalide.", e);
			} catch (IOException e) {
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de l'écriture du fichier PDF sur le disque.", e);
			}
		}

		java.util.List<LabResultEntity> existingResults = labResultRepository.findByLabOrderId(labOrder.getId());
		boolean hasValidated = existingResults.stream()
				.anyMatch(r -> r.getStatus() == com.joprelys.backend.lab.infrastructure.persistence.LabResultStatus.VALIDATED);

		String resultNumber;
		int nextVersion = 1;

		if (!existingResults.isEmpty()) {
			resultNumber = existingResults.get(0).getResultNumber();
			if (hasValidated) {
				int maxVersion = existingResults.stream()
						.mapToInt(LabResultEntity::getVersion)
						.max()
						.orElse(1);
				nextVersion = maxVersion + 1;
			} else {
				// Tous sont DRAFT, on les supprime pour recréer proprement à la version 1
				labResultRepository.deleteAll(existingResults);
				nextVersion = 1;
			}
		} else {
			String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
			Long seqVal;
			try {
				seqVal = labResultRepository.getNextResultNumberSequenceValue();
			} catch (Exception e) {
				seqVal = labResultRepository.count() + 1;
			}
			resultNumber = String.format("EXAM-RES-%s-%06d", dateStr, seqVal);
		}

		com.joprelys.backend.lab.infrastructure.persistence.LabResultStatus finalStatus =
				request.status() != null ? request.status() : com.joprelys.backend.lab.infrastructure.persistence.LabResultStatus.VALIDATED;
		UUID validatorUserId = request.validatorUserId();
		if (validatorUserId == null && request.validatorName() != null && !request.validatorName().isBlank()) {
			validatorUserId = userAccountRepository.findAll().stream()
					.filter(u -> u.getDisplayName().equalsIgnoreCase(request.validatorName().trim()))
					.map(com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity::getId)
					.findFirst()
					.orElse(null);
		}

		UUID savedDocId = null;
		if (pdfFilePath != null) {
			try {
				byte[] pdfBytes = Files.readAllBytes(Paths.get(pdfFilePath));
				java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
				byte[] hashBytes = digest.digest(pdfBytes);
				StringBuilder hexString = new StringBuilder();
				for (byte b : hashBytes) {
					String hex = Integer.toHexString(0xff & b);
					if (hex.length() == 1) hexString.append('0');
					hexString.append(hex);
				}
				String hash = hexString.toString();

				String docNum = documentNumberGenerator.generateNextDocumentNumber();
				MedicalDocumentEntity doc = new MedicalDocumentEntity(
						labOrder.getVisit(),
						docNum,
						pdfFilePath,
						DocumentType.RESULTAT_LABORATOIRE
				);
				doc.setHash(hash);

				String verificationUrl = verificationBaseUrl + "/verify/" + doc.getId();
				String qrCodeUrl = "/api/public/documents/" + doc.getId() + "/qr";
				doc.setVerificationUrl(verificationUrl);
				doc.setQrCodeUrl(qrCodeUrl);

				if (validatorUserId != null) {
					doc.setAuthorUserId(validatorUserId);
				}

				List<MedicalDocumentEntity> previousDocs = medicalDocumentRepository.findAllByVisitIdAndDocumentTypeOrderByVersionDesc(labOrder.getVisit().getId(), DocumentType.RESULTAT_LABORATOIRE);
				if (!previousDocs.isEmpty()) {
					MedicalDocumentEntity previous = previousDocs.get(0);
					previous.setStatus(DocumentStatus.REMPLACE);
					medicalDocumentRepository.save(previous);
					doc.setPreviousDocumentId(previous.getId());
					doc.setVersion(previous.getVersion() + 1);
				} else {
					doc.setVersion(1);
				}

				MedicalDocumentEntity savedDoc = medicalDocumentRepository.save(doc);
				savedDocId = savedDoc.getId();
			} catch (Exception e) {
				// failed to register PDF as medical document
			}
		}

		for (LabResultItem item : request.results()) {
			final int currentMaxVersion = nextVersion - 1;
			LabResultEntity parentResult = existingResults.stream()
					.filter(r -> r.getVersion() == currentMaxVersion && r.getAnalyteName().equals(item.analyteName()))
					.findFirst()
					.orElse(null);

			LabResultEntity resultEntity = new LabResultEntity(
					resultNumber,
					labOrder,
					labOrder.getPatient(),
					request.validatorName(),
					finalStatus,
					validatorUserId,
					request.conclusion(),
					savedDocId, // documentId relié
					nextVersion,
					parentResult,
					item.analyteName(),
					item.value(),
					item.unit(),
					item.referenceRange(),
					item.interpretation(),
					item.comment(),
					pdfFilePath,
					request.sampleCollectedAt(),
					request.resultAt(),
					request.validatedAt()
			);
			labResultRepository.save(resultEntity);
		}

		if (finalStatus == com.joprelys.backend.lab.infrastructure.persistence.LabResultStatus.VALIDATED) {
			labOrder.setStatus(com.joprelys.backend.lab.infrastructure.persistence.LabOrderStatus.VALIDATED);
		} else {
			labOrder.setStatus(com.joprelys.backend.lab.infrastructure.persistence.LabOrderStatus.RESULT_AVAILABLE);
		}
		labOrderRepository.save(labOrder);

		// Notifier le patient que des résultats sont disponibles
		boolean hasCritical = request.results().stream()
				.anyMatch(r -> "CRITICAL".equals(r.interpretation()) || "ABNORMAL".equals(r.interpretation()));

		String notifTitle = hasCritical
				? "Résultats d'analyses disponibles — Valeur critique"
				: "Résultats d'analyses disponibles";
		String notifType = hasCritical ? "EMERGENCY" : "INFO";
		String notifMessage = hasCritical
				? "Vos résultats d'analyses sont disponibles. Une ou plusieurs valeurs nécessitent une attention médicale immédiate."
				: "Vos résultats d'analyses pour la demande " + labOrder.getExamRequestNumber() + " sont disponibles.";

		notificationService.sendNotification(
				labOrder.getPatient().getId(),
				notifTitle,
				notifMessage,
				notifType
		);

		auditService.logSuccess(
				null,
				labOrder.getOrganizationId(),
				labOrder.getPatient().getId(),
				"LAB_ORDER",
				labOrder.getId(),
				"UPLOAD_LAB_RESULTS",
				"Téléversement de résultats d'analyses pour la demande " + labOrder.getExamRequestNumber()
		);
	}

	@Transactional(readOnly = true)
	public byte[] getResultPdfBytes(UUID resultId) {
		LabResultEntity result = labResultRepository.findById(resultId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Résultat introuvable."));

		if (result.getPdfFilePath() == null || result.getPdfFilePath().isBlank()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun fichier PDF associé à ce résultat.");
		}

		try {
			return java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(result.getPdfFilePath()));
		} catch (java.io.IOException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la lecture du fichier PDF.", e);
		}
	}

	@Transactional(readOnly = true)
	public LabResultResponse getResultById(UUID resultId) {
		LabResultEntity result = labResultRepository.findById(resultId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Résultat introuvable."));
		return LabResultResponse.fromEntity(result);
	}
}
