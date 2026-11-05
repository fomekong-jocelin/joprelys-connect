package com.joprelys.backend.lab.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.common.application.VerificationUrlProvider;
import com.joprelys.backend.lab.api.LabResultItem;
import com.joprelys.backend.lab.api.LabResultResponse;
import com.joprelys.backend.lab.api.LabResultUploadRequest;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderItemEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderRepository;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderStatus;
import com.joprelys.backend.lab.infrastructure.persistence.LabResultEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabResultRepository;
import com.joprelys.backend.lab.infrastructure.persistence.LabResultStatus;
import com.joprelys.backend.visit.application.DocumentNumberGenerator;
import com.joprelys.backend.visit.infrastructure.persistence.DocumentStatus;
import com.joprelys.backend.visit.infrastructure.persistence.DocumentType;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LabResultService {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(LabResultService.class);

	private final LabOrderRepository labOrderRepository;
	private final LabResultRepository labResultRepository;
	private final LabOrderService labOrderService;
	private final AuditService auditService;
	private final com.joprelys.backend.notification.application.NotificationService notificationService;
	private final com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository userAccountRepository;
	private final DocumentNumberGenerator documentNumberGenerator;
	private final MedicalDocumentRepository medicalDocumentRepository;
	private final VerificationUrlProvider verificationUrlProvider;

	@Value("${joprelys.documents.storage-dir:./storage/documents}")
	private String storageDir;

	public LabResultService(
			LabOrderRepository labOrderRepository,
			LabResultRepository labResultRepository,
			LabOrderService labOrderService,
			AuditService auditService,
			com.joprelys.backend.notification.application.NotificationService notificationService,
			com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository userAccountRepository,
			DocumentNumberGenerator documentNumberGenerator,
			MedicalDocumentRepository medicalDocumentRepository,
			VerificationUrlProvider verificationUrlProvider) {
		this.labOrderRepository = labOrderRepository;
		this.labResultRepository = labResultRepository;
		this.labOrderService = labOrderService;
		this.auditService = auditService;
		this.notificationService = notificationService;
		this.userAccountRepository = userAccountRepository;
		this.documentNumberGenerator = documentNumberGenerator;
		this.medicalDocumentRepository = medicalDocumentRepository;
		this.verificationUrlProvider = verificationUrlProvider;
	}

	/** Résultats visibles par le patient : uniquement les résultats validés par le laboratoire. */
	@Transactional(readOnly = true)
	public List<LabResultResponse> getPatientPublishedResults(UUID patientId) {
		return getPatientResults(patientId).stream()
				.filter(result -> LabResultStatus.VALIDATED.name().equals(result.status()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<LabResultResponse> getPatientResults(UUID patientId) {
		List<LabResultEntity> allResults = labResultRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
		java.util.Map<String, LabResultEntity> latestMap = new java.util.HashMap<>();
		for (LabResultEntity result : allResults) {
			String key = result.getResultNumber() + "_" + result.getAnalyteName();
			LabResultEntity existing = latestMap.get(key);
			if (existing == null || result.getVersion() > existing.getVersion()) {
				latestMap.put(key, result);
			}
		}
		return latestMap.values().stream()
				.map(LabResultResponse::fromEntity)
				.sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
				.toList();
	}

	/** Saisie par un membre du personnel : le validateur est toujours l'utilisateur connecté (ADR-20261017 D-04). */
	@Transactional
	public void uploadResultsByStaff(LabResultUploadRequest request, String actorEmail) {
		var actor = userAccountRepository.findByEmail(actorEmail.trim().toLowerCase())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable."));
		LabResultUploadRequest attributed = new LabResultUploadRequest(
				request.examRequestNumber(),
				actor.getDisplayName(),
				actor.getId(),
				// Sans statut explicite, une saisie reste un brouillon : seule une validation volontaire publie (CLIN-05).
				request.status() != null ? request.status() : LabResultStatus.DRAFT,
				request.sampleCollectedAt(),
				request.resultAt(),
				request.validatedAt(),
				request.conclusion(),
				request.results(),
				request.pdfBase64(),
				request.labOrderItemId());
		upload(attributed, actor.getId());
	}

	/** Intégration partenaire (clé d'API) : pas d'utilisateur connecté. */
	@Transactional
	public void uploadResults(LabResultUploadRequest request) {
		upload(request, null);
	}

	private void upload(LabResultUploadRequest request, UUID actorId) {
		requireKnownInterpretations(request.results());
		LabOrderEntity labOrder = labOrderRepository.findByExamRequestNumber(request.examRequestNumber())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande d'examen introuvable."));

		if (labOrder.getStatus() == LabOrderStatus.CANCELLED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de téléverser des résultats sur une demande annulée.");
		}

		LabOrderItemEntity labOrderItem = resolveItem(labOrder, request.labOrderItemId());
		if (labOrderItem != null && labOrderItem.getStatus() == LabOrderStatus.CANCELLED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de téléverser un résultat sur un examen annulé.");
		}

		String pdfFilePath = storePdfIfPresent(labOrder, request.pdfBase64());
		List<LabResultEntity> existingResults = labOrderItem != null
				? labResultRepository.findByLabOrderItemId(labOrderItem.getId())
				: labResultRepository.findByLabOrderId(labOrder.getId());
		boolean hasValidated = existingResults.stream()
				.anyMatch(result -> result.getStatus() == LabResultStatus.VALIDATED);

		String resultNumber;
		int nextVersion = 1;
		if (!existingResults.isEmpty()) {
			resultNumber = existingResults.get(0).getResultNumber();
			if (hasValidated) {
				nextVersion = existingResults.stream()
						.mapToInt(LabResultEntity::getVersion)
						.max()
						.orElse(1) + 1;
			} else {
				labResultRepository.deleteAll(existingResults);
			}
		} else {
			resultNumber = nextResultNumber();
		}

		LabResultStatus finalStatus = request.status() != null ? request.status() : LabResultStatus.VALIDATED;
		UUID validatorUserId = resolveValidatorUserId(request);
		UUID savedDocId = registerPdfDocument(labOrder, pdfFilePath, validatorUserId);

		for (LabResultItem resultItem : request.results()) {
			final int currentMaxVersion = nextVersion - 1;
			LabResultEntity parentResult = existingResults.stream()
					.filter(result -> result.getVersion() == currentMaxVersion
							&& result.getAnalyteName().equals(resultItem.analyteName()))
					.findFirst()
					.orElse(null);

			LabResultEntity resultEntity = new LabResultEntity(
					resultNumber,
					labOrder,
					labOrderItem,
					labOrder.getPatient(),
					request.validatorName(),
					finalStatus,
					validatorUserId,
					request.conclusion(),
					savedDocId,
					nextVersion,
					parentResult,
					resultItem.analyteName(),
					resultItem.value(),
					resultItem.unit(),
					resultItem.referenceRange(),
					interpretationOf(resultItem).name(),
					resultItem.comment(),
					pdfFilePath,
					request.sampleCollectedAt(),
					request.resultAt(),
					request.validatedAt());
			labResultRepository.save(resultEntity);
		}

		applyResultStatus(labOrder, labOrderItem, finalStatus, request);
		labOrderRepository.save(labOrder);
		if (finalStatus == LabResultStatus.VALIDATED) {
			notifyPatient(labOrder, request);
		}
		auditService.logSuccess(
				actorId,
				labOrder.getOrganizationId(),
				labOrder.getPatient().getId(),
				"LAB_ORDER",
				labOrder.getId(),
				"UPLOAD_LAB_RESULTS",
				labOrderItem == null
						? "Téléversement de résultats d'analyses pour la demande " + labOrder.getExamRequestNumber()
						: "Téléversement de résultats pour l'examen " + labOrderItem.getExamName());
	}

	private LabOrderItemEntity resolveItem(LabOrderEntity labOrder, UUID itemId) {
		if (itemId == null) {
			return null;
		}
		return labOrder.getItems().stream()
				.filter(item -> item.getId().equals(itemId))
				.findFirst()
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.BAD_REQUEST,
						"L'examen ciblé n'appartient pas à cette demande."));
	}

	private void applyResultStatus(
			LabOrderEntity labOrder,
			LabOrderItemEntity item,
			LabResultStatus resultStatus,
			LabResultUploadRequest request) {
		LabOrderStatus orderStatus = resultStatus == LabResultStatus.VALIDATED
				? LabOrderStatus.VALIDATED
				: LabOrderStatus.RESULT_AVAILABLE;
		Instant now = Instant.now();
		if (item != null) {
			item.applyResultStatus(
					orderStatus,
					request.sampleCollectedAt(),
					request.resultAt(),
					request.validatedAt(),
					now);
			labOrderService.recalculateOrderStatus(labOrder);
			return;
		}

		for (LabOrderItemEntity legacyItem : labOrder.getItems()) {
			if (legacyItem.getStatus() != LabOrderStatus.CANCELLED) {
				legacyItem.applyResultStatus(
						orderStatus,
						request.sampleCollectedAt(),
						request.resultAt(),
						request.validatedAt(),
						now);
			}
		}
		labOrder.setStatus(orderStatus);
	}

	private String storePdfIfPresent(LabOrderEntity labOrder, String pdfBase64) {
		if (pdfBase64 == null || pdfBase64.isBlank()) {
			return null;
		}
		try {
			String base64Data = pdfBase64.trim();
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
			if (pdfBytes.length < 4
					|| pdfBytes[0] != 0x25
					|| pdfBytes[1] != 0x50
					|| pdfBytes[2] != 0x44
					|| pdfBytes[3] != 0x46) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier n'est pas un document PDF valide (signature magique manquante).");
			}

			Path targetDir = Paths.get(storageDir).toAbsolutePath().normalize();
			if (!Files.exists(targetDir)) {
				Files.createDirectories(targetDir);
			}
			String fileName = "lab-result-" + labOrder.getExamRequestNumber() + "-" + System.currentTimeMillis() + ".pdf";
			Path filePath = targetDir.resolve(fileName);
			Files.write(filePath, pdfBytes);
			return filePath.toString();
		} catch (IllegalArgumentException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format PDF Base64 invalide.", exception);
		} catch (IOException exception) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de l'écriture du fichier PDF sur le disque.", exception);
		}
	}

	private String nextResultNumber() {
		String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		Long sequence;
		try {
			sequence = labResultRepository.getNextResultNumberSequenceValue();
		} catch (Exception exception) {
			sequence = labResultRepository.count() + 1;
		}
		return String.format("EXAM-RES-%s-%06d", dateStr, sequence);
	}

	private UUID resolveValidatorUserId(LabResultUploadRequest request) {
		UUID validatorUserId = request.validatorUserId();
		if (validatorUserId != null || request.validatorName() == null || request.validatorName().isBlank()) {
			return validatorUserId;
		}
		return userAccountRepository.findAll().stream()
				.filter(user -> user.getDisplayName().equalsIgnoreCase(request.validatorName().trim()))
				.map(com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity::getId)
				.findFirst()
				.orElse(null);
	}

	private UUID registerPdfDocument(LabOrderEntity labOrder, String pdfFilePath, UUID validatorUserId) {
		if (pdfFilePath == null || labOrder.getVisit() == null) {
			return null;
		}
		try {
			byte[] pdfBytes = Files.readAllBytes(Paths.get(pdfFilePath));
			java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(pdfBytes);
			StringBuilder hexString = new StringBuilder();
			for (byte value : hashBytes) {
				String hex = Integer.toHexString(0xff & value);
				if (hex.length() == 1) {
					hexString.append('0');
				}
				hexString.append(hex);
			}

			String docNum = documentNumberGenerator.generateNextDocumentNumber();
			MedicalDocumentEntity document = new MedicalDocumentEntity(
					labOrder.getVisit(),
					docNum,
					pdfFilePath,
					DocumentType.RESULTAT_LABORATOIRE);
			document.setHash(hexString.toString());
			document.setVerificationUrl(verificationUrlProvider.getVerificationUrl("verify/" + document.getId()));
			document.setQrCodeUrl("/api/public/documents/" + document.getId() + "/qr");
			if (validatorUserId != null) {
				document.setAuthorUserId(validatorUserId);
			}

			List<MedicalDocumentEntity> previousDocs = medicalDocumentRepository
					.findAllByVisitIdAndDocumentTypeOrderByVersionDesc(
							labOrder.getVisit().getId(),
							DocumentType.RESULTAT_LABORATOIRE);
			if (!previousDocs.isEmpty()) {
				MedicalDocumentEntity previous = previousDocs.get(0);
				previous.setStatus(DocumentStatus.REMPLACE);
				medicalDocumentRepository.save(previous);
				document.setPreviousDocumentId(previous.getId());
				document.setVersion(previous.getVersion() + 1);
			} else {
				document.setVersion(1);
			}
			return medicalDocumentRepository.save(document).getId();
		} catch (java.io.IOException | java.security.NoSuchAlgorithmException | RuntimeException exception) {
			// Le résultat reste enregistré ; l'absence de document vérifiable doit être visible en exploitation.
			LOG.error("Lab result document could not be created for lab order {}", labOrder.getId(), exception);
			return null;
		}
	}

	/**
	 * Message au patient gradué selon l'interprétation validée (CLIN-04) : seule une valeur critique déclenche
	 * une notification d'urgence ; une valeur simplement anormale invite à en parler avec son médecin.
	 */
	private void notifyPatient(LabOrderEntity labOrder, LabResultUploadRequest request) {
		List<LabInterpretation> interpretations = request.results().stream().map(LabResultService::interpretationOf).toList();
		boolean hasCritical = interpretations.contains(LabInterpretation.CRITICAL);
		boolean hasAbnormal = interpretations.stream().anyMatch(LabInterpretation::isOutsideReference);
		String available = "Vos résultats d'analyses pour la demande " + labOrder.getExamRequestNumber()
				+ " sont disponibles dans votre espace patient.";
		String title;
		String message;
		String type;
		if (hasCritical) {
			title = "Résultats d'analyses disponibles — à revoir rapidement avec un médecin";
			message = available + " Une valeur doit être revue rapidement avec un médecin : contactez votre médecin ou l'établissement."
					+ " En cas de malaise ou de symptômes inquiétants, rendez-vous sans attendre aux urgences.";
			type = "EMERGENCY";
		} else if (hasAbnormal) {
			title = "Résultats d'analyses disponibles";
			message = available + " Certaines valeurs sont en dehors des valeurs de référence du laboratoire."
					+ " Une valeur hors de ces limites n'est pas forcément anormale pour vous : parlez-en avec votre médecin.";
			type = "INFO";
		} else {
			title = "Résultats d'analyses disponibles";
			message = available;
			type = "INFO";
		}
		notificationService.sendNotification(labOrder.getPatient().getId(), title, message, type);
	}

	private static void requireKnownInterpretations(List<LabResultItem> results) {
		if (results != null && results.stream().anyMatch(item -> LabInterpretation.fromCanonical(item.interpretation()).isEmpty())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Interprétation inconnue : utilisez NORMAL, LOW, HIGH, ABNORMAL, CRITICAL ou NOT_INTERPRETED.");
		}
	}

	private static LabInterpretation interpretationOf(LabResultItem item) {
		return LabInterpretation.fromCanonical(item.interpretation()).orElse(LabInterpretation.NOT_INTERPRETED);
	}

	@Transactional(readOnly = true)
	public byte[] getResultPdfBytes(UUID resultId) {
		LabResultEntity result = labResultRepository.findById(resultId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Résultat introuvable."));
		if (result.getPdfFilePath() == null || result.getPdfFilePath().isBlank()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun fichier PDF associé à ce résultat.");
		}
		try {
			return Files.readAllBytes(Paths.get(result.getPdfFilePath()));
		} catch (IOException exception) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la lecture du fichier PDF.", exception);
		}
	}

	@Transactional(readOnly = true)
	public LabResultResponse getResultById(UUID resultId) {
		LabResultEntity result = labResultRepository.findById(resultId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Résultat introuvable."));
		return LabResultResponse.fromEntity(result);
	}
}
