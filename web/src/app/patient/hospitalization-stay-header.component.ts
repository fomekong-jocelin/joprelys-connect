import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../core/i18n/i18n.service';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { Hospitalization } from './patient.models';
import { PatientApiService } from './patient-api.service';

@Component({
  selector: 'app-hospitalization-stay-header',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, FormsModule],
  template: `
    <section class="ui-card overflow-hidden p-4 sm:p-6" aria-labelledby="stay-workspace-title">
      <div class="grid gap-5 border-b border-[var(--app-border)] pb-5 xl:grid-cols-[minmax(0,1fr)_minmax(28rem,34rem)] xl:items-start">
        <div class="min-w-0">
          <p class="text-xs font-bold uppercase tracking-widest text-[var(--text-muted)]">{{ t('patients.hospitalization', 'Hospitalisation') }}</p>
          <div class="mt-2 flex flex-wrap items-center gap-2">
            @if (awaitingPhysicalDeparture()) {
              <span class="rounded-md border border-[var(--brand-warning-border)] bg-[var(--brand-warning-subtle)] px-2 py-1 text-xs font-bold text-[var(--brand-warning-text)]">
                {{ t('patients.hospitalization.status.DISCHARGE_DECIDED', 'Sortie médicale décidée') }}
              </span>
            } @else {
              <span class="rounded-md border border-[var(--brand-success-border)] bg-[var(--brand-success-subtle)] px-2 py-1 text-xs font-bold text-[var(--brand-success-text)]">
                {{ t('patients.hospitalization.status.EN_COURS', 'En cours') }}
              </span>
            }
            <span class="font-mono text-xs font-semibold text-[var(--text-secondary)]">{{ stay().hospitalizationNumber }}</span>
          </div>
          <h2 id="stay-workspace-title" class="mt-3 break-words text-lg leading-7 font-bold text-[var(--text-primary)]">{{ stay().serviceName }} · {{ t('patients.hospitalization.room', 'Chambre') }} {{ stay().roomNumber }} · {{ t('patients.hospitalization.bed', 'Lit') }} {{ stay().bedNumber }}</h2>
          <dl class="mt-4 grid gap-x-8 gap-y-2 text-sm sm:grid-cols-2">
            <div>
              <dt class="text-xs font-semibold text-[var(--text-muted)]">{{ t('patients.hospitalization.responsiblePractitioner', 'Médecin responsable') }}</dt>
              <dd class="mt-0.5 font-medium text-[var(--text-secondary)]">{{ responsibleName() }}</dd>
            </div>
            <div>
              <dt class="text-xs font-semibold text-[var(--text-muted)]">{{ t('patients.hospitalization.admittedAt', 'Admis le') }}</dt>
              <dd class="mt-0.5 font-medium text-[var(--text-secondary)]">{{ stay().admittedAt | date:'dd/MM/yyyy HH:mm' }}</dd>
            </div>
            @if (stay().dischargeDecidedAt) {
              <div>
                <dt class="text-xs font-semibold text-[var(--text-muted)]">{{ t('patients.hospitalization.dischargeDecidedAt', 'Sortie décidée le') }}</dt>
                <dd class="mt-0.5 font-medium text-[var(--text-secondary)]">{{ stay().dischargeDecidedAt | date:'dd/MM/yyyy HH:mm' }}</dd>
              </div>
            }
          </dl>
        </div>
        <div class="grid w-full gap-2 sm:grid-cols-2 xl:max-w-[34rem]" [attr.aria-label]="t('patients.hospitalization.actions', 'Actions du séjour')">
          <button type="button" class="ui-button ui-button-secondary min-w-0 whitespace-normal px-3 text-center sm:col-span-2" (click)="entryPdf.emit()">{{ t('patients.hospitalization.downloadPdf', 'Télécharger le document') }}</button>
          @if (canTransfer()) {
            <button type="button" class="ui-button ui-button-secondary min-w-0 whitespace-normal px-3 text-center" (click)="transfer.emit()">{{ t('patients.hospitalization.transfer', 'Transférer de lit') }}</button>
          }
          @if (canDischarge()) {
            <button type="button" class="ui-button ui-button-danger min-w-0 whitespace-normal px-3 text-center" (click)="discharge.emit()">{{ t('patients.hospitalization.dischargeDecision', 'Décider la sortie médicale') }}</button>
          }
          @if (canConfirmPhysicalDeparture()) {
            <button type="button" class="ui-button ui-button-danger min-w-0 whitespace-normal px-3 text-center sm:col-span-2" (click)="openPhysicalDepartureDialog()">{{ t('patients.hospitalization.confirmPhysicalDeparture', 'Confirmer le départ physique') }}</button>
          }
        </div>
      </div>
      @if (awaitingPhysicalDeparture()) {
        <div class="mt-5 border-l-2 border-[var(--brand-warning)] bg-[var(--brand-warning-subtle)] px-4 py-3 text-sm text-[var(--brand-warning-text)]">
          <p class="font-bold">{{ t('patients.hospitalization.awaitingPhysicalDepartureTitle', 'Patient encore présent dans l’unité') }}</p>
          <p class="mt-1 leading-6">{{ t('patients.hospitalization.awaitingPhysicalDepartureDetail', 'La décision médicale est enregistrée, mais le séjour et le lit restent actifs jusqu’à la confirmation du départ physique.') }}</p>
        </div>
      }
      <div class="mt-5 border-l-2 border-[var(--brand-primary)] bg-[var(--app-surface-muted)] px-4 py-3 text-sm text-[var(--text-secondary)]">
        <p class="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">{{ t('patients.hospitalization.reason', 'Motif d’hospitalisation') }}</p>
        <p class="mt-1 leading-6">{{ stay().admissionReason }}</p>
      </div>
    </section>

    @if (showPhysicalDepartureDialog()) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-4 backdrop-blur-xs" role="dialog" aria-modal="true" aria-labelledby="physical-departure-title">
        <div class="w-full max-w-md overflow-hidden rounded-xl border border-[var(--app-border)] bg-[var(--app-surface)] shadow-xl">
          <header class="border-b border-[var(--app-border)] px-5 py-4">
            <h3 id="physical-departure-title" class="font-display font-bold text-[var(--text-primary)]">{{ t('patients.hospitalization.confirmPhysicalDeparture', 'Confirmer le départ physique') }}</h3>
            <p class="mt-1 text-xs leading-5 text-[var(--text-muted)]">{{ t('patients.hospitalization.physicalDepartureWarning', 'Cette action clôt le séjour, libère l’affectation et place le lit en nettoyage. Elle ne doit être réalisée qu’après le départ réel du patient.') }}</p>
          </header>
          <form class="space-y-4 p-5" (submit)="confirmPhysicalDeparture($event)">
            @if (physicalDepartureError()) {
              <div class="rounded-lg border border-[var(--brand-danger-border)] bg-[var(--brand-danger-subtle)] p-3 text-xs font-semibold text-[var(--brand-danger-text)]">
                {{ physicalDepartureError() }}
              </div>
            }
            <div class="space-y-1">
              <label for="physical-departure-note" class="block text-[10px] font-bold uppercase tracking-wider text-[var(--text-muted)]">{{ t('patients.hospitalization.physicalDepartureNote', 'Note de départ — facultative') }}</label>
              <textarea id="physical-departure-note" name="physicalDepartureNote" [(ngModel)]="physicalDepartureNote" maxlength="500" rows="3" class="ui-textarea" [disabled]="physicalDepartureSubmitting()"></textarea>
            </div>
            <label class="flex items-start gap-2 rounded-lg border border-[var(--brand-warning-border)] bg-[var(--brand-warning-subtle)] p-3 text-xs font-semibold text-[var(--brand-warning-text)]">
              <input type="checkbox" name="departureConfirmed" [(ngModel)]="departureConfirmed" class="mt-0.5 h-4 w-4" [disabled]="physicalDepartureSubmitting()" />
              <span>{{ t('patients.hospitalization.physicalDepartureExplicitConfirmation', 'Je confirme que le patient a physiquement quitté l’unité.') }}</span>
            </label>
            <footer class="flex justify-end gap-2 border-t border-[var(--app-border)] pt-4">
              <button type="button" class="ui-button ui-button-secondary" (click)="closePhysicalDepartureDialog()" [disabled]="physicalDepartureSubmitting()">{{ t('common.cancel', 'Annuler') }}</button>
              <button type="submit" class="ui-button ui-button-danger" [disabled]="!departureConfirmed || physicalDepartureSubmitting()">
                {{ physicalDepartureSubmitting() ? t('common.saving', 'Enregistrement…') : t('patients.hospitalization.confirmPhysicalDeparture', 'Confirmer le départ physique') }}
              </button>
            </footer>
          </form>
        </div>
      </div>
    }
  `,
})
export class HospitalizationStayHeaderComponent {
  readonly stay = input.required<Hospitalization>();
  readonly responsibleName = input.required<string>();
  readonly canModify = input.required<boolean>();
  readonly entryPdf = output<void>();
  readonly transfer = output<void>();
  readonly discharge = output<void>();

