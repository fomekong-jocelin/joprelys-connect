import { Component, computed, inject, signal, effect } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthApiService } from './auth-api.service';
import { AuthTokenStorageService } from './auth-token-storage.service';
import { PatientPortalService } from '../patient/portal/services/patient-portal.service';
import { I18nService } from '../core/i18n/i18n.service';
import { AppLocale } from '../core/config/app-brand.config';
import { ThemeService } from '../core/theme/theme.service';
import { AppLogoComponent } from '../shared/ui/app-logo.component';

type LoginMode = 'staff' | 'patient';

interface ApiErrorPayload {
  detail?: string;
  message?: string;
  error?: {
    code?: string;
    message?: string;
    trace_id?: string;
  };
}

interface HttpErrorLike {
  status?: number;
  error?: ApiErrorPayload;
}

@Component({
  selector: 'app-login',
  imports: [RouterLink, AppLogoComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.host.css',
})
export class LoginComponent {
  private readonly authApi = inject(AuthApiService);
  private readonly tokenStorage = inject(AuthTokenStorageService);
  private readonly portalService = inject(PatientPortalService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute, { optional: true });
  private readonly i18n = inject(I18nService);
  private readonly themeService = inject(ThemeService);

  private readonly requestedReturnUrl = this.resolveReturnUrl();

  readonly locale = this.i18n.locale;
  readonly theme = this.themeService.theme;

  // --- Mode toggle ---
  readonly mode = signal<LoginMode>('staff');

  // --- Staff login state ---
  readonly email = signal('');
  readonly password = signal('');
  readonly error = signal<string | null>(this.resolveInitialError());
  readonly loading = signal(false);
  readonly session = this.tokenStorage.session;
  readonly staffStep = signal<1 | 2>(1);
  readonly staffOtpCode = signal('');
  readonly canSubmit = computed(() =>
    this.isValidEmail(this.email()) && this.password().length > 0 && !this.loading()
  );

  // --- Patient OTP state ---
  readonly patientStep = signal<1 | 2>(1);
  readonly patientNumber = signal('');
  readonly patientPhone = signal('');
  readonly patientBirthDate = signal('');
  readonly otpCode = signal('');

  constructor() {
    effect(() => {
      const s = this.session();
      if (s) {
        this.router.navigateByUrl(this.destinationAfterLogin(s.role));
      }
    });
  }

  setLang(lang: AppLocale): void {
    this.i18n.setLocale(lang);
  }

  toggleTheme(): void {
    this.themeService.setTheme(this.theme() === 'light' ? 'dark' : 'light');
  }

  themeTooltip(): string {
    return this.theme() === 'dark'
      ? this.t('shell.theme.light')
      : this.t('shell.theme.dark');
  }

  setMode(m: LoginMode): void {
    this.mode.set(m);
    this.error.set(null);
    this.staffStep.set(1);
    this.staffOtpCode.set('');
    if (m === 'patient') {
      this.patientStep.set(1);
      this.patientNumber.set('');
      this.patientPhone.set('');
      this.patientBirthDate.set('');
      this.otpCode.set('');
    }
  }

  submit(): void {
    this.error.set(null);
    if (!this.canSubmit()) {
      this.error.set(this.t('login.error.requiredFields'));
      return;
    }
    this.loading.set(true);
    this.authApi.login({ email: this.email(), password: this.password() }).subscribe({
      next: (res) => {
        this.password.set('');
        this.loading.set(false);
        if (res.requiresOtp) {
          this.staffStep.set(2);
        } else {
          this.router.navigateByUrl(this.destinationAfterLogin(res.role || ''));
        }
      },
      error: (err: HttpErrorLike) => {
        this.error.set(this.staffLoginError(err));
        this.loading.set(false);
      },
    });
  }

  verifyStaffOtp(): void {
    this.loading.set(true);
    this.error.set(null);
    this.authApi.verifyStaffOtp(this.email(), this.staffOtpCode()).subscribe({
      next: () => {
        this.loading.set(false);
        const s = this.session();
        this.router.navigateByUrl(this.destinationAfterLogin(s?.role || ''));
      },
      error: (err: HttpErrorLike) => {
        this.loading.set(false);
        this.error.set(
          err.error?.error?.message
          ?? err.error?.detail
          ?? err.error?.message
          ?? this.t('login.error.invalidOtp')
        );
      }
    });
  }

  logout(): void {
    this.loading.set(true);
    this.authApi.logout().subscribe({
      next: () => {
        this.loading.set(false);
        this.staffStep.set(1);
      },
      error: () => {
        this.loading.set(false);
        this.staffStep.set(1);
      },
    });
  }

  requestOtp(): void {
    this.loading.set(true);
    this.error.set(null);
    this.portalService.requestOtp({
      globalPatientNumber: this.patientNumber().trim().toUpperCase(),
      phone: this.patientPhone().trim(),
      birthDate: this.patientBirthDate()
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.patientStep.set(2);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.detail || this.t('login.error.invalidPatientInfo'));
      }
    });
  }

  verifyOtp(): void {
    this.loading.set(true);
    this.error.set(null);
    this.portalService.verifyOtp({
      globalPatientNumber: this.patientNumber().trim().toUpperCase(),
      otpCode: this.otpCode().trim()
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/patient/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.detail || this.t('login.error.invalidOtp'));
      }
    });
  }

  updateEmail(event: Event): void { this.email.set(this.inputValue(event)); }
  updatePassword(event: Event): void { this.password.set(this.inputValue(event)); }
  updatePatientNumber(event: Event): void { this.patientNumber.set(this.inputValue(event)); }
  updatePatientPhone(event: Event): void { this.patientPhone.set(this.inputValue(event)); }
  updatePatientBirthDate(event: Event): void { this.patientBirthDate.set(this.inputValue(event)); }
  updateOtpCode(event: Event): void { this.otpCode.set(this.inputValue(event)); }
  updateStaffOtpCode(event: Event): void { this.staffOtpCode.set(this.inputValue(event)); }

  t(key: string): string {
    return this.i18n.t(key);
  }

  private inputValue(event: Event): string {
    return event.target instanceof HTMLInputElement ? event.target.value : '';
  }

  getLandingPage(role: string): string {
    const roles = role.split(',').map((r) => r.trim());
    if (roles.includes('PATIENT')) {
      return '/patient/dashboard';
    }
    return '/dashboard';
  }

  private destinationAfterLogin(role: string): string {
    return this.requestedReturnUrl ?? this.getLandingPage(role);
  }

  private resolveReturnUrl(): string | null {
    const returnUrl = this.route?.snapshot.queryParamMap.get('returnUrl')?.trim();
    if (!returnUrl || !returnUrl.startsWith('/') || returnUrl.startsWith('//')) {
      return null;
    }
    return returnUrl;
  }

  private resolveInitialError(): string | null {
    return this.route?.snapshot.queryParamMap.get('sessionExpired') === 'true'
      ? this.t('common.error.unauthorized')
      : null;
  }

  private staffLoginError(error: HttpErrorLike): string {
    const code = error.error?.error?.code;
    if (code === 'MAIL_RECIPIENT_REJECTED') {
      return this.t('login.error.otpRecipientRejected');
    }
    if (code === 'MAIL_DELIVERY_UNAVAILABLE') {
      return this.t('login.error.otpDeliveryUnavailable');
    }
    return this.t('login.error.invalidCredentials');
  }

  private isValidEmail(value: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
  }
}
