import { Component, inject, input, output } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';
import { ButtonComponent } from '../../shared/ui/button.component';
import { Patient } from '../patient.models';

@Component({
  selector: 'app-patient-provisional-identity-card',
  standalone: true,
  imports: [ButtonComponent],
  template: `
    @let currentPatient = patient();
    <section class="ui-card border-l-2 border-l-[var(--brand-warning)] p-4 sm:p-5">
      <div class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <div class="flex flex-wrap items-center gap-2">
            <span class="rounded-sm bg-[var(--brand-warning-subtle)] px-2 py-1 text-[10px] font-black uppercase tracking-wider text-[var(--brand-warning-text)]">
              {{ t('patient.urgTemp.provisional.badge') }}
            </span>
            <strong class="font-mono text-sm text-[var(--text-primary)]">
              {{ currentPatient.temporaryPatientNumber || currentPatient.displayName || currentPatient.globalPatientNumber }}
            </strong>
          </div>
          <h3 class="mt-3 font-display text-base font-black text-[var(--text-primary)]">
            {{ t('patient.urgTemp.provisional.title') }}
          </h3>
          <p class="mt-1 max-w-2xl text-sm text-[var(--text-secondary)]">
            {{ t('patient.urgTemp.provisional.description') }}
          </p>
        </div>
        <app-ui-button variant="primary" class="w-full sm:w-auto" (pressed)="identifyRequested.emit()">
          {{ t('patient.urgTemp.provisional.identify') }}
        </app-ui-button>
      </div>

      <dl class="mt-4 grid grid-cols-1 gap-3 border-t border-[var(--divider-subtle)] pt-4 sm:grid-cols-2 lg:grid-cols-4">
        <div>
          <dt class="ui-label">{{ t('patient.urgTemp.provisional.apparentGender') }}</dt>
          <dd class="mt-1 text-sm font-bold text-[var(--text-primary)]">{{ valueOrFallback(currentPatient.apparentGender, true) }}</dd>
        </div>
        <div>
          <dt class="ui-label">{{ t('patient.urgTemp.provisional.estimatedAge') }}</dt>
          <dd class="mt-1 text-sm font-bold text-[var(--text-primary)]">{{ valueOrFallback(currentPatient.estimatedAgeRange) }}</dd>
        </div>
        <div>
          <dt class="ui-label">{{ t('patient.urgTemp.provisional.foundLocation') }}</dt>
          <dd class="mt-1 text-sm font-bold text-[var(--text-primary)]">{{ valueOrFallback(currentPatient.foundLocation) }}</dd>
        </div>
        <div>
          <dt class="ui-label">{{ t('patient.urgTemp.provisional.foundAt') }}</dt>
          <dd class="mt-1 text-sm font-bold text-[var(--text-primary)]">{{ formatDate(currentPatient.foundAt) }}</dd>
        </div>
      </dl>

      @if (currentPatient.physicalDescription) {
        <div class="mt-3 rounded-sm border border-[var(--app-border)] bg-[var(--app-surface-muted)] p-3">
          <span class="ui-label">{{ t('patient.urgTemp.provisional.physicalDescription') }}</span>
          <p class="mt-1 text-sm text-[var(--text-secondary)]">{{ currentPatient.physicalDescription }}</p>
        </div>
      }
    </section>
  `,
})
export class PatientProvisionalIdentityCardComponent {
  readonly patient = input.required<Patient>();
  readonly identifyRequested = output<void>();
  private readonly i18n = inject(I18nService);

  t(key: string): string {
    return this.i18n.t(key);
  }

  valueOrFallback(value?: string | null, useUndetermined = false): string {
    if (value?.trim()) return value;
    return this.t(useUndetermined
      ? 'patient.urgTemp.common.notDetermined'
      : 'patient.urgTemp.common.notProvided');
  }

  formatDate(value?: string | null): string {
    if (!value) return this.t('patient.urgTemp.common.notProvided');
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    const locale = this.i18n.locale() === 'en' ? 'en-GB' : 'fr-FR';
    return new Intl.DateTimeFormat(locale, { dateStyle: 'short', timeStyle: 'short' }).format(date);
  }
}
