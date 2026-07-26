import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { LoginResponse } from './auth.models';
import { AuthTokenStorageService } from './auth-token-storage.service';

describe('AuthTokenStorageService session boundaries', () => {
  let service: AuthTokenStorageService;
  let local: MemoryStorage;
  let session: MemoryStorage;

  beforeEach(() => {
    local = new MemoryStorage();
    session = new MemoryStorage();
    clearTestCookie();
    const fakeDocument = {
      defaultView: { localStorage: local, sessionStorage: session },
      get cookie() {
        return document.cookie;
      },
      set cookie(value: string) {
        document.cookie = value;
      },
    } as unknown as Document;
    TestBed.configureTestingModule({
      providers: [{ provide: DOCUMENT, useValue: fakeDocument }],
    });
    service = TestBed.inject(AuthTokenStorageService);
  });

  afterEach(() => {
    clearTestCookie();
  });

  it('should purge tab identity state without deleting shared localStorage or cookies', () => {
    const cleanup = vi.fn();
    service.registerSessionBoundaryCleanup(cleanup);
    service.save(loginResponse('doctor-token', 'doctor@test.local', 'MEDECIN'));
    local.setItem('joprelys.theme', 'dark');
    session.setItem('joprelys.rbac.organizationScope', 'clinic-1');
    document.cookie = 'joprelys_test_cookie=secret; Path=/; SameSite=Lax';

    service.clear();

    expect(service.session()).toBeNull();
    expect(local.getItem('joprelys.theme')).toBe('dark');
    expect(session.length).toBe(0);
    expect(document.cookie).toContain('joprelys_test_cookie=secret');
    expect(cleanup).toHaveBeenCalledOnce();
  });

  it('should purge previous tab identity state before saving a different identity', () => {
    const cleanup = vi.fn();
    service.registerSessionBoundaryCleanup(cleanup);
    service.save(loginResponse('doctor-token', 'doctor@test.local', 'MEDECIN'));
    local.setItem('shared-preference', 'keep-me');
    session.setItem('feature-scope', 'doctor-scope');

    service.save(loginResponse('patient-token', 'patient@test.local', 'PATIENT'));

    expect(service.accessToken).toBe('patient-token');
    expect(local.getItem('shared-preference')).toBe('keep-me');
    expect(session.getItem('feature-scope')).toBeNull();
    expect(session.length).toBe(1);
    expect(cleanup).toHaveBeenCalledOnce();
    expect(service.isPatientSession()).toBe(true);
  });

  it('should preserve non-session storage during a same-account access-token refresh', () => {
    service.save(loginResponse('old-token', 'doctor@test.local', 'MEDECIN'));
    local.setItem('joprelys.theme', 'dark');

    service.clearAccessToken();
    service.save(loginResponse('new-token', 'doctor@test.local', 'MEDECIN'));

    expect(service.accessToken).toBe('new-token');
    expect(local.getItem('joprelys.theme')).toBe('dark');
  });
});

function loginResponse(accessToken: string, email: string, role: string): LoginResponse {
  return {
    accessToken,
    tokenType: 'Bearer',
    expiresAt: '2999-07-18T12:00:00Z',
    email,
    name: email,
    role,
  };
}

function clearTestCookie(): void {
  document.cookie = 'joprelys_test_cookie=; Max-Age=0; Path=/; SameSite=Lax';
}

class MemoryStorage implements Storage {
  private readonly values = new Map<string, string>();

  get length(): number {
    return this.values.size;
  }

  clear(): void {
    this.values.clear();
  }

  getItem(key: string): string | null {
    return this.values.get(key) ?? null;
  }

  key(index: number): string | null {
    return [...this.values.keys()][index] ?? null;
  }

  removeItem(key: string): void {
    this.values.delete(key);
  }

  setItem(key: string, value: string): void {
    this.values.set(key, value);
  }
}
