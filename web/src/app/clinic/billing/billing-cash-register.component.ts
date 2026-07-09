import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BillingApiService } from '../../patient/billing-api.service';
import { CashSession, CashMovement, CashRegister } from '../../patient/patient.models';
import { IconComponent } from '../../shared/ui/icon.component';

@Component({
  selector: 'app-billing-cash-register',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent],
  template: `
    <div class="space-y-6">
      <!-- Alerts -->
      @if (successMessage()) {
        <div class="p-3 bg-emerald-500/10 border border-emerald-500/30 text-emerald-600 dark:text-emerald-400 rounded-sm text-xs flex justify-between items-center">
          <span>{{ successMessage() }}</span>
          <button (click)="successMessage.set(null)" class="hover:opacity-70"><app-ui-icon name="x-mark" /></button>
        </div>
      }
      @if (errorMessage()) {
        <div class="p-3 bg-red-500/10 border border-red-500/30 text-red-600 dark:text-red-400 rounded-sm text-xs flex justify-between items-center">
          <span>{{ errorMessage() }}</span>
          <button (click)="errorMessage.set(null)" class="hover:opacity-70"><app-ui-icon name="x-mark" /></button>
        </div>
      }

      <!-- Session State -->
      @if (activeSession(); as session) {
        <!-- Open Session Dashboard -->
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <!-- Session summary -->
          <div class="ui-card-subtle p-4 space-y-4 lg:col-span-1">
            <div class="border-b border-[var(--app-border)]/40 pb-2 flex justify-between items-center">
              <div>
                <h3 class="font-bold text-xs text-[var(--text-primary)] uppercase tracking-wider">Session Active</h3>
                <p class="text-[10px] text-[var(--text-muted)]">Ouverte le : {{ session.openedAt | date:'dd/MM/yyyy HH:mm' }}</p>
              </div>
              <span class="px-2 py-0.5 text-[9px] font-bold uppercase rounded-sm bg-emerald-500/15 text-emerald-600 dark:text-emerald-400">
                OUVERTE
              </span>
            </div>

            <div class="space-y-2 text-xs text-[var(--text-secondary)]">
              <div class="flex justify-between">
                <span>Fond de caisse initial :</span>
                <span class="font-bold text-[var(--text-primary)]">{{ session.openingBalance | number:'1.0-0' }} FCFA</span>
              </div>
              <div class="flex justify-between">
                <span>Total Recettes (+) :</span>
                <span class="font-bold text-emerald-600 dark:text-emerald-400">+ {{ totalRecettes() | number:'1.0-0' }} FCFA</span>
              </div>
              <div class="flex justify-between">
                <span>Total Dépenses (-) :</span>
                <span class="font-bold text-red-600 dark:text-red-400">- {{ totalDepenses() | number:'1.0-0' }} FCFA</span>
              </div>
              <div class="flex justify-between border-t border-[var(--app-border)]/40 pt-2 font-bold text-sm text-[var(--text-primary)]">
                <span>Solde Théorique :</span>
                <span>{{ soldeTheorique() | number:'1.0-0' }} FCFA</span>
              </div>
            </div>

            <div class="pt-2">
              <button (click)="openCloseModal()" class="ui-button ui-button-primary w-full justify-center">
                <app-ui-icon name="check" />
                Clôturer la Caisse
              </button>
            </div>
          </div>

          <!-- Add Operation -->
          <div class="ui-card-subtle p-4 space-y-3 lg:col-span-2">
            <h3 class="font-bold text-xs text-[var(--text-primary)] uppercase tracking-wider border-b border-[var(--app-border)]/40 pb-2">
              <app-ui-icon name="plus" />
              Consigner une Dépense / Mouvement de Caisse
            </h3>
            
            <form (submit)="submitMovement()" class="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
              <div>
                <label class="font-bold text-[var(--text-secondary)] block mb-1">Type de mouvement :</label>
                <select [(ngModel)]="movType" name="movType" class="ui-select w-full" required>
                  <option value="OUT">Dépense (OUT)</option>
                  <option value="TRANSFER_TO_BANK">Versement Banque (TRANSFER_TO_BANK)</option>
                </select>
              </div>

              <div>
                <label class="font-bold text-[var(--text-secondary)] block mb-1">Montant (FCFA) :</label>
                <input type="number" [(ngModel)]="movAmount" name="movAmount" class="ui-input w-full" placeholder="Ex: 5000" required />
              </div>

              <div class="md:col-span-2">
                <label class="font-bold text-[var(--text-secondary)] block mb-1">Description / Motif :</label>
                <input type="text" [(ngModel)]="movDescription" name="movDescription" class="ui-input w-full" placeholder="Ex: Achat papier rame" required />
              </div>

              <div>
                <label class="font-bold text-[var(--text-secondary)] block mb-1">Mode de règlement :</label>
                <select [(ngModel)]="movMethod" name="movMethod" class="ui-select w-full" required>
                  <option value="CASH">Espèces</option>
                  <option value="CHECK">Chèque</option>
                  <option value="BANK_TRANSFER">Virement</option>
                </select>
              </div>

              <div>
                <label class="font-bold text-[var(--text-secondary)] block mb-1">Référence (N° chèque, pièce...) :</label>
                <input type="text" [(ngModel)]="movReference" name="movReference" class="ui-input w-full" placeholder="Optionnel" />
              </div>

              @if (movAmount() > 100000 && movType() === 'OUT') {
                <div class="md:col-span-2 p-2 bg-amber-500/10 border border-amber-500/30 text-amber-700 dark:text-amber-400 rounded-sm text-[10px] space-y-1">
                  <p class="font-bold"><app-ui-icon name="information-circle" /> Seuil de 100 000 FCFA dépassé</p>
                  <p>Cette dépense nécessite le double visa de la DAF et du Médecin Chef.</p>
                  <label class="flex items-center gap-1.5 cursor-pointer mt-1">
                    <input type="checkbox" [(ngModel)]="movDoubleVisaApproved" name="movDoubleVisaApproved" required />
                    <span>Je confirme l'obtention du double visa signé.</span>
                  </label>
                </div>
              }

              <div class="md:col-span-2 flex justify-end pt-2">
                <button type="submit" [disabled]="savingMovement()" class="ui-button ui-button-secondary disabled:opacity-50">
                  <app-ui-icon name="plus" />
                  Enregistrer l'opération
                </button>
              </div>
            </form>
          </div>
        </div>

        <!-- Session Movements List -->
        <div class="ui-card-subtle p-4 space-y-3">
          <h3 class="font-bold text-xs text-[var(--text-primary)] uppercase tracking-wider border-b border-[var(--app-border)]/40 pb-2">
            Mouvements de la session
          </h3>
          @if (movements().length > 0) {
            <div class="overflow-x-auto">
              <table class="w-full text-left text-xs border-collapse">
                <thead>
                  <tr class="bg-[var(--app-surface-muted)] border-b border-[var(--app-border)] text-[10px] font-bold text-[var(--text-secondary)]">
                    <th class="p-2">Date/Heure</th>
                    <th class="p-2">Type</th>
                    <th class="p-2">Description</th>
                    <th class="p-2 text-right">Montant</th>
                    <th class="p-2">Règlement</th>
                    <th class="p-2">Référence</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-[var(--app-border)]/40">
                  @for (m of movements(); track m.id) {
                    <tr class="hover:bg-[var(--app-surface-muted)]/30">
                      <td class="p-2 text-[10px]">{{ m.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
                      <td class="p-2">
                        <span class="px-1.5 py-0.5 text-[9px] font-bold rounded-sm"
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
            <p class="text-center text-[10px] text-[var(--text-muted)] py-4 italic">Aucun mouvement enregistré pour cette session.</p>
          }
        </div>

      } @else {
        <!-- Closed Session State - Form to Open -->
        <div class="ui-card-subtle p-6 max-w-md mx-auto space-y-4">
          <div class="text-center space-y-1">
            <div class="inline-flex items-center justify-center p-3 bg-amber-500/15 text-amber-500 rounded-full mb-2">
              <app-ui-icon name="calculator" class="text-2xl" />
            </div>
            <h3 class="font-bold text-sm text-[var(--text-primary)]">Caisse Clôturée</h3>
            <p class="text-xs text-[var(--text-muted)]">
              Vous devez ouvrir une session de caisse avec un fond initial pour pouvoir encaisser les règlements.
            </p>
          </div>

          <form (submit)="submitOpen()" class="space-y-4 text-xs pt-2">
            <div>
              <label class="font-bold text-[var(--text-secondary)] block mb-1">Caisse d'affectation :</label>
              <select [(ngModel)]="selectedRegisterId" name="register" class="ui-select w-full">
                <option value="">Caisse par défaut (Automatique)</option>
                @for (c of registers(); track c.id) {
                  <option [value]="c.id">{{ c.name }} ({{ c.code }})</option>
                }
              </select>
            </div>

            <div>
              <label class="font-bold text-[var(--text-secondary)] block mb-1">Fond de caisse d'ouverture (FCFA) :</label>
              <input type="number" [(ngModel)]="openBalance" name="openBalance" class="ui-input w-full" placeholder="Ex: 50000" required />
            </div>

            <button type="submit" [disabled]="savingOpen()" class="ui-button ui-button-primary w-full justify-center disabled:opacity-50">
              <app-ui-icon name="check" />
              Ouvrir la session de caisse
            </button>
          </form>
        </div>
      }
    </div>

    <!-- Close Session Modal -->
    @if (showCloseModal()) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs">
        <div class="bg-[var(--app-surface)] border border-[var(--app-border)] rounded-sm max-w-md w-full p-5 space-y-4 shadow-xl">
          <div class="flex justify-between items-center border-b border-[var(--app-border)]/40 pb-2">
            <h3 class="font-bold text-xs text-[var(--text-primary)] uppercase tracking-wider">Clôturer la Session</h3>
            <button (click)="openCloseModal()" class="hover:opacity-70 text-[var(--text-muted)]"><app-ui-icon name="x-mark" /></button>
          </div>

          <div class="space-y-2 text-xs">
            <p class="text-[var(--text-secondary)]">
              Veuillez compter la caisse physique et déclarer le montant total d'espèces et de pièces en votre possession.
            </p>
            <div class="p-2 bg-[var(--app-surface-muted)] rounded-sm flex justify-between font-bold">
              <span>Solde théorique attendu :</span>
              <span>{{ soldeTheorique() | number:'1.0-0' }} FCFA</span>
            </div>
          </div>

          <form (submit)="submitClose()" class="space-y-4 text-xs">
            <div>
              <label class="font-bold text-[var(--text-secondary)] block mb-1">Montant physique constaté (FCFA) :</label>
              <input type="number" [(ngModel)]="declaredBalance" name="declaredBalance" class="ui-input w-full" placeholder="Ex: 45000" required />
            </div>

            @if (discrepancy() !== 0) {
              <div class="p-2 bg-amber-500/10 border border-amber-500/30 text-amber-700 dark:text-amber-400 rounded-sm text-[10px] space-y-1">
                <p class="font-bold"><app-ui-icon name="information-circle" /> Écart de caisse détecté : {{ discrepancy() | number:'1.0-0' }} FCFA</p>
                <label class="font-bold block mt-1">Justification / Motif de l'écart :</label>
                <input type="text" [(ngModel)]="discrepancyReason" name="discrepancyReason" class="ui-input w-full bg-[var(--app-surface)] text-xs mt-0.5" placeholder="Ex: Erreur rendu monnaie ticket 14" required />
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
  `
})
export class BillingCashRegisterComponent implements OnInit {
  private readonly billingApi = inject(BillingApiService);

