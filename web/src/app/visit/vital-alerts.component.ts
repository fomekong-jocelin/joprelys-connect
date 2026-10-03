import { Component, inject, input } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';
import { VitalAlert } from './visit.models';

/** Pastilles d'alerte sur les constantes (calculées par le backend). */
@Component({
  selector: 'app-vital-alerts',
  standalone: true,
  template: `
    @if (alerts().length > 0) {
      <ul class="flex flex-wrap gap-1" [attr.aria-label]="i18n.t('vitals.alert.label')">
        @for (alert of alerts(); track alert.code) {
          <li
            class="inline-flex items-center gap-1 rounded-[4px] px-1.5 py-0.5 text-[10px] font-bold uppercase tracking-wide"
            [class]="alert.severity === 'CRITICAL'
              ? 'bg-[var(--brand-danger-subtle)] text-[var(--brand-danger-text)]'
              : 'bg-[var(--brand-warning-subtle)] text-[var(--brand-warning-text)]'"
          >
            <span aria-hidden="true">{{ alert.severity === 'CRITICAL' ? '!!' : '!' }}</span>
            {{ i18n.t('vitals.alert.' + alert.code) }}
          </li>
        }
      </ul>
    }
  `,
})
export class VitalAlertsComponent {
  readonly i18n = inject(I18nService);
  readonly alerts = input<readonly VitalAlert[]>([]);
}
