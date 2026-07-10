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
  templateUrl: './billing-cash-register.component.html',
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