  activeSession = signal<CashSession | null>(null);
  registers = signal<CashRegister[]>([]);
  movements = signal<CashMovement[]>([]);

  // Open form
  selectedRegisterId = signal<string>('');
  openBalance = signal<number>(0);
  savingOpen = signal(false);

  // Movement form
  movType = signal<'OUT' | 'TRANSFER_TO_BANK'>('OUT');
  movAmount = signal<number>(0);
  movDescription = signal<string>('');
  movMethod = signal<'CASH' | 'CHECK' | 'BANK_TRANSFER'>('CASH');
  movReference = signal<string>('');
  movDoubleVisaApproved = signal<boolean>(false);
  savingMovement = signal(false);

  // Close form
  showCloseModal = signal(false);
  declaredBalance = signal<number>(0);
  discrepancyReason = signal<string>('');
  savingClose = signal(false);

  // Alerts
  successMessage = signal<string | null>(null);
  errorMessage = signal<string | null>(null);

  // Computeds
  totalRecettes = computed(() => {
    return this.movements().filter(m => m.movementType === 'IN').reduce((acc, m) => acc + m.amount, 0);
  });

  totalDepenses = computed(() => {
    return this.movements().filter(m => m.movementType === 'OUT' || m.movementType === 'TRANSFER_TO_BANK').reduce((acc, m) => acc + m.amount, 0);
  });

