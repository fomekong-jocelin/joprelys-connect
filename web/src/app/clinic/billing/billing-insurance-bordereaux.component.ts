import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BillingApiService } from '../../patient/billing-api.service';
import { InsuranceConvention, Bordereau, BordereauDetails } from '../../patient/patient.models';
import { IconComponent } from '../../shared/ui/icon.component';
import { extractApiErrorMessage } from '../../shared/utils/api-error.utils';

@Component({
  selector: 'app-billing-insurance-bordereaux',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent],
  template: `
    <div class="space-y-6">
      <!-- Top Action / Form & Filters -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        <!-- Generate Form Card -->
        <div class="ui-card-subtle p-4 space-y-4">
          <h3 class="font-bold text-xs text-[var(--text-primary)] uppercase tracking-wider border-b border-[var(--app-border)]/40 pb-2 flex items-center gap-1.5">
            <app-ui-icon name="plus" />
            Nouveau Bordereau d'Assurance
          </h3>
          
          <div class="space-y-3 text-xs">
            <div>
              <label class="font-bold text-[var(--text-secondary)] block mb-1">Convention d'Assurance :</label>
              <select [ngModel]="selectedConventionId()" (ngModelChange)="selectedConventionId.set($event)" class="ui-select w-full">
                <option value="">-- Choisir une convention --</option>
                @for (c of conventions(); track c.id) {
                  <option [value]="c.id">{{ c.name }} ({{ c.coveragePercentage * 100 }}%)</option>
                }
              </select>
            </div>

            <div class="grid grid-cols-2 gap-2">
              <div>
                <label class="font-bold text-[var(--text-secondary)] block mb-1">Date Début :</label>
                <input type="date" [ngModel]="startDate()" (ngModelChange)="startDate.set($event)" class="ui-input w-full" />
              </div>
              <div>
                <label class="font-bold text-[var(--text-secondary)] block mb-1">Date Fin :</label>
                <input type="date" [ngModel]="endDate()" (ngModelChange)="endDate.set($event)" class="ui-input w-full" />
              </div>
            </div>

            <button
              (click)="generateBordereau()"
              [disabled]="generating() || !selectedConventionId() || !startDate() || !endDate()"
              class="ui-button ui-button-primary w-full text-xs py-2 flex items-center justify-center gap-1.5"
            >
              @if (generating()) {
                <span>Génération en cours...</span>
              } @else {
                <app-ui-icon name="calculator" />
                Générer le Bordereau
              }
            </button>
          </div>
        </div>

        <!-- Bordereaux List / Filtered List -->
        <div class="lg:col-span-2 ui-card-subtle p-4 space-y-4">
          <div class="flex items-center justify-between border-b border-[var(--app-border)]/40 pb-2">
            <h3 class="font-bold text-xs text-[var(--text-primary)] uppercase tracking-wider flex items-center gap-1.5">
              <app-ui-icon name="document-text" />
              Liste des Bordereaux Générés
            </h3>
            
            <!-- Inline filter -->
            <select [ngModel]="statusFilter()" (ngModelChange)="statusFilter.set($event); applyFilters()" class="ui-select text-[10px] py-1">
              <option value="ALL">Tous les statuts</option>
              <option value="DRAFT">Brouillons (DRAFT)</option>
              <option value="SENT">Envoyés (SENT)</option>
              <option value="PAID">Réglés (PAID)</option>
            </select>
          </div>

          @if (filteredBordereaux().length > 0) {
            <div class="overflow-x-auto max-h-64 overflow-y-auto">
              <table class="w-full text-left text-xs border-collapse">
                <thead>
                  <tr class="bg-[var(--app-surface-muted)] border-b border-[var(--app-border)] text-[10px] font-bold text-[var(--text-secondary)]">
                    <th class="p-2" scope="col">N° Bordereau</th>
                    <th class="p-2" scope="col">Assurance / Convention</th>
                    <th class="p-2" scope="col">Période</th>
                    <th class="p-2 text-right" scope="col">Montant Total</th>
                    <th class="p-2" scope="col">Statut</th>
                    <th class="p-2 text-center" scope="col">Actions</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-[var(--app-border)]/40">
                  @for (b of filteredBordereaux(); track b.id) {
                    <tr class="hover:bg-[var(--app-surface-muted)]/30">
                      <td class="p-2 font-mono font-bold text-brand-cyan">{{ b.bordereauNumber }}</td>
                      <td class="p-2">{{ b.insuranceConventionName }}</td>
                      <td class="p-2 text-[10px]">{{ b.startDate | date:'dd/MM/yyyy' }} - {{ b.endDate | date:'dd/MM/yyyy' }}</td>
                      <td class="p-2 text-right font-bold">{{ b.totalAmount | number:'1.0-0' }} FCFA</td>
                      <td class="p-2">
                        <span class="px-1.5 py-0.5 text-[9px] font-bold rounded-sm"
                              [class.bg-gray-500\/10]="b.status === 'DRAFT'"
                              [class.text-gray-600]="b.status === 'DRAFT'"
                              [class.bg-blue-500\/10]="b.status === 'SENT'"
                              [class.text-blue-600]="b.status === 'SENT'"
                              [class.bg-emerald-500\/10]="b.status === 'PAID'"
                              [class.text-emerald-600]="b.status === 'PAID'">
                          {{ b.status }}
                        </span>
                      </td>
                      <td class="p-2 text-center">
                        <button (click)="viewDetails(b.id)" class="ui-button ui-button-secondary text-[10px] py-1 px-2">
                          Détails
                        </button>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          } @else {
            <p class="text-center text-[10px] text-[var(--text-muted)] py-8 italic">Aucun bordereau trouvé.</p>
          }
        </div>
      </div>

      <!-- Selected Bordereau Details Card -->
      @if (selectedBordereau()) {
        <div class="ui-card-subtle p-4 space-y-4">
          <div class="flex items-center justify-between border-b border-[var(--app-border)]/40 pb-2">
            <div>
              <h3 class="font-bold text-xs text-[var(--text-primary)]">
                Détails du Bordereau : <span class="font-mono text-brand-cyan">{{ selectedBordereau()!.bordereauNumber }}</span>
              </h3>
              <p class="text-[10px] text-[var(--text-muted)] mt-0.5">
                Période: {{ selectedBordereau()!.startDate | date:'dd/MM/yyyy' }} au {{ selectedBordereau()!.endDate | date:'dd/MM/yyyy' }} | Convention: {{ selectedBordereau()!.insuranceConventionName }}
              </p>
            </div>
            
            <div class="flex gap-2">
              @if (selectedBordereau()!.status === 'DRAFT') {
                <button (click)="markAsSent(selectedBordereau()!.id)" class="ui-button ui-button-primary text-xs py-1 px-3 flex items-center gap-1">
                  <app-ui-icon name="document-text" />
                  Marquer comme envoyé
                </button>
              }
              @if (selectedBordereau()!.status === 'SENT') {
                <button (click)="showPaymentForm.set(true)" class="ui-button ui-button-success text-xs py-1 px-3 flex items-center gap-1">
                  <app-ui-icon name="credit-card" />
                  Enregistrer le Règlement
                </button>
              }
              <button (click)="selectedBordereau.set(null); showPaymentForm.set(false)" class="ui-button ui-button-secondary text-xs py-1 px-3">
                Fermer
              </button>
            </div>
          </div>

          <!-- Payment Form Inline -->
          @if (showPaymentForm()) {
            <div class="p-3 bg-[var(--app-surface-muted)] border border-brand-cyan/30 rounded-sm grid grid-cols-1 md:grid-cols-3 gap-4 items-end text-xs">
              <div>
                <label class="font-bold text-[var(--text-secondary)] block mb-1">Montant Reçu (FCFA) :</label>
                <input type="number" [ngModel]="paymentAmount()" class="ui-input w-full" disabled />
              </div>
              <div>
                <label class="font-bold text-[var(--text-secondary)] block mb-1">Référence du Règlement :</label>
                <input type="text" [ngModel]="paymentReference()" (ngModelChange)="paymentReference.set($event)" class="ui-input w-full" placeholder="N° Virement, Chèque..." />
              </div>
              <div class="flex gap-2">
                <button (click)="submitPayment()" [disabled]="!paymentReference().trim()" class="ui-button ui-button-primary flex-1 py-1.5">
                  Valider le Règlement
                </button>
                <button (click)="showPaymentForm.set(false)" class="ui-button ui-button-secondary py-1.5">
                  Annuler
                </button>
              </div>
            </div>
          }

          <!-- Linked Invoices Table -->
          <div class="space-y-2">
            <h4 class="font-bold text-[10px] uppercase text-[var(--text-secondary)] tracking-wider">
              Factures associées ({{ selectedBordereau()!.invoices.length }})
            </h4>
            <div class="overflow-x-auto max-h-60 overflow-y-auto">
              <table class="w-full text-left text-xs border-collapse">
                <thead>
                  <tr class="bg-[var(--app-surface-muted)]/60 border-b border-[var(--app-border)]/60 text-[10px] font-bold text-[var(--text-secondary)]">
                    <th class="p-2" scope="col">N° Facture</th>
                    <th class="p-2 text-right" scope="col">Montant Total</th>
                    <th class="p-2 text-right" scope="col">Part Patient</th>
                    <th class="p-2 text-right" scope="col">Part Assurance</th>
                    <th class="p-2" scope="col">Statut</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-[var(--app-border)]/40">
                  @for (inv of selectedBordereau()!.invoices; track inv.id) {
                    <tr>
                      <td class="p-2 font-mono font-bold text-brand-cyan">{{ inv.invoiceNumber }}</td>
                      <td class="p-2 text-right">{{ inv.totalAmount | number:'1.0-0' }} FCFA</td>
                      <td class="p-2 text-right">{{ inv.patientShare | number:'1.0-0' }} FCFA</td>
                      <td class="p-2 text-right font-bold text-brand-cyan">{{ inv.insuranceShare | number:'1.0-0' }} FCFA</td>
                      <td class="p-2 text-[10px]">{{ inv.status }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </div>
        </div>
      }

      <!-- Alert feedbacks -->
      @if (successFeedback()) {
        <div class="ui-alert-success text-xs">
          <app-ui-icon name="check" />
          {{ successFeedback() }}
        </div>
      }
      @if (errorFeedback()) {
        <div class="ui-alert-danger text-xs">
          <app-ui-icon name="x-mark" />
          {{ errorFeedback() }}
        </div>
      }
    </div>
  `
})
export class BillingInsuranceBordereauxComponent implements OnInit {
  private readonly billingApi = inject(BillingApiService);

