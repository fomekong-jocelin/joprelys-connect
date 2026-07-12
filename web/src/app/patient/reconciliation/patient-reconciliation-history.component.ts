import { Component, inject, input } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';
import { PatientReconciliationEvent } from './patient-reconciliation.models';

@Component({
  selector: 'app-patient-reconciliation-history',
  standalone: true,
  templateUrl: './patient-reconciliation-history.component.html',
})
export class PatientReconciliationHistoryComponent {
  private readonly i18n = inject(I18nService);

  readonly events = input.required<PatientReconciliationEvent[]>();
  readonly loading = input(false);

  t(key: string): string {
    return this.i18n.t(key);
  }

  formatDateTime(value: string): string {
    return new Intl.DateTimeFormat(this.i18n.locale(), {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(value));
  }
}
