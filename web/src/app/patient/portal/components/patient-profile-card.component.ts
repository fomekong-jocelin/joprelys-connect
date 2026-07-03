import { Component, inject, Input } from '@angular/core';
import { PatientPortalMeResponse } from '../services/patient-portal.service';
import { I18nService } from '../../../core/i18n/i18n.service';

@Component({
  selector: 'app-patient-profile-card',
  standalone: true,
  template: `
    <div class="ui-card-subtle p-5 flex flex-col gap-5">
      <div class="flex items-start gap-4 border-b border-[var(--app-border)] pb-4">
        <div class="ui-avatar w-14 h-14 text-xl flex items-center justify-center font-bold shrink-0">
          {{ patient.fullName.charAt(0) }}
        </div>
        <div class="min-w-0 flex-1">
          <p class="ui-label">{{ i18n.t('patient.profile.identity') }}</p>
          <h2 class="font-display text-xl font-extrabold leading-tight truncate" style="color: var(--text-primary)" [title]="patient.fullName">
            {{ patient.fullName }}
          </h2>
          <div class="mt-2">
            <span class="ui-label normal-case tracking-normal">{{ i18n.t('patient.dashboard.nationalDpu') }}</span>
            <code
              class="mt-1 block max-w-full overflow-x-auto whitespace-nowrap rounded-[var(--radius-brand-sm)] bg-[var(--app-surface-muted)] px-2 py-1 font-mono text-[11px] font-bold text-[var(--text-secondary)]"
              [title]="patient.globalPatientNumber"
            >
              {{ patient.globalPatientNumber }}
            </code>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-4 text-sm">
        <div>
          <span class="ui-label">{{ i18n.t('patients.gender') }}</span>
          <span class="font-semibold text-[var(--text-primary)]">{{ patient.gender }}</span>
        </div>
        <div>
          <span class="ui-label">{{ i18n.t('patient.profile.birth') }}</span>
          <span class="font-semibold text-[var(--text-primary)]">{{ patient.birthDate }}</span>
        </div>
        <div>
          <span class="ui-label">{{ i18n.t('patients.phone') }}</span>
          <span class="font-semibold text-[var(--text-primary)] whitespace-nowrap">{{ patient.phone }}</span>
        </div>
        <div>
          <span class="ui-label">{{ i18n.t('patients.city') }}</span>
          <span class="font-semibold text-[var(--text-primary)]">{{ patient.city }}</span>
        </div>
      </div>

      <div>
        <span class="ui-label">{{ i18n.t('patients.address') }}</span>
        <p class="mt-1 text-sm font-semibold leading-relaxed text-[var(--text-primary)]">
          {{ patient.address || '-' }}, {{ patient.city }}
        </p>
      </div>

      <div class="border-t border-[var(--app-border)] pt-4 flex flex-col gap-3">
        <div class="rounded-[var(--radius-brand-md)] border border-rose-200 dark:border-rose-900/50 bg-rose-50/70 dark:bg-rose-950/15 p-3">
          <span class="ui-label text-rose-700 dark:text-rose-300">{{ i18n.t('patients.allergies') }}</span>
          <p class="mt-1 text-sm font-semibold leading-relaxed text-rose-800 dark:text-rose-200">
            {{ patient.allergies || i18n.t('patient.profile.noAllergies') }}
          </p>
        </div>
        <div class="rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface-muted)]/70 p-3">
          <span class="ui-label">{{ i18n.t('patients.medicalHistory') }}</span>
          <p class="mt-1 text-sm font-semibold leading-relaxed text-[var(--text-secondary)]">
            {{ patient.medicalHistory || i18n.t('patient.profile.noHistory') }}
          </p>
        </div>
      </div>
    </div>
  `
})
export class PatientProfileCardComponent {
  readonly i18n = inject(I18nService);
  @Input({ required: true }) patient!: PatientPortalMeResponse;
}
