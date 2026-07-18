import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { BillingApiService } from '../../patient/billing-api.service';
import { CashSession } from '../../patient/patient.models';
import { BillingCashRegisterComponent } from './billing-cash-register.component';
import { RbacApiService } from '../rbac/rbac-api.service';

describe('BillingCashRegisterComponent', () => {
  let fixture: ComponentFixture<BillingCashRegisterComponent>;
  let permissions: Set<string>;
  const cashSessionClosedSubject = new Subject<CashSession>();

  const billingApi = {
    getActiveCashSession: vi.fn(() => of(null)),
    listCashRegisters: vi.fn(() => of([])),
    listMyCashSessions: vi.fn(() => of([])),
    getSessionMovements: vi.fn(() => of([])),
    getActiveCashSessionSummary: vi.fn(),
    openCashSession: vi.fn(),
    addCashMovement: vi.fn(),
    closeCashSession: vi.fn(),
    downloadCashCloseoutReport: vi.fn(),
    cashSessionClosed$: cashSessionClosedSubject.asObservable(),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    permissions = new Set([
      'CASH_QUEUE_READ',
      'CASH_PAYMENT_COLLECT',
      'CASH_SESSION_OPEN',
      'CASH_SESSION_CLOSE',
      'CASH_MOVEMENT_WRITE',
      'CASH_HISTORY_READ',
    ]);

    await TestBed.configureTestingModule({
      imports: [BillingCashRegisterComponent],
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

    fixture = TestBed.createComponent(BillingCashRegisterComponent);
    fixture.detectChanges();
  });

  it('renders the session history inside the shared cash register workspace', () => {
    const host = fixture.nativeElement as HTMLElement;

    expect(host.querySelector('app-billing-cash-session-history')).not.toBeNull();
    expect(host.textContent).toContain('Historique des sessions');
    expect(billingApi.listMyCashSessions).toHaveBeenCalledOnce();
  });

  it('renders a full-width responsive opening workspace when no session is active', () => {
    const host = fixture.nativeElement as HTMLElement;
    const panel = host.querySelector<HTMLElement>('[data-testid="cash-open-panel"]');

    expect(panel).not.toBeNull();
    expect(panel?.classList.contains('max-w-md')).toBe(false);
    expect(panel?.querySelector('form')).not.toBeNull();
    expect(panel?.textContent).toContain('Paramètres d’ouverture');
  });

  it('does not call or render cash features outside the granted dynamic permission', () => {
    permissions = new Set(['CASH_PAYMENT_COLLECT']);
    billingApi.getActiveCashSession.mockClear();
    billingApi.listCashRegisters.mockClear();
    billingApi.listMyCashSessions.mockClear();

    const restrictedFixture = TestBed.createComponent(BillingCashRegisterComponent);
    restrictedFixture.detectChanges();
    const host = restrictedFixture.nativeElement as HTMLElement;

    expect(billingApi.getActiveCashSession).toHaveBeenCalledOnce();
    expect(billingApi.listCashRegisters).not.toHaveBeenCalled();
    expect(billingApi.listMyCashSessions).not.toHaveBeenCalled();
    expect(host.querySelector('[data-testid="cash-open-panel"]')).toBeNull();
    expect(host.querySelector('app-billing-cash-session-history')).toBeNull();
  });
});
