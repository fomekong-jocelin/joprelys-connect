import { Component, inject, signal, effect } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { PatientPortalService } from './services/patient-portal.service';
import { AuthTokenStorageService } from '../../auth/auth-token-storage.service';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppLogoComponent } from '../../shared/ui/app-logo.component';

@Component({
  selector: 'app-patient-login',
  standalone: true,
  imports: [FormsModule, RouterLink, AppLogoComponent],
  template: `
    <main class="min-h-screen flex flex-col items-center justify-center p-6 bg-[var(--app-bg)] transition-colors duration-300">
      <div class="w-full max-w-[400px] flex flex-col items-center py-8">
        <!-- Logo -->
        <div class="mb-8">
          <app-logo size="lg"></app-logo>
        </div>

        <div class="w-full text-center mb-6">
          <h2 class="font-display font-bold text-2xl tracking-tight text-[var(--text-primary)] dark:text-white mb-2">
            {{ i18n.t('patient.login.title') }}
          </h2>
          <p class="text-sm text-[var(--text-muted)] font-medium">
            @if (step() === 1) {
              {{ i18n.t('patient.login.step1Subtitle') }}
            } @else {
              {{ i18n.t('patient.login.step2Subtitle') }}
            }
          </p>
        </div>

        <!-- Step 1: Input Credentials -->
        @if (step() === 1) {
          <form class="w-full space-y-4" (submit)="$event.preventDefault(); requestOtp()">
            <div class="space-y-1">
              <label class="block text-[11px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ i18n.t('patient.login.dpu') }}</label>
              <input
                type="text"
                required
                [(ngModel)]="globalPatientNumber"
                name="globalPatientNumber"
                class="w-full min-h-[46px] px-4 py-2 border border-[var(--app-border)] bg-[var(--bg-input)] dark:bg-slate-900 rounded-[var(--radius-brand-md)] text-[var(--text-primary)] focus:outline-hidden focus:border-[var(--brand-primary)] focus:bg-white dark:focus:bg-slate-950 text-sm"
                placeholder="DPU-JOP-AAAAMMJJ-000001"
              />
            </div>

            <div class="space-y-1">
              <label class="block text-[11px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ i18n.t('patients.phone') }}</label>
              <input
                type="tel"
                required
                [(ngModel)]="phone"
                name="phone"
                class="w-full min-h-[46px] px-4 py-2 border border-[var(--app-border)] bg-[var(--bg-input)] dark:bg-slate-900 rounded-[var(--radius-brand-md)] text-[var(--text-primary)] focus:outline-hidden focus:border-[var(--brand-primary)] focus:bg-white dark:focus:bg-slate-950 text-sm"
                placeholder="+237 699 99 99 99"
              />
            </div>

            <div class="space-y-1">
              <label class="block text-[11px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ i18n.t('patients.birthDate') }}</label>
              <input
                type="date"
                required
                [(ngModel)]="birthDate"
                name="birthDate"
                class="w-full min-h-[46px] px-4 py-2 border border-[var(--app-border)] bg-[var(--bg-input)] dark:bg-slate-900 rounded-[var(--radius-brand-md)] text-[var(--text-primary)] focus:outline-hidden focus:border-[var(--brand-primary)] focus:bg-white dark:focus:bg-slate-950 text-sm"
              />
            </div>

            @if (error()) {
              <div class="p-3 border border-[var(--brand-danger-border)] dark:border-red-950/20 rounded-[var(--radius-brand-md)] bg-[var(--brand-danger-subtle)]/50 dark:bg-red-950/10 text-xs text-[var(--brand-danger-text)] flex items-center gap-2 font-medium">
                {{ error() }}
              </div>
            }

            <button
              class="w-full min-h-[46px] flex items-center justify-center gap-2 py-3 px-4 rounded-[var(--radius-brand-md)] text-sm font-semibold text-white bg-[var(--brand-primary)] hover:bg-[var(--brand-primary-hover)] focus:outline-hidden transition-all cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
              type="submit"
              [disabled]="loading() || !globalPatientNumber || !phone || !birthDate"
            >
              @if (loading()) {
                {{ i18n.t('patient.login.requesting') }}
              } @else {
                {{ i18n.t('patient.login.submit') }}
              }
            </button>
          </form>
        } @else {
          <!-- Step 2: Input OTP -->
          <form class="w-full space-y-4" (submit)="$event.preventDefault(); verifyOtp()">
            <div class="space-y-1">
              <label class="block text-[11px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ i18n.t('patient.login.otp') }}</label>
              <input
                type="text"
                required
                [(ngModel)]="otpCode"
                name="otpCode"
                maxlength="6"
                class="w-full min-h-[46px] px-4 py-2 border border-[var(--app-border)] bg-[var(--bg-input)] dark:bg-slate-900 rounded-[var(--radius-brand-md)] text-[var(--text-primary)] focus:outline-hidden focus:border-[var(--brand-primary)] focus:bg-white dark:focus:bg-slate-950 text-center text-xl font-bold tracking-widest"
                placeholder="000000"
              />
            </div>

            @if (error()) {
              <div class="p-3 border border-[var(--brand-danger-border)] dark:border-red-950/20 rounded-[var(--radius-brand-md)] bg-[var(--brand-danger-subtle)]/50 dark:bg-red-950/10 text-xs text-[var(--brand-danger-text)] flex items-center gap-2 font-medium">
                {{ error() }}
              </div>
            }

            <button
              class="w-full min-h-[46px] flex items-center justify-center gap-2 py-3 px-4 rounded-[var(--radius-brand-md)] text-sm font-semibold text-white bg-[var(--brand-primary)] hover:bg-[var(--brand-primary-hover)] focus:outline-hidden transition-all cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
              type="submit"
              [disabled]="loading() || otpCode.length !== 6"
            >
              @if (loading()) {
                {{ i18n.t('patient.login.verifying') }}
              } @else {
                {{ i18n.t('patient.login.verify') }}
              }
            </button>

            <button
              type="button"
              (click)="step.set(1); error.set('')"
              class="w-full text-center text-xs font-semibold text-[var(--brand-primary)] hover:underline cursor-pointer"
            >
              {{ i18n.t('patient.login.back') }}
            </button>
          </form>
        }

        <div class="mt-6 text-center text-xs text-[var(--text-muted)]">
          {{ i18n.t('patient.login.clinicLogin') }}
          <a routerLink="/" class="text-[var(--brand-primary)] hover:underline font-semibold ml-1">{{ i18n.t('patient.login.clinicSpace') }}</a>
        </div>
      </div>
    </main>
  `
})
export class PatientLoginComponent {
  private readonly portalService = inject(PatientPortalService);
  private readonly tokenStorage = inject(AuthTokenStorageService);
  private readonly router = inject(Router);
  readonly i18n = inject(I18nService);

