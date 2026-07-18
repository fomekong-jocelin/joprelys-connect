import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { AuthTokenStorageService } from '../../auth/auth-token-storage.service';
import { LoginResponse } from '../../auth/auth.models';
import { RbacApiService } from './rbac-api.service';

describe('RbacApiService session isolation', () => {
  let service: RbacApiService;
  let storage: AuthTokenStorageService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(RbacApiService);
    storage = TestBed.inject(AuthTokenStorageService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('should hide cached professional access as soon as the authenticated token changes', async () => {
    storage.save(session('doctor-token', 'doctor@test.local', 'MEDECIN'));
    const loaded = firstValueFrom(service.ensureMyAccess());
    http.expectOne('/api/rbac/me').flush({
      userId: 'doctor-1',
      roles: ['MEDECIN'],
      permissions: ['AVAILABILITY_MANAGE'],
    });
    await loaded;
    expect(service.access()?.permissions).toContain('AVAILABILITY_MANAGE');

    storage.save(session('patient-token', 'DPU-001', 'PATIENT'));

    expect(service.access()).toBeNull();
    expect(service.hasPermission('AVAILABILITY_MANAGE')).toBe(false);
  });

  it('should isolate cached permissions between any two professional roles', async () => {
    storage.save(session('cashier-token', 'cashier@test.local', 'CAISSIER'));
    const loaded = firstValueFrom(service.ensureMyAccess());
    http.expectOne('/api/rbac/me').flush({
      userId: 'cashier-1',
      roles: ['CAISSIER'],
      permissions: ['CASH_PAYMENT_COLLECT'],
    });
    await loaded;
    expect(service.hasPermission('CASH_PAYMENT_COLLECT')).toBe(true);

    storage.save(session('nurse-token', 'nurse@test.local', 'INFIRMIER'));

    expect(service.access()).toBeNull();
    expect(service.hasPermission('CASH_PAYMENT_COLLECT')).toBe(false);
  });

  it('should ignore a professional RBAC response completed after a patient login', async () => {
    storage.save(session('doctor-token', 'doctor@test.local', 'MEDECIN'));
    const staleAccess = firstValueFrom(service.ensureMyAccess());
    const staleRequest = http.expectOne('/api/rbac/me');

    storage.save(session('patient-token', 'DPU-001', 'PATIENT'));
    staleRequest.flush({
      userId: 'doctor-1',
      roles: ['MEDECIN'],
      permissions: ['AVAILABILITY_MANAGE'],
    });

    await expect(staleAccess).rejects.toThrow('active session');
    expect(service.access()).toBeNull();
  });
});

function session(accessToken: string, email: string, role: string): LoginResponse {
  return {
    accessToken,
    tokenType: 'Bearer',
    expiresAt: '2999-07-18T12:00:00Z',
    email,
    name: email,
    role,
  };
}
