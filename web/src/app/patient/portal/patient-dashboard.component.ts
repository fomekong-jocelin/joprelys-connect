import { Component, inject, OnInit, signal } from '@angular/core';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { PatientProfileCardComponent } from './components/patient-profile-card.component';
import { PatientVisitsListComponent } from './components/patient-visits-list.component';
import { PatientPortalMeResponse, PatientPortalService } from './services/patient-portal.service';
import { PatientConsentsListComponent } from './components/patient-consents-list.component';
import { PatientAuditListComponent } from './components/patient-audit-list.component';
import { I18nService } from '../../core/i18n/i18n.service';

@Component({
  selector: 'app-patient-dashboard',
  standalone: true,
  imports: [AppShellComponent, PatientProfileCardComponent, PatientVisitsListComponent, PatientConsentsListComponent, PatientAuditListComponent],
  template: `
    <app-shell>
      <div class="app-container-wide py-6 lg:py-8 flex flex-col gap-5">
        <div class="flex flex-col gap-1">
          <p class="ui-label">{{ i18n.t('patient.dashboard.portal') }}</p>
          <div class="flex flex-col gap-2 md:flex-row md:items-end md:justify-between">
            <div>
              <h1 class="font-display text-2xl lg:text-3xl font-extrabold leading-tight" style="color: var(--text-primary)">
                @if (patientData()) {
                  {{ i18n.t('patient.dashboard.greeting') }}, {{ (patientData()!.fullName.split(' ')[0]) || patientData()!.fullName }}
                } @else {
                  {{ i18n.t('patient.dashboard.title') }}
                }
              </h1>
              <p class="text-sm text-[var(--text-secondary)]">{{ i18n.t('patient.dashboard.subtitle') }}</p>
            </div>
            @if (patientData()) {
              <div class="hidden md:flex items-center gap-2 text-xs text-[var(--text-muted)]">
                <span class="px-2 py-1 rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface)]">
                  {{ patientData()!.consultations.length }}
                  {{ i18n.t(patientData()!.consultations.length > 1 ? 'patient.dashboard.consultationCountPlural' : 'patient.dashboard.consultationCount') }}
                </span>
                <span class="px-2 py-1 rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface)]">
                  {{ i18n.t('patient.dashboard.readOnlyProfile') }}
                </span>
              </div>
            }
          </div>
        </div>

        @if (isLoading()) {
          <div class="ui-card-subtle flex items-center justify-center p-12">
            <div class="w-8 h-8 border-4 border-[var(--brand-primary)] border-t-transparent rounded-full animate-spin" [attr.aria-label]="i18n.t('common.loading')"></div>
          </div>
        } @else if (error()) {
          <div class="p-4 rounded-[var(--radius-brand-md)] bg-rose-50 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-800/40 text-rose-700 dark:text-rose-300 text-sm font-semibold">
            {{ error() }}
          </div>
        } @else if (patientData()) {

          <!--<div class="ui-card-subtle p-4 lg:p-5 flex flex-col gap-4 md:flex-row md:items-center">
            <div class="ui-avatar w-12 h-12 text-lg flex items-center justify-center font-bold shrink-0">
              {{ patientData()!.fullName.charAt(0) }}
            </div>
            <div class="min-w-0 flex-1 space-y-1">
              <p class="font-display font-extrabold text-base text-[var(&#45;&#45;text-primary)] truncate">{{ patientData()!.fullName }}</p>
              <div class="flex flex-col gap-1 sm:flex-row sm:items-center sm:gap-2">
                <span class="ui-label normal-case tracking-normal">{{ i18n.t('patient.dashboard.nationalDpu') }}</span>
                <code class="block max-w-full overflow-x-auto whitespace-nowrap rounded-[var(&#45;&#45;radius-brand-sm)] bg-[var(&#45;&#45;app-surface-muted)] px-2 py-1 font-mono text-[11px] font-bold text-[var(&#45;&#45;text-secondary)]">
                  {{ patientData()!.globalPatientNumber }}
                </code>
              </div>
            </div>
            <div class="grid grid-cols-2 gap-3 text-xs text-[var(&#45;&#45;text-secondary)] sm:flex sm:items-center sm:gap-5">
              <div>
                <span class="ui-label">{{ i18n.t('patient.profile.birth') }}</span>
                <span class="font-semibold text-[var(&#45;&#45;text-primary)]">{{ patientData()!.birthDate }}</span>
              </div>
              <div>
                <span class="ui-label">{{ i18n.t('patients.phone') }}</span>
                <span class="font-semibold text-[var(&#45;&#45;text-primary)] whitespace-nowrap">{{ patientData()!.phone }}</span>
              </div>
            </div>
          </div>-->

          <div class="flex gap-3 overflow-x-auto pb-1 -mx-4 px-4 md:mx-0 md:px-0 md:grid md:grid-cols-3 snap-x snap-mandatory">
            <button
              type="button"
              (click)="activeTab.set('visits')"
              [attr.aria-pressed]="activeTab() === 'visits'"
              [class]="tabClass('visits')"
            >
              <div class="p-2 rounded-[var(--radius-brand-sm)] bg-[var(--brand-primary)]/10 text-[var(--brand-primary)] shrink-0">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
                </svg>
              </div>
              <div class="min-w-0">
                <div class="flex items-center gap-1.5">
                  <span class="font-display font-bold text-sm text-[var(--text-primary)] truncate">{{ i18n.t('patient.dashboard.prescriptions') }}</span>
                  <span class="text-[10px] font-bold px-1.5 py-0.5 rounded-[var(--radius-brand-xs)] bg-[var(--brand-primary)]/10 text-[var(--brand-primary)] shrink-0">
                    {{ patientData()?.consultations?.length || 0 }}
                  </span>
                </div>
                <p class="text-[11px] text-[var(--text-muted)] truncate">{{ i18n.t('patient.dashboard.medicalDocuments') }}</p>
              </div>
            </button>

            <button
              type="button"
              (click)="activeTab.set('consents')"
              [attr.aria-pressed]="activeTab() === 'consents'"
              [class]="tabClass('consents')"
            >
              <div class="p-2 rounded-[var(--radius-brand-sm)] bg-[var(--brand-primary)]/10 text-[var(--brand-primary)] shrink-0">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285z" />
                </svg>
              </div>
              <div class="min-w-0">
                <span class="font-display font-bold text-sm text-[var(--text-primary)]">{{ i18n.t('patient.dashboard.consent') }}</span>
                <p class="text-[11px] text-[var(--text-muted)] truncate">{{ i18n.t('patient.dashboard.dataSharing') }}</p>
              </div>
            </button>

            <button
              type="button"
              (click)="activeTab.set('audit')"
              [attr.aria-pressed]="activeTab() === 'audit'"
              [class]="tabClass('audit')"
            >
              <div class="p-2 rounded-[var(--radius-brand-sm)] bg-[var(--brand-primary)]/10 text-[var(--brand-primary)] shrink-0">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </div>
              <div class="min-w-0">
                <span class="font-display font-bold text-sm text-[var(--text-primary)]">{{ i18n.t('patient.dashboard.securityAudit') }}</span>
                <p class="text-[11px] text-[var(--text-muted)] truncate">{{ i18n.t('patient.dashboard.dpuAccess') }}</p>
              </div>
            </button>
          </div>

          <div class="grid grid-cols-1 lg:grid-cols-[360px_minmax(0,1fr)] gap-5 items-start">
            <div class="hidden lg:block lg:sticky lg:top-6">
              <app-patient-profile-card [patient]="patientData()!" />
            </div>
            <div class="min-w-0">
              @if (activeTab() === 'visits') {
                <app-patient-visits-list
                  [consultations]="patientData()!.consultations"
                  (download)="onDownloadDocument($event)"
                />
              } @else if (activeTab() === 'consents') {
                <app-patient-consents-list />
              } @else if (activeTab() === 'audit') {
                <app-patient-audit-list />
              }
            </div>
          </div>

          <div class="lg:hidden">
            <details class="ui-card-subtle">
              <summary class="p-4 font-display font-bold text-sm text-[var(--text-primary)] cursor-pointer flex items-center justify-between gap-2">
                <span>{{ i18n.t('patient.dashboard.fullProfile') }}</span>
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4 text-[var(--text-muted)]">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                </svg>
              </summary>
              <div class="px-4 pb-4">
                <app-patient-profile-card [patient]="patientData()!" />
              </div>
            </details>
          </div>
        }
      </div>
    </app-shell>
  `
})
export class PatientDashboardComponent implements OnInit {
  private readonly portalService = inject(PatientPortalService);
  readonly i18n = inject(I18nService);

  readonly activeTab = signal<'visits' | 'consents' | 'audit'>('visits');
  readonly patientData = signal<PatientPortalMeResponse | null>(null);
  readonly isLoading = signal(false);
  readonly error = signal('');
  readonly expandedConsultations = signal<Record<string, boolean>>({});

  tabClass(tab: 'visits' | 'consents' | 'audit'): string {
    const base = 'ui-card-subtle p-3.5 flex items-center gap-3 text-left transition-colors cursor-pointer snap-start shrink-0 w-[236px] md:w-auto';
    return this.activeTab() === tab
      ? `${base} border-[var(--brand-primary)] bg-[var(--app-surface)]`
      : `${base} hover:border-[var(--brand-primary)]`;
  }

  toggleConsultation(id: string): void {
    this.expandedConsultations.update(prev => ({ ...prev, [id]: !prev[id] }));
  }

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

  onDownloadDocument(visitId: string): void {
    this.portalService.downloadDocument(visitId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `ordonnance-${visitId}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        alert(this.i18n.t('patients.downloadPdfError'));
      }
    });
  }
}
