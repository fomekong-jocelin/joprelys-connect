import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AuthTokenStorageService } from './auth-token-storage.service';
import { authTokenInterceptor } from './auth-token.interceptor';
import { LoginResponse } from './auth.models';

describe('authTokenInterceptor', () => {
  let router: { url: string; navigate: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    sessionStorage.clear();
    router = {
      url: '/patients',
      navigate: vi.fn().mockResolvedValue(true),
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authTokenInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
      ],
    });
  });

  afterEach(() => {
    try {
      TestBed.inject(HttpTestingController).verify();
    } finally {
      sessionStorage.clear();
      TestBed.resetTestingModule();
    }
  });

  it('should attach bearer token to api requests', () => {
    const http = TestBed.inject(HttpClient);
    const httpTesting = TestBed.inject(HttpTestingController);
    const tokenStorage = TestBed.inject(AuthTokenStorageService);

    tokenStorage.save(loginResponse('jwt-token', '2999-07-02T12:30:00Z'));

    http.get('/api/patients').subscribe();

    const request = httpTesting.expectOne('/api/patients');
    expect(request.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    request.flush([]);
  });

  it('should recover a professional request from the HttpOnly refresh cookie when access token is missing', async () => {
    const http = TestBed.inject(HttpClient);
    const httpTesting = TestBed.inject(HttpTestingController);
    const tokenStorage = TestBed.inject(AuthTokenStorageService);

    expect(tokenStorage.session()).toBeNull();
    http.get('/api/patients').subscribe();

    const refreshRequest = httpTesting.expectOne('/api/auth/refresh');
    expect(refreshRequest.request.withCredentials).toBe(true);
    refreshRequest.flush(loginResponse('restored-token', '2999-07-02T12:30:00Z'));
    await settleRefresh();

    const protectedRequest = httpTesting.expectOne('/api/patients');
    expect(protectedRequest.request.headers.get('Authorization')).toBe('Bearer restored-token');
    protectedRequest.flush([]);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should not attach bearer token to external requests', () => {
    const http = TestBed.inject(HttpClient);
    const httpTesting = TestBed.inject(HttpTestingController);
    const tokenStorage = TestBed.inject(AuthTokenStorageService);

    tokenStorage.save(loginResponse('jwt-token', '2999-07-02T12:30:00Z'));

    http.get('https://example.com/status').subscribe();

    const request = httpTesting.expectOne('https://example.com/status');
    expect(request.request.headers.has('Authorization')).toBeFalsy();
    request.flush({});
  });

  it('should refresh an expired professional token before sending the protected request', async () => {
    const http = TestBed.inject(HttpClient);
    const httpTesting = TestBed.inject(HttpTestingController);
    const tokenStorage = TestBed.inject(AuthTokenStorageService);

    tokenStorage.save(loginResponse('expired-token', '2020-01-01T00:00:00Z'));

    http.get('/api/patients').subscribe();

    const refreshRequest = httpTesting.expectOne('/api/auth/refresh');
    expect(refreshRequest.request.method).toBe('POST');
    expect(refreshRequest.request.withCredentials).toBe(true);
    refreshRequest.flush(loginResponse('fresh-token', '2999-07-02T12:30:00Z'));
    await settleRefresh();

    const protectedRequest = httpTesting.expectOne('/api/patients');
    expect(protectedRequest.request.headers.get('Authorization')).toBe('Bearer fresh-token');
    protectedRequest.flush([]);
    expect(tokenStorage.accessToken).toBe('fresh-token');
  });

  it('should refresh and retry once when the professional api rejects the current token', async () => {
    const http = TestBed.inject(HttpClient);
    const httpTesting = TestBed.inject(HttpTestingController);
    const tokenStorage = TestBed.inject(AuthTokenStorageService);

    tokenStorage.save(loginResponse('revoked-token', '2999-07-02T12:30:00Z'));

    http.get('/api/patients').subscribe();

    const firstRequest = httpTesting.expectOne('/api/patients');
    expect(firstRequest.request.headers.get('Authorization')).toBe('Bearer revoked-token');
    firstRequest.flush({}, { status: 401, statusText: 'Unauthorized' });

    const refreshRequest = httpTesting.expectOne('/api/auth/refresh');
    refreshRequest.flush(loginResponse('replacement-token', '2999-07-02T12:30:00Z'));
    await settleRefresh();

    const retryRequest = httpTesting.expectOne('/api/patients');
    expect(retryRequest.request.headers.get('Authorization')).toBe('Bearer replacement-token');
    retryRequest.flush([]);
  });

  it('should reuse a newer professional token when an in-flight request is rejected after another refresh', () => {
    const http = TestBed.inject(HttpClient);
    const httpTesting = TestBed.inject(HttpTestingController);
    const tokenStorage = TestBed.inject(AuthTokenStorageService);

    tokenStorage.save(loginResponse('old-token', '2999-07-02T12:30:00Z'));
    http.get('/api/patients').subscribe();

    const firstRequest = httpTesting.expectOne('/api/patients');
    expect(firstRequest.request.headers.get('Authorization')).toBe('Bearer old-token');

    tokenStorage.save(loginResponse('peer-refreshed-token', '2999-07-02T12:45:00Z'));
    firstRequest.flush({}, { status: 401, statusText: 'Unauthorized' });

    httpTesting.expectNone('/api/auth/refresh');
    const retryRequest = httpTesting.expectOne('/api/patients');
    expect(retryRequest.request.headers.get('Authorization')).toBe('Bearer peer-refreshed-token');
    retryRequest.flush([]);
  });

  it('should preserve the professional session and surface a 403 authorization refusal without refreshing', () => {
    const http = TestBed.inject(HttpClient);
    const httpTesting = TestBed.inject(HttpTestingController);
    const tokenStorage = TestBed.inject(AuthTokenStorageService);

    tokenStorage.save(loginResponse('valid-token', '2999-07-02T12:30:00Z'));

    http.get('/api/patients').subscribe({
      error: (error: HttpErrorResponse) => expect(error.status).toBe(403),
    });

    httpTesting.expectOne('/api/patients')
      .flush({}, { status: 403, statusText: 'Forbidden' });

    httpTesting.expectNone('/api/auth/refresh');
    expect(tokenStorage.accessToken).toBe('valid-token');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should not reclassify a professional refresh server error as an expired session', async () => {
    const http = TestBed.inject(HttpClient);
    const httpTesting = TestBed.inject(HttpTestingController);
    const tokenStorage = TestBed.inject(AuthTokenStorageService);

    tokenStorage.save(loginResponse('expired-token', '2020-01-01T00:00:00Z'));

    http.get('/api/patients').subscribe({
      error: (error: HttpErrorResponse) => expect(error.status).toBe(500),
    });

    httpTesting.expectOne('/api/auth/refresh')
      .flush({}, { status: 500, statusText: 'Server Error' });
    await settleRefresh();

    expect(tokenStorage.session()).not.toBeNull();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should clear the professional session and redirect when professional refresh is rejected', async () => {
    const http = TestBed.inject(HttpClient);
    const httpTesting = TestBed.inject(HttpTestingController);
    const tokenStorage = TestBed.inject(AuthTokenStorageService);

    tokenStorage.save(loginResponse('expired-token', '2020-01-01T00:00:00Z'));

    http.get('/api/patients').subscribe({
      error: (error: HttpErrorResponse) => expect(error.status).toBe(401),
    });

    httpTesting.expectOne('/api/auth/refresh')
      .flush({}, { status: 401, statusText: 'Unauthorized' });
    await settleRefresh();

    expect(tokenStorage.session()).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/'], {
      queryParams: {
        sessionExpired: 'true',
        returnUrl: '/patients',
      },
      replaceUrl: true,
    });
  });

  it('should never refresh a patient JWT with the professional cookie after a 401', () => {
    const http = TestBed.inject(HttpClient);
    const httpTesting = TestBed.inject(HttpTestingController);
    const tokenStorage = TestBed.inject(AuthTokenStorageService);
    router.url = '/patient/dashboard';
    tokenStorage.save(patientLoginResponse('patient-token', '2999-07-02T12:30:00Z'));

    http.get('/api/patient/me').subscribe({
      error: (error: HttpErrorResponse) => expect(error.status).toBe(401),
    });

    const request = httpTesting.expectOne('/api/patient/me');
    expect(request.request.headers.get('Authorization')).toBe('Bearer patient-token');
    request.flush({}, { status: 401, statusText: 'Unauthorized' });

    httpTesting.expectNone('/api/auth/refresh');
    expect(tokenStorage.session()).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/'], {
      queryParams: {
        mode: 'patient',
        sessionExpired: 'true',
        returnUrl: '/patient/dashboard',
      },
      replaceUrl: true,
    });
  });

  it('should reject an expired patient JWT locally without touching the professional refresh cookie', () => {
    const http = TestBed.inject(HttpClient);
    const httpTesting = TestBed.inject(HttpTestingController);
    const tokenStorage = TestBed.inject(AuthTokenStorageService);
    router.url = '/patient/dashboard';
    tokenStorage.save(patientLoginResponse('expired-patient-token', '2020-01-01T00:00:00Z'));

    http.get('/api/patient/me').subscribe({
      error: (error: HttpErrorResponse) => expect(error.status).toBe(401),
    });

    httpTesting.expectNone('/api/patient/me');
    httpTesting.expectNone('/api/auth/refresh');
    expect(tokenStorage.session()).toBeNull();
  });
});

async function settleRefresh(): Promise<void> {
  // AuthSessionRecoveryService deliberately crosses a Promise boundary so it can
  // coordinate refresh with Web Locks across browser tabs. HttpTestingController
  // flushes the refresh response synchronously, therefore tests must allow the
  // firstValueFrom/Promise continuation to enqueue the protected retry.
  await Promise.resolve();
  await Promise.resolve();
}

function loginResponse(accessToken: string, expiresAt: string): LoginResponse {
  return {
    accessToken,
    tokenType: 'Bearer',
    expiresAt,
    email: 'agent@example.com',
    name: 'Agent Accueil',
    role: 'AGENT_ACCUEIL',
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
