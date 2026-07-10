import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { BillingApiService } from '../../patient/billing-api.service';
import { CashSession } from '../../patient/patient.models';
import { BillingCashRegisterComponent } from './billing-cash-register.component';

describe('BillingCashRegisterComponent', () => {
  let fixture: ComponentFixture<BillingCashRegisterComponent>;
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

    await TestBed.configureTestingModule({
      imports: [BillingCashRegisterComponent],
      providers: [
        { provide: BillingApiService, useValue: billingApi },
        { provide: I18nService, useValue: { t: (_key: string, fallback: string) => fallback } },
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
});
