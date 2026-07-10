import { CommonModule } from '@angular/common';
import {
  Component,
  ElementRef,
  NgZone,
  OnInit,
  ViewChild,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { catchError, finalize, forkJoin, of, take } from 'rxjs';
import { AuthTokenStorageService } from '../../auth/auth-token-storage.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { BillingApiService } from '../../patient/billing-api.service';
import { PatientApiService } from '../../patient/patient-api.service';
import {
  InsuranceConvention,
  Invoice,
  InvoiceItem,
  InvoiceSettlementSummary,
  Patient,
  TariffGrid,
} from '../../patient/patient.models';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { IconComponent } from '../../shared/ui/icon.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { VisitApiService } from '../../visit/visit-api.service';
import { Visit } from '../../visit/visit.models';
import { BillingAdminTabsComponent } from './billing-admin-tabs.component';
import { BillingCashRegisterComponent } from './billing-cash-register.component';
import { BillingDafDashboardComponent } from './billing-daf-dashboard.component';
import { BillingEstimatesComponent } from './billing-estimates.component';
import { BillingInsuranceBordereauxComponent } from './billing-insurance-bordereaux.component';
import { BillingInvoiceHistoryComponent } from './billing-invoice-history.component';
import { BillingPaymentForm, BillingPaymentModalComponent } from './billing-payment-modal.component';
import { BillingReceivablesComponent } from './billing-receivables.component';

@Component({
  selector: 'app-billing-management-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    IconComponent,
    AppShellComponent,
    PageHeaderComponent,
    BillingAdminTabsComponent,
    BillingInvoiceHistoryComponent,
    BillingPaymentModalComponent,
    BillingEstimatesComponent,
    BillingCashRegisterComponent,
    BillingReceivablesComponent,
    BillingInsuranceBordereauxComponent,
    BillingDafDashboardComponent,
  ],
  templateUrl: './billing-management-page.component.html',
  styles: [`
    ::-webkit-scrollbar { width: 4px; height: 4px; }
    ::-webkit-scrollbar-thumb { background: var(--app-border); border-radius: 2px; }
  `]
})
export class BillingManagementPageComponent implements OnInit {
  private readonly billingApi = inject(BillingApiService);
  private readonly patientApi = inject(PatientApiService);
  private readonly visitApi = inject(VisitApiService);
  private readonly i18n = inject(I18nService);
  private readonly tokenStorage = inject(AuthTokenStorageService);
  private readonly route = inject(ActivatedRoute);
  private readonly zone = inject(NgZone);

  @ViewChild('invoiceDetailsPanel')
  private invoiceDetailsPanel?: ElementRef<HTMLElement>;

  @ViewChild(BillingInvoiceHistoryComponent)
  private invoiceHistory?: BillingInvoiceHistoryComponent;

  activeTab = signal<'facturation' | 'caisse' | 'creances' | 'bordereaux' | 'conventions' | 'tariffs' | 'daf'>('facturation');

  hasRole(allowedRoles: string[] | string): boolean {
    const role = this.tokenStorage.session()?.role;
    if (!role) return false;
    const roles = role.split(',').map((r) => r.trim());
    return Array.isArray(allowedRoles)
      ? roles.some((r) => allowedRoles.includes(r))
      : roles.includes(allowedRoles);
  }

  readonly isDafOrAdmin = computed(() => this.hasRole(['DAF', 'ADMIN_CLINIQUE']));
  readonly isAdminOnly = computed(() => this.hasRole(['ADMIN_CLINIQUE']));
  readonly canAccessCaisse = computed(() => this.hasRole(['AGENT_ACCUEIL', 'ADMIN_CLINIQUE']));

  // Search & Patient
  searchQuery = '';
  searched = signal(false);
  patients = signal<Patient[]>([]);
  selectedPatient = signal<Patient | null>(null);
  patientVisits = signal<Visit[]>([]);
  selectedVisitId = signal<string>('');

  // Invoice Form
  conventions = signal<InsuranceConvention[]>([]);
  tariffs = signal<TariffGrid[]>([]);
  selectedConventionId = signal<string>('');
  invoiceItems = signal<InvoiceItem[]>([]);
  savingInvoice = signal(false);

  // Totals
  calculatedTotal = computed(() =>
    this.invoiceItems().reduce((acc, item) => {
      return acc + ((item.unitPrice || 0) * (item.quantity || 0) * (item.coefficient || 1.0));
    }, 0)
  );

  calculatedInsuranceShare = computed(() => {
    const convention = this.conventions().find(c => c.id === this.selectedConventionId());
    return convention ? this.calculatedTotal() * convention.coveragePercentage : 0;
  });

  calculatedPatientShare = computed(() => this.calculatedTotal() - this.calculatedInsuranceShare());

  isItemInvalid(item: InvoiceItem): boolean {
    return (item.unitPrice ?? 0) <= 0 || (item.quantity ?? 0) < 1 || (item.coefficient ?? 1) <= 0;
  }

  readonly hasInvalidItems = computed(() => this.invoiceItems().some(item => this.isItemInvalid(item)));

