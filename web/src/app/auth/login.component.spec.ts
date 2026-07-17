import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthTokenStorageService } from './auth-token-storage.service';
import { LoginComponent } from './login.component';
import { ActivatedRoute, Router } from '@angular/router';

describe('LoginComponent', () => {
  let mockRouter: any;
  let mockRoute: any;

  beforeEach(async () => {
    sessionStorage.clear();
    mockRoute = {
      snapshot: {
        queryParamMap: { get: vi.fn(() => null) },
      },
    };
    mockRouter = {
      navigate: vi.fn(),
      navigateByUrl: vi.fn(),
      parseUrl: vi.fn(),
    };
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: mockRouter },
        { provide: ActivatedRoute, useValue: mockRoute },
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
    expect(mockRouter.navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('should return to the scanned consultation route after successful login', () => {
    mockRoute.snapshot.queryParamMap.get.mockReturnValue(
      '/clinic/consultation/8dfc8352-505c-41a6-b936-a2db49901ee4',
    );
    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    const httpTesting = TestBed.inject(HttpTestingController);

    component.email.set('doctor@example.com');
    component.password.set('Password123!');
    component.submit();

    httpTesting.expectOne('/api/auth/login').flush({
      accessToken: 'jwt-token',
      tokenType: 'Bearer',
      expiresAt: '2026-07-17T20:00:00Z',
      email: 'doctor@example.com',
      name: 'Doctor',
      role: 'MEDECIN',
    });

    expect(mockRouter.navigateByUrl).toHaveBeenCalledWith(
      '/clinic/consultation/8dfc8352-505c-41a6-b936-a2db49901ee4',
    );
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
