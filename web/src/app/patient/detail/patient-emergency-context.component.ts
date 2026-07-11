import { Component, inject, input, OnInit, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { EmergencyApiService } from '../../emergency/emergency-api.service';
import { EmergencyRecord } from '../../emergency/emergency.models';
import { PatientEmergencyMedicoLegalSummaryComponent } from './patient-emergency-medico-legal-summary.component';

@Component({
  selector: 'app-patient-emergency-context',
  standalone: true,
  imports: [PatientEmergencyMedicoLegalSummaryComponent],
  template: `
    <section class="ui-card p-4 sm:p-5">
      <div class="flex items-start justify-between gap-3 border-b border-[var(--divider-subtle)] pb-3">
        <div>
          <h3 class="font-display text-sm font-black uppercase tracking-wider text-[var(--text-primary)]">
            {{ t('patient.urgTemp.emergency.title') }}
          </h3>
          <p class="mt-1 text-xs text-[var(--text-muted)]">
            {{ t('patient.urgTemp.emergency.subtitle') }}
          </p>
        </div>
        <span class="rounded-sm bg-[var(--brand-warning-subtle)] px-2 py-1 text-[10px] font-black uppercase tracking-wider text-[var(--brand-warning-text)]">
          {{ t('patient.urgTemp.emergency.historyPreserved') }}
        </span>
      </div>

      @if (loading()) {
        <p class="py-6 text-center text-sm text-[var(--text-muted)]">{{ t('patient.urgTemp.emergency.loading') }}</p>
      } @else if (error(); as message) {
        <p class="py-4 text-sm font-semibold text-[var(--brand-danger-text)]">{{ message }}</p>
      } @else if (emergencies().length === 0) {
        <p class="py-5 text-sm italic text-[var(--text-muted)]">{{ t('patient.urgTemp.emergency.empty') }}</p>
      } @else {
        <div class="mt-4 space-y-4">
          @for (emergency of emergencies(); track emergency.id) {
            <article class="rounded-sm border border-[var(--app-border)] bg-[var(--app-surface-muted)] p-3 sm:p-4">
              <header class="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <div class="flex flex-wrap items-center gap-2">
                    <strong class="text-sm text-[var(--text-primary)]">{{ formatDateTime(emergency.createdAt) }}</strong>
                    <span [class]="triageBadgeClass(emergency.triageLevel)">
                      {{ t('patient.urgTemp.emergency.triage') }} {{ emergency.triageLevel }}
                    </span>
                    @if (emergency.stabilizedAt) {
                      <span class="rounded-sm bg-[var(--brand-success-subtle)] px-2 py-0.5 text-[10px] font-black uppercase text-[var(--brand-success-text)]">
                        {{ t('patient.urgTemp.emergency.stabilized') }} — {{ emergency.orientation || t('patient.urgTemp.emergency.orientationMissing') }}
                      </span>
                    } @else {
                      <span class="rounded-sm bg-[var(--brand-danger-subtle)] px-2 py-0.5 text-[10px] font-black uppercase text-[var(--brand-danger-text)]">
                        {{ t('patient.urgTemp.emergency.active') }}
                      </span>
                    }
                    @if (emergency.thirdPartyRecorded) {
                      <span class="rounded-sm border border-[var(--app-border)] bg-[var(--app-surface)] px-2 py-0.5 text-[10px] font-bold text-[var(--text-muted)]">
                        {{ t('medicoLegal.thirdParty.recorded') }}
                      </span>
                    }
                  </div>
                  <p class="mt-2 text-sm font-semibold text-[var(--text-secondary)]">{{ emergency.chiefComplaint }}</p>
                </div>
                <div class="text-xs text-[var(--text-muted)]">
                  <div><strong>{{ t('patient.urgTemp.emergency.arrival') }} :</strong> {{ arrivalLabel(emergency.arrivalMode) }}</div>
                  @if (emergency.stabilizedAt) {
                    <div><strong>{{ t('patient.urgTemp.emergency.stabilizedAt') }} :</strong> {{ formatDateTime(emergency.stabilizedAt) }}</div>
                  }
                </div>
              </header>

              <dl class="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-4">
                <div class="rounded-sm border border-[var(--app-border)] bg-[var(--app-surface)] p-2">
                  <dt class="ui-label block">{{ t('patient.urgTemp.emergency.bp') }}</dt>
                  <dd class="text-sm font-bold text-[var(--text-primary)]">{{ emergency.initialBpSystolic ?? '—' }}/{{ emergency.initialBpDiastolic ?? '—' }} mmHg</dd>
                </div>
                <div class="rounded-sm border border-[var(--app-border)] bg-[var(--app-surface)] p-2">
                  <dt class="ui-label block">{{ t('patient.urgTemp.emergency.pulse') }}</dt>
                  <dd class="text-sm font-bold text-[var(--text-primary)]">{{ emergency.initialHr ?? '—' }} bpm</dd>
                </div>
                <div class="rounded-sm border border-[var(--app-border)] bg-[var(--app-surface)] p-2">
                  <dt class="ui-label block">{{ t('patient.urgTemp.emergency.temperature') }}</dt>
                  <dd class="text-sm font-bold text-[var(--text-primary)]">{{ emergency.initialTemp ?? '—' }} °C</dd>
                </div>
                <div class="rounded-sm border border-[var(--app-border)] bg-[var(--app-surface)] p-2">
                  <dt class="ui-label block">{{ t('patient.urgTemp.emergency.hemodynamic') }}</dt>
                  <dd class="text-sm font-bold text-[var(--text-primary)]">{{ emergency.hemodynamicStatus }}</dd>
                </div>
              </dl>

              <app-patient-emergency-medico-legal-summary [emergencyId]="emergency.id" />

              @if (emergency.resuscitationLogs?.length) {
                <section class="mt-3 border-t border-[var(--divider-subtle)] pt-3">
                  <h4 class="ui-label mb-2">{{ t('patient.urgTemp.emergency.careTimeline') }}</h4>
                  <div class="space-y-2">
                    @for (log of emergency.resuscitationLogs; track log.id) {
                      <div class="flex items-start justify-between gap-3 rounded-sm border border-[var(--app-border)] bg-[var(--app-surface)] p-2 text-xs">
                        <div>
                          <strong class="text-[var(--text-primary)]">{{ careLabel(log.actionType) }} — {{ log.description }}</strong>
                          @if (log.quantity) {
                            <span class="ml-1 font-bold text-[var(--brand-primary)]">({{ log.quantity }} {{ log.unit }})</span>
                          }
                        </div>
                        <time class="shrink-0 text-[var(--text-muted)]">{{ formatDateTime(log.administeredAt) }}</time>
                      </div>
                    }
                  </div>
                </section>
              }
            </article>
          }
        </div>
      }
    </section>
  `,
})
export class PatientEmergencyContextComponent implements OnInit {
  readonly patientId = input.required<string>();

  private readonly emergencyApi = inject(EmergencyApiService);
  private readonly i18n = inject(I18nService);

  readonly emergencies = signal<EmergencyRecord[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.loading.set(true);
    this.error.set(null);
    this.emergencyApi.getPatientEmergencies(this.patientId()).pipe(
      finalize(() => this.loading.set(false)),
    ).subscribe({
      next: records => this.emergencies.set(
        [...records].sort((first, second) => second.createdAt.localeCompare(first.createdAt)),
      ),
      error: () => this.error.set(this.t('patient.urgTemp.emergency.loadError')),
    });
  }

  t(key: string, fallback?: string): string {
    return this.i18n.t(key, fallback);
  }

  arrivalLabel(value?: string): string {
    return this.enumLabel('patient.urgTemp.emergency.arrival', value);
  }

  careLabel(value: string): string {
    return this.enumLabel('patient.urgTemp.emergency.care', value);
  }

  formatDateTime(value?: string | null): string {
    if (!value) return this.t('patient.urgTemp.common.notProvided');
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    const locale = this.i18n.locale() === 'en' ? 'en-GB' : 'fr-FR';
    return new Intl.DateTimeFormat(locale, { dateStyle: 'short', timeStyle: 'short' }).format(date);
  }

  triageBadgeClass(level?: string): string {
    const common = 'rounded-sm px-2 py-0.5 text-[10px] font-black uppercase';
    return level === 'RED'
      ? `${common} bg-[var(--brand-danger-subtle)] text-[var(--brand-danger-text)]`
      : `${common} bg-[var(--brand-warning-subtle)] text-[var(--brand-warning-text)]`;
  }

  private enumLabel(prefix: string, value?: string): string {
    if (!value) return this.t('patient.urgTemp.common.notProvided');
    return this.t(`${prefix}.${value}`, value);
  }
}
