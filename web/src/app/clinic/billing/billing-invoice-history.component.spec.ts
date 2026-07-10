import { TestBed } from '@angular/core/testing';
import { Invoice, InvoiceSettlementSummary } from '../../patient/patient.models';
import { BillingInvoiceHistoryComponent } from './billing-invoice-history.component';

describe('BillingInvoiceHistoryComponent', () => {
  let component: BillingInvoiceHistoryComponent;

  const invoice: Invoice = {
    id: 'invoice-2201',
    patientId: 'patient-2201',
    invoiceNumber: 'FAC-20260710-002201',
    totalAmount: 100000,
    patientShare: 20000,
    insuranceShare: 80000,
    status: 'PAID',
    items: [],
    createdAt: '2026-07-10T08:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BillingInvoiceHistoryComponent],
    }).compileComponents();
    component = TestBed.createComponent(BillingInvoiceHistoryComponent).componentInstance;
    component.translate = (_key, fallback) => fallback;
  });

  it('does not offer patient collection for a settled invoice without a summary', () => {
    expect(component.canCollectPatient({ ...invoice, status: 'SETTLED' })).toBe(false);
  });

  it('uses the debtor summary rather than the persistent PAID status', () => {
    const summary: InvoiceSettlementSummary = {
      invoiceId: invoice.id,
      collectionStatus: 'INSURANCE_DUE',
      patient: { totalAmount: 20000, paidAmount: 20000, remainingAmount: 0, status: 'PAID' },
      insurance: { totalAmount: 80000, paidAmount: 0, remainingAmount: 80000, status: 'UNPAID' },
    };
    component.settlements = { [invoice.id]: summary };

    expect(component.canCollectPatient(invoice)).toBe(false);
    expect(component.isInsuranceDue(invoice)).toBe(true);
    expect(component.getCollectionStatusLabel(invoice)).toBe('Assurance à recouvrer');
  });

  it('renders cancelled invoices as terminal', () => {
    component.settlements = {
      [invoice.id]: {
        invoiceId: invoice.id,
        collectionStatus: 'CANCELLED',
        patient: { totalAmount: 20000, paidAmount: 0, remainingAmount: 20000, status: 'UNPAID' },
        insurance: { totalAmount: 80000, paidAmount: 0, remainingAmount: 80000, status: 'UNPAID' },
      },
    };

    expect(component.getCollectionStatusLabel({ ...invoice, status: 'CANCELLED' })).toBe('Annulée');
    expect(component.getCollectionStatusClass({ ...invoice, status: 'CANCELLED' })).toContain('text-red-600');
  });
});
