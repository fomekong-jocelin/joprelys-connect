package com.joprelys.backend.billing.application;

import com.joprelys.backend.billing.api.*;
import com.joprelys.backend.billing.infrastructure.persistence.TariffGridEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Façade de compatibilité — délègue vers les services spécialisés.
 * Conservé pour ne pas casser les contrôleurs existants.
 * @deprecated Injectez directement {@link InvoiceCrudService}, {@link InvoicePrecalculationService}
 *             ou {@link ConventionTariffService} selon le besoin.
 */
@Deprecated(since = "billing-service-split", forRemoval = true)
@Service
public class BillingService {

    private final InvoiceCrudService invoiceCrudService;
    private final InvoicePrecalculationService precalculationService;
    private final ConventionTariffService conventionTariffService;

    public BillingService(InvoiceCrudService invoiceCrudService,
                          InvoicePrecalculationService precalculationService,
                          ConventionTariffService conventionTariffService) {
        this.invoiceCrudService = invoiceCrudService;
        this.precalculationService = precalculationService;
        this.conventionTariffService = conventionTariffService;
    }

    public List<InsuranceConventionDto> listConventions() {
        return conventionTariffService.listConventions();
    }

    public InsuranceConventionDto createConvention(String name, BigDecimal coveragePercentage) {
        return conventionTariffService.createConvention(name, coveragePercentage);
    }

    public List<TariffGridEntity> listTariffs() {
        return conventionTariffService.listTariffs();
    }

    public TariffGridEntity createOrUpdateTariff(String keyLetter, BigDecimal unitValue) {
        return conventionTariffService.createOrUpdateTariff(keyLetter, unitValue);
    }

    public InvoiceResponse precalculateInvoice(UUID patientId, UUID visitId, UUID insuranceConventionId) {
        return precalculationService.precalculate(patientId, visitId, insuranceConventionId);
    }

    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        return invoiceCrudService.createInvoice(request);
    }

    public List<InvoiceResponse> listInvoices(UUID patientId) {
        return invoiceCrudService.listInvoices(patientId);
    }

    public InvoiceResponse getInvoice(UUID id) {
        return invoiceCrudService.getInvoice(id);
    }
}
