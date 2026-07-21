package com.joprelys.backend.billing.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.billing.api.CreateInvoiceRequest;
import com.joprelys.backend.billing.api.InvoiceItemDto;
import com.joprelys.backend.billing.domain.InvoiceRegularizationStatus;
import com.joprelys.backend.billing.infrastructure.persistence.InsuranceConventionRepository;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceItemType;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceRepository;
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.patient.reconciliation.application.PatientCanonicalResolver;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class InvoiceCrudServiceContinuityTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InsuranceConventionRepository insuranceConventionRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private VisitRepository visitRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private InvoicePrecalculationService precalculationService;
    @Mock
    private PatientCanonicalResolver canonicalResolver;

    @InjectMocks
    private InvoiceCrudService service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAggregateInvoicesAcrossCanonicalContributorsWithoutMovingThem() {
        UUID sourceId = UUID.randomUUID();
        UUID canonicalId = UUID.randomUUID();
        PatientEntity canonical = org.mockito.Mockito.mock(PatientEntity.class);
        InvoiceEntity sourceInvoice = new InvoiceEntity(sourceId, null, "FAC-SOURCE", null);
        sourceInvoice.setRegularizationStatus(InvoiceRegularizationStatus.REGULARIZATION_PENDING);
        InvoiceEntity canonicalInvoice = new InvoiceEntity(canonicalId, null, "FAC-CANONICAL", null);

        when(canonicalResolver.resolve(canonicalId)).thenReturn(new PatientCanonicalResolver.CanonicalPatientContext(
                canonical,
                canonical,
                Set.of(sourceId, canonicalId),
                false));
        when(invoiceRepository.findByPatientIdsWithDetails(Set.of(sourceId, canonicalId)))
                .thenReturn(List.of(canonicalInvoice, sourceInvoice));

        var result = service.listInvoices(canonicalId);

        assertEquals(2, result.size());
        assertEquals(canonicalId, result.get(0).patientId());
        assertEquals(sourceId, result.get(1).patientId());
        assertEquals(InvoiceRegularizationStatus.REGULARIZATION_PENDING, result.get(1).regularizationStatus());
    }

    @Test
    void shouldMarkAnInvoicePendingWhenThePatientIdentityIsProvisional() {
        UUID patientId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        PatientEntity patient = org.mockito.Mockito.mock(PatientEntity.class);
        UserAccountEntity actor = org.mockito.Mockito.mock(UserAccountEntity.class);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("billing@joprelys.com", "secret"));

        when(userAccountRepository.findByEmail("billing@joprelys.com")).thenReturn(Optional.of(actor));
        when(actor.getId()).thenReturn(UUID.randomUUID());
        when(actor.getOrganizationId()).thenReturn(organizationId);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patient.getId()).thenReturn(patientId);
        when(patient.getIdentityStatus()).thenReturn(PatientIdentityStatus.PROVISIONAL_URGENCY);
        when(canonicalResolver.resolve(patientId)).thenReturn(new PatientCanonicalResolver.CanonicalPatientContext(
                patient,
                patient,
                Set.of(patientId),
                false));
        when(invoiceRepository.getNextInvoiceNumberSequenceValue()).thenReturn(12L);
        when(invoiceRepository.save(any(InvoiceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateInvoiceRequest request = new CreateInvoiceRequest(
                patientId,
                null,
                null,
                List.of(new InvoiceItemDto(
                        "Prise en charge urgence",
                        InvoiceItemType.CONSULTATION,
                        BigDecimal.valueOf(10_000),
                        BigDecimal.ONE,
                        BigDecimal.ONE)));

        var response = service.createInvoice(request);

        assertEquals(InvoiceRegularizationStatus.REGULARIZATION_PENDING, response.regularizationStatus());
        assertEquals(patientId, response.patientId());
        verify(invoiceRepository).save(any(InvoiceEntity.class));
    }
}
