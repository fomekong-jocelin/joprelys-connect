import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthApiService } from './auth-api.service';
import { I18nService } from '../core/i18n/i18n.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [RouterLink],
  template: `
    <main class="min-h-screen flex flex-col items-center justify-center p-6 bg-white dark:bg-slate-950 transition-colors duration-300 relative">
      <!-- Sélecteur de langue -->
      <div class="fixed top-4 right-4 z-50">
        <div class="flex items-center gap-1.5 text-xs font-bold border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 px-2.5 py-1.5 rounded-sm shadow-sm select-none">
          <button type="button" (click)="setLang('fr')" [class]="locale() === 'fr' ? 'text-brand-cyan font-extrabold pointer-events-none' : 'text-slate-400 dark:text-slate-500 hover:text-slate-600 dark:hover:text-slate-300 cursor-pointer'">FR</button>
          <span class="text-slate-300 dark:text-slate-700">|</span>
          <button type="button" (click)="setLang('en')" [class]="locale() === 'en' ? 'text-brand-cyan font-extrabold pointer-events-none' : 'text-slate-400 dark:text-slate-500 hover:text-slate-600 dark:hover:text-slate-300 cursor-pointer'">EN</button>
        </div>
      </div>

      <div class="w-full max-w-[420px] flex flex-col items-center py-8">
        <!-- Logo -->
        <div class="flex items-center gap-2.5 mb-8">
          <svg class="h-9 w-9 shrink-0" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M30 65 C15 50, 15 30, 30 15 C45 0, 65 0, 80 15 C95 30, 95 50, 80 65 L65 80" stroke="url(#logo-grad)" stroke-width="12" stroke-linecap="round" fill="none"/>
            <path d="M70 35 C85 50, 85 70, 70 85 C55 100, 35 100, 20 85 C5 70, 5 50, 20 35 L35 20" stroke="url(#logo-grad-reverse)" stroke-width="12" stroke-linecap="round" fill="none"/>
            <defs>
              <linearGradient id="logo-grad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#0B91B2" />
                <stop offset="100%" stop-color="#0A1D3D" />
              </linearGradient>
              <linearGradient id="logo-grad-reverse" x1="100%" y1="100%" x2="0%" y2="0%">
                <stop offset="0%" stop-color="#0B91B2" />
                <stop offset="100%" stop-color="#16A34A" />
              </linearGradient>
            </defs>
          </svg>
          <span class="font-display font-bold text-2xl tracking-tight text-brand-night dark:text-white">
            Joprelys<span class="text-brand-cyan">Connect</span>
          </span>
        </div>

        <div class="w-full bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800/60 p-6 rounded-[var(--radius-brand-md)] shadow-sm">
          @if (step() === 1) {
            <div class="text-center mb-6">
              <h2 class="font-display font-bold text-xl text-brand-night dark:text-white mb-1">{{ t('auth.forgotPassword.title') }}</h2>
              <p class="text-xs text-slate-500 dark:text-slate-400">{{ t('auth.forgotPassword.subtitle') }}</p>
            </div>

            <form class="space-y-4" (submit)="$event.preventDefault(); submitRequest()">
              <div class="space-y-1.5">
                <label class="block text-[10px] font-bold text-slate-400 dark:text-slate-400 uppercase tracking-wider">{{ t('auth.forgotPassword.email') }}</label>
                <input type="email" [value]="email()" (input)="updateEmail($event)" required class="w-full min-h-[44px] px-4 py-2 border border-slate-100 dark:border-slate-800 bg-brand-gray dark:bg-slate-950 rounded-[var(--radius-brand-md)] text-slate-900 dark:text-white placeholder-slate-400 focus:outline-hidden focus:border-brand-cyan text-sm" placeholder="nom@clinique.com" />
              </div>

              @if (error()) {
                <div class="p-3 bg-red-50 dark:bg-red-950/20 text-xs text-red-700 dark:text-red-400 rounded-md font-semibold">{{ error() }}</div>
              }

              <button type="submit" [disabled]="!canSubmitEmail()" class="w-full min-h-[44px] flex items-center justify-center py-2.5 px-4 rounded-[var(--radius-brand-md)] text-sm font-semibold text-white bg-brand-cyan hover:bg-[#097b98] transition-colors cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed">
                @if (loading()) { {{ t('common.saving') }} } @else { {{ t('auth.forgotPassword.submitRequest') }} }
              </button>
            </form>
          } @else if (step() === 2) {
            <div class="text-center mb-6">
              <h2 class="font-display font-bold text-xl text-brand-night dark:text-white mb-1">{{ t('auth.forgotPassword.otpTitle') }}</h2>
              <p class="text-xs text-slate-500 dark:text-slate-400">{{ t('auth.forgotPassword.otpSubtitle') }}</p>
            </div>

            <form class="space-y-4" (submit)="$event.preventDefault(); submitReset()">
              <div class="space-y-1.5">
                <label class="block text-[10px] font-bold text-slate-400 dark:text-slate-400 uppercase tracking-wider">{{ t('auth.forgotPassword.code') }}</label>
                <input type="text" [value]="otpCode()" (input)="updateOtpCode($event)" required class="w-full min-h-[44px] px-4 py-2 border border-slate-100 dark:border-slate-800 bg-brand-gray dark:bg-slate-950 rounded-[var(--radius-brand-md)] text-slate-900 dark:text-white placeholder-slate-400 focus:outline-hidden focus:border-brand-cyan text-sm" placeholder="E.g. 123456" />
              </div>

              <div class="space-y-1.5">
                <label class="block text-[10px] font-bold text-slate-400 dark:text-slate-400 uppercase tracking-wider">{{ t('auth.forgotPassword.newPassword') }}</label>
                <input type="password" [value]="newPassword()" (input)="updateNewPassword($event)" required class="w-full min-h-[44px] px-4 py-2 border border-slate-100 dark:border-slate-800 bg-brand-gray dark:bg-slate-950 rounded-[var(--radius-brand-md)] text-slate-900 dark:text-white placeholder-slate-400 focus:outline-hidden focus:border-brand-cyan text-sm" placeholder="••••••••" />
              </div>

              <div class="space-y-1.5">
                <label class="block text-[10px] font-bold text-slate-400 dark:text-slate-400 uppercase tracking-wider">{{ t('auth.forgotPassword.confirmPassword') }}</label>
                <input type="password" [value]="confirmPassword()" (input)="updateConfirmPassword($event)" required class="w-full min-h-[44px] px-4 py-2 border border-slate-100 dark:border-slate-800 bg-brand-gray dark:bg-slate-950 rounded-[var(--radius-brand-md)] text-slate-900 dark:text-white placeholder-slate-400 focus:outline-hidden focus:border-brand-cyan text-sm" placeholder="••••••••" />
              </div>

              @if (error()) {
                <div class="p-3 bg-red-50 dark:bg-red-950/20 text-xs text-red-700 dark:text-red-400 rounded-md font-semibold">{{ error() }}</div>
              }

              <button type="submit" [disabled]="!canSubmitReset()" class="w-full min-h-[44px] flex items-center justify-center py-2.5 px-4 rounded-[var(--radius-brand-md)] text-sm font-semibold text-white bg-brand-cyan hover:bg-[#097b98] transition-colors cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed">
                @if (loading()) { {{ t('common.saving') }} } @else { {{ t('auth.forgotPassword.submitReset') }} }
              </button>
            </form>
          } @else if (step() === 3) {
            <div class="text-center py-4 space-y-4">
              <span class="inline-flex items-center justify-center w-12 h-12 rounded-full bg-emerald-100 text-emerald-600 dark:bg-emerald-950/40 dark:text-emerald-400 text-2xl">✓</span>
              <h2 class="font-display font-bold text-xl text-brand-night dark:text-white mb-1">{{ t('auth.forgotPassword.successTitle') }}</h2>
              <p class="text-xs text-slate-500 dark:text-slate-400 leading-relaxed">{{ t('auth.forgotPassword.successDesc') }}</p>
              <button (click)="goToLogin()" class="w-full min-h-[44px] flex items-center justify-center py-2.5 px-4 rounded-[var(--radius-brand-md)] text-sm font-semibold text-white bg-brand-cyan hover:bg-[#097b98] transition-colors cursor-pointer">
                {{ t('auth.forgotPassword.backToLogin') }}
              </button>
            </div>
          }

          @if (step() < 3) {
            <div class="mt-4 pt-4 border-t border-slate-100 dark:border-slate-800/60 text-center">
              <a routerLink="/" class="text-xs font-semibold text-slate-500 dark:text-slate-400 hover:text-brand-cyan dark:hover:text-brand-cyan transition-colors">
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
      next: () => {
        this.loading.set(false);
        this.step.set(2);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.detail || err.error?.title || 'Une erreur est survenue.');
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
        this.error.set(err.error?.detail || err.error?.title || 'Code incorrect ou expiré.');
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
