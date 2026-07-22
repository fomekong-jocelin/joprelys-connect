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
        assertPermission(
                SpatialController.class.getMethod(
                        "updateBedStatus",
                        UUID.class,
                        UpdateBedStatusRequest.class),
                "hasAuthority('BED_OPERATIONAL_STATUS_MANAGE')");
    }

    @Test
    void bedCapacityEndpointShouldRequireDedicatedOperationalPermission() throws NoSuchMethodException {
        assertPermission(
                SpatialController.class.getMethod(
                        "updateBedCapacityStatus",
                        UUID.class,
                        UpdateBedCapacityStatusRequest.class),
                "hasAuthority('BED_OPERATIONAL_STATUS_MANAGE')");
    }

    @Test
    void transferEndpointShouldKeepHospitalizationPermissionInThisIncrement() throws NoSuchMethodException {
        assertPermission(
                SpatialController.class.getMethod(
                        "transferPatient",
                        TransferRequest.class),
                "hasAuthority('HOSPITALIZATION_MANAGE')");
    }

    private void assertPermission(Method method, String expectedExpression) {
        PreAuthorize authorization = method.getAnnotation(PreAuthorize.class);
        assertNotNull(authorization);
        assertEquals(expectedExpression, authorization.value());
    }
}
