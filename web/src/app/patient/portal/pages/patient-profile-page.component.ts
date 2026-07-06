import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppShellComponent } from '../../../shared/layout/app-shell.component';
import { PageHeaderComponent } from '../../../shared/ui/page-header.component';
import { PatientPortalMeResponse, PatientPortalService } from '../services/patient-portal.service';
import { I18nService } from '../../../core/i18n/i18n.service';

@Component({
  selector: 'app-patient-profile-page',
  standalone: true,
  imports: [CommonModule, AppShellComponent, PageHeaderComponent],
  template: `
    <app-shell>
      <app-page-header
        [title]="t('patient.profile.title')"
        [subtitle]="t('patient.profile.subtitle')"
      />

      <div class="app-container pb-10">
        @if (isLoading()) {
          <div class="flex items-center justify-center py-12">
            <div class="w-8 h-8 border-4 border-[var(--brand-primary)] border-t-transparent rounded-full animate-spin"></div>
          </div>
        } @else if (error()) {
          <div class="p-4 rounded-[var(--radius-brand-md)] bg-rose-50 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-800/40 text-rose-700 dark:text-rose-300 text-sm font-semibold">
            {{ error() }}
          </div>
        } @else if (patient(); as p) {
          <!-- Premium Profile Banner -->
          <div class="relative overflow-hidden rounded-[var(--radius-brand-lg)] border border-[var(--app-border)] bg-[var(--app-surface)] shadow-[var(--shadow-panel-subtle)] p-6 mb-8 transition-all hover:shadow-[var(--shadow-panel)] duration-300">
            <!-- Mesh Gradient Background Accent -->
            <div class="absolute inset-0 bg-gradient-to-r from-[color-mix(in srgb,var(--brand-primary)_12%,var(--app-surface))] to-[color-mix(in srgb,var(--brand-primary)_4%,var(--app-surface))] opacity-40"></div>
            <div class="absolute -right-16 -top-16 w-48 h-48 rounded-full bg-[var(--brand-primary)] opacity-10 blur-3xl"></div>
            
            <div class="relative flex flex-col md:flex-row items-center gap-6">
              <!-- Big Premium Avatar with initial -->
              <div class="flex-shrink-0 relative">
                <div class="w-24 h-24 rounded-full bg-gradient-to-tr from-[var(--brand-primary)] to-[color-mix(in srgb,var(--brand-primary)_60%,#ffffff)] flex items-center justify-center text-white text-3xl font-black shadow-lg font-display select-none">
                  {{ p.fullName.charAt(0) }}
                </div>
                @if (p.bloodGroup) {
                  <div class="absolute -bottom-1 -right-1 bg-red-600 dark:bg-rose-500 text-white text-xs font-black px-2.5 py-1 rounded-[var(--radius-brand-sm)] shadow-md border border-[var(--app-surface)]">
                    {{ p.bloodGroup }}
                  </div>
                }
              </div>

              <!-- Identity & Summary Info -->
              <div class="flex-grow text-center md:text-left space-y-2">
                <div class="flex flex-col md:flex-row md:items-center gap-3 justify-center md:justify-start">
                  <h2 class="text-2xl font-black font-display tracking-tight" style="color: var(--text-primary)">
                    {{ p.fullName }}
                  </h2>
                  <span class="inline-flex w-fit mx-auto md:mx-0 items-center gap-1.5 px-3 py-1 rounded-[var(--radius-brand-sm)] text-xs font-black bg-[color-mix(in srgb,var(--brand-primary)_12%,var(--app-surface))] text-[var(--brand-primary)] border border-[color-mix(in srgb,var(--brand-primary)_25%,transparent)] select-none uppercase tracking-wider">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="w-3.5 h-3.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12c0 1.268-.63 2.39-1.593 3.068a3.745 3.745 0 0 1-1.043 3.296 3.745 3.745 0 0 1-3.296 1.043A3.745 3.745 0 0 1 12 21c-1.268 0-2.39-.63-3.068-1.593a3.746 3.746 0 0 1-3.296-1.043 3.745 3.745 0 0 1-1.043-3.296A3.745 3.745 0 0 1 3 12c0-1.268.63-2.39 1.593-3.068a3.745 3.745 0 0 1 1.043-3.296 3.746 3.746 0 0 1 3.296-1.043A3.746 3.746 0 0 1 12 3c1.268 0 2.39.63 3.068 1.593a3.746 3.746 0 0 1 3.296 1.043 3.746 3.746 0 0 1 1.043 3.296A3.745 3.745 0 0 1 21 12Z" />
                    </svg>
                    {{ t('patient.profile.activeStatus') }}
                  </span>
                </div>
                
                <div class="flex flex-wrap items-center justify-center md:justify-start gap-x-4 gap-y-1.5 text-sm font-semibold" style="color: var(--text-secondary)">
                  <div class="flex items-center gap-1.5">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4 text-slate-400">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 6a7.5 7.5 0 1 0 7.5 7.5h-7.5V6Z" />
                      <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 10.5H21A7.5 7.5 0 0 0 13.5 3v7.5Z" />
                    </svg>
                    <span class="font-mono text-xs uppercase tracking-wide bg-[var(--app-bg)] px-2 py-0.5 rounded border border-[var(--app-border)]">{{ p.globalPatientNumber }}</span>
                  </div>
                  <span class="text-slate-300 dark:text-slate-700">|</span>
                  <div class="flex items-center gap-1.5">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4 text-slate-400">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 0 1 2.25-2.25h13.5A2.25 2.25 0 0 1 21 7.5v11.25m-18 0A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75m-18 0v-7.5A2.25 2.25 0 0 1 5.25 9h13.5A2.25 2.25 0 0 1 21 11.25v7.5m-9-6h.008v.008H12v-.008ZM12 15h.008v.008H12V15Zm0 2.25h.008v.008H12v-.008ZM9.75 15h.008v.008H9.75V15Zm0 2.25h.008v.008H9.75v-.008ZM7.5 15h.008v.008H7.5V15Zm0 2.25h.008v.008H7.5v-.008Zm6.75-4.5h.008v.008h-.008v-.008Zm0 2.25h.008v.008h-.008V15Zm0 2.25h.008v.008h-.008v-.008Zm2.25-4.5h.008v.008H16.5v-.008Zm0 2.25h.008v.008H16.5V15Z" />
                    </svg>
                    <span>{{ p.birthDate | date:'dd/MM/yyyy' }}</span>
                  </div>
                  <span class="text-slate-300 dark:text-slate-700">|</span>
                  <div class="flex items-center gap-1.5">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4 text-slate-400">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0A17.933 17.933 0 0 1 12 21.75c-2.676 0-5.216-.584-7.499-1.632Z" />
                    </svg>
                    <span>{{ p.gender }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <!-- Left Column -->
            <div class="lg:col-span-1 space-y-6">
              <!-- Identity Card -->
              <section class="ui-card p-5 md:p-6 transition-all hover:shadow-[var(--shadow-panel)] duration-300">
                <div class="flex items-center gap-2.5 mb-5 pb-3 border-b border-[var(--app-border)]">
                  <div class="w-8 h-8 rounded-sm bg-[color-mix(in srgb,var(--brand-primary)_12%,var(--app-surface))] text-[var(--brand-primary)] flex items-center justify-center">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4.5 h-4.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M15 9h3.75M15 12h3.75M15 15h3.75M4.5 19.5h15a2.25 2.25 0 0 0 2.25-2.25V6.75A2.25 2.25 0 0 0 19.5 4.5h-15a2.25 2.25 0 0 0-2.25 2.25v10.5A2.25 2.25 0 0 0 4.5 19.5Zm6-10.125a1.875 1.875 0 1 1-3.75 0 1.875 1.875 0 0 1 3.75 0Zm-1.25 5.625c0 .484-.338.88-.797.973a5.978 5.978 0 0 0-3.206 0 .973 .973 0 0 1-.797-.973v-.253c0-1.153.816-2.13 1.957-2.29a7.16 7.16 0 0 1 2.05 0c1.14.16 1.956 1.137 1.956 2.29v.253Z" />
                    </svg>
                  </div>
                  <h3 class="ui-title text-base font-bold">{{ t('patient.profile.identity') }}</h3>
                </div>
                
                <div class="space-y-4 text-sm">
                  <div class="ui-card-muted p-3">
                    <span class="ui-label text-[10px]">{{ t('patient.profile.fullName') }}</span>
                    <span class="font-bold text-base block mt-0.5" style="color: var(--text-primary)">{{ p.fullName }}</span>
                  </div>
                  <div class="ui-card-muted p-3">
                    <span class="ui-label text-[10px]">{{ t('patient.profile.globalPatientNumber') }}</span>
                    <span class="font-mono text-xs block mt-0.5 select-all" style="color: var(--text-secondary)">{{ p.globalPatientNumber }}</span>
                  </div>
                  <div class="grid grid-cols-2 gap-3">
                    <div class="ui-card-muted p-3">
                      <span class="ui-label text-[10px]">{{ t('patient.profile.birthDate') }}</span>
                      <span class="font-semibold block mt-0.5" style="color: var(--text-primary)">{{ p.birthDate | date:'dd/MM/yyyy' }}</span>
                    </div>
                    <div class="ui-card-muted p-3">
                      <span class="ui-label text-[10px]">{{ t('patient.profile.gender') }}</span>
                      <span class="font-semibold block mt-0.5" style="color: var(--text-primary)">{{ p.gender }}</span>
                    </div>
                  </div>
                  @if (p.bloodGroup) {
                    <div class="ui-card-muted p-3 flex items-center justify-between">
                      <span class="ui-label text-[10px]">{{ t('patient.profile.bloodGroup') }}</span>
                      <span class="bg-red-50 dark:bg-rose-950/30 text-red-600 dark:text-rose-400 text-xs font-black px-2.5 py-1 rounded-[var(--radius-brand-sm)] border border-red-200 dark:border-rose-900/50">
                        {{ p.bloodGroup }}
                      </span>
                    </div>
                  }
                </div>
              </section>

              <!-- Contact Card -->
              <section class="ui-card p-5 md:p-6 transition-all hover:shadow-[var(--shadow-panel)] duration-300">
                <div class="flex items-center gap-2.5 mb-5 pb-3 border-b border-[var(--app-border)]">
                  <div class="w-8 h-8 rounded-sm bg-[color-mix(in srgb,var(--brand-primary)_12%,var(--app-surface))] text-[var(--brand-primary)] flex items-center justify-center">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4.5 h-4.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 6.75c0 8.284 6.716 15 15 15h2.25a2.25 2.25 0 0 0 2.25-2.25v-1.372c0-.516-.351-.966-.852-1.091l-4.423-1.106c-.44-.11-.902.055-1.173.417l-.97 1.293c-2.824-1.802-5.122-4.1-6.924-6.924l1.293-.97a1.125 1.125 0 0 0 .417-1.173L6.963 3.102a1.125 1.125 0 0 0-1.091-.852H4.5A2.25 2.25 0 0 0 2.25 4.5v2.25Z" />
                    </svg>
                  </div>
                  <h3 class="ui-title text-base font-bold">{{ t('patient.profile.contact') }}</h3>
                </div>
                
                <div class="space-y-3.5 text-sm">
                  <div class="flex items-start gap-3">
                    <div class="w-7 h-7 rounded-full bg-[var(--app-surface-muted)] flex items-center justify-center text-slate-400 mt-0.5 flex-shrink-0">
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-3.5 h-3.5">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 1.5H8.25A2.25 2.25 0 0 0 6 3.75v16.5a2.25 2.25 0 0 0 2.25 2.25h7.5A2.25 2.25 0 0 0 18 20.25V3.75a2.25 2.25 0 0 0-2.25-2.25H13.5m-3 0V3h3V1.5m-3 0h3m-3 18.75h3" />
                      </svg>
                    </div>
                    <div>
                      <span class="ui-label text-[10px]">{{ t('patient.profile.phone') }}</span>
                      <span class="font-semibold block" style="color: var(--text-primary)">{{ p.phone || '-' }}</span>
                    </div>
                  </div>

                  <div class="flex items-start gap-3">
                    <div class="w-7 h-7 rounded-full bg-[var(--app-surface-muted)] flex items-center justify-center text-slate-400 mt-0.5 flex-shrink-0">
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-3.5 h-3.5">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 0 1-2.25 2.25h-15a2.25 2.25 0 0 1-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0 0 19.5 4.5h-15a2.25 2.25 0 0 0-2.25 2.25m19.5 0v.243a2.25 2.25 0 0 1-1.07 1.916l-7.5 4.615a2.25 2.25 0 0 1-2.36 0L3.32 8.91a2.25 2.25 0 0 1-1.07-1.916V6.75" />
                      </svg>
                    </div>
                    <div>
                      <span class="ui-label text-[10px]">{{ t('patient.profile.email') }}</span>
                      <span class="font-semibold block break-all" style="color: var(--text-primary)">{{ p.email || '-' }}</span>
                    </div>
                  </div>

                  <div class="flex items-start gap-3">
                    <div class="w-7 h-7 rounded-full bg-[var(--app-surface-muted)] flex items-center justify-center text-slate-400 mt-0.5 flex-shrink-0">
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-3.5 h-3.5">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M15 10.5a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
                        <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1 1 15 0Z" />
                      </svg>
                    </div>
                    <div>
                      <span class="ui-label text-[10px]">{{ t('patient.profile.address') }}</span>
                      <span class="font-semibold block" style="color: var(--text-primary)">{{ p.address || '-' }}</span>
                    </div>
                  </div>

                  <div class="flex items-start gap-3">
                    <div class="w-7 h-7 rounded-full bg-[var(--app-surface-muted)] flex items-center justify-center text-slate-400 mt-0.5 flex-shrink-0">
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-3.5 h-3.5">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 21h16.5M4.5 3h15M5.25 3v18m13.5-18v18M9 6.75h1.5m-1.5 3h1.5m-1.5 3h1.5m3-6H15m-1.5 3H15m-1.5 3H15M9 21v-3.375c0-.621.504-1.125 1.125-1.125h3.75c.621 0 1.125.504 1.125 1.125V21" />
                      </svg>
                    </div>
                    <div>
                      <span class="ui-label text-[10px]">{{ t('patient.profile.city') }}</span>
                      <span class="font-semibold block" style="color: var(--text-primary)">{{ p.city || '-' }}</span>
                    </div>
                  </div>
                </div>
              </section>
            </div>

            <!-- Right Column -->
            <div class="lg:col-span-2 space-y-6">
              <!-- Emergency Contact Card -->
              <section class="ui-card p-5 md:p-6 transition-all hover:shadow-[var(--shadow-panel)] duration-300">
                <div class="flex items-center gap-2.5 mb-5 pb-3 border-b border-[var(--app-border)]">
                  <div class="w-8 h-8 rounded-sm bg-orange-50 dark:bg-orange-950/20 text-orange-600 dark:text-orange-400 flex items-center justify-center border border-orange-200/50 dark:border-orange-900/30">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4.5 h-4.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z" />
                    </svg>
                  </div>
                  <h3 class="ui-title text-base font-bold">{{ t('patient.profile.emergencyContact') }}</h3>
                </div>
                
                <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div class="ui-card-muted p-3">
                    <span class="ui-label text-[10px]">{{ t('patient.profile.emergencyContactName') }}</span>
                    <span class="font-bold text-sm block mt-0.5" style="color: var(--text-primary)">{{ p.emergencyContactName || '-' }}</span>
                  </div>
                  <div class="ui-card-muted p-3">
                    <span class="ui-label text-[10px]">{{ t('patient.profile.emergencyContactPhone') }}</span>
                    <span class="font-semibold block mt-0.5" style="color: var(--text-primary)">{{ p.emergencyContactPhone || '-' }}</span>
                  </div>
                </div>
              </section>

              <!-- Allergies Card -->
              <section class="ui-card p-5 md:p-6 transition-all hover:shadow-[var(--shadow-panel)] duration-300">
                <div class="flex items-center gap-2.5 mb-4 pb-3 border-b border-[var(--app-border)]">
                  <div class="w-8 h-8 rounded-sm bg-red-50 dark:bg-rose-950/20 text-red-600 dark:text-rose-400 flex items-center justify-center border border-red-200/50 dark:border-rose-900/30">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4.5 h-4.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9 3.75h.008v.008H12v-.008Z" />
                    </svg>
                  </div>
                  <h3 class="ui-title text-base font-bold">{{ t('patient.profile.allergies') }}</h3>
                </div>
                
                <div class="p-4 rounded-[var(--radius-brand-md)] bg-[var(--app-surface-muted)] border border-[var(--app-border)]">
                  <p class="text-sm leading-relaxed whitespace-pre-line font-medium" style="color: var(--text-secondary)">
                    {{ p.allergies || t('patient.profile.noAllergies') }}
                  </p>
                </div>
              </section>

              <!-- Medical History Card -->
              <section class="ui-card p-5 md:p-6 transition-all hover:shadow-[var(--shadow-panel)] duration-300">
                <div class="flex items-center gap-2.5 mb-4 pb-3 border-b border-[var(--app-border)]">
                  <div class="w-8 h-8 rounded-sm bg-blue-50 dark:bg-blue-950/20 text-blue-600 dark:text-blue-400 flex items-center justify-center border border-blue-200/50 dark:border-blue-900/30">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4.5 h-4.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 0 0 2.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 0 0-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 0 0 .75-.75 2.25 2.25 0 0 0-.1-.664m-5.8 0A2.251 2.251 0 0 1 13.5 2.25H15c1.03 0 1.9.693 2.166 1.638m-7.377 2.24a.75.75 0 0 1 .75-.75h7.5a.75.75 0 0 1 .75.75v11.75a.75.75 0 0 1-.75.75h-7.5a.75.75 0 0 1-.75-.75V6.75Z" />
                    </svg>
                  </div>
                  <h3 class="ui-title text-base font-bold">{{ t('patient.profile.medicalHistory') }}</h3>
                </div>
                
                <div class="p-4 rounded-[var(--radius-brand-md)] bg-[var(--app-surface-muted)] border border-[var(--app-border)]">
                  <p class="text-sm leading-relaxed whitespace-pre-line font-medium" style="color: var(--text-secondary)">
                    {{ p.medicalHistory || t('patient.profile.noMedicalHistory') }}
                  </p>
                </div>
              </section>
            </div>
          </div>
        }
      </div>
    </app-shell>
  `,
})
export class PatientProfilePageComponent implements OnInit {
  private readonly portalService = inject(PatientPortalService);
  readonly i18n = inject(I18nService);

  readonly patient = signal<PatientPortalMeResponse | null>(null);
  readonly isLoading = signal(false);
  readonly error = signal('');

  ngOnInit(): void {
    this.loadProfile();
  }

  private loadProfile(): void {
    this.isLoading.set(true);
    this.error.set('');
    this.portalService.getMe().subscribe({
      next: (data) => {
        this.patient.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.error.set(err.error?.detail || this.t('patient.profile.loadError'));
      },
    });
  }

  t(key: string): string {
    return this.i18n.t(key);
  }
}
