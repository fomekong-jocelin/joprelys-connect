import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppShellComponent } from '../../../shared/layout/app-shell.component';
import { PatientPortalService } from '../services/patient-portal.service';
import { I18nService } from '../../../core/i18n/i18n.service';

@Component({
  selector: 'app-patient-summary-page',
  standalone: true,
  imports: [CommonModule, AppShellComponent],
  template: `
    <app-shell>
      <div class="app-container py-6 space-y-6">
        <!-- Header Section -->
        <div class="flex flex-col md:flex-row md:items-center md:justify-between border-b border-slate-200 dark:border-slate-800 pb-5 gap-4">
          <div>
            <h1 class="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
              {{ i18n.t('patient.summary.title') }}
            </h1>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
              {{ i18n.t('patient.summary.subtitle') }}
            </p>
          </div>
          <div>
            <button
              (click)="downloadPdf()"
              [disabled]="isDownloading() || isLoading()"
              class="inline-flex items-center justify-center px-4 py-2 text-sm font-semibold text-white bg-[var(--brand-primary)] hover:bg-[var(--brand-primary-hover)] disabled:opacity-50 cursor-pointer transition-colors shadow-sm focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--brand-primary)] rounded-[6px]"
            >
              @if (isDownloading()) {
                <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                {{ i18n.t('patient.summary.downloading') }}
              } @else {
                <svg class="-ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5M16.5 12 12 16.5m0 0L7.5 12m4.5 4.5V3" />
                </svg>
                {{ i18n.t('patient.summary.downloadPdf') }}
              }
            </button>
          </div>
        </div>

        @if (isLoading()) {
          <div class="flex items-center justify-center py-12">
            <div class="w-8 h-8 border-4 border-[var(--brand-primary)] border-t-transparent rounded-full animate-spin"></div>
          </div>
        } @else if (error()) {
          <div class="p-4 bg-rose-50 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-800/40 text-rose-700 dark:text-rose-300 text-sm font-semibold rounded-[6px]">
            {{ error() }}
          </div>
        } @else if (summary()) {
          <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <!-- Left Side: Identity Info & Vitals -->
            <div class="lg:col-span-1 space-y-6">
              <!-- Identity Card -->
              <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 shadow-sm rounded-[6px]">
                <h2 class="text-base font-bold text-slate-900 dark:text-white mb-4 border-b border-slate-100 dark:border-slate-800 pb-2">
                  {{ i18n.t('patient.summary.identity') }}
                </h2>
                <div class="space-y-3 text-sm">
                  <div>
                    <span class="block text-xs font-medium text-slate-400 dark:text-slate-500 uppercase">{{ i18n.t('patient.profile.fullName') }}</span>
                    <span class="font-semibold text-slate-800 dark:text-slate-200">{{ summary()?.fullName }}</span>
                  </div>
                  <div>
                    <span class="block text-xs font-medium text-slate-400 dark:text-slate-500 uppercase">{{ i18n.t('patient.summary.dpuLabel') }}</span>
                    <span class="font-mono text-slate-700 dark:text-slate-300">{{ summary()?.globalPatientNumber }}</span>
                  </div>
                  <div class="grid grid-cols-2 gap-4">
                    <div>
                      <span class="block text-xs font-medium text-slate-400 dark:text-slate-500 uppercase">{{ i18n.t('patient.profile.birthDate') }}</span>
                      <span class="text-slate-700 dark:text-slate-300">{{ summary()?.birthDate | date:'dd/MM/yyyy' }}</span>
                    </div>
                    <div>
                      <span class="block text-xs font-medium text-slate-400 dark:text-slate-500 uppercase">{{ i18n.t('patient.profile.gender') }}</span>
                      <span class="text-slate-700 dark:text-slate-300">{{ summary()?.gender }}</span>
                    </div>
                  </div>
                  @if (summary()?.bloodGroup) {
                    <div>
                      <span class="block text-xs font-medium text-slate-400 dark:text-slate-500 uppercase">{{ i18n.t('patient.profile.bloodGroup') }}</span>
                      <span class="inline-flex items-center px-2 py-0.5 text-xs font-semibold rounded-[4px] bg-red-50 dark:bg-red-950/20 text-red-700 dark:text-red-300 border border-red-100 dark:border-red-900/30">
                        {{ summary()?.bloodGroup }}
                      </span>
                    </div>
                  }
                </div>
              </div>

              <!-- Vitals Card (If any vitals from last visits) -->
              <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 shadow-sm rounded-[6px]">
                <h2 class="text-base font-bold text-slate-900 dark:text-white mb-4 border-b border-slate-100 dark:border-slate-800 pb-2">
                  {{ i18n.t('patient.summary.recentVitals') }}
                </h2>
                @if (summary()?.recentDiagnostics?.[0]?.vitals; as vt) {
                  <div class="grid grid-cols-2 gap-4 text-sm">
                    <div class="p-3 bg-slate-50 dark:bg-slate-800/40 rounded-[4px]">
                      <span class="block text-xs text-slate-400">{{ i18n.t('patient.visits.temperature') }}</span>
                      <span class="text-base font-bold text-slate-800 dark:text-slate-200">{{ vt.temperature ? vt.temperature + ' °C' : '-' }}</span>
                    </div>
                    <div class="p-3 bg-slate-50 dark:bg-slate-800/40 rounded-[4px]">
                      <span class="block text-xs text-slate-400">{{ i18n.t('patient.summary.bloodPressure') }}</span>
                      <span class="text-base font-bold text-slate-800 dark:text-slate-200">
                        {{ vt.systolic && vt.diastolic ? vt.systolic + '/' + vt.diastolic + ' mmHg' : '-' }}
                      </span>
                    </div>
                    <div class="p-3 bg-slate-50 dark:bg-slate-800/40 rounded-[4px]">
                      <span class="block text-xs text-slate-400">{{ i18n.t('patient.visits.pulse') }}</span>
                      <span class="text-base font-bold text-slate-800 dark:text-slate-200">{{ vt.pulse ? vt.pulse + ' bpm' : '-' }}</span>
                    </div>
                    <div class="p-3 bg-slate-50 dark:bg-slate-800/40 rounded-[4px]">
                      <span class="block text-xs text-slate-400">{{ i18n.t('patient.summary.spo2') }}</span>
                      <span class="text-base font-bold text-slate-800 dark:text-slate-200">{{ vt.spo2 ? vt.spo2 + ' %' : '-' }}</span>
                    </div>
                  </div>
                } @else {
                  <p class="text-sm text-slate-500 dark:text-slate-400 italic">{{ i18n.t('patient.summary.noVitals') }}</p>
                }
              </div>
            </div>

            <!-- Right Side: Medical Records Lists -->
            <div class="lg:col-span-2 space-y-6">
              <!-- Allergies Section (Card Widget with gravity) -->
              <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 shadow-sm rounded-[6px]">
                <div class="flex items-center justify-between mb-4 border-b border-slate-100 dark:border-slate-800 pb-2">
                  <h2 class="text-base font-bold text-slate-900 dark:text-white flex items-center">
                    <svg class="h-5 w-5 text-amber-500 mr-2" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z" />
                    </svg>
                    {{ i18n.t('patient.summary.allergies') }}
                  </h2>
                </div>

                @if (summary()?.allergies?.length) {
                  <div class="overflow-x-auto">
                    <table class="min-w-full divide-y divide-slate-200 dark:divide-slate-800 text-sm text-left">
                      <thead>
                        <tr class="text-slate-400 font-semibold uppercase text-xs">
                          <th class="py-2">{{ i18n.t('patients.medicalInfo.allergies.substance') }}</th>
                          <th class="py-2">{{ i18n.t('patients.medicalInfo.allergies.severity') }}</th>
                          <th class="py-2">{{ i18n.t('patients.medicalInfo.allergies.reaction') }}</th>
                          <th class="py-2">{{ i18n.t('patients.medicalInfo.allergies.discoveredAt') }}</th>
                        </tr>
                      </thead>
                      <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                        @for (allergy of summary()?.allergies; track allergy.id) {
                          <tr class="text-slate-700 dark:text-slate-300">
                            <td class="py-2.5 font-medium text-slate-900 dark:text-white">{{ allergy.substance }}</td>
                            <td class="py-2.5">
                              <span class="inline-flex items-center px-2 py-0.5 text-xs font-semibold rounded-[4px] border"
                                    [ngClass]="{
                                      'bg-red-50 text-red-700 border-red-100 dark:bg-red-950/20 dark:text-red-300 dark:border-red-900/30': allergy.severity === 'HIGH',
                                      'bg-amber-50 text-amber-700 border-amber-100 dark:bg-amber-950/20 dark:text-amber-300 dark:border-amber-900/30': allergy.severity === 'MEDIUM',
                                      'bg-emerald-50 text-emerald-700 border-emerald-100 dark:bg-emerald-950/20 dark:text-emerald-300 dark:border-emerald-900/30': allergy.severity === 'LOW'
                                    }">
                                {{ i18n.t('patients.medicalInfo.allergies.severity.' + allergy.severity) }}
                              </span>
                            </td>
                            <td class="py-2.5">{{ allergy.reaction || '-' }}</td>
                            <td class="py-2.5">{{ allergy.discoveredAt | date:'dd/MM/yyyy' }}</td>
                          </tr>
                        }
                      </tbody>
                    </table>
                  </div>
                } @else {
                  <p class="text-sm text-slate-500 dark:text-slate-400 italic">
                    {{ i18n.t('patient.summary.noAllergies') }}
                  </p>
                }
              </div>

              <!-- Critical Lab Results Section (Alert card widget) -->
              <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 shadow-sm rounded-[6px]">
                <div class="flex items-center justify-between mb-4 border-b border-slate-100 dark:border-slate-800 pb-2">
                  <h2 class="text-base font-bold text-slate-900 dark:text-white flex items-center">
                    <svg class="h-5 w-5 text-red-500 mr-2" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m0-10.036A11.959 11.959 0 0 1 3.598 6 11.99 11.99 0 0 0 3 9.75c0 5.592 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.57-.598-3.75h-.152c-3.196 0-6.1-1.249-8.25-3.286Zm0 13.036h.008v.008H12v-.008Z" />
                    </svg>
                    {{ i18n.t('patient.summary.results') }}
                  </h2>
                </div>

                @if (summary()?.criticalResults?.length) {
                  <div class="space-y-3">
                    @for (result of summary()?.criticalResults; track result.id) {
                      <div class="flex items-start justify-between p-3 rounded-[4px] bg-red-50/50 dark:bg-red-950/10 border border-red-200/50 dark:border-red-900/30 text-sm">
                        <div>
                          <span class="block font-semibold text-red-900 dark:text-red-300">{{ result.analyteName }}</span>
                          <span class="text-xs text-slate-500 dark:text-slate-400">{{ i18n.t('patient.summary.analyzedAt') }} {{ result.validatedAt | date:'dd/MM/yyyy HH:mm' }}</span>
                        </div>
                        <div class="text-right">
                          <span class="block font-bold text-red-700 dark:text-red-400">{{ result.value }} {{ result.unit }}</span>
                          <span class="inline-flex items-center px-1.5 py-0.5 text-xs font-semibold rounded-[4px] bg-red-100 dark:bg-red-900/40 text-red-800 dark:text-red-200 uppercase">
                            {{ i18n.t('lab.interpretation.' + result.interpretation) }}
                          </span>
                        </div>
                      </div>
                    }
                  </div>
                } @else {
                  <p class="text-sm text-slate-500 dark:text-slate-400 italic">
                    {{ i18n.t('patient.summary.noResults') }}
                  </p>
                }
              </div>

              <!-- Medical History Section -->
              <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 shadow-sm rounded-[6px]">
                <h2 class="text-base font-bold text-slate-900 dark:text-white mb-4 border-b border-slate-100 dark:border-slate-800 pb-2">
                  {{ i18n.t('patient.summary.history') }}
                </h2>
                @if (summary()?.medicalHistory?.length) {
                  <div class="overflow-x-auto">
                    <table class="min-w-full divide-y divide-slate-200 dark:divide-slate-800 text-sm text-left">
                      <thead>
                        <tr class="text-slate-400 font-semibold uppercase text-xs">
                          <th class="py-2">{{ i18n.t('patients.medicalInfo.history.category') }}</th>
                          <th class="py-2">{{ i18n.t('patients.medicalInfo.history.description') }}</th>
                          <th class="py-2">{{ i18n.t('patients.medicalInfo.history.onsetDate') }}</th>
                          <th class="py-2">{{ i18n.t('patients.status') }}</th>
                        </tr>
                      </thead>
                      <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                        @for (h of summary()?.medicalHistory; track h.id) {
                          <tr class="text-slate-700 dark:text-slate-300">
                            <td class="py-2.5 font-medium text-slate-900 dark:text-white">{{ i18n.t('patients.medicalInfo.history.category.' + h.category) }}</td>
                            <td class="py-2.5">{{ h.description }}</td>
                            <td class="py-2.5">{{ h.onsetDate | date:'dd/MM/yyyy' }}</td>
                            <td class="py-2.5">
                              <span class="inline-flex items-center px-2 py-0.5 text-xs font-semibold rounded-[4px]"
                                    [ngClass]="{
                                      'bg-amber-50 text-amber-700 border border-amber-100 dark:bg-amber-950/20 dark:text-amber-300 dark:border-amber-900/30': h.important,
                                      'bg-blue-50 text-blue-700 border border-blue-100 dark:bg-blue-950/20 dark:text-blue-300 dark:border-blue-900/30': !h.important
                                    }">
                                {{ h.important ? ('⚠️ ' + i18n.t('patients.medicalInfo.history.important')) : '' }} {{ h.isOngoing ? i18n.t('patients.medicalInfo.history.isOngoing') : '' }}
                              </span>
                            </td>
                          </tr>
                        }
                      </tbody>
                    </table>
                  </div>
                } @else {
                  <p class="text-sm text-slate-500 dark:text-slate-400 italic">
                    {{ i18n.t('patient.summary.noHistory') }}
                  </p>
                }
              </div>

              <!-- Ongoing Treatments Section -->
              <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 shadow-sm rounded-[6px]">
                <h2 class="text-base font-bold text-slate-900 dark:text-white mb-4 border-b border-slate-100 dark:border-slate-800 pb-2">
                  {{ i18n.t('patient.summary.treatments') }}
                </h2>
                @if (summary()?.activePrescriptions?.length) {
                  <div class="space-y-4">
                    @for (prescription of summary()?.activePrescriptions; track prescription.id) {
                      <div class="border border-slate-100 dark:border-slate-800 rounded-[6px] p-4 bg-slate-50/50 dark:bg-slate-800/20">
                        <div class="flex items-center justify-between mb-3">
                          <span class="font-semibold text-slate-900 dark:text-white">{{ i18n.t('patient.summary.prescriptionNumber') }} {{ prescription.prescriptionNumber }}</span>
                          <span class="text-xs text-slate-500">{{ i18n.t('patient.summary.prescriptionDate') }} {{ prescription.createdAt | date:'dd/MM/yyyy' }}</span>
                        </div>
                        <ul class="divide-y divide-slate-100 dark:divide-slate-800 text-sm">
                          @for (item of prescription.items; track item.drugName) {
                            <li class="py-2 flex justify-between items-start">
                              <div>
                                <span class="font-medium text-slate-900 dark:text-white block">{{ item.drugName }}</span>
                                <span class="text-xs text-slate-500">{{ item.instructions }}</span>
                              </div>
                              <div class="text-right text-xs">
                                <span class="block font-semibold text-slate-700 dark:text-slate-300">{{ item.posology }}</span>
                                <span class="text-slate-400">{{ i18n.t('patient.visits.durationPrefix') }} {{ item.duration }}</span>
                              </div>
                            </li>
                          }
                        </ul>
                      </div>
                    }
                  </div>
                } @else {
                  <p class="text-sm text-slate-500 dark:text-slate-400 italic">
                    {{ i18n.t('patient.summary.noTreatments') }}
                  </p>
                }
              </div>

              <!-- History Lists Grid: Visits & Diagnostics -->
              <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <!-- Visits Card -->
                <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 shadow-sm rounded-[6px]">
                  <h2 class="text-base font-bold text-slate-900 dark:text-white mb-4 border-b border-slate-100 dark:border-slate-800 pb-2">
                    {{ i18n.t('patient.summary.visits') }}
                  </h2>
                  @if (summary()?.recentVisits?.length) {
                    <ul class="divide-y divide-slate-100 dark:divide-slate-800 text-sm">
                      @for (v of summary()?.recentVisits; track v.id) {
                        <li class="py-2.5">
                          <div class="flex justify-between items-center">
                            <span class="font-semibold text-slate-900 dark:text-white">{{ v.visitNumber }}</span>
                            <span class="text-xs text-slate-500">{{ v.createdAt | date:'dd/MM/yyyy' }}</span>
                          </div>
                          <p class="text-xs text-slate-600 dark:text-slate-400 mt-0.5">{{ i18n.t('patient.summary.visitReason') }} {{ v.reason }}</p>
                          @if (v.service) {
                            <span class="inline-flex items-center px-1.5 py-0.5 text-[10px] font-medium rounded bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-300 mt-1">
                              {{ v.service }}
                            </span>
                          }
                        </li>
                      }
                    </ul>
                  } @else {
                    <p class="text-sm text-slate-500 dark:text-slate-400 italic">
                      {{ i18n.t('patient.summary.noVisits') }}
                    </p>
                  }
                </div>

                <!-- Diagnostics Card -->
                <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-5 shadow-sm rounded-[6px]">
                  <h2 class="text-base font-bold text-slate-900 dark:text-white mb-4 border-b border-slate-100 dark:border-slate-800 pb-2">
                    {{ i18n.t('patient.summary.diagnostics') }}
                  </h2>
                  @if (summary()?.recentDiagnostics?.length) {
                    <ul class="divide-y divide-slate-100 dark:divide-slate-800 text-sm">
                      @for (d of summary()?.recentDiagnostics; track d.id) {
                        <li class="py-2.5">
                          <div class="flex justify-between items-center">
                            <span class="font-semibold text-slate-900 dark:text-white">{{ d.finalDiagnosis || d.diagnosis }}</span>
                            <span class="text-xs text-slate-500">{{ d.createdAt | date:'dd/MM/yyyy' }}</span>
                          </div>
                          <p class="text-xs text-slate-500 dark:text-slate-400 mt-0.5">{{ i18n.t('patient.visits.doctor') }}: {{ d.doctorName }}</p>
                          @if (d.conclusion) {
                            <p class="text-xs text-slate-600 dark:text-slate-300 italic mt-1 bg-slate-50 dark:bg-slate-800/40 p-1.5 rounded-[4px]">{{ d.conclusion }}</p>
                          }
                        </li>
                      }
                    </ul>
                  } @else {
                    <p class="text-sm text-slate-500 dark:text-slate-400 italic">
                      {{ i18n.t('patient.summary.noDiagnostics') }}
                    </p>
                  }
                </div>
              </div>
            </div>
          </div>
        }
      </div>
    </app-shell>
  `
})
export class PatientSummaryPageComponent implements OnInit {
  private readonly portalService = inject(PatientPortalService);
  readonly i18n = inject(I18nService);

  readonly summary = signal<any | null>(null);
  readonly isLoading = signal(false);
  readonly isDownloading = signal(false);
  readonly error = signal('');

  ngOnInit(): void {
    this.loadSummary();
  }

  loadSummary(): void {
    this.isLoading.set(true);
    this.error.set('');
    this.portalService.getMedicalSummary().subscribe({
      next: (data) => {
        this.summary.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.error.set(err.error?.detail || this.i18n.t('patient.summary.loadError'));
      }
    });
  }

  downloadPdf(): void {
    this.isDownloading.set(true);
    this.portalService.downloadSummaryPdf().subscribe({
      next: (blob) => {
        this.isDownloading.set(false);
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `synthese-medicale.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.isDownloading.set(false);
        alert(this.i18n.t('patient.summary.downloadError'));
      }
    });
  }
}
