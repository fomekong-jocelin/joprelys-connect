package com.joprelys.backend.spatial.domain;

import com.joprelys.backend.spatial.infrastructure.persistence.WardEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WardEntityTest {

    @Test
    void shouldAllowRoomsOnlyForHospitalizationAndEmergencyServices() {
        assertTrue(new WardEntity("Médecine", HospitalServiceType.HOSPITALIZATION).allowsRooms());
        assertTrue(new WardEntity("Urgences", HospitalServiceType.EMERGENCY).allowsRooms());

        assertFalse(new WardEntity("Consultations", HospitalServiceType.OUTPATIENT).allowsRooms());
        assertFalse(new WardEntity("Laboratoire", HospitalServiceType.MEDICO_TECHNICAL).allowsRooms());
        assertFalse(new WardEntity("Pharmacie", HospitalServiceType.PHARMACY).allowsRooms());
        assertFalse(new WardEntity("Caisse", HospitalServiceType.ADMINISTRATIVE).allowsRooms());
    }

    @Test
    void shouldRejectRoomsForAdministrativeService() {
        WardEntity cashDesk = new WardEntity("Caisse", HospitalServiceType.ADMINISTRATIVE);

        SpatialConfigurationRuleException exception = assertThrows(
                SpatialConfigurationRuleException.class,
                cashDesk::requireRoomsAllowed);

        assertEquals("Le type de ce service n'autorise pas la création de chambres.", exception.getMessage());
    }

    @Test
    void shouldRejectIncompatibleTypeChangeWhileRoomsExist() {
        WardEntity ward = new WardEntity("Médecine", HospitalServiceType.HOSPITALIZATION);

        assertThrows(
                SpatialConfigurationRuleException.class,
                () -> ward.changeServiceType(HospitalServiceType.ADMINISTRATIVE, true));
        assertEquals(HospitalServiceType.HOSPITALIZATION, ward.getServiceType());
    }

    @Test
    void shouldAllowTypeChangeAfterRoomsHaveBeenRemoved() {
        WardEntity ward = new WardEntity("Ancienne unité", HospitalServiceType.HOSPITALIZATION);

        assertDoesNotThrow(() -> ward.changeServiceType(HospitalServiceType.ADMINISTRATIVE, false));
        assertEquals(HospitalServiceType.ADMINISTRATIVE, ward.getServiceType());
    }
}
