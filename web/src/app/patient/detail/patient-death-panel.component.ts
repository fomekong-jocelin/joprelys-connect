import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RbacApiService } from '../../clinic/rbac/rbac-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { PatientApiService } from '../patient-api.service';
import { Patient } from '../patient.models';

/**
 * Statut vital du patient : bandeau « décédé » et déclaration/annulation par un médecin (ADR-20261017 D-07).
 * La déclaration clôt les épisodes en cours ; le séjour éventuel attend le départ du corps.
 */
@Component({
  selector: 'app-patient-death-panel',
  standalone: true,
  imports: [DatePipe, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (isDeceased()) {
      <section class="ui-card border-l-4 border-[var(--text-primary)] p-4 sm:p-5" role="status" data-testid="patient-deceased-banner">
        <p class="text-sm font-bold text-[var(--text-primary)]">
          {{ t('patients.death.deceasedOn') }} {{ patient().deceasedAt | date:'dd/MM/yyyy HH:mm' }}
          @if (patient().deathPlace) { — {{ patient().deathPlace }} }
        </p>
        <p class="mt-1 text-xs text-[var(--text-muted)]">{{ t('patients.death.deceasedHint') }}</p>
        @if (canDeclare()) {
          @if (!revokeOpen()) {
            <button type="button" class="ui-button ui-button-secondary mt-3" (click)="revokeOpen.set(true)">{{ t('patients.death.revoke') }}</button>
          } @else {
            <form class="mt-3 space-y-2" (submit)="revoke($event)">
              <label class="block text-xs font-semibold text-[var(--text-secondary)]" for="death-revoke-reason">{{ t('patients.death.revokeReason') }}</label>
              <input id="death-revoke-reason" class="ui-input" name="revokeReason" maxlength="500" [(ngModel)]="revokeReason" />
              <div class="flex gap-2">
                <button type="button" class="ui-button ui-button-secondary" (click)="revokeOpen.set(false)">{{ t('common.cancel') }}</button>
                <button type="submit" class="ui-button ui-button-primary" [disabled]="!revokeReason.trim() || submitting()">{{ t('patients.death.confirmRevoke') }}</button>
              </div>
            </form>
          }
        }
      </section>
    } @else if (canDeclare()) {
      @if (!declareOpen()) {
        <div class="flex justify-end">
          <button type="button" class="ui-button ui-button-secondary" (click)="openDeclaration()">{{ t('patients.death.declare') }}</button>
        </div>
      } @else {
        <form class="ui-card space-y-3 p-4 sm:p-5" (submit)="declare($event)" data-testid="patient-death-form">
          <h3 class="text-sm font-bold text-[var(--text-primary)]">{{ t('patients.death.declare') }}</h3>
          <p class="text-xs text-[var(--text-muted)]">{{ t('patients.death.declareHint') }}</p>
          <div class="grid gap-3 md:grid-cols-2">
            <label class="text-xs font-semibold text-[var(--text-secondary)]">{{ t('patients.death.deceasedAt') }}
              <input type="datetime-local" class="ui-input mt-1" name="deceasedAt" [(ngModel)]="deceasedAt" required />
            </label>
            <label class="text-xs font-semibold text-[var(--text-secondary)]">{{ t('patients.death.place') }}
              <input class="ui-input mt-1" name="place" maxlength="200" [(ngModel)]="place" />
            </label>
          </div>
          <label class="block text-xs font-semibold text-[var(--text-secondary)]">{{ t('patients.death.circumstances') }}
            <textarea class="ui-textarea mt-1" rows="2" name="circumstances" maxlength="1000" [(ngModel)]="circumstances"></textarea>
          </label>
          <label class="flex items-start gap-2 text-xs text-[var(--text-secondary)]">
            <input type="checkbox" class="mt-0.5 h-4 w-4" name="confirmed" [(ngModel)]="confirmed" />
            <span>{{ t('patients.death.confirmation') }}</span>
          </label>
          @if (error()) { <p class="text-xs font-semibold text-[var(--brand-danger-text)]" role="alert">{{ error() }}</p> }
          <div class="flex justify-end gap-2">
            <button type="button" class="ui-button ui-button-secondary" (click)="declareOpen.set(false)" [disabled]="submitting()">{{ t('common.cancel') }}</button>
            <button type="submit" class="ui-button ui-button-danger" [disabled]="!confirmed || !deceasedAt || submitting()">{{ t('patients.death.confirmDeclare') }}</button>
          </div>
        </form>
      }
    }
  `,
})
export class PatientDeathPanelComponent {
  readonly patient = input.required<Patient>();
  readonly changed = output<void>();

  private readonly i18n = inject(I18nService);
  private readonly rbac = inject(RbacApiService);
  private readonly patientApi = inject(PatientApiService);

  readonly isDeceased = computed(() => this.patient().status === 'DECEASED');
  readonly canDeclare = computed(() => this.rbac.hasPermission('PATIENT_DEATH_DECLARE'));
  readonly declareOpen = signal(false);
  readonly revokeOpen = signal(false);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  deceasedAt = '';
  place = '';
  circumstances = '';
  confirmed = false;
  revokeReason = '';

  t(key: string): string {
    return this.i18n.t(key);
  }

  openDeclaration(): void {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    this.deceasedAt = now.toISOString().slice(0, 16);
    this.place = '';
    this.circumstances = '';
    this.confirmed = false;
    this.error.set(null);
    this.declareOpen.set(true);
  }

  declare(event: Event): void {
    event.preventDefault();
    if (!this.confirmed || !this.deceasedAt || this.submitting()) return;
    this.submitting.set(true);
    this.error.set(null);
    this.patientApi.declareDeath(this.patient().id, {
      deceasedAt: new Date(this.deceasedAt).toISOString(),
      place: this.place.trim() || undefined,
      circumstances: this.circumstances.trim() || undefined,
    }).subscribe({
      next: () => { this.submitting.set(false); this.declareOpen.set(false); this.changed.emit(); },
      error: (err) => { this.submitting.set(false); this.error.set(err.error?.detail || this.t('patients.death.error')); },
    });
  }

  revoke(event: Event): void {
    event.preventDefault();
    const reason = this.revokeReason.trim();
    if (!reason || this.submitting()) return;
    this.submitting.set(true);
    this.patientApi.revokeDeath(this.patient().id, reason).subscribe({
      next: () => { this.submitting.set(false); this.revokeOpen.set(false); this.revokeReason = ''; this.changed.emit(); },
      error: (err) => { this.submitting.set(false); this.error.set(err.error?.detail || this.t('patients.death.error')); },
    });
  }
}
