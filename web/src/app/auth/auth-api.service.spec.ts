import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthApiService } from './auth-api.service';
import { AuthTokenStorageService } from './auth-token-storage.service';
import { LoginResponse } from './auth.models';

describe('AuthApiService', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: Router,
          useValue: { url: '/', navigate: vi.fn().mockResolvedValue(true) },
        },
      ],
    });
  });

  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    sessionStorage.clear();
  });

  it('should keep a valid professional session without calling the refresh endpoint', async () => {
    const service = TestBed.inject(AuthApiService);
    const storage = TestBed.inject(AuthTokenStorageService);
    const http = TestBed.inject(HttpTestingController);

    storage.save(loginResponse('valid-token', '2999-07-17T20:00:00Z'));

    await expect(firstValueFrom(service.restoreSession())).resolves.toBeUndefined();
    http.expectNone('/api/auth/refresh');
    expect(storage.accessToken).toBe('valid-token');
  });

  it('should restore a professional session from the refresh cookie when a new tab starts', async () => {
    const service = TestBed.inject(AuthApiService);
    const storage = TestBed.inject(AuthTokenStorageService);
    const http = TestBed.inject(HttpTestingController);

    const restored = firstValueFrom(service.restoreSession());
    const request = http.expectOne('/api/auth/refresh');
    expect(request.request.method).toBe('POST');
    expect(request.request.withCredentials).toBe(true);
    request.flush(loginResponse('restored-token', '2999-07-17T20:00:00Z'));

    await restored;
    expect(storage.accessToken).toBe('restored-token');
  });

  it('should replace an expired professional token from the refresh cookie without clearing state first', async () => {
    const service = TestBed.inject(AuthApiService);
    const storage = TestBed.inject(AuthTokenStorageService);
    const http = TestBed.inject(HttpTestingController);

    storage.save(loginResponse('expired-token', '2020-01-01T00:00:00Z'));

    const restored = firstValueFrom(service.restoreSession());
    expect(storage.session()).not.toBeNull();
    http.expectOne('/api/auth/refresh')
      .flush(loginResponse('renewed-token', '2999-07-17T20:00:00Z'));

    await restored;
    expect(storage.accessToken).toBe('renewed-token');
  });

  it('should never restore a professional identity over a valid patient session', async () => {
    const service = TestBed.inject(AuthApiService);
    const storage = TestBed.inject(AuthTokenStorageService);
    const http = TestBed.inject(HttpTestingController);

    storage.save(patientLoginResponse('patient-token', '2999-07-17T20:00:00Z'));

    await expect(firstValueFrom(service.restoreSession())).resolves.toBeUndefined();
    http.expectNone('/api/auth/refresh');
    expect(storage.accessToken).toBe('patient-token');
  });

  it('should expire a patient JWT locally without falling back to the professional refresh cookie', async () => {
    const service = TestBed.inject(AuthApiService);
    const storage = TestBed.inject(AuthTokenStorageService);
    const http = TestBed.inject(HttpTestingController);

    storage.save(patientLoginResponse('expired-patient-token', '2020-01-01T00:00:00Z'));

    await expect(firstValueFrom(service.restoreSession())).resolves.toBeUndefined();
    http.expectNone('/api/auth/refresh');
    expect(storage.session()).toBeNull();
  });

  it('should continue anonymously when no professional refresh cookie is available', async () => {
    const service = TestBed.inject(AuthApiService);
    const http = TestBed.inject(HttpTestingController);

    const restored = firstValueFrom(service.restoreSession());
    http.expectOne('/api/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });

    await expect(restored).resolves.toBeUndefined();
  });

  it('should keep bootstrap alive when the professional refresh service returns a server error', async () => {
    const service = TestBed.inject(AuthApiService);
    const http = TestBed.inject(HttpTestingController);

    const restored = firstValueFrom(service.restoreSession());
    http.expectOne('/api/auth/refresh').flush(
      { detail: 'Session service unavailable' },
      { status: 500, statusText: 'Internal Server Error' },
    );

    await expect(restored).resolves.toBeUndefined();
  });

  it('should preserve an expired professional session during a transient refresh outage', async () => {
    const service = TestBed.inject(AuthApiService);
    const storage = TestBed.inject(AuthTokenStorageService);
    const http = TestBed.inject(HttpTestingController);

    storage.save(loginResponse('expired-token', '2020-01-01T00:00:00Z'));

    const restored = firstValueFrom(service.restoreSession());
    http.expectOne('/api/auth/refresh').flush(
      { detail: 'Temporary gateway error' },
      { status: 503, statusText: 'Service Unavailable' },
    );

    await expect(restored).resolves.toBeUndefined();
    expect(storage.accessToken).toBe('expired-token');
  });
});

function loginResponse(accessToken: string, expiresAt: string): LoginResponse {
  return {
    accessToken,
    tokenType: 'Bearer',
    expiresAt,
    email: 'doctor@joprelys.local',
    name: 'Doctor',
    role: 'MEDECIN',
  };
}

function patientLoginResponse(accessToken: string, expiresAt: string): LoginResponse {
  return {
    accessToken,
    tokenType: 'Bearer',
    expiresAt,
    email: 'DPU-001@joprelys.local',
    name: 'Patient',
    role: 'PATIENT',
  };
}
