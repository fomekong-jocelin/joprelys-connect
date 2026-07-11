package com.joprelys.backend.patient.reconciliation.application;

import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.reconciliation.domain.PatientAliasType;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientIdentityAliasEntity;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientIdentityAliasRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PatientAliasService {

    private final PatientIdentityAliasRepository aliasRepository;

    public PatientAliasService(PatientIdentityAliasRepository aliasRepository) {
        this.aliasRepository = aliasRepository;
    }

    public void pointOriginAliasesTo(
            PatientEntity originPatient,
            PatientEntity canonicalPatient,
            UUID actorId) {
        ensureAlias(
                originPatient,
                canonicalPatient,
                PatientAliasType.URG_TEMP,
                originPatient.getTemporaryPatientNumber(),
                actorId);
        ensureAlias(
                originPatient,
                canonicalPatient,
                PatientAliasType.LOCAL_PATIENT_NUMBER,
                originPatient.getLocalPatientNumber(),
                actorId);
        ensureAlias(
                originPatient,
                canonicalPatient,
                PatientAliasType.GLOBAL_PATIENT_NUMBER,
                originPatient.getGlobalPatientNumber(),
                actorId);
    }

    private void ensureAlias(
            PatientEntity originPatient,
            PatientEntity canonicalPatient,
            PatientAliasType aliasType,
            String aliasValue,
            UUID actorId) {
        if (aliasValue == null || aliasValue.isBlank()) {
            return;
        }

        String normalizedAlias = aliasValue.trim();
        var existing = aliasRepository.findByAliasTypeAndAliasValue(aliasType, normalizedAlias);
        if (existing.isPresent()) {
            if (!existing.get().getOriginPatient().getId().equals(originPatient.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "PATIENT_ALIAS_ALREADY_ASSIGNED");
            }
            existing.get().repointTo(canonicalPatient);
            aliasRepository.save(existing.get());
            return;
        }

        aliasRepository.save(new PatientIdentityAliasEntity(
                originPatient.getOrganizationId(),
                originPatient,
                canonicalPatient,
                aliasType,
                normalizedAlias,
                actorId));
    }
}
