import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthApiService } from './auth-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { AppLogoComponent } from '../shared/ui/app-logo.component';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [RouterLink, AppLogoComponent],
  template: `
    <main class="min-h-screen flex flex-col items-center justify-center p-6 transition-colors duration-300 relative"
      style="background:var(--app-bg); color:var(--text-primary)">
      <!-- Sélecteur de langue -->
      <div class="fixed top-4 right-4 z-50">
        <div class="flex items-center gap-1.5 text-xs font-bold border border-[var(--app-border)] bg-[var(--app-surface)] px-2.5 py-1.5 rounded-sm shadow-sm select-none">
          <button type="button" (click)="setLang('fr')" [class]="locale() === 'fr' ? 'text-brand-cyan font-extrabold pointer-events-none' : 'text-[var(--text-muted)] hover:text-[var(--text-secondary)] cursor-pointer'">FR</button>
          <span style="color:var(--divider)">|</span>
          <button type="button" (click)="setLang('en')" [class]="locale() === 'en' ? 'text-brand-cyan font-extrabold pointer-events-none' : 'text-[var(--text-muted)] hover:text-[var(--text-secondary)] cursor-pointer'">EN</button>
        </div>
      </div>

      <div class="w-full max-w-[420px] flex flex-col items-center py-8">
        <!-- Logo -->
        <div class="mb-8">
          <app-logo size="lg"></app-logo>
        </div>

        <div class="ui-card w-full p-6">
          @if (step() === 1) {
            <div class="text-center mb-6">
              <h2 class="font-display font-bold text-xl mb-1" style="color:var(--text-primary)">{{ t('auth.forgotPassword.title') }}</h2>
              <p class="text-xs" style="color:var(--text-muted)">{{ t('auth.forgotPassword.subtitle') }}</p>
            </div>

            <form class="space-y-4" (submit)="$event.preventDefault(); submitRequest()">
              <div class="space-y-1.5">
                <label class="ui-label">{{ t('auth.forgotPassword.email') }}</label>
                <input type="email" [value]="email()" (input)="updateEmail($event)" required aria-required="true" class="ui-input" [placeholder]="t('forgotPassword.emailPlaceholder')" />
              </div>

              @if (error()) {
                <div class="ui-alert-danger" role="alert" aria-live="assertive">{{ error() }}</div>
              }

              <button type="submit" [disabled]="!canSubmitEmail()" class="ui-button ui-button-primary w-full">
                @if (loading()) { {{ t('common.saving') }} } @else { {{ t('auth.forgotPassword.submitRequest') }} }
              </button>
            </form>
          } @else if (step() === 2) {
            <div class="text-center mb-6">
              <h2 class="font-display font-bold text-xl mb-1" style="color:var(--text-primary)">{{ t('auth.forgotPassword.otpTitle') }}</h2>
              <p class="text-xs" style="color:var(--text-muted)">{{ t('auth.forgotPassword.otpSubtitle') }}</p>
            </div>

            @if (receivedOtpCode(); as code) {
              <div class="mb-4 p-3 rounded-[var(--radius-brand-md)] border" style="background:var(--brand-warning-subtle); border-color:var(--brand-warning-border)">
                <p class="text-[10px] font-bold uppercase tracking-wider mb-1" style="color:var(--brand-warning-text)">{{ t('auth.forgotPassword.pilotWarning') }}</p>
                <p class="text-xs" style="color:var(--brand-warning-text)">{{ t('auth.forgotPassword.pilotDesc') }}</p>
                <code class="mt-2 inline-flex rounded-sm px-3 py-1.5 font-mono text-lg font-extrabold"
                  style="background:var(--brand-warning-code-bg); color:var(--brand-warning-text)">
                  {{ code }}
                </code>
              </div>
            }

            <form class="space-y-4" (submit)="$event.preventDefault(); submitReset()">
              <div class="space-y-1.5">
                <label class="ui-label">{{ t('auth.forgotPassword.code') }}</label>
                <input type="text" [value]="otpCode()" (input)="updateOtpCode($event)" required aria-required="true" class="ui-input" [placeholder]="t('forgotPassword.otpPlaceholder')" />
              </div>

              <div class="space-y-1.5">
                <label class="ui-label">{{ t('auth.forgotPassword.newPassword') }}</label>
                <input type="password" [value]="newPassword()" (input)="updateNewPassword($event)" required aria-required="true" class="ui-input" placeholder="••••••••" />
              </div>

              <div class="space-y-1.5">
                <label class="ui-label">{{ t('auth.forgotPassword.confirmPassword') }}</label>
                <input type="password" [value]="confirmPassword()" (input)="updateConfirmPassword($event)" required aria-required="true" class="ui-input" placeholder="••••••••" />
              </div>

              @if (error()) {
                <div class="ui-alert-danger" role="alert" aria-live="assertive">{{ error() }}</div>
              }

              <button type="submit" [disabled]="!canSubmitReset()" class="ui-button ui-button-primary w-full">
                @if (loading()) { {{ t('common.saving') }} } @else { {{ t('auth.forgotPassword.submitReset') }} }
              </button>
            </form>
          } @else if (step() === 3) {
            <div class="text-center py-4 space-y-4">
              <span class="inline-flex items-center justify-center w-12 h-12 rounded-full text-2xl"
                style="background:var(--brand-success-subtle); color:var(--brand-success-text)">✓</span>
              <h2 class="font-display font-bold text-xl mb-1" style="color:var(--text-primary)">{{ t('auth.forgotPassword.successTitle') }}</h2>
              <p class="text-xs leading-relaxed" style="color:var(--text-muted)">{{ t('auth.forgotPassword.successDesc') }}</p>
              <button (click)="goToLogin()" class="ui-button ui-button-primary w-full">
                {{ t('auth.forgotPassword.backToLogin') }}
              </button>
            </div>
          }

          @if (step() < 3) {
            <div class="mt-4 pt-4 border-t border-[var(--app-border)] text-center">
              <a routerLink="/" class="ui-link text-xs">
                ← {{ t('auth.forgotPassword.backToLogin') }}
              </a>
            </div>
          }
        </div>
      </div>
    </main>
  `
})
export class ForgotPasswordComponent {
  private readonly authApi = inject(AuthApiService);
  private readonly i18n = inject(I18nService);
  private readonly router = inject(Router);

