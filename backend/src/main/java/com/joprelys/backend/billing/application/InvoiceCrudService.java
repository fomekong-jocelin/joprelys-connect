package com.joprelys.backend.billing.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.billing.api.CreateInvoiceRequest;
import com.joprelys.backend.billing.api.InsuranceConventionDto;
import com.joprelys.backend.billing.api.InvoiceItemDto;
import com.joprelys.backend.billing.api.InvoiceItemResponse;
import com.joprelys.backend.billing.api.InvoiceResponse;
import com.joprelys.backend.billing.domain.InvoiceRegularizationStatus;
import com.joprelys.backend.billing.infrastructure.persistence.InsuranceConventionEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InsuranceConventionRepository;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceItemEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceRepository;
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.patient.reconciliation.application.PatientCanonicalResolver;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Création, lecture et liste des factures.
 */
@Service
public class InvoiceCrudService {

    private final InvoiceRepository invoiceRepository;
    private final InsuranceConventionRepository insuranceConventionRepository;
    private final PatientRepository patientRepository;
    private final VisitRepository visitRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;
    private final InvoicePrecalculationService precalculationService;
    private final PatientCanonicalResolver canonicalResolver;

    public InvoiceCrudService(
            InvoiceRepository invoiceRepository,
            InsuranceConventionRepository insuranceConventionRepository,
            PatientRepository patientRepository,
            VisitRepository visitRepository,
            UserAccountRepository userAccountRepository,
            AuditService auditService,
            InvoicePrecalculationService precalculationService,
            PatientCanonicalResolver canonicalResolver) {
        this.invoiceRepository = invoiceRepository;
        this.insuranceConventionRepository = insuranceConventionRepository;
        this.patientRepository = patientRepository;
        this.visitRepository = visitRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
        this.precalculationService = precalculationService;
        this.canonicalResolver = canonicalResolver;
    }

    private UserAccountEntity getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userAccountRepository.findByEmail(email).orElse(null);
    }

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        UserAccountEntity actor = getCurrentUser();
        UUID orgId = actor != null ? actor.getOrganizationId() : null;

        PatientEntity requestedPatient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable"));
        var patientContext = canonicalResolver.resolve(requestedPatient.getId());
        PatientEntity billingPatient = resolveBillingPatient(request, requestedPatient, patientContext);

        if (request.visitId() != null) {
            invoiceRepository.findByVisitId(request.visitId()).ifPresent(inv -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Une facture existe déjà pour cette visite.");
            });
        }

        InsuranceConventionEntity convention = null;
        if (request.insuranceConventionId() != null) {
            convention = insuranceConventionRepository.findById(request.insuranceConventionId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Convention d'assurance introuvable"));
        }

        Long seqVal = invoiceRepository.getNextInvoiceNumberSequenceValue();
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String invoiceNumber = String.format("FAC-%s-%06d", dateStr, seqVal);

        InvoiceEntity invoice = new InvoiceEntity(
                billingPatient.getId(),
                request.visitId(),
                invoiceNumber,
                convention);
        invoice.setOrganizationId(orgId);
        invoice.setRegularizationStatus(resolveRegularizationStatus(billingPatient));

        if (request.items() != null && !request.items().isEmpty()) {
            for (InvoiceItemDto itemDto : request.items()) {
                InvoiceItemEntity item = new InvoiceItemEntity(
                        itemDto.label(),
                        itemDto.itemType(),
                        itemDto.unitPrice(),
                        itemDto.quantity(),
                        itemDto.coefficient());
                item.setOrganizationId(orgId);
                invoice.addItem(item);
            }
        } else {
            InvoiceResponse precalc = precalculationService.precalculate(
                    billingPatient.getId(),
                    request.visitId(),
                    request.insuranceConventionId());
            for (InvoiceItemResponse itemResp : precalc.items()) {
                InvoiceItemEntity item = new InvoiceItemEntity(
                        itemResp.label(),
                        itemResp.itemType(),
                        itemResp.unitPrice(),
                        itemResp.quantity(),
                        itemResp.coefficient());
                item.setOrganizationId(orgId);
                invoice.addItem(item);
            }
        }

        InvoiceEntity saved = invoiceRepository.save(invoice);

        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(), orgId, billingPatient.getId(),
                    "BILLING", saved.getId(), "CREATE_INVOICE",
                    "Création de la facture N° " + saved.getInvoiceNumber()
                            + " pour un montant total de " + saved.getTotalAmount() + " FCFA."
                            + " Régularisation : " + saved.getRegularizationStatus());
        }

        return InvoiceResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listInvoices(UUID patientId) {
        var context = canonicalResolver.resolve(patientId);
        return invoiceRepository.findByPatientIdsWithDetails(context.contributingPatientIds()).stream()
                .map(InvoiceResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID id) {
        InvoiceEntity entity = invoiceRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable"));
        return InvoiceResponse.fromEntity(entity);
    }

    private PatientEntity resolveBillingPatient(
            CreateInvoiceRequest request,
            PatientEntity requestedPatient,
            PatientCanonicalResolver.CanonicalPatientContext context) {
        if (request.visitId() == null) {
            return requestedPatient.getIdentityStatus() == PatientIdentityStatus.MERGED
                    ? context.canonicalPatient()
                    : requestedPatient;
        }

        VisitEntity visit = visitRepository.findById(request.visitId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable"));
        UUID visitPatientId = visit.getPatient().getId();
        if (!context.contributingPatientIds().contains(visitPatientId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La visite ne correspond pas au dossier patient ou à l'une de ses identités sources.");
        }
        return visit.getPatient();
    }

    private InvoiceRegularizationStatus resolveRegularizationStatus(PatientEntity patient) {
        return patient.getIdentityStatus() == PatientIdentityStatus.PROVISIONAL_URGENCY
                || patient.getIdentityStatus() == PatientIdentityStatus.DECLARED
                ? InvoiceRegularizationStatus.REGULARIZATION_PENDING
                : InvoiceRegularizationStatus.RESOLVED;
    }
}
