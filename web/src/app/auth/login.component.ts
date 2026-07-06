import { Component, computed, inject, signal, effect } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthApiService } from './auth-api.service';
import { AuthTokenStorageService } from './auth-token-storage.service';
import { PatientPortalService } from '../patient/portal/services/patient-portal.service';
import { I18nService } from '../core/i18n/i18n.service';
import { AppLocale } from '../core/config/app-brand.config';
import { AppLogoComponent } from '../shared/ui/app-logo.component';

type LoginMode = 'staff' | 'patient';

@Component({
  selector: 'app-login',
  imports: [RouterLink, AppLogoComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  private readonly authApi = inject(AuthApiService);
  private readonly tokenStorage = inject(AuthTokenStorageService);
  private readonly portalService = inject(PatientPortalService);
  private readonly router = inject(Router);
  private readonly i18n = inject(I18nService);

  readonly locale = this.i18n.locale;

  // --- Mode toggle ---
  readonly mode = signal<LoginMode>('staff');

  // --- Staff login state ---
  readonly email = signal('');
  readonly password = signal('');
  readonly error = signal<string | null>(null);
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
    // Rediriger si session staff active
    effect(() => {
      const s = this.session();
      if (s && s.role !== 'PATIENT') {
        this.router.navigate(['/dashboard']);
      }
      if (s && s.role === 'PATIENT') {
        this.router.navigate(['/patient/dashboard']);
      }
    });
  }

  setLang(lang: AppLocale): void {
    this.i18n.setLocale(lang);
  }

  setMode(m: LoginMode): void {
    this.mode.set(m);
    this.error.set(null);
    this.staffStep.set(1);
    this.staffOtpCode.set('');
    // Reset états patient lors du changement de mode
    if (m === 'patient') {
      this.patientStep.set(1);
      this.patientNumber.set('');
      this.patientPhone.set('');
      this.patientBirthDate.set('');
      this.otpCode.set('');
    }
  }

  // ---- Staff ----

  submit(): void {
    this.error.set(null);
    if (!this.canSubmit()) {
      this.error.set('Renseignez un e-mail valide et un mot de passe.');
      return;
    }
    this.loading.set(true);
    this.authApi.login({ email: this.email(), password: this.password() }).subscribe({
      next: (res) => {
        this.password.set('');
        this.loading.set(false);
        if (res.requiresOtp) {
          this.staffStep.set(2);
          this.staffOtpCode.set(res.otpCode || '');
        } else {
          this.router.navigate(['/dashboard']);
        }
      },
      error: () => {
        this.error.set('Identifiants invalides.');
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
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.detail || 'Code incorrect ou expiré. Réessayez.');
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

  // ---- Patient OTP ----

  requestOtp(): void {
    this.loading.set(true);
    this.error.set(null);
    this.portalService.requestOtp({
      globalPatientNumber: this.patientNumber(),
      phone: this.patientPhone(),
      birthDate: this.patientBirthDate()
    }).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.patientStep.set(2);
        if (res && res.otpCode) {
          this.otpCode.set(res.otpCode);
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.detail || 'Informations incorrectes. Vérifiez votre numéro DPU, téléphone et date de naissance.');
      }
    });
  }

  verifyOtp(): void {
    this.loading.set(true);
    this.error.set(null);
    this.portalService.verifyOtp({
      globalPatientNumber: this.patientNumber(),
      otpCode: this.otpCode()
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/patient/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.detail || 'Code incorrect ou expiré. Réessayez.');
      }
    });
  }

  // ---- Input helpers ----

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

  private isValidEmail(value: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
  }
}
