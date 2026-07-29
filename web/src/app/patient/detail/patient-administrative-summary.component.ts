import { Component, inject, input, signal } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';
import { Patient } from '../patient.models';

@Component({
  selector: 'app-patient-administrative-summary',
  standalone: true,
  template: `
    @let currentPatient = patient();
    <div class="grid gap-4">
      @if (!isProvisional(currentPatient)) {
        <div class="flex justify-end">
          <span class="rounded-sm bg-[var(--brand-success-subtle)] px-2 py-1 text-[10px] font-black uppercase tracking-wider text-[var(--brand-success-text)]">
            {{ t('patient.urgTemp.admin.verified') }}
          </span>
        </div>
      }

      <!-- One aligned information sheet is easier to scan on mobile than a mosaic of cards. -->
      <dl
        class="overflow-hidden rounded-md border border-[var(--app-border)] bg-[var(--app-surface)]"
        data-testid="patient-identity-information-sheet"
      >
        <div class="grid grid-cols-[minmax(7rem,0.75fr)_minmax(0,1.25fr)] items-start gap-3 border-b border-[var(--divider-subtle)] px-4 py-3 sm:grid-cols-[10rem_minmax(0,1fr)]">
          <dt class="ui-label pt-0.5">{{ t('patient.urgTemp.admin.gender') }}</dt>
          <dd class="min-w-0 text-sm font-extrabold leading-5 text-[var(--text-primary)]">{{ valueOrFallback(currentPatient.gender) }}</dd>
        </div>

        <div class="grid grid-cols-[minmax(7rem,0.75fr)_minmax(0,1.25fr)] items-start gap-3 border-b border-[var(--divider-subtle)] px-4 py-3 sm:grid-cols-[10rem_minmax(0,1fr)]">
          <dt class="ui-label pt-0.5">{{ t('patient.urgTemp.admin.birthDate') }}</dt>
          <dd class="min-w-0 text-sm font-extrabold leading-5 text-[var(--text-primary)]">{{ birthDateLabel(currentPatient.birthDate) }}</dd>
        </div>

        <div class="grid grid-cols-[minmax(7rem,0.75fr)_minmax(0,1.25fr)] items-start gap-3 border-b border-[var(--divider-subtle)] px-4 py-3 sm:grid-cols-[10rem_minmax(0,1fr)]">
          <dt class="ui-label pt-0.5">{{ t('patient.urgTemp.admin.phone') }}</dt>
          <dd class="min-w-0 break-words text-sm font-extrabold leading-5 text-[var(--text-primary)]">{{ valueOrFallback(currentPatient.phone) }}</dd>
        </div>

        <div class="grid grid-cols-[minmax(7rem,0.75fr)_minmax(0,1.25fr)] items-start gap-3 border-b border-[var(--divider-subtle)] px-4 py-3 sm:grid-cols-[10rem_minmax(0,1fr)]">
          <dt class="ui-label pt-0.5">{{ t('patient.urgTemp.admin.email') }}</dt>
          <dd class="min-w-0 break-words text-sm font-extrabold leading-5 text-[var(--text-primary)] [overflow-wrap:anywhere]">{{ valueOrFallback(currentPatient.email) }}</dd>
        </div>

        <div class="grid grid-cols-[minmax(7rem,0.75fr)_minmax(0,1.25fr)] items-start gap-3 border-b border-[var(--divider-subtle)] px-4 py-3 sm:grid-cols-[10rem_minmax(0,1fr)]">
          <dt class="ui-label pt-0.5">{{ t('patient.urgTemp.admin.city') }}</dt>
          <dd class="min-w-0 break-words text-sm font-extrabold leading-5 text-[var(--text-primary)]">{{ valueOrFallback(currentPatient.city) }}</dd>
        </div>

        <div class="grid grid-cols-[minmax(7rem,0.75fr)_minmax(0,1.25fr)] items-start gap-3 border-b border-[var(--divider-subtle)] px-4 py-3 sm:grid-cols-[10rem_minmax(0,1fr)]">
          <dt class="ui-label pt-0.5">{{ t('patient.urgTemp.admin.district') }}</dt>
          <dd class="min-w-0 break-words text-sm font-extrabold leading-5 text-[var(--text-primary)]">{{ valueOrFallback(currentPatient.district) }}</dd>
        </div>

        <div class="grid grid-cols-[minmax(7rem,0.75fr)_minmax(0,1.25fr)] items-start gap-3 border-b border-[var(--divider-subtle)] px-4 py-3 sm:grid-cols-[10rem_minmax(0,1fr)]">
          <dt class="ui-label pt-0.5">{{ t('patient.urgTemp.admin.address') }}</dt>
          <dd class="min-w-0 break-words text-sm font-extrabold leading-5 text-[var(--text-primary)] [overflow-wrap:anywhere]">{{ valueOrFallback(currentPatient.address) }}</dd>
        </div>

        <div class="grid grid-cols-[minmax(7rem,0.75fr)_minmax(0,1.25fr)] items-start gap-3 px-4 py-3 sm:grid-cols-[10rem_minmax(0,1fr)]">
          <dt class="ui-label pt-0.5">{{ t('patient.urgTemp.admin.bloodGroup') }}</dt>
          <dd class="min-w-0 text-sm font-extrabold leading-5 text-[var(--text-primary)]">{{ valueOrFallback(currentPatient.bloodGroup) }}</dd>
        </div>
      </dl>

      <!-- Emergency contact remains secondary inside the identity view. -->
      <section class="border-t border-[var(--divider-subtle)] pt-3">
        <button
          type="button"
          class="flex min-h-11 w-full items-center justify-between gap-3 text-left"
          [attr.aria-expanded]="contactExpanded()"
          (click)="toggleContact()"
          data-testid="patient-emergency-contact-toggle"
        >
          <h3 class="whitespace-nowrap text-sm font-extrabold text-[var(--text-primary)]">
            {{ t('patient.urgTemp.contact.title') }}
          </h3>
          <svg
            class="h-4 w-4 shrink-0 text-[var(--text-muted)] transition-transform"
            [class.rotate-180]="contactExpanded()"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            aria-hidden="true"
          >
            <path stroke-linecap="round" stroke-linejoin="round" d="m6 9 6 6 6-6" />
          </svg>
        </button>

        @if (contactExpanded()) {
          <dl class="mt-2 overflow-hidden rounded-md border border-[var(--app-border)] bg-[var(--app-surface)] animate-fade-in">
            <div class="grid grid-cols-[minmax(7rem,0.75fr)_minmax(0,1.25fr)] items-start gap-3 border-b border-[var(--divider-subtle)] px-4 py-3 sm:grid-cols-[10rem_minmax(0,1fr)]">
              <dt class="ui-label pt-0.5">{{ t('patient.urgTemp.contact.name') }}</dt>
              <dd class="min-w-0 break-words text-sm font-extrabold leading-5 text-[var(--text-primary)]">{{ valueOrFallback(currentPatient.emergencyContactName) }}</dd>
            </div>
            <div class="grid grid-cols-[minmax(7rem,0.75fr)_minmax(0,1.25fr)] items-start gap-3 px-4 py-3 sm:grid-cols-[10rem_minmax(0,1fr)]">
              <dt class="ui-label pt-0.5">{{ t('patient.urgTemp.contact.phone') }}</dt>
              <dd class="min-w-0 break-words text-sm font-extrabold leading-5 text-[var(--text-primary)]">{{ valueOrFallback(currentPatient.emergencyContactPhone) }}</dd>
            </div>
          </dl>
        }
      </section>
    </div>
  `,
})
export class PatientAdministrativeSummaryComponent {
  readonly patient = input.required<Patient>();
  readonly contactExpanded = signal(false);
  private readonly i18n = inject(I18nService);

  t(key: string): string {
    return this.i18n.t(key);
  }

  toggleContact(): void {
    this.contactExpanded.update(value => !value);
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
    return `${formattedDate} · ${Math.max(age, 0)} ${this.t('patient.urgTemp.admin.ageUnit')}`;
  }
}