  // History
  invoices = signal<Invoice[]>([]);
  invoiceSettlements = signal<Record<string, InvoiceSettlementSummary>>({});
  invoiceHistoryLoading = signal(false);
  invoiceHistoryError = signal(false);
  selectedInvoiceForEstimates = signal<Invoice | null>(null);

  readonly existingInvoiceForSelectedVisit = computed(() => {
    const visitId = this.selectedVisitId();
    return visitId ? this.invoices().find((invoice) => invoice.visitId === visitId) ?? null : null;
  });

  // Payment Modal
  showPaymentModal = signal(false);
  paymentInvoice = signal<Invoice | null>(null);
  savingPayment = signal(false);
  cashSessionOpen = signal<boolean | null>(null);

  // Alerts
  successMessage = signal<string | null>(null);
  errorMessage = signal<string | null>(null);

  /** P1-B — true pendant le chargement initial du deep-link invoice */
  deepLinkLoading = signal(false);

  readonly translate = (key: string, defaultValue: string): string => this.t(key, defaultValue);

  ngOnInit(): void {
    this.loadGlobalConfigs();
    this.loadCashSessionState();
    this.handleDeepLink();
  }

  /**
   * P1-B — Si la route contient :invoiceId, charge la facture depuis l'API
   * et ouvre automatiquement le panneau latéral de détail.
   */
  private handleDeepLink(): void {
    const invoiceId = this.route.snapshot.paramMap.get('invoiceId');
    if (!invoiceId) return;

    this.deepLinkLoading.set(true);
    this.activeTab.set('facturation');

    this.billingApi.getInvoice(invoiceId).subscribe({
      next: (invoice) => {
        this.patientApi.getById(invoice.patientId).subscribe({
          next: (patient) => {
            this.selectedPatient.set(patient);
            this.loadPatientHistory(patient.id);
          },
          error: () => { /* patient non critique pour afficher le panneau */ }
        });
        this.openInvoiceDetails(invoice);
        this.deepLinkLoading.set(false);
      },
      error: () => {
        this.deepLinkLoading.set(false);
        this.showError('billing.error.invoiceNotFound');
      }
    });
  }

  loadCashSessionState(): void {
    this.billingApi.getActiveCashSession().subscribe({
      next: (session) => this.cashSessionOpen.set(!!session),
      error: () => this.cashSessionOpen.set(false),
    });
  }

  t(key: string, defaultValue: string): string {
    const translated = this.i18n.t(key);
    return translated === key ? defaultValue : translated;
  }

  loadGlobalConfigs(): void {
    this.billingApi.listConventions().subscribe({
      next: (res) => this.conventions.set(res),
      error: () => this.showError('billing.error.load')
    });
    this.billingApi.listTariffs().subscribe({
      next: (res) => this.tariffs.set(res),
      error: () => this.showError('billing.error.load')
    });
  }

  searchPatients(): void {
    if (!this.searchQuery.trim()) return;
    this.patientApi.list(this.searchQuery).subscribe({
      next: (res) => { this.patients.set(res); this.searched.set(true); },
      error: () => this.showError('billing.error.load')
    });
  }

  selectPatient(patient: Patient): void {
    this.selectedPatient.set(patient);
    this.selectedVisitId.set('');
    this.invoiceItems.set([]);
    this.selectedInvoiceForEstimates.set(null);
    this.loadPatientHistory(patient.id);
    this.patients.set([]);
    this.searched.set(false);
    this.visitApi.getPatientVisits(patient.id).subscribe({
      next: (visits) => {
        this.patientVisits.set(visits);
        const active = visits.find(visit => visit.status === 'EN_COURS' || visit.status === 'ACTIVE');
        if (active) {
          this.selectedVisitId.set(active.id);
          this.precalculateFromVisit();
        }
      },
      error: () => this.showError('billing.error.load')
    });
  }

  loadPatientHistory(patientId: string): void {
    this.invoiceHistoryLoading.set(true);
    this.invoiceHistoryError.set(false);

    forkJoin({
      invoices: this.billingApi.listInvoices(patientId),
      summaries: this.billingApi.listInvoiceSettlementSummaries(patientId).pipe(
        catchError(() => of([] as InvoiceSettlementSummary[]))
      ),
    }).pipe(
      finalize(() => this.invoiceHistoryLoading.set(false))
    ).subscribe({
      next: ({ invoices, summaries }) => {
        this.invoices.set(invoices);
        this.invoiceSettlements.set(Object.fromEntries(
          summaries.map(summary => [summary.invoiceId, summary])
        ));
      },
      error: () => {
        this.invoices.set([]);
        this.invoiceSettlements.set({});
        this.invoiceHistoryError.set(true);
      },
    });
  }

  retryInvoiceHistory(): void {
    const patient = this.selectedPatient();
    if (patient) this.loadPatientHistory(patient.id);
  }

  onVisitSelected(): void {
    if (this.selectedVisitId()) this.precalculateFromVisit();
    else this.invoiceItems.set([]);
  }

