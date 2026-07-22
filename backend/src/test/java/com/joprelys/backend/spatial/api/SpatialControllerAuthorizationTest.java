package com.joprelys.backend.spatial.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class SpatialControllerAuthorizationTest {

    @Test
    void legacyBedStatusEndpointShouldRemainSupervisorOnly() throws NoSuchMethodException {
        assertPermission(
                SpatialController.class.getMethod(
                        "updateBedStatus",
                        UUID.class,
                        UpdateBedStatusRequest.class),
                "hasAuthority('BED_OPERATIONAL_STATUS_MANAGE')");
    }

    @Test
    void cleaningEndpointShouldRequireCleaningPermission() throws NoSuchMethodException {
        assertPermission(
                SpatialController.class.getMethod(
                        "updateBedCleaningStatus",
                        UUID.class,
                        UpdateBedCleaningStatusRequest.class),
                "hasAuthority('BED_CLEANING_MANAGE')");
    }

    @Test
    void maintenanceEndpointShouldRequireMaintenancePermission() throws NoSuchMethodException {
        assertPermission(
                SpatialController.class.getMethod(
                        "updateBedMaintenanceStatus",
                        UUID.class,
                        UpdateBedMaintenanceStatusRequest.class),
                "hasAuthority('BED_MAINTENANCE_MANAGE')");
    }

    @Test
    void bedCapacityEndpointShouldRemainSupervisorOnly() throws NoSuchMethodException {
        assertPermission(
                SpatialController.class.getMethod(
                        "updateBedCapacityStatus",
                        UUID.class,
                        UpdateBedCapacityStatusRequest.class),
                "hasAuthority('BED_OPERATIONAL_STATUS_MANAGE')");
    }

    @Test
    void transferEndpointShouldRequireDedicatedTransferPermission() throws NoSuchMethodException {
        assertPermission(
                SpatialController.class.getMethod(
                        "transferPatient",
                        TransferRequest.class),
                "hasAuthority('HOSPITALIZATION_TRANSFER')");
    }

    private void assertPermission(Method method, String expectedExpression) {
        PreAuthorize authorization = method.getAnnotation(PreAuthorize.class);
        assertNotNull(authorization);
        assertEquals(expectedExpression, authorization.value());
    }
}
