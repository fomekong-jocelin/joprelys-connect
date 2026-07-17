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
    TestBed.inject(HttpTestingController).verify();
    sessionStorage.clear();
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

  it('should refresh an expired token before sending the protected request', () => {
    const http = TestBed.inject(HttpClient);
    const httpTesting = TestBed.inject(HttpTestingController);
    const tokenStorage = TestBed.inject(AuthTokenStorageService);

    tokenStorage.save(loginResponse('expired-token', '2020-01-01T00:00:00Z'));

    http.get('/api/patients').subscribe();

    const refreshRequest = httpTesting.expectOne('/api/auth/refresh');
    expect(refreshRequest.request.method).toBe('POST');
    refreshRequest.flush(loginResponse('fresh-token', '2999-07-02T12:30:00Z'));

    const protectedRequest = httpTesting.expectOne('/api/patients');
    expect(protectedRequest.request.headers.get('Authorization')).toBe('Bearer fresh-token');
    protectedRequest.flush([]);
    expect(tokenStorage.accessToken).toBe('fresh-token');
  });

  it('should refresh and retry once when the api rejects the current token', () => {
    const http = TestBed.inject(HttpClient);
    const httpTesting = TestBed.inject(HttpTestingController);
    const tokenStorage = TestBed.inject(AuthTokenStorageService);

    tokenStorage.save(loginResponse('revoked-token', '2999-07-02T12:30:00Z'));

    http.get('/api/patients').subscribe();

    const firstRequest = httpTesting.expectOne('/api/patients');
    expect(firstRequest.request.headers.get('Authorization')).toBe('Bearer revoked-token');
    firstRequest.flush({}, { status: 401, statusText: 'Unauthorized' });

    httpTesting.expectOne('/api/auth/refresh')
      .flush(loginResponse('replacement-token', '2999-07-02T12:30:00Z'));

    const retryRequest = httpTesting.expectOne('/api/patients');
    expect(retryRequest.request.headers.get('Authorization')).toBe('Bearer replacement-token');
    retryRequest.flush([]);
  });

  it('should clear the session and redirect with an explanation when refresh is rejected', () => {
    const http = TestBed.inject(HttpClient);
    const httpTesting = TestBed.inject(HttpTestingController);
    const tokenStorage = TestBed.inject(AuthTokenStorageService);

    tokenStorage.save(loginResponse('expired-token', '2020-01-01T00:00:00Z'));

    http.get('/api/patients').subscribe({
      error: (error: HttpErrorResponse) => expect(error.status).toBe(401),
    });

    httpTesting.expectOne('/api/auth/refresh')
      .flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(tokenStorage.session()).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/'], {
      queryParams: {
        sessionExpired: 'true',
        returnUrl: '/patients',
      },
      replaceUrl: true,
    });
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
