import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { firstValueFrom, Observable, of } from 'rxjs';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { AuthSession } from './auth.models';
import { AuthTokenStorageService } from './auth-token-storage.service';
import { roleGuard } from './role.guard';

describe('roleGuard', () => {
  let mockRouter: { parseUrl: ReturnType<typeof vi.fn>; createUrlTree: ReturnType<typeof vi.fn> };
  let mockTokenStorage: { session: ReturnType<typeof signal<AuthSession | null>> };
  let mockRbacApi: { ensureMyAccess: ReturnType<typeof vi.fn> };
  let sessionSignal: ReturnType<typeof signal<AuthSession | null>>;

  beforeEach(() => {
    sessionSignal = signal<AuthSession | null>(null);
    mockRouter = {
      parseUrl: vi.fn((url: string) => url),
      createUrlTree: vi.fn((_commands: unknown[], options: { queryParams: { returnUrl: string } }) =>
        `/?returnUrl=${encodeURIComponent(options.queryParams.returnUrl)}`),
    };
    mockTokenStorage = { session: sessionSignal };
    mockRbacApi = { ensureMyAccess: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: mockRouter },
        { provide: AuthTokenStorageService, useValue: mockTokenStorage },
        { provide: RbacApiService, useValue: mockRbacApi },
      ],
    });
  });

  it('should preserve the scanned consultation route when no session exists', () => {
    sessionSignal.set(null);

    const result = TestBed.runInInjectionContext(() =>
      roleGuard(
        { data: { expectedRoles: ['MEDECIN'] } } as any,
        { url: '/clinic/consultation/visit-123' } as any,
      ),
    );

    expect(result).toBe('/?returnUrl=%2Fclinic%2Fconsultation%2Fvisit-123');
    expect(mockRouter.createUrlTree).toHaveBeenCalledWith(['/'], {
      queryParams: { returnUrl: '/clinic/consultation/visit-123' },
    });
  });

  it('should redirect to unauthorized if a legacy-only route does not match the role', () => {
    sessionSignal.set(session('MEDECIN'));
    const result = TestBed.runInInjectionContext(() =>
      roleGuard({ data: { expectedRoles: ['ADMIN_JOPRELYS'] } } as any, {} as any),
    );
    expect(result).toBe('/unauthorized');
    expect(mockRouter.parseUrl).toHaveBeenCalledWith('/unauthorized');
  });

  it('should allow a legacy-only route when the role matches', () => {
    sessionSignal.set(session('ADMIN_JOPRELYS'));
    const result = TestBed.runInInjectionContext(() =>
      roleGuard({ data: { expectedRoles: ['ADMIN_JOPRELYS'] } } as any, {} as any),
    );
    expect(result).toBe(true);
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

  it('should allow a route when the effective role matches even without the expected permission', async () => {
    sessionSignal.set(session('OLD_ROLE'));
    mockRbacApi.ensureMyAccess.mockReturnValue(of({
      userId: 'user-3', roles: ['ADMIN_CLINIQUE'], permissions: [],
    }));
    const result$ = TestBed.runInInjectionContext(() => roleGuard({
      data: { expectedRoles: ['ADMIN_CLINIQUE'], expectedPermissions: ['RBAC_READ'] },
    } as any, {} as any)) as Observable<boolean | string>;
    expect(await firstValueFrom(result$)).toBe(true);
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

  it('should not treat a patient role as an internal dashboard role', async () => {
    sessionSignal.set(session('PATIENT'));
    mockRbacApi.ensureMyAccess.mockReturnValue(of({
      userId: 'patient-1', roles: ['PATIENT'], permissions: [],
    }));
    const result$ = TestBed.runInInjectionContext(() => roleGuard({
      routeConfig: { path: 'dashboard' },
      data: { expectedRoles: ['ADMIN_CLINIQUE', 'CAISSIER'] },
    } as any, {} as any)) as Observable<boolean | string>;
    expect(await firstValueFrom(result$)).toBe('/unauthorized');
  });

  function session(role: string): AuthSession {
    return {
      accessToken: 'token',
      expiresAt: '2026-07-02T12:00:00Z',
      email: 'user@joprelys.local',
      name: 'User',
      role,
    };
  }
});
