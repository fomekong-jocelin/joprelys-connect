import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../core/i18n/i18n.service';
import { BillingApiService } from '../../patient/billing-api.service';
import { CashierCollectionQueueItem } from '../../patient/cashier-collection.models';
import { PatientSearchPickerComponent } from '../../patient/patient-search-picker.component';
import { Patient, PatientDeposit } from '../../patient/patient.models';

/**
 * Acomptes au poste caissier (ADR-20261017 D-16) : encaissement avant facturation, puis imputation
 * sur une facture validée du patient sans nouvel encaissement.
 */
@Component({
  selector: 'app-billing-deposits-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, PatientSearchPickerComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="ui-card space-y-4 p-4 sm:p-5" [attr.aria-label]="t('billing.deposit.title')">
      <div>
        <h3 class="text-sm font-bold text-[var(--text-primary)]">{{ t('billing.deposit.title') }}</h3>
        <p class="mt-1 text-xs text-[var(--text-muted)]">{{ t('billing.deposit.hint') }}</p>
      </div>
      <app-patient-search-picker inputId="deposit-patient" [selected]="patient()" (patientSelected)="selectPatient($event)" />

      @if (patient(); as selected) {
        <form class="grid gap-3 md:grid-cols-4" (submit)="collect($event)">
          <label class="text-xs font-semibold text-[var(--text-secondary)]">{{ t('billing.deposit.amount') }}
            <input class="ui-input mt-1" type="number" min="1" name="amount" [(ngModel)]="amount" required />
          </label>
          <label class="text-xs font-semibold text-[var(--text-secondary)]">{{ t('billing.deposit.method') }}
            <select class="ui-select mt-1" name="method" [(ngModel)]="method">
              <option value="CASH">{{ t('billing.payment.method.CASH') }}</option>
              <option value="CHECK">{{ t('billing.payment.method.CHECK') }}</option>
              <option value="BANK_TRANSFER">{{ t('billing.payment.method.BANK_TRANSFER') }}</option>
            </select>
          </label>
          <label class="text-xs font-semibold text-[var(--text-secondary)]">{{ t('billing.deposit.reference') }}
            <input class="ui-input mt-1" name="reference" maxlength="100" [(ngModel)]="reference" />
          </label>
          <div class="flex items-end">
            <button type="submit" class="ui-button ui-button-primary w-full" [disabled]="busy() || amount <= 0">{{ t('billing.deposit.collect') }}</button>
          </div>
        </form>

        @if (deposits().length > 0) {
          <ul class="divide-y divide-[var(--app-border)] border border-[var(--app-border)]">
            @for (deposit of deposits(); track deposit.id) {
              <li class="flex flex-wrap items-center justify-between gap-2 p-3 text-xs">
                <span><span class="mono">{{ deposit.depositNumber }}</span> — {{ deposit.amount | number:'1.0-0' }} FCFA
                  ({{ t('billing.deposit.remaining') }} {{ deposit.remainingAmount | number:'1.0-0' }} FCFA)</span>
                @if (deposit.remainingAmount > 0 && openInvoices().length > 0) {
                  <span class="flex flex-wrap items-center gap-2">
                    <select class="ui-select w-48" [ngModel]="targetInvoiceId()" (ngModelChange)="targetInvoiceId.set($event)" [name]="'invoice-' + deposit.id" [attr.aria-label]="t('billing.deposit.invoice')">
                      @for (invoice of openInvoices(); track invoice.invoiceId) {
                        <option [value]="invoice.invoiceId">{{ invoice.invoiceNumber }} — {{ invoice.patientRemainingAmount | number:'1.0-0' }} FCFA</option>
                      }
                    </select>
                    <button type="button" class="ui-button ui-button-secondary" (click)="apply(deposit)" [disabled]="busy() || !targetInvoiceId()">{{ t('billing.deposit.apply') }}</button>
                  </span>
                }
              </li>
            }
          </ul>
        }
        <p class="sr-only">{{ selected.fullName }}</p>
      }
      @if (message()) { <p class="text-xs font-semibold text-[var(--brand-success-text)]" role="status">{{ message() }}</p> }
      @if (error()) { <p class="text-xs font-semibold text-[var(--brand-danger-text)]" role="alert">{{ error() }}</p> }
    </section>
  `,
})
export class BillingDepositsPanelComponent {
  readonly changed = output<void>();

  private readonly billingApi = inject(BillingApiService);
  private readonly i18n = inject(I18nService);

  readonly patient = signal<Patient | null>(null);
  readonly deposits = signal<PatientDeposit[]>([]);
  private readonly queue = signal<CashierCollectionQueueItem[]>([]);
  readonly openInvoices = computed(() => this.queue().filter(item => item.patientId === this.patient()?.id));
  readonly targetInvoiceId = signal('');
  readonly busy = signal(false);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  amount = 0;
  method: 'CASH' | 'CHECK' | 'BANK_TRANSFER' = 'CASH';
  reference = '';

  t(key: string): string {
    return this.i18n.t(key);
  }

  selectPatient(patient: Patient | null): void {
    this.patient.set(patient);
    this.deposits.set([]);
    this.message.set(null);
    this.error.set(null);
    if (patient) this.refresh(patient.id);
  }

  collect(event: Event): void {
    event.preventDefault();
    const patient = this.patient();
    if (!patient || this.amount <= 0 || this.busy()) return;
    this.busy.set(true);
    this.billingApi.collectDeposit({ patientId: patient.id, amount: this.amount, method: this.method, reference: this.reference || undefined })
      .subscribe({
        next: (deposit) => {
          this.done(this.t('billing.deposit.collected') + ' ' + deposit.depositNumber);
          this.amount = 0;
          this.reference = '';
          this.refresh(patient.id);
        },
        error: (err) => this.fail(err),
      });
  }

  apply(deposit: PatientDeposit): void {
    const invoiceId = this.targetInvoiceId();
    if (!invoiceId || this.busy()) return;
    this.busy.set(true);
    this.billingApi.applyDeposit(invoiceId, deposit.id).subscribe({
      next: () => { this.done(this.t('billing.deposit.applied')); this.refresh(deposit.patientId); },
      error: (err) => this.fail(err),
    });
  }

  private refresh(patientId: string): void {
    this.billingApi.listDeposits(patientId).subscribe({ next: (deposits) => this.deposits.set(deposits), error: () => undefined });
    this.billingApi.listCashierCollectionQueue().subscribe({
      next: (items) => {
        this.queue.set(items);
        this.targetInvoiceId.set(this.openInvoices()[0]?.invoiceId ?? '');
      },
      error: () => undefined,
    });
  }

  private done(message: string): void {
    this.busy.set(false);
    this.error.set(null);
    this.message.set(message);
    this.changed.emit();
  }

  private fail(err: { error?: { detail?: string } }): void {
    this.busy.set(false);
    this.message.set(null);
    this.error.set(err?.error?.detail || this.t('billing.deposit.error'));
  }
}
