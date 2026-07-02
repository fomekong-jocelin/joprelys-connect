import { Component, inject, signal, effect } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { PatientPortalService } from './services/patient-portal.service';
import { AuthTokenStorageService } from '../../auth/auth-token-storage.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-patient-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <main class="min-h-screen flex flex-col items-center justify-center p-6 bg-white dark:bg-slate-950 transition-colors duration-300">
      <div class="w-full max-w-[400px] flex flex-col items-center py-8">
        <!-- Logo -->
        <div class="flex items-center gap-2.5 mb-8">
          <svg class="h-9 w-9 shrink-0" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
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
          <span class="font-display font-bold text-2xl tracking-tight text-[#0A1D3D] dark:text-white">
            Joprelys<span class="text-[#0B91B2]">Connect</span>
          </span>
        </div>

        <div class="w-full text-center mb-6">
          <h2 class="font-display font-bold text-2xl tracking-tight text-[#0A1D3D] dark:text-white mb-2">
            Espace Patient Sécurisé
          </h2>
          <p class="text-sm text-slate-500 dark:text-slate-400 font-medium">
            @if (step() === 1) {
              Connectez-vous pour accéder à vos ordonnances
            } @else {
              Saisissez le code de sécurité envoyé à votre clinique
            }
          </p>
        </div>

        <!-- Step 1: Input Credentials -->
        @if (step() === 1) {
          <form class="w-full space-y-4" (submit)="$event.preventDefault(); requestOtp()">
            <div class="space-y-1">
              <label class="block text-[11px] font-bold text-slate-400 uppercase tracking-wider">Numéro DPU</label>
              <input
                type="text"
                required
                [(ngModel)]="globalPatientNumber"
                name="globalPatientNumber"
                class="w-full min-h-[46px] px-4 py-2 border border-slate-100 dark:border-slate-800 bg-[var(--bg-input)] dark:bg-slate-900 rounded-[var(--radius-brand-md)] text-slate-900 dark:text-white focus:outline-hidden focus:border-[var(--brand-primary)] focus:bg-white dark:focus:bg-slate-950 text-sm"
                placeholder="PAT-YYYYMMDD-XXXXXX"
              />
            </div>

            <div class="space-y-1">
              <label class="block text-[11px] font-bold text-slate-400 uppercase tracking-wider">Téléphone</label>
              <input
                type="tel"
                required
                [(ngModel)]="phone"
                name="phone"
                class="w-full min-h-[46px] px-4 py-2 border border-slate-100 dark:border-slate-800 bg-[var(--bg-input)] dark:bg-slate-900 rounded-[var(--radius-brand-md)] text-slate-900 dark:text-white focus:outline-hidden focus:border-[var(--brand-primary)] focus:bg-white dark:focus:bg-slate-950 text-sm"
                placeholder="+237 699 99 99 99"
              />
            </div>

            <div class="space-y-1">
              <label class="block text-[11px] font-bold text-slate-400 uppercase tracking-wider">Date de naissance</label>
              <input
                type="date"
                required
                [(ngModel)]="birthDate"
                name="birthDate"
                class="w-full min-h-[46px] px-4 py-2 border border-slate-100 dark:border-slate-800 bg-[var(--bg-input)] dark:bg-slate-900 rounded-[var(--radius-brand-md)] text-slate-900 dark:text-white focus:outline-hidden focus:border-[var(--brand-primary)] focus:bg-white dark:focus:bg-slate-950 text-sm"
              />
            </div>

            @if (error()) {
              <div class="p-3 border border-red-100 dark:border-red-950/20 rounded-[var(--radius-brand-md)] bg-red-50/50 dark:bg-red-950/10 text-xs text-red-700 dark:text-red-400 flex items-center gap-2 font-medium">
                {{ error() }}
              </div>
            }

            <button
              class="w-full min-h-[46px] flex items-center justify-center gap-2 py-3 px-4 rounded-[var(--radius-brand-md)] text-sm font-semibold text-white bg-[var(--brand-primary)] hover:bg-[var(--brand-primary-hover)] focus:outline-hidden transition-all cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
              type="submit"
              [disabled]="loading() || !globalPatientNumber() || !phone() || !birthDate()"
            >
              @if (loading()) {
                Demande en cours...
              } @else {
                Recevoir le code de sécurité
              }
            </button>
          </form>
        } @else {
          <!-- Step 2: Input OTP -->
          <form class="w-full space-y-4" (submit)="$event.preventDefault(); verifyOtp()">
            <div class="space-y-1">
              <label class="block text-[11px] font-bold text-slate-400 uppercase tracking-wider">Code de sécurité (OTP)</label>
              <input
                type="text"
                required
                [(ngModel)]="otpCode"
                name="otpCode"
                maxlength="6"
                class="w-full min-h-[46px] px-4 py-2 border border-slate-100 dark:border-slate-800 bg-[var(--bg-input)] dark:bg-slate-900 rounded-[var(--radius-brand-md)] text-slate-900 dark:text-white focus:outline-hidden focus:border-[var(--brand-primary)] focus:bg-white dark:focus:bg-slate-950 text-center text-xl font-bold tracking-widest"
                placeholder="000000"
              />
            </div>

            @if (error()) {
              <div class="p-3 border border-red-100 dark:border-red-950/20 rounded-[var(--radius-brand-md)] bg-red-50/50 dark:bg-red-950/10 text-xs text-red-700 dark:text-red-400 flex items-center gap-2 font-medium">
                {{ error() }}
              </div>
            }

            <button
              class="w-full min-h-[46px] flex items-center justify-center gap-2 py-3 px-4 rounded-[var(--radius-brand-md)] text-sm font-semibold text-white bg-[var(--brand-primary)] hover:bg-[var(--brand-primary-hover)] focus:outline-hidden transition-all cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
              type="submit"
              [disabled]="loading() || otpCode().length !== 6"
            >
              @if (loading()) {
                Vérification...
              } @else {
                Se connecter
              }
            </button>

            <button
              type="button"
              (click)="step.set(1); error.set('')"
              class="w-full text-center text-xs font-semibold text-[var(--brand-primary)] hover:underline cursor-pointer"
            >
              Retour à l'étape précédente
            </button>
          </form>
        }

        <div class="mt-6 text-center text-xs text-slate-500">
          Connexion clinique ?
          <a routerLink="/" class="text-[var(--brand-primary)] hover:underline font-semibold ml-1">Espace Clinique</a>
        </div>
      </div>
    </main>
  `
})
export class PatientLoginComponent {
  private readonly portalService = inject(PatientPortalService);
  private readonly tokenStorage = inject(AuthTokenStorageService);
  private readonly router = inject(Router);

  readonly step = signal<number>(1);
  readonly loading = signal(false);
  readonly error = signal('');

  // Form bindings
  globalPatientNumber = signal('');
  phone = signal('');
  birthDate = signal('');
  otpCode = signal('');

  constructor() {
    effect(() => {
      const session = this.tokenStorage.session();
      if (session && session.role === 'PATIENT') {
        this.router.navigate(['/patient/dashboard']);
      }
    });
  }

  requestOtp(): void {
    this.loading.set(true);
    this.error.set('');
    this.portalService.requestOtp({
      globalPatientNumber: this.globalPatientNumber(),
      phone: this.phone(),
      birthDate: this.birthDate()
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.step.set(2);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.detail || "Informations incorrectes.");
      }
    });
  }

  verifyOtp(): void {
    this.loading.set(true);
    this.error.set('');
    this.portalService.verifyOtp({
      globalPatientNumber: this.globalPatientNumber(),
      otpCode: this.otpCode()
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/patient/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.detail || "Code incorrect.");
      }
    });
  }
}
