import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { BillingApiService } from '../../patient/billing-api.service';
import { CashSessionHistory } from '../../patient/cash-session-history.models';
import { CashSession } from '../../patient/patient.models';
import { BillingCashSessionHistoryComponent } from './billing-cash-session-history.component';

describe('BillingCashSessionHistoryComponent', () => {
  let fixture: ComponentFixture<BillingCashSessionHistoryComponent>;
  let component: BillingCashSessionHistoryComponent;
  let closeEvents: Subject<CashSession>;

  const history: CashSessionHistory = {
    id: 'session-1',
    cashRegisterId: 'register-1',
    cashRegisterName: 'Caisse principale',
    reportNumber: 'CLS-20260710-ABCDEF12',
    openedByUserId: 'cashier-1',
    openedByName: 'Pierre Caisse',
    openedAt: '2026-07-10T08:00:00Z',
    closedByUserId: 'cashier-1',
    closedByName: 'Pierre Caisse',
    closedAt: '2026-07-10T18:00:00Z',
    status: 'CLOSED',
    openingBalance: 50000,
    cashReceipts: 20000,
    chequeReceipts: 10000,
    transferReceipts: 5000,
    cashExpenses: 3000,
    bankDeposits: 2000,
    expectedCash: 65000,
    declaredBalance: 65000,
    discrepancyAmount: 0,
    discrepancyResolved: false,
  };

  const closedSession: CashSession = {
    id: 'session-1',
    cashRegisterId: 'register-1',
    cashRegisterName: 'Caisse principale',
    openedByUserId: 'cashier-1',
    openedAt: '2026-07-10T08:00:00Z',
    openingBalance: 50000,
    closedByUserId: 'cashier-1',
    closedAt: '2026-07-10T18:00:00Z',
    closingBalance: 65000,
    declaredBalance: 65000,
    discrepancyAmount: 0,
    status: 'CLOSED',
    discrepancyResolved: false,
  };

  const movement = {
    id: 'movement-1',
    cashRegisterSessionId: 'session-1',
    movementType: 'IN' as const,
    amount: 20000,
    description: 'Règlement facture',
    paymentMethod: 'CASH' as const,
    createdByUserId: 'cashier-1',
    createdAt: '2026-07-10T10:00:00Z',
  };

  const billingApi = {
    cashSessionClosed$: undefined as unknown,
    listMyCashSessions: vi.fn(),
    getSessionMovements: vi.fn(),
    downloadCashCloseoutReport: vi.fn(),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    closeEvents = new Subject<CashSession>();
    billingApi.cashSessionClosed$ = closeEvents.asObservable();
    billingApi.listMyCashSessions.mockReturnValue(of([history]));
    billingApi.getSessionMovements.mockReturnValue(of([movement]));
    billingApi.downloadCashCloseoutReport.mockReturnValue(of(new Blob(['%PDF-test'], { type: 'application/pdf' })));

    await TestBed.configureTestingModule({
      imports: [BillingCashSessionHistoryComponent],
      providers: [
        { provide: BillingApiService, useValue: billingApi },
        { provide: I18nService, useValue: { t: (_key: string, fallback: string) => fallback } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BillingCashSessionHistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads and renders the current cashier session history', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(billingApi.listMyCashSessions).toHaveBeenCalledOnce();
    expect(component.sessions()).toEqual([history]);
    expect(text).toContain('Caisse principale');
    expect(text).toContain('CLS-20260710-ABCDEF12');
    expect(text).toContain('65');
  });

  it('refreshes and highlights the session immediately after a successful close', () => {
    closeEvents.next(closedSession);
    fixture.detectChanges();

    expect(billingApi.listMyCashSessions).toHaveBeenCalledTimes(2);
    expect(component.highlightedSessionId()).toBe('session-1');
    expect(component.expandedSessionId()).toBe('session-1');
    expect(billingApi.getSessionMovements).toHaveBeenCalledWith('session-1');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('vient d’être clôturée');
  });

  it('loads the session movements only when details are opened', () => {
    component.toggleDetails(history);
    fixture.detectChanges();

    expect(billingApi.getSessionMovements).toHaveBeenCalledWith('session-1');
    expect(component.movementsFor('session-1')).toEqual([movement]);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Règlement facture');
  });

  it('shows an accessible retry state when history loading fails', () => {
    billingApi.listMyCashSessions.mockReturnValueOnce(throwError(() => new Error('network')));

    component.loadSessions();
    fixture.detectChanges();

    expect(component.error()).toBe(true);
    expect((fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent)
      .toContain('Impossible de charger');
  });
});