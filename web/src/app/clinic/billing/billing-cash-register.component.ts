import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../core/i18n/i18n.service';
import { BillingApiService } from '../../patient/billing-api.service';
import { CashMovement, CashRegister, CashSession, CashSessionSummary } from '../../patient/patient.models';
import { IconComponent } from '../../shared/ui/icon.component';
import { BillingCashSessionHistoryComponent } from './billing-cash-session-history.component';
import { BillingCashierQueueComponent } from './billing-cashier-queue.component';

@Component({
  selector: 'app-billing-cash-register',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    IconComponent,
    BillingCashierQueueComponent,
    BillingCashSessionHistoryComponent,
  ],
  template: `
    <div class="space-y-6">
      @if (successMessage()) {
        <div class="flex items-center justify-between rounded-[var(--radius-brand-sm)] border border-emerald-500/30 bg-emerald-500/10 p-3 text-xs text-emerald-600 dark:text-emerald-400">
          <span>{{ successMessage() }}</span>
          <button type="button" (click)="successMessage.set(null)" [attr.aria-label]="t('common.aria.close', 'Fermer')" class="hover:opacity-70"><app-ui-icon name="x-mark" /></button>
        </div>
      }
      @if (errorMessage()) {
        <div class="flex items-center justify-between rounded-[var(--radius-brand-sm)] border border-red-500/30 bg-red-500/10 p-3 text-xs text-red-600 dark:text-red-400" role="alert">
          <span>{{ errorMessage() }}</span>
          <button type="button" (click)="errorMessage.set(null)" [attr.aria-label]="t('common.aria.close', 'Fermer')" class="hover:opacity-70"><app-ui-icon name="x-mark" /></button>
        </div>
      }

      @if (activeSession(); as session) {
        <app-billing-cashier-queue></app-billing-cashier-queue>

        <div class="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <div class="ui-card-subtle space-y-4 p-4 lg:col-span-1">
            <div class="flex items-center justify-between border-b border-[var(--app-border)]/40 pb-2">
              <div>
                <h3 class="text-xs font-bold uppercase tracking-wider text-[var(--text-primary)]">Session Active</h3>
                <p class="text-[10px] text-[var(--text-muted)]">Ouverte le : {{ session.openedAt | date:'dd/MM/yyyy HH:mm' }}</p>
              </div>
              <span class="rounded-[var(--radius-brand-sm)] bg-emerald-500/15 px-2 py-0.5 text-[9px] font-bold uppercase text-emerald-600 dark:text-emerald-400">
                OUVERTE
              </span>
            </div>

            <div class="space-y-2 text-xs text-[var(--text-secondary)]">
              <div class="flex justify-between">
                <span>Fond de caisse initial :</span>
                <span class="font-bold text-[var(--text-primary)]">{{ session.openingBalance | number:'1.0-0' }} FCFA</span>
              </div>
              <div class="flex justify-between">
                <span>Encaissements espèces :</span>
                <span class="font-bold text-emerald-600 dark:text-emerald-400">+ {{ cashReceipts() | number:'1.0-0' }} FCFA</span>
              </div>
              <div class="flex justify-between">
                <span>Dépenses espèces :</span>
                <span class="font-bold text-red-600 dark:text-red-400">- {{ cashExpenses() | number:'1.0-0' }} FCFA</span>
              </div>
              <div class="flex justify-between">
                <span>Versements banque :</span>
                <span class="font-bold text-blue-600 dark:text-blue-400">- {{ bankDeposits() | number:'1.0-0' }} FCFA</span>
              </div>
              <div class="flex justify-between border-t border-[var(--app-border)]/40 pt-2 text-sm font-bold text-[var(--text-primary)]">
                <span>Solde Théorique :</span>
                <span>{{ soldeTheorique() | number:'1.0-0' }} FCFA</span>
              </div>
            </div>

            <div class="space-y-1.5 border-t border-[var(--app-border)]/40 pt-3 text-[10px] text-[var(--text-secondary)]">
              <p class="font-bold uppercase tracking-wider text-[var(--text-muted)]">Encaissements hors espèces</p>
              <div class="flex justify-between"><span>Chèques reçus</span><span class="font-semibold">{{ chequeReceipts() | number:'1.0-0' }} FCFA</span></div>
              <div class="flex justify-between"><span>Virements reçus</span><span class="font-semibold">{{ transferReceipts() | number:'1.0-0' }} FCFA</span></div>
              <p class="pt-1 text-[var(--text-muted)]">Ces montants sont tracés, mais ne sont pas inclus dans le comptage physique.</p>
            </div>

            <div class="pt-2">
              <button type="button" (click)="openCloseModal()" class="ui-button ui-button-primary w-full justify-center">
                <app-ui-icon name="check" />
                Clôturer la Caisse
              </button>
            </div>
          </div>

          <div class="ui-card-subtle space-y-3 p-4 lg:col-span-2">
            <h3 class="border-b border-[var(--app-border)]/40 pb-2 text-xs font-bold uppercase tracking-wider text-[var(--text-primary)]">
              <app-ui-icon name="plus" />
              Consigner une Dépense / Mouvement de Caisse
            </h3>

            <form (submit)="submitMovement()" class="grid grid-cols-1 gap-3 text-xs md:grid-cols-2">
              <div>
                <label class="mb-1 block font-bold text-[var(--text-secondary)]">Type de mouvement :</label>
                <select [(ngModel)]="movType" (ngModelChange)="onMovementTypeChange()" name="movType" class="ui-select w-full" required>
                  <option value="OUT">Dépense (OUT)</option>
                  <option value="TRANSFER_TO_BANK">Versement Banque (TRANSFER_TO_BANK)</option>
                </select>
              </div>

              <div>
                <label class="mb-1 block font-bold text-[var(--text-secondary)]">Montant (FCFA) :</label>
                <input type="number" [(ngModel)]="movAmount" name="movAmount" class="ui-input w-full" placeholder="Ex: 5000" required />
              </div>

              <div class="md:col-span-2">
                <label class="mb-1 block font-bold text-[var(--text-secondary)]">Description / Motif :</label>
                <input type="text" [(ngModel)]="movDescription" name="movDescription" class="ui-input w-full" placeholder="Ex: Achat papier rame" required />
              </div>

              <div>
                <label class="mb-1 block font-bold text-[var(--text-secondary)]">{{ movType() === 'TRANSFER_TO_BANK' ? 'Origine du versement :' : 'Mode de règlement :' }}</label>
                <select [(ngModel)]="movMethod" name="movMethod" class="ui-select w-full" required [disabled]="movType() === 'TRANSFER_TO_BANK'">
                  <option value="CASH">Espèces</option>
                  <option value="CHECK">Chèque</option>
                  <option value="BANK_TRANSFER">Virement</option>
                </select>
              </div>

              <div>
                <label class="mb-1 block font-bold text-[var(--text-secondary)]">{{ movType() === 'TRANSFER_TO_BANK' ? 'Bordereau de dépôt :' : 'Référence (N° chèque, pièce...) :' }}</label>
                <input type="text" [(ngModel)]="movReference" name="movReference" class="ui-input w-full" [placeholder]="movType() === 'TRANSFER_TO_BANK' ? 'Ex: BORD-20260709-001' : 'Optionnel'" [required]="movType() === 'TRANSFER_TO_BANK'" />
              </div>

              @if (movType() === 'TRANSFER_TO_BANK') {
                <p class="rounded-[var(--radius-brand-sm)] border border-blue-500/30 bg-blue-500/10 p-2 text-[10px] text-blue-700 md:col-span-2 dark:text-blue-300">
                  Ce versement diminue le solde espèces attendu à la clôture. Conservez le bordereau de dépôt avec la session.
                </p>
              }

              @if (movAmount() > 100000 && movType() === 'OUT') {
                <div class="space-y-1 rounded-[var(--radius-brand-sm)] border border-amber-500/30 bg-amber-500/10 p-2 text-[10px] text-amber-700 md:col-span-2 dark:text-amber-400">
                  <p class="font-bold"><app-ui-icon name="information-circle" /> Seuil de 100 000 FCFA dépassé</p>
                  <p>Cette dépense nécessite le double visa de la DAF et du Médecin Chef.</p>
                  <label class="mt-1 flex cursor-pointer items-center gap-1.5">
                    <input type="checkbox" [(ngModel)]="movDoubleVisaApproved" name="movDoubleVisaApproved" required />
                    <span>Je confirme l'obtention du double visa signé.</span>
                  </label>
                </div>
              }

              <div class="flex justify-end pt-2 md:col-span-2">
                <button type="submit" [disabled]="savingMovement()" class="ui-button ui-button-secondary disabled:opacity-50">
                  <app-ui-icon name="plus" />
                  Enregistrer l'opération
                </button>
              </div>
            </form>
          </div>
        </div>

        <div class="ui-card-subtle space-y-3 p-4">
          <h3 class="border-b border-[var(--app-border)]/40 pb-2 text-xs font-bold uppercase tracking-wider text-[var(--text-primary)]">
            Mouvements de la session
          </h3>
          @if (movements().length > 0) {
            <div class="overflow-x-auto">
              <table class="w-full border-collapse text-left text-xs">
                <thead>
                  <tr class="border-b border-[var(--app-border)] bg-[var(--app-surface-muted)] text-[10px] font-bold text-[var(--text-secondary)]">
                    <th class="p-2" scope="col">Date/Heure</th>
                    <th class="p-2" scope="col">Type</th>
                    <th class="p-2" scope="col">Description</th>
                    <th class="p-2 text-right" scope="col">Montant</th>
                    <th class="p-2" scope="col">Règlement</th>
                    <th class="p-2" scope="col">Référence</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-[var(--app-border)]/40">
                  @for (m of movements(); track m.id) {
                    <tr class="hover:bg-[var(--app-surface-muted)]/30">
                      <td class="p-2 text-[10px]">{{ m.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
                      <td class="p-2">
                        <span class="rounded-[var(--radius-brand-sm)] px-1.5 py-0.5 text-[9px] font-bold"
                              [class.bg-emerald-500\/10]="m.movementType === 'IN'"
                              [class.text-emerald-600]="m.movementType === 'IN'"
                              [class.dark:text-emerald-400]="m.movementType === 'IN'"
                              [class.bg-red-500\/10]="m.movementType === 'OUT'"
                              [class.text-red-600]="m.movementType === 'OUT'"
                              [class.dark:text-red-400]="m.movementType === 'OUT'"
                              [class.bg-blue-500\/10]="m.movementType === 'TRANSFER_TO_BANK'"
                              [class.text-blue-600]="m.movementType === 'TRANSFER_TO_BANK'"
                              [class.dark:text-blue-400]="m.movementType === 'TRANSFER_TO_BANK'">
                          {{ m.movementType }}
                        </span>
                      </td>
                      <td class="p-2">{{ m.description }}</td>
                      <td class="p-2 text-right font-bold"
                          [class.text-emerald-600]="m.movementType === 'IN'"
                          [class.text-red-600]="m.movementType === 'OUT' || m.movementType === 'TRANSFER_TO_BANK'">
                        {{ m.movementType === 'IN' ? '+' : '-' }} {{ m.amount | number:'1.0-0' }} FCFA
                      </td>
                      <td class="p-2 text-[10px]">{{ m.paymentMethod }}</td>
                      <td class="p-2 text-[10px]">{{ m.referenceNumber || '-' }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          } @else {
            <p class="py-4 text-center text-[10px] italic text-[var(--text-muted)]">Aucun mouvement enregistré pour cette session.</p>
          }
        </div>
      } @else {
        <div class="ui-card-subtle mx-auto max-w-md space-y-4 p-6">
          <div class="space-y-1 text-center">
            <div class="mb-2 inline-flex items-center justify-center rounded-full bg-amber-500/15 p-3 text-amber-500">
              <app-ui-icon name="calculator" class="text-2xl" />
            </div>
            <h3 class="text-sm font-bold text-[var(--text-primary)]">Caisse Clôturée</h3>
            <p class="text-xs text-[var(--text-muted)]">
              Vous devez ouvrir une session de caisse avec un fond initial pour pouvoir encaisser les règlements.
            </p>
          </div>

          <form (submit)="submitOpen()" class="space-y-4 pt-2 text-xs">
            <div>
              <label class="mb-1 block font-bold text-[var(--text-secondary)]">Caisse d'affectation :</label>
              <select [(ngModel)]="selectedRegisterId" name="register" class="ui-select w-full">
                <option value="">Caisse par défaut (Automatique)</option>
                @for (c of registers(); track c.id) {
                  <option [value]="c.id">{{ c.name }} ({{ c.code }})</option>
                }
              </select>
            </div>

            <div>
              <label class="mb-1 block font-bold text-[var(--text-secondary)]">Fond de caisse d'ouverture (FCFA) :</label>
              <input type="number" [(ngModel)]="openBalance" name="openBalance" class="ui-input w-full" placeholder="Ex: 50000" required />
            </div>

            <button type="submit" [disabled]="savingOpen()" class="ui-button ui-button-primary w-full justify-center disabled:opacity-50">
              <app-ui-icon name="check" />
              Ouvrir la session de caisse
            </button>
          </form>
        </div>
      }

      <app-billing-cash-session-history></app-billing-cash-session-history>
    </div>

    @if (showCloseModal()) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-xs">
        <div class="w-full max-w-md space-y-4 rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface)] p-5 shadow-xl" role="dialog" aria-modal="true" aria-labelledby="cash-close-title">
          <div class="flex items-center justify-between border-b border-[var(--app-border)]/40 pb-2">
            <h3 id="cash-close-title" class="text-xs font-bold uppercase tracking-wider text-[var(--text-primary)]">Clôturer la Session</h3>
            <button type="button" (click)="openCloseModal()" [attr.aria-label]="t('common.aria.close', 'Fermer')" class="text-[var(--text-muted)] hover:opacity-70"><app-ui-icon name="x-mark" /></button>
          </div>

          <div class="space-y-2 text-xs">
            <p class="text-[var(--text-secondary)]">
              Veuillez compter la caisse physique et déclarer le montant total d'espèces et de pièces en votre possession.
            </p>
            <div class="flex justify-between rounded-[var(--radius-brand-sm)] bg-[var(--app-surface-muted)] p-2 font-bold">
              <span>Solde théorique attendu :</span>
              <span>{{ soldeTheorique() | number:'1.0-0' }} FCFA</span>
            </div>
          </div>

          <form (submit)="submitClose()" class="space-y-4 text-xs">
            <div>
              <label class="mb-1 block font-bold text-[var(--text-secondary)]">Montant physique constaté (FCFA) :</label>
              <input type="number" [(ngModel)]="declaredBalance" name="declaredBalance" class="ui-input w-full" placeholder="Ex: 45000" required />
            </div>

            @if (discrepancy() !== 0) {
              <div class="space-y-1 rounded-[var(--radius-brand-sm)] border border-amber-500/30 bg-amber-500/10 p-2 text-[10px] text-amber-700 dark:text-amber-400">
                <p class="font-bold"><app-ui-icon name="information-circle" /> Écart de caisse détecté : {{ discrepancy() | number:'1.0-0' }} FCFA</p>
                <label class="mt-1 block font-bold">Justification / Motif de l'écart :</label>
                <input type="text" [(ngModel)]="discrepancyReason" name="discrepancyReason" class="ui-input mt-0.5 w-full bg-[var(--app-surface)] text-xs" placeholder="Ex: Erreur rendu monnaie ticket 14" required />
              </div>
            }

            <div class="flex justify-end gap-2 pt-2">
              <button type="button" (click)="openCloseModal()" class="ui-button ui-button-secondary">Annuler</button>
              <button type="submit" [disabled]="savingClose()" class="ui-button ui-button-primary disabled:opacity-50">
                Confirmer la clôture
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
})
export class BillingCashRegisterComponent implements OnInit {
  private readonly billingApi = inject(BillingApiService);
  private readonly i18n = inject(I18nService);

  t(key: string, defaultValue: string): string {
    return this.i18n.t(key, defaultValue);
  }

  activeSession = signal<CashSession | null>(null);
  registers = signal<CashRegister[]>([]);
  movements = signal<CashMovement[]>([]);
  summary = signal<CashSessionSummary | null>(null);

  selectedRegisterId = signal<string>('');
  openBalance = signal<number>(0);
  savingOpen = signal(false);

  movType = signal<'OUT' | 'TRANSFER_TO_BANK'>('OUT');
  movAmount = signal<number>(0);
  movDescription = signal<string>('');
  movMethod = signal<'CASH' | 'CHECK' | 'BANK_TRANSFER'>('CASH');
  movReference = signal<string>('');
  movDoubleVisaApproved = signal<boolean>(false);
  savingMovement = signal(false);

  showCloseModal = signal(false);
  declaredBalance = signal<number>(0);
  discrepancyReason = signal<string>('');
  savingClose = signal(false);

  successMessage = signal<string | null>(null);
  errorMessage = signal<string | null>(null);

  cashReceipts = computed(() => this.summary()?.cashReceipts
    ?? this.movements().filter(m => m.movementType === 'IN' && m.paymentMethod === 'CASH').reduce((acc, m) => acc + m.amount, 0));

  chequeReceipts = computed(() => this.summary()?.chequeReceipts
    ?? this.movements().filter(m => m.movementType === 'IN' && m.paymentMethod === 'CHECK').reduce((acc, m) => acc + m.amount, 0));

  transferReceipts = computed(() => this.summary()?.transferReceipts
    ?? this.movements().filter(m => m.movementType === 'IN' && m.paymentMethod === 'BANK_TRANSFER').reduce((acc, m) => acc + m.amount, 0));

  cashExpenses = computed(() => this.summary()?.cashExpenses
    ?? this.movements().filter(m => m.movementType === 'OUT' && m.paymentMethod === 'CASH').reduce((acc, m) => acc + m.amount, 0));

  bankDeposits = computed(() => this.summary()?.bankDeposits
    ?? this.movements().filter(m => m.movementType === 'TRANSFER_TO_BANK').reduce((acc, m) => acc + m.amount, 0));

  soldeTheorique = computed(() => {
    const session = this.activeSession();
    if (!session) return 0;
    return this.summary()?.expectedCash ?? session.openingBalance + this.cashReceipts() - this.cashExpenses() - this.bankDeposits();
  });

  discrepancy = computed(() => this.declaredBalance() - this.soldeTheorique());

  ngOnInit(): void {
    this.loadActiveSession();
    this.loadRegisters();
  }

  loadActiveSession(): void {
    this.billingApi.getActiveCashSession().subscribe({
      next: (session) => {
        if (session) {
          this.activeSession.set(session);
          this.loadSessionMovements(session.id);
          this.loadSessionSummary();
        } else {
          this.activeSession.set(null);
          this.movements.set([]);
          this.summary.set(null);
        }
      },
      error: () => {
        this.activeSession.set(null);
        this.movements.set([]);
        this.summary.set(null);
      },
    });
  }

  loadRegisters(): void {
    this.billingApi.listCashRegisters().subscribe({
      next: (registers) => this.registers.set(registers),
    });
  }

  loadSessionMovements(sessionId: string): void {
    this.billingApi.getSessionMovements(sessionId).subscribe({
      next: (movements) => this.movements.set(movements),
    });
  }

  loadSessionSummary(): void {
    this.billingApi.getActiveCashSessionSummary().subscribe({
      next: (summary) => this.summary.set(summary),
      error: () => this.summary.set(null),
    });
  }

  submitOpen(): void {
    if (this.openBalance() < 0) return;
    this.savingOpen.set(true);
    const registerId = this.selectedRegisterId() || null;
    this.billingApi.openCashSession(registerId, this.openBalance()).subscribe({
      next: (session) => {
        this.savingOpen.set(false);
        this.activeSession.set(session);
        this.successMessage.set('Session de caisse ouverte avec succès.');
        this.loadSessionMovements(session.id);
        this.loadSessionSummary();
        this.errorMessage.set(null);
      },
      error: (error) => {
        this.savingOpen.set(false);
        this.errorMessage.set(error.error?.error?.message || "Erreur lors de l'ouverture de la session.");
      },
    });
  }

  submitMovement(): void {
    if (this.movAmount() <= 0 || !this.movDescription()) return;

    if (this.movType() === 'OUT' && this.movAmount() > 100000 && !this.movDoubleVisaApproved()) {
      this.errorMessage.set('Double visa obligatoire pour les dépenses supérieures à 100 000 FCFA.');
      return;
    }
    if (this.movType() === 'TRANSFER_TO_BANK' && !this.movReference().trim()) {
      this.errorMessage.set('La référence du bordereau de dépôt est obligatoire.');
      return;
    }

    this.savingMovement.set(true);
    this.billingApi.addCashMovement({
      movementType: this.movType(),
      amount: this.movAmount(),
      description: this.movDescription(),
      paymentMethod: this.movMethod(),
      referenceNumber: this.movReference(),
      doubleVisaApproved: this.movDoubleVisaApproved(),
    }).subscribe({
      next: (movement) => {
        this.savingMovement.set(false);
        this.movements.update((list) => [...list, movement]);
        this.loadSessionSummary();
        this.successMessage.set('Mouvement de caisse consigné avec succès.');
        this.errorMessage.set(null);
        this.movAmount.set(0);
        this.movDescription.set('');
        this.movReference.set('');
        this.movDoubleVisaApproved.set(false);
      },
      error: (error) => {
        this.savingMovement.set(false);
        this.errorMessage.set(error.error?.error?.message || "Erreur lors de l'enregistrement du mouvement.");
      },
    });
  }

  onMovementTypeChange(): void {
    if (this.movType() === 'TRANSFER_TO_BANK') {
      this.movMethod.set('CASH');
    }
  }

  openCloseModal(): void {
    this.declaredBalance.set(this.soldeTheorique());
    this.discrepancyReason.set('');
    this.showCloseModal.update((value) => !value);
  }

  submitClose(): void {
    if (this.discrepancy() !== 0 && !this.discrepancyReason().trim()) {
      this.errorMessage.set("Vous devez justifier l'écart de caisse.");
      return;
    }
    this.savingClose.set(true);
    this.billingApi.closeCashSession(this.declaredBalance(), this.discrepancyReason()).subscribe({
      next: () => {
        this.savingClose.set(false);
        this.showCloseModal.set(false);
        this.activeSession.set(null);
        this.movements.set([]);
        this.summary.set(null);
        this.successMessage.set("Caisse clôturée avec succès. L'état théorique et l'écart ont été enregistrés.");
        this.errorMessage.set(null);
      },
      error: (error) => {
        this.savingClose.set(false);
        this.errorMessage.set(error.error?.error?.message || 'Erreur lors de la clôture de la caisse.');
      },
    });
  }
}
