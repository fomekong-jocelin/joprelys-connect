package com.joprelys.backend.spatial.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class SpatialControllerAuthorizationTest {

    @Test
    void bedStatusEndpointShouldRequireDedicatedOperationalPermission() throws NoSuchMethodException {
        Method method = SpatialController.class.getMethod(
                "updateBedStatus",
                UUID.class,
                UpdateBedStatusRequest.class);

        PreAuthorize authorization = method.getAnnotation(PreAuthorize.class);

        assertNotNull(authorization);
        assertEquals(
                "hasAuthority('BED_OPERATIONAL_STATUS_MANAGE')",
                authorization.value());
    }

    @Test
    void transferEndpointShouldKeepHospitalizationPermissionInThisIncrement() throws NoSuchMethodException {
        Method method = SpatialController.class.getMethod(
                "transferPatient",
                TransferRequest.class);

        PreAuthorize authorization = method.getAnnotation(PreAuthorize.class);

        assertNotNull(authorization);
        assertEquals(
                "hasAuthority('HOSPITALIZATION_MANAGE')",
                authorization.value());
    }
}
