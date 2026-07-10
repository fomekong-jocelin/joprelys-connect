import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BillingApiService } from '../../patient/billing-api.service';
import { Receivable } from '../../patient/patient.models';
import { IconComponent } from '../../shared/ui/icon.component';
import { I18nService } from '../../core/i18n/i18n.service';
import { BillingReminderModalComponent } from './billing-reminder-modal.component';

@Component({
  selector: 'app-billing-receivables',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent, BillingReminderModalComponent],
  template: `
    <div class="space-y-6">
      <!-- Filter Bar -->
      <div class="ui-card-subtle p-4 flex flex-wrap gap-4 items-center justify-between text-xs">
        <div class="flex flex-wrap gap-3 items-center">
          <div>
            <label class="font-bold text-[var(--text-secondary)] block mb-1">Filtrer par statut :</label>
            <select [(ngModel)]="statusFilter" (change)="applyFilters()" class="ui-select">
              <option value="ALL">Tous les statuts</option>
              <option value="UNPAID">Non payé (UNPAID)</option>
              <option value="PARTIALLY_PAID">Partiellement payé</option>
              <option value="PAID">Réglé (PAID)</option>
            </select>
          </div>

          <div>
            <label class="font-bold text-[var(--text-secondary)] block mb-1">Recherche débiteur :</label>
            <input type="text" [(ngModel)]="searchDebtor" (input)="applyFilters()" class="ui-input" placeholder="ID Débiteur..." />
          </div>

          <div>
            <label class="font-bold text-[var(--text-secondary)] block mb-1">Type de débiteur :</label>
            <select [(ngModel)]="debtorTypeFilter" (change)="applyFilters()" class="ui-select">
              <option value="ALL">Tous types</option>
              <option value="PATIENT">Patient</option>
              <option value="INSURANCE">Assurance (Tiers-Payant)</option>
            </select>
          </div>

          <div>
            <label class="font-bold text-[var(--text-secondary)] block mb-1">Tranche d'ancienneté :</label>
            <select [(ngModel)]="agingFilter" (change)="applyFilters()" class="ui-select">
              <option value="ALL">Toutes les tranches</option>
              <option value="0_30">Sain (0-30j)</option>
              <option value="31_60">À relancer (31-60j)</option>
              <option value="61_90">Urgent (61-90j)</option>
              <option value="90_PLUS">Contentieux (>90j)</option>
            </select>
          </div>
        </div>

        <div class="flex gap-2 self-end">
          <div class="px-3 py-2 bg-[var(--app-surface-muted)] border border-[var(--app-border)]/60 rounded-sm">
            <span class="text-[var(--text-muted)] text-[10px] block uppercase font-bold">Reste à recouvrer</span>
            <span class="font-bold text-sm text-[var(--text-primary)]">{{ totalRemaining() | number:'1.0-0' }} FCFA</span>
          </div>
        </div>
      </div>

      <!-- Receivables Table -->
      <div class="ui-card-subtle p-4 space-y-3">
        <h3 class="font-bold text-xs text-[var(--text-primary)] uppercase tracking-wider border-b border-[var(--app-border)]/40 pb-2">
          Suivi des créances clients & tiers-payant
        </h3>

        @if (filteredReceivables().length > 0) {
          <div class="overflow-x-auto">
            <table class="w-full text-left text-xs border-collapse">
              <thead>
                <tr class="bg-[var(--app-surface-muted)] border-b border-[var(--app-border)] text-[10px] font-bold text-[var(--text-secondary)]">
                  <th class="p-2" scope="col">Date Création</th>
                  <th class="p-2" scope="col">Facture</th>
                  <th class="p-2" scope="col">Débiteur</th>
                  <th class="p-2 text-right" scope="col">Montant Initial</th>
                  <th class="p-2 text-right" scope="col">Montant Réglé</th>
                  <th class="p-2 text-right" scope="col">Solde Restant</th>
                  <th class="p-2" scope="col">Statut</th>
                  <th class="p-2" scope="col">Tranche (Aging)</th>
                  <th class="p-2" scope="col">Action</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-[var(--app-border)]/40">
                @for (r of filteredReceivables(); track r.id) {
                  <tr class="hover:bg-[var(--app-surface-muted)]/30">
                    <td class="p-2 text-[10px]">{{ r.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
                    <td class="p-2 text-[10px] font-bold text-brand-cyan">{{ r.invoiceId.substring(0, 8) }}...</td>
                    <td class="p-2">
                      <span class="px-1.5 py-0.5 text-[9px] font-bold rounded-sm mr-1.5"
                            [class.bg-blue-500\/10]="r.debtorType === 'INSURANCE'"
                            [class.text-blue-600]="r.debtorType === 'INSURANCE'"
                            [class.bg-purple-500\/10]="r.debtorType === 'PATIENT'"
                            [class.text-purple-600]="r.debtorType === 'PATIENT'">
                        {{ r.debtorType }}
                      </span>
                      <span class="text-[10px] font-mono">{{ r.debtorId }}</span>
                    </td>
                    <td class="p-2 text-right">{{ r.totalAmount | number:'1.0-0' }} FCFA</td>
                    <td class="p-2 text-right text-emerald-600 font-semibold">{{ r.paidAmount | number:'1.0-0' }} FCFA</td>
                    <td class="p-2 text-right font-bold text-[var(--text-primary)]">{{ r.remainingAmount | number:'1.0-0' }} FCFA</td>
                    <td class="p-2">
                      <span class="px-1.5 py-0.5 text-[9px] font-bold rounded-sm"
                            [class.bg-red-500\/10]="r.status === 'UNPAID'"
                            [class.text-red-600]="r.status === 'UNPAID'"
                            [class.bg-amber-500\/10]="r.status === 'PARTIALLY_PAID'"
                            [class.text-amber-600]="r.status === 'PARTIALLY_PAID'"
                            [class.bg-emerald-500\/10]="r.status === 'PAID'"
                            [class.text-emerald-600]="r.status === 'PAID'">
                        {{ r.status === 'UNPAID' ? 'Non payé' : r.status === 'PARTIALLY_PAID' ? 'Partiel' : 'Réglé' }}
                      </span>
                    </td>
                    <td class="p-2">
                      <span class="px-1.5 py-0.5 text-[9px] font-bold rounded-sm"
                            [class.bg-emerald-500\/10]="r.agingSlice === '0_30'"
                            [class.text-emerald-600]="r.agingSlice === '0_30'"
                            [class.bg-blue-500\/10]="r.agingSlice === '31_60'"
                            [class.text-blue-600]="r.agingSlice === '31_60'"
                            [class.bg-amber-500\/10]="r.agingSlice === '61_90'"
                            [class.text-amber-600]="r.agingSlice === '61_90'"
                            [class.bg-rose-500\/10]="r.agingSlice === '90_PLUS'"
                            [class.text-rose-600]="r.agingSlice === '90_PLUS'">
                        {{ r.agingSlice === '0_30' ? '0-30j (Sain)' : r.agingSlice === '31_60' ? '31-60j' : r.agingSlice === '61_90' ? '61-90j' : '>90j (Juridique)' }}
                      </span>
                    </td>
                    <td class="p-2">
                      <button (click)="openReminderModal(r)" class="text-xs text-[var(--brand-cyan)] hover:underline flex items-center gap-1">
                        <app-ui-icon name="information-circle" />
                        Relancer
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        } @else {
          <p class="text-center text-[10px] text-[var(--text-muted)] py-6 italic">Aucune créance ne correspond aux critères de filtre.</p>
        }
      </div>

      <!-- Success Alert -->
      @if (reminderSentMessage()) {
        <div class="fixed bottom-4 right-4 z-50 p-3 bg-emerald-500 text-white rounded-sm text-xs shadow-lg flex gap-2 items-center">
          <app-ui-icon name="check" />
          <span>{{ reminderSentMessage() }}</span>
          <button (click)="reminderSentMessage.set(null)" [attr.aria-label]="t('common.aria.close', 'Fermer')" class="hover:opacity-75 font-bold"><app-ui-icon name="x-mark" /></button>
        </div>
      }

      <!-- Reminder Modal -->
      <app-billing-reminder-modal
        [visible]="reminderModalVisible()"
        [receivable]="selectedReceivable()"
        [saving]="savingReminder()"
        (close)="reminderModalVisible.set(false)"
        (submitReminder)="onReminderSubmitted($event)"
      />
    </div>
  `
})
export class BillingReceivablesComponent implements OnInit {
  private readonly billingApi = inject(BillingApiService);
  private readonly i18n = inject(I18nService);

