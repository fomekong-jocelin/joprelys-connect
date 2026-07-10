from pathlib import Path


def replace_required(path: str, old: str, new: str) -> None:
    file_path = Path(path)
    text = file_path.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Pattern missing in {path}: {old[:120]!r}")
    file_path.write_text(text.replace(old, new, 1), encoding="utf-8")


# Avoid recalculating historical paid amounts twice and expose a coherent fallback party status.
replace_required(
    "backend/src/main/java/com/joprelys/backend/billing/application/InvoiceFinancialStateService.java",
    '''                .orElseGet(() -> new SettlementPartyResponse(
                        share,
                        inferredPaidAmount(invoice, debtorType, share),
                        share.subtract(inferredPaidAmount(invoice, debtorType, share)).max(BigDecimal.ZERO),
                        fallbackPartyStatus(invoice, share)));''',
    '''                .orElseGet(() -> {
                    BigDecimal inferredPaid = inferredPaidAmount(invoice, debtorType, share);
                    return new SettlementPartyResponse(
                            share,
                            inferredPaid,
                            share.subtract(inferredPaid).max(BigDecimal.ZERO),
                            fallbackPartyStatus(invoice, share, inferredPaid));
                });'''
)
replace_required(
    "backend/src/main/java/com/joprelys/backend/billing/application/InvoiceFinancialStateService.java",
    '''    private String fallbackPartyStatus(InvoiceEntity invoice, BigDecimal share) {
        if (share.signum() <= 0 || invoice.getStatus() == InvoiceStatus.PENDING || invoice.getStatus() == InvoiceStatus.PROFORMA) {
            return "NOT_DUE";
        }
        BigDecimal paid = invoice.getStatus() == InvoiceStatus.SETTLED ? share : BigDecimal.ZERO;
        return paid.compareTo(share) >= 0 ? "PAID" : "UNPAID";
    }''',
    '''    private String fallbackPartyStatus(InvoiceEntity invoice, BigDecimal share, BigDecimal paid) {
        if (share.signum() <= 0 || invoice.getStatus() == InvoiceStatus.PENDING || invoice.getStatus() == InvoiceStatus.PROFORMA) {
            return "NOT_DUE";
        }
        if (paid.compareTo(share) >= 0) {
            return "PAID";
        }
        return paid.signum() > 0 ? "PARTIALLY_PAID" : "UNPAID";
    }'''
)

# The E2E lifecycle must validate the invoice before payment and end as SETTLED.
replace_required(
    "backend/src/test/java/com/joprelys/backend/billing/FullFinancialE2ETest.java",
    '''        invoice = invoiceRepository.saveAndFlush(invoice);

        // Déclencher le workflow financier sur la facture''',
    '''        invoice = invoiceRepository.saveAndFlush(invoice);

        TenantContext.clear();
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/validate")
                        .header("Authorization", "Bearer " + tokenMedecin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATED"));
        setTenant();
        invoice = invoiceRepository.findById(invoice.getId()).orElseThrow();

        // Déclencher le workflow financier sur la facture'''
)
replace_required(
    "backend/src/test/java/com/joprelys/backend/billing/FullFinancialE2ETest.java",
    '''        assertEquals(InvoiceStatus.PAID, savedInvoice.getStatus());''',
    '''        assertEquals(InvoiceStatus.SETTLED, savedInvoice.getStatus());'''
)

# Historical invoice missing its patient receivable must be repaired without becoming settled.
replace_required(
    "backend/src/test/java/com/joprelys/backend/billing/InsuranceBordereauControllerTest.java",
    '''        assertEquals(new java.math.BigDecimal("20000.0000"), recs.get(0).getPaidAmount());''',
    '''        assertEquals(new java.math.BigDecimal("20000.0000"), recs.get(0).getPaidAmount());

        InvoiceEntity synchronizedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertEquals(InvoiceStatus.VALIDATED, synchronizedInvoice.getStatus());
        List<ReceivableEntity> patientReceivables = receivableRepository
                .findByInvoiceIdAndDebtorTypeIgnoreCase(invoice.getId(), "PATIENT");
        assertEquals(1, patientReceivables.size());
        assertEquals("UNPAID", patientReceivables.get(0).getStatus());'''
)

# Extend the Angular contracts and prevent collection actions on settled invoices.
replace_required(
    "web/src/app/patient/patient.models.ts",
    "status: 'PENDING' | 'PARTIALLY_PAID' | 'PAID' | 'PROFORMA' | 'VALIDATED' | 'CANCELLED';",
    "status: 'PENDING' | 'PARTIALLY_PAID' | 'PAID' | 'SETTLED' | 'PROFORMA' | 'VALIDATED' | 'CANCELLED';"
)
replace_required(
    "web/src/app/patient/patient.models.ts",
    "collectionStatus: 'NOT_YET_DUE' | 'PATIENT_DUE' | 'PATIENT_PARTIALLY_PAID' | 'INSURANCE_DUE' | 'SETTLED';",
    "collectionStatus: 'NOT_YET_DUE' | 'PATIENT_DUE' | 'PATIENT_PARTIALLY_PAID' | 'INSURANCE_DUE' | 'SETTLED' | 'CANCELLED';"
)
replace_required(
    "web/src/app/clinic/billing/billing-invoice-history.component.ts",
    "return invoice.status !== 'PAID' && invoice.status !== 'CANCELLED';",
    "return invoice.status !== 'PAID' && invoice.status !== 'SETTLED' && invoice.status !== 'CANCELLED';"
)
replace_required(
    "web/src/app/clinic/billing/billing-invoice-history.component.ts",
    '''      case 'INSURANCE_DUE':
        return 'bg-blue-500/15 text-blue-600 dark:text-blue-400';''',
    '''      case 'INSURANCE_DUE':
        return 'bg-blue-500/15 text-blue-600 dark:text-blue-400';
      case 'CANCELLED':
        return 'bg-red-500/15 text-red-600 dark:text-red-400';'''
)
replace_required(
    "web/src/app/clinic/billing/billing-invoice-history.component.ts",
    '''      case 'NOT_YET_DUE':
        return this.translate('billing.collection.notYetDue', 'À valider');''',
    '''      case 'NOT_YET_DUE':
        return this.translate('billing.collection.notYetDue', 'À valider');
      case 'CANCELLED':
        return this.translate('billing.collection.cancelled', 'Annulée');'''
)
