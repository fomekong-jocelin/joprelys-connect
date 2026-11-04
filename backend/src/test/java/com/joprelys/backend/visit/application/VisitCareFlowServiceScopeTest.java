package com.joprelys.backend.visit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.common.api.ApiStatusException;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class VisitCareFlowServiceScopeTest {

    private static final UUID VISIT_ORG = UUID.randomUUID();
    private static final UUID OTHER_ORG = UUID.randomUUID();

    @Mock private VisitRepository visitRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private AuditService auditService;

    private VisitCareFlowService service;
    private VisitEntity visit;

    @BeforeEach
    void setUp() {
        ClinicalWriteScopePolicy policy =
                new VisitOrganizationClinicalWriteScopePolicy(visitRepository, userAccountRepository);
        service = new VisitCareFlowService(visitRepository, userAccountRepository, auditService, policy);
        PatientEntity patient = new PatientEntity("DPU-T", "PAT-T", "Patient Test", "FEMININ",
                LocalDate.of(1990, 1, 1), "+237690000000", "Douala", "", "", "", "", "", "");
        visit = new VisitEntity(patient, "VIS-T001", "Contrôle", "Médecine générale");
        visit.setOrganizationId(VISIT_ORG);
        visit.setStatus("EN_COURS");
    }

    @Test
    void practitionerFromAnotherOrganizationCannotStartTheConsultation() {
        when(visitRepository.findByIdForUpdate(any())).thenReturn(Optional.of(visit));
        when(userAccountRepository.findByEmail("medecin.b@joprelys.local"))
                .thenReturn(Optional.of(practitioner(OTHER_ORG)));

        assertOutOfScope(() -> service.startConsultation(UUID.randomUUID(), "medecin.b@joprelys.local", false));
        verify(visitRepository, never()).save(any());
    }

    @Test
    void practitionerFromAnotherOrganizationCannotTakeConsultationOwnership() {
        UUID practitionerId = UUID.randomUUID();
        when(userAccountRepository.findById(practitionerId)).thenReturn(Optional.of(practitioner(OTHER_ORG)));

        assertOutOfScope(() -> service.ensureConsultationOwnership(visit, practitionerId, "Dr. Beta"));
        assertThat(visit.getConsultingPractitionerId()).isNull();
    }

    @Test
    void practitionerOfTheVisitOrganizationTakesOwnership() {
        UUID practitionerId = UUID.randomUUID();
        when(userAccountRepository.findById(practitionerId)).thenReturn(Optional.of(practitioner(VISIT_ORG)));

        service.ensureConsultationOwnership(visit, practitionerId, "Dr. Alpha");

        assertThat(visit.getConsultingPractitionerId()).isEqualTo(practitionerId);
    }

    private static void assertOutOfScope(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiStatusException.class, exception -> {
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(exception.apiCode()).isEqualTo(ClinicalWriteScopePolicy.OUT_OF_SCOPE_CODE);
        });
    }

    private static UserAccountEntity practitioner(UUID organizationId) {
        UserAccountEntity practitioner = new UserAccountEntity(
                "medecin@joprelys.local", "Dr. Test", "MEDECIN", "hash");
        practitioner.setOrganizationId(organizationId);
        return practitioner;
    }
}
