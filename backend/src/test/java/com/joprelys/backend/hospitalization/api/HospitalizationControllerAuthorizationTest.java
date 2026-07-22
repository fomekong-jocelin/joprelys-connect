package com.joprelys.backend.hospitalization.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class HospitalizationControllerAuthorizationTest {

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

    private void assertPermission(Method method, String expectedExpression) {
        PreAuthorize authorization = method.getAnnotation(PreAuthorize.class);
        assertNotNull(authorization);
        assertEquals(expectedExpression, authorization.value());
    }
}
