package com.joprelys.backend.billing.api;

import com.joprelys.backend.billing.application.BillingPaymentService;
import com.joprelys.backend.billing.application.CashierCollectionQueueService;
import com.joprelys.backend.billing.application.ConventionTariffService;
import com.joprelys.backend.billing.application.InvoiceCrudService;
import com.joprelys.backend.billing.application.InvoicePrecalculationService;
import com.joprelys.backend.billing.application.InvoiceSettlementQueryService;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceRepository;
import com.joprelys.backend.billing.infrastructure.persistence.PaymentRepository;
import com.joprelys.backend.billing.infrastructure.persistence.TariffGridEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.application.PdfGeneratorService;
import com.joprelys.backend.visit.application.QrCodeGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Billing", description = "Gestion de la facturation médicale et des règlements de caisse")
public class InvoiceController {

    private final InvoiceCrudService invoiceCrudService;
    private final InvoicePrecalculationService precalculationService;
    private final ConventionTariffService conventionTariffService;
    private final BillingPaymentService billingPaymentService;
    private final InvoiceSettlementQueryService invoiceSettlementQueryService;
    private final CashierCollectionQueueService cashierCollectionQueueService;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PatientRepository patientRepository;
    private final OrganizationRepository organizationRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final QrCodeGeneratorService qrCodeGeneratorService;

    public InvoiceController(InvoiceCrudService invoiceCrudService,
                             InvoicePrecalculationService precalculationService,
                             ConventionTariffService conventionTariffService,
                             BillingPaymentService billingPaymentService,
                             InvoiceSettlementQueryService invoiceSettlementQueryService,
                             CashierCollectionQueueService cashierCollectionQueueService,
                             InvoiceRepository invoiceRepository,
                             PaymentRepository paymentRepository,
                             PatientRepository patientRepository,
                             OrganizationRepository organizationRepository,
                             PdfGeneratorService pdfGeneratorService,
                             QrCodeGeneratorService qrCodeGeneratorService) {
        this.invoiceCrudService = invoiceCrudService;
        this.precalculationService = precalculationService;
        this.conventionTariffService = conventionTariffService;
        this.billingPaymentService = billingPaymentService;
        this.invoiceSettlementQueryService = invoiceSettlementQueryService;
        this.cashierCollectionQueueService = cashierCollectionQueueService;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.patientRepository = patientRepository;
        this.organizationRepository = organizationRepository;
        this.pdfGeneratorService = pdfGeneratorService;
        this.qrCodeGeneratorService = qrCodeGeneratorService;
    }

