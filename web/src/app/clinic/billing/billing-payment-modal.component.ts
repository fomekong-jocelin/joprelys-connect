import { CommonModule } from '@angular/common';
import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IconComponent } from '../../shared/ui/icon.component';

export interface BillingPaymentInvoiceContext {
  id: string;
  invoiceNumber: string;
  patientShare: number;
}

export interface BillingPaymentForm {
  amount: number;
  method: 'CASH' | 'CHECK' | 'BANK_TRANSFER';
  reference: string;
}

@Component({
  selector: 'app-billing-payment-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent],
  template: `
    @if (visible && invoice) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs">
        <div
          #dialog
          class="ui-card w-full max-w-md space-y-4 p-6"
          role="dialog"
          aria-modal="true"
          aria-labelledby="billing-payment-dialog-title"
          [attr.aria-describedby]="errorMessage ? 'billing-payment-dialog-error' : null"
        >
          <div class="flex items-center justify-between gap-4 border-b border-[var(--app-border)]/40 pb-2">
            <h3 id="billing-payment-dialog-title" class="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-[var(--text-primary)]">
              <app-ui-icon name="banknotes" />
              {{ translate('billing.addPayment', 'Enregistrer un règlement') }}
            </h3>
            <button
              type="button"
              (click)="close.emit()"
              [attr.aria-label]="translate('common.aria.close', 'Fermer')"
              class="rounded-[var(--radius-brand-sm)] p-1 text-xs font-bold text-[var(--text-muted)] hover:bg-[var(--app-surface-muted)]"
            >
              <app-ui-icon name="x-mark" class="text-base" />
            </button>
          </div>

          <div class="space-y-1 rounded-[var(--radius-brand-sm)] bg-[var(--app-surface-muted)] p-3 text-xs">
            @if (patientName) {
              <div>{{ translate('billing.cashierQueue.patient', 'Patient') }} : <strong>{{ patientName }}</strong></div>
            }
            <div>{{ translate('billing.invoice', 'Facture') }} : <strong>{{ invoice.invoiceNumber }}</strong></div>
            <div>{{ translate('billing.patientShare', 'Part patient à régler') }} : <strong>{{ invoice.patientShare | number:'1.0-0' }} FCFA</strong></div>
          </div>

          @if (cashSessionOpen === null) {
            <div class="ui-alert-warning flex items-center gap-2 text-xs" role="status">
              <app-ui-icon name="information-circle" />
              {{ translate('billing.cashSessionChecking', 'Vérification de la session de caisse...') }}
            </div>
          }

          @if (cashSessionOpen === false) {
            <div class="ui-alert-warning flex items-start gap-2 text-xs" role="alert">
              <app-ui-icon name="information-circle" />
              <div class="space-y-2">
                <p class="font-semibold">{{ translate('billing.cashSessionRequired', 'Aucune session de caisse ouverte.') }}</p>
                <p class="text-[var(--text-muted)]">{{ translate('billing.cashSessionRequiredHint', 'Vous devez ouvrir une session de caisse avant d\'encaisser ce règlement.') }}</p>
                <button type="button" (click)="openCashRegister.emit()" class="ui-button ui-button-secondary">
                  <app-ui-icon name="calculator" />
                  {{ translate('billing.openCashRegister', 'Ouvrir la caisse →') }}
                </button>
              </div>
            </div>
          }

          @if (errorMessage) {
            <div id="billing-payment-dialog-error" class="ui-alert-danger text-xs" role="alert">
              <app-ui-icon name="exclamation-triangle" />
              {{ errorMessage }}
            </div>
          }

          @if (cashSessionOpen === true) {
            <div class="space-y-3">
              <div>
                <label for="billing-payment-amount" class="mb-1 block text-[10px] font-bold text-[var(--text-secondary)]">
                  {{ translate('billing.paymentAmount', 'Montant versé (FCFA)') }} :
                </label>
                <input
                  #amountInput
                  id="billing-payment-amount"
                  type="number"
                  [(ngModel)]="amount"
                  [min]="1"
                  [max]="invoice.patientShare"
                  class="ui-input w-full text-xs"
                  inputmode="decimal"
                />
                @if ((amount ?? 0) > invoice.patientShare) {
                  <p class="mt-1 text-[10px] text-[var(--brand-danger-text)]" role="alert">
                    {{ translate('billing.cashierQueue.amountTooHigh', 'Le montant dépasse le reste patient.') }}
                  </p>
                }
              </div>

              <div>
                <label for="billing-payment-method" class="mb-1 block text-[10px] font-bold text-[var(--text-secondary)]">
                  {{ translate('billing.paymentMethod', 'Mode de règlement') }} :
                </label>
                <select id="billing-payment-method" [(ngModel)]="method" class="ui-select w-full text-xs">
                  <option value="CASH">{{ translate('billing.paymentMethod.cash', 'Espèces') }}</option>
                  <option value="CHECK">{{ translate('billing.paymentMethod.check', 'Chèque') }}</option>
                  <option value="BANK_TRANSFER">{{ translate('billing.paymentMethod.bankTransfer', 'Virement bancaire') }}</option>
                </select>
              </div>

              <div>
                <label for="billing-payment-reference" class="mb-1 block text-[10px] font-bold text-[var(--text-secondary)]">
                  {{ translate('billing.paymentReference', 'Référence / Reçu') }}
                  @if (requiresReference()) { <span aria-hidden="true">*</span> }
                </label>
                <input
                  id="billing-payment-reference"
                  type="text"
                  [(ngModel)]="reference"
                  [required]="requiresReference()"
                  [attr.aria-required]="requiresReference()"
                  [placeholder]="translate('billing.paymentReferencePlaceholder', 'N° chèque, code de transaction...')"
                  class="ui-input w-full text-xs"
                />
                @if (requiresReference() && !reference.trim()) {
                  <p class="mt-1 text-[10px] text-[var(--text-muted)]">
                    {{ translate('billing.cashierQueue.referenceHint', 'Obligatoire pour un chèque ou un virement.') }}
                  </p>
                }
              </div>

              <div class="flex flex-col-reverse gap-2 pt-2 sm:flex-row sm:justify-end">
                <button type="button" (click)="close.emit()" class="ui-button ui-button-secondary justify-center">
                  {{ translate('billing.cancel', 'Annuler') }}
                </button>
                <button
                  type="button"
                  (click)="submit()"
                  [disabled]="!canSubmit()"
                  class="ui-button ui-button-primary justify-center disabled:opacity-50"
                >
                  @if (saving) {
                    {{ translate('common.saving', 'Enregistrement...') }}
                  } @else {
                    <app-ui-icon name="banknotes" />
                    {{ translate('billing.validatePayment', 'Valider le règlement') }}
                  }
                </button>
              </div>
            </div>
          }

          @if (cashSessionOpen !== true) {
            <div class="flex justify-end">
              <button type="button" (click)="close.emit()" class="ui-button ui-button-secondary">
                {{ translate('billing.cancel', 'Fermer') }}
              </button>
            </div>
          }
        </div>
      </div>
    }
  `,
})
export class BillingPaymentModalComponent implements OnChanges {
  @ViewChild('amountInput')
  private amountInput?: ElementRef<HTMLInputElement>;

