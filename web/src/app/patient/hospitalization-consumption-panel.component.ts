import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../core/i18n/i18n.service';
import { PatientApiService } from './patient-api.service';

interface PatientConsumption { id: string; itemName: string; quantity: number; unitPrice: number; consumedBy: string; consumedAt: string; }

@Component({
  selector: 'app-hospitalization-consumption-panel', standalone: true, imports: [DatePipe, FormsModule], changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="space-y-4" role="tabpanel" aria-label="Consommables">
      <div class="flex flex-wrap items-center justify-between gap-3"><div><h3 class="text-sm font-bold text-[var(--text-primary)]">{{ t('patients.hospitalization.consumption.title', 'Consommables') }}</h3><p class="mt-1 text-xs text-[var(--text-muted)]">{{ t('patients.hospitalization.consumption.hint', 'Tracez les articles consommés pour ce séjour.') }}</p></div>@if (canModify()) { <button type="button" class="ui-button ui-button-primary" (click)="showForm.set(!showForm())">{{ showForm() ? t('common.cancel', 'Annuler') : t('patients.hospitalization.consumption.add', 'Ajouter un article') }}</button> }</div>
      @if (showForm()) {
        <form class="grid gap-4 border border-[var(--app-border)] bg-[var(--app-surface-muted)] p-4 md:grid-cols-3" (submit)="save($event)">
          <label class="text-xs font-semibold text-[var(--text-secondary)] md:col-span-2">{{ t('patients.hospitalization.consumption.item', 'Article') }}<input class="ui-input mt-1" [ngModel]="itemName()" (ngModelChange)="itemName.set($event)" name="itemName" required /></label>
          <label class="text-xs font-semibold text-[var(--text-secondary)]">{{ t('patients.hospitalization.consumption.quantity', 'Quantité') }}<input type="number" min="1" class="ui-input mt-1" [ngModel]="quantity()" (ngModelChange)="quantity.set($event)" name="quantity" required /></label>
          <label class="text-xs font-semibold text-[var(--text-secondary)]">{{ t('patients.hospitalization.consumption.price', 'Prix unitaire FCFA') }}<input type="number" min="0" class="ui-input mt-1" [ngModel]="unitPrice()" (ngModelChange)="unitPrice.set($event)" name="unitPrice" /></label>
          <div class="flex justify-end md:col-span-2"><button type="submit" class="ui-button ui-button-primary" [disabled]="saving() || !itemName().trim() || quantity() < 1">{{ t('common.save', 'Enregistrer') }}</button></div>
          @if (error()) { <p class="text-xs font-semibold text-[var(--brand-danger-text)] md:col-span-3" role="alert">{{ error() }}</p> }
        </form>
      }
      @if (loading()) { <p class="py-6 text-center text-sm text-[var(--text-muted)]">{{ t('common.loading', 'Chargement…') }}</p> } @else if (consumptions().length === 0) { <div class="border border-dashed border-[var(--app-border)] p-6 text-center text-sm text-[var(--text-muted)]">{{ t('patients.hospitalization.consumption.empty', 'Aucun article n’est enregistré pour ce séjour.') }}</div> } @else {
        <div class="overflow-x-auto border border-[var(--app-border)]"><table class="min-w-full text-left text-sm"><thead class="bg-[var(--app-surface-muted)] text-xs uppercase tracking-wider text-[var(--text-muted)]"><tr><th scope="col" class="px-3 py-2">{{ t('patients.hospitalization.consumption.item', 'Article') }}</th><th scope="col" class="px-3 py-2">{{ t('patients.hospitalization.consumption.quantity', 'Quantité') }}</th><th scope="col" class="px-3 py-2">{{ t('patients.hospitalization.consumption.price', 'Prix unitaire FCFA') }}</th><th scope="col" class="px-3 py-2">{{ t('patients.hospitalization.consumption.consumedBy', 'Consommé par') }}</th></tr></thead><tbody class="divide-y divide-[var(--app-border)]">@for (consumption of consumptions(); track consumption.id) { <tr><td class="px-3 py-3 font-semibold text-[var(--text-primary)]">{{ consumption.itemName }}</td><td class="px-3 py-3">{{ consumption.quantity }}</td><td class="px-3 py-3">{{ consumption.unitPrice }}</td><td class="px-3 py-3 text-[var(--text-secondary)]">{{ consumption.consumedBy }} · {{ consumption.consumedAt | date:'dd/MM/yyyy HH:mm' }}</td></tr> }</tbody></table></div>
      }
    </section>`,
})
export class HospitalizationConsumptionPanelComponent {
  readonly hospitalizationId = input.required<string>();
  readonly canModify = input(false);
  readonly consumptions = signal<PatientConsumption[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly showForm = signal(false);
  readonly itemName = signal('');
  readonly quantity = signal(1);
  readonly unitPrice = signal(0);

  private readonly patientApi = inject(PatientApiService);
  private readonly i18n = inject(I18nService);
  readonly t = (key: string, fallback: string) => this.i18n.t(key, fallback);

  constructor() {
    effect(() => this.load(this.hospitalizationId()));
  }

  save(event: Event): void {
    event.preventDefault();
    const itemName = this.itemName().trim();
    if (!itemName || this.quantity() < 1) return;

    this.saving.set(true);
    this.error.set(null);
    this.patientApi.addPatientConsumption(this.hospitalizationId(), {
      itemName,
      quantity: this.quantity(),
      unitPrice: this.unitPrice(),
      consumedAt: new Date().toISOString(),
    }).subscribe({
      next: () => this.handleSavedConsumption(),
      error: () => this.handleSaveError(),
    });
  }

  private handleSavedConsumption(): void {
    this.saving.set(false);
    this.showForm.set(false);
    this.itemName.set('');
    this.quantity.set(1);
    this.unitPrice.set(0);
    this.load(this.hospitalizationId());
  }

  private handleSaveError(): void {
    this.saving.set(false);
    this.error.set(this.t('common.error.server', 'Une erreur est survenue.'));
  }

  private load(hospitalizationId: string): void {
    this.loading.set(true);
    this.patientApi.getPatientConsumptions(hospitalizationId).subscribe({
      next: (consumptions) => {
        this.consumptions.set(consumptions);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(this.t('common.error.server', 'Une erreur est survenue.'));
      },
    });
  }
}
