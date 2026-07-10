import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Invoice, InvoiceSettlementSummary } from '../../patient/patient.models';
import { IconComponent } from '../../shared/ui/icon.component';

@Component({
  selector: 'app-billing-invoice-history',
  standalone: true,
  imports: [CommonModule, IconComponent],
  template: `
    <div class="ui-card-subtle p-4 space-y-4">
      <h3 class="font-bold text-xs text-[var(--text-primary)] uppercase tracking-wider border-b border-[var(--app-border)]/40 pb-2">
        <app-ui-icon name="folder-open" />
        {{ translate('billing.invoices', 'Historique des Factures') }}
      </h3>

      @if (invoices.length > 0) {
        <div class="space-y-3">
          @for (inv of invoices; track inv.id) {
            <div class="border border-[var(--app-border)]/60 rounded-[var(--radius-brand-sm)] p-3 flex flex-col md:flex-row justify-between items-start md:items-center gap-3">
              <div class="space-y-1">
                <div class="flex items-center gap-2">
                  <span class="font-bold text-xs text-[var(--text-primary)]">{{ inv.invoiceNumber }}</span>
                  <span [class]="'px-1.5 py-0.5 rounded-[var(--radius-brand-sm)] text-[8px] font-bold ' + getCollectionStatusClass(inv)">
                    {{ getCollectionStatusLabel(inv) }}
                  </span>
                </div>
                <div class="text-[10px] text-[var(--text-muted)]">
                  {{ translate('billing.issuedAt', 'Émise le') }} : {{ inv.createdAt | date:'dd/MM/yyyy HH:mm' }}
                </div>
                <div class="text-[10px] text-[var(--text-secondary)]">
                  {{ translate('billing.invoiceTotal', 'Total') }} : <strong>{{ inv.totalAmount | number:'1.0-0' }} FCFA</strong> |
                  {{ translate('billing.invoicePatient', 'Patient') }} : <strong>{{ inv.patientShare | number:'1.0-0' }} FCFA</strong>
                  @if (inv.insuranceConvention) {
                    | {{ translate('billing.invoiceInsurance', 'Assur') }} : {{ inv.insuranceConvention.name }} ({{ inv.insuranceShare | number:'1.0-0' }} FCFA)
                  }
                </div>
              </div>

              <div class="flex gap-2">
                <button (click)="selectInvoice.emit(inv)" class="ui-button ui-button-secondary">
                  <app-ui-icon name="wrench" />
                  {{ translate('billing.manage', 'Gérer') }}
                </button>
                <button (click)="printPdf.emit(inv.id)" class="ui-button ui-button-secondary">
                  <app-ui-icon name="printer" />
                  PDF
                </button>
                @if (inv.status !== 'PAID') {
                  <button (click)="openPayment.emit(inv)" class="ui-button ui-button-primary">
                    <app-ui-icon name="banknotes" />
                    {{ translate('billing.pay', 'Régler') }}
                  </button>
                }
              </div>
            </div>
          }
        </div>
      } @else {
        <div class="text-center text-[10px] text-[var(--text-muted)] py-6">
          {{ translate('billing.noInvoices', 'Aucune facture émise pour ce patient.') }}
        </div>
      }
    </div>
  `,
})
export class BillingInvoiceHistoryComponent {
  @Input({ required: true }) invoices: Invoice[] = [];
  @Input() settlements: Record<string, InvoiceSettlementSummary> = {};
  @Input({ required: true }) translate!: (key: string, defaultValue: string) => string;

  @Output() printPdf = new EventEmitter<string>();
  @Output() openPayment = new EventEmitter<Invoice>();
  @Output() selectInvoice = new EventEmitter<Invoice>();

  getCollectionStatusClass(invoice: Invoice): string {
    switch (this.settlements[invoice.id]?.collectionStatus) {
      case 'SETTLED':
        return 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400';
      case 'PATIENT_PARTIALLY_PAID':
        return 'bg-amber-500/15 text-amber-600 dark:text-amber-400';
      case 'INSURANCE_DUE':
        return 'bg-blue-500/15 text-blue-600 dark:text-blue-400';
      default:
        return 'bg-gray-500/15 text-gray-600 dark:text-gray-400';
    }
  }

  getCollectionStatusLabel(invoice: Invoice): string {
    switch (this.settlements[invoice.id]?.collectionStatus) {
      case 'SETTLED':
        return this.translate('billing.collection.settled', 'Soldée');
      case 'INSURANCE_DUE':
        return this.translate('billing.collection.insuranceDue', 'Assurance à recouvrer');
      case 'PATIENT_PARTIALLY_PAID':
        return this.translate('billing.collection.patientPartiallyPaid', 'Part patient partielle');
      case 'PATIENT_DUE':
        return this.translate('billing.collection.patientDue', 'Part patient à régler');
      case 'NOT_YET_DUE':
        return this.translate('billing.collection.notYetDue', 'À valider');
      default:
        return this.translate('billing.invoiceStatus.pending', 'En attente');
    }
  }
}
