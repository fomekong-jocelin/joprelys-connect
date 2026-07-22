import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../core/i18n/i18n.service';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { PatientApiService } from './patient-api.service';

interface MedicationAdministration {
  id: string;
  medicationName: string;
  dose: string;
  administeredBy: string;
  administeredAt: string;
}

@Component({
  selector: 'app-hospitalization-medication-panel',
  standalone: true,
  imports: [DatePipe, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="space-y-4" role="tabpanel" aria-label="Administration médicamenteuse">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 class="text-sm font-bold text-[var(--text-primary)]">{{ t('patients.hospitalization.medication.title', 'Administration médicamenteuse') }}</h3>
          <p class="mt-1 text-xs text-[var(--text-muted)]">{{ t('patients.hospitalization.medication.hint', 'Chaque administration est associée à un auteur et à un horodatage.') }}</p>
        </div>
        @if (canModify()) { <button type="button" class="ui-button ui-button-primary" (click)="showForm.set(!showForm())">{{ showForm() ? t('common.cancel', 'Annuler') : t('patients.hospitalization.medication.add', 'Administrer') }}</button> }
      </div>

      @if (showForm()) {
        <form class="grid gap-4 border border-[var(--app-border)] bg-[var(--app-surface-muted)] p-4 md:grid-cols-2" (submit)="save($event)">
          <label class="text-xs font-semibold text-[var(--text-secondary)]">{{ t('patients.hospitalization.medication.name', 'Médicament') }}<input class="ui-input mt-1" [ngModel]="name()" (ngModelChange)="name.set($event)" name="medicationName" required /></label>
          <label class="text-xs font-semibold text-[var(--text-secondary)]">{{ t('patients.hospitalization.medication.dose', 'Dose administrée') }}<input class="ui-input mt-1" [ngModel]="dose()" (ngModelChange)="dose.set($event)" name="dose" required /></label>
          <div class="flex justify-end md:col-span-2"><button type="submit" class="ui-button ui-button-primary" [disabled]="saving() || !name().trim() || !dose().trim()">{{ t('common.save', 'Enregistrer') }}</button></div>
          @if (error()) { <p class="text-xs font-semibold text-[var(--brand-danger-text)] md:col-span-2" role="alert">{{ error() }}</p> }
        </form>
      }

      @if (loading()) {
        <p class="py-6 text-center text-sm text-[var(--text-muted)]">{{ t('common.loading', 'Chargement…') }}</p>
      } @else if (administrations().length === 0) {
        <div class="border border-dashed border-[var(--app-border)] p-6 text-center text-sm text-[var(--text-muted)]">{{ t('patients.hospitalization.medication.empty', 'Aucune administration n’est enregistrée pour ce séjour.') }}</div>
      } @else {
        <ol class="space-y-3 border-l border-[var(--app-border)] pl-5">
          @for (administration of administrations(); track administration.id) {
            <li class="relative"><span class="absolute -left-[25px] top-1.5 h-2 w-2 border-2 border-[var(--app-surface)] bg-[var(--brand-primary)]"></span><article class="border border-[var(--app-border)] bg-[var(--app-surface)] p-3"><div class="flex flex-wrap items-center justify-between gap-2"><strong class="text-sm text-[var(--text-primary)]">{{ administration.medicationName }}</strong><time class="text-xs text-[var(--text-muted)]">{{ administration.administeredAt | date:'dd/MM/yyyy HH:mm' }}</time></div><p class="mt-1 text-sm text-[var(--text-secondary)]">{{ administration.dose }}</p><p class="mt-1 text-xs text-[var(--text-muted)]">{{ administration.administeredBy }}</p></article></li>
          }
        </ol>
      }
    </section>
  `,
})
export class HospitalizationMedicationPanelComponent {
  readonly hospitalizationId = input.required<string>();
  readonly parentCanModify = input(false, { alias: 'canModify' });
  readonly administrations = signal<MedicationAdministration[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly showForm = signal(false);
  readonly name = signal('');
  readonly dose = signal('');

  private readonly patientApi = inject(PatientApiService);
  private readonly rbacApi = inject(RbacApiService);
  private readonly i18n = inject(I18nService);
  readonly t = (key: string, fallback: string) => this.i18n.t(key, fallback);

  constructor() { effect(() => this.load(this.hospitalizationId())); }

  canModify(): boolean {
    return this.rbacApi.hasPermission('HOSPITALIZATION_MEDICATION_ADMINISTER');
  }

  save(event: Event): void {
    event.preventDefault();
    if (!this.canModify()) return;
    const medicationName = this.name().trim();
    const dose = this.dose().trim();
    if (!medicationName || !dose) return;
    this.saving.set(true);
    this.error.set(null);
    this.patientApi.addMedicationAdministration(this.hospitalizationId(), { medicationName, dose, prescriptionItemId: null, administeredAt: new Date().toISOString() }).subscribe({
      next: () => { this.saving.set(false); this.showForm.set(false); this.name.set(''); this.dose.set(''); this.load(this.hospitalizationId()); },
      error: () => { this.saving.set(false); this.error.set(this.t('common.error.server', 'Une erreur est survenue.')); },
    });
  }

  private load(hospitalizationId: string): void {
    this.loading.set(true);
    this.patientApi.getMedicationAdministrations(hospitalizationId).subscribe({
      next: (administrations) => { this.administrations.set(administrations); this.loading.set(false); },
      error: () => { this.loading.set(false); this.error.set(this.t('common.error.server', 'Une erreur est survenue.')); },
    });
  }
}
