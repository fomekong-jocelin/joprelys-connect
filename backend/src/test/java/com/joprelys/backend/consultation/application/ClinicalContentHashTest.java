package com.joprelys.backend.consultation.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

class ClinicalContentHashTest {
    @Test
    void fieldBoundariesAndMissingValuesCannotHideChangesInClinicalContent() {
        assertNotEquals(ClinicalContentHash.of("ab", "c"), ClinicalContentHash.of("a", "bc"));
        assertNotEquals(ClinicalContentHash.of((String) null), ClinicalContentHash.of(""));
        assertNotEquals(ClinicalContentHash.of("diagnostic", "500 mg"), ClinicalContentHash.of("diagnostic", "5 mg"));
        assertEquals(ClinicalContentHash.of("Fièvre", "évaluation"), ClinicalContentHash.of("Fièvre", "évaluation"));
    }
}
