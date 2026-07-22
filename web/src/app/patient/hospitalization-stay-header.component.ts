import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { Hospitalization } from './patient.models';

@Component({
  selector: 'app-hospitalization-stay-header',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe],
  template: `
    <section class="ui-card overflow-hidden p-4 sm:p-6" aria-labelledby="stay-workspace-title">
      <div class="grid gap-5 border-b border-[var(--app-border)] pb-5 xl:grid-cols-[minmax(0,1fr)_minmax(28rem,34rem)] xl:items-start">
        <div class="min-w-0">
          <p class="text-xs font-bold uppercase tracking-widest text-[var(--text-muted)]">{{ t('patients.hospitalization', 'Hospitalisation') }}</p>
          <div class="mt-2 flex flex-wrap items-center gap-2">
            <span class="rounded-md border border-[var(--brand-success-border)] bg-[var(--brand-success-subtle)] px-2 py-1 text-xs font-bold text-[var(--brand-success-text)]">{{ t('patients.hospitalization.status.EN_COURS', 'En cours') }}</span>
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
          </dl>
        </div>
        <div class="grid w-full gap-2 sm:grid-cols-2 xl:max-w-[34rem]" [attr.aria-label]="t('patients.hospitalization.actions', 'Actions du séjour')">
          <button type="button" class="ui-button ui-button-secondary min-w-0 whitespace-normal px-3 text-center sm:col-span-2" (click)="entryPdf.emit()">{{ t('patients.hospitalization.downloadPdf', 'Télécharger le document') }}</button>
          @if (canTransfer()) {
            <button type="button" class="ui-button ui-button-secondary min-w-0 whitespace-normal px-3 text-center" (click)="transfer.emit()">{{ t('patients.hospitalization.transfer', 'Transférer de lit') }}</button>
          }
          @if (canDischarge()) {
            <button type="button" class="ui-button ui-button-danger min-w-0 whitespace-normal px-3 text-center" (click)="discharge.emit()">{{ t('patients.hospitalization.discharge', 'Déclarer la sortie') }}</button>
          }
        </div>
      </div>
      <div class="mt-5 border-l-2 border-[var(--brand-primary)] bg-[var(--app-surface-muted)] px-4 py-3 text-sm text-[var(--text-secondary)]">
        <p class="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">{{ t('patients.hospitalization.reason', 'Motif d’hospitalisation') }}</p>
        <p class="mt-1 leading-6">{{ stay().admissionReason }}</p>
      </div>
    </section>
  `,
})
export class HospitalizationStayHeaderComponent {
  readonly stay = input.required<Hospitalization>();
  readonly responsibleName = input.required<string>();
  readonly canModify = input.required<boolean>();
  readonly entryPdf = output<void>();
  readonly transfer = output<void>();
  readonly discharge = output<void>();

  private readonly i18n = inject(I18nService);
  private readonly rbacApi = inject(RbacApiService);

  readonly canTransfer = computed(() => this.rbacApi.hasPermission('HOSPITALIZATION_TRANSFER'));
  readonly canDischarge = computed(() => this.rbacApi.hasPermission('HOSPITALIZATION_DISCHARGE_DECIDE'));
  readonly t = (key: string, defaultValue: string) => this.i18n.t(key, defaultValue);
}
