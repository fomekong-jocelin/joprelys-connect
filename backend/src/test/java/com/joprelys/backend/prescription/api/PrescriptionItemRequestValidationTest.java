package com.joprelys.backend.prescription.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class PrescriptionItemRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldAllowAnExplicitlyEmptyDosageForDraftPersistence() {
        PrescriptionItemRequest request = new PrescriptionItemRequest(
                "Inhalateur", "", null, null, null, null, null, null, null, true);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldStillRejectAMissingDosageField() {
        PrescriptionItemRequest request = new PrescriptionItemRequest(
                "Inhalateur", null, null, null, null, null, null, null, null, true);

        assertFalse(validator.validate(request).isEmpty());
    }
}
