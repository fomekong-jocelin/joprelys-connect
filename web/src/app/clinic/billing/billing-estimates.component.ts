import { Component, EventEmitter, OnInit, Output, inject, signal, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BillingApiService } from '../../patient/billing-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { Invoice, Estimate, CreditNote, Receivable, InvoiceSettlementSummary } from '../../patient/patient.models';
import { Visit } from '../../visit/visit.models';
import { ConfirmationDialogComponent } from '../../shared/ui/confirmation-dialog.component';
import { IconComponent } from '../../shared/ui/icon.component';

/**
 * Composant dédié aux devis, validation de factures, remises, avoirs et créances.
 * Respecte SOLID — séparé de BillingManagementPageComponent.
 */
@Component({
  selector: 'app-billing-estimates',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent, ConfirmationDialogComponent],
  templateUrl: './billing-estimates.component.html',
  styleUrl: './billing-estimates.component.css'
})
export class BillingEstimatesComponent implements OnInit {
  @Output() close = new EventEmitter<void>();
  @Input() patientId!: string;
  @Input() visits: Visit[] = [];
  @Input() settlement?: InvoiceSettlementSummary;
  @Input() set initialVisitId(value: string | null | undefined) {
    if (value && !this.estimateVisitId) this.estimateVisitId = value;
  }
  @Input() set invoice(v: Invoice | null) {
    this._selectedInvoice.set(v);
    if (v) {
      this.loadInvoiceData(v.id);
    }
  }

  private readonly billingApi = inject(BillingApiService);
  private readonly i18n = inject(I18nService);

  private _selectedInvoice = signal<Invoice | null>(null);
  selectedInvoice = this._selectedInvoice.asReadonly();

  estimates = signal<Estimate[]>([]);
  creditNotes = signal<CreditNote[]>([]);
  receivables = signal<Receivable[]>([]);

  showEstimateForm = signal(false);
  showDiscountForm = signal(false);
  showCreditNoteForm = signal(false);
  showCancelInvoiceDialog = signal(false);
  savingEstimate = signal(false);
  cancellingInvoice = signal(false);
  estimateVisitId = '';

  newEstimateItems = signal<Array<{ label: string; itemType: string; unitPrice: number; quantity: number }>>([
    { label: '', itemType: 'CONSULTATION', unitPrice: 0, quantity: 1 }
  ]);

  discountAmount = 0;
  discountReason = '';
  creditNoteAmount = 0;
  creditNoteReason = '';

  successMessage = signal<string | null>(null);
  errorMessage = signal<string | null>(null);

  t(key: string, defaultValue: string): string {
    const translated = this.i18n.t(key);
    return translated === key ? defaultValue : translated;
  }

  ngOnInit(): void {
    if (this.patientId) {
      this.loadEstimates();
    }
  }

  loadEstimates(): void {
    this.billingApi.listEstimates(this.patientId).subscribe({
      next: (res) => this.estimates.set(res),
      error: () => this.showError(this.t('billing.estimates.error.load', 'Impossible de charger les devis.'))
    });
  }

  loadInvoiceData(invoiceId: string): void {
    this.billingApi.listCreditNotes(invoiceId).subscribe({
      next: (res) => this.creditNotes.set(res),
      error: () => {}
    });
    this.billingApi.getReceivablesByInvoice(invoiceId).subscribe({
      next: (res) => this.receivables.set(res),
      error: () => {}
    });
  }

  addEstimateItem(): void {
    this.newEstimateItems.update(items => [...items, { label: '', itemType: 'CONSULTATION', unitPrice: 0, quantity: 1 }]);
  }

  removeEstimateItem(index: number): void {
    this.newEstimateItems.update(items => items.filter((_, i) => i !== index));
  }

  submitEstimate(): void {
    const items = this.newEstimateItems().filter(i => i.label.trim());
    if (!items.length) return;
    this.savingEstimate.set(true);
    this.billingApi.createEstimate({
      patientId: this.patientId,
      visitId: this.estimateVisitId || undefined,
      items,
    }).subscribe({
      next: (est) => {
        this.estimates.update(list => [est, ...list]);
        this.showEstimateForm.set(false);
        this.newEstimateItems.set([{ label: '', itemType: 'CONSULTATION', unitPrice: 0, quantity: 1 }]);
        this.savingEstimate.set(false);
        this.showSuccess(this.t('billing.estimates.success.created', 'Devis créé avec succès.'));
      },
      error: () => {
        this.savingEstimate.set(false);
        this.showError(this.t('billing.estimates.error.create', 'Erreur lors de la création du devis.'));
      }
    });
  }

  acceptEstimate(est: Estimate): void {
    this.billingApi.updateEstimateStatus(est.id, 'ACCEPTED').subscribe({
      next: (updated) => {
        this.estimates.update(list => list.map(e => e.id === updated.id ? updated : e));
        this.showSuccess(this.t('billing.estimates.success.accepted', 'Devis accepté.'));
      },
      error: () => this.showError(this.t('billing.estimates.error.update', 'Erreur lors de la mise à jour du devis.'))
    });
  }

  rejectEstimate(est: Estimate): void {
    this.billingApi.updateEstimateStatus(est.id, 'REJECTED').subscribe({
      next: (updated) => {
        this.estimates.update(list => list.map(e => e.id === updated.id ? updated : e));
        this.showSuccess(this.t('billing.estimates.success.rejected', 'Devis refusé.'));
      },
      error: () => this.showError(this.t('billing.estimates.error.reject', 'Erreur lors du refus du devis.'))
    });
  }