  conventions = signal<InsuranceConvention[]>([]);
  bordereaux = signal<Bordereau[]>([]);
  filteredBordereaux = signal<Bordereau[]>([]);
  selectedBordereau = signal<BordereauDetails | null>(null);

  // Form signals
  selectedConventionId = signal<string>('');
  startDate = signal<string>('');
  endDate = signal<string>('');
  generating = signal<boolean>(false);

  // Filters
  statusFilter = signal<string>('ALL');

  // Payment Form signals
  showPaymentForm = signal<boolean>(false);
  paymentAmount = signal<number>(0);
  paymentReference = signal<string>('');

  // Alerts
  successFeedback = signal<string | null>(null);
  errorFeedback = signal<string | null>(null);

  ngOnInit() {
    this.loadConventions();
    this.loadBordereaux();
  }

  loadConventions() {
    this.billingApi.listConventions().subscribe({
      next: (res) => this.conventions.set(res),
      error: () => this.showError('Erreur de chargement des conventions')
    });
  }

  loadBordereaux() {
    this.billingApi.listInsuranceBordereaux().subscribe({
      next: (res) => {
        this.bordereaux.set(res);
        this.applyFilters();
      },
      error: () => this.showError('Erreur de chargement des bordereaux')
    });
  }

  applyFilters() {
    const filter = this.statusFilter();
    if (filter === 'ALL') {
      this.filteredBordereaux.set(this.bordereaux());
    } else {
      this.filteredBordereaux.set(this.bordereaux().filter(b => b.status === filter));
    }
  }

