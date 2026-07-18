import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, Observable } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { BillingApiService } from '../../patient/billing-api.service';
import {
  BordereauStatus,
  InsuranceBordereau,
  InsuranceBordereauDetails,
} from '../../patient/insurance-bordereau.models';
import { InsuranceConvention } from '../../patient/patient.models';
import { IconComponent } from '../../shared/ui/icon.component';
import { extractApiErrorMessage } from '../../shared/utils/api-error.utils';
import { RbacApiService } from '../rbac/rbac-api.service';

type InsuranceActionMode = 'receive' | 'accept' | 'reject' | 'pay';
type PeriodFilter = '30' | '90' | '365' | 'ALL';

@Component({
  selector: 'app-billing-insurance-bordereaux',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent],
  templateUrl: './billing-insurance-bordereaux.component.html',
  styleUrl: './billing-insurance-bordereaux.component.css',
})
export class BillingInsuranceBordereauxComponent implements OnInit {
  private readonly billingApi = inject(BillingApiService);
  private readonly rbacApi = inject(RbacApiService);
  private readonly i18n = inject(I18nService);

  readonly statuses: BordereauStatus[] = [
    'DRAFT',
    'SENT',
    'RECEIVED',
    'ACCEPTED',
    'PARTIALLY_PAID',
    'SETTLED',
    'REJECTED',
    'CANCELLED',
  ];

  readonly conventions = signal<InsuranceConvention[]>([]);
  readonly bordereaux = signal<InsuranceBordereau[]>([]);
  readonly selectedBordereau = signal<InsuranceBordereauDetails | null>(null);

  readonly loading = signal(false);
  readonly loadError = signal(false);
  readonly detailLoading = signal(false);
  readonly generating = signal(false);
  readonly actionSaving = signal(false);
  readonly generationOpen = signal(false);

  readonly selectedConventionId = signal('');
  readonly startDate = signal('');
  readonly endDate = signal('');

  readonly searchQuery = signal('');
  readonly statusFilter = signal<BordereauStatus | 'ALL'>('ALL');
  readonly conventionFilter = signal('ALL');
  readonly periodFilter = signal<PeriodFilter>('90');

  readonly actionMode = signal<InsuranceActionMode | null>(null);
  readonly actionBordereau = signal<InsuranceBordereau | null>(null);
  readonly insurerReference = signal('');
  readonly acceptedAmount = signal(0);
  readonly rejectionReason = signal('');
  readonly paymentAmount = signal(0);
  readonly paymentReference = signal('');

  readonly successFeedback = signal<string | null>(null);
  readonly errorFeedback = signal<string | null>(null);

  readonly canDecide = computed(() => this.rbacApi.hasPermission('INSURANCE_BORDEREAU_SETTLE'));
  readonly canProgress = computed(() => this.rbacApi.hasPermission('INSURANCE_BORDEREAU_PROGRESS'));

  readonly canGenerate = computed(() => {
    const start = this.startDate();
    const end = this.endDate();
    return this.canProgress() && Boolean(this.selectedConventionId() && start && end && start <= end);
  });

  readonly activeCount = computed(() => this.bordereaux().filter((item) => {
    const status = this.normalizedStatus(item.status);
    return status !== 'SETTLED' && status !== 'REJECTED' && status !== 'CANCELLED';
  }).length);

  readonly claimedTotal = computed(() => this.bordereaux().reduce((sum, item) => sum + item.totalAmount, 0));
  readonly paidTotal = computed(() => this.bordereaux().reduce((sum, item) => sum + item.paidAmount, 0));
  readonly remainingTotal = computed(() => this.bordereaux().reduce((sum, item) => sum + item.remainingAmount, 0));

  readonly filteredBordereaux = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const status = this.statusFilter();
    const conventionId = this.conventionFilter();
    const period = this.periodFilter();
    const cutoff = period === 'ALL' ? null : new Date(Date.now() - Number(period) * 86_400_000);

