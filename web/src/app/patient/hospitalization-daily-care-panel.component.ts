import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../core/i18n/i18n.service';
import { PatientApiService } from './patient-api.service';

interface DailyCare {
  id: string;
  careType: string;
  description?: string;
  billable: boolean;
  price?: number | null;
  performedBy: string;
  performedAt: string;
}

@Component({
  selector: 'app-hospitalization-daily-care-panel',
  standalone: true,
  imports: [DatePipe, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="space-y-4" role="tabpanel" aria-label="Soins journaliers">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 class="text-sm font-bold text-[var(--text-primary)]">{{ t('patients.hospitalization.dailyCare.title', 'Soins journaliers') }}</h3>
          <p class="mt-1 text-xs text-[var(--text-muted)]">{{ t('patients.hospitalization.dailyCare.hint', 'Consignez chaque soin réalisé pendant le séjour.') }}</p>
        </div>
        @if (canModify()) {
          <button type="button" class="ui-button ui-button-primary" (click)="showForm.set(!showForm())">{{ showForm() ? t('common.cancel', 'Annuler') : t('patients.hospitalization.dailyCare.add', 'Ajouter un soin') }}</button>
        }
      </div>

      @if (showForm()) {
        <form class="grid gap-4 border border-[var(--app-border)] bg-[var(--app-surface-muted)] p-4 md:grid-cols-2" (submit)="save($event)">
          <label class="text-xs font-semibold text-[var(--text-secondary)]">
            {{ t('patients.hospitalization.dailyCare.type', 'Type de soin') }}
            <select class="ui-select mt-1" [ngModel]="careType()" (ngModelChange)="careType.set($event)" name="careType">
              <option value="PANSEMENT">{{ t('patients.hospitalization.dailyCare.dressing', 'Pansement') }}</option>
              <option value="INJECTION">{{ t('patients.hospitalization.dailyCare.injection', 'Injection') }}</option>
              <option value="PERFUSION">{{ t('patients.hospitalization.dailyCare.infusion', 'Perfusion') }}</option>
              <option value="AUTRE">{{ t('patients.hospitalization.dailyCare.other', 'Autre') }}</option>
            </select>
          </label>
          <label class="text-xs font-semibold text-[var(--text-secondary)]">
            {{ t('patients.hospitalization.dailyCare.description', 'Observation clinique') }}
            <input class="ui-input mt-1" [ngModel]="description()" (ngModelChange)="description.set($event)" name="description" />
          </label>
          <label class="flex items-center gap-2 text-xs font-semibold text-[var(--text-secondary)]">
            <input type="checkbox" [ngModel]="billable()" (ngModelChange)="billable.set($event)" name="billable" />
            {{ t('patients.hospitalization.dailyCare.billable', 'Générer un acte facturable') }}
          </label>
          @if (billable()) {
            <label class="text-xs font-semibold text-[var(--text-secondary)]">
              {{ t('patients.hospitalization.dailyCare.price', 'Tarif FCFA') }}
              <input type="number" min="0" class="ui-input mt-1" [ngModel]="price()" (ngModelChange)="price.set($event)" name="price" />
            </label>
          }
          <div class="flex items-end justify-end gap-2 md:col-span-2">
            <button type="submit" class="ui-button ui-button-primary" [disabled]="saving()">{{ t('common.save', 'Enregistrer') }}</button>
          </div>
          @if (error()) { <p class="text-xs font-semibold text-[var(--brand-danger-text)] md:col-span-2" role="alert">{{ error() }}</p> }
        </form>
      }

      @if (loading()) {
        <p class="py-6 text-center text-sm text-[var(--text-muted)]">{{ t('common.loading', 'Chargement…') }}</p>
      } @else if (cares().length === 0) {
        <div class="border border-dashed border-[var(--app-border)] p-6 text-center text-sm text-[var(--text-muted)]">{{ t('patients.hospitalization.dailyCare.empty', 'Aucun soin enregistré pour ce séjour.') }}</div>
      } @else {
        <div class="overflow-x-auto border border-[var(--app-border)]">
          <table class="min-w-full text-left text-sm">
            <thead class="bg-[var(--app-surface-muted)] text-xs uppercase tracking-wider text-[var(--text-muted)]">
              <tr><th scope="col" class="px-3 py-2">{{ t('patients.hospitalization.dailyCare.type', 'Type de soin') }}</th><th scope="col" class="px-3 py-2">{{ t('patients.hospitalization.dailyCare.description', 'Observation clinique') }}</th><th scope="col" class="px-3 py-2">{{ t('patients.hospitalization.dailyCare.performedBy', 'Réalisé par') }}</th><th scope="col" class="px-3 py-2">{{ t('patients.hospitalization.dailyCare.performedAt', 'Horodatage') }}</th></tr>
            </thead>
            <tbody class="divide-y divide-[var(--app-border)]">
              @for (care of cares(); track care.id) {
                <tr><td class="px-3 py-3 font-semibold text-[var(--text-primary)]">{{ care.careType }}</td><td class="px-3 py-3 text-[var(--text-secondary)]">{{ care.description || '—' }}</td><td class="px-3 py-3 text-[var(--text-secondary)]">{{ care.performedBy }}</td><td class="px-3 py-3 whitespace-nowrap text-[var(--text-muted)]">{{ care.performedAt | date:'dd/MM/yyyy HH:mm' }}</td></tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `,
})
export class HospitalizationDailyCarePanelComponent {
  readonly hospitalizationId = input.required<string>();
  readonly canModify = input(false);
  readonly cares = signal<DailyCare[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly showForm = signal(false);
  readonly careType = signal('PANSEMENT');
  readonly description = signal('');
  readonly billable = signal(false);
  readonly price = signal<number | null>(null);

  private readonly patientApi = inject(PatientApiService);
  private readonly i18n = inject(I18nService);
  readonly t = (key: string, fallback: string) => this.i18n.t(key, fallback);

  constructor() {
    effect(() => this.load(this.hospitalizationId()));
  }

  save(event: Event): void {
    event.preventDefault();
    this.saving.set(true);
    this.error.set(null);
    this.patientApi.addDailyCare(this.hospitalizationId(), {
      careType: this.careType(),
      description: this.description().trim(),
      billable: this.billable(),
      price: this.billable() ? this.price() : null,
      performedAt: new Date().toISOString(),
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.showForm.set(false);
        this.description.set('');
        this.billable.set(false);
        this.price.set(null);
        this.load(this.hospitalizationId());
      },
      error: () => {
        this.saving.set(false);
        this.error.set(this.t('common.error.server', 'Une erreur est survenue.'));
      },
    });
  }

  private load(hospitalizationId: string): void {
    this.loading.set(true);
    this.patientApi.getDailyCares(hospitalizationId).subscribe({
      next: (cares) => {
        this.cares.set(cares);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(this.t('common.error.server', 'Une erreur est survenue.'));
      },
    });
  }
}
