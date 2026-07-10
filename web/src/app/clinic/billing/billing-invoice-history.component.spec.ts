import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Invoice, InvoiceSettlementSummary } from '../../patient/patient.models';
import { BillingInvoiceHistoryComponent } from './billing-invoice-history.component';

describe('BillingInvoiceHistoryComponent', () => {
  let component: BillingInvoiceHistoryComponent;
  let fixture: ComponentFixture<BillingInvoiceHistoryComponent>;

  const invoice: Invoice = {
    id: 'invoice-2202',
    patientId: 'patient-2202',
    visitId: 'visit-2202',
    invoiceNumber: 'FAC-20260710-002202',
    totalAmount: 100000,
    patientShare: 20000,
    insuranceShare: 80000,
    status: 'VALIDATED',
    items: [],
    createdAt: '2026-07-10T08:00:00Z',
  };

  const summary = (
    collectionStatus: InvoiceSettlementSummary['collectionStatus'],
    patientRemainingAmount: number,
    insuranceRemainingAmount = 0,
  ): InvoiceSettlementSummary => ({
    invoiceId: invoice.id,
    collectionStatus,
    patient: {
      totalAmount: 20000,
      paidAmount: 20000 - patientRemainingAmount,
      remainingAmount: patientRemainingAmount,
      status: patientRemainingAmount === 0 ? 'PAID' : patientRemainingAmount === 20000 ? 'UNPAID' : 'PARTIALLY_PAID',
    },
    insurance: {
      totalAmount: 80000,
      paidAmount: 80000 - insuranceRemainingAmount,
      remainingAmount: insuranceRemainingAmount,
      status: insuranceRemainingAmount === 0 ? 'PAID' : insuranceRemainingAmount === 80000 ? 'UNPAID' : 'PARTIALLY_PAID',
    },
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BillingInvoiceHistoryComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(BillingInvoiceHistoryComponent);
    component = fixture.componentInstance;
    component.translate = (_key, fallback) => fallback;
    component.invoices = [invoice];
  });

  it('distinguishes loading from an empty history', () => {
    component.loading = true;
    component.invoices = [];
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('[role="status"]')?.textContent).toContain('Chargement des factures');
    expect(host.textContent).not.toContain('Aucune facture émise');
  });

  it('renders a retry action when invoice loading fails', () => {
    component.error = true;
    component.invoices = [];
    const retrySpy = vi.spyOn(component.retry, 'emit');
    fixture.detectChanges();

    const retryButton = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button')
    ).find((button) => button.textContent?.includes('Réessayer'));

    expect((fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')).not.toBeNull();
    retryButton?.click();
    expect(retrySpy).toHaveBeenCalledOnce();
  });

  it('renders backend remaining amounts and the patient collection action', () => {
    component.settlements = { [invoice.id]: summary('PATIENT_PARTIALLY_PAID', 5000, 80000) };
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(component.getPatientRemainingAmount(invoice)).toBe(5000);
    expect(component.getInsuranceRemainingAmount(invoice)).toBe(80000);
    expect(text).toContain('Reste patient');
    expect(text).toContain('Reste assurance');
    expect(text).toContain('Encaisser');
    expect(text).not.toContain('Suivre l’assurance');
  });

  it('offers insurance follow-up as the only financial action when the patient share is paid', () => {
    component.settlements = { [invoice.id]: summary('INSURANCE_DUE', 0, 80000) };
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Suivre l’assurance');
    expect(text).not.toContain('Encaisser');
  });

  it('does not expose a financial action for a settled invoice', () => {
    component.settlements = { [invoice.id]: summary('SETTLED', 0, 0) };
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Soldée');
    expect(text).not.toContain('Encaisser');
    expect(text).not.toContain('Suivre l’assurance');
    expect(text).toContain('Détail');
    expect(text).toContain('PDF');
  });

  it('marks the selected invoice and restores focus to its card', () => {
    component.selectedInvoiceId = invoice.id;
    fixture.detectChanges();

    const card = (fixture.nativeElement as HTMLElement).querySelector<HTMLElement>('[data-invoice-id]');
    expect(card?.getAttribute('aria-current')).toBe('true');

    component.focusInvoice(invoice.id);
    expect(document.activeElement).toBe(card);
  });

  it('uses safe persistent-status fallbacks when no settlement summary is available', () => {
    expect(component.canCollectPatient({ ...invoice, status: 'PENDING' })).toBe(false);
    expect(component.canCollectPatient({ ...invoice, status: 'VALIDATED' })).toBe(true);
    expect(component.getCollectionStatusLabel({ ...invoice, status: 'SETTLED' })).toBe('Soldée');
    expect(component.getCollectionStatusLabel({ ...invoice, status: 'CANCELLED' })).toBe('Annulée');
  });
});