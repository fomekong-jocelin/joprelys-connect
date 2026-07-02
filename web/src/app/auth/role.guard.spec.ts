import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { roleGuard } from './role.guard';
import { AuthTokenStorageService } from './auth-token-storage.service';
import { signal } from '@angular/core';
import { AuthSession } from './auth.models';

describe('roleGuard', () => {
  let mockRouter: any;
  let mockTokenStorage: any;
  let sessionSignal: any;

  beforeEach(() => {
    sessionSignal = signal<AuthSession | null>(null);
    mockRouter = {
      parseUrl: vi.fn((url: string) => url as any)
    };
    mockTokenStorage = {
      session: sessionSignal
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: mockRouter },
        { provide: AuthTokenStorageService, useValue: mockTokenStorage }
      ]
    });
  });

  it('should redirect to root if no session exists', () => {
    sessionSignal.set(null);
    const result = TestBed.runInInjectionContext(() => 
      roleGuard({ data: { expectedRoles: ['ADMIN_JOPRELYS'] } } as any, {} as any)
    );
    expect(result).toBe('/');
    expect(mockRouter.parseUrl).toHaveBeenCalledWith('/');
  });

  it('should redirect to unauthorized if role does not match expectedRoles', () => {
    sessionSignal.set({
      accessToken: 'token',
      expiresAt: '2026-07-02T12:00:00Z',
      email: 'user@joprelys.local',
      name: 'User',
      role: 'MEDECIN'
    });
    const result = TestBed.runInInjectionContext(() => 
      roleGuard({ data: { expectedRoles: ['ADMIN_JOPRELYS'] } } as any, {} as any)
    );
    expect(result).toBe('/unauthorized');
    expect(mockRouter.parseUrl).toHaveBeenCalledWith('/unauthorized');
  });

  it('should allow activation if role matches expectedRoles', () => {
    sessionSignal.set({
      accessToken: 'token',
      expiresAt: '2026-07-02T12:00:00Z',
      email: 'admin@joprelys.local',
      name: 'Admin',
      role: 'ADMIN_JOPRELYS'
    });
    const result = TestBed.runInInjectionContext(() => 
      roleGuard({ data: { expectedRoles: ['ADMIN_JOPRELYS'] } } as any, {} as any)
    );
    expect(result).toBe(true);
  });
});
