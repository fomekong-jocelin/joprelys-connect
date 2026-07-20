import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { ThemeService } from '../core/theme/theme.service';
import { AuthTokenStorageService } from './auth-token-storage.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let mockRouter: any;
  let mockRoute: any;
  let queryParams: Record<string, string | null>;

  beforeEach(async () => {
    sessionStorage.clear();
    localStorage.clear();
    queryParams = {};
    mockRoute = {
      snapshot: {
        queryParamMap: {
          get: vi.fn((key: string) => queryParams[key] ?? null),
        },
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
    localStorage.clear();
  });

  it('should render the premium mobile-first controls with the shared logo', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    const root = fixture.nativeElement as HTMLElement;
    const frenchButton = root.querySelector<HTMLButtonElement>('#login-language-fr');
    const englishButton = root.querySelector<HTMLButtonElement>('#login-language-en');

    expect(root.querySelector('app-logo')).not.toBeNull();
    expect(root.querySelector('#login-theme-toggle')).not.toBeNull();
    expect(frenchButton?.textContent).toContain('🇫🇷');
    expect(englishButton?.textContent).toContain('🇬🇧');
    expect(root.textContent).not.toContain('Flux clinique synchronisé');
    expect(root.querySelector('#toggle-staff')?.getAttribute('aria-pressed')).toBe('true');
  });

  it('should switch language from the flag controls', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    const root = fixture.nativeElement as HTMLElement;
    root.querySelector<HTMLButtonElement>('#login-language-en')?.click();
    fixture.detectChanges();

    expect(component.locale()).toBe('en');
    expect(root.querySelector('#login-language-en')?.getAttribute('aria-pressed')).toBe('true');

    root.querySelector<HTMLButtonElement>('#login-language-fr')?.click();
    expect(component.locale()).toBe('fr');
  });

  it('should switch and persist the theme before authentication', () => {
    const themeService = TestBed.inject(ThemeService);
    themeService.setTheme('light');

    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    const root = fixture.nativeElement as HTMLElement;
    root.querySelector<HTMLButtonElement>('#login-theme-toggle')?.click();
    fixture.detectChanges();

    expect(component.theme()).toBe('dark');
    expect(localStorage.getItem('joprelys.theme')).toBe('dark');
    expect(document.documentElement.dataset['theme']).toBe('dark');
  });

  it('should switch between staff and patient forms', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    const root = fixture.nativeElement as HTMLElement;
    root.querySelector<HTMLButtonElement>('#toggle-patient')?.click();
    fixture.detectChanges();

    expect(component.mode()).toBe('patient');
    expect(root.querySelector('#toggle-patient')?.getAttribute('aria-pressed')).toBe('true');
    expect(root.querySelector('#patient-number')).not.toBeNull();

    root.querySelector<HTMLButtonElement>('#toggle-staff')?.click();
    fixture.detectChanges();

    expect(component.mode()).toBe('staff');
    expect(root.querySelector('#staff-email')).not.toBeNull();
  });

  it('should explain that the session expired', () => {
    queryParams['sessionExpired'] = 'true';

    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;

    expect(component.error()).toBe('common.error.unauthorized');
  });

  it('should store the professional session only after staff OTP verification', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    const httpTesting = TestBed.inject(HttpTestingController);
    const tokenStorage = TestBed.inject(AuthTokenStorageService);

    component.email.set('cashier@example.com');
    component.password.set('Password123!');
    component.submit();

    const loginRequest = httpTesting.expectOne('/api/auth/login');
    expect(loginRequest.request.method).toBe('POST');
    expect(loginRequest.request.body).toEqual({
      email: 'cashier@example.com',
      password: 'Password123!',
    });
    loginRequest.flush({
      requiresOtp: true,
      email: 'cashier@example.com',
      name: 'Caissier Test',
      role: 'CAISSIER',
    });

    expect(component.staffStep()).toBe(2);
    expect(component.password()).toBe('');
    expect(tokenStorage.accessToken).toBeNull();
    expect(mockRouter.navigateByUrl).not.toHaveBeenCalled();

    component.staffOtpCode.set('123456');
    component.verifyStaffOtp();

    const otpRequest = httpTesting.expectOne('/api/auth/verify-otp');
    expect(otpRequest.request.method).toBe('POST');
    expect(otpRequest.request.body).toEqual({
      email: 'cashier@example.com',
      otpCode: '123456',
    });
    otpRequest.flush({
      accessToken: 'jwt-token',
      tokenType: 'Bearer',
      expiresAt: '2999-07-02T12:30:00Z',
      email: 'cashier@example.com',
      name: 'Caissier Test',
      role: 'CAISSIER',
      requiresOtp: false,
    });

    expect(tokenStorage.accessToken).toBe('jwt-token');
    expect(component.error()).toBeNull();
    expect(mockRouter.navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('should return to the scanned consultation route after OTP verification', () => {
    queryParams['returnUrl'] = '/clinic/consultation/8dfc8352-505c-41a6-b936-a2db49901ee4';
    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    const httpTesting = TestBed.inject(HttpTestingController);

    component.email.set('doctor@example.com');
    component.password.set('Password123!');
    component.submit();

    httpTesting.expectOne('/api/auth/login').flush({
      requiresOtp: true,
      email: 'doctor@example.com',
      name: 'Doctor',
      role: 'MEDECIN',
    });
    expect(mockRouter.navigateByUrl).not.toHaveBeenCalled();

    component.staffOtpCode.set('654321');
    component.verifyStaffOtp();
    httpTesting.expectOne('/api/auth/verify-otp').flush({
      accessToken: 'jwt-token',
      tokenType: 'Bearer',
      expiresAt: '2999-07-17T20:00:00Z',
      email: 'doctor@example.com',
      name: 'Doctor',
      role: 'MEDECIN',
      requiresOtp: false,
    });

    expect(mockRouter.navigateByUrl).toHaveBeenCalledWith(
      '/clinic/consultation/8dfc8352-505c-41a6-b936-a2db49901ee4',
    );
  });

  it('should keep the error generic when credentials are invalid', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    const httpTesting = TestBed.inject(HttpTestingController);

    component.email.set('agent@example.com');
    component.password.set('wrong-password');
    component.submit();

    httpTesting.expectOne('/api/auth/login').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(component.error()).toBe('login.error.invalidCredentials');
  });

  it('should explain when the OTP recipient address is rejected', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    const httpTesting = TestBed.inject(HttpTestingController);

    component.email.set('cashier@example.com');
    component.password.set('Password123!');
    component.submit();

    httpTesting.expectOne('/api/auth/login').flush({
      error: {
        code: 'MAIL_RECIPIENT_REJECTED',
        message: 'Recipient rejected',
        trace_id: 'trc_test',
      },
    }, { status: 422, statusText: 'Unprocessable Entity' });

    expect(component.error()).toBe('login.error.otpRecipientRejected');
    expect(component.staffStep()).toBe(1);
  });

  it('should distinguish a temporary OTP delivery outage', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    const httpTesting = TestBed.inject(HttpTestingController);

    component.email.set('cashier@example.com');
    component.password.set('Password123!');
    component.submit();

    httpTesting.expectOne('/api/auth/login').flush({
      error: {
        code: 'MAIL_DELIVERY_UNAVAILABLE',
        message: 'Mail unavailable',
        trace_id: 'trc_test',
      },
    }, { status: 503, statusText: 'Service Unavailable' });

    expect(component.error()).toBe('login.error.otpDeliveryUnavailable');
    expect(component.staffStep()).toBe(1);
  });

  it('should display the normalized API message when staff OTP verification fails', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    const httpTesting = TestBed.inject(HttpTestingController);

    component.email.set('cashier@example.com');
    component.staffOtpCode.set('000000');
    component.verifyStaffOtp();

    httpTesting.expectOne('/api/auth/verify-otp').flush({
      error: {
        code: 'UNAUTHORIZED',
        message: 'Code de sécurité incorrect.',
        trace_id: 'trc_test',
      },
    }, { status: 401, statusText: 'Unauthorized' });

    expect(component.error()).toBe('Code de sécurité incorrect.');
  });
});
