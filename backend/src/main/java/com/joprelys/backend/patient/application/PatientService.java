package com.joprelys.backend.patient.application;

import com.joprelys.backend.patient.api.CreatePatientRequest;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {

	private final PatientRepository patientRepository;
	private final PatientNumberGenerator patientNumberGenerator;

	public PatientService(PatientRepository patientRepository, PatientNumberGenerator patientNumberGenerator) {
		this.patientRepository = patientRepository;
		this.patientNumberGenerator = patientNumberGenerator;
	}

	@Transactional
	public PatientEntity createPatient(CreatePatientRequest request) {
		PatientNumberGenerator.GeneratedNumbers numbers = patientNumberGenerator.generateNextNumbers();

		var patient = new PatientEntity(
				numbers.globalNumber(),
				numbers.localNumber(),
				request.fullName(),
				request.gender(),
				request.birthDate(),
				request.phone(),
				request.city(),
				request.district(),
				request.address(),
				request.emergencyContactName(),
				request.emergencyContactPhone(),
				request.allergies(),
				request.medicalHistory()
		);

		return patientRepository.save(patient);
	}

	@Transactional(readOnly = true)
	public List<PatientEntity> searchPatients(String query) {
		if (query == null || query.isBlank()) {
			return patientRepository.findAll();
		}
		return patientRepository.searchPatients(query.trim());
	}

	@Transactional(readOnly = true)
	public PatientEntity getPatientById(UUID id) {
		return patientRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient non trouvé"));
	}
}
