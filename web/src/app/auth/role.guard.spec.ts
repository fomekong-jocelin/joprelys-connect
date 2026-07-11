import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { firstValueFrom, Observable, of, throwError } from 'rxjs';
import { AuthApiService } from './auth-api.service';
import { AuthSession } from './auth.models';
import { AuthTokenStorageService } from './auth-token-storage.service';
import { roleGuard } from './role.guard';

describe('roleGuard', () => {
  let mockRouter: { parseUrl: ReturnType<typeof vi.fn> };
  let mockTokenStorage: {
    session: ReturnType<typeof signal<AuthSession | null>>;
    clear: ReturnType<typeof vi.fn>;
  };
  let mockAuthApi: { refreshCurrentSession: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    mockRouter = {
      parseUrl: vi.fn((url: string) => url),
    };
    mockTokenStorage = {
      session: signal<AuthSession | null>(null),
      clear: vi.fn(),
    };
    mockAuthApi = {
      refreshCurrentSession: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: mockRouter },
        { provide: AuthTokenStorageService, useValue: mockTokenStorage },
        { provide: AuthApiService, useValue: mockAuthApi },
      ],
    });
  });

  it('should redirect to root if no session exists', () => {
    const result = TestBed.runInInjectionContext(() =>
      roleGuard({ data: { expectedRoles: ['ADMIN_JOPRELYS'] } } as never, {} as never),
    );

    expect(result).toBe('/');
    expect(mockRouter.parseUrl).toHaveBeenCalledWith('/');
    expect(mockAuthApi.refreshCurrentSession).not.toHaveBeenCalled();
  });

  it('should use the current backend roles for staff authorization', async () => {
    mockTokenStorage.session.set(session('MEDECIN'));
    mockAuthApi.refreshCurrentSession.mockReturnValue(of({
      email: 'user@joprelys.local',
      name: 'User',
      role: 'MEDECIN,DAF',
    }));

    const result = TestBed.runInInjectionContext(() =>
      roleGuard({ data: { expectedRoles: ['DAF'] } } as never, {} as never),
    );

    await expect(firstValueFrom(result as Observable<unknown>)).resolves.toBe(true);
  });

  it('should redirect to unauthorized when refreshed roles do not match', async () => {
    mockTokenStorage.session.set(session('MEDECIN'));
    mockAuthApi.refreshCurrentSession.mockReturnValue(of({
      email: 'user@joprelys.local',
      name: 'User',
      role: 'MEDECIN',
    }));

    const result = TestBed.runInInjectionContext(() =>
      roleGuard({ data: { expectedRoles: ['DAF'] } } as never, {} as never),
    );

    await expect(firstValueFrom(result as Observable<unknown>)).resolves.toBe('/unauthorized');
    expect(mockRouter.parseUrl).toHaveBeenCalledWith('/unauthorized');
  });

  it('should clear the session when the backend rejects the current token', async () => {
    mockTokenStorage.session.set(session('CAISSIER'));
    mockAuthApi.refreshCurrentSession.mockReturnValue(throwError(() => new Error('unauthorized')));

    const result = TestBed.runInInjectionContext(() =>
      roleGuard({ data: { expectedRoles: ['CAISSIER'] } } as never, {} as never),
    );

    await expect(firstValueFrom(result as Observable<unknown>)).resolves.toBe('/');
    expect(mockTokenStorage.clear).toHaveBeenCalled();
  });

  it('should authorize a patient locally without calling the staff session endpoint', () => {
    mockTokenStorage.session.set(session('PATIENT'));

    const result = TestBed.runInInjectionContext(() =>
      roleGuard({ data: { expectedRoles: ['PATIENT'] } } as never, {} as never),
    );

    expect(result).toBe(true);
    expect(mockAuthApi.refreshCurrentSession).not.toHaveBeenCalled();
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
