import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { firstValueFrom, Observable, of } from 'rxjs';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { AuthSession } from './auth.models';
import { AuthSessionRecoveryService } from './auth-session-recovery.service';
import { AuthTokenStorageService } from './auth-token-storage.service';
import { roleGuard } from './role.guard';

describe('roleGuard', () => {
  let mockRouter: { parseUrl: ReturnType<typeof vi.fn>; createUrlTree: ReturnType<typeof vi.fn> };
  let mockTokenStorage: {
    session: ReturnType<typeof signal<AuthSession | null>>;
    isPatientSession: ReturnType<typeof vi.fn>;
  };
  let mockRecovery: { refreshAccessToken: ReturnType<typeof vi.fn> };
  let mockRbacApi: { ensureMyAccess: ReturnType<typeof vi.fn> };
  let sessionSignal: ReturnType<typeof signal<AuthSession | null>>;

  beforeEach(() => {
    sessionSignal = signal<AuthSession | null>(null);
    mockRouter = {
      parseUrl: vi.fn((url: string) => url),
      createUrlTree: vi.fn((_commands: unknown[], options: { queryParams: Record<string, string> }) => {
        const params = new URLSearchParams(options.queryParams).toString();
        return `/?${params}`;
      }),
    };
    mockTokenStorage = {
      session: sessionSignal,
      isPatientSession: vi.fn((value?: AuthSession | null) => {
        const current = value ?? sessionSignal();
        return current?.role.split(',').map(role => role.trim()).includes('PATIENT') ?? false;
      }),
    };
    mockRecovery = { refreshAccessToken: vi.fn() };
    mockRbacApi = { ensureMyAccess: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: mockRouter },
        { provide: AuthTokenStorageService, useValue: mockTokenStorage },
        { provide: AuthSessionRecoveryService, useValue: mockRecovery },
        { provide: RbacApiService, useValue: mockRbacApi },
      ],
    });
  });

  it('should recover a professional consultation route from the persistent session before redirecting', async () => {
    sessionSignal.set(null);
    mockRecovery.refreshAccessToken.mockImplementation(() => {
      sessionSignal.set(session('MEDECIN'));
      return of('restored-token');
    });

    const result$ = TestBed.runInInjectionContext(() =>
      roleGuard(
        { data: { expectedRoles: [] } } as any,
        { url: '/clinic/consultation/visit-123' } as any,
      ),
    ) as Observable<boolean | string>;

    expect(await firstValueFrom(result$)).toBe(true);
    expect(mockRecovery.refreshAccessToken).toHaveBeenCalledOnce();
    expect(mockRouter.createUrlTree).not.toHaveBeenCalled();
  });

  it('should send a patient route without a session to patient login without professional refresh', () => {
    sessionSignal.set(null);

    const result = TestBed.runInInjectionContext(() => roleGuard({
      routeConfig: { path: 'patient/dashboard' },
      data: { expectedRoles: ['PATIENT'] },
    } as any, { url: '/patient/dashboard' } as any));

    expect(result).toBe('/?mode=patient&returnUrl=%2Fpatient%2Fdashboard');
    expect(mockRecovery.refreshAccessToken).not.toHaveBeenCalled();
  });

  it('should not reuse a professional session to authorize a patient route', () => {
    sessionSignal.set(session('MEDECIN'));

    const result = TestBed.runInInjectionContext(() => roleGuard({
      routeConfig: { path: 'patient/dashboard' },
      data: { expectedRoles: ['PATIENT'] },
    } as any, { url: '/patient/dashboard' } as any));

    expect(result).toBe('/?mode=patient&returnUrl=%2Fpatient%2Fdashboard');
    expect(mockRbacApi.ensureMyAccess).not.toHaveBeenCalled();
  });

  it('should reject a professional route configured with roles only', () => {
    sessionSignal.set(session('MEDECIN'));
    mockRbacApi.ensureMyAccess.mockReturnValue(of({
      userId: 'user-legacy-1', roles: ['MEDECIN'], permissions: [],
    }));
    const result = TestBed.runInInjectionContext(() =>
      roleGuard({ data: { expectedRoles: ['ADMIN_JOPRELYS'] } } as any, {} as any),
    );
    expect(result).toBe('/unauthorized');
    expect(mockRouter.parseUrl).toHaveBeenCalledWith('/unauthorized');
    expect(mockRbacApi.ensureMyAccess).not.toHaveBeenCalled();
  });

  it('should not let an effective system role bypass missing permission metadata', () => {
    sessionSignal.set(session('ADMIN_JOPRELYS'));
    mockRbacApi.ensureMyAccess.mockReturnValue(of({
      userId: 'user-legacy-2', roles: ['ADMIN_JOPRELYS'], permissions: [],
    }));
    const result = TestBed.runInInjectionContext(() =>
      roleGuard({ data: { expectedRoles: ['ADMIN_JOPRELYS'] } } as any, {} as any),
    );
    expect(result).toBe('/unauthorized');
    expect(mockRouter.parseUrl).toHaveBeenCalledWith('/unauthorized');
    expect(mockRbacApi.ensureMyAccess).not.toHaveBeenCalled();
  });

  it('should deny a permission route when a stale JWT role lost its effective access', async () => {
    sessionSignal.set(session('CAISSIER'));
    mockRbacApi.ensureMyAccess.mockReturnValue(of({
      userId: 'user-1', roles: ['AGENT_ACCUEIL'], permissions: ['PATIENT_READ'],
    }));
    const result$ = TestBed.runInInjectionContext(() => roleGuard({
      data: { expectedRoles: ['CAISSIER'], expectedPermissions: ['CASH_QUEUE_READ', 'CASH_PAYMENT_COLLECT'] },
    } as any, {} as any)) as Observable<boolean | string>;
    expect(await firstValueFrom(result$)).toBe('/unauthorized');
    expect(mockRbacApi.ensureMyAccess).toHaveBeenCalledWith(true);
  });

  it('should allow a custom role when an effective permission matches', async () => {
    sessionSignal.set(session('CAISSE_SUPERVISEUR'));
    mockRbacApi.ensureMyAccess.mockReturnValue(of({
      userId: 'user-2', roles: ['CAISSE_SUPERVISEUR'], permissions: ['CASH_QUEUE_READ'],
    }));
    const result$ = TestBed.runInInjectionContext(() => roleGuard({
      data: { expectedRoles: ['CAISSIER'], expectedPermissions: ['CASH_QUEUE_READ', 'CASH_PAYMENT_COLLECT'] },
    } as any, {} as any)) as Observable<boolean | string>;
    expect(await firstValueFrom(result$)).toBe(true);
  });

  it('should deny a permission route when only the effective role matches', async () => {
    sessionSignal.set(session('OLD_ROLE'));
    mockRbacApi.ensureMyAccess.mockReturnValue(of({
      userId: 'user-3', roles: ['ADMIN_CLINIQUE'], permissions: [],
    }));
    const result$ = TestBed.runInInjectionContext(() => roleGuard({
      data: { expectedRoles: ['ADMIN_CLINIQUE'], expectedPermissions: ['RBAC_READ'] },
    } as any, {} as any)) as Observable<boolean | string>;
    expect(await firstValueFrom(result$)).toBe('/unauthorized');
  });

  it('should allow a custom internal role to enter the dashboard', async () => {
    sessionSignal.set(session('ROLE_PERSONNALISE'));
    mockRbacApi.ensureMyAccess.mockReturnValue(of({
      userId: 'user-4', roles: ['ROLE_PERSONNALISE'], permissions: ['AUDIT_READ'],
    }));
    const result$ = TestBed.runInInjectionContext(() => roleGuard({
      routeConfig: { path: 'dashboard' },
      data: { expectedRoles: ['ADMIN_CLINIQUE', 'CAISSIER'] },
    } as any, {} as any)) as Observable<boolean | string>;
    expect(await firstValueFrom(result$)).toBe(true);
    expect(mockRbacApi.ensureMyAccess).toHaveBeenCalledWith(true);
  });

  it('should not treat a patient role as an internal dashboard role', () => {
    sessionSignal.set(session('PATIENT'));
    mockRbacApi.ensureMyAccess.mockReturnValue(of({
      userId: 'patient-1', roles: ['PATIENT'], permissions: [],
    }));
    const result = TestBed.runInInjectionContext(() => roleGuard({
      routeConfig: { path: 'dashboard' },
      data: { expectedRoles: ['ADMIN_CLINIQUE', 'CAISSIER'] },
    } as any, {} as any));
    expect(result).toBe('/unauthorized');
    expect(mockRbacApi.ensureMyAccess).not.toHaveBeenCalled();
  });

  it('should deny a patient session on a professional route before consulting stale RBAC access', () => {
    sessionSignal.set(session('PATIENT'));
    mockRbacApi.ensureMyAccess.mockReturnValue(of({
      userId: 'doctor-1', roles: ['MEDECIN'], permissions: ['AVAILABILITY_MANAGE'],
    }));

    const result = TestBed.runInInjectionContext(() => roleGuard({
      routeConfig: { path: 'clinic/availability' },
      data: { expectedRoles: ['MEDECIN'], expectedPermissions: ['AVAILABILITY_MANAGE'] },
    } as any, {} as any));

    expect(result).toBe('/unauthorized');
    expect(mockRbacApi.ensureMyAccess).not.toHaveBeenCalled();
  });

  it('should treat a mixed patient role as patient-only and allow patient routes', () => {
    sessionSignal.set(session('PATIENT,MEDECIN'));

    const patientResult = TestBed.runInInjectionContext(() => roleGuard({
      routeConfig: { path: 'patient/appointments' },
      data: { expectedRoles: ['PATIENT'] },
    } as any, {} as any));
    const professionalResult = TestBed.runInInjectionContext(() => roleGuard({
      routeConfig: { path: 'clinic/availability' },
      data: { expectedRoles: ['MEDECIN'], expectedPermissions: ['AVAILABILITY_MANAGE'] },
    } as any, {} as any));

    expect(patientResult).toBe(true);
    expect(professionalResult).toBe('/unauthorized');
    expect(mockRbacApi.ensureMyAccess).not.toHaveBeenCalled();
  });

  it('should deny billing management to a cashier-only profile', async () => {
    sessionSignal.set(session('CAISSIER'));
    mockRbacApi.ensureMyAccess.mockReturnValue(of({
      userId: 'cashier-1',
      roles: ['CAISSIER'],
      permissions: [
        'BILLING_INVOICE_READ',
        'CASH_QUEUE_READ',
        'CASH_PAYMENT_COLLECT',
        'CASH_SESSION_OPEN',
        'CASH_SESSION_CLOSE',
        'CASH_MOVEMENT_WRITE',
        'CASH_HISTORY_READ',
      ],
    }));

    const result$ = TestBed.runInInjectionContext(() => roleGuard({
      routeConfig: { path: 'clinic/billing' },
      data: { expectedPermissions: ['BILLING_INVOICE_READ'] },
    } as any, {} as any)) as Observable<boolean | string>;

    expect(await firstValueFrom(result$)).toBe('/unauthorized');
  });

  it('should allow billing management when invoice and patient access are both granted', async () => {
    sessionSignal.set(session('ROLE_FACTURATION'));
    mockRbacApi.ensureMyAccess.mockReturnValue(of({
      userId: 'billing-1',
      roles: ['ROLE_FACTURATION'],
      permissions: ['BILLING_INVOICE_READ', 'PATIENT_READ'],
    }));

    const result$ = TestBed.runInInjectionContext(() => roleGuard({
      routeConfig: { path: 'clinic/billing' },
      data: { expectedPermissions: ['BILLING_INVOICE_READ'] },
    } as any, {} as any)) as Observable<boolean | string>;

    expect(await firstValueFrom(result$)).toBe(true);
  });

  function session(role: string): AuthSession {
    return {
      accessToken: 'token',
      expiresAt: '2999-07-02T12:00:00Z',
      email: 'user@joprelys.local',
      name: 'User',
      role,
    };
  }
});