  @Input({ required: true }) visible = false;
  @Input() invoice: BillingPaymentInvoiceContext | null = null;
  @Input() patientName: string | null = null;
  @Input({ required: true }) saving = false;
  @Input() cashSessionOpen: boolean | null = null;
  @Input() errorMessage: string | null = null;
  @Input({ required: true }) translate!: (key: string, defaultValue: string) => string;

  @Output() close = new EventEmitter<void>();
  @Output() submitPayment = new EventEmitter<BillingPaymentForm>();
  @Output() openCashRegister = new EventEmitter<void>();

  amount: number | null = null;
  method: BillingPaymentForm['method'] = 'CASH';
  reference = '';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['invoice'] && this.invoice) {
      this.amount = this.invoice.patientShare;
      this.method = 'CASH';
      this.reference = '';
    }

    if (changes['visible'] && this.visible) {
      setTimeout(() => this.amountInput?.nativeElement.focus());
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.visible && !this.saving) {
      this.close.emit();
    }
  }

  requiresReference(): boolean {
    return this.method === 'CHECK' || this.method === 'BANK_TRANSFER';
  }

  canSubmit(): boolean {
    if (!this.invoice || this.saving || this.cashSessionOpen !== true) return false;
    if (!this.amount || this.amount <= 0 || this.amount > this.invoice.patientShare) return false;
    return !this.requiresReference() || !!this.reference.trim();
  }

  submit(): void {
    if (!this.canSubmit() || this.amount === null) return;
    this.submitPayment.emit({
      amount: this.amount,
      method: this.method,
      reference: this.reference.trim(),
    });
  }
}