  generateBordereau() {
    this.clearFeedbacks();

    const validationError = this.validateGenerateForm();
    if (validationError) {
      this.showError(validationError);
      return;
    }

    this.generating.set(true);
    this.billingApi.generateInsuranceBordereau({
      insuranceConventionId: this.selectedConventionId(),
      startDate: this.startDate(),
      endDate: this.endDate()
    }).subscribe({
      next: (res) => {
        this.generating.set(false);
        this.selectedConventionId.set('');
        this.startDate.set('');
        this.endDate.set('');
        this.showSuccess('Bordereau d\'assurance généré avec succès.');
        this.loadBordereaux();
      },
      error: (err) => {
        this.generating.set(false);
        this.showError(this.extractErrorMessage(err) || 'Impossible de générer le bordereau. Vérifiez qu\'il existe des factures éligibles pour cette convention et cette période.');
      }
    });
  }

  private validateGenerateForm(): string | null {
    if (!this.selectedConventionId()) {
      return 'Veuillez sélectionner une convention d\'assurance.';
    }
    if (!this.startDate() || !this.endDate()) {
      return 'Veuillez renseigner les dates de début et de fin.';
    }
    if (this.startDate() > this.endDate()) {
      return 'La date de début doit être antérieure ou égale à la date de fin.';
    }
    return null;
  }

