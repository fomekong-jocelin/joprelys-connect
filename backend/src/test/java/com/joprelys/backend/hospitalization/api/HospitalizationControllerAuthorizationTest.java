package com.joprelys.backend.hospitalization.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

class HospitalizationControllerAuthorizationTest {

    @Test
    void admissionEndpointShouldRequireDedicatedAdmissionPermission() throws NoSuchMethodException {
        assertPermission(
                HospitalizationController.class.getMethod(
                        "admitPatient",
                        CreateHospitalizationRequest.class),
                "hasAuthority('HOSPITALIZATION_ADMIT')");
    }

    @Test
    void noteEndpointShouldRequireDedicatedWritePermission() throws NoSuchMethodException {
        assertPermission(
                HospitalizationController.class.getMethod(
                        "addNote",
                        UUID.class,
                        CreateHospitalizationNoteRequest.class),
                "hasAuthority('HOSPITALIZATION_NOTE_WRITE')");
    }

    @Test
    void consentEndpointShouldRequireDedicatedRecordPermission() throws NoSuchMethodException {
        assertPermission(
                HospitalizationController.class.getMethod(
                        "addConsent",
                        UUID.class,
                        String.class,
                        boolean.class,
                        String.class,
                        MultipartFile.class),
                "hasAuthority('HOSPITALIZATION_CONSENT_RECORD')");
    }

    @Test
    void careEndpointShouldRequireDedicatedRecordPermission() throws NoSuchMethodException {
        assertPermission(
                HospitalizationController.class.getMethod(
                        "addDailyCare",
                        UUID.class,
                        CreateDailyCareRequest.class),
                "hasAuthority('HOSPITALIZATION_CARE_RECORD')");
    }

    @Test
    void medicationEndpointShouldRequireAdministrationNotPrescriptionPermission() throws NoSuchMethodException {
        assertPermission(
                HospitalizationController.class.getMethod(
                        "addMedicationAdministration",
                        UUID.class,
                        CreateMedicationAdministrationRequest.class),
                "hasAuthority('HOSPITALIZATION_MEDICATION_ADMINISTER')");
    }

    @Test
    void consumableEndpointShouldRequireDedicatedRecordPermission() throws NoSuchMethodException {
        assertPermission(
                HospitalizationController.class.getMethod(
                        "addPatientConsumption",
                        UUID.class,
                        CreatePatientConsumptionRequest.class),
                "hasAuthority('HOSPITALIZATION_CONSUMABLE_RECORD')");
    }

    @Test
    void dischargeDecisionEndpointShouldRequireMedicalDecisionPermission() throws NoSuchMethodException {
        assertPermission(
                HospitalizationController.class.getMethod(
                        "decideDischarge",
                        UUID.class,
                        DischargeHospitalizationRequest.class),
                "hasAuthority('HOSPITALIZATION_DISCHARGE_DECIDE')");
    }

    @Test
    void physicalDepartureEndpointShouldRequireDedicatedConfirmationPermission() throws NoSuchMethodException {
        assertPermission(
                HospitalizationController.class.getMethod(
                        "confirmPhysicalDeparture",
                        UUID.class,
                        ConfirmPhysicalDepartureRequest.class),
                "hasAuthority('HOSPITALIZATION_PHYSICAL_DEPARTURE_CONFIRM')");
    }

    @Test
    void noHospitalizationWriteEndpointShouldConsumeLegacyManagePermission() {
        List<Method> writeEndpoints = List.of(HospitalizationController.class.getDeclaredMethods()).stream()
                .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
                .filter(method -> method.getAnnotation(PreAuthorize.class).value().contains("HOSPITALIZATION"))
                .toList();

        assertFalse(writeEndpoints.isEmpty());
        assertFalse(writeEndpoints.stream()
                .map(method -> method.getAnnotation(PreAuthorize.class).value())
                .anyMatch(expression -> expression.contains("HOSPITALIZATION_MANAGE")));
    }

    private void assertPermission(Method method, String expectedExpression) {
        PreAuthorize authorization = method.getAnnotation(PreAuthorize.class);
        assertNotNull(authorization);
        assertEquals(expectedExpression, authorization.value());
    }
}
