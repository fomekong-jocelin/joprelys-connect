import { CommonModule } from '@angular/common';
import { Component, ElementRef, EventEmitter, Input, Output } from '@angular/core';
import { Invoice, InvoiceSettlementSummary } from '../../patient/patient.models';
import { IconComponent } from '../../shared/ui/icon.component';

type CollectionStatus = InvoiceSettlementSummary['collectionStatus'];

@Component({
  selector: 'app-billing-invoice-history',
  standalone: true,
  imports: [CommonModule, IconComponent],
  templateUrl: './billing-invoice-history.component.html',
  styleUrl: './billing-invoice-history.component.css',
})
export class BillingInvoiceHistoryComponent {
  readonly historyTitleId = 'billing-invoice-history-title';

  @Input({ required: true }) invoices: Invoice[] = [];
  @Input() settlements: Record<string, InvoiceSettlementSummary> = {};
  @Input() loading = false;
  @Input() error = false;
  @Input() selectedInvoiceId: string | null = null;
  @Input() canCollectPayments = false;
  @Input() canFollowInsurance = false;
  @Input({ required: true }) translate!: (key: string, defaultValue: string) => string;

  @Output() printPdf = new EventEmitter<string>();
  @Output() openPayment = new EventEmitter<Invoice>();
  @Output() openInsurance = new EventEmitter<Invoice>();
  @Output() selectInvoice = new EventEmitter<Invoice>();
  @Output() retry = new EventEmitter<void>();

  constructor(private readonly host: ElementRef<HTMLElement>) {}

  focusInvoice(invoiceId: string): void {
    const cards = Array.from(
      this.host.nativeElement.querySelectorAll<HTMLElement>('[data-invoice-id]')
    );
    cards.find((card) => card.dataset['invoiceId'] === invoiceId)?.focus();
  }

  isSelected(invoice: Invoice): boolean {
    return this.selectedInvoiceId === invoice.id;
  }

  canCollectPatient(invoice: Invoice): boolean {
    const summary = this.settlements[invoice.id];
    if (!summary) {
      return invoice.status === 'VALIDATED' || invoice.status === 'PARTIALLY_PAID';
    }
    return summary.collectionStatus === 'PATIENT_DUE'
      || summary.collectionStatus === 'PATIENT_PARTIALLY_PAID';
  }

  isInsuranceDue(invoice: Invoice): boolean {
    return this.getCollectionStatus(invoice) === 'INSURANCE_DUE';
  }

  hasInsuranceShare(invoice: Invoice): boolean {
    return invoice.insuranceShare > 0 || !!this.settlements[invoice.id]?.insurance;
  }

  getPatientRemainingAmount(invoice: Invoice): number {
    return this.settlements[invoice.id]?.patient.remainingAmount ?? invoice.patientShare;
  }

  getInsuranceRemainingAmount(invoice: Invoice): number {
    return this.settlements[invoice.id]?.insurance?.remainingAmount ?? invoice.insuranceShare;
  }

  getPatientAmountLabel(invoice: Invoice): string {
    return this.settlements[invoice.id]
      ? this.translate('billing.patientRemaining', 'Reste patient')
      : this.translate('billing.invoicePatient', 'Part patient');
  }

  getInsuranceAmountLabel(invoice: Invoice): string {
    return this.settlements[invoice.id]
      ? this.translate('billing.insuranceRemaining', 'Reste assurance')
      : this.translate('billing.invoiceInsurance', 'Part assurance');
  }

  getCollectionStatusClass(invoice: Invoice): string {
    switch (this.getCollectionStatus(invoice)) {
      case 'SETTLED':
        return 'bg-emerald-500/15 text-emerald-700 dark:text-emerald-300';
      case 'PATIENT_DUE':
        return 'bg-orange-500/15 text-orange-700 dark:text-orange-300';
      case 'PATIENT_PARTIALLY_PAID':
        return 'bg-amber-500/15 text-amber-700 dark:text-amber-300';
      case 'INSURANCE_DUE':
        return 'bg-blue-500/15 text-blue-700 dark:text-blue-300';
      case 'CANCELLED':
        return 'bg-red-500/15 text-red-700 dark:text-red-300';
      default:
        return 'bg-gray-500/15 text-gray-700 dark:text-gray-300';
    }
  }

  getCollectionStatusLabel(invoice: Invoice): string {
    switch (this.getCollectionStatus(invoice)) {
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
      case 'CANCELLED':
        return this.translate('billing.collection.cancelled', 'Annulée');
    }
  }

  private getCollectionStatus(invoice: Invoice): CollectionStatus {
    const summary = this.settlements[invoice.id];
    if (summary) return summary.collectionStatus;

    switch (invoice.status) {
      case 'SETTLED':
        return 'SETTLED';
      case 'CANCELLED':
        return 'CANCELLED';
      case 'PARTIALLY_PAID':
        return 'PATIENT_PARTIALLY_PAID';
      case 'VALIDATED':
        return 'PATIENT_DUE';
      case 'PAID':
        return invoice.insuranceShare > 0 ? 'INSURANCE_DUE' : 'SETTLED';
      default:
        return 'NOT_YET_DUE';
    }
  }
}
