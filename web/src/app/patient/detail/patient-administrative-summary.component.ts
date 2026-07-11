import { Component, inject, input } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';
import { Patient } from '../patient.models';

@Component({
  selector: 'app-patient-administrative-summary',
  standalone: true,
  template: `
    @let currentPatient = patient();
    <section>
      <div class="mb-4 flex items-center justify-between gap-3">
        <h3 class="text-xs font-black uppercase tracking-wider text-[var(--text-muted)]">
          {{ t('patient.urgTemp.admin.title') }}
        </h3>
        @if (!isProvisional(currentPatient)) {
          <span class="rounded-sm bg-[var(--brand-success-subtle)] px-2 py-1 text-[10px] font-black uppercase tracking-wider text-[var(--brand-success-text)]">
            {{ t('patient.urgTemp.admin.verified') }}
          </span>
        }
      </div>

      <dl class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <div class="ui-card-muted p-3">
          <dt class="ui-label block">{{ t('patient.urgTemp.admin.gender') }}</dt>
          <dd class="text-sm font-extrabold text-[var(--text-primary)]">{{ valueOrFallback(currentPatient.gender) }}</dd>
        </div>
        <div class="ui-card-muted p-3">
          <dt class="ui-label block">{{ t('patient.urgTemp.admin.birthDate') }}</dt>
          <dd class="text-sm font-extrabold text-[var(--text-primary)]">{{ birthDateLabel(currentPatient.birthDate) }}</dd>
        </div>
        <div class="ui-card-muted p-3">
          <dt class="ui-label block">{{ t('patient.urgTemp.admin.phone') }}</dt>
          <dd class="text-sm font-extrabold text-[var(--text-primary)]">{{ valueOrFallback(currentPatient.phone) }}</dd>
        </div>
        <div class="ui-card-muted p-3">
          <dt class="ui-label block">{{ t('patient.urgTemp.admin.bloodGroup') }}</dt>
          <dd class="text-sm font-extrabold text-[var(--text-primary)]">{{ valueOrFallback(currentPatient.bloodGroup) }}</dd>
        </div>
        <div class="ui-card-muted p-3">
          <dt class="ui-label block">{{ t('patient.urgTemp.admin.email') }}</dt>
          <dd class="text-sm font-extrabold text-[var(--text-primary)]">{{ valueOrFallback(currentPatient.email) }}</dd>
        </div>
        <div class="ui-card-muted p-3">
          <dt class="ui-label block">{{ t('patient.urgTemp.admin.city') }}</dt>
          <dd class="text-sm font-extrabold text-[var(--text-primary)]">{{ valueOrFallback(currentPatient.city) }}</dd>
        </div>
        <div class="ui-card-muted p-3">
          <dt class="ui-label block">{{ t('patient.urgTemp.admin.district') }}</dt>
          <dd class="text-sm font-extrabold text-[var(--text-primary)]">{{ valueOrFallback(currentPatient.district) }}</dd>
        </div>
        <div class="ui-card-muted p-3">
          <dt class="ui-label block">{{ t('patient.urgTemp.admin.address') }}</dt>
          <dd class="text-sm font-extrabold text-[var(--text-primary)]">{{ valueOrFallback(currentPatient.address) }}</dd>
        </div>
      </dl>
    </section>

    <section>
      <h3 class="mb-4 text-xs font-black uppercase tracking-wider text-[var(--text-muted)]">
        {{ t('patient.urgTemp.contact.title') }}
      </h3>
      <dl class="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div class="ui-card-muted p-3">
          <dt class="ui-label block">{{ t('patient.urgTemp.contact.name') }}</dt>
          <dd class="text-sm font-extrabold text-[var(--text-primary)]">{{ valueOrFallback(currentPatient.emergencyContactName) }}</dd>
        </div>
        <div class="ui-card-muted p-3">
          <dt class="ui-label block">{{ t('patient.urgTemp.contact.phone') }}</dt>
          <dd class="text-sm font-extrabold text-[var(--text-primary)]">{{ valueOrFallback(currentPatient.emergencyContactPhone) }}</dd>
        </div>
      </dl>
    </section>
  `,
})
export class PatientAdministrativeSummaryComponent {
  readonly patient = input.required<Patient>();
  private readonly i18n = inject(I18nService);

  t(key: string): string {
    return this.i18n.t(key);
  }

  isProvisional(patient: Patient): boolean {
    return patient.identityStatus === 'PROVISIONAL_URGENCY' || patient.identityStatus === 'DECLARED';
  }

  valueOrFallback(value?: string | null): string {
    return value?.trim() || this.t('patient.urgTemp.common.notProvided');
  }

  birthDateLabel(value?: string | null): string {
    if (!value) return this.t('patient.urgTemp.common.notProvided');
    const birthDate = new Date(value);
    if (Number.isNaN(birthDate.getTime())) return value;

    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const monthDifference = today.getMonth() - birthDate.getMonth();
    if (monthDifference < 0 || (monthDifference === 0 && today.getDate() < birthDate.getDate())) {
      age--;
    }

    const locale = this.i18n.locale() === 'en' ? 'en-GB' : 'fr-FR';
    const formattedDate = new Intl.DateTimeFormat(locale).format(birthDate);
    return `${formattedDate} (${Math.max(age, 0)} ${this.t('patient.urgTemp.admin.ageUnit')})`;
  }
}
