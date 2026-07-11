package com.joprelys.backend.patient.reconciliation.application;

import com.joprelys.backend.patient.application.PatientSimilarityService;
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PatientReconciliationCandidateService {

    private static final double MINIMUM_PRESENTATION_SCORE = 30.0;
    private static final int MAX_CANDIDATES = 10;

    private final PatientRepository patientRepository;
    private final PatientSimilarityService similarityService;

    public PatientReconciliationCandidateService(
            PatientRepository patientRepository,
            PatientSimilarityService similarityService) {
        this.patientRepository = patientRepository;
        this.similarityService = similarityService;
    }

    @Transactional(readOnly = true)
    public List<PatientReconciliationCandidate> findCandidates(UUID sourcePatientId) {
        PatientEntity source = requirePatient(sourcePatientId);

        return patientRepository.findAllByIdentityStatus(PatientIdentityStatus.VERIFIED).stream()
                .filter(candidate -> !candidate.getId().equals(source.getId()))
                .filter(candidate -> "ACTIVE".equals(candidate.getStatus()))
                .map(candidate -> score(source, candidate))
                .filter(candidate -> candidate.score().doubleValue() >= MINIMUM_PRESENTATION_SCORE)
                .sorted(Comparator.comparing(PatientReconciliationCandidate::score).reversed())
                .limit(MAX_CANDIDATES)
                .toList();
    }

    @Transactional(readOnly = true)
    public PatientReconciliationCandidate scoreCandidate(UUID sourcePatientId, UUID candidatePatientId) {
        PatientEntity source = requirePatient(sourcePatientId);
        PatientEntity candidate = requirePatient(candidatePatientId);
        if (source.getId().equals(candidate.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PATIENT_RECONCILIATION_SELF_LINK_FORBIDDEN");
        }
        if (candidate.getIdentityStatus() != PatientIdentityStatus.VERIFIED
                || !"ACTIVE".equals(candidate.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PATIENT_RECONCILIATION_TARGET_NOT_ELIGIBLE");
        }
        return score(source, candidate);
    }

    private PatientEntity requirePatient(UUID patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND"));
    }

    private PatientReconciliationCandidate score(PatientEntity source, PatientEntity candidate) {
        double total = 0.0;
        List<String> reasons = new ArrayList<>();

        if (hasText(source.getFullName()) && hasText(candidate.getFullName())) {
            double nameSimilarity = similarityService.getSimilarityScore(
                    source.getFullName(), candidate.getFullName());
            total += nameSimilarity * 0.35;
            if (nameSimilarity >= 85.0) {
                reasons.add("NAME_STRONG_MATCH");
            } else if (nameSimilarity >= 65.0) {
                reasons.add("NAME_PARTIAL_MATCH");
            }
        }

        if (source.getBirthDate() != null && source.getBirthDate().equals(candidate.getBirthDate())) {
            total += 35.0;
            reasons.add("BIRTH_DATE_EXACT_MATCH");
        }

        if (sameNormalized(source.getGender(), candidate.getGender())) {
            total += 10.0;
            reasons.add("GENDER_MATCH");
        }

        if (samePhone(source.getPhone(), candidate.getPhone())) {
            total += 15.0;
            reasons.add("PHONE_EXACT_MATCH");
        }

        if (sameNormalized(source.getCity(), candidate.getCity())) {
            total += 5.0;
            reasons.add("CITY_MATCH");
        }

        return new PatientReconciliationCandidate(
                candidate.getId(),
                candidate.getGlobalPatientNumber(),
                candidate.getLocalPatientNumber(),
                candidate.getDisplayName(),
                candidate.getGender(),
                candidate.getBirthDate(),
                candidate.getPhone(),
                candidate.getCity(),
                BigDecimal.valueOf(Math.min(total, 100.0)).setScale(2, RoundingMode.HALF_UP),
                List.copyOf(reasons));
    }

    private static boolean sameNormalized(String left, String right) {
        return hasText(left)
                && hasText(right)
                && left.trim().toLowerCase(Locale.ROOT).equals(right.trim().toLowerCase(Locale.ROOT));
    }

    private static boolean samePhone(String left, String right) {
        String normalizedLeft = normalizePhone(left);
        String normalizedRight = normalizePhone(right);
        return !normalizedLeft.isEmpty() && normalizedLeft.equals(normalizedRight);
    }

    private static String normalizePhone(String value) {
        return value == null ? "" : value.replaceAll("[^0-9+]", "");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record PatientReconciliationCandidate(
            UUID patientId,
            String globalPatientNumber,
            String localPatientNumber,
            String displayName,
            String gender,
            java.time.LocalDate birthDate,
            String phone,
            String city,
            BigDecimal score,
            List<String> reasons) {
    }
}
