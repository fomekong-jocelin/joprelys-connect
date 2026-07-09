package com.joprelys.backend.billing.api;

import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.billing.application.BillingService;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceRepository;
import com.joprelys.backend.billing.infrastructure.persistence.PaymentEntity;
import com.joprelys.backend.billing.infrastructure.persistence.PaymentRepository;
import com.joprelys.backend.billing.infrastructure.persistence.TariffGridEntity;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Billing", description = "Gestion de la facturation médicale et des règlements de caisse")
public class InvoiceController {

    private final BillingService billingService;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PatientRepository patientRepository;
    private final OrganizationRepository organizationRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final QrCodeGeneratorService qrCodeGeneratorService;

    public InvoiceController(BillingService billingService,
                             InvoiceRepository invoiceRepository,
                             PaymentRepository paymentRepository,
                             PatientRepository patientRepository,
                             OrganizationRepository organizationRepository,
                             PdfGeneratorService pdfGeneratorService,
                             QrCodeGeneratorService qrCodeGeneratorService) {
        this.billingService = billingService;
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
        InvoiceResponse response = billingService.precalculateInvoice(patientId, visitId, insuranceConventionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER')")
    @Operation(summary = "Créer une facture", description = "Crée et enregistre une facture pour un patient.")
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        InvoiceResponse response = billingService.createInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER')")
    @Operation(summary = "Lister les factures d'un patient", description = "Récupère l'historique des factures d'un patient.")
    public ResponseEntity<List<InvoiceResponse>> listInvoices(@RequestParam UUID patientId) {
        List<InvoiceResponse> list = billingService.listInvoices(patientId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER')")
    @Operation(summary = "Détails d'une facture", description = "Récupère les détails d'une facture par son identifiant.")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable UUID id) {
        InvoiceResponse response = billingService.getInvoice(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER')")
    @Operation(summary = "Enregistrer un règlement", description = "Enregistre un paiement sur une facture.")
    public ResponseEntity<PaymentResponse> addPayment(@PathVariable UUID id,
                                                      @Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = billingService.addPayment(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER')")
    @Operation(summary = "Lister les règlements d'une facture", description = "Récupère tous les paiements enregistrés pour une facture.")
    public ResponseEntity<List<PaymentResponse>> listPayments(@PathVariable UUID id) {
        List<PaymentResponse> list = billingService.listPayments(id);
        return ResponseEntity.ok(list);
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

        double totalPaid = paymentRepository.findByInvoiceId(id).stream()
                .mapToDouble(PaymentEntity::getAmount)
                .sum();

        // 1. Generate QR Code for public verification of this invoice
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
        return ResponseEntity.ok(billingService.listConventions());
    }

    @PostMapping("/conventions")
    @PreAuthorize("hasRole('ADMIN_CLINIQUE')")
    @Operation(summary = "Créer une convention d'assurance", description = "Paramètre une nouvelle convention d'assurance.")
    public ResponseEntity<InsuranceConventionDto> createConvention(@RequestParam String name,
                                                                   @RequestParam Double coveragePercentage) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billingService.createConvention(name, coveragePercentage));
    }

    @GetMapping("/tariffs")
    @PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'MEDECIN', 'INFIRMIER')")
    @Operation(summary = "Lister la grille des tarifs", description = "Récupère la grille tarifaire (K, AMI, etc.).")
    public ResponseEntity<List<TariffGridEntity>> listTariffs() {
        return ResponseEntity.ok(billingService.listTariffs());
    }

    @PostMapping("/tariffs")
    @PreAuthorize("hasRole('ADMIN_CLINIQUE')")
    @Operation(summary = "Créer ou mettre à jour un tarif", description = "Ajoute ou modifie un tarif clé de la grille.")
    public ResponseEntity<TariffGridEntity> createOrUpdateTariff(@RequestParam String keyLetter,
                                                                 @RequestParam Double unitValue) {
        return ResponseEntity.ok(billingService.createOrUpdateTariff(keyLetter, unitValue));
    }
}