  validateCurrentInvoice(): void {
    const inv = this._selectedInvoice();
    if (!inv) return;
    this.billingApi.validateInvoice(inv.id).subscribe({
      next: (updated) => {
        this._selectedInvoice.set(updated);
        this.loadInvoiceData(updated.id);
        this.showSuccess(this.t('billing.invoice.success.validated', 'Facture validée avec succès.'));
      },
      error: (err) => this.showError(err?.error?.message || this.t('billing.invoice.error.validate', 'Erreur lors de la validation.'))
    });
  }

  requestCancelCurrentInvoice(): void {
    if (this._selectedInvoice()) this.showCancelInvoiceDialog.set(true);
  }

  closeCancelInvoiceDialog(): void {
    if (!this.cancellingInvoice()) this.showCancelInvoiceDialog.set(false);
  }

  confirmCancelCurrentInvoice(): void {
    const inv = this._selectedInvoice();
    if (!inv || this.cancellingInvoice()) return;
    this.cancellingInvoice.set(true);
    this.billingApi.cancelInvoice(inv.id).subscribe({
      next: (updated) => {
        this._selectedInvoice.set(updated);
        this.cancellingInvoice.set(false);
        this.showCancelInvoiceDialog.set(false);
        this.showSuccess(this.t('billing.invoice.success.cancelled', 'Facture annulée.'));
      },
      error: (err) => {
        this.cancellingInvoice.set(false);
        this.showError(err?.error?.message || this.t('billing.invoice.error.cancel', 'Erreur lors de l\'annulation.'));
      },
    });
  }

  cancelInvoiceMessage(): string {
    const invoiceNumber = this._selectedInvoice()?.invoiceNumber ?? this.t('common.notSpecified', 'Non spécifié');
    return this.t(
      'billing.invoice.cancelDialogMessage',
      'Vous êtes sur le point d’annuler la facture {invoiceNumber}. Cette action est tracée.',
    ).replace('{invoiceNumber}', invoiceNumber);
  }

  submitDiscount(): void {
    const inv = this._selectedInvoice();
    if (!inv || !this.discountReason.trim() || this.discountAmount <= 0) return;
    this.billingApi.applyDiscount(inv.id, this.discountAmount, this.discountReason).subscribe({
      next: (updated) => {
        this._selectedInvoice.set(updated);
        this.showDiscountForm.set(false);
        this.discountAmount = 0;
        this.discountReason = '';
        this.showSuccess(this.t('billing.invoice.success.discount', 'Remise appliquée.'));
      },
      error: (err) => this.showError(err?.error?.message || this.t('billing.invoice.error.discount', 'Erreur lors de l\'application de la remise.'))
    });
  }

  submitCreditNote(): void {
    const inv = this._selectedInvoice();
    if (!inv || !this.creditNoteReason.trim() || this.creditNoteAmount <= 0) return;
    this.billingApi.createCreditNote(inv.id, this.creditNoteAmount, this.creditNoteReason).subscribe({
      next: (cn) => {
        this.creditNotes.update(list => [cn, ...list]);
        this.showCreditNoteForm.set(false);
        this.creditNoteAmount = 0;
        this.creditNoteReason = '';
        this.showSuccess(this.t('billing.creditNote.success.created', 'Avoir créé avec succès.'));
      },
      error: (err) => this.showError(err?.error?.message || this.t('billing.creditNote.error.create', 'Erreur lors de la création de l\'avoir.'))
    });
  }

  estimateStatusClass(status: string): Record<string, boolean> {
    return {
      'badge-success': status === 'ACCEPTED',
      'badge-warning': status === 'DRAFT',
      'badge-danger': status === 'REJECTED',
      'badge-info': status === 'INVOICED',
    };
  }

  invoiceStatusClass(status: string): Record<string, boolean> {
    return {
      'badge-success': status === 'PAID' || status === 'VALIDATED',
      'badge-warning': status === 'PENDING' || status === 'PARTIALLY_PAID' || status === 'PROFORMA',
      'badge-danger': status === 'CANCELLED',
    };
  }

  collectionStatusClass(): Record<string, boolean> {
    const status = this.settlement?.collectionStatus;
    return {
      'badge-success': status === 'SETTLED',
      'badge-warning': status === 'PATIENT_DUE' || status === 'PATIENT_PARTIALLY_PAID',
      'badge-info': status === 'INSURANCE_DUE',
      'badge-danger': !status && this._selectedInvoice()?.status === 'CANCELLED',
    };
  }

  collectionStatusLabel(): string {
    switch (this.settlement?.collectionStatus) {
      case 'SETTLED': return this.t('billing.collection.settled', 'Soldée');
      case 'INSURANCE_DUE': return this.t('billing.collection.insuranceDue', 'Assurance à recouvrer');
      case 'PATIENT_PARTIALLY_PAID': return this.t('billing.collection.patientPartiallyPaid', 'Part patient partielle');
      case 'PATIENT_DUE': return this.t('billing.collection.patientDue', 'Part patient à régler');
      default: return this._selectedInvoice()?.status ?? '';
    }
  }

  receivableStatusClass(status: string): Record<string, boolean> {
    return {
      'badge-success': status === 'PAID',
      'badge-warning': status === 'PARTIALLY_PAID',
      'badge-danger': status === 'UNPAID',
    };
  }

  private showSuccess(msg: string): void {
    this.successMessage.set(msg);
    this.errorMessage.set(null);
    setTimeout(() => this.successMessage.set(null), 3500);
  }

  private showError(msg: string): void {
    this.errorMessage.set(msg);
    this.successMessage.set(null);
    setTimeout(() => this.errorMessage.set(null), 5000);
  }
}
