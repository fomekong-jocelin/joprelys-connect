import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Invoice } from '../../patient/patient.models';
import { IconComponent } from '../../shared/ui/icon.component';

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
      <div class="fixed inset-0 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
        <div class="ui-card max-w-md w-full p-6 space-y-4 animate-in fade-in zoom-in-95 duration-150">

          <!-- En-tête -->
          <div class="flex justify-between items-center border-b border-[var(--app-border)]/40 pb-2">
            <h3 class="font-bold text-xs text-[var(--text-primary)] uppercase tracking-wider">
              <app-ui-icon name="banknotes" />
              {{ translate('billing.addPayment', 'Enregistrer un Règlement') }}
            </h3>
            <button (click)="close.emit()" [attr.aria-label]="translate('common.aria.close', 'Fermer')" class="text-xs text-[var(--text-muted)] font-bold p-1 rounded-[var(--radius-brand-sm)] hover:bg-[var(--app-surface-muted)]">
              <app-ui-icon name="x-mark" class="text-base" />
            </button>
          </div>

          <!-- Résumé facture -->
          <div class="text-xs space-y-1 bg-[var(--app-surface-muted)] p-3 rounded-[var(--radius-brand-sm)]">
            <div>{{ translate('billing.invoice', 'Facture') }} : <strong>{{ invoice.invoiceNumber }}</strong></div>
            <div>{{ translate('billing.patientShare', 'Part Patient à régler') }} : <strong>{{ invoice.patientShare | number:'1.0-0' }} FCFA</strong></div>
          </div>

          <!-- État chargement session caisse -->
          @if (cashSessionOpen === null) {
            <div class="ui-alert-warning text-xs flex items-center gap-2" role="status">
              <app-ui-icon name="information-circle" />
              {{ translate('billing.cashSessionChecking', 'Vérification de la session de caisse...') }}
            </div>
          }

          <!-- Session caisse fermée : bloquer le formulaire et proposer l'action -->
          @if (cashSessionOpen === false) {
            <div class="ui-alert-warning text-xs flex items-start gap-2" role="alert">
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

          <!-- Formulaire : masqué si session fermée ou en cours de vérification -->
          @if (cashSessionOpen === true) {
            <div class="space-y-3">
              <div>
                <label class="text-[10px] font-bold text-[var(--text-secondary)] block mb-1">{{ translate('billing.paymentAmount', 'Montant versé (FCFA)') }} :</label>
                <input type="number" [(ngModel)]="amount" [min]="1" [max]="invoice.patientShare" class="ui-input text-xs w-full" />
              </div>

              <div>
                <label class="text-[10px] font-bold text-[var(--text-secondary)] block mb-1">{{ translate('billing.paymentMethod', 'Mode de règlement') }} :</label>
                <select [(ngModel)]="method" class="ui-select text-xs w-full">
                  <option value="CASH">{{ translate('billing.paymentMethod.cash', 'Espèces (CASH)') }}</option>
                  <option value="CHECK">{{ translate('billing.paymentMethod.check', 'Chèque') }}</option>
                  <option value="BANK_TRANSFER">{{ translate('billing.paymentMethod.bankTransfer', 'Virement bancaire') }}</option>
                </select>
              </div>

              <div>
                <label class="text-[10px] font-bold text-[var(--text-secondary)] block mb-1">{{ translate('billing.paymentReference', 'Référence / Reçu') }} :</label>
                <input
                  type="text"
                  [(ngModel)]="reference"
                  [placeholder]="translate('billing.paymentReferencePlaceholder', 'N° chèque, code de transaction...')"
                  class="ui-input text-xs w-full"
                />
              </div>

              <div class="flex justify-end gap-2 pt-2">
                <button (click)="close.emit()" class="ui-button ui-button-secondary">
                  {{ translate('billing.cancel', 'Annuler') }}
                </button>
                <button (click)="submit()" [disabled]="saving || !amount || (amount ?? 0) <= 0" class="ui-button ui-button-primary disabled:opacity-50">
                  @if (saving) {
                    {{ translate('common.saving', 'Enregistrement...') }}
                  } @else {
                    <app-ui-icon name="banknotes" />
                    {{ translate('billing.validatePayment', 'Valider le Règlement') }}
                  }
                </button>
              </div>
            </div>
          }

          <!-- Bouton fermer quand session fermée ou en chargement -->
          @if (cashSessionOpen !== true) {
            <div class="flex justify-end">
              <button (click)="close.emit()" class="ui-button ui-button-secondary">
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
  @Input({ required: true }) visible = false;
  @Input() invoice: Invoice | null = null;
  @Input({ required: true }) saving = false;
  @Input() cashSessionOpen: boolean | null = null;
  @Input({ required: true }) translate!: (key: string, defaultValue: string) => string;

  @Output() close = new EventEmitter<void>();
  @Output() submitPayment = new EventEmitter<BillingPaymentForm>();
  @Output() openCashRegister = new EventEmitter<void>();

  amount: number | null = null;
  method: 'CASH' | 'CHECK' | 'BANK_TRANSFER' = 'CASH';
  reference = '';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['invoice'] && this.invoice) {
      this.amount = this.invoice.patientShare;
      this.method = 'CASH';
      this.reference = '';
    }
  }

  submit(): void {
    if (!this.amount || this.amount <= 0) return;
    this.submitPayment.emit({
      amount: this.amount,
      method: this.method,
      reference: this.reference,
    });
  }
}
