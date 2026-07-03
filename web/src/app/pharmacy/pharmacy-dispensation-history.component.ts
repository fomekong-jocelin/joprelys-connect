import { DatePipe } from '@angular/common';
import { Component, Input, inject } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';
import { PharmacyDispensationHistoryEntry } from './pharmacy.models';

@Component({
  selector: 'app-pharmacy-dispensation-history',
  standalone: true,
  imports: [DatePipe],
  template: `
    <aside class="ui-card-muted p-4 md:p-5">
      <div class="mb-4">
        <p class="ui-label">{{ t('pharmacy.historySection') }}</p>
        <h3 class="font-display text-base font-extrabold" style="color: var(--text-primary)">
          {{ t('pharmacy.historyTitle') }}
        </h3>
      </div>

      @if (loading) {
        <p class="text-sm font-bold" style="color: var(--text-muted)">{{ t('common.loading') }}</p>
      } @else if (error) {
        <p class="text-sm font-semibold" style="color: var(--brand-danger)">{{ error }}</p>
      } @else if (history.length === 0) {
        <p class="text-sm leading-relaxed" style="color: var(--text-secondary)">{{ t('pharmacy.historyEmpty') }}</p>
      } @else {
        <div class="space-y-3">
          @for (entry of history; track entry.dispensationId) {
            <article class="border p-3" style="border-radius: var(--radius-brand-md); border-color: var(--app-border); background: var(--app-surface);">
              <div class="flex items-start justify-between gap-3">
                <div>
                  <p class="text-sm font-black" style="color: var(--text-primary)">{{ entry.pharmacyName }}</p>
                  <p class="text-xs" style="color: var(--text-muted)">{{ entry.pharmacistLicense }}</p>
                </div>
                <time class="text-right text-xs font-bold" style="color: var(--text-muted)">{{ entry.dispensedAt | date:'short' }}</time>
              </div>
              <ul class="mt-3 space-y-2">
                @for (item of entry.items; track item.prescriptionItemId) {
                  <li class="text-xs" style="color: var(--text-secondary)">
                    <strong style="color: var(--text-primary)">{{ item.drugName }}</strong>
                    · {{ item.quantityDispensed }}
                    @if (item.substitutedWith) {
                      <span> · {{ t('pharmacy.substitutedWith') }}: {{ item.substitutedWith }}</span>
                    }
                  </li>
                }
              </ul>
            </article>
          }
        </div>
      }
    </aside>
  `,
})
export class PharmacyDispensationHistoryComponent {
  @Input() history: PharmacyDispensationHistoryEntry[] = [];
  @Input() loading = false;
  @Input() error = '';

  readonly i18n = inject(I18nService);

  t(key: string): string {
    return this.i18n.t(key);
  }
}
