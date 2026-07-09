import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { InsuranceConvention, TariffGrid, Invoice, Payment, InvoiceItem, Estimate, CreditNote, Receivable } from './patient.models';

@Injectable({
  providedIn: 'root',
})
export class BillingApiService {
  private readonly http = inject(HttpClient);

  // ── Conventions ──────────────────────────────────────────────────
  listConventions(): Observable<InsuranceConvention[]> {
    return this.http.get<InsuranceConvention[]>('/api/invoices/conventions');
  }

  createConvention(name: string, coveragePercentage: number): Observable<InsuranceConvention> {
    const params = new HttpParams()
      .set('name', name)
      .set('coveragePercentage', coveragePercentage.toString());
    return this.http.post<InsuranceConvention>('/api/invoices/conventions', null, { params });
  }

  // ── Tariffs ───────────────────────────────────────────────────────
  listTariffs(): Observable<TariffGrid[]> {
    return this.http.get<TariffGrid[]>('/api/invoices/tariffs');
  }

  createOrUpdateTariff(keyLetter: string, unitValue: number): Observable<TariffGrid> {
    const params = new HttpParams()
      .set('keyLetter', keyLetter)
      .set('unitValue', unitValue.toString());
    return this.http.post<TariffGrid>('/api/invoices/tariffs', null, { params });
  }

  // ── Invoices ──────────────────────────────────────────────────────
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
    const params = new HttpParams().set('patientId', patientId);
    return this.http.get<Invoice[]>('/api/invoices', { params });
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

  // ── Payments ──────────────────────────────────────────────────────
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

  // ── Credit Notes ──────────────────────────────────────────────────
  createCreditNote(invoiceId: string, amount: number, reason: string): Observable<CreditNote> {
    return this.http.post<CreditNote>(`/api/invoices/${invoiceId}/credit-notes`, { amount, reason });
  }

  listCreditNotes(invoiceId: string): Observable<CreditNote[]> {
    return this.http.get<CreditNote[]>(`/api/invoices/${invoiceId}/credit-notes`);
  }

  // ── Receivables ───────────────────────────────────────────────────
  getReceivablesByDebtor(debtorId: string): Observable<Receivable[]> {
    const params = new HttpParams().set('debtorId', debtorId);
    return this.http.get<Receivable[]>('/api/receivables', { params });
  }

  getReceivablesByInvoice(invoiceId: string): Observable<Receivable[]> {
    return this.http.get<Receivable[]>(`/api/invoices/${invoiceId}/receivables`);
  }

  getReceivablesByStatus(status: string): Observable<Receivable[]> {
    const params = new HttpParams().set('status', status);
    return this.http.get<Receivable[]>('/api/receivables/by-status', { params });
  }

  // ── Estimates ─────────────────────────────────────────────────────
  createEstimate(request: {
    patientId: string;
    visitId?: string;
    items: Array<{ label: string; itemType: string; unitPrice: number; quantity: number }>;
  }): Observable<Estimate> {
    return this.http.post<Estimate>('/api/estimates', request);
  }

  listEstimates(patientId: string): Observable<Estimate[]> {
    const params = new HttpParams().set('patientId', patientId);
    return this.http.get<Estimate[]>('/api/estimates', { params });
  }

  getEstimate(id: string): Observable<Estimate> {
    return this.http.get<Estimate>(`/api/estimates/${id}`);
  }

  updateEstimateStatus(id: string, status: string): Observable<Estimate> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<Estimate>(`/api/estimates/${id}/status`, null, { params });
  }
}


