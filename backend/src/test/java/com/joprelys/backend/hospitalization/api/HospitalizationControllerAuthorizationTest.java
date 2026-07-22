package com.joprelys.backend.hospitalization.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class HospitalizationControllerAuthorizationTest {

    @Test
    void dischargeEndpointShouldRequireDedicatedPermission() throws NoSuchMethodException {
        Method method = HospitalizationController.class.getMethod(
                "dischargePatient",
                UUID.class,
                DischargeHospitalizationRequest.class);

        PreAuthorize authorization = method.getAnnotation(PreAuthorize.class);

        assertNotNull(authorization);
        assertEquals(
                "hasAuthority('HOSPITALIZATION_DISCHARGE_DECIDE')",
                authorization.value());
    }
}