  soldeTheorique = computed(() => {
    const session = this.activeSession();
    if (!session) return 0;
    return session.openingBalance + this.totalRecettes() - this.totalDepenses();
  });

  discrepancy = computed(() => {
    return this.declaredBalance() - this.soldeTheorique();
  });

  ngOnInit() {
    this.loadActiveSession();
    this.loadRegisters();
  }

  loadActiveSession() {
    this.billingApi.getActiveCashSession().subscribe({
      next: (session) => {
        if (session) {
          this.activeSession.set(session);
          this.loadSessionMovements(session.id);
        } else {
          this.activeSession.set(null);
          this.movements.set([]);
        }
      },
      error: () => {
        this.activeSession.set(null);
        this.movements.set([]);
      }
    });
  }

  loadRegisters() {
    this.billingApi.listCashRegisters().subscribe({
      next: (registers) => {
        this.registers.set(registers);
      }
    });
  }

  loadSessionMovements(sessionId: string) {
    this.billingApi.getSessionMovements(sessionId).subscribe({
      next: (movements) => this.movements.set(movements)
    });
  }

  submitOpen() {
    if (this.openBalance() < 0) return;
    this.savingOpen.set(true);
    const regId = this.selectedRegisterId() ? this.selectedRegisterId() : null;
    this.billingApi.openCashSession(regId, this.openBalance()).subscribe({
      next: (session) => {
        this.savingOpen.set(false);
        this.activeSession.set(session);
        this.successMessage.set("Session de caisse ouverte avec succès.");
        this.loadSessionMovements(session.id);
        this.errorMessage.set(null);
      },
      error: (err) => {
        this.savingOpen.set(false);
        this.errorMessage.set(err.error?.error?.message || "Erreur lors de l'ouverture de la session.");
      }
    });
  }

