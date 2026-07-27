package com.joprelys.backend.ai.facts.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanCandidate;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanView;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactView;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.FactPayload;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Authority;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactStatus;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactType;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Laterality;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Polarity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClinicalFactRevisionSemanticGuardTest {

    @Test
    void shouldTreatSameClinicalFactAndEvidenceAsKeepEvenIfEvidenceOrderDiffers() {
        UUID firstItem = UUID.randomUUID();
        UUID secondItem = UUID.randomUUID();
        FactView current = fact(firstItem, secondItem);
        FactPayload proposed = payload(
                List.of(
                        evidence(secondItem, "depuis trois jours", false),
                        evidence(firstItem, "douleur abdominale", true)),
                "douleur abdominale");

        assertThat(ClinicalFactRevisionSemanticGuard.sameFact(current, proposed)).isTrue();
    }

    @Test
    void shouldDetectARealClinicalChange() {
        UUID firstItem = UUID.randomUUID();
        UUID secondItem = UUID.randomUUID();
        FactView current = fact(firstItem, secondItem);
        FactPayload changed = payload(
                List.of(
                        evidence(firstItem, "douleur abdominale", true),
                        evidence(secondItem, "depuis quatre jours", false)),
                "douleur abdominale");

        assertThat(ClinicalFactRevisionSemanticGuard.sameFact(current, changed)).isFalse();
    }

    private FactView fact(UUID firstItem, UUID secondItem) {
        return new FactView(
                UUID.randomUUID(),
                4,
                "existing-fact",
                FactType.SYMPTOM.name(),
                Authority.PATIENT_REPORTED.name(),
                "ABDOMINAL_PAIN",
                "douleur abdominale",
                Polarity.POSITIVE.name(),
                null,
                null,
                null,
                "depuis trois jours",
                Laterality.UNSPECIFIED.name(),
                null,
                null,
                FactStatus.ASSERTED.name(),
                null,
                Instant.now(),
                List.of(
                        evidenceView(firstItem, "douleur abdominale", true),
                        evidenceView(secondItem, "depuis trois jours", false)));
    }

    private FactPayload payload(List<EvidenceSpanCandidate> evidence, String concept) {
        return new FactPayload(
                FactType.SYMPTOM,
                Authority.PATIENT_REPORTED,
                "ABDOMINAL_PAIN",
                concept,
                Polarity.POSITIVE,
                null,
                null,
                null,
                "depuis trois jours",
                Laterality.UNSPECIFIED,
                null,
                null,
                evidence);
    }

    private EvidenceSpanCandidate evidence(UUID itemId, String quote, boolean primary) {
        return new EvidenceSpanCandidate(itemId, 0, quote.length(), quote, primary);
    }

    private EvidenceSpanView evidenceView(UUID itemId, String quote, boolean primary) {
        return new EvidenceSpanView(UUID.randomUUID(), itemId, 0, quote.length(), quote, primary);
    }
}
