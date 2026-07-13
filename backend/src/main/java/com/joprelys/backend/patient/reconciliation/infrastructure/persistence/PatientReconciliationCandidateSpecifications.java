package com.joprelys.backend.patient.reconciliation.infrastructure.persistence;

import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class PatientReconciliationCandidateSpecifications {

    private PatientReconciliationCandidateSpecifications() {
    }

    public static Specification<PatientEntity> eligibleForReconciliation(
            UUID sourcePatientId,
            PatientIdentityStatus identityStatus,
            LocalDate birthDate,
            String phone,
            String city,
            String gender) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> requiredPredicates = new ArrayList<>();
            requiredPredicates.add(criteriaBuilder.equal(
                    root.<PatientIdentityStatus>get("identityStatus"),
                    identityStatus));
            requiredPredicates.add(criteriaBuilder.equal(root.<String>get("status"), "ACTIVE"));
            requiredPredicates.add(criteriaBuilder.notEqual(root.<UUID>get("id"), sourcePatientId));

            List<Predicate> identitySignals = new ArrayList<>();
            if (birthDate != null) {
                identitySignals.add(criteriaBuilder.equal(root.<LocalDate>get("birthDate"), birthDate));
            }
            if (hasText(phone)) {
                identitySignals.add(criteriaBuilder.equal(root.<String>get("phone"), phone));
            }
            if (hasText(city)) {
                identitySignals.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.<String>get("city")),
                        city.toLowerCase(Locale.ROOT)));
            }
            if (hasText(gender)) {
                identitySignals.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.<String>get("gender")),
                        gender.toLowerCase(Locale.ROOT)));
            }

            if (!identitySignals.isEmpty()) {
                requiredPredicates.add(criteriaBuilder.or(identitySignals.toArray(Predicate[]::new)));
            }

            return criteriaBuilder.and(requiredPredicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
