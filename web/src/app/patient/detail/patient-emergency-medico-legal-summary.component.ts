import { CommonModule, DatePipe } from '@angular/common';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { ApiErrorI18nService } from '../../core/i18n/api-error-i18n.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { EmergencyMedicoLegalApiService } from '../../emergency/medico-legal/emergency-medico-legal-api.service';
import { EmergencyMedicoLegalDossier } from '../../emergency/medico-legal/emergency-medico-legal.models';

@Component({
  selector: 'app-patient-emergency-medico-legal-summary',
  standalone: true,
  imports: [CommonModule, DatePipe],
  template: `
    @if (loading()) {
      <p class="mt-3 text-xs font-semibold text-[var(--text-muted)]">{{ t('medicoLegal.loading') }}</p>
    } @else if (dossier(); as data) {
      @if (data.thirdParties.length || data.capacityHistory.length || data.legalBases.length || data.belongings.length) {
        <section class="mt-3 rounded-sm border border-[var(--app-border)] bg-[var(--app-surface)] p-3">
          <div class="flex items-start justify-between gap-2">
            <div>
              <h4 class="text-xs font-black uppercase tracking-wider text-[var(--text-primary)]">
                {{ t('medicoLegal.patientHistory.title') }}
              </h4>
              <p class="mt-1 text-[11px] leading-4 text-[var(--text-muted)]">
                {{ t('medicoLegal.patientHistory.protected') }}
              </p>
            </div>
            <span class="rounded-sm border border-[var(--app-border)] bg-[var(--app-surface-muted)] px-2 py-1 text-[10px] font-bold text-[var(--text-muted)]">
              {{ data.thirdParties.length + data.belongings.length }}
            </span>
          </div>

          @if (data.thirdParties.length) {
            <div class="mt-3 space-y-2">
              <p class="ui-label">{{ t('medicoLegal.thirdParty.title') }}</p>
              @for (thirdParty of data.thirdParties; track thirdParty.id) {
                <div class="rounded-sm bg-[var(--app-surface-muted)] px-3 py-2 text-xs">
                  <div class="flex flex-wrap items-start justify-between gap-2">
                    <strong class="text-[var(--text-primary)]">{{ thirdParty.fullName }}</strong>
                    <span class="text-[10px] font-bold text-[var(--text-muted)]">{{ thirdParty.createdAt | date:'dd/MM/yyyy HH:mm' }}</span>
                  </div>
                  <p class="mt-1 text-[var(--text-secondary)]">
                    {{ thirdParty.phone || t('medicoLegal.notRecorded') }}
                    @if (thirdParty.relationshipToPatient) { · {{ thirdParty.relationshipToPatient }} }
                  </p>
                  <div class="mt-2 flex flex-wrap gap-1">
                    @for (quality of thirdParty.qualities; track quality) {
                      <span class="rounded-sm border border-[var(--app-border)] bg-[var(--app-surface)] px-2 py-0.5 text-[10px] font-semibold text-[var(--text-secondary)]">
                        {{ t('medicoLegal.thirdParty.quality.' + quality.toLowerCase()) }}
                      </span>
                    }
                  </div>
                </div>
              }
            </div>
          }

          @if (data.currentCapacity; as capacity) {
            <div class="mt-3 border-t border-[var(--divider-subtle)] pt-3 text-xs">
              <span class="ui-label">{{ t('medicoLegal.capacity.current') }} :</span>
              <strong class="ml-1" [class.text-[var(--brand-danger-text)]]="capacity.status === 'INCAPABLE'" [class.text-[var(--brand-success-text)]]="capacity.status === 'CAPABLE'">
                {{ t('medicoLegal.capacity.status.' + capacity.status.toLowerCase()) }}
              </strong>
              <p class="mt-1 text-[var(--text-secondary)]">{{ capacity.clinicalReason }}</p>
            </div>
          }

          @if (data.belongings.length) {
            <div class="mt-3 border-t border-[var(--divider-subtle)] pt-3">
              <p class="ui-label">{{ t('medicoLegal.belonging.title') }}</p>
              <div class="mt-2 flex flex-wrap gap-1.5">
                @for (item of data.belongings; track item.id) {
                  <span class="rounded-sm border border-[var(--app-border)] bg-[var(--app-surface-muted)] px-2 py-1 text-[10px] font-semibold text-[var(--text-secondary)]">
                    {{ item.description }} · {{ t('medicoLegal.belonging.status.' + item.custodyStatus.toLowerCase()) }}
                  </span>
                }
              </div>
            </div>
          }
        </section>
      }
    } @else if (error()) {
      <p class="mt-3 rounded-sm border border-[var(--app-border)] bg-[var(--app-surface)] px-3 py-2 text-xs text-[var(--text-muted)]">
        {{ t('medicoLegal.patientHistory.restricted') }}
      </p>
    }
  `,
})
export class PatientEmergencyMedicoLegalSummaryComponent implements OnInit {
  private readonly api = inject(EmergencyMedicoLegalApiService);
  private readonly apiErrors = inject(ApiErrorI18nService);
  private readonly i18n = inject(I18nService);

  readonly emergencyId = input.required<string>();
  readonly dossier = signal<EmergencyMedicoLegalDossier | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.loading.set(true);
    this.api.getDossier(this.emergencyId()).pipe(
      finalize(() => this.loading.set(false)),
    ).subscribe({
      next: dossier => this.dossier.set(dossier),
      error: error => this.error.set(this.apiErrors.message(
        error,
        'medicoLegal.error',
        'medicoLegal.error.load',
      )),
    });
  }

  t(key: string): string {
    return this.i18n.t(key);
  }
}
