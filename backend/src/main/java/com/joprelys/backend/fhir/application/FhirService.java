package com.joprelys.backend.fhir.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.fhir.*;
import com.joprelys.backend.fhir.model.*;
import com.joprelys.backend.patient.application.PatientService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.application.VisitService;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VitalsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FhirService {

	private final PatientService patientService;
	private final PatientRepository patientRepository;
	private final VisitService visitService;
	private final VitalsRepository vitalsRepository;
	private final com.joprelys.backend.lab.infrastructure.persistence.LabResultRepository labResultRepository;
	private final AuditService auditService;
	private final UserAccountRepository userAccountRepository;

	public FhirService(
			PatientService patientService,
			PatientRepository patientRepository,
			VisitService visitService,
			VitalsRepository vitalsRepository,
			com.joprelys.backend.lab.infrastructure.persistence.LabResultRepository labResultRepository,
			AuditService auditService,
			UserAccountRepository userAccountRepository) {
		this.patientService = patientService;
		this.patientRepository = patientRepository;
		this.visitService = visitService;
		this.vitalsRepository = vitalsRepository;
		this.labResultRepository = labResultRepository;
		this.auditService = auditService;
		this.userAccountRepository = userAccountRepository;
	}

	@Transactional(readOnly = true)
	public FhirPatientDto getPatient(UUID id) {
		PatientEntity patient = patientService.getPatientById(id);
		
		var actor = getCurrentUser();
		if (actor != null) {
			auditService.logSuccess(
					actor.getId(),
					actor.getOrganizationId(),
					patient.getId(),
					"Patient",
					patient.getId(),
					"READ_FHIR_RESOURCE",
					"Accès FHIR à la ressource Patient : " + patient.getFullName()
			);
		}
		
		return FhirPatientMapper.toFhir(patient);
	}

	@Transactional(readOnly = true)
	public FhirEncounterDto getEncounter(UUID id) {
		VisitEntity visit = visitService.getVisitGlobally(id);
		if (visit == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable");
		}

		
		// Enforce tenant/consent security by invoking the patient check
		if (visit.getPatient() != null) {
			patientService.getPatientById(visit.getPatient().getId());
		}
		
		var actor = getCurrentUser();
		if (actor != null) {
			UUID patientId = visit.getPatient() != null ? visit.getPatient().getId() : null;
			auditService.logSuccess(
					actor.getId(),
					actor.getOrganizationId(),
					patientId,
					"Encounter",
					visit.getId(),
					"READ_FHIR_RESOURCE",
					"Accès FHIR à la ressource Encounter : " + visit.getVisitNumber()
			);
		}
		
		return FhirEncounterMapper.toFhir(visit);
	}

	@Transactional(readOnly = true)
	public FhirBundleDto<FhirObservationDto> getObservations(UUID patientId) {
		// Enforce tenant/consent security by invoking the patient check
		PatientEntity patient = patientService.getPatientById(patientId);
		
		List<VitalsEntity> vitalsList = vitalsRepository.findAllByPatientId(patientId);
		List<com.joprelys.backend.lab.infrastructure.persistence.LabResultEntity> labResults = labResultRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
		
		List<FhirObservationDto> observations = vitalsList.stream()
				.flatMap(v -> FhirObservationMapper.toFhir(v).stream())
				.collect(Collectors.toCollection(java.util.ArrayList::new));

		for (com.joprelys.backend.lab.infrastructure.persistence.LabResultEntity lr : labResults) {
			observations.add(FhirObservationMapper.toFhir(lr));
		}
		
		var actor = getCurrentUser();
		if (actor != null) {
			auditService.logSuccess(
					actor.getId(),
					actor.getOrganizationId(),
					patient.getId(),
					"Observation",
					patient.getId(),
					"READ_FHIR_RESOURCE",
					"Accès FHIR à la liste des ressources Observation pour le patient : " + patient.getFullName()
			);
		}
		
		List<FhirBundleDto.BundleEntry<FhirObservationDto>> entries = observations.stream()
				.map(FhirBundleDto.BundleEntry::new)
				.collect(Collectors.toList());
				
		return new FhirBundleDto<>(
				"Bundle",
				"searchset",
				observations.size(),
				entries
		);
	}

	@Transactional(readOnly = true)
	public FhirBundleDto<FhirDiagnosticReportDto> getDiagnosticReports(UUID patientId) {
		PatientEntity patient = patientService.getPatientById(patientId);

		List<com.joprelys.backend.lab.infrastructure.persistence.LabResultEntity> labResults = labResultRepository.findByPatientIdOrderByCreatedAtDesc(patientId);

		java.util.Map<String, List<com.joprelys.backend.lab.infrastructure.persistence.LabResultEntity>> groupedResults = labResults.stream()
				.collect(Collectors.groupingBy(com.joprelys.backend.lab.infrastructure.persistence.LabResultEntity::getResultNumber));

		List<FhirDiagnosticReportDto> reports = groupedResults.values().stream()
				.map(FhirDiagnosticReportMapper::toFhir)
				.collect(Collectors.toList());

		var actor = getCurrentUser();
		if (actor != null) {
			auditService.logSuccess(
					actor.getId(),
					actor.getOrganizationId(),
					patient.getId(),
					"DiagnosticReport",
					patient.getId(),
					"READ_FHIR_RESOURCE",
					"Accès FHIR à la liste des ressources DiagnosticReport pour le patient : " + patient.getFullName()
			);
		}

		List<FhirBundleDto.BundleEntry<FhirDiagnosticReportDto>> entries = reports.stream()
				.map(FhirBundleDto.BundleEntry::new)
				.collect(Collectors.toList());

		return new FhirBundleDto<>(
				"Bundle",
				"searchset",
				reports.size(),
				entries
		);
	}

	private UserAccountEntity getCurrentUser() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
			return userAccountRepository.findByEmail(auth.getName().trim().toLowerCase()).orElse(null);
		}
		return null;
	}
}
