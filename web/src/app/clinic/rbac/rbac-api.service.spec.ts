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

  it('should keep cached access across a transparent token refresh for the same identity', async () => {
    storage.save(session('doctor-token-1', 'doctor@test.local', 'MEDECIN'));
    const loaded = firstValueFrom(service.ensureMyAccess());
    http.expectOne('/api/rbac/me').flush({
      userId: 'doctor-1',
      roles: ['MEDECIN'],
      permissions: ['AVAILABILITY_MANAGE', 'PATIENT_READ'],
    });
    await loaded;
    expect(service.access()?.permissions).toContain('PATIENT_READ');

    storage.save(session('doctor-token-2', 'doctor@test.local', 'MEDECIN'));

    expect(service.access()?.permissions).toContain('PATIENT_READ');
    expect(service.hasPermission('AVAILABILITY_MANAGE')).toBe(true);
  });

  it('should isolate cached permissions between any two professional identities', async () => {
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

  it('should invalidate cached access when the role set changes for the same email', async () => {
    storage.save(session('staff-token-1', 'staff@test.local', 'MEDECIN'));
    const loaded = firstValueFrom(service.ensureMyAccess());
    http.expectOne('/api/rbac/me').flush({
      userId: 'staff-1',
      roles: ['MEDECIN'],
      permissions: ['CLINICAL_WRITE'],
    });
    await loaded;

    storage.save(session('staff-token-2', 'staff@test.local', 'INFIRMIER'));

    expect(service.access()).toBeNull();
    expect(service.hasPermission('CLINICAL_WRITE')).toBe(false);
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

  it('should never infer one dynamic permission from another', () => {
    const permissions = service.effectivePermissionSet(['LAB_QUEUE_READ']);

    expect(permissions.has('LAB_QUEUE_READ')).toBe(true);
    expect(permissions.has('LAB_ORDER_READ')).toBe(false);
    expect(permissions.has('LAB_ORDER_WRITE')).toBe(false);
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
