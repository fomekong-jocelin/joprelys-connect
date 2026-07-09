import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BillingApiService } from '../../patient/billing-api.service';
import { PatientApiService } from '../../patient/patient-api.service';
import { VisitApiService } from '../../visit/visit-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { Patient, Invoice, InsuranceConvention, TariffGrid, InvoiceItem } from '../../patient/patient.models';
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
  ],
  templateUrl: './billing-management-page.component.html',
  styles: [`
    /* Scrollbar minimaliste */
    ::-webkit-scrollbar {
      width: 4px;
      height: 4px;
    }
    ::-webkit-scrollbar-thumb {
      background: var(--app-border);
      border-radius: 2px;
    }
  `]
})
export class BillingManagementPageComponent implements OnInit {
  private readonly billingApi = inject(BillingApiService);
  private readonly patientApi = inject(PatientApiService);
  private readonly visitApi = inject(VisitApiService);
  private readonly i18n = inject(I18nService);

  activeTab = signal<'facturation' | 'caisse' | 'creances' | 'bordereaux' | 'conventions' | 'tariffs'>('facturation');
  
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

  // Totals calculations
  calculatedTotal = computed(() => {
    return this.invoiceItems().reduce((acc, item) => {
      const price = item.unitPrice || 0;
      const qty = item.quantity || 0;
      const coeff = item.coefficient || 1.0;
      return acc + (price * qty * coeff);
    }, 0);
  });

  calculatedInsuranceShare = computed(() => {
    const total = this.calculatedTotal();
    const convention = this.conventions().find(c => c.id === this.selectedConventionId());
    if (!convention) return 0;
    return total * convention.coveragePercentage;
  });

  calculatedPatientShare = computed(() => {
    const total = this.calculatedTotal();
    const insurance = this.calculatedInsuranceShare();
    return total - insurance;
  });

  // History
  invoices = signal<Invoice[]>([]);
  selectedInvoiceForEstimates = signal<Invoice | null>(null);

  // Payment Modal
  showPaymentModal = signal(false);
  paymentInvoice = signal<Invoice | null>(null);
  savingPayment = signal(false);

  // Alerts
  successMessage = signal<string | null>(null);
  errorMessage = signal<string | null>(null);

  readonly translate = (key: string, defaultValue: string): string => this.t(key, defaultValue);

  ngOnInit(): void {
    this.loadGlobalConfigs();
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
      next: (res) => {
        this.patients.set(res);
        this.searched.set(true);
      },
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

    // Load patient visits
    this.visitApi.getPatientVisits(p.id).subscribe({
      next: (res) => {
        this.patientVisits.set(res);
        // Auto select active visit if any
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
  }

  onVisitSelected(): void {
    if (this.selectedVisitId()) {
      this.precalculateFromVisit();
    } else {
      this.invoiceItems.set([]);
    }
  }

  precalculateFromVisit(): void {
    const patient = this.selectedPatient();
    if (!patient) return;

    this.billingApi.precalculateInvoice(patient.id, this.selectedVisitId() || undefined, this.selectedConventionId() || undefined).subscribe({
      next: (res) => {
        this.invoiceItems.set(res.items);
        if (res.insuranceConvention) {
          this.selectedConventionId.set(res.insuranceConvention.id);
        }
      },
      error: (err) => {
        this.showError('billing.noActiveVisit');
        this.invoiceItems.set([]);
      }
    });
  }

  addCustomItem(): void {
    const items = [...this.invoiceItems()];
    items.push({
      label: 'Prestation libre',
      itemType: 'CONSULTATION',
      unitPrice: 0,
      quantity: 1,
      coefficient: 1.0
    });
    this.invoiceItems.set(items);
  }

  removeItem(index: number): void {
    const items = [...this.invoiceItems()];
    items.splice(index, 1);
    this.invoiceItems.set(items);
  }

  recalculateTotals(): void {
    // Simply forces computed triggers
    this.invoiceItems.set([...this.invoiceItems()]);
  }

  saveInvoice(): void {
    const patient = this.selectedPatient();
    if (!patient) return;

    this.savingInvoice.set(true);
    this.billingApi.createInvoice({
      patientId: patient.id,
      visitId: this.selectedVisitId() || undefined,
      insuranceConventionId: this.selectedConventionId() || undefined,
      items: this.invoiceItems()
    }).subscribe({
      next: (res) => {
        this.savingInvoice.set(false);
        this.showSuccess('billing.success.invoiceCreated');
        this.invoiceItems.set([]);
        this.loadPatientHistory(patient.id);
      },
      error: () => {
        this.savingInvoice.set(false);
        this.showError('billing.error.save');
      }
    });
  }

  printInvoicePdf(invoiceId: string): void {
    this.billingApi.downloadInvoicePdf(invoiceId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
      },
      error: (err) => {
        console.error('Error downloading invoice PDF:', err);
        this.showError('billing.error.save');
      }
    });
  }

  // Payment Modal actions
  openPaymentModal(inv: Invoice): void {
    this.paymentInvoice.set(inv);
    this.showPaymentModal.set(true);
  }

  closePaymentModal(): void {
    this.showPaymentModal.set(false);
    this.paymentInvoice.set(null);
  }

  submitPayment(payment: BillingPaymentForm): void {
    const inv = this.paymentInvoice();
    if (!inv) return;

    this.savingPayment.set(true);
    this.billingApi.addPayment(inv.id, payment.amount, payment.method, payment.reference).subscribe({
      next: () => {
        this.savingPayment.set(false);
        this.showSuccess('billing.success.paymentAdded');
        this.closePaymentModal();
        if (this.selectedPatient()) {
          this.loadPatientHistory(this.selectedPatient()!.id);
        }
      },
      error: () => {
        this.savingPayment.set(false);
        this.showError('billing.error.save');
      }
    });
  }

  // Configuration management
  saveConvention(convention: { name: string; rate: number }): void {
    this.billingApi.createConvention(convention.name, convention.rate).subscribe({
      next: () => {
        this.showSuccess('billing.success.save');
        this.loadGlobalConfigs();
      },
      error: () => this.showError('billing.error.save')
    });
  }

  saveTariff(tariff: { keyLetter: string; unitValue: number }): void {
    this.billingApi.createOrUpdateTariff(tariff.keyLetter, tariff.unitValue).subscribe({
      next: () => {
        this.showSuccess('billing.success.save');
        this.loadGlobalConfigs();
      },
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
