import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, ViewChild, computed, inject, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, finalize, of, switchMap } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { BillingApiService } from '../../patient/billing-api.service';
import {
  CashierCollectionQueueItem,
  CashierCollectionStatus,
} from '../../patient/cashier-collection.models';
import { PaymentReceipt } from '../../patient/patient.models';
import { IconComponent } from '../../shared/ui/icon.component';
import {
  BillingPaymentForm,
  BillingPaymentInvoiceContext,
  BillingPaymentModalComponent,
} from './billing-payment-modal.component';

type QueueFilter = 'ALL' | CashierCollectionStatus;

interface LastPaymentSummary {
  item: CashierCollectionQueueItem;
  amount: number;
  method: BillingPaymentForm['method'];
  receipt: PaymentReceipt | null;
}

@Component({
  selector: 'app-billing-cashier-queue',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent, BillingPaymentModalComponent],
  templateUrl: './billing-cashier-queue.component.html',
  styleUrl: './billing-cashier-queue.component.css',
})
export class BillingCashierQueueComponent implements OnInit {
  private readonly billingApi = inject(BillingApiService);
  private readonly i18n = inject(I18nService);
  private paymentTrigger?: HTMLElement;

  @ViewChild('queueHeading')
  private queueHeading?: ElementRef<HTMLElement>;

  readonly items = signal<CashierCollectionQueueItem[]>([]);
  readonly loading = signal(false);
  readonly error = signal(false);
  readonly searchQuery = signal('');
  readonly statusFilter = signal<QueueFilter>('ALL');
  readonly selectedItem = signal<CashierCollectionQueueItem | null>(null);
  readonly savingPayment = signal(false);
  readonly paymentError = signal<string | null>(null);
  readonly lastPayment = signal<LastPaymentSummary | null>(null);
  readonly paymentRecorded = output<void>();

  readonly filteredItems = computed(() => {
    const query = this.normalize(this.searchQuery());
    const filter = this.statusFilter();

    return this.items().filter((item) => {
      const matchesStatus = filter === 'ALL' || item.collectionStatus === filter;
      if (!matchesStatus) return false;
      if (!query) return true;

      return [item.patientName, item.globalPatientNumber, item.invoiceNumber, item.phone]
        .filter((value): value is string => !!value)
        .some((value) => this.normalize(value).includes(query));
    });
  });

  readonly totalRemaining = computed(() =>
    this.items().reduce((total, item) => total + item.patientRemainingAmount, 0)
  );

  readonly partialCount = computed(() =>
    this.items().filter((item) => item.collectionStatus === 'PATIENT_PARTIALLY_PAID').length
  );

  readonly paymentInvoice = computed<BillingPaymentInvoiceContext | null>(() => {
    const item = this.selectedItem();
    if (!item) return null;
    return {
      id: item.invoiceId,
      invoiceNumber: item.invoiceNumber,
      patientShare: item.patientRemainingAmount,
    };
  });

  readonly translate = (key: string, fallback: string): string => this.t(key, fallback);

  ngOnInit(): void {
    this.loadQueue();
  }

  loadQueue(): void {
    this.loading.set(true);
    this.error.set(false);

    this.billingApi.listCashierCollectionQueue().pipe(
      finalize(() => this.loading.set(false))
    ).subscribe({
      next: (items) => this.items.set(items),
      error: () => {
        this.items.set([]);
        this.error.set(true);
      },
    });
  }

  openPayment(item: CashierCollectionQueueItem, event: Event): void {
    this.paymentTrigger = event.currentTarget as HTMLElement;
    this.paymentError.set(null);
    this.selectedItem.set(item);
  }

  closePayment(): void {
    this.selectedItem.set(null);
    this.paymentError.set(null);
    queueMicrotask(() => this.paymentTrigger?.focus());
  }

  submitPayment(form: BillingPaymentForm): void {
    const item = this.selectedItem();
    if (!item || this.savingPayment()) return;

    const reference = form.reference.trim();
    if (form.amount <= 0 || form.amount > item.patientRemainingAmount) {
      this.paymentError.set(this.t(
        'billing.cashierQueue.invalidAmount',
        'Le montant doit être positif et ne pas dépasser le reste patient.'
      ));
      return;
    }
    if (form.method !== 'CASH' && !reference) {
      this.paymentError.set(this.t(
        'billing.cashierQueue.referenceRequired',
        'La référence est obligatoire pour un chèque ou un virement.'
      ));
      return;
    }

    this.savingPayment.set(true);
    this.paymentError.set(null);

    this.billingApi.addPayment(item.invoiceId, form.amount, form.method, reference || undefined).pipe(
      switchMap((payment) => this.billingApi.getPaymentReceipt(payment.id).pipe(
        catchError(() => of(null))
      )),
      finalize(() => this.savingPayment.set(false))
    ).subscribe({
      next: (receipt) => {
        this.lastPayment.set({ item, amount: form.amount, method: form.method, receipt });
        this.selectedItem.set(null);
        this.loadQueue();
        this.paymentRecorded.emit();
        queueMicrotask(() => this.queueHeading?.nativeElement.focus());
      },
      error: (error) => {
        this.paymentError.set(
          error?.error?.error?.message
          ?? this.t('billing.cashierQueue.paymentError', 'Impossible d’enregistrer le règlement.')
        );
      },
    });
  }

  clearLastPayment(): void {
    this.lastPayment.set(null);
    queueMicrotask(() => this.queueHeading?.nativeElement.focus());
  }

  statusLabel(status: CashierCollectionStatus): string {
    return status === 'PATIENT_PARTIALLY_PAID'
      ? this.t('billing.cashierQueue.status.partial', 'Paiement partiel')
      : this.t('billing.cashierQueue.status.due', 'À encaisser');
  }

  statusClass(status: CashierCollectionStatus): string {
    return status === 'PATIENT_PARTIALLY_PAID'
      ? 'bg-amber-500/15 text-amber-700 dark:text-amber-300'
      : 'bg-orange-500/15 text-orange-700 dark:text-orange-300';
  }

  paymentMethodLabel(method: BillingPaymentForm['method']): string {
    switch (method) {
      case 'CHECK':
        return this.t('billing.paymentMethod.check', 'Chèque');
      case 'BANK_TRANSFER':
        return this.t('billing.paymentMethod.bankTransfer', 'Virement bancaire');
      default:
        return this.t('billing.paymentMethod.cash', 'Espèces');
    }
  }

  t(key: string, fallback: string): string {
    return this.i18n.t(key, fallback);
  }

  private normalize(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim()
      .toLowerCase();
  }
}
