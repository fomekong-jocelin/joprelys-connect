import { HospitalizationLocationApiService } from './hospitalization-location-api.service';
import { EligibleMedication } from './hospitalization-workflow.models';
import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../core/i18n/i18n.service';
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
    <section class="space-y-4" role="tabpanel" [attr.aria-label]="t('patients.hospitalization.medication.title')">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 class="text-sm font-bold text-[var(--text-primary)]">{{ t('patients.hospitalization.medication.title') }}</h3>
          <p class="mt-1 text-xs text-[var(--text-muted)]">{{ t('patients.hospitalization.medication.hint') }}</p>
        </div>
        @if (canModify()) { <button type="button" class="ui-button ui-button-primary" (click)="showForm.set(!showForm())">{{ showForm() ? t('common.cancel') : t('patients.hospitalization.medication.add') }}</button> }
      </div>

      @if (showForm()) {
        <form class="grid gap-4 border border-[var(--app-border)] bg-[var(--app-surface-muted)] p-4 md:grid-cols-2" (submit)="save($event)">
          <label class="text-xs font-semibold text-[var(--text-secondary)]">{{ t('patients.hospitalization.workflow.prescribedMedication') }}
            <select class="ui-select mt-1" [ngModel]="prescriptionItemId()" (ngModelChange)="prescriptionItemId.set($event)" name="prescriptionItemId" required [disabled]="optionsLoading() || !!optionsError()">
              <option value="">{{ t('patients.hospitalization.workflow.selectMedication') }}</option>
              @for (item of medications(); track item.prescriptionItemId) { <option [value]="item.prescriptionItemId">{{ item.medicationName }} — {{ item.dosage }} ({{ item.prescriptionNumber }})</option> }
            </select>
          </label>
          @if (optionsError()) { <p role="alert" class="text-[var(--brand-danger-text)]">{{ optionsError() }} <button type="button" class="ui-button ui-button-secondary" (click)="loadOptions()">{{ t('common.retry') }}</button></p> }
          @else if (!optionsLoading() && medications().length === 0) { <p role="status">{{ t('patients.hospitalization.workflow.noPrescription') }}</p> }

          <label class="text-xs font-semibold text-[var(--text-secondary)]">{{ t('patients.hospitalization.medication.dose') }}<input class="ui-input mt-1" [ngModel]="dose()" (ngModelChange)="dose.set($event)" name="dose" required /></label>
          <div class="flex justify-end md:col-span-2"><button type="submit" class="ui-button ui-button-primary" [disabled]="saving() || loading() || optionsLoading() || !!optionsError() || !prescriptionItemId() || !dose().trim()">{{ t('common.save') }}</button></div>
          @if (error()) { <p class="text-xs font-semibold text-[var(--brand-danger-text)] md:col-span-2" role="alert">{{ error() }}</p> }
        </form>
      }

      @if (loading()) {
        <p class="py-6 text-center text-sm text-[var(--text-muted)]">{{ t('common.loading') }}</p>
      } @else if (historyError()) {
        <p role="alert">{{ t('patients.hospitalization.workflow.loadError') }} <button type="button" class="ui-button ui-button-secondary" (click)="reloadHistory()">{{ t('common.retry') }}</button></p>
      } @else if (administrations().length === 0) {
        <div class="border border-dashed border-[var(--app-border)] p-6 text-center text-sm text-[var(--text-muted)]">{{ t('patients.hospitalization.medication.empty') }}</div>
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
  readonly canModify = input(false);
  readonly administrations = signal<MedicationAdministration[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly historyError = signal(false);
  readonly showForm = signal(false);
  readonly medications = signal<EligibleMedication[]>([]);
  readonly prescriptionItemId = signal('');
  readonly optionsLoading = signal(false);
  readonly optionsError = signal<string | null>(null);
  private readonly hospitalizationApi = inject(HospitalizationLocationApiService);
  readonly dose = signal('');

  private readonly patientApi = inject(PatientApiService);
  private readonly i18n = inject(I18nService);
  readonly t = (key: string, fallback?: string) => this.i18n.t(key, fallback);

  constructor() { effect(() => { this.load(this.hospitalizationId()); if (this.canModify()) this.loadOptions(); }); }

  save(event: Event): void {
    event.preventDefault();
    const item = this.medications().find(candidate => candidate.prescriptionItemId === this.prescriptionItemId());
    const medicationName = item?.medicationName;
    const dose = this.dose().trim();
    if (!this.canModify() || this.saving() || this.loading() || this.historyError() || this.optionsLoading() || this.optionsError() || !item || !medicationName || !dose) return;
    this.saving.set(true);
    this.error.set(null);
    this.patientApi.addMedicationAdministration(this.hospitalizationId(), { medicationName, dose, prescriptionItemId: item.prescriptionItemId, administeredAt: new Date().toISOString() }).subscribe({
      next: () => { this.saving.set(false); this.showForm.set(false); this.prescriptionItemId.set(''); this.dose.set(''); this.load(this.hospitalizationId()); },
      error: () => { this.saving.set(false); this.error.set(this.t('patients.hospitalization.workflow.medicationError')); this.loadOptions(); },
    });
  }

  loadOptions(): void {
    if (!this.canModify()) return;
    this.optionsLoading.set(true); this.optionsError.set(null);
    this.hospitalizationApi.eligibleMedications(this.hospitalizationId()).subscribe({
      next: items => { this.medications.set(items); this.optionsLoading.set(false); this.prescriptionItemId.set(''); },
      error: () => { this.medications.set([]); this.optionsLoading.set(false); this.optionsError.set(this.t('patients.hospitalization.workflow.loadPrescriptionsError')); },
    });
  }

  reloadHistory(): void { this.load(this.hospitalizationId()); }

  private load(hospitalizationId: string): void {
    this.loading.set(true); this.historyError.set(false);
    this.patientApi.getMedicationAdministrations(hospitalizationId).subscribe({
      next: (administrations) => { this.administrations.set(administrations); this.loading.set(false); },
      error: () => { this.loading.set(false); this.historyError.set(true); this.error.set(this.t('common.error.server')); },
    });
  }
}
