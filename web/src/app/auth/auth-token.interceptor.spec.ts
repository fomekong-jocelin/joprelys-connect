import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthTokenStorageService } from './auth-token-storage.service';
import { authTokenInterceptor } from './auth-token.interceptor';

describe('authTokenInterceptor', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authTokenInterceptor])),
        provideHttpClientTesting(),
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

    tokenStorage.save({
      accessToken: 'jwt-token',
      tokenType: 'Bearer',
      expiresAt: '2026-07-02T12:30:00Z',
      email: 'agent@example.com',
      name: 'Agent Accueil',
      role: 'AGENT_ACCUEIL',
    });

    http.get('/api/patients').subscribe();

    const request = httpTesting.expectOne('/api/patients');
    expect(request.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    request.flush([]);
  });

  it('should not attach bearer token to external requests', () => {
    const http = TestBed.inject(HttpClient);
    const httpTesting = TestBed.inject(HttpTestingController);
    const tokenStorage = TestBed.inject(AuthTokenStorageService);

    tokenStorage.save({
      accessToken: 'jwt-token',
      tokenType: 'Bearer',
      expiresAt: '2026-07-02T12:30:00Z',
      email: 'agent@example.com',
      name: 'Agent Accueil',
      role: 'AGENT_ACCUEIL',
    });

    http.get('https://example.com/status').subscribe();

    const request = httpTesting.expectOne('https://example.com/status');
    expect(request.request.headers.has('Authorization')).toBeFalsy();
    request.flush({});
  });
});
