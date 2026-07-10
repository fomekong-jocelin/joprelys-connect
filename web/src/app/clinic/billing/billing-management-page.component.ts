import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BillingApiService } from '../../patient/billing-api.service';
import { PatientApiService } from '../../patient/patient-api.service';
import { VisitApiService } from '../../visit/visit-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { AuthTokenStorageService } from '../../auth/auth-token-storage.service';
import { Patient, Invoice, InvoiceSettlementSummary, InsuranceConvention, TariffGrid, InvoiceItem } from '../../patient/patient.models';
import { Visit } from '../../visit/visit.models';
import { IconComponent } from '../../shared/ui/icon.component';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { BillingAdminTabsComponent } from './billing-admin-tabs.component';
import { BillingInvoiceHistoryComponent } from './billing-invoice-history.component';
import { BillingPaymentForm, BillingPaymentModalComponent } from './billing-payment-modal.component';
import { BillingEstimatesComponent } from './billing-estimates.component';
import { BillingCashRegisterComponent } from './billing-cash-register.component';
import { BillingReceivablesComponent } from './billing-receivables.component';
import { BillingInsuranceBordereauxComponent } from './billing-insurance-bordereaux.component';
import { BillingDafDashboardComponent } from './billing-daf-dashboard.component';

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

  activeTab = signal<'facturation' | 'caisse' | 'creances' | 'bordereaux' | 'conventions' | 'tariffs' | 'daf'>('facturation');

  hasRole(allowedRoles: string[] | string): boolean {
    const role = this.tokenStorage.session()?.role;
    if (!role) return false;
    const roles = role.split(',').map((r) => r.trim());
    return Array.isArray(allowedRoles)
      ? roles.some((r) => allowedRoles.includes(r))
      : roles.includes(allowedRoles);
  }

  /** DAF ou Admin : accès Pilotage DAF */
  readonly isDafOrAdmin = computed(() => this.hasRole(['DAF', 'ADMIN_CLINIQUE']));

  /** Admin uniquement : Conventions, Grille tarifaire, Créances, Bordereaux */
  readonly isAdminOnly = computed(() => this.hasRole(['ADMIN_CLINIQUE']));

  /** Agent accueil + Admin : Facturation patients, Caisse & Sessions */
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

  /** P1 — true si au moins une ligne a prix ≤ 0 ou quantité < 1 */
  isItemInvalid(item: InvoiceItem): boolean {
    return (item.unitPrice ?? 0) <= 0 || (item.quantity ?? 0) < 1 || (item.coefficient ?? 1) <= 0;
  }

  readonly hasInvalidItems = computed(() => this.invoiceItems().some(item => this.isItemInvalid(item)));

  // History
  invoices = signal<Invoice[]>([]);
  invoiceSettlements = signal<Record<string, InvoiceSettlementSummary>>({});
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

  readonly translate = (key: string, defaultValue: string): string => this.t(key, defaultValue);

  ngOnInit(): void {
    this.loadGlobalConfigs();
    this.loadCashSessionState();
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

  selectPatient(p: Patient): void {
    this.selectedPatient.set(p);
    this.selectedVisitId.set('');
    this.invoiceItems.set([]);
    this.selectedInvoiceForEstimates.set(null);
    this.loadPatientHistory(p.id);
    this.patients.set([]);
    this.searched.set(false);
    this.visitApi.getPatientVisits(p.id).subscribe({
      next: (res) => {
        this.patientVisits.set(res);
        const active = res.find(v => v.status === 'EN_COURS' || v.status === 'ACTIVE');
        if (active) {
          this.selectedVisitId.set(active.id);
          this.precalculateFromVisit();
        }
      },
      error: () => this.showError('billing.error.load')
    });
  }

  loadPatientHistory(patientId: string): void {
    this.billingApi.listInvoices(patientId).subscribe({
      next: (res) => this.invoices.set(res),
      error: () => this.showError('billing.error.load')
    });
    this.billingApi.listInvoiceSettlementSummaries(patientId).subscribe({
      next: (summaries) => this.invoiceSettlements.set(
        Object.fromEntries(summaries.map(s => [s.invoiceId, s]))
      ),
      error: () => this.invoiceSettlements.set({})
    });
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

  openInvoiceDetails(invoice: Invoice): void { this.selectedInvoiceForEstimates.set(invoice); }
  closeInvoiceDetails(): void { this.selectedInvoiceForEstimates.set(null); }
  openInsuranceFollowUp(): void { this.activeTab.set('bordereaux'); }
  openCashRegisterFromPayment(): void { this.closePaymentModal(); this.activeTab.set('caisse'); }

  printInvoicePdf(invoiceId: string): void {
    this.billingApi.downloadInvoicePdf(invoiceId).subscribe({
      next: (blob) => { const url = window.URL.createObjectURL(blob); window.open(url, '_blank'); },
      error: () => this.showError('billing.error.save')
    });
  }

  openPaymentModal(inv: Invoice): void { this.paymentInvoice.set(inv); this.showPaymentModal.set(true); }
  closePaymentModal(): void { this.showPaymentModal.set(false); this.paymentInvoice.set(null); }

  submitPayment(payment: BillingPaymentForm): void {
    const inv = this.paymentInvoice();
    if (!inv) return;
    this.savingPayment.set(true);
    this.billingApi.addPayment(inv.id, payment.amount, payment.method, payment.reference).subscribe({
      next: () => {
        this.savingPayment.set(false);
        this.showSuccess('billing.success.paymentAdded');
        this.closePaymentModal();
        this.cashSessionOpen.set(true);
        if (this.selectedPatient()) this.loadPatientHistory(this.selectedPatient()!.id);
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