  submitMovement() {
    if (this.movAmount() <= 0 || !this.movDescription()) return;
    
    // Protection double visa
    if (this.movType() === 'OUT' && this.movAmount() > 100000 && !this.movDoubleVisaApproved()) {
      this.errorMessage.set("Double visa obligatoire pour les dépenses supérieures à 100 000 FCFA.");
      return;
    }

    this.savingMovement.set(true);
    this.billingApi.addCashMovement({
      movementType: this.movType(),
      amount: this.movAmount(),
      description: this.movDescription(),
      paymentMethod: this.movMethod(),
      referenceNumber: this.movReference(),
      doubleVisaApproved: this.movDoubleVisaApproved()
    }).subscribe({
      next: (m) => {
        this.savingMovement.set(false);
        this.movements.update(list => [...list, m]);
        this.successMessage.set("Mouvement de caisse consigné avec succès.");
        this.errorMessage.set(null);
        // Reset form
        this.movAmount.set(0);
        this.movDescription.set('');
        this.movReference.set('');
        this.movDoubleVisaApproved.set(false);
      },
      error: (err) => {
        this.savingMovement.set(false);
        this.errorMessage.set(err.error?.error?.message || "Erreur lors de l'enregistrement du mouvement.");
      }
    });
  }

  openCloseModal() {
    this.declaredBalance.set(this.soldeTheorique());
    this.discrepancyReason.set('');
    this.showCloseModal.update(v => !v);
  }

  submitClose() {
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
        this.successMessage.set("Caisse clôturée avec succès. L'état théorique et l'écart ont été enregistrés.");
        this.errorMessage.set(null);
      },
      error: (err) => {
        this.savingClose.set(false);
        this.errorMessage.set(err.error?.error?.message || "Erreur lors de la clôture de la caisse.");
      }
    });
  }
}
