package com.joprelys.backend.billing.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.billing.api.*;
import com.joprelys.backend.billing.infrastructure.persistence.*;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

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

    public InvoiceCrudService(InvoiceRepository invoiceRepository,
                              InsuranceConventionRepository insuranceConventionRepository,
                              PatientRepository patientRepository,
                              VisitRepository visitRepository,
                              UserAccountRepository userAccountRepository,
                              AuditService auditService,
                              InvoicePrecalculationService precalculationService) {
        this.invoiceRepository = invoiceRepository;
        this.insuranceConventionRepository = insuranceConventionRepository;
        this.patientRepository = patientRepository;
        this.visitRepository = visitRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
        this.precalculationService = precalculationService;
    }

    private UserAccountEntity getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userAccountRepository.findByEmail(email).orElse(null);
    }

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        UserAccountEntity actor = getCurrentUser();
        UUID orgId = actor != null ? actor.getOrganizationId() : null;

        patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable"));

        if (request.visitId() != null) {
            visitRepository.findById(request.visitId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable"));
            invoiceRepository.findByVisitId(request.visitId()).ifPresent(inv -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Une facture existe déjà pour cette visite.");
            });
        }

        InsuranceConventionEntity convention = null;
        if (request.insuranceConventionId() != null) {
            convention = insuranceConventionRepository.findById(request.insuranceConventionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Convention d'assurance introuvable"));
        }

        Long seqVal = invoiceRepository.getNextInvoiceNumberSequenceValue();
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String invoiceNumber = String.format("FAC-%s-%06d", dateStr, seqVal);

        InvoiceEntity invoice = new InvoiceEntity(request.patientId(), request.visitId(), invoiceNumber, convention);
        invoice.setOrganizationId(orgId);

        if (request.items() != null && !request.items().isEmpty()) {
            for (InvoiceItemDto itemDto : request.items()) {
                InvoiceItemEntity item = new InvoiceItemEntity(
                        itemDto.label(),
                        itemDto.itemType(),
                        itemDto.unitPrice(),
                        itemDto.quantity(),
                        itemDto.coefficient()
                );
                item.setOrganizationId(orgId);
                invoice.addItem(item);
            }
        } else {
            // Auto-précalcul si aucun item fourni
            InvoiceResponse precalc = precalculationService.precalculate(
                    request.patientId(), request.visitId(), request.insuranceConventionId());
            for (InvoiceItemResponse itemResp : precalc.items()) {
                InvoiceItemEntity item = new InvoiceItemEntity(
                        itemResp.label(),
                        itemResp.itemType(),
                        itemResp.unitPrice(),
                        itemResp.quantity(),
                        itemResp.coefficient()
                );
                item.setOrganizationId(orgId);
                invoice.addItem(item);
            }
        }

        InvoiceEntity saved = invoiceRepository.save(invoice);

        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(), orgId, request.patientId(),
                    "BILLING", saved.getId(), "CREATE_INVOICE",
                    "Création de la facture N° " + saved.getInvoiceNumber()
                            + " pour un montant total de " + saved.getTotalAmount() + " FCFA."
            );
        }

        return InvoiceResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listInvoices(UUID patientId) {
        return invoiceRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(InvoiceResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID id) {
        InvoiceEntity entity = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable"));
        return InvoiceResponse.fromEntity(entity);
    }
}
