import { Component, inject, input, output } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';
import { PatientReconciliationQueueItem } from './patient-reconciliation.models';

@Component({
  selector: 'app-patient-reconciliation-queue',
  standalone: true,
  templateUrl: './patient-reconciliation-queue.component.html',
})
export class PatientReconciliationQueueComponent {
  private readonly i18n = inject(I18nService);

  readonly items = input.required<PatientReconciliationQueueItem[]>();
  readonly selectedPatientId = input<string | null>(null);
  readonly selected = output<PatientReconciliationQueueItem>();

  t(key: string): string {
    return this.i18n.t(key);
  }

  formatDate(value: string | null): string {
    if (!value) return this.t('patientReconciliation.notRecorded');
    return new Intl.DateTimeFormat(this.i18n.locale(), {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(value));
  }
}
