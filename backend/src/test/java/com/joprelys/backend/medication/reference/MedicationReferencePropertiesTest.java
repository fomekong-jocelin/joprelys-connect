package com.joprelys.backend.medication.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class MedicationReferencePropertiesTest {

    @Test
    void shouldKeepNetworkReferenceDisabledByDefaultOutsideDeploymentProfile() {
        MedicationReferenceProperties properties = new MedicationReferenceProperties();

        assertFalse(properties.isEnabled());
        assertFalse(properties.getRxnorm().isEnabled());
        assertEquals("https://rxnav.nlm.nih.gov", properties.getRxnorm().getBaseUrl());
        assertEquals(1500, properties.getRxnorm().getConnectTimeoutMs());
        assertEquals(2500, properties.getRxnorm().getReadTimeoutMs());
    }
}
