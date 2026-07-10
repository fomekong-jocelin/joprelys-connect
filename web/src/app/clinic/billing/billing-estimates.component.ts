import { Component, EventEmitter, OnInit, Output, inject, signal, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BillingApiService } from '../../patient/billing-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { Invoice, Estimate, CreditNote, Receivable, InvoiceSettlementSummary } from '../../patient/patient.models';
import { IconComponent } from '../../shared/ui/icon.component';

/**
 * Composant dédié aux devis, validation de factures, remises, avoirs et créances.
 * Respecte SOLID — séparé de BillingManagementPageComponent.
 */
@Component({
  selector: 'app-billing-estimates',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent],
  template: `
    <div class="estimates-panel">

      <!-- ── Devis / Proformas ─────────────────────────────────────────── -->
      <section class="panel-section secondary-section">
        <div class="section-header">
          <div>
            <h3 class="section-title">
            <app-ui-icon name="document-text" class="section-icon" />
            {{ t('billing.estimates.title', 'Devis / Proformas') }}
            </h3>
            <p class="section-caption">{{ t('billing.estimates.readOnlyHint', 'Historique des devis enregistrés pour ce patient') }}</p>
          </div>
          <button
            type="button"
            class="ui-button ui-button-primary action-button"
            (click)="showEstimateForm.set(!showEstimateForm())"
            [attr.aria-expanded]="showEstimateForm()"
            aria-controls="estimate-form"
          >
            <app-ui-icon name="plus" />
            {{ showEstimateForm() ? t('billing.estimates.closeForm', 'Fermer la création') : t('billing.estimates.new', 'Créer un devis') }}
          </button>
        </div>

        @if (showEstimateForm()) {
          <div id="estimate-form" class="form-card action-form" role="form" [attr.aria-label]="t('billing.estimates.formLabel', 'Création d’un devis')">
            <div class="form-intro">
              <span class="form-kicker">{{ t('billing.estimates.actionKicker', 'Action') }}</span>
              <h4>{{ t('billing.estimates.formTitle', 'Créer un devis / proforma') }}</h4>
              <p>{{ t('billing.estimates.formHint', 'Ajoutez les prestations puis enregistrez le document pour le patient.') }}</p>
            </div>
            <div class="form-grid">
              @for (item of newEstimateItems(); track $index) {
                <div class="estimate-item-row">
                  <label class="field-label">
                    {{ t('billing.estimates.itemLabel', 'Prestation') }}
                    <input class="ui-input" [(ngModel)]="item.label" [placeholder]="t('billing.estimates.itemPlaceholder', 'Ex. Consultation')" />
                  </label>
                  <label class="field-label">
                    {{ t('billing.estimates.itemType', 'Type') }}
                    <select class="ui-select" [(ngModel)]="item.itemType">
                    <option value="CONSULTATION">Consultation</option>
                    <option value="K_SURGEON">K Chirurgien</option>
                    <option value="K_ANESTHESIST">K Anesthésiste</option>
                    <option value="MEDICATION">Médicament</option>
                    <option value="STAY_FEE">Frais de séjour</option>
                    </select>
                  </label>
                  <label class="field-label">
                    {{ t('billing.estimates.unitPrice', 'Prix unitaire') }}
                    <input class="ui-input input-sm" type="number" [(ngModel)]="item.unitPrice" min="0" />
                  </label>
                  <label class="field-label">
                    {{ t('billing.estimates.quantity', 'Qté') }}
                    <input class="ui-input input-sm" type="number" [(ngModel)]="item.quantity" min="1" />
                  </label>
                  <button type="button" class="ui-button ui-button-secondary remove-line" (click)="removeEstimateItem($index)" [attr.aria-label]="t('billing.estimates.removeLine', 'Supprimer cette ligne')" [title]="t('billing.estimates.removeLine', 'Supprimer cette ligne')">
                    <app-ui-icon name="trash" />
                    {{ t('billing.estimates.remove', 'Supprimer') }}
                  </button>
                </div>
              }
            </div>
            <div class="form-actions">
              <button type="button" class="ui-button ui-button-secondary" (click)="addEstimateItem()">
                <app-ui-icon name="plus" /> Ajouter une ligne
              </button>
              <div class="spacer"></div>
              <button type="button" class="ui-button ui-button-secondary" (click)="showEstimateForm.set(false)">{{ t('common.cancel', 'Annuler') }}</button>
              <button type="button" class="ui-button ui-button-primary action-button" (click)="submitEstimate()" [disabled]="savingEstimate()">
                @if (savingEstimate()) { <span class="spinner"></span> } @else { <app-ui-icon name="check" /> }
                {{ t('billing.estimates.save', 'Enregistrer le devis') }}
              </button>
            </div>
          </div>
        }

        <!-- Liste des devis -->
        @if (estimates().length > 0) {
          <div class="table-wrapper">
            <table class="data-table">
              <thead>
                <tr>
                  <th>N° Devis</th>
                  <th>Montant total</th>
                  <th>Part patient</th>
                  <th>Statut</th>
                  <th>Date</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (est of estimates(); track est.id) {
                  <tr>
                    <td><span class="mono">{{ est.estimateNumber }}</span></td>
                    <td class="amount">{{ est.totalAmount | number:'1.0-0' }} FCFA</td>
                    <td class="amount">{{ est.patientShare | number:'1.0-0' }} FCFA</td>
                    <td><span class="badge" [ngClass]="estimateStatusClass(est.status)">{{ est.status }}</span></td>
                    <td>{{ est.createdAt | date:'dd/MM/yyyy' }}</td>
                    <td class="actions-cell">
                      @if (est.status === 'DRAFT') {
                        <button type="button" class="ui-button ui-button-secondary table-action" (click)="acceptEstimate(est)" title="Accepter">
                          <app-ui-icon name="check" />
                        </button>
                        <button type="button" class="ui-button ui-button-danger table-action" (click)="rejectEstimate(est)" title="Refuser">
                          <app-ui-icon name="x-mark" />
                        </button>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        } @else {
          <p class="empty-state">Aucun devis enregistré pour ce patient.</p>
        }
      </section>

      <!-- ── Validation de facture & Remise ────────────────────────────── -->
      @if (selectedInvoice()) {
        <section class="panel-section invoice-detail-section">
          <div class="section-header">
            <h3 class="section-title">
              <app-ui-icon name="document-text" class="section-icon" />
              {{ t('billing.selectedInvoice', 'Facture sélectionnée') }} — {{ selectedInvoice()!.invoiceNumber }}
            </h3>
            <div class="badge" [ngClass]="collectionStatusClass()">
              {{ collectionStatusLabel() }}
            </div>
          </div>

          @if (settlement?.collectionStatus === 'INSURANCE_DUE') {
            <div class="info-banner">
              <app-ui-icon name="information-circle" />
              Part patient réglée. La part assurance de
              <strong>{{ settlement?.insurance?.remainingAmount | number:'1.0-0' }} FCFA</strong>
              reste à recouvrer via un bordereau d'assurance.
            </div>
          }

          @if (selectedInvoice()!.discountAmount && selectedInvoice()!.discountAmount! > 0) {
            <div class="info-banner">
              <app-ui-icon name="receipt-percent" />
              Remise appliquée : <strong>{{ selectedInvoice()!.discountAmount | number:'1.0-0' }} FCFA</strong>
              — {{ selectedInvoice()!.discountReason }}
            </div>
          }

          <div class="action-bar" aria-label="Actions sur la facture">
            @if (selectedInvoice()!.status === 'PENDING' || selectedInvoice()!.status === 'PROFORMA') {
              <button type="button" class="ui-button ui-button-primary" (click)="validateCurrentInvoice()">
                <app-ui-icon name="check" /> Valider la facture
              </button>
              <button type="button" class="ui-button ui-button-secondary" (click)="showDiscountForm.set(!showDiscountForm())">
                <app-ui-icon name="receipt-percent" /> Appliquer une remise
              </button>
              <button type="button" class="ui-button ui-button-danger" (click)="cancelCurrentInvoice()">
                <app-ui-icon name="x-mark" /> Annuler la facture
              </button>
            }
            @if (selectedInvoice()!.status === 'VALIDATED' || selectedInvoice()!.status === 'PAID') {
              <button type="button" class="ui-button btn-credit action-button" (click)="showCreditNoteForm.set(!showCreditNoteForm())" [attr.aria-expanded]="showCreditNoteForm()" aria-controls="credit-note-form">
                <app-ui-icon name="receipt-percent" /> {{ showCreditNoteForm() ? t('billing.creditNote.close', 'Fermer l’avoir') : t('billing.creditNote.action', 'Créer un avoir') }}
              </button>
            }
          </div>

          <!-- Formulaire remise -->
          @if (showDiscountForm()) {
            <div class="inline-form" role="form" aria-label="Formulaire remise">
              <h4 class="inline-form-title">Remise commerciale</h4>
              <div class="form-row">
                <input class="ui-input" type="number" [(ngModel)]="discountAmount" placeholder="Montant remise (FCFA)" min="0" id="discount-amount" />
                <input class="ui-input" [(ngModel)]="discountReason" placeholder="Motif de la remise" id="discount-reason" />
                <button type="button" class="ui-button ui-button-primary" (click)="submitDiscount()">Appliquer</button>
                <button type="button" class="ui-button ui-button-secondary" (click)="showDiscountForm.set(false)">Annuler</button>
              </div>
            </div>
          }

          <!-- Formulaire avoir -->
          @if (showCreditNoteForm()) {
            <div id="credit-note-form" class="inline-form credit-note-form" role="form" [attr.aria-label]="t('billing.creditNote.formLabel', 'Création d’un avoir')">
              <div class="form-intro">
                <span class="form-kicker">{{ t('billing.creditNote.kicker', 'Action exceptionnelle') }}</span>
                <h4>{{ t('billing.creditNote.title', 'Créer un avoir') }}</h4>
                <p>{{ t('billing.creditNote.hint', 'Corrige tout ou partie de cette facture validée. Cette action est tracée.') }}</p>
              </div>
              <div class="form-row">
                <label class="field-label">
                  {{ t('billing.creditNote.amount', 'Montant de l’avoir (FCFA)') }}
                  <input class="ui-input" type="number" [(ngModel)]="creditNoteAmount" min="0" id="credit-note-amount" />
                </label>
                <label class="field-label field-label-wide">
                  {{ t('billing.creditNote.reason', 'Motif obligatoire') }}
                  <input class="ui-input" [(ngModel)]="creditNoteReason" [placeholder]="t('billing.creditNote.reasonPlaceholder', 'Ex. Correction d’une prestation')" id="credit-note-reason" />
                </label>
                <button type="button" class="ui-button btn-credit-solid action-button" (click)="submitCreditNote()"><app-ui-icon name="check" /> {{ t('billing.creditNote.submit', 'Enregistrer l’avoir') }}</button>
                <button type="button" class="ui-button ui-button-secondary" (click)="showCreditNoteForm.set(false)">{{ t('common.cancel', 'Annuler') }}</button>
              </div>
            </div>
          }

          <!-- Avoirs de la facture -->
          @if (creditNotes().length > 0) {
            <div class="sub-section">
              <h4 class="sub-section-title">Avoirs</h4>
              <div class="table-wrapper">
                <table class="data-table data-table-sm">
                  <thead><tr><th>N° Avoir</th><th>Montant</th><th>Motif</th><th>Date</th></tr></thead>
                  <tbody>
                    @for (cn of creditNotes(); track cn.id) {
                      <tr>
                        <td><span class="mono">{{ cn.creditNoteNumber }}</span></td>
                        <td class="amount text-warning">-{{ cn.amount | number:'1.0-0' }} FCFA</td>
                        <td>{{ cn.reason }}</td>
                        <td>{{ cn.createdAt | date:'dd/MM/yyyy' }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            </div>
          }

          <!-- Créances de la facture -->
          @if (receivables().length > 0) {
            <div class="sub-section">
              <h4 class="sub-section-title">Créances</h4>
              <div class="table-wrapper">
                <table class="data-table data-table-sm">
                  <thead><tr><th>Débiteur</th><th>Total</th><th>Payé</th><th>Reste</th><th>Statut</th></tr></thead>
                  <tbody>
                    @for (r of receivables(); track r.id) {
                      <tr>
                        <td><span class="badge badge-type">{{ r.debtorType }}</span></td>
                        <td class="amount">{{ r.totalAmount | number:'1.0-0' }} FCFA</td>
                        <td class="amount text-success">{{ r.paidAmount | number:'1.0-0' }} FCFA</td>
                        <td class="amount text-danger">{{ r.remainingAmount | number:'1.0-0' }} FCFA</td>
                        <td><span class="badge" [ngClass]="receivableStatusClass(r.status)">{{ r.status }}</span></td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            </div>
          }
        </section>
      }
    </div>
  `,
  styles: [`
    .estimates-panel { display: flex; flex-direction: column; gap: 1.5rem; }
    .invoice-detail-section { order: -1; }
    .panel-section { background: var(--app-surface); border: 1px solid var(--app-border); border-radius: 6px; padding: 1.25rem; }
    .section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1rem; }
    .section-title { display: flex; align-items: center; gap: 0.5rem; font-size: 0.95rem; font-weight: 600; color: var(--text-primary); margin: 0; }
    .section-caption { margin: 0.25rem 0 0; color: var(--text-muted); font-size: 0.72rem; }
    .section-icon { font-size: 1.1rem; color: var(--brand-primary); }
    .form-card { background: var(--app-bg); border: 1px solid var(--app-border); border-radius: 6px; padding: 1rem; margin-bottom: 1rem; }
    .action-form { border-left: 3px solid var(--brand-primary); }
    .form-intro { margin-bottom: 0.9rem; }
    .form-intro h4 { margin: 0.15rem 0 0.2rem; color: var(--text-primary); font-size: 0.9rem; }
    .form-intro p { margin: 0; color: var(--text-muted); font-size: 0.75rem; }
    .form-kicker { color: var(--brand-primary); font-size: 0.68rem; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; }
    .estimate-item-row { display: grid; grid-template-columns: minmax(0, 2fr) minmax(0, 1.5fr) minmax(0, 1fr) minmax(0, 0.5fr) auto; gap: 0.5rem; margin-bottom: 0.5rem; align-items: end; }
    .field-label { display: flex; flex: 1; min-width: 0; flex-direction: column; gap: 0.25rem; color: var(--text-secondary); font-size: 0.68rem; font-weight: 700; }
    .field-label-wide { min-width: 220px; }
    .estimate-item-row .ui-input, .estimate-item-row .ui-select { min-width: 0; width: 100%; }
    .form-grid { display: flex; flex-direction: column; gap: 0.25rem; margin-bottom: 0.75rem; }
    .form-actions { display: flex; align-items: center; gap: 0.5rem; }
    .spacer { flex: 1; }
    .action-bar { display: flex; gap: 0.5rem; flex-wrap: wrap; margin-bottom: 1rem; }
    .action-button { font-weight: 700; }
    .btn-credit { border: 1px solid var(--brand-warning-border); color: var(--brand-warning-text); background: var(--brand-warning-subtle); }
    .btn-credit:hover { background: var(--brand-warning-muted); }
    .btn-credit-solid { background: var(--brand-warning); color: var(--text-inverse); border: 1px solid var(--brand-warning); }
    .btn-credit-solid:hover { filter: brightness(0.95); }
    .inline-form { background: var(--app-bg); border: 1px solid var(--brand-primary-border); border-radius: 5px; padding: 1rem; margin-bottom: 1rem; }
    .credit-note-form { border-color: var(--brand-warning-border); border-left: 3px solid var(--brand-warning); }
    .inline-form-title { font-size: 0.85rem; font-weight: 600; margin: 0 0 0.75rem 0; color: var(--brand-primary); }
    .form-row { display: flex; gap: 0.5rem; flex-wrap: wrap; align-items: center; }
    .form-row .ui-input { flex: 1; min-width: 140px; }
    .sub-section { margin-top: 1rem; }
    .sub-section-title { font-size: 0.85rem; font-weight: 600; color: var(--app-text-muted); margin: 0 0 0.5rem 0; }
    .table-wrapper { overflow-x: auto; }
    .data-table { width: 100%; border-collapse: collapse; font-size: 0.8rem; }
    .data-table th, .data-table td { padding: 0.5rem 0.75rem; text-align: left; border-bottom: 1px solid var(--app-border); }
    .data-table th { font-weight: 600; color: var(--app-text-muted); background: var(--app-bg); }
    .data-table-sm th, .data-table-sm td { padding: 0.35rem 0.5rem; font-size: 0.78rem; }
    .amount { font-variant-numeric: tabular-nums; }
    .mono { font-family: monospace; font-size: 0.75rem; }
    .info-banner { display: flex; align-items: center; gap: 0.5rem; padding: 0.6rem 0.8rem; background: var(--brand-primary-subtle); border-radius: 5px; font-size: 0.82rem; margin-bottom: 0.75rem; }
    .actions-cell { white-space: nowrap; }
    .empty-state { text-align: center; padding: 1.5rem; color: var(--app-text-muted); font-size: 0.85rem; }
    .text-success { color: var(--app-success, #22c55e); }
    .text-danger { color: var(--app-danger, #ef4444); }
    .text-warning { color: var(--app-warning, #f59e0b); }
    .spinner { width: 12px; height: 12px; border: 2px solid transparent; border-top-color: currentColor; border-radius: 50%; animation: spin 0.6s linear infinite; display: inline-block; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .badge-type { font-size: 0.7rem; }
    .table-action { min-height: 30px; padding: 0 0.55rem; }
    @media (max-width: 700px) {
      .section-header { align-items: flex-start; gap: 0.75rem; }
      .section-header .action-button { flex-shrink: 0; }
      .form-actions { flex-wrap: wrap; }
      .form-row .field-label-wide { min-width: 100%; }
    }
  `]
})
export class BillingEstimatesComponent implements OnInit {
  @Output() close = new EventEmitter<void>();
  @Input() patientId!: string;
  @Input() settlement?: InvoiceSettlementSummary;
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
  savingEstimate = signal(false);

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
      error: () => this.showError('Impossible de charger les devis.')
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
    this.billingApi.createEstimate({ patientId: this.patientId, items }).subscribe({
      next: (est) => {
        this.estimates.update(list => [est, ...list]);
        this.showEstimateForm.set(false);
        this.newEstimateItems.set([{ label: '', itemType: 'CONSULTATION', unitPrice: 0, quantity: 1 }]);
        this.savingEstimate.set(false);
        this.showSuccess('Devis créé avec succès.');
      },
      error: () => {
        this.savingEstimate.set(false);
        this.showError('Erreur lors de la création du devis.');
      }
    });
  }

  acceptEstimate(est: Estimate): void {
    this.billingApi.updateEstimateStatus(est.id, 'ACCEPTED').subscribe({
      next: (updated) => this.estimates.update(list => list.map(e => e.id === updated.id ? updated : e)),
      error: () => this.showError('Erreur lors de la mise à jour du devis.')
    });
  }

  rejectEstimate(est: Estimate): void {
    this.billingApi.updateEstimateStatus(est.id, 'REJECTED').subscribe({
      next: (updated) => this.estimates.update(list => list.map(e => e.id === updated.id ? updated : e)),
      error: () => this.showError('Erreur lors du refus du devis.')
    });
  }

  validateCurrentInvoice(): void {
    const inv = this._selectedInvoice();
    if (!inv) return;
    this.billingApi.validateInvoice(inv.id).subscribe({
      next: (updated) => {
        this._selectedInvoice.set(updated);
        this.loadInvoiceData(updated.id);
        this.showSuccess('Facture validée avec succès.');
      },
      error: (err) => this.showError(err?.error?.message || 'Erreur lors de la validation.')
    });
  }

  cancelCurrentInvoice(): void {
    const inv = this._selectedInvoice();
    if (!inv || !confirm('Confirmer l\'annulation de cette facture ?')) return;
    this.billingApi.cancelInvoice(inv.id).subscribe({
      next: (updated) => {
        this._selectedInvoice.set(updated);
        this.showSuccess('Facture annulée.');
      },
      error: (err) => this.showError(err?.error?.message || 'Erreur lors de l\'annulation.')
    });
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
        this.showSuccess('Remise appliquée.');
      },
      error: (err) => this.showError(err?.error?.message || 'Erreur lors de l\'application de la remise.')
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
        this.showSuccess('Avoir créé avec succès.');
      },
      error: (err) => this.showError(err?.error?.message || 'Erreur lors de la création de l\'avoir.')
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
