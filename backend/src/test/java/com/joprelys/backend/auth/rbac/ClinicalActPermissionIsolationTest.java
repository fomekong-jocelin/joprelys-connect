package com.joprelys.backend.auth.rbac;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.Test;
class ClinicalActPermissionIsolationTest {
    @Test void doctorsOwnMedicalSigningAndNursesOnlyClinicalDocumentation() {
        var doctor = RbacCatalog.permissionsForLegacyRoles(Set.of("MEDECIN"));
        var nurse = RbacCatalog.permissionsForLegacyRoles(Set.of("INFIRMIER"));
        assertTrue(doctor.containsAll(Set.of("PRESCRIPTION_WRITE", "PRESCRIPTION_SIGN", "CLINICAL_SIGN", "CONSULTATION_LOCK")));
        assertTrue(nurse.contains("CLINICAL_WRITE"));
        assertTrue(java.util.Collections.disjoint(nurse, Set.of("PRESCRIPTION_WRITE", "PRESCRIPTION_SIGN", "CLINICAL_SIGN", "CONSULTATION_LOCK", "PHARMACY_DISPENSE")));
    }
    @Test void administrativeRightsDoNotAutomaticallyBecomeMedicalSigningRights() {
        assertTrue(java.util.Collections.disjoint(RbacCatalog.permissionsForLegacyRoles(Set.of("ADMIN_CLINIQUE")),
                Set.of("PRESCRIPTION_WRITE", "PRESCRIPTION_SIGN", "CLINICAL_SIGN", "CONSULTATION_LOCK", "PHARMACY_VALIDATE", "PHARMACY_DISPENSE")));
    }
    @Test void pharmacistsCanValidateAndDispenseButCannotSignMedicalPrescriptions() {
        var pharmacist = RbacCatalog.permissionsForLegacyRoles(Set.of("PHARMACIEN"));
        assertTrue(pharmacist.containsAll(Set.of("PHARMACY_VALIDATE", "PHARMACY_DISPENSE")));
        assertFalse(pharmacist.contains("PRESCRIPTION_SIGN"));
    }
}