  precalculateFromVisit(): void {
    const patient = this.selectedPatient();
    if (!patient) return;
    this.billingApi.precalculateInvoice(patient.id, this.selectedVisitId() || undefined, this.selectedConventionId() || undefined).subscribe({
      next: (res) => {
        this.invoiceItems.set(res.items);
        if (res.insuranceConvention) this.selectedConventionId.set(res.insuranceConvention.id);
      },
      error: () => { this.showError('billing.noActiveVisit'); this.invoiceItems.set([]); }
    });
  }

  addCustomItem(): void {
    this.invoiceItems.set([...this.invoiceItems(), {
      label: 'Prestation libre',
      itemType: 'CONSULTATION',
      unitPrice: 0,
      quantity: 1,
      coefficient: 1.0
    }]);
  }

  removeItem(index: number): void {
    const items = [...this.invoiceItems()];
    items.splice(index, 1);
    this.invoiceItems.set(items);
  }

  recalculateTotals(): void {
    this.invoiceItems.set([...this.invoiceItems()]);
  }

  saveInvoice(): void {
    const patient = this.selectedPatient();
    if (!patient || this.existingInvoiceForSelectedVisit() || this.hasInvalidItems()) return;
    this.savingInvoice.set(true);
    this.billingApi.createInvoice({
      patientId: patient.id,
      visitId: this.selectedVisitId() || undefined,
      insuranceConventionId: this.selectedConventionId() || undefined,
      items: this.invoiceItems()
    }).subscribe({
      next: () => {
        this.savingInvoice.set(false);
        this.showSuccess('billing.success.invoiceCreated');
        this.invoiceItems.set([]);
        this.loadPatientHistory(patient.id);
      },
      error: () => { this.savingInvoice.set(false); this.showError('billing.error.save'); }
    });
  }

  viewExistingInvoice(): void {
    const invoice = this.existingInvoiceForSelectedVisit();
    if (invoice) this.openInvoiceDetails(invoice);
  }

  openInvoiceDetails(invoice: Invoice): void {
    this.selectedInvoiceForEstimates.set(invoice);
    this.zone.onStable.pipe(take(1)).subscribe(() => this.invoiceDetailsPanel?.nativeElement.focus());
  }

  closeInvoiceDetails(): void {
    const invoiceId = this.selectedInvoiceForEstimates()?.id;
    this.selectedInvoiceForEstimates.set(null);
    if (invoiceId) {
      this.zone.onStable.pipe(take(1)).subscribe(() => this.invoiceHistory?.focusInvoice(invoiceId));
    }
  }

  openInsuranceFollowUp(): void { this.activeTab.set('bordereaux'); }
  openCashRegisterFromPayment(): void { this.closePaymentModal(); this.activeTab.set('caisse'); }

  printInvoicePdf(invoiceId: string): void {
    this.billingApi.downloadInvoicePdf(invoiceId).subscribe({
      next: (blob) => { const url = window.URL.createObjectURL(blob); window.open(url, '_blank'); },
      error: () => this.showError('billing.error.save')
    });
  }

  openPaymentModal(invoice: Invoice): void {
    this.paymentInvoice.set(invoice);
    this.showPaymentModal.set(true);
  }

  closePaymentModal(): void {
    this.showPaymentModal.set(false);
    this.paymentInvoice.set(null);
  }

  submitPayment(payment: BillingPaymentForm): void {
    const invoice = this.paymentInvoice();
    if (!invoice) return;
    this.savingPayment.set(true);
    this.billingApi.addPayment(invoice.id, payment.amount, payment.method, payment.reference).subscribe({
      next: () => {
        this.savingPayment.set(false);
        this.showSuccess('billing.success.paymentAdded');
        this.closePaymentModal();
        this.cashSessionOpen.set(true);
        const patient = this.selectedPatient();
        if (patient) this.loadPatientHistory(patient.id);
      },
      error: () => { this.savingPayment.set(false); this.showError('billing.error.save'); }
    });
  }

  saveConvention(convention: { name: string; rate: number }): void {
    this.billingApi.createConvention(convention.name, convention.rate).subscribe({
      next: () => { this.showSuccess('billing.success.save'); this.loadGlobalConfigs(); },
      error: () => this.showError('billing.error.save')
    });
  }

  saveTariff(tariff: { keyLetter: string; unitValue: number }): void {
    this.billingApi.createOrUpdateTariff(tariff.keyLetter, tariff.unitValue).subscribe({
      next: () => { this.showSuccess('billing.success.save'); this.loadGlobalConfigs(); },
      error: () => this.showError('billing.error.save')
    });
  }

  private showSuccess(key: string): void {
    this.successMessage.set(this.t(key, 'Action effectuée avec succès.'));
    this.errorMessage.set(null);
    setTimeout(() => this.successMessage.set(null), 5000);
  }

  private showError(key: string): void {
    this.errorMessage.set(this.t(key, 'Une erreur est survenue.'));
    this.successMessage.set(null);
    setTimeout(() => this.errorMessage.set(null), 5000);
  }
}