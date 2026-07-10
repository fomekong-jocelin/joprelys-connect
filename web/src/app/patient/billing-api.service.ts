import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, Subject, tap } from 'rxjs';
import { CashSessionHistory } from './cash-session-history.models';
import { CashierCollectionQueueItem } from './cashier-collection.models';
import {
  GenerateInsuranceBordereauRequest,
  InsuranceBordereau,
  InsuranceBordereauDetails,
} from './insurance-bordereau.models';
import {
  CashMovement,
  CashRegister,
  CashSession,
  CashSessionSummary,
  CreditNote,
  Estimate,
  InsuranceConvention,
  Invoice,
  InvoiceItem,
  InvoiceSettlementSummary,
  Payment,
  PaymentReceipt,
  Receivable,
  TariffGrid,
} from './patient.models';

@Injectable({ providedIn: 'root' })
export class BillingApiService {
  private readonly http = inject(HttpClient);
  private readonly cashSessionClosedSubject = new Subject<CashSession>();

  readonly cashSessionClosed$ = this.cashSessionClosedSubject.asObservable();

  listConventions(): Observable<InsuranceConvention[]> {
    return this.http.get<InsuranceConvention[]>('/api/invoices/conventions');
  }

  createConvention(name: string, coveragePercentage: number): Observable<InsuranceConvention> {
    const params = new HttpParams()
      .set('name', name)
      .set('coveragePercentage', coveragePercentage.toString());
    return this.http.post<InsuranceConvention>('/api/invoices/conventions', null, { params });
  }

  listTariffs(): Observable<TariffGrid[]> {
    return this.http.get<TariffGrid[]>('/api/invoices/tariffs');
  }

  createOrUpdateTariff(keyLetter: string, unitValue: number): Observable<TariffGrid> {
    const params = new HttpParams()
      .set('keyLetter', keyLetter)
      .set('unitValue', unitValue.toString());
    return this.http.post<TariffGrid>('/api/invoices/tariffs', null, { params });
  }

  precalculateInvoice(patientId: string, visitId?: string, insuranceConventionId?: string): Observable<Invoice> {
    let params = new HttpParams().set('patientId', patientId);
    if (visitId) params = params.set('visitId', visitId);
    if (insuranceConventionId) params = params.set('insuranceConventionId', insuranceConventionId);
    return this.http.post<Invoice>('/api/invoices/precalculate', null, { params });
  }

  createInvoice(request: {
    patientId: string;
    visitId?: string;
    insuranceConventionId?: string;
    items?: InvoiceItem[];
  }): Observable<Invoice> {
    return this.http.post<Invoice>('/api/invoices', request);
  }

  listInvoices(patientId: string): Observable<Invoice[]> {
    return this.http.get<Invoice[]>('/api/invoices', { params: new HttpParams().set('patientId', patientId) });
  }

  listCashierCollectionQueue(): Observable<CashierCollectionQueueItem[]> {
    return this.http.get<CashierCollectionQueueItem[]>('/api/invoices/collection-queue');
  }

  listInvoiceSettlementSummaries(patientId: string): Observable<InvoiceSettlementSummary[]> {
    return this.http.get<InvoiceSettlementSummary[]>('/api/invoices/settlement-summaries', {
      params: new HttpParams().set('patientId', patientId),
    });
  }

  getInvoice(id: string): Observable<Invoice> {
    return this.http.get<Invoice>(`/api/invoices/${id}`);
  }

  validateInvoice(invoiceId: string): Observable<Invoice> {
    return this.http.post<Invoice>(`/api/invoices/${invoiceId}/validate`, null);
  }

  cancelInvoice(invoiceId: string): Observable<Invoice> {
    return this.http.post<Invoice>(`/api/invoices/${invoiceId}/cancel`, null);
  }

  applyDiscount(invoiceId: string, discountAmount: number, discountReason: string): Observable<Invoice> {
    return this.http.post<Invoice>(`/api/invoices/${invoiceId}/discount`, { discountAmount, discountReason });
  }

