import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';
import { BillingApiService } from '../../patient/billing-api.service';
import { Invoice } from '../../patient/patient.models';

/**
 * Lignes « à tarifer » d'une facture en attente (ADR-20261017 D-11) : aucune valeur inventée, la facture
 * se recalcule une fois la grille tarifaire complétée.
 */
@Component({
  selector: 'app-billing-tariff-alert',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (missingKeys().length > 0) {
      <div class="ui-alert-warning space-y-2 text-xs" role="alert">
        <p class="font-semibold">{{ t('billing.tariff.missingTitle') }}</p>
        <p>{{ t('billing.tariff.missingHint') }} <span class="mono">{{ missingKeys().join(', ') }}</span></p>
        @if (invoice().status === 'PENDING') {
          <button type="button" class="ui-button ui-button-secondary" (click)="recalculate()" [disabled]="busy()">{{ t('billing.tariff.recalculate') }}</button>
        }
        @if (error()) { <p class="font-semibold text-[var(--brand-danger-text)]">{{ error() }}</p> }
      </div>
    }
  `,
})
export class BillingTariffAlertComponent {
  readonly invoice = input.required<Invoice>();
  readonly recalculated = output<Invoice>();

  private readonly billingApi = inject(BillingApiService);
  private readonly i18n = inject(I18nService);

  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly missingKeys = computed(() => [...new Set((this.invoice().items ?? [])
    .filter(item => item.tariffMissing)
    .map(item => item.tariffKey || item.label))]);

  t(key: string): string {
    return this.i18n.t(key);
  }

  recalculate(): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set(null);
    this.billingApi.recalculateInvoice(this.invoice().id).subscribe({
      next: (invoice) => { this.busy.set(false); this.recalculated.emit(invoice); },
      error: (err) => { this.busy.set(false); this.error.set(err?.error?.detail || this.t('billing.tariff.recalculateError')); },
    });
  }
}
