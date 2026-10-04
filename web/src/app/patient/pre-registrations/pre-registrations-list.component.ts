import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe, CommonModule } from '@angular/common';
import { AlertComponent } from '../../shared/ui/alert.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state.component';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { PatientApiService } from '../patient-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { PatientPreRegistrationResponse, PreRegistrationValidationRequest } from '../patient.models';

@Component({
  selector: 'app-pre-registrations-list',
  standalone: true,
  imports: [
    CommonModule,
    AlertComponent,
    ButtonComponent,
    PageHeaderComponent,
    EmptyStateComponent,
    AppShellComponent,
    DatePipe,
    RouterLink
  ],
  templateUrl: './pre-registrations-list.component.html'
})
export class PreRegistrationsListComponent implements OnInit, OnDestroy {
  private readonly patientApiService = inject(PatientApiService);
  readonly i18n = inject(I18nService);

  showQrCodeModal = signal(false);
  readonly qrCodeUrl = signal<string | null>(null);
  readonly qrCodeError = signal(false);

  // État du chargement et pagination
  preRegistrations = signal<PatientPreRegistrationResponse[]>([]);
  isLoading = signal(false);
  page = signal(0);
  size = signal(10);
  totalElements = signal(0);
  totalPages = signal(0);

  // Filtre sélectionné (par défaut AWAITING_VALIDATION)
  filter = signal<'AWAITING_VALIDATION' | 'VALIDATED' | 'REJECTED'>('AWAITING_VALIDATION');

  // État du tiroir de détails (Drawer)
  selectedPreRegistration = signal<PatientPreRegistrationResponse | null>(null);
  
  // Option de réconciliation : 'NEW' (Nouveau) ou 'MERGE' (Fusion avec similarPatientId)
  reconcileOption = signal<'NEW' | 'MERGE'>('NEW');

  // États pour les notifications de retour d'API
  error = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  actionLoading = signal(false);
  validatedPatientId = signal<string | null>(null);

  ngOnInit(): void {
    this.loadPreRegistrations();
  }

  ngOnDestroy(): void {
    const qrUrl = this.qrCodeUrl();
    if (qrUrl) window.URL.revokeObjectURL(qrUrl);
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  openQrModal(): void {
    this.showQrCodeModal.set(true);
    this.qrCodeError.set(false);
    if (this.qrCodeUrl()) return;
    this.patientApiService.getAdmissionQrCode().subscribe({
      next: (blob) => this.qrCodeUrl.set(window.URL.createObjectURL(blob)),
      error: () => this.qrCodeError.set(true),
    });
  }

  closeQrModal(): void {
    this.showQrCodeModal.set(false);
  }

  printQrCode(): void {
    const qrUrl = this.qrCodeUrl();
    if (!qrUrl) return;
    const logoUrl = `${window.location.origin}/assets/branding/logo_principal.png`;
    const printWindow = window.open('', '_blank');
    if (printWindow) {
      printWindow.document.write(`
        <html>
          <head>
            <title>${this.t('preRegistrations.qrPrintTitle')}</title>
            <style>
              body {
                font-family: system-ui, sans-serif;
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                height: 100vh;
                margin: 0;
                text-align: center;
              }
              .container {
                border: 2px solid #e2e8f0;
                padding: 40px;
                border-radius: 6px;
                max-width: 400px;
              }
              .logo-container {
                display: flex;
                align-items: center;
                justify-content: center;
                gap: 12px;
                margin-bottom: 24px;
              }
              .logo { height: 36px; width: auto; object-fit: contain; }
              p { font-size: 14px; color: #64748b; margin-bottom: 24px; line-height: 1.5; }
              .qr-img { margin-bottom: 8px; border: 1px solid #e2e8f0; padding: 8px; border-radius: 6px; }
            </style>
          </head>
          <body>
            <div class="container">
              <div class="logo-container">
                <img src="${logoUrl}" alt="" class="logo" />
              </div>
              <p>${this.t('preRegistrations.qrPrintHelp')}</p>
              <img src="${qrUrl}" width="250" height="250" class="qr-img" alt="QR" />
            </div>
            <script>
              window.onload = function() {
                window.print();
                setTimeout(() => window.close(), 500);
              };
            </script>
          </body>
        </html>
      `);
      printWindow.document.close();
    }
  }

  loadPreRegistrations(): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.patientApiService.getPreRegistrations(this.filter(), this.page(), this.size()).subscribe({
      next: (res) => {
        this.preRegistrations.set(res.content);
        this.totalElements.set(res.totalElements);
        this.totalPages.set(res.totalPages);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.error.set(this.t('common.error.server'));
      }
    });
  }

