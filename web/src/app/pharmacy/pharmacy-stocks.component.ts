import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { PharmacyApiService } from './pharmacy-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { DrugStockResponse, CreateDrugStockRequest } from './pharmacy.models';
import { Router } from '@angular/router';
import { AppShellComponent } from '../shared/layout/app-shell.component';

@Component({
  selector: 'app-pharmacy-stocks',
  standalone: true,
  imports: [DatePipe, AppShellComponent],
  template: `
    <app-shell>
      <!-- En-tête de la page -->
      <header class="bg-[var(--app-surface)] border-b border-[var(--app-border)]/80 sticky top-0 z-30 transition-colors">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div class="flex items-center gap-3">
            <button 
              (click)="goBack()"
              class="p-2 text-[var(--text-muted)] hover:text-[var(--text-secondary)] dark:hover:text-[var(--text-primary)] rounded-[var(--radius-brand-sm)] hover:bg-[var(--app-surface-muted)] transition-colors cursor-pointer"
            >
              <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
              </svg>
            </button>
            <div>
              <h1 class="text-xl font-display font-bold text-[var(--text-primary)]">{{ t('pharmacy.stocks.title') }}</h1>
              <p class="text-xs text-[var(--text-muted)]">{{ t('pharmacy.stocks.subtitle') }}</p>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <!-- Filtre Alertes -->
            <button
              (click)="toggleFilter()"
              [class]="filterOnlyAlerts() 
                ? 'px-3 py-1.5 rounded-[var(--radius-brand-sm)] text-xs font-semibold bg-[var(--brand-danger-subtle)] text-[var(--brand-danger-text)] dark:bg-red-950/20 dark:text-red-400 border border-red-200 dark:border-red-800/40 cursor-pointer transition-all'
                : 'px-3 py-1.5 rounded-[var(--radius-brand-sm)] text-xs font-semibold bg-white text-[var(--text-secondary)] dark:bg-slate-900 dark:text-[var(--text-muted)] border border-[var(--app-border)]/60 hover:bg-[var(--app-surface-muted)] dark:hover:bg-slate-800/50 cursor-pointer transition-all'"
            >
              {{ t('pharmacy.stocks.alerts') }} 
              @if (alertCount() > 0) {
                <span class="ml-1 px-1.5 py-0.5 rounded-full text-[10px] bg-red-600 text-white font-bold">{{ alertCount() }}</span>
              }
            </button>

            <!-- Bouton Ajouter -->
            <button
              (click)="openAddModal()"
              class="px-4 py-1.5 rounded-[var(--radius-brand-sm)] text-xs font-semibold text-white bg-brand-cyan hover:bg-[var(--brand-primary-hover)] active:bg-[var(--brand-primary-active)] transition-all cursor-pointer flex items-center gap-1.5"
            >
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
              {{ t('pharmacy.stocks.addStock') }}
            </button>
          </div>
        </div>
      </header>

      <!-- Zone d'affichage principale -->
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        @if (error()) {
          <div class="mb-4 p-4 bg-[var(--brand-danger-subtle)] text-sm text-[var(--brand-danger-text)] rounded-[var(--radius-brand-sm)] font-semibold flex items-center gap-2 border border-red-200/50 dark:border-red-800/20">
            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            {{ error() }}
          </div>
        }

        @if (loading()) {
          <div class="flex items-center justify-center py-12">
            <div class="flex flex-col items-center gap-3">
              <div class="w-8 h-8 rounded-full border-2 border-[var(--app-border)] border-t-brand-cyan animate-spin"></div>
              <p class="text-xs text-[var(--text-muted)]">{{ t('common.loading') }}</p>
            </div>
          </div>
        } @else {
          <!-- Tableau des stocks -->
          @if (filteredStocks().length === 0) {
            <div class="bg-[var(--app-surface)] border border-[var(--app-border)]/80 rounded-[var(--radius-brand-sm)] p-12 text-center shadow-xs">
              <div class="w-12 h-12 rounded-full bg-[var(--app-surface-muted)] dark:bg-[var(--bg-input)] flex items-center justify-center mx-auto mb-4 text-[var(--text-muted)]">
                <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
                </svg>
              </div>
              <h3 class="text-sm font-semibold text-[var(--text-primary)] mb-1">{{ t('pharmacy.stocks.empty') }}</h3>
            </div>
          } @else {
            <div class="bg-[var(--app-surface)] border border-[var(--app-border)]/80 rounded-[var(--radius-brand-sm)] shadow-xs overflow-hidden">
              <div class="overflow-x-auto">
                <table class="w-full text-left border-collapse">
                  <thead>
                    <tr class="bg-[var(--app-surface-muted)] dark:bg-[var(--bg-input)]/40 text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider border-b border-[var(--app-border)]">
                      <th class="py-3 px-4">{{ t('pharmacy.stocks.drugName') }}</th>
                      <th class="py-3 px-4">{{ t('pharmacy.stocks.genericName') }}</th>
                      <th class="py-3 px-4 text-center">{{ t('pharmacy.stocks.quantityAvailable') }}</th>
                      <th class="py-3 px-4 text-center">{{ t('pharmacy.stocks.minimumThreshold') }}</th>
                      <th class="py-3 px-4">{{ t('pharmacy.stocks.batchNumber') }}</th>
                      <th class="py-3 px-4">{{ t('pharmacy.stocks.expiryDate') }}</th>
                      <th class="py-3 px-4">{{ t('pharmacy.stocks.updatedAt') }}</th>
                      <th class="py-3 px-4 text-right">{{ t('common.actions') }}</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-slate-100 dark:divide-slate-800 text-sm text-[var(--text-secondary)]">
                    @for (stock of filteredStocks(); track stock.id) {
                      <tr class="hover:bg-[var(--app-surface-muted)] dark:hover:bg-slate-800/30 transition-colors">
                        <td class="py-3.5 px-4 font-semibold text-[var(--text-primary)]">
                          {{ stock.drugName }}
                        </td>
                        <td class="py-3.5 px-4 text-xs text-[var(--text-muted)]">
                          {{ stock.genericName || '—' }}
                        </td>
                        <td class="py-3.5 px-4 text-center">
                          <div class="flex items-center justify-center gap-1.5">
                            <span 
                              [class]="stock.belowThreshold 
                                ? 'px-2 py-0.5 rounded-sm text-xs font-bold bg-[var(--brand-danger-subtle)] text-red-700 dark:bg-red-950/30 dark:text-red-400 border border-[var(--brand-danger-border)] dark:border-red-900/20' 
                                : 'px-2 py-0.5 rounded-sm text-xs font-semibold bg-[var(--brand-success-subtle)] text-[var(--brand-success-text)]  dark:text-emerald-400'"
                            >
                              {{ stock.quantityAvailable }} {{ stock.unit }}
                            </span>
                          </div>
                        </td>
                        <td class="py-3.5 px-4 text-center text-xs text-[var(--text-muted)]">
                          {{ stock.minimumThreshold }}
                        </td>
                        <td class="py-3.5 px-4 text-xs font-mono">
                          {{ stock.batchNumber || '—' }}
                        </td>
                        <td class="py-3.5 px-4 text-xs">
                          @if (stock.expiryDate) {
                            <span [class]="isExpired(stock.expiryDate) ? 'text-[var(--brand-danger-text)] font-semibold' : ''">
                              {{ stock.expiryDate | date:'dd/MM/yyyy' }}
                            </span>
                          } @else {
                            —
                          }
                        </td>
                        <td class="py-3.5 px-4 text-xs text-[var(--text-muted)]">
                          {{ stock.updatedAt | date:'dd/MM/yyyy HH:mm' }}
                        </td>
                        <td class="py-3.5 px-4 text-right">
                          <button
                            (click)="openEditModal(stock)"
                            class="p-1 rounded-sm text-brand-cyan hover:bg-[var(--app-surface-muted)] hover:text-[var(--brand-primary-hover)] transition-colors cursor-pointer"
                          >
                            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                            </svg>
                          </button>
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            </div>
          }
        }
      </div>

      <!-- Modale de Création / Modification (Drawer ou Popin simple) -->
      @if (showModal()) {
        <div class="fixed inset-0 bg-slate-950/40 backdrop-blur-xs flex items-center justify-center p-4 z-50 transition-opacity">
          <div class="bg-[var(--app-surface)] border border-[var(--app-border)]/80 rounded-[var(--radius-brand-sm)] w-full max-w-[460px] shadow-lg overflow-hidden transition-all">
            <header class="px-5 py-4 border-b border-[var(--app-border)] flex items-center justify-between">
              <h3 class="font-display font-bold text-[var(--text-primary)]">
                {{ isEditMode() ? t('pharmacy.stocks.updateStock') : t('pharmacy.stocks.addStock') }}
              </h3>
              <button 
                (click)="closeModal()"
                class="p-1 text-[var(--text-muted)] hover:text-[var(--text-secondary)] dark:hover:text-[var(--text-primary)] rounded-[var(--radius-brand-sm)] hover:bg-[var(--app-surface-muted)] cursor-pointer"
              >
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </header>

            <form (submit)="saveStock($event)" class="p-5 space-y-4">
              <!-- Nom du médicament -->
              <div class="space-y-1.5">
                <label class="block text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ t('pharmacy.stocks.drugName') }}*</label>
                <input 
                  type="text" 
                  name="drugName"
                  [value]="formDrugName()"
                  (input)="formDrugName.set($any($event.target).value)"
                  [disabled]="isEditMode()"
                  required 
                  class="w-full min-h-[40px] px-3.5 py-2 border border-[var(--app-border)] bg-[var(--app-surface-muted)] dark:bg-[var(--app-bg)] rounded-[var(--radius-brand-sm)] text-[var(--text-primary)] text-sm focus:outline-hidden focus:border-brand-cyan disabled:opacity-50"
                  placeholder="Ex: Paracetamol 500mg"
                />
              </div>

              <!-- Nom générique -->
              <div class="space-y-1.5">
                <label class="block text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ t('pharmacy.stocks.genericName') }}</label>
                <input 
                  type="text" 
                  name="genericName"
                  [value]="formGenericName()"
                  (input)="formGenericName.set($any($event.target).value)"
                  class="w-full min-h-[40px] px-3.5 py-2 border border-[var(--app-border)] bg-[var(--app-surface-muted)] dark:bg-[var(--app-bg)] rounded-[var(--radius-brand-sm)] text-[var(--text-primary)] text-sm focus:outline-hidden focus:border-brand-cyan"
                  placeholder="Ex: Acetaminophene"
                />
              </div>

              <div class="grid grid-cols-2 gap-4">
                <!-- Unité -->
                <div class="space-y-1.5">
                  <label class="block text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ t('pharmacy.stocks.unit') }}*</label>
                  <input 
                    type="text" 
                    name="unit"
                    [value]="formUnit()"
                    (input)="formUnit.set($any($event.target).value)"
                    required
                    class="w-full min-h-[40px] px-3.5 py-2 border border-[var(--app-border)] bg-[var(--app-surface-muted)] dark:bg-[var(--app-bg)] rounded-[var(--radius-brand-sm)] text-[var(--text-primary)] text-sm focus:outline-hidden focus:border-brand-cyan"
                    placeholder="Ex: comprime"
                  />
                </div>

                <!-- Quantité -->
                <div class="space-y-1.5">
                  <label class="block text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ t('pharmacy.stocks.quantityAvailable') }}*</label>
                  <input 
                    type="number" 
                    name="quantity"
                    [value]="formQuantityAvailable()"
                    (input)="formQuantityAvailable.set(+$any($event.target).value)"
                    min="0"
                    required
                    class="w-full min-h-[40px] px-3.5 py-2 border border-[var(--app-border)] bg-[var(--app-surface-muted)] dark:bg-[var(--app-bg)] rounded-[var(--radius-brand-sm)] text-[var(--text-primary)] text-sm focus:outline-hidden focus:border-brand-cyan"
                  />
                </div>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <!-- Seuil d'alerte -->
                <div class="space-y-1.5">
                  <label class="block text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ t('pharmacy.stocks.minimumThreshold') }}*</label>
                  <input 
                    type="number" 
                    name="threshold"
                    [value]="formMinimumThreshold()"
                    (input)="formMinimumThreshold.set(+$any($event.target).value)"
                    min="0"
                    required
                    class="w-full min-h-[40px] px-3.5 py-2 border border-[var(--app-border)] bg-[var(--app-surface-muted)] dark:bg-[var(--app-bg)] rounded-[var(--radius-brand-sm)] text-[var(--text-primary)] text-sm focus:outline-hidden focus:border-brand-cyan"
                  />
                </div>

                <!-- Numéro de lot -->
                <div class="space-y-1.5">
                  <label class="block text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ t('pharmacy.stocks.batchNumber') }}</label>
                  <input 
                    type="text" 
                    name="batchNumber"
                    [value]="formBatchNumber()"
                    (input)="formBatchNumber.set($any($event.target).value)"
                    class="w-full min-h-[40px] px-3.5 py-2 border border-[var(--app-border)] bg-[var(--app-surface-muted)] dark:bg-[var(--app-bg)] rounded-[var(--radius-brand-sm)] text-[var(--text-primary)] text-sm focus:outline-hidden focus:border-brand-cyan"
                    placeholder="Ex: LOT-10293"
                  />
                </div>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <!-- Fournisseur -->
                <div class="space-y-1.5">
                  <label class="block text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ t('pharmacy.stocks.supplier') }}</label>
                  <input 
                    type="text" 
                    name="supplier"
                    [value]="formSupplier()"
                    (input)="formSupplier.set($any($event.target).value)"
                    class="w-full min-h-[40px] px-3.5 py-2 border border-[var(--app-border)] bg-[var(--app-surface-muted)] dark:bg-[var(--app-bg)] rounded-[var(--radius-brand-sm)] text-[var(--text-primary)] text-sm focus:outline-hidden focus:border-brand-cyan"
                    placeholder="Ex: MedTech S.A."
                  />
                </div>

                <!-- Expiry Date -->
                <div class="space-y-1.5">
                  <label class="block text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ t('pharmacy.stocks.expiryDate') }}</label>
                  <input 
                    type="date" 
                    name="expiryDate"
                    [value]="formExpiryDate()"
                    (input)="formExpiryDate.set($any($event.target).value)"
                    class="w-full min-h-[40px] px-3.5 py-2 border border-[var(--app-border)] bg-[var(--app-surface-muted)] dark:bg-[var(--app-bg)] rounded-[var(--radius-brand-sm)] text-[var(--text-primary)] text-sm focus:outline-hidden focus:border-brand-cyan"
                  />
                </div>
              </div>

              <footer class="pt-4 border-t border-[var(--app-border)]/80 flex items-center justify-end gap-2">
                <button
                  type="button"
                  (click)="closeModal()"
                  class="px-4 py-2 border border-[var(--app-border)] rounded-[var(--radius-brand-sm)] text-xs font-semibold text-[var(--text-secondary)] hover:bg-[var(--app-surface-muted)] dark:hover:bg-slate-800/40 cursor-pointer"
                >
                  {{ t('common.cancel') }}
                </button>
                <button
                  type="submit"
                  [disabled]="submitting()"
                  class="px-5 py-2 rounded-[var(--radius-brand-sm)] text-xs font-semibold text-white bg-brand-cyan hover:bg-[var(--brand-primary-hover)] active:bg-[var(--brand-primary-active)] transition-colors cursor-pointer disabled:opacity-50 flex items-center gap-1.5"
                >
                  @if (submitting()) {
                    <div class="w-3.5 h-3.5 rounded-full border border-[var(--app-border)] border-t-white animate-spin"></div>
                  }
                  {{ t('common.save') }}
                </button>
              </footer>
            </form>
          </div>
        </div>
      }
    </app-shell>
  `
})
export class PharmacyStocksComponent implements OnInit {
  private readonly i18n = inject(I18nService);
  private readonly pharmacyApi = inject(PharmacyApiService);
  private readonly router = inject(Router);