  addPayment(invoiceId: string, amount: number, method: 'CASH' | 'CHECK' | 'BANK_TRANSFER', reference?: string): Observable<Payment> {
    return this.http.post<Payment>(`/api/invoices/${invoiceId}/payments`, { amount, method, reference });
  }

  listPayments(invoiceId: string): Observable<Payment[]> {
    return this.http.get<Payment[]>(`/api/invoices/${invoiceId}/payments`);
  }

  getInvoicePdfUrl(invoiceId: string): string {
    return `/api/invoices/${invoiceId}/pdf`;
  }

  downloadInvoicePdf(invoiceId: string): Observable<Blob> {
    return this.http.get(`/api/invoices/${invoiceId}/pdf`, { responseType: 'blob' });
  }

  createCreditNote(invoiceId: string, amount: number, reason: string): Observable<CreditNote> {
    return this.http.post<CreditNote>(`/api/invoices/${invoiceId}/credit-notes`, { amount, reason });
  }

  listCreditNotes(invoiceId: string): Observable<CreditNote[]> {
    return this.http.get<CreditNote[]>(`/api/invoices/${invoiceId}/credit-notes`);
  }

  getReceivablesByDebtor(debtorId: string): Observable<Receivable[]> {
    return this.http.get<Receivable[]>('/api/receivables', { params: new HttpParams().set('debtorId', debtorId) });
  }

  getReceivablesByInvoice(invoiceId: string): Observable<Receivable[]> {
    return this.http.get<Receivable[]>(`/api/invoices/${invoiceId}/receivables`);
  }

  getReceivablesByStatus(status: string): Observable<Receivable[]> {
    return this.http.get<Receivable[]>('/api/receivables/by-status', { params: new HttpParams().set('status', status) });
  }

  recordReminder(receivableId: string, request: { actionType: string; status: string; notes: string }): Observable<unknown> {
    return this.http.post<unknown>(`/api/receivables/${receivableId}/reminders`, request);
  }

  getReminders(receivableId: string): Observable<unknown[]> {
    return this.http.get<unknown[]>(`/api/receivables/${receivableId}/reminders`);
  }

  createEstimate(request: {
    patientId: string;
    visitId?: string;
    items: Array<{ label: string; itemType: string; unitPrice: number; quantity: number }>;
  }): Observable<Estimate> {
    return this.http.post<Estimate>('/api/estimates', request);
  }

  listEstimates(patientId: string): Observable<Estimate[]> {
    return this.http.get<Estimate[]>('/api/estimates', { params: new HttpParams().set('patientId', patientId) });
  }

  getEstimate(id: string): Observable<Estimate> {
    return this.http.get<Estimate>(`/api/estimates/${id}`);
  }

  updateEstimateStatus(id: string, status: string): Observable<Estimate> {
    return this.http.patch<Estimate>(`/api/estimates/${id}/status`, null, {
      params: new HttpParams().set('status', status),
    });
  }

  listCashRegisters(): Observable<CashRegister[]> {
    return this.http.get<CashRegister[]>('/api/cash-registers');
  }

  openCashSession(cashRegisterId: string | null, openingBalance: number): Observable<CashSession> {
    return this.http.post<CashSession>('/api/cash-registers/sessions/open', { cashRegisterId, openingBalance });
  }

  getActiveCashSession(): Observable<CashSession> {
    return this.http.get<CashSession>('/api/cash-registers/sessions/active');
  }

  getActiveCashSessionSummary(): Observable<CashSessionSummary> {
    return this.http.get<CashSessionSummary>('/api/cash-registers/sessions/active/summary');
  }

  listMyCashSessions(): Observable<CashSessionHistory[]> {
    return this.http.get<CashSessionHistory[]>('/api/cash-registers/sessions/mine');
  }

  downloadCashCloseoutReport(sessionId: string): Observable<Blob> {
    return this.http.get(`/api/cash-registers/sessions/${sessionId}/closeout-report`, { responseType: 'blob' });
  }

