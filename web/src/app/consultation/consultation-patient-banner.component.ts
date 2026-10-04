import { Component, computed, inject, input } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';
import { Patient, PatientAllergy } from '../patient/patient.models';
import { Vitals } from '../visit/visit.models';
import { VitalAlertsComponent } from '../visit/vital-alerts.component';

/**
 * Identité du patient et signaux de sécurité visibles dès l'ouverture de la consultation,
 * avant tout choix entre assistant IA et saisie manuelle.
 */
@Component({
  selector: 'app-consultation-patient-banner',
  standalone: true,
  imports: [VitalAlertsComponent],
  template: `
    @if (patient(); as p) {
      <section
        class="flex flex-col gap-3 rounded-[6px] border border-[var(--app-border)] bg-[var(--app-surface)] p-3 shadow-xs sm:flex-row sm:items-start sm:justify-between sm:p-4"
        [attr.aria-label]="i18n.t('consultation.banner.label')"
      >
        <div class="min-w-0">
          <p class="text-base font-extrabold text-[var(--text-primary)]">
            {{ p.displayName || p.fullName }}
            @if (age() !== null) {
              <span class="ml-1 text-sm font-semibold text-[var(--text-secondary)]">{{ age() }} {{ i18n.t('consultation.banner.years') }}</span>
            }
            @if (p.gender) {
              <span class="ml-1 text-xs font-semibold text-[var(--text-muted)]">{{ genderLabel(p.gender) }}</span>
            }
          </p>
          <p class="font-mono text-xs text-[var(--text-muted)]">{{ p.globalPatientNumber }}</p>
          @if (visitReason()) {
            <p class="mt-1 text-sm text-[var(--text-secondary)]">
              <span class="font-bold">{{ i18n.t('consultation.banner.reason') }} :</span> {{ visitReason() }}
            </p>
          }
        </div>
        <div class="flex min-w-0 flex-col gap-1.5 sm:items-end">
          @if (activeAllergies().length > 0) {
            <ul class="flex flex-wrap gap-1 sm:justify-end" [attr.aria-label]="i18n.t('consultation.clinicalNote.allergies')">
              @for (allergy of activeAllergies(); track allergy.id || allergy.substance) {
                <li class="rounded-[4px] border border-[var(--brand-danger-border)] bg-[var(--brand-danger-subtle)] px-1.5 py-0.5 text-[11px] font-bold text-[var(--brand-danger-text)]">
                  {{ i18n.t('consultation.banner.allergy') }} {{ allergy.substance }}
                </li>
              }
            </ul>
          } @else if (allergiesLoaded()) {
            <span class="text-xs text-[var(--text-muted)]">{{ i18n.t('consultation.clinicalNote.noKnownAllergy') }}</span>
          }
          <app-vital-alerts [alerts]="vitals()?.alerts ?? []" />
        </div>
      </section>
    }
  `,
})
export class ConsultationPatientBannerComponent {
  readonly i18n = inject(I18nService);

  readonly patient = input<Patient | null>(null);
  readonly allergies = input<readonly PatientAllergy[]>([]);
  readonly allergiesLoaded = input(true);
  readonly vitals = input<Vitals | null>(null);
  readonly visitReason = input('');

  /** Allergies actives, les plus graves en premier. */
  readonly activeAllergies = computed(() => {
    const rank = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 } as const;
    return this.allergies()
      .filter((allergy) => allergy.status !== 'INACTIVE')
      .slice()
      .sort((a, b) => rank[a.severity] - rank[b.severity]);
  });

  genderLabel(gender: string): string {
    if (gender === 'MASCULIN') return this.i18n.t('selfRegistration.genderMale');
    if (gender === 'FEMININ') return this.i18n.t('selfRegistration.genderFemale');
    return gender;
  }

  readonly age = computed(() => {
    const birthDate = this.patient()?.birthDate;
    if (!birthDate) return null;
    const birth = new Date(birthDate);
    const now = new Date();
    let years = now.getFullYear() - birth.getFullYear();
    if (now.getMonth() < birth.getMonth() || (now.getMonth() === birth.getMonth() && now.getDate() < birth.getDate())) {
      years--;
    }
    return years >= 0 ? years : null;
  });
}
