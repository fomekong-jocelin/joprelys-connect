from pathlib import Path


def replace_required(path: str, old: str, new: str) -> None:
    file_path = Path(path)
    text = file_path.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Pattern missing in {path}: {old[:160]!r}")
    file_path.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_required(
    "backend/src/main/java/com/joprelys/backend/accounting/application/AccountingExportService.java",
    "// 1. Journal des Ventes (Factures VALIDATED, PAID, PARTIALLY_PAID)\n        List<InvoiceStatus> salesStatuses = List.of(InvoiceStatus.VALIDATED, InvoiceStatus.PAID, InvoiceStatus.PARTIALLY_PAID);",
    "// 1. Journal des Ventes (toutes les factures validées, quel que soit leur niveau de recouvrement)\n        List<InvoiceStatus> salesStatuses = List.of(\n                InvoiceStatus.VALIDATED,\n                InvoiceStatus.PARTIALLY_PAID,\n                InvoiceStatus.PAID,\n                InvoiceStatus.SETTLED);"
)

replace_required(
    "backend/src/test/java/com/joprelys/backend/cash/CashRegisterControllerTest.java",
    '''    void testOpenCloseSessionAndMovements() throws Exception {
        // 1. Essayer de payer la facture sans session ouverte -> 409 CONFLICT
        PaymentRequest payReq = new PaymentRequest(''',
    '''    void testOpenCloseSessionAndMovements() throws Exception {
        // La facture doit être validée avant tout encaissement.
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/validate")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATED"));

        // 1. Essayer de payer la facture sans session ouverte -> 409 CONFLICT
        PaymentRequest payReq = new PaymentRequest('''
)