  t(key: string, defaultValue: string): string {
    return this.i18n.t(key, defaultValue);
  }

  receivables = signal<Receivable[]>([]);
  filteredReceivables = signal<Receivable[]>([]);

  // Filters
  statusFilter = signal<string>('ALL');
  searchDebtor = signal<string>('');
  debtorTypeFilter = signal<string>('ALL');
  agingFilter = signal<string>('ALL');

  reminderSentMessage = signal<string | null>(null);

  // Modal State
  reminderModalVisible = signal<boolean>(false);
  selectedReceivable = signal<Receivable | null>(null);
  savingReminder = signal<boolean>(false);

  totalRemaining = computed(() => {
    return this.filteredReceivables().reduce((acc, r) => acc + r.remainingAmount, 0);
  });

  ngOnInit() {
    this.loadAllReceivables();
  }

  loadAllReceivables() {
    this.billingApi.getReceivablesByStatus('ALL').subscribe({
      next: (data) => {
        this.receivables.set(data);
        this.applyFilters();
      }
    });
  }

  applyFilters() {
    let result = this.receivables();

    // Filtre par statut
    if (this.statusFilter() !== 'ALL') {
      result = result.filter(r => r.status === this.statusFilter());
    }

    // Filtre par type débiteur
    if (this.debtorTypeFilter() !== 'ALL') {
      result = result.filter(r => r.debtorType === this.debtorTypeFilter());
    }

    // Filtre par ID débiteur
    if (this.searchDebtor().trim()) {
      const term = this.searchDebtor().trim().toLowerCase();
      result = result.filter(r => r.debtorId.toLowerCase().includes(term));
    }

    // Filtre par balance âgée (agingSlice)
    if (this.agingFilter() !== 'ALL') {
      result = result.filter(r => r.agingSlice === this.agingFilter());
    }

    this.filteredReceivables.set(result);
  }

  openReminderModal(r: Receivable) {
    this.selectedReceivable.set(r);
    this.reminderModalVisible.set(true);
  }

  onReminderSubmitted(form: any) {
    const receivable = this.selectedReceivable();
    if (!receivable) return;

    this.savingReminder.set(true);
    this.billingApi.recordReminder(receivable.id, form).subscribe({
      next: () => {
        this.savingReminder.set(false);
        this.reminderModalVisible.set(false);
        this.reminderSentMessage.set(`L'action de relance a bien été consignée.`);
        setTimeout(() => this.reminderSentMessage.set(null), 4000);
        this.loadAllReceivables();
      },
      error: () => {
      }
    });
  }
}