    return this.bordereaux().filter((item) => {
      const normalized = this.normalizedStatus(item.status);
      const matchesStatus = status === 'ALL' || normalized === status;
      const matchesConvention = conventionId === 'ALL' || item.insuranceConventionId === conventionId;
      const matchesPeriod = cutoff === null || new Date(item.createdAt) >= cutoff;
      const matchesQuery = !query
        || item.bordereauNumber.toLowerCase().includes(query)
        || item.insuranceConventionName.toLowerCase().includes(query)
        || (item.insurerReference ?? '').toLowerCase().includes(query);
      return matchesStatus && matchesConvention && matchesPeriod && matchesQuery;
    });
  });

  readonly actionTitle = computed(() => {
    switch (this.actionMode()) {
      case 'receive': return this.t('billing.insurance.action.receive', 'Réceptionner le bordereau');
      case 'accept': return this.t('billing.insurance.action.accept', 'Accepter le bordereau');
      case 'reject': return this.t('billing.insurance.action.reject', 'Rejeter le bordereau');
      case 'pay': return this.t('billing.insurance.action.pay', 'Enregistrer un règlement');
      default: return '';
    }
  });

  readonly actionValid = computed(() => {
    const item = this.actionBordereau();
    if (!item) return false;
    switch (this.actionMode()) {
      case 'receive': return this.insurerReference().trim().length > 0;
      case 'accept': return this.acceptedAmount() > 0 && this.acceptedAmount() <= item.totalAmount;
      case 'reject': return this.rejectionReason().trim().length > 0;
      case 'pay': return this.paymentAmount() > 0
        && this.paymentAmount() <= item.remainingAmount
        && this.paymentReference().trim().length > 0;
      default: return false;
    }
  });

  ngOnInit(): void {
    this.loadConventions();
    this.loadBordereaux();
  }

  t(key: string, fallback: string): string {
    const translated = this.i18n.t(key);
    return translated === key ? fallback : translated;
  }

  normalizedStatus(status: BordereauStatus): BordereauStatus {
    return status === 'PAID' ? 'SETTLED' : status;
  }

  statusLabel(status: BordereauStatus): string {
    const normalized = this.normalizedStatus(status);
    const labels: Record<BordereauStatus, string> = {
      DRAFT: 'Brouillon',
      SENT: 'Envoyé',
      RECEIVED: 'Réceptionné',
      ACCEPTED: 'Accepté',
      PARTIALLY_PAID: 'Partiellement réglé',
      SETTLED: 'Soldé',
      REJECTED: 'Rejeté',
      CANCELLED: 'Annulé',
      PAID: 'Soldé',
    };
    return this.t(`billing.insurance.status.${normalized}`, labels[normalized]);
  }

  loadConventions(): void {
    this.billingApi.listConventions().subscribe({
      next: (items) => this.conventions.set(items),
      error: () => this.showError(this.t('billing.insurance.conventionError', 'Impossible de charger les conventions.')),
    });
  }

  loadBordereaux(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.billingApi.listInsuranceBordereaux().pipe(
      finalize(() => this.loading.set(false)),
    ).subscribe({
      next: (items) => this.bordereaux.set(items),
      error: () => {
        this.bordereaux.set([]);
        this.loadError.set(true);
      },
    });
  }

  generateBordereau(): void {
    if (!this.canGenerate()) return;
    this.clearFeedbacks();
    this.generating.set(true);
    this.billingApi.generateInsuranceBordereau({
      insuranceConventionId: this.selectedConventionId(),
      startDate: this.startDate(),
      endDate: this.endDate(),
    }).pipe(finalize(() => this.generating.set(false))).subscribe({
      next: (created) => {
        this.selectedConventionId.set('');
        this.startDate.set('');
        this.endDate.set('');
        this.generationOpen.set(false);
        this.showSuccess(this.t('billing.insurance.generated', 'Bordereau généré avec succès.'));
        this.loadBordereaux();
        this.viewDetails(created.id);
      },
      error: (error) => this.showError(
        extractApiErrorMessage(error)
        || this.t('billing.insurance.generateError', 'Impossible de générer le bordereau.'),
      ),
    });
  }

  viewDetails(id: string): void {
    this.detailLoading.set(true);
    this.billingApi.getInsuranceBordereauDetails(id).pipe(
      finalize(() => this.detailLoading.set(false)),
    ).subscribe({
      next: (details) => this.selectedBordereau.set(details),
      error: () => this.showError(this.t('billing.insurance.detailsError', 'Impossible de charger les détails.')),
    });
  }

  closeDetails(): void {
    this.selectedBordereau.set(null);
  }

  markAsSent(id: string): void {
    if (!this.canProgress()) return;
    this.executeSimpleAction(
      this.billingApi.sendInsuranceBordereau(id),
      this.t('billing.insurance.sent', 'Bordereau marqué comme envoyé.'),
      id,
    );
  }

  openAction(item: InsuranceBordereau, mode: InsuranceActionMode): void {
    const authorized = mode === 'receive' ? this.canProgress() : this.canDecide();
    if (!authorized) return;
    this.actionBordereau.set(item);
    this.actionMode.set(mode);
    this.insurerReference.set(item.insurerReference ?? '');
    this.acceptedAmount.set(item.acceptedAmount ?? item.totalAmount);
    this.rejectionReason.set('');
    this.paymentAmount.set(item.remainingAmount);
    this.paymentReference.set('');
  }

  closeAction(): void {
    if (this.actionSaving()) return;
    this.actionMode.set(null);
    this.actionBordereau.set(null);
  }

  submitAction(): void {
    const item = this.actionBordereau();
    const mode = this.actionMode();
    if (!item || !mode || !this.actionValid()) return;
    if (mode === 'receive' && !this.canProgress()) return;
    if (mode !== 'receive' && !this.canDecide()) return;

    let request: Observable<InsuranceBordereau>;
    switch (mode) {
      case 'receive':
        request = this.billingApi.receiveInsuranceBordereau(item.id, this.insurerReference().trim());
        break;
      case 'accept':
        request = this.billingApi.acceptInsuranceBordereau(
          item.id,
          this.acceptedAmount(),
          this.insurerReference().trim() || undefined,
        );
        break;
      case 'reject':
        request = this.billingApi.rejectInsuranceBordereau(
          item.id,
          this.rejectionReason().trim(),
          this.insurerReference().trim() || undefined,
        );
        break;
      case 'pay':
        request = this.billingApi.payInsuranceBordereau(
          item.id,
          this.paymentAmount(),
          this.paymentReference().trim(),
        );
        break;
    }

    this.actionSaving.set(true);
    request.pipe(finalize(() => this.actionSaving.set(false))).subscribe({
      next: () => {
        this.showSuccess(this.t('billing.insurance.actionSuccess', 'Mise à jour enregistrée.'));
        this.closeAction();
        this.loadBordereaux();
        this.viewDetails(item.id);
      },
      error: (error) => this.showError(
        extractApiErrorMessage(error)
        || this.t('billing.insurance.actionError', 'Impossible d’enregistrer cette action.'),
      ),
    });
  }

  private executeSimpleAction(request: Observable<InsuranceBordereau>, successMessage: string, id: string): void {
    this.clearFeedbacks();
    request.subscribe({
      next: () => {
        this.showSuccess(successMessage);
        this.loadBordereaux();
        this.viewDetails(id);
      },
      error: (error) => this.showError(
        extractApiErrorMessage(error)
        || this.t('billing.insurance.actionError', 'Impossible d’enregistrer cette action.'),
      ),
    });
  }

  private showSuccess(message: string): void {
    this.successFeedback.set(message);
    this.errorFeedback.set(null);
  }

  private showError(message: string): void {
    this.errorFeedback.set(message);
    this.successFeedback.set(null);
  }

  private clearFeedbacks(): void {
    this.successFeedback.set(null);
    this.errorFeedback.set(null);
  }
}
