import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AuthTokenStorageService } from '../../auth/auth-token-storage.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { BillingApiService } from '../../patient/billing-api.service';
import { InsuranceBordereau } from '../../patient/insurance-bordereau.models';
import { BillingInsuranceBordereauxComponent } from './billing-insurance-bordereaux.component';

const bordereaux: InsuranceBordereau[] = [
  {
    id: 'bord-1',
    bordereauNumber: 'BORD-20260710-000001',
    insuranceConventionId: 'conv-1',
    insuranceConventionName: 'SAAR Assurance',
    startDate: '2026-07-01',
    endDate: '2026-07-10',
    totalAmount: 100000,
    acceptedAmount: 95000,
    paidAmount: 40000,
    remainingAmount: 60000,
    disputedAmount: 5000,
    insurerReference: 'AR-SAAR-01',
    status: 'PARTIALLY_PAID',
    createdAt: new Date().toISOString(),
  },
  {
    id: 'bord-2',
    bordereauNumber: 'BORD-20260710-000002',
    insuranceConventionId: 'conv-2',
    insuranceConventionName: 'ACTIVA',
    startDate: '2026-06-01',
    endDate: '2026-06-30',
    totalAmount: 50000,
    acceptedAmount: 50000,
    paidAmount: 50000,
    remainingAmount: 0,
    disputedAmount: 0,
    status: 'SETTLED',
    createdAt: new Date().toISOString(),
  },
];

describe('BillingInsuranceBordereauxComponent', () => {
  let fixture: ComponentFixture<BillingInsuranceBordereauxComponent>;
  let component: BillingInsuranceBordereauxComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BillingInsuranceBordereauxComponent],
      providers: [
        {
          provide: BillingApiService,
          useValue: {
            listConventions: vi.fn().mockReturnValue(of([
              { id: 'conv-1', name: 'SAAR Assurance', coveragePercentage: 0.8 },
              { id: 'conv-2', name: 'ACTIVA', coveragePercentage: 0.7 },
            ])),
            listInsuranceBordereaux: vi.fn().mockReturnValue(of(bordereaux)),
            getInsuranceBordereauDetails: vi.fn().mockReturnValue(of({ ...bordereaux[0], invoices: [] })),
          },
        },
        {
          provide: AuthTokenStorageService,
          useValue: { session: vi.fn().mockReturnValue({ role: 'DAF' }) },
        },
        {
          provide: I18nService,
          useValue: { t: (key: string) => key },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BillingInsuranceBordereauxComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('renders financial KPIs and responsive bordereau cards', () => {
    const root = fixture.nativeElement as HTMLElement;
    const cards = root.querySelectorAll('.bordereau-card');

    expect(cards.length).toBe(2);
    expect(component.claimedTotal()).toBe(150000);
    expect(component.paidTotal()).toBe(90000);
    expect(component.remainingTotal()).toBe(60000);
    expect(root.querySelector('.kpi-grid')).not.toBeNull();
  });

  it('filters bordereaux by status and insurer reference', () => {
    component.statusFilter.set('PARTIALLY_PAID');
    expect(component.filteredBordereaux().map((item) => item.id)).toEqual(['bord-1']);

    component.statusFilter.set('ALL');
    component.searchQuery.set('AR-SAAR');
    expect(component.filteredBordereaux().map((item) => item.id)).toEqual(['bord-1']);
  });

  it('prepares a partial payment without allowing an overpayment', () => {
    component.openAction(bordereaux[0], 'pay');

    expect(component.paymentAmount()).toBe(60000);
    expect(component.actionValid()).toBe(false);

    component.paymentReference.set('VIR-2026-01');
    expect(component.actionValid()).toBe(true);

    component.paymentAmount.set(60001);
    expect(component.actionValid()).toBe(false);
  });
});
