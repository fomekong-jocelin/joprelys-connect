import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { BillingApiService } from '../../patient/billing-api.service';
import { Receivable } from '../../patient/patient.models';
import { RbacApiService } from '../rbac/rbac-api.service';
import { BillingReceivablesComponent } from './billing-receivables.component';

const receivable: Receivable = {
  id: 'receivable-1',
  invoiceId: 'invoice-1',
  debtorType: 'PATIENT',
  debtorId: 'patient-1',
  totalAmount: 10000,
  paidAmount: 0,
  remainingAmount: 10000,
  status: 'UNPAID',
  createdAt: '2026-07-18T10:00:00Z',
  agingSlice: '0_30',
};

describe('BillingReceivablesComponent dynamic permissions', () => {
  let fixture: ComponentFixture<BillingReceivablesComponent>;
  let permissions: Set<string>;

  const billingApi = {
    getReceivablesByStatus: vi.fn(() => of([receivable])),
    recordReminder: vi.fn(() => of(null)),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    permissions = new Set(['BILLING_INVOICE_READ']);
    await TestBed.configureTestingModule({
      imports: [BillingReceivablesComponent],
      providers: [
        { provide: BillingApiService, useValue: billingApi },
        { provide: I18nService, useValue: { t: (_key: string, fallback: string) => fallback } },
        {
          provide: RbacApiService,
          useValue: {
            hasPermission: (permission: string) => permissions.has(permission),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BillingReceivablesComponent);
  });

  afterEach(() => fixture.destroy());

  it('should keep a billing reader in read-only mode without reminder API calls', () => {
    fixture.detectChanges();
    const component = fixture.componentInstance;

    component.openReminderModal(receivable);
    component.onReminderSubmitted({ actionType: 'PHONE_CALL' });

    expect(billingApi.getReceivablesByStatus).toHaveBeenCalledOnce();
    expect(billingApi.recordReminder).not.toHaveBeenCalled();
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Relancer');
  });
});
