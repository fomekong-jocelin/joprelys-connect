import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { BillingApiService } from '../../patient/billing-api.service';
import { PatientApiService } from '../../patient/patient-api.service';
import { VisitApiService } from '../../visit/visit-api.service';
import { RbacApiService } from '../rbac/rbac-api.service';
import { BillingManagementPageComponent } from './billing-management-page.component';

describe('BillingManagementPageComponent dynamic permissions', () => {
  let fixture: ComponentFixture<BillingManagementPageComponent>;
  let permissions: Set<string>;

  const billingApi = {
    listConventions: vi.fn(() => of([])),
    listTariffs: vi.fn(() => of([])),
    getActiveCashSession: vi.fn(() => of(null)),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    permissions = new Set();

    await TestBed.configureTestingModule({
      imports: [BillingManagementPageComponent],
      providers: [
        provideRouter([]),
        { provide: BillingApiService, useValue: billingApi },
        { provide: PatientApiService, useValue: {} },
        { provide: VisitApiService, useValue: {} },
        { provide: I18nService, useValue: { t: (key: string) => key } },
        {
          provide: RbacApiService,
          useValue: {
            access: () => ({
              userId: 'dynamic-user',
              roles: ['ROLE_PERSONNALISE'],
              permissions: [...permissions],
            }),
            hasPermission: (permission: string) => permissions.has(permission),
          },
        },
      ],
    }).compileComponents();
  });

  afterEach(() => fixture?.destroy());

  it('should initialize a cash-history-only profile without calling billing or collection APIs', () => {
    permissions = new Set(['CASH_HISTORY_READ']);
    fixture = TestBed.createComponent(BillingManagementPageComponent);

    fixture.componentInstance.ngOnInit();

    expect(fixture.componentInstance.activeTab()).toBe('caisse');
    expect(billingApi.listConventions).not.toHaveBeenCalled();
    expect(billingApi.listTariffs).not.toHaveBeenCalled();
    expect(billingApi.getActiveCashSession).not.toHaveBeenCalled();
  });

  it('should load only billing configuration for an invoice reader with patient access', () => {
    permissions = new Set(['BILLING_INVOICE_READ', 'PATIENT_READ']);
    fixture = TestBed.createComponent(BillingManagementPageComponent);

    fixture.componentInstance.ngOnInit();

    expect(fixture.componentInstance.activeTab()).toBe('facturation');
    expect(billingApi.listConventions).toHaveBeenCalledOnce();
    expect(billingApi.listTariffs).toHaveBeenCalledOnce();
    expect(billingApi.getActiveCashSession).not.toHaveBeenCalled();
  });

  it('should check the active cash session only for a payment collector', () => {
    permissions = new Set(['CASH_PAYMENT_COLLECT']);
    fixture = TestBed.createComponent(BillingManagementPageComponent);

    fixture.componentInstance.ngOnInit();

    expect(billingApi.getActiveCashSession).toHaveBeenCalledOnce();
    expect(billingApi.listConventions).not.toHaveBeenCalled();
    expect(billingApi.listTariffs).not.toHaveBeenCalled();
  });
});
