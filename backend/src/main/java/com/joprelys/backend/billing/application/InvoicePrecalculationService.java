package com.joprelys.backend.billing.application;

import com.joprelys.backend.billing.api.InvoiceResponse;
import com.joprelys.backend.billing.infrastructure.persistence.InsuranceConventionEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InsuranceConventionRepository;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Calcul préventif d'une facture à partir des sources cliniques d'une visite (ADR-20261017 D-11 à D-15).
 * N'écrit rien en base — lecture seule.
 */
@Service
public class InvoicePrecalculationService {

    private final InsuranceConventionRepository insuranceConventionRepository;
    private final VisitRepository visitRepository;
    private final HospitalizationRepository hospitalizationRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceChargeCollector chargeCollector;

    public InvoicePrecalculationService(
            InsuranceConventionRepository insuranceConventionRepository,
            VisitRepository visitRepository,
            HospitalizationRepository hospitalizationRepository,
            InvoiceRepository invoiceRepository,
            InvoiceChargeCollector chargeCollector) {
        this.insuranceConventionRepository = insuranceConventionRepository;
        this.visitRepository = visitRepository;
        this.hospitalizationRepository = hospitalizationRepository;
        this.invoiceRepository = invoiceRepository;
        this.chargeCollector = chargeCollector;
    }

    @Transactional(readOnly = true)
    public InvoiceResponse precalculate(UUID patientId, UUID visitId, UUID insuranceConventionId) {
        return InvoiceResponse.fromEntity(draft(patientId, visitId, insuranceConventionId, null));
    }

    /**
     * Facture non persistée contenant les charges encore non facturées de la visite.
     * {@code excludedInvoiceId} permet de recalculer une facture en attente sans compter ses propres lignes.
     */
    @Transactional(readOnly = true)
    public InvoiceEntity draft(UUID patientId, UUID visitId, UUID insuranceConventionId, UUID excludedInvoiceId) {
        InsuranceConventionEntity convention = null;
        if (insuranceConventionId != null) {
            convention = insuranceConventionRepository.findById(insuranceConventionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Convention introuvable"));
        }
        InvoiceEntity invoice = new InvoiceEntity(patientId, visitId, "PRE-CALC", convention);
        if (visitId == null) {
            return invoice;
        }
        VisitEntity visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable"));
        HospitalizationEntity stay = hospitalizationRepository.findByVisitId(visitId).orElse(null);
        BilledSources billed = BilledSources.of(invoiceRepository.findAllByVisitId(visitId), excludedInvoiceId);
        chargeCollector.collect(invoice, visit, stay, billed);
        return invoice;
    }
}
