import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthSessionRecoveryService } from './auth-session-recovery.service';
import { AuthTokenStorageService } from './auth-token-storage.service';
import { authTokenInterceptor } from './auth-token.interceptor';
import { LoginResponse } from './auth.models';

describe('authTokenInterceptor', () => {
  let router: { url: string; navigate: ReturnType<typeof vi.fn> };
  let recovery: {
    refreshAccessToken: ReturnType<typeof vi.fn>;
    expireSession: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    sessionStorage.clear();
    router = {
      url: '/patients',
      navigate: vi.fn().mockResolvedValue(true),
    };
    recovery = {
      refreshAccessToken: vi.fn(),
      expireSession: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authTokenInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
        { provide: AuthSessionRecoveryService, useValue: recovery },
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

  it('should recover a professional request when access token is missing', () => {
    const http = TestBed.inject(HttpClient);
    const httpTesting = TestBed.inject(HttpTestingController);
    const tokenStorage = TestBed.inject(AuthTokenStorageService);
    recovery.refreshAccessToken.mockReturnValue(of('restored-token'));

    expect(tokenStorage.session()).toBeNull();
    http.get('/api/patients').subscribe();

    expect(recovery.refreshAccessToken).toHaveBeenCalledWith(undefined);
    const protectedRequest = httpTesting.expectOne('/api/patients');
    expect(protectedRequest.request.headers.get('Authorization')).toBe('Bearer restored-token');
    protectedRequest.flush([]);
    expect(recovery.expireSession).not.toHaveBeenCalled();
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
    expect(recovery.refreshAccessToken).not.toHaveBeenCalled();
  });

  it('should refresh an expired professional token before sending the protected request', () => {
    const http = TestBed.inject(HttpClient);
    const httpTesting = TestBed.inject(HttpTestingController);
    const tokenStorage = TestBed.inject(AuthTokenStorageService);
    recovery.refreshAccessToken.mockReturnValue(of('fresh-token'));

    tokenStorage.save(loginResponse('expired-token', '2020-01-01T00:00:00Z'));
    http.get('/api/patients').subscribe();

    expect(recovery.refreshAccessToken).toHaveBeenCalledWith('expired-token');
    const protectedRequest = httpTesting.expectOne('/api/patients');
    expect(protectedRequest.request.headers.get('Authorization')).toBe('Bearer fresh-token');
    protectedRequest.flush([]);
  });

  it('should refresh and retry once when the professional api rejects the current token', () => {
    const http = TestBed.inject(HttpClient);
    const httpTesting = TestBed.inject(HttpTestingController);
    const tokenStorage = TestBed.inject(AuthTokenStorageService);
    recovery.refreshAccessToken.mockReturnValue(of('replacement-token'));

    tokenStorage.save(loginResponse('revoked-token', '2999-07-02T12:30:00Z'));
    http.get('/api/patients').subscribe();

    const firstRequest = httpTesting.expectOne('/api/patients');
    expect(firstRequest.request.headers.get('Authorization')).toBe('Bearer revoked-token');
    firstRequest.flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(recovery.refreshAccessToken).toHaveBeenCalledWith('revoked-token');
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

    expect(recovery.refreshAccessToken).not.toHaveBeenCalled();
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

    expect(recovery.refreshAccessToken).not.toHaveBeenCalled();
    expect(recovery.expireSession).not.toHaveBeenCalled();
    expect(tokenStorage.accessToken).toBe('valid-token');
  });

  it('should not reclassify a professional refresh server error as an expired session', () => {
    const http = TestBed.inject(HttpClient);
    const httpTesting = TestBed.inject(HttpTestingController);
    const tokenStorage = TestBed.inject(AuthTokenStorageService);
    recovery.refreshAccessToken.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 500,
      statusText: 'Server Error',
    })));

    tokenStorage.save(loginResponse('expired-token', '2020-01-01T00:00:00Z'));
    http.get('/api/patients').subscribe({
      error: (error: HttpErrorResponse) => expect(error.status).toBe(500),
    });

    httpTesting.expectNone('/api/patients');
    expect(recovery.expireSession).not.toHaveBeenCalled();
    expect(tokenStorage.session()).not.toBeNull();
  });

  it('should expire the professional session when refresh is positively rejected', () => {
    const http = TestBed.inject(HttpClient);
    const httpTesting = TestBed.inject(HttpTestingController);
    const tokenStorage = TestBed.inject(AuthTokenStorageService);
    recovery.refreshAccessToken.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 401,
      statusText: 'Unauthorized',
    })));

    tokenStorage.save(loginResponse('expired-token', '2020-01-01T00:00:00Z'));
    http.get('/api/patients').subscribe({
      error: (error: HttpErrorResponse) => expect(error.status).toBe(401),
    });

    httpTesting.expectNone('/api/patients');
    expect(recovery.expireSession).toHaveBeenCalledTimes(1);
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

    expect(recovery.refreshAccessToken).not.toHaveBeenCalled();
    expect(recovery.expireSession).toHaveBeenCalledTimes(1);
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
    expect(recovery.refreshAccessToken).not.toHaveBeenCalled();
    expect(recovery.expireSession).toHaveBeenCalledTimes(1);
  });
});

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
