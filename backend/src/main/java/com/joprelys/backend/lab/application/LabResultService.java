package com.joprelys.backend.lab.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.lab.api.LabResultItem;
import com.joprelys.backend.lab.api.LabResultResponse;
import com.joprelys.backend.lab.api.LabResultUploadRequest;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderRepository;
import com.joprelys.backend.lab.infrastructure.persistence.LabResultEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabResultRepository;
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
import java.util.UUID;

@Service
public class LabResultService {

	private final LabOrderRepository labOrderRepository;
	private final LabResultRepository labResultRepository;
	private final AuditService auditService;
	private final com.joprelys.backend.notification.application.NotificationService notificationService;

	@Value("${joprelys.documents.storage-dir:./storage/documents}")
	private String storageDir;

	public LabResultService(
			LabOrderRepository labOrderRepository,
			LabResultRepository labResultRepository,
			AuditService auditService,
			com.joprelys.backend.notification.application.NotificationService notificationService) {
		this.labOrderRepository = labOrderRepository;
		this.labResultRepository = labResultRepository;
		this.auditService = auditService;
		this.notificationService = notificationService;
	}

	@Transactional(readOnly = true)
	public java.util.List<LabResultResponse> getPatientResults(UUID patientId) {
		return labResultRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
				.stream()
				.map(LabResultResponse::fromEntity)
				.toList();
	}

	@Transactional
	public void uploadResults(LabResultUploadRequest request) {
		LabOrderEntity labOrder = labOrderRepository.findByExamRequestNumber(request.examRequestNumber())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande d'examen introuvable."));

		if ("CANCELLED".equals(labOrder.getStatus())) {
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

		String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		long nextVal = labResultRepository.count() + 1;
		String resultNumber = String.format("EXAM-RES-%s-%06d", dateStr, nextVal);

		for (LabResultItem item : request.results()) {
			LabResultEntity resultEntity = new LabResultEntity(
					resultNumber,
					labOrder,
					labOrder.getPatient(),
					request.validatorName(),
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

		labOrder.setStatus("VALIDATED");
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
}
