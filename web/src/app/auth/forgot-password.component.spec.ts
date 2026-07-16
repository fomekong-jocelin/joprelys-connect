import { TestBed, ComponentFixture } from '@angular/core/testing';
import { ForgotPasswordComponent } from './forgot-password.component';
import { AuthApiService } from './auth-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { signal } from '@angular/core';

describe('ForgotPasswordComponent', () => {
  let component: ForgotPasswordComponent;
  let fixture: ComponentFixture<ForgotPasswordComponent>;
  let mockAuthApi: any;
  let mockI18n: any;
  let router: Router;

  beforeEach(async () => {
    mockAuthApi = {
      requestPasswordRecovery: vi.fn().mockReturnValue(of(void 0)),
      resetPassword: vi.fn().mockReturnValue(of(null))
    };

    mockI18n = {
      t: vi.fn().mockImplementation((key) => key),
      locale: signal('fr'),
      setLocale: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [ForgotPasswordComponent, RouterTestingModule],
      providers: [
        { provide: AuthApiService, useValue: mockAuthApi },
        { provide: I18nService, useValue: mockI18n }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ForgotPasswordComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockImplementation(() => Promise.resolve(true));
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
    expect(component.step()).toBe(1);
  });

  it('should validate email field and send request', () => {
    component.email.set('medecin@joprelys.local');
    expect(component.canSubmitEmail()).toBe(true);

    component.submitRequest();

    expect(mockAuthApi.requestPasswordRecovery).toHaveBeenCalledWith('medecin@joprelys.local');
    expect(component.step()).toBe(2);
  });

  it('should validate inputs for reset step and show error if mismatch', () => {
    component.step.set(2);
    component.email.set('medecin@joprelys.local');
    component.otpCode.set('123456');
    component.newPassword.set('newpassword123');
    component.confirmPassword.set('different'); // password mismatch

    component.submitReset();

    expect(component.error()).toBe('auth.forgotPassword.error.mismatch');
    expect(mockAuthApi.resetPassword).not.toHaveBeenCalled();

    // Correcting matching password
    component.confirmPassword.set('newpassword123');
    component.submitReset();

    expect(mockAuthApi.resetPassword).toHaveBeenCalledWith({
      email: 'medecin@joprelys.local',
      otpCode: '123456',
      newPassword: 'newpassword123'
    });
    expect(component.step()).toBe(3);
  });

  it('should navigate to login page on success step', () => {
    component.step.set(3);
    component.goToLogin();
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });

  it('should display the normalized mail delivery error', () => {
    mockAuthApi.requestPasswordRecovery.mockReturnValue(throwError(() => ({
      status: 503,
      error: { error: { code: 'MAIL_DELIVERY_UNAVAILABLE', message: 'Service e-mail indisponible.' } }
    })));
    component.email.set('medecin@joprelys.local');

    component.submitRequest();

    expect(component.error()).toBe('Service e-mail indisponible.');
    expect(component.step()).toBe(1);
  });
});
