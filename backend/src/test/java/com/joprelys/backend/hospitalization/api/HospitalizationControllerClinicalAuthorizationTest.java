package com.joprelys.backend.hospitalization.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

class HospitalizationControllerClinicalAuthorizationTest {

    @Test
    void admissionShouldRequireDedicatedPermission() throws NoSuchMethodException {
        assertPermission(
                HospitalizationController.class.getMethod("admitPatient", CreateHospitalizationRequest.class),
                "hasAuthority('HOSPITALIZATION_ADMIT')");
    }

    @Test
    void noteShouldRequireDedicatedPermission() throws NoSuchMethodException {
        assertPermission(
                HospitalizationController.class.getMethod(
                        "addNote", UUID.class, CreateHospitalizationNoteRequest.class),
                "hasAuthority('HOSPITALIZATION_NOTE_WRITE')");
    }

    @Test
    void consentShouldRequireDedicatedPermission() throws NoSuchMethodException {
        assertPermission(
                HospitalizationController.class.getMethod(
                        "addConsent", UUID.class, String.class, boolean.class, String.class, MultipartFile.class),
                "hasAuthority('HOSPITALIZATION_CONSENT_MANAGE')");
    }

    @Test
    void dailyCareShouldRequireDedicatedPermission() throws NoSuchMethodException {
        assertPermission(
                HospitalizationController.class.getMethod(
                        "addDailyCare", UUID.class, CreateDailyCareRequest.class),
                "hasAuthority('HOSPITALIZATION_CARE_WRITE')");
    }

    @Test
    void medicationAdministrationShouldNotUsePrescriptionOrLegacyPermission() throws NoSuchMethodException {
        assertPermission(
                HospitalizationController.class.getMethod(
                        "addMedicationAdministration", UUID.class, CreateMedicationAdministrationRequest.class),
                "hasAuthority('HOSPITALIZATION_MEDICATION_ADMINISTER')");
    }

    @Test
    void consumableUsageShouldRequireDedicatedPermission() throws NoSuchMethodException {
        assertPermission(
                HospitalizationController.class.getMethod(
                        "addPatientConsumption", UUID.class, CreatePatientConsumptionRequest.class),
                "hasAuthority('HOSPITALIZATION_CONSUMABLE_MANAGE')");
    }

    private static void assertPermission(Method method, String expectedExpression) {
        PreAuthorize authorization = method.getAnnotation(PreAuthorize.class);
        assertNotNull(authorization);
        assertEquals(expectedExpression, authorization.value());
    }
}