  readonly locale = this.i18n.locale;
  readonly step = signal<1 | 2 | 3>(1);
  readonly email = signal('');
  readonly otpCode = signal('');
  readonly newPassword = signal('');
  readonly confirmPassword = signal('');
  readonly error = signal<string | null>(null);
  readonly loading = signal(false);
  readonly receivedOtpCode = signal<string | null>(null);

  readonly canSubmitEmail = computed(() =>
    this.isValidEmail(this.email()) && !this.loading()
  );

  readonly canSubmitReset = computed(() =>
    this.otpCode().trim().length > 0 &&
    this.newPassword().length >= 8 &&
    this.confirmPassword().length >= 8 &&
    !this.loading()
  );

  setLang(lang: any): void {
    this.i18n.setLocale(lang);
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  submitRequest(): void {
    this.error.set(null);
    this.loading.set(true);

    this.authApi.requestPasswordRecovery(this.email()).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.receivedOtpCode.set(res.otpCode);
        this.step.set(2);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.detail || err.error?.title || this.t('forgotPassword.error.generic'));
      }
    });
  }

  submitReset(): void {
    this.error.set(null);

    if (this.newPassword() !== this.confirmPassword()) {
      this.error.set(this.t('auth.forgotPassword.error.mismatch'));
      return;
    }

    this.loading.set(true);

    const payload = {
      email: this.email(),
      otpCode: this.otpCode(),
      newPassword: this.newPassword()
    };

    this.authApi.resetPassword(payload).subscribe({
      next: () => {
        this.loading.set(false);
        this.step.set(3);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.detail || err.error?.title || this.t('forgotPassword.error.invalidCode'));
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/']);
  }

  updateEmail(e: Event): void { this.email.set(this.inputValue(e)); }
  updateOtpCode(e: Event): void { this.otpCode.set(this.inputValue(e)); }
  updateNewPassword(e: Event): void { this.newPassword.set(this.inputValue(e)); }
  updateConfirmPassword(e: Event): void { this.confirmPassword.set(this.inputValue(e)); }

  private inputValue(e: Event): string {
    return e.target instanceof HTMLInputElement ? e.target.value : '';
  }

  private isValidEmail(value: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
  }
}
