import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { BillingApiService } from '../../patient/billing-api.service';
import { CreditNote, Estimate, Invoice } from '../../patient/patient.models';
import { Visit } from '../../visit/visit.models';
import { BillingEstimatesComponent } from './billing-estimates.component';

describe('BillingEstimatesComponent', () => {
  let component: BillingEstimatesComponent;
  let fixture: ComponentFixture<BillingEstimatesComponent>;
  let billingApi: Record<string, ReturnType<typeof vi.fn>>;

  const invoice: Invoice = {
    id: 'invoice-123', patientId: 'patient-123', visitId: 'visit-123',
    invoiceNumber: 'FAC-20260710-000001', totalAmount: 50000, patientShare: 50000,
    insuranceShare: 0, status: 'PENDING', items: [], createdAt: '2026-07-10T08:00:00Z',
  };
  const estimate: Estimate = {
    id: 'estimate-123', patientId: 'patient-123', visitId: 'visit-123',
    estimateNumber: 'DEV-20260710-000001', totalAmount: 15000, patientShare: 15000,
    insuranceShare: 0, status: 'DRAFT', items: [], createdAt: '2026-07-10T08:00:00Z', updatedAt: '2026-07-10T08:00:00Z',
  };
  const creditNote: CreditNote = {
    id: 'credit-note-123', invoiceId: invoice.id, creditNoteNumber: 'AV-20260710-000001',
    amount: 5000, reason: 'Correction', status: 'ACTIVE', createdAt: '2026-07-10T08:00:00Z',
  };
  const visit: Visit = {
    id: 'visit-123', visitNumber: 'VIS-000123', patientId: 'patient-123', patientName: 'Patient Test',
    patientDpu: 'DPU-001', reason: 'Consultation', orientation: 'AMBULATOIRE', status: 'EN_COURS',
    createdAt: '2026-07-10T08:00:00Z',
  };

  beforeEach(async () => {
    vi.useFakeTimers();
    billingApi = {
      listEstimates: vi.fn().mockReturnValue(of([])),
      listCreditNotes: vi.fn().mockReturnValue(of([])),
      getReceivablesByInvoice: vi.fn().mockReturnValue(of([])),
      createEstimate: vi.fn().mockReturnValue(of(estimate)),
      updateEstimateStatus: vi.fn().mockReturnValue(of(estimate)),
      validateInvoice: vi.fn().mockReturnValue(of({ ...invoice, status: 'VALIDATED' })),
      cancelInvoice: vi.fn().mockReturnValue(of({ ...invoice, status: 'CANCELLED' })),
      applyDiscount: vi.fn().mockReturnValue(of({ ...invoice, discountAmount: 1000, discountReason: 'Geste commercial' })),
      createCreditNote: vi.fn().mockReturnValue(of(creditNote)),
    };
    await TestBed.configureTestingModule({
      imports: [BillingEstimatesComponent],
      providers: [
        { provide: BillingApiService, useValue: billingApi },
        { provide: I18nService, useValue: { t: vi.fn().mockImplementation((key: string) => key), locale: signal('fr') } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(BillingEstimatesComponent);
    component = fixture.componentInstance;
    component.patientId = 'patient-123';
    component.visits = [visit];
    component.initialVisitId = visit.id;
    component.invoice = invoice;
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.runOnlyPendingTimers();
    vi.useRealTimers();
  });

  it('shows a labelled remove action for every estimate line', () => {
    component.showEstimateForm.set(true);
    component.addEstimateItem();
    fixture.detectChanges();
    const buttons = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('.remove-line'));
    expect(buttons).toHaveLength(2);
    expect(buttons[0].textContent).toContain('Supprimer');
    expect(buttons[0].getAttribute('aria-label')).toBe('Supprimer cette ligne');
  });

  it('removes only the selected estimate line', () => {
    component.addEstimateItem();
    component.removeEstimateItem(0);
    expect(component.newEstimateItems()).toHaveLength(1);
    expect(component.newEstimateItems()[0].itemType).toBe('CONSULTATION');
  });

  it('submits an estimate with the selected visit and shows feedback', () => {
    component.newEstimateItems.set([{ label: 'Consultation', itemType: 'CONSULTATION', unitPrice: 15000, quantity: 1 }]);
    component.estimateVisitId = visit.id;
    component.submitEstimate();
    fixture.detectChanges();
    expect(billingApi['createEstimate']).toHaveBeenCalledWith({
      patientId: 'patient-123', visitId: 'visit-123',
      items: [{ label: 'Consultation', itemType: 'CONSULTATION', unitPrice: 15000, quantity: 1 }],
    });
    expect(component.successMessage()).toBe('Devis créé avec succès.');
    expect((fixture.nativeElement as HTMLElement).querySelector('[role="status"]')).not.toBeNull();
  });

  it('validates the selected invoice through the billing API', () => {
    component.validateCurrentInvoice();
    expect(billingApi['validateInvoice']).toHaveBeenCalledWith(invoice.id);
    expect(component.selectedInvoice()?.status).toBe('VALIDATED');
  });

  it('opens a custom confirmation dialog before cancellation', () => {
    component.requestCancelCurrentInvoice();
    fixture.detectChanges();
    expect(component.showCancelInvoiceDialog()).toBe(true);
    expect(billingApi['cancelInvoice']).not.toHaveBeenCalled();
    expect((fixture.nativeElement as HTMLElement).querySelector('[role="alertdialog"]')).not.toBeNull();
    component.confirmCancelCurrentInvoice();
    expect(billingApi['cancelInvoice']).toHaveBeenCalledWith(invoice.id);
    expect(component.selectedInvoice()?.status).toBe('CANCELLED');
    expect(component.showCancelInvoiceDialog()).toBe(false);
  });

  it('creates a credit note', () => {
    component.creditNoteAmount = 5000;
    component.creditNoteReason = 'Correction';
    component.submitCreditNote();
    expect(billingApi['createCreditNote']).toHaveBeenCalledWith(invoice.id, 5000, 'Correction');
    expect(component.creditNotes()).toEqual([creditNote]);
  });

  it('renders an error banner when estimate creation fails', () => {
    billingApi['createEstimate'].mockReturnValueOnce(throwError(() => new Error('network error')));
    component.newEstimateItems.set([{ label: 'Consultation', itemType: 'CONSULTATION', unitPrice: 15000, quantity: 1 }]);
    component.submitEstimate();
    fixture.detectChanges();
    expect(component.errorMessage()).toBe('Erreur lors de la création du devis.');
    expect((fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')).not.toBeNull();
  });
});
