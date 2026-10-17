import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../core/i18n/i18n.service';
import { BillingApiService } from '../../patient/billing-api.service';
import { CreditNote } from '../../patient/patient.models';
import { RbacApiService } from '../rbac/rbac-api.service';

/**
 * Avoirs d'une facture (ADR-20261017 D-17) : statut, approbation DAF au-delà du seuil, remboursement en caisse.
 */
@Component({
  selector: 'app-billing-credit-notes-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (creditNotes().length > 0) {
      <div class="sub-section">
        <h4 class="sub-section-title">{{ t('billing.creditNote.listTitle') }}</h4>
        @if (error()) { <p class="text-xs font-semibold text-[var(--brand-danger-text)]" role="alert">{{ error() }}</p> }
        <div class="table-wrapper">
          <table class="data-table data-table-sm">
            <thead>
              <tr>
                <th>{{ t('billing.creditNote.number') }}</th>
                <th>{{ t('billing.creditNote.amountColumn') }}</th>
                <th>{{ t('billing.creditNote.reasonColumn') }}</th>
                <th>{{ t('billing.creditNote.statusColumn') }}</th>
                <th>{{ t('billing.creditNote.refunded') }}</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (cn of creditNotes(); track cn.id) {
                <tr>
                  <td><span class="mono">{{ cn.creditNoteNumber }}</span><br /><span class="text-[10px]">{{ cn.createdAt | date:'dd/MM/yyyy' }}</span></td>
                  <td class="amount text-warning">-{{ cn.amount | number:'1.0-0' }} FCFA</td>
                  <td>{{ cn.reason }}</td>
                  <td>{{ t('billing.creditNote.status.' + cn.status) }}</td>
                  <td>{{ (cn.refundedAmount ?? 0) | number:'1.0-0' }} FCFA</td>
                  <td class="space-y-1">
                    @if (cn.status === 'PENDING_APPROVAL' && canApprove()) {
                      <button type="button" class="ui-button ui-button-primary" (click)="approve(cn)" [disabled]="busy()">{{ t('billing.creditNote.approve') }}</button>
                      <button type="button" class="ui-button ui-button-secondary" (click)="reject(cn)" [disabled]="busy()">{{ t('billing.creditNote.reject') }}</button>
                    }
                    @if (cn.status === 'ACTIVE' && canRefund() && (cn.refundedAmount ?? 0) < cn.amount) {
                      @if (refundTarget() !== cn.id) {
                        <button type="button" class="ui-button ui-button-secondary" (click)="openRefund(cn)">{{ t('billing.creditNote.refund') }}</button>
                      } @else {
                        <div class="flex flex-wrap items-center gap-1">
                          <input class="ui-input w-28" type="number" min="1" [(ngModel)]="refundAmount" [attr.aria-label]="t('billing.creditNote.refundAmount')" />
                          <select class="ui-select w-32" [(ngModel)]="refundMethod" [attr.aria-label]="t('billing.creditNote.refundMethod')">
                            <option value="CASH">{{ t('billing.payment.method.CASH') }}</option>
                            <option value="CHECK">{{ t('billing.payment.method.CHECK') }}</option>
                            <option value="BANK_TRANSFER">{{ t('billing.payment.method.BANK_TRANSFER') }}</option>
                          </select>
                          <button type="button" class="ui-button ui-button-primary" (click)="refund(cn)" [disabled]="busy() || refundAmount <= 0">{{ t('billing.creditNote.confirmRefund') }}</button>
                          <button type="button" class="ui-button ui-button-secondary" (click)="refundTarget.set(null)">{{ t('common.cancel') }}</button>
                        </div>
                      }
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    }
  `,
})
export class BillingCreditNotesPanelComponent {
  readonly creditNotes = input.required<CreditNote[]>();
  readonly changed = output<void>();

  private readonly billingApi = inject(BillingApiService);
  private readonly rbac = inject(RbacApiService);
  private readonly i18n = inject(I18nService);

  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly refundTarget = signal<string | null>(null);
  refundAmount = 0;
  refundMethod: 'CASH' | 'CHECK' | 'BANK_TRANSFER' = 'CASH';

  t(key: string): string {
    return this.i18n.t(key);
  }

  canApprove(): boolean {
    return this.rbac.hasPermission('BILLING_CREDIT_NOTE_APPROVE');
  }

  canRefund(): boolean {
    return this.rbac.hasPermission('CASH_PAYMENT_COLLECT');
  }

  approve(cn: CreditNote): void {
    this.run(this.billingApi.approveCreditNote(cn.id));
  }

  reject(cn: CreditNote): void {
    this.run(this.billingApi.rejectCreditNote(cn.id));
  }

  openRefund(cn: CreditNote): void {
    this.refundAmount = cn.amount - (cn.refundedAmount ?? 0);
    this.refundMethod = 'CASH';
    this.refundTarget.set(cn.id);
  }

  refund(cn: CreditNote): void {
    this.run(this.billingApi.refundCreditNote(cn.id, this.refundAmount, this.refundMethod), () => this.refundTarget.set(null));
  }

  private run(request: ReturnType<BillingApiService['approveCreditNote']>, done?: () => void): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set(null);
    request.subscribe({
      next: () => { this.busy.set(false); done?.(); this.changed.emit(); },
      error: (err) => { this.busy.set(false); this.error.set(err?.error?.detail || this.t('billing.creditNote.actionError')); },
    });
  }
}