    @PostMapping("/precalculate")
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER')")
    @Operation(summary = "Précalculer une facture", description = "Simule le calcul de la facture sans persistance.")
    public ResponseEntity<InvoiceResponse> precalculate(@RequestParam UUID patientId,
                                                        @RequestParam(required = false) UUID visitId,
                                                        @RequestParam(required = false) UUID insuranceConventionId) {
        return ResponseEntity.ok(precalculationService.precalculate(patientId, visitId, insuranceConventionId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER')")
    @Operation(summary = "Créer une facture", description = "Crée et enregistre une facture pour un patient.")
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceCrudService.createInvoice(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER')")
    @Operation(summary = "Lister les factures d'un patient", description = "Récupère l'historique des factures d'un patient.")
    public ResponseEntity<List<InvoiceResponse>> listInvoices(@RequestParam UUID patientId) {
        return ResponseEntity.ok(invoiceCrudService.listInvoices(patientId));
    }

    @GetMapping("/collection-queue")
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'CAISSIER', 'DAF')")
    @Operation(
            summary = "File d'encaissement patient",
            description = "Retourne les factures validées dont la part patient reste à encaisser pour le tenant courant."
    )
    public ResponseEntity<List<CashierCollectionQueueItemResponse>> listCollectionQueue() {
        return ResponseEntity.ok(cashierCollectionQueueService.listQueue());
    }

    @GetMapping("/settlement-summaries")
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER', 'CAISSIER', 'DAF')")
    @Operation(summary = "Synthèse de règlement des factures", description = "Retourne les montants dus et réglés par patient et assurance pour un patient.")
    public ResponseEntity<List<InvoiceSettlementSummaryResponse>> listSettlementSummaries(@RequestParam UUID patientId) {
        return ResponseEntity.ok(invoiceSettlementQueryService.listByPatient(patientId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER')")
    @Operation(summary = "Détails d'une facture", description = "Récupère les détails d'une facture par son identifiant.")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceCrudService.getInvoice(id));
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER', 'CAISSIER', 'DAF')")
    @Operation(summary = "Enregistrer un règlement", description = "Enregistre un paiement sur une facture.")
    public ResponseEntity<PaymentResponse> addPayment(@PathVariable UUID id,
                                                      @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(billingPaymentService.addPayment(id, request));
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER', 'CAISSIER', 'DAF')")
    @Operation(summary = "Lister les règlements d'une facture", description = "Récupère tous les paiements enregistrés pour une facture.")
    public ResponseEntity<List<PaymentResponse>> listPayments(@PathVariable UUID id) {
        return ResponseEntity.ok(billingPaymentService.listPayments(id));
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER')")
    @Operation(summary = "Générer la facture PDF certifiée", description = "Génère et télécharge le fichier PDF d'une facture.")
    public ResponseEntity<byte[]> getPdf(@PathVariable UUID id, Authentication authentication) {
        InvoiceEntity invoice = invoiceRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable"));

        PatientEntity patient = patientRepository.findById(invoice.getPatientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable"));

        OrganizationEntity org = organizationRepository.findById(invoice.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clinique introuvable"));

        BigDecimal totalPaid = paymentRepository.findByInvoiceId(id).stream()
                .map(p -> p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String verificationUrl = "https://joprelys.com/verify/invoice/" + id;
        byte[] qrCodeBytes = qrCodeGeneratorService.generateQrCode(verificationUrl, 150, 150);
        String cashierName = authentication != null ? authentication.getName() : "Caissier principal";

        byte[] pdfBytes = pdfGeneratorService.generateInvoicePdf(
                invoice,
                patient,
                org.getName(),
                org.getAddress(),
                org.getPhone(),
                qrCodeBytes,
                cashierName,
                totalPaid
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"FACTURE_" + invoice.getInvoiceNumber() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/conventions")
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER')")
    @Operation(summary = "Lister les conventions d'assurances", description = "Récupère les conventions paramétrées.")
    public ResponseEntity<List<InsuranceConventionDto>> listConventions() {
        return ResponseEntity.ok(conventionTariffService.listConventions());
    }

    @PostMapping("/conventions")
    @PreAuthorize("hasRole('ADMIN_CLINIQUE')")
    @Operation(summary = "Créer une convention d'assurance", description = "Paramètre une nouvelle convention d'assurance.")
    public ResponseEntity<InsuranceConventionDto> createConvention(@RequestParam String name,
                                                                   @RequestParam BigDecimal coveragePercentage) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conventionTariffService.createConvention(name, coveragePercentage));
    }

    @GetMapping("/tariffs")
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER')")
    @Operation(summary = "Lister la grille des tarifs", description = "Récupère la grille tarifaire (K, AMI, etc.).")
    public ResponseEntity<List<TariffGridEntity>> listTariffs() {
        return ResponseEntity.ok(conventionTariffService.listTariffs());
    }

    @PostMapping("/tariffs")
    @PreAuthorize("hasRole('ADMIN_CLINIQUE')")
    @Operation(summary = "Créer ou mettre à jour un tarif", description = "Ajoute ou modifie un tarif clé de la grille.")
    public ResponseEntity<TariffGridEntity> createOrUpdateTariff(@RequestParam String keyLetter,
                                                                 @RequestParam BigDecimal unitValue) {
        return ResponseEntity.ok(conventionTariffService.createOrUpdateTariff(keyLetter, unitValue));
    }
}
