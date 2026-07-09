import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthTokenStorageService } from './auth-token-storage.service';
import { LoginComponent } from './login.component';

import { Router } from '@angular/router';

describe('LoginComponent', () => {
  let mockRouter: any;

  beforeEach(async () => {
    sessionStorage.clear();
    mockRouter = {
      navigate: vi.fn(),
      parseUrl: vi.fn()
    };
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: mockRouter }
      ],
    }).compileComponents();
  });

  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    sessionStorage.clear();
  });

  it('should store the auth session after successful login', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    const httpTesting = TestBed.inject(HttpTestingController);
    const tokenStorage = TestBed.inject(AuthTokenStorageService);

    component.email.set('agent@example.com');
    component.password.set('Password123!');
    component.submit();

    const request = httpTesting.expectOne('/api/auth/login');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      email: 'agent@example.com',
      password: 'Password123!',
    });

    request.flush({
      accessToken: 'jwt-token',
      tokenType: 'Bearer',
      expiresAt: '2026-07-02T12:30:00Z',
      email: 'agent@example.com',
      name: 'Agent Accueil',
      role: 'AGENT_ACCUEIL',
    });

    expect(tokenStorage.accessToken).toBe('jwt-token');
    expect(component.password()).toBe('');
    expect(component.error()).toBeNull();
  });

  it('should keep the error generic when login fails', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    const httpTesting = TestBed.inject(HttpTestingController);

    component.email.set('agent@example.com');
    component.password.set('wrong-password');
    component.submit();

    httpTesting.expectOne('/api/auth/login').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(component.error()).toBe('login.error.invalidCredentials');
  });
});
