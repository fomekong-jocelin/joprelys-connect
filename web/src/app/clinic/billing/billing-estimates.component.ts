import { Component, OnInit, inject, signal, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BillingApiService } from '../../patient/billing-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { Invoice, Estimate, CreditNote, Receivable } from '../../patient/patient.models';
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
      <section class="panel-section">
        <div class="section-header">
          <h3 class="section-title">
            <app-ui-icon name="document-text" class="section-icon" />
            {{ t('billing.estimates.title', 'Devis / Proformas') }}
          </h3>
          <button class="btn btn-primary btn-sm" (click)="showEstimateForm.set(!showEstimateForm())">
            <app-ui-icon name="plus" />
            {{ t('billing.estimates.new', 'Nouveau devis') }}
          </button>
        </div>

        @if (showEstimateForm()) {
          <div class="form-card" role="form" aria-label="Formulaire devis">
            <div class="form-grid">
              @for (item of newEstimateItems(); track $index) {
                <div class="estimate-item-row">
                  <input class="input" [(ngModel)]="item.label" placeholder="Libellé de l'acte" />
                  <select class="input" [(ngModel)]="item.itemType">
                    <option value="CONSULTATION">Consultation</option>
                    <option value="K_SURGEON">K Chirurgien</option>
                    <option value="K_ANESTHESIST">K Anesthésiste</option>
                    <option value="MEDICATION">Médicament</option>
                    <option value="STAY_FEE">Frais de séjour</option>
                  </select>
                  <input class="input input-sm" type="number" [(ngModel)]="item.unitPrice" placeholder="Prix unitaire" min="0" />
                  <input class="input input-sm" type="number" [(ngModel)]="item.quantity" placeholder="Qté" min="1" />
                  <button class="btn btn-ghost btn-xs" (click)="removeEstimateItem($index)">
                    <app-ui-icon name="trash" />
                  </button>
                </div>
              }
            </div>
            <div class="form-actions">
              <button class="btn btn-ghost btn-sm" (click)="addEstimateItem()">
                <app-ui-icon name="plus" /> Ajouter une ligne
              </button>
              <div class="spacer"></div>
              <button class="btn btn-outline btn-sm" (click)="showEstimateForm.set(false)">Annuler</button>
              <button class="btn btn-primary btn-sm" (click)="submitEstimate()" [disabled]="savingEstimate()">
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
                        <button class="btn btn-ghost btn-xs" (click)="acceptEstimate(est)" title="Accepter">
                          <app-ui-icon name="check" />
                        </button>
                        <button class="btn btn-ghost btn-xs text-danger" (click)="rejectEstimate(est)" title="Refuser">
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
        <section class="panel-section">
          <div class="section-header">
            <h3 class="section-title">
              <app-ui-icon name="check" class="section-icon" />
              Facture sélectionnée — {{ selectedInvoice()!.invoiceNumber }}
            </h3>
            <div class="badge" [ngClass]="invoiceStatusClass(selectedInvoice()!.status)">
              {{ selectedInvoice()!.status }}
            </div>
          </div>

          @if (selectedInvoice()!.discountAmount && selectedInvoice()!.discountAmount! > 0) {
            <div class="info-banner">
              <app-ui-icon name="receipt-percent" />
              Remise appliquée : <strong>{{ selectedInvoice()!.discountAmount | number:'1.0-0' }} FCFA</strong>
              — {{ selectedInvoice()!.discountReason }}
            </div>
          }

          <div class="action-bar">
            @if (selectedInvoice()!.status === 'PENDING' || selectedInvoice()!.status === 'PROFORMA') {
              <button class="btn btn-success btn-sm" (click)="validateCurrentInvoice()">
                <app-ui-icon name="check" /> Valider la facture
              </button>
              <button class="btn btn-outline btn-sm" (click)="showDiscountForm.set(!showDiscountForm())">
                <app-ui-icon name="receipt-percent" /> Appliquer une remise
              </button>
              <button class="btn btn-danger-outline btn-sm" (click)="cancelCurrentInvoice()">
                <app-ui-icon name="x-mark" /> Annuler la facture
              </button>
            }
            @if (selectedInvoice()!.status === 'VALIDATED' || selectedInvoice()!.status === 'PAID') {
              <button class="btn btn-outline btn-sm" (click)="showCreditNoteForm.set(!showCreditNoteForm())">
                <app-ui-icon name="arrow-path" /> Créer un avoir
              </button>
            }
          </div>

          <!-- Formulaire remise -->
          @if (showDiscountForm()) {
            <div class="inline-form" role="form" aria-label="Formulaire remise">
              <h4 class="inline-form-title">Remise commerciale</h4>
              <div class="form-row">
                <input class="input" type="number" [(ngModel)]="discountAmount" placeholder="Montant remise (FCFA)" min="0" id="discount-amount" />
                <input class="input" [(ngModel)]="discountReason" placeholder="Motif de la remise" id="discount-reason" />
                <button class="btn btn-primary btn-sm" (click)="submitDiscount()">Appliquer</button>
                <button class="btn btn-ghost btn-sm" (click)="showDiscountForm.set(false)">Annuler</button>
              </div>
            </div>
          }

          <!-- Formulaire avoir -->
          @if (showCreditNoteForm()) {
            <div class="inline-form" role="form" aria-label="Formulaire avoir">
              <h4 class="inline-form-title">Créer un avoir</h4>
              <div class="form-row">
                <input class="input" type="number" [(ngModel)]="creditNoteAmount" placeholder="Montant de l'avoir (FCFA)" min="0" id="credit-note-amount" />
                <input class="input" [(ngModel)]="creditNoteReason" placeholder="Motif de l'avoir" id="credit-note-reason" />
                <button class="btn btn-primary btn-sm" (click)="submitCreditNote()">Créer l'avoir</button>
                <button class="btn btn-ghost btn-sm" (click)="showCreditNoteForm.set(false)">Annuler</button>
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
    .panel-section { background: var(--app-surface); border: 1px solid var(--app-border); border-radius: 6px; padding: 1.25rem; }
    .section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1rem; }
    .section-title { display: flex; align-items: center; gap: 0.5rem; font-size: 0.95rem; font-weight: 600; color: var(--app-text); margin: 0; }
    .section-icon { font-size: 1.1rem; color: var(--app-primary); }
    .form-card { background: var(--app-bg); border: 1px solid var(--app-border); border-radius: 6px; padding: 1rem; margin-bottom: 1rem; }
    .estimate-item-row { display: grid; grid-template-columns: 2fr 1.5fr 1fr 0.5fr auto; gap: 0.5rem; margin-bottom: 0.5rem; align-items: center; }
    .form-grid { display: flex; flex-direction: column; gap: 0.25rem; margin-bottom: 0.75rem; }
    .form-actions { display: flex; align-items: center; gap: 0.5rem; }
    .spacer { flex: 1; }
    .action-bar { display: flex; gap: 0.5rem; flex-wrap: wrap; margin-bottom: 1rem; }
    .inline-form { background: var(--app-bg); border: 1px solid var(--app-primary); border-radius: 5px; padding: 1rem; margin-bottom: 1rem; }
    .inline-form-title { font-size: 0.85rem; font-weight: 600; margin: 0 0 0.75rem 0; color: var(--app-primary); }
    .form-row { display: flex; gap: 0.5rem; flex-wrap: wrap; align-items: center; }
    .form-row .input { flex: 1; min-width: 140px; }
    .sub-section { margin-top: 1rem; }
    .sub-section-title { font-size: 0.85rem; font-weight: 600; color: var(--app-text-muted); margin: 0 0 0.5rem 0; }
    .table-wrapper { overflow-x: auto; }
    .data-table { width: 100%; border-collapse: collapse; font-size: 0.8rem; }
    .data-table th, .data-table td { padding: 0.5rem 0.75rem; text-align: left; border-bottom: 1px solid var(--app-border); }
    .data-table th { font-weight: 600; color: var(--app-text-muted); background: var(--app-bg); }
    .data-table-sm th, .data-table-sm td { padding: 0.35rem 0.5rem; font-size: 0.78rem; }
    .amount { font-variant-numeric: tabular-nums; }
    .mono { font-family: monospace; font-size: 0.75rem; }
    .info-banner { display: flex; align-items: center; gap: 0.5rem; padding: 0.6rem 0.8rem; background: color-mix(in srgb, var(--app-primary) 10%, transparent); border-radius: 5px; font-size: 0.82rem; margin-bottom: 0.75rem; }
    .actions-cell { white-space: nowrap; }
    .empty-state { text-align: center; padding: 1.5rem; color: var(--app-text-muted); font-size: 0.85rem; }
    .text-success { color: var(--app-success, #22c55e); }
    .text-danger { color: var(--app-danger, #ef4444); }
    .text-warning { color: var(--app-warning, #f59e0b); }
    .spinner { width: 12px; height: 12px; border: 2px solid transparent; border-top-color: currentColor; border-radius: 50%; animation: spin 0.6s linear infinite; display: inline-block; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .badge-type { font-size: 0.7rem; }
  `]
})
export class BillingEstimatesComponent implements OnInit {
  @Input() patientId!: string;
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
