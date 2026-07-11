package com.joprelys.backend.patient.reconciliation.application;

import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.patient.reconciliation.domain.PatientAliasType;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientCanonicalLinkEntity;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientCanonicalLinkRepository;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientIdentityAliasRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PatientCanonicalResolver {

    private final PatientRepository patientRepository;
    private final PatientCanonicalLinkRepository canonicalLinkRepository;
    private final PatientIdentityAliasRepository aliasRepository;

    public PatientCanonicalResolver(
            PatientRepository patientRepository,
            PatientCanonicalLinkRepository canonicalLinkRepository,
            PatientIdentityAliasRepository aliasRepository) {
        this.patientRepository = patientRepository;
        this.canonicalLinkRepository = canonicalLinkRepository;
        this.aliasRepository = aliasRepository;
    }

    @Transactional(readOnly = true)
    public CanonicalPatientContext resolve(UUID patientId) {
        PatientEntity requestedPatient = patientRepository.findById(patientId)
                .orElseThrow(() -> notFound("PATIENT_NOT_FOUND"));

        PatientCanonicalLinkEntity link = canonicalLinkRepository.findBySourcePatient_Id(patientId).orElse(null);
        PatientEntity canonicalPatient = link == null ? requestedPatient : link.getCanonicalPatient();

        LinkedHashSet<UUID> contributingPatientIds = new LinkedHashSet<>();
        contributingPatientIds.add(canonicalPatient.getId());
        canonicalLinkRepository.findAllByCanonicalPatient_Id(canonicalPatient.getId()).stream()
                .map(PatientCanonicalLinkEntity::getSourcePatient)
                .map(PatientEntity::getId)
                .forEach(contributingPatientIds::add);

        return new CanonicalPatientContext(
                requestedPatient,
                canonicalPatient,
                Set.copyOf(contributingPatientIds),
                link != null);
    }

    @Transactional(readOnly = true)
    public CanonicalPatientContext resolveAlias(PatientAliasType aliasType, String aliasValue) {
        var alias = aliasRepository.findByAliasTypeAndAliasValue(aliasType, normalizeAlias(aliasValue))
                .orElseThrow(() -> notFound("PATIENT_ALIAS_NOT_FOUND"));
        return resolve(alias.getCanonicalPatient().getId());
    }

    private static String normalizeAlias(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PATIENT_ALIAS_REQUIRED");
        }
        return value.trim();
    }

    private static ResponseStatusException notFound(String code) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, code);
    }

    public record CanonicalPatientContext(
            PatientEntity requestedPatient,
            PatientEntity canonicalPatient,
            Set<UUID> contributingPatientIds,
            boolean requestedPatientIsAlias) {
    }
}
