import { DatePipe } from '@angular/common';
import { Component, effect, inject, input, signal } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';
import { VisitApiService } from './visit-api.service';
import { VitalMeasurement } from './visit.models';
import { VitalAlertsComponent } from './vital-alerts.component';

/** Historique horodaté et signé des mesures de constantes d'une visite (plus récente en premier). */
@Component({
  selector: 'app-vitals-history',
  standalone: true,
  imports: [DatePipe, VitalAlertsComponent],
  template: `
    @if (error()) {
      <p class="text-xs font-semibold text-[var(--brand-danger-text)]">{{ i18n.t('vitals.history.loadError') }}</p>
    } @else if (measurements().length > 1) {
      <details class="rounded-[6px] border border-[var(--app-border)] bg-[var(--app-surface)]">
        <summary class="cursor-pointer px-3 py-2 text-xs font-bold text-[var(--text-secondary)]">
          {{ i18n.t('vitals.history.title') }} ({{ measurements().length }})
        </summary>
        <ol class="divide-y divide-[var(--app-border)] text-xs">
          @for (measure of measurements(); track measure.id) {
            <li class="space-y-1 px-3 py-2">
              <div class="flex flex-wrap items-center justify-between gap-2 text-[var(--text-muted)]">
                <span class="font-bold text-[var(--text-primary)]">{{ measure.recordedAt | date: 'dd/MM HH:mm' }}</span>
                <span>{{ measure.recordedByName || i18n.t('vitals.history.unknownAuthor') }}</span>
              </div>
              <p class="text-[var(--text-secondary)]">{{ summary(measure) }}</p>
              <app-vital-alerts [alerts]="measure.alerts ?? []" />
            </li>
          }
        </ol>
      </details>
    }
  `,
})
export class VitalsHistoryComponent {
  readonly i18n = inject(I18nService);
  private readonly visitApi = inject(VisitApiService);

  readonly visitId = input.required<string>();
  /** Change à chaque nouvelle mesure pour recharger l'historique. */
  readonly refreshKey = input<unknown>(null);

  readonly measurements = signal<VitalMeasurement[]>([]);
  readonly error = signal(false);

  constructor() {
    effect(() => {
      const visitId = this.visitId();
      this.refreshKey();
      this.visitApi.getVitalsHistory(visitId).subscribe({
        next: (items) => {
          this.measurements.set(items);
          this.error.set(false);
        },
        error: () => this.error.set(true),
      });
    });
  }

  summary(measure: VitalMeasurement): string {
    const parts: string[] = [];
    if (measure.temperature != null) parts.push(`${measure.temperature} °C`);
    if (measure.systolic != null || measure.diastolic != null) {
      parts.push(`${measure.systolic ?? '—'}/${measure.diastolic ?? '—'} mmHg`);
    }
    if (measure.pulse != null) parts.push(`${measure.pulse} bpm`);
    if (measure.spo2 != null) parts.push(`SpO2 ${measure.spo2} %`);
    if (measure.respiratoryRate != null) parts.push(`${measure.respiratoryRate} /min`);
    if (measure.glycemia != null) parts.push(`${measure.glycemia} g/L`);
    if (measure.painScale != null) parts.push(`${this.i18n.t('vitals.history.pain')} ${measure.painScale}/10`);
    return parts.join(' · ');
  }
}