  readonly step = signal<number>(1);
  readonly loading = signal(false);
  readonly error = signal('');

  // Form bindings — propriétés string simples requises pour [(ngModel)]
  // Les signals WritableSignal ne sont pas compatibles avec [(ngModel)] car ngModel
  // réassigne la référence au lieu d'appeler .set(), vidant ainsi les valeurs.
  globalPatientNumber = '';
  phone = '';
  birthDate = '';
  otpCode = '';

  constructor() {
    effect(() => {
      const session = this.tokenStorage.session();
      if (session && session.role.split(',').map((role) => role.trim()).includes('PATIENT')) {
        this.router.navigate(['/patient/dashboard']);
      }
    });
  }

  requestOtp(): void {
    this.loading.set(true);
    this.error.set('');
    this.portalService.requestOtp({
      globalPatientNumber: this.globalPatientNumber.trim().toUpperCase(),
      phone: this.phone.trim(),
      birthDate: this.birthDate
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.step.set(2);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.detail || this.i18n.t('patient.login.error.invalidCredentials'));
      }
    });
  }

  verifyOtp(): void {
    this.loading.set(true);
    this.error.set('');
    this.portalService.verifyOtp({
      globalPatientNumber: this.globalPatientNumber.trim().toUpperCase(),
      otpCode: this.otpCode.trim()
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/patient/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.detail || this.i18n.t('patient.login.error.invalidOtp'));
      }
    });
  }
}