  readonly showPhysicalDepartureDialog = signal(false);
  readonly physicalDepartureError = signal<string | null>(null);
  readonly physicalDepartureSubmitting = signal(false);
  physicalDepartureNote = '';
  departureConfirmed = false;

  private readonly i18n = inject(I18nService);
  private readonly rbacApi = inject(RbacApiService);
  private readonly patientApi = inject(PatientApiService);

  readonly t = (key: string, defaultValue: string) => this.i18n.t(key, defaultValue);

  awaitingPhysicalDeparture(): boolean {
    return Boolean(this.stay().dischargeDecidedAt && !this.stay().physicalDepartureAt);
  }

  canTransfer(): boolean {
    return !this.awaitingPhysicalDeparture() && this.rbacApi.hasPermission('HOSPITALIZATION_TRANSFER');
  }

  canDischarge(): boolean {
    return !this.awaitingPhysicalDeparture()
      && this.rbacApi.hasPermission('HOSPITALIZATION_DISCHARGE_DECIDE');
  }

  canConfirmPhysicalDeparture(): boolean {
    return this.awaitingPhysicalDeparture()
      && this.rbacApi.hasPermission('HOSPITALIZATION_PHYSICAL_DEPARTURE_CONFIRM');
  }

  openPhysicalDepartureDialog(): void {
    this.physicalDepartureNote = '';
    this.departureConfirmed = false;
    this.physicalDepartureError.set(null);
    this.showPhysicalDepartureDialog.set(true);
  }

  closePhysicalDepartureDialog(): void {
    if (this.physicalDepartureSubmitting()) return;
    this.showPhysicalDepartureDialog.set(false);
  }

  confirmPhysicalDeparture(event: Event): void {
    event.preventDefault();
    if (!this.departureConfirmed || this.physicalDepartureSubmitting()) return;

    this.physicalDepartureError.set(null);
    this.physicalDepartureSubmitting.set(true);
    this.patientApi.confirmPhysicalDeparture(this.stay().id, {
      confirmed: true,
      note: this.physicalDepartureNote.trim() || undefined,
    }).subscribe({
      next: () => {
        this.physicalDepartureSubmitting.set(false);
        this.showPhysicalDepartureDialog.set(false);
        window.location.reload();
      },
      error: (err) => {
        this.physicalDepartureSubmitting.set(false);
        this.physicalDepartureError.set(
          err.error?.detail
          || err.error?.title
          || this.t('patients.hospitalization.physicalDepartureError', 'Impossible de confirmer le départ physique.'),
        );
      },
    });
  }
}