  viewDetails(id: string) {
    this.billingApi.getInsuranceBordereauDetails(id).subscribe({
      next: (res) => {
        this.selectedBordereau.set(res);
        this.paymentAmount.set(res.totalAmount);
        this.paymentReference.set('');
        this.showPaymentForm.set(false);
      },
      error: () => this.showError('Impossible de récupérer les détails du bordereau.')
    });
  }

  markAsSent(id: string) {
    this.billingApi.sendInsuranceBordereau(id).subscribe({
      next: (res) => {
        this.showSuccess('Le bordereau a été marqué comme envoyé.');
        this.viewDetails(id);
        this.loadBordereaux();
      },
      error: () => this.showError('Erreur lors de la mise à jour du statut du bordereau.')
    });
  }

  submitPayment() {
    const bordereau = this.selectedBordereau();
    if (!bordereau) return;

    this.billingApi.payInsuranceBordereau(bordereau.id, this.paymentAmount(), this.paymentReference()).subscribe({
      next: (res) => {
        this.showSuccess('Le règlement global a été enregistré avec succès.');
        this.showPaymentForm.set(false);
        this.viewDetails(bordereau.id);
        this.loadBordereaux();
      },
      error: (err) => {
        this.showError(this.extractErrorMessage(err) || 'Impossible d\'enregistrer le règlement. Vérifiez le montant et la référence.');
      }
    });
  }

  private showSuccess(msg: string) {
    this.successFeedback.set(msg);
    setTimeout(() => this.successFeedback.set(null), 5000);
  }

  private showError(msg: string) {
    this.errorFeedback.set(msg);
    setTimeout(() => this.errorFeedback.set(null), 5000);
  }

  private clearFeedbacks() {
    this.successFeedback.set(null);
    this.errorFeedback.set(null);
  }

  private extractErrorMessage(err: any): string | null {
    return extractApiErrorMessage(err);
  }
}
