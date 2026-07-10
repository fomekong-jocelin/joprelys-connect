import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { BillingApiService } from '../../patient/billing-api.service';
import { CashierCollectionQueueItem } from '../../patient/cashier-collection.models';
import { BillingCashierQueueComponent } from './billing-cashier-queue.component';

describe('BillingCashierQueueComponent', () => {
  let fixture: ComponentFixture<BillingCashierQueueComponent>;
  let component: BillingCashierQueueComponent;

  const items: CashierCollectionQueueItem[] = [
    {
      invoiceId: 'invoice-due',
      invoiceNumber: 'FAC-20260710-000001',
      patientId: 'patient-1',
      patientName: 'Alice Mbarga',
      globalPatientNumber: 'DPU-000001',
      phone: '677000001',
      totalAmount: 50000,
      patientRemainingAmount: 20000,
      collectionStatus: 'PATIENT_DUE',
      createdAt: '2026-07-10T08:00:00Z',
      validatedAt: '2026-07-10T08:15:00Z',
    },
    {
      invoiceId: 'invoice-partial',
      invoiceNumber: 'FAC-20260710-000002',
      patientId: 'patient-2',
      patientName: 'Brice Nkoa',
      globalPatientNumber: 'DPU-000002',
      totalAmount: 80000,
      patientRemainingAmount: 5000,
      collectionStatus: 'PATIENT_PARTIALLY_PAID',
      createdAt: '2026-07-10T09:00:00Z',
      validatedAt: '2026-07-10T09:10:00Z',
    },
  ];

  const billingApi = {
    listCashierCollectionQueue: vi.fn(() => of(items)),
    addPayment: vi.fn(() => of({
      id: 'payment-1',
      invoiceId: 'invoice-due',
      amount: 20000,
      paymentMethod: 'CASH',
      receivedByUserId: 'cashier-1',
      createdAt: '2026-07-10T10:00:00Z',
    })),
    getPaymentReceipt: vi.fn(() => of({
      id: 'receipt-1',
      paymentId: 'payment-1',
      receiptNumber: 'REC-20260710-000001',
      amount: 20000,
      paymentMethod: 'CASH',
      createdAt: '2026-07-10T10:00:01Z',
    })),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    billingApi.listCashierCollectionQueue.mockReturnValue(of(items));
    billingApi.addPayment.mockReturnValue(of({
      id: 'payment-1',
      invoiceId: 'invoice-due',
      amount: 20000,
      paymentMethod: 'CASH',
      receivedByUserId: 'cashier-1',
      createdAt: '2026-07-10T10:00:00Z',
    }));
    billingApi.getPaymentReceipt.mockReturnValue(of({
      id: 'receipt-1',
      paymentId: 'payment-1',
      receiptNumber: 'REC-20260710-000001',
      amount: 20000,
      paymentMethod: 'CASH',
      createdAt: '2026-07-10T10:00:01Z',
    }));

    await TestBed.configureTestingModule({
      imports: [BillingCashierQueueComponent],
      providers: [
        { provide: BillingApiService, useValue: billingApi },
        { provide: I18nService, useValue: { t: (_key: string, fallback: string) => fallback } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BillingCashierQueueComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads and renders the backend queue without recomputing patient balances', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(billingApi.listCashierCollectionQueue).toHaveBeenCalledOnce();
    expect(component.totalRemaining()).toBe(25000);
    expect(component.partialCount()).toBe(1);
    expect(text).toContain('Alice Mbarga');
    expect(text).toContain('20 000');
    expect(text).toContain('Brice Nkoa');
    expect(text).toContain('Paiement partiel');
  });

  it('filters by accent-insensitive patient search and collection status', () => {
    component.searchQuery.set('alice');
    expect(component.filteredItems().map((item) => item.invoiceId)).toEqual(['invoice-due']);

    component.searchQuery.set('');
    component.statusFilter.set('PATIENT_PARTIALLY_PAID');
    expect(component.filteredItems().map((item) => item.invoiceId)).toEqual(['invoice-partial']);
  });

  it('shows a retry state when the queue request fails', () => {
    billingApi.listCashierCollectionQueue.mockReturnValueOnce(throwError(() => new Error('network')));

    component.loadQueue();
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    expect(component.error()).toBe(true);
    expect(host.querySelector('[role="alert"]')?.textContent).toContain('Impossible de charger');
  });

  it('records the payment, loads the receipt and refreshes the queue', () => {
    component.selectedItem.set(items[0]);

    component.submitPayment({ amount: 20000, method: 'CASH', reference: '' });
    fixture.detectChanges();

    expect(billingApi.addPayment).toHaveBeenCalledWith('invoice-due', 20000, 'CASH', undefined);
    expect(billingApi.getPaymentReceipt).toHaveBeenCalledWith('payment-1');
    expect(billingApi.listCashierCollectionQueue).toHaveBeenCalledTimes(2);
    expect(component.lastPayment()?.receipt?.receiptNumber).toBe('REC-20260710-000001');
    expect(component.selectedItem()).toBeNull();
  });

  it('blocks an amount above the patient balance before calling the API', () => {
    component.selectedItem.set(items[1]);

    component.submitPayment({ amount: 6000, method: 'CASH', reference: '' });

    expect(billingApi.addPayment).not.toHaveBeenCalled();
    expect(component.paymentError()).toContain('ne pas dépasser');
  });

  it('requires a reference for cheque and bank transfer payments', () => {
    component.selectedItem.set(items[0]);

    component.submitPayment({ amount: 10000, method: 'CHECK', reference: '  ' });

    expect(billingApi.addPayment).not.toHaveBeenCalled();
    expect(component.paymentError()).toContain('référence est obligatoire');
  });
});