  addCashMovement(request: {
    movementType: 'IN' | 'OUT' | 'TRANSFER_TO_BANK';
    amount: number;
    description: string;
    paymentMethod: 'CASH' | 'CHECK' | 'BANK_TRANSFER';
    referenceNumber?: string;
    doubleVisaApproved?: boolean;
  }): Observable<CashMovement> {
    return this.http.post<CashMovement>('/api/cash-registers/movements', request);
  }

  getSessionMovements(sessionId: string): Observable<CashMovement[]> {
    return this.http.get<CashMovement[]>(`/api/cash-registers/sessions/${sessionId}/movements`);
  }

  closeCashSession(declaredBalance: number, discrepancyReason?: string): Observable<CashSession> {
    return this.http.post<CashSession>('/api/cash-registers/sessions/close', { declaredBalance, discrepancyReason }).pipe(
      tap((session) => this.cashSessionClosedSubject.next(session)),
    );
  }

  getPaymentReceipt(paymentId: string): Observable<PaymentReceipt> {
    return this.http.get<PaymentReceipt>(`/api/cash-registers/payments/${paymentId}/receipt`);
  }

  listSessionsByRegister(registerId: string): Observable<CashSession[]> {
    return this.http.get<CashSession[]>(`/api/cash-registers/${registerId}/sessions`);
  }

  generateInsuranceBordereau(request: GenerateInsuranceBordereauRequest): Observable<InsuranceBordereau> {
    return this.http.post<InsuranceBordereau>('/api/billing/insurance-bordereaux', request);
  }

  listInsuranceBordereaux(): Observable<InsuranceBordereau[]> {
    return this.http.get<InsuranceBordereau[]>('/api/billing/insurance-bordereaux');
  }

  getInsuranceBordereauDetails(id: string): Observable<InsuranceBordereauDetails> {
    return this.http.get<InsuranceBordereauDetails>(`/api/billing/insurance-bordereaux/${id}`);
  }

  sendInsuranceBordereau(id: string): Observable<InsuranceBordereau> {
    return this.http.post<InsuranceBordereau>(`/api/billing/insurance-bordereaux/${id}/send`, null);
  }

  receiveInsuranceBordereau(id: string, insurerReference: string): Observable<InsuranceBordereau> {
    return this.http.post<InsuranceBordereau>(`/api/billing/insurance-bordereaux/${id}/receive`, { insurerReference });
  }

  acceptInsuranceBordereau(id: string, acceptedAmount: number, insurerReference?: string): Observable<InsuranceBordereau> {
    return this.http.post<InsuranceBordereau>(`/api/billing/insurance-bordereaux/${id}/accept`, {
      acceptedAmount,
      insurerReference,
    });
  }

  rejectInsuranceBordereau(id: string, rejectionReason: string, insurerReference?: string): Observable<InsuranceBordereau> {
    return this.http.post<InsuranceBordereau>(`/api/billing/insurance-bordereaux/${id}/reject`, {
      rejectionReason,
      insurerReference,
    });
  }

  payInsuranceBordereau(id: string, amount: number, referenceNumber: string): Observable<InsuranceBordereau> {
    return this.http.post<InsuranceBordereau>(`/api/billing/insurance-bordereaux/${id}/pay`, {
      amount,
      referenceNumber,
    });
  }

  listAllSessions(): Observable<CashSession[]> {
    return this.http.get<CashSession[]>('/api/cash-registers/sessions');
  }

  resolveDiscrepancy(sessionId: string, resolutionNotes: string): Observable<CashSession> {
    return this.http.post<CashSession>(`/api/cash-registers/sessions/${sessionId}/resolve-discrepancy`, { resolutionNotes });
  }

  exportAccounting(startDate?: string, endDate?: string): Observable<Blob> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get('/api/accounting/export', { params, responseType: 'blob' });
  }
}