  t(key: string): string {
    return this.i18n.t(key);
  }

  readonly stocks = signal<DrugStockResponse[]>([]);
  readonly loading = signal<boolean>(false);
  readonly submitting = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  // Filtre
  readonly filterOnlyAlerts = signal<boolean>(false);
  
  // Modale
  readonly showModal = signal<boolean>(false);
  readonly isEditMode = signal<boolean>(false);
  readonly selectedStockId = signal<string | null>(null);

  // Formulaire signals
  readonly formDrugName = signal<string>('');
  readonly formGenericName = signal<string>('');
  readonly formUnit = signal<string>('comprime');
  readonly formQuantityAvailable = signal<number>(0);
  readonly formMinimumThreshold = signal<number>(10);
  readonly formBatchNumber = signal<string>('');
  readonly formExpiryDate = signal<string>('');
  readonly formSupplier = signal<string>('');

  ngOnInit(): void {
    this.loadStocks();
  }

  loadStocks(): void {
    this.loading.set(true);
    this.error.set(null);
    this.pharmacyApi.getStocks().subscribe({
      next: (data) => {
        this.stocks.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.t('pharmacy.stocks.loadError'));
        this.loading.set(false);
      }
    });
  }

  alertCount(): number {
    return this.stocks().filter(s => s.belowThreshold).length;
  }

  filteredStocks(): DrugStockResponse[] {
    return this.filterOnlyAlerts() 
      ? this.stocks().filter(s => s.belowThreshold)
      : this.stocks();
  }

  toggleFilter(): void {
    this.filterOnlyAlerts.update(val => !val);
  }

  isExpired(dateStr: string): boolean {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return new Date(dateStr) < today;
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }

  openAddModal(): void {
    this.isEditMode.set(false);
    this.selectedStockId.set(null);
    
    // Reset form
    this.formDrugName.set('');
    this.formGenericName.set('');
    this.formUnit.set('comprime');
    this.formQuantityAvailable.set(0);
    this.formMinimumThreshold.set(10);
    this.formBatchNumber.set('');
    this.formExpiryDate.set('');
    this.formSupplier.set('');

    this.showModal.set(true);
  }

  openEditModal(stock: DrugStockResponse): void {
    this.isEditMode.set(true);
    this.selectedStockId.set(stock.id);

    // Populate form
    this.formDrugName.set(stock.drugName);
    this.formGenericName.set(stock.genericName || '');
    this.formUnit.set(stock.unit);
    this.formQuantityAvailable.set(stock.quantityAvailable);
    this.formMinimumThreshold.set(stock.minimumThreshold);
    this.formBatchNumber.set(stock.batchNumber || '');
    this.formExpiryDate.set(stock.expiryDate || '');
    this.formSupplier.set(stock.supplier || '');

    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
  }

  saveStock(event: Event): void {
    event.preventDefault();
    if (this.submitting()) return;

    this.submitting.set(true);
    this.error.set(null);

    const request: CreateDrugStockRequest = {
      drugName: this.formDrugName().trim(),
      genericName: this.formGenericName().trim() || undefined,
      unit: this.formUnit().trim(),
      quantityAvailable: this.formQuantityAvailable(),
      minimumThreshold: this.formMinimumThreshold(),
      batchNumber: this.formBatchNumber().trim() || undefined,
      expiryDate: this.formExpiryDate() || undefined,
      supplier: this.formSupplier().trim() || undefined,
    };

    this.pharmacyApi.createOrUpdateStock(request).subscribe({
      next: () => {
        this.submitting.set(false);
        this.showModal.set(false);
        this.loadStocks();
      },
      error: () => {
        this.error.set(this.t('pharmacy.stocks.saveError'));
        this.submitting.set(false);
      }
    });
  }
}
