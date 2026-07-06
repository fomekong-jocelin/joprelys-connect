import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { PatientProfileCardComponent } from './components/patient-profile-card.component';
import { PatientPortalMeResponse, PatientPortalService } from './services/patient-portal.service';
import { I18nService } from '../../core/i18n/i18n.service';

@Component({
  selector: 'app-patient-dashboard',
  standalone: true,
  imports: [AppShellComponent, PatientProfileCardComponent, RouterLink],
  template: `
    <app-shell>
      <div class="app-container py-6 flex flex-col gap-6">
        <!-- Welcome Section -->
        <div class="bg-gradient-to-r from-[var(--brand-primary)]/10 to-[var(--brand-primary)]/5 p-6 rounded-lg border border-[var(--app-border)]">
          <h1 class="font-display text-2xl lg:text-3xl font-extrabold text-brand-night dark:text-white">
            @if (patientData()) {
              {{ i18n.t('patient.dashboard.greeting') }}, {{ (patientData()!.fullName.split(' ')[0]) || patientData()!.fullName }}
            } @else {
              {{ i18n.t('patient.dashboard.title') }}
            }
          </h1>
          <p class="text-sm text-[var(--text-secondary)] mt-2">
            {{ i18n.t('patient.dashboard.welcomeDescription') }}
          </p>
        </div>

        @if (isLoading()) {
          <div class="flex items-center justify-center p-12">
            <div class="w-8 h-8 border-4 border-[var(--brand-primary)] border-t-transparent rounded-full animate-spin"></div>
          </div>
        } @else if (error()) {
          <div class="p-4 rounded-[var(--radius-brand-md)] bg-rose-50 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-800/40 text-rose-700 dark:text-rose-300 text-sm font-semibold">
            {{ error() }}
          </div>
        } @else if (patientData(); as p) {
          <div class="grid grid-cols-1 lg:grid-cols-[360px_1fr] gap-6 items-start">
            <!-- Left Side: Profile Card -->
            <div class="space-y-4">
              <h2 class="text-xs font-black uppercase tracking-wider text-slate-400 dark:text-slate-500">
                {{ i18n.t('patient.dashboard.profileCardTitle') }}
              </h2>
              <app-patient-profile-card [patient]="p" />
            </div>

            <!-- Right Side: Quick Cards Grid -->
            <div class="space-y-4">
              <h2 class="text-xs font-black uppercase tracking-wider text-slate-400 dark:text-slate-500">
                {{ i18n.t('patient.dashboard.shortcutsTitle') }}
              </h2>
              
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <!-- Prescriptions Quick Card -->
                <div class="bg-[var(--app-surface)] border border-[var(--app-border)] p-5 rounded-lg flex flex-col justify-between h-44 shadow-sm hover:shadow-md transition-shadow">
                  <div>
                    <div class="flex items-center justify-between">
                      <h3 class="font-display font-extrabold text-slate-800 dark:text-white text-base">{{ i18n.t('patient.dashboard.card.prescriptions.title') }}</h3>
                      <span class="px-2 py-0.5 rounded text-[10px] font-extrabold bg-[var(--brand-primary)]/10 text-[var(--brand-primary)]">
                        {{ p.consultations.length }} {{ i18n.t(p.consultations.length > 1 ? 'patient.dashboard.card.prescriptions.activePlural' : 'patient.dashboard.card.prescriptions.active') }}
                      </span>
                    </div>
                    <p class="text-xs text-[var(--text-secondary)] mt-2 leading-relaxed">
                      {{ i18n.t('patient.dashboard.card.prescriptions.description') }}
                    </p>
                  </div>
                  <a routerLink="/patient/prescriptions" class="text-xs font-extrabold text-[var(--brand-primary)] hover:underline flex items-center gap-1.5 no-underline mt-4">
                    {{ i18n.t('patient.dashboard.card.prescriptions.link') }}
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="w-3.5 h-3.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="m8.25 4.5 7.5 7.5-7.5 7.5" />
                    </svg>
                  </a>
                </div>

                <!-- Consents Quick Card -->
                <div class="bg-[var(--app-surface)] border border-[var(--app-border)] p-5 rounded-lg flex flex-col justify-between h-44 shadow-sm hover:shadow-md transition-shadow">
                  <div>
                    <div class="flex items-center justify-between">
                      <h3 class="font-display font-extrabold text-slate-800 dark:text-white text-base">{{ i18n.t('patient.dashboard.card.consents.title') }}</h3>
                      <span class="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
                    </div>
                    <p class="text-xs text-[var(--text-secondary)] mt-2 leading-relaxed">
                      {{ i18n.t('patient.dashboard.card.consents.description') }}
                    </p>
                  </div>
                  <a routerLink="/patient/consents" class="text-xs font-extrabold text-[var(--brand-primary)] hover:underline flex items-center gap-1.5 no-underline mt-4">
                    {{ i18n.t('patient.dashboard.card.consents.link') }}
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="w-3.5 h-3.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="m8.25 4.5 7.5 7.5-7.5 7.5" />
                    </svg>
                  </a>
                </div>

                <!-- Audit Quick Card -->
                <div class="bg-[var(--app-surface)] border border-[var(--app-border)] p-5 rounded-lg flex flex-col justify-between h-44 shadow-sm hover:shadow-md transition-shadow">
                  <div>
                    <div class="flex items-center justify-between">
                      <h3 class="font-display font-extrabold text-slate-800 dark:text-white text-base">{{ i18n.t('patient.dashboard.card.audit.title') }}</h3>
                      <span class="px-2 py-0.5 rounded text-[10px] font-extrabold bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300">
                        {{ i18n.t('patient.dashboard.card.audit.badge') }}
                      </span>
                    </div>
                    <p class="text-xs text-[var(--text-secondary)] mt-2 leading-relaxed">
                      {{ i18n.t('patient.dashboard.card.audit.description') }}
                    </p>
                  </div>
                  <a routerLink="/patient/audit" class="text-xs font-extrabold text-[var(--brand-primary)] hover:underline flex items-center gap-1.5 no-underline mt-4">
                    {{ i18n.t('patient.dashboard.card.audit.link') }}
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="w-3.5 h-3.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="m8.25 4.5 7.5 7.5-7.5 7.5" />
                    </svg>
                  </a>
                </div>

                <!-- Requests Quick Card -->
                <div class="bg-[var(--app-surface)] border border-[var(--app-border)] p-5 rounded-lg flex flex-col justify-between h-44 shadow-sm hover:shadow-md transition-shadow">
                  <div>
                    <div class="flex items-center justify-between">
                      <h3 class="font-display font-extrabold text-slate-800 dark:text-white text-base">{{ i18n.t('patient.dashboard.card.requests.title') }}</h3>
                      <span class="px-2 py-0.5 rounded text-[10px] font-extrabold bg-indigo-50 text-indigo-600 dark:bg-indigo-950/20 dark:text-indigo-400">
                        {{ i18n.t('patient.dashboard.card.requests.badge') }}
                      </span>
                    </div>
                    <p class="text-xs text-[var(--text-secondary)] mt-2 leading-relaxed">
                      {{ i18n.t('patient.dashboard.card.requests.description') }}
                    </p>
                  </div>
                  <a routerLink="/patient/requests" class="text-xs font-extrabold text-[var(--brand-primary)] hover:underline flex items-center gap-1.5 no-underline mt-4">
                    {{ i18n.t('patient.dashboard.card.requests.link') }}
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="w-3.5 h-3.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="m8.25 4.5 7.5 7.5-7.5 7.5" />
                    </svg>
                  </a>
                </div>

                <!-- Results Quick Card -->
                <div class="bg-[var(--app-surface)] border border-[var(--app-border)] p-5 rounded-lg flex flex-col justify-between h-44 shadow-sm hover:shadow-md transition-shadow">
                  <div>
                    <div class="flex items-center justify-between">
                      <h3 class="font-display font-extrabold text-slate-800 dark:text-white text-base">{{ i18n.t('patient.dashboard.card.results.title') }}</h3>
                      <span class="px-2 py-0.5 rounded text-[10px] font-extrabold bg-emerald-50 text-emerald-600 dark:bg-emerald-950/20 dark:text-emerald-400">
                        {{ i18n.t('patient.dashboard.card.results.badge') }}
                      </span>
                    </div>
                    <p class="text-xs text-[var(--text-secondary)] mt-2 leading-relaxed">
                      {{ i18n.t('patient.dashboard.card.results.description') }}
                    </p>
                  </div>
                  <a routerLink="/patient/results" class="text-xs font-extrabold text-[var(--brand-primary)] hover:underline flex items-center gap-1.5 no-underline mt-4">
                    {{ i18n.t('patient.dashboard.card.results.link') }}
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="w-3.5 h-3.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="m8.25 4.5 7.5 7.5-7.5 7.5" />
                    </svg>
                  </a>
                </div>
              </div>
            </div>
          </div>
        }
      </div>
    </app-shell>
  `
})
export class PatientDashboardComponent implements OnInit {
  private readonly portalService = inject(PatientPortalService);
  readonly i18n = inject(I18nService);

  readonly patientData = signal<PatientPortalMeResponse | null>(null);
  readonly isLoading = signal(false);
  readonly error = signal('');

  ngOnInit(): void {
    this.loadPatientData();
  }

  loadPatientData(): void {
    this.isLoading.set(true);
    this.error.set('');
    this.portalService.getMe().subscribe({
      next: (data) => {
        this.patientData.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.error.set(err.error?.detail || this.i18n.t('patients.loadError'));
      }
    });
  }
}