  setFilter(status: 'AWAITING_VALIDATION' | 'VALIDATED' | 'REJECTED'): void {
    this.filter.set(status);
    this.page.set(0);
    this.loadPreRegistrations();
  }

  prevPage(): void {
    if (this.page() > 0) {
      this.page.update(p => p - 1);
      this.loadPreRegistrations();
    }
  }

  nextPage(): void {
    if ((this.page() + 1) < this.totalPages()) {
      this.page.update(p => p + 1);
      this.loadPreRegistrations();
    }
  }

  openDetails(preReg: PatientPreRegistrationResponse): void {
    this.selectedPreRegistration.set(preReg);
    this.reconcileOption.set(preReg.similarPatientId ? 'MERGE' : 'NEW');
    this.validatedPatientId.set(null);
    this.error.set(null);
    this.successMessage.set(null);
  }

  closeDetails(): void {
    if (!this.actionLoading()) {
      this.selectedPreRegistration.set(null);
    }
  }

  setReconcileOption(option: 'NEW' | 'MERGE'): void {
    this.reconcileOption.set(option);
  }

  rejectRequest(id: string): void {
    if (this.actionLoading()) return;
    if (!confirm(this.t('preRegistrations.rejectConfirm'))) return;

    this.actionLoading.set(true);
    this.error.set(null);
    this.successMessage.set(null);

    this.patientApiService.rejectPreRegistration(id).subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.successMessage.set(this.t('preRegistrations.successRejected'));
        this.closeDetails();
        this.loadPreRegistrations();
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.error.set(err.error?.message || this.t('common.error.server'));
      }
    });
  }

  /** Patient lié à une demande validée (nouveau dossier ou dossier réconcilié). */
  linkedPatientId(preReg: PatientPreRegistrationResponse): string | null {
    return preReg.validatedPatientId || this.validatedPatientId() || null;
  }

  validateRequest(preReg: PatientPreRegistrationResponse): void {
    if (this.actionLoading()) return;

    this.actionLoading.set(true);
    this.error.set(null);
    this.successMessage.set(null);

    const validationRequest: PreRegistrationValidationRequest = {
      firstName: preReg.firstName,
      lastName: preReg.lastName,
      gender: preReg.gender,
      birthDate: preReg.birthDate,
      bloodGroup: preReg.bloodGroup || undefined,
      phone: preReg.phone || undefined,
      email: preReg.email || undefined,
      address: preReg.address || undefined,
      emergencyContactName: preReg.emergencyContactName || undefined,
      emergencyContactPhone: preReg.emergencyContactPhone || undefined,
      emergencyContactRelation: preReg.emergencyContactRelation || undefined,
      reconcileWithPatientId: this.reconcileOption() === 'MERGE' ? preReg.similarPatientId : undefined
    };

    this.patientApiService.validatePreRegistration(preReg.id, validationRequest).subscribe({
      next: (res) => {
        this.actionLoading.set(false);
        this.validatedPatientId.set(res.patientId);
        this.successMessage.set(this.t('preRegistrations.successValidated'));
        
        // Télécharger automatiquement le PDF de la fiche d'admission physique
        this.downloadSummaryPdf(res.patientId);

        // Mettre à jour la ligne validée localement
        preReg.status = 'VALIDATED';
        this.closeDetails();
        this.loadPreRegistrations();
      },
      error: (err) => {
        this.actionLoading.set(false);
        this.error.set(err.error?.message || this.t('common.error.server'));
      }
    });
  }

  downloadSummaryPdf(patientId: string): void {
    this.patientApiService.downloadSummaryPdf(patientId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `fiche-admission-patient-${patientId}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Error downloading summary pdf', err);
        this.error.set(this.t('preRegistrations.downloadError'));
      }
    });
  }
}
