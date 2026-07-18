import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { BillingApiService } from '../../patient/billing-api.service';
import { RbacApiService } from '../rbac/rbac-api.service';
import { BillingDafDashboardComponent } from './billing-daf-dashboard.component';

describe('BillingDafDashboardComponent dynamic permissions', () => {
  let fixture: ComponentFixture<BillingDafDashboardComponent>;
  let permissions: Set<string>;

  const billingApi = {
    listAllSessions: vi.fn(() => of([])),
    exportAccounting: vi.fn(() => of(new Blob())),
    resolveDiscrepancy: vi.fn(() => of(null)),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    permissions = new Set();
    await TestBed.configureTestingModule({
      imports: [BillingDafDashboardComponent],
      providers: [
        { provide: BillingApiService, useValue: billingApi },
        {
          provide: RbacApiService,
          useValue: {
            hasPermission: (permission: string) => permissions.has(permission),
          },
        },
      ],
    }).compileComponents();
  });

  afterEach(() => fixture?.destroy());

  it('should not call cash-history APIs from accounting dashboard read alone', () => {
    permissions = new Set(['ACCOUNTING_DASHBOARD_READ']);
    fixture = TestBed.createComponent(BillingDafDashboardComponent);

    fixture.componentInstance.ngOnInit();
    fixture.componentInstance.exportSage100();

    expect(billingApi.listAllSessions).not.toHaveBeenCalled();
    expect(billingApi.exportAccounting).not.toHaveBeenCalled();
  });

  it('should load and export only when the exact permissions are granted', () => {
    permissions = new Set(['ACCOUNTING_DASHBOARD_READ', 'CASH_HISTORY_READ', 'ACCOUNTING_EXPORT']);
    fixture = TestBed.createComponent(BillingDafDashboardComponent);

    fixture.componentInstance.ngOnInit();

    expect(billingApi.listAllSessions).toHaveBeenCalledOnce();
    expect(fixture.componentInstance.canExport()).toBe(true);
  });
});
