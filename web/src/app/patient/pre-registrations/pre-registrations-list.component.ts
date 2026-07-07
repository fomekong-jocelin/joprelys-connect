import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, CommonModule } from '@angular/common';
import { AlertComponent } from '../../shared/ui/alert.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state.component';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { PatientApiService } from '../patient-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { AuthTokenStorageService } from '../../auth/auth-token-storage.service';
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
    DatePipe
  ],
  templateUrl: './pre-registrations-list.component.html'
})
export class PreRegistrationsListComponent implements OnInit {
  private readonly patientApiService = inject(PatientApiService);
  readonly i18n = inject(I18nService);
  private readonly tokenStorage = inject(AuthTokenStorageService);

  showQrCodeModal = signal(false);

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

  t(key: string): string {
    return this.i18n.t(key);
  }

  getAdmissionLink(): string {
    const orgId = this.getOrganizationId();
    if (!orgId) return '';
    return `${window.location.origin}/public/register?orgId=${orgId}`;
  }

  getQrCodeUrl(): string {
    const link = this.getAdmissionLink();
    if (!link) return '';
    return `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodeURIComponent(link)}`;
  }

  getOrganizationId(): string | null {
    const token = this.tokenStorage.accessToken;
    if (!token) return null;
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        window.atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      const payload = JSON.parse(jsonPayload);
      return payload.org || null;
    } catch (e) {
      console.error('Error decoding JWT token:', e);
      return null;
    }
  }

  openQrModal(): void {
    this.showQrCodeModal.set(true);
  }

  closeQrModal(): void {
    this.showQrCodeModal.set(false);
  }

  printQrCode(): void {
    const qrUrl = this.getQrCodeUrl();
    const logoUrl = `${window.location.origin}/assets/branding/logo_principal.png`;
    const printWindow = window.open('', '_blank');
    if (printWindow) {
      printWindow.document.write(`
        <html>
          <head>
            <title>QR Code d'Admission Joprelys Connect</title>
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
                border-radius: 12px;
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
              .logo-text { font-family: system-ui, sans-serif; font-size: 24px; font-weight: 800; letter-spacing: -0.025em; color: #4f46e5; }
              p { font-size: 14px; color: #64748b; margin-bottom: 24px; line-height: 1.5; }
              .qr-img { margin-bottom: 8px; border: 1px solid #e2e8f0; padding: 8px; border-radius: 6px; }
            </style>
          </head>
          <body>
            <div class="container">
              <div class="logo-container">
                <img src="${logoUrl}" alt="Joprelys" class="logo" />
                <span class="logo-text">Connect</span>
              </div>
              <p>Scannez ce QR Code pour remplir votre formulaire d'admission autonome sur votre smartphone.</p>
              <img src="${qrUrl}" width="250" height="250" class="qr-img" alt="QR Code" />
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
    this.patientApiService.getPendingPreRegistrations(this.page(), this.size()).subscribe({
      next: (res) => {
        // Filtrer localement ou conserver tout
        // L'API backend filtre par défaut sur AWAITING_VALIDATION ou retourne tout selon la page,
        // mais pour des raisons d'UX propres, l'API envoie l'ensemble paginé et nous filtrons/affichons.
        // Attends ! Le backend Spring Boot filtre-t-il sur statut via requêtes SQL ?
        // Dans STORY-0303 : "endpoints privés gérant le listing...". En général le listing retourne
        // toutes les demandes paginées. L'agent peut alors filtrer.
        // Filtrons localement ou affichons tel quel. Le plus robuste est d'afficher les éléments reçus du backend.
        // Pour s'assurer de la cohérence avec le filtre sélectionné, filtrons les résultats si le backend retourne tout.
        // Si le backend filtre directement par statut ?
        // Regardons dans PatientPreRegistrationService.java comment est fait getPendingPreRegistrations ou getPreRegistrations.
        // Si on passe page/size, le backend filtre-t-il sur AWAITING_VALIDATION ?
        // Oui, "getPendingPreRegistrations" implique typiquement les demandes en attente de validation (AWAITING_VALIDATION).
        // Donc si on demande les "pending", il renvoie les AWAITING_VALIDATION.
        // Et si le filtre sélectionné est VALIDATED ou REJECTED ?
        // L'API privée GET /api/pre-registrations accepte-t-elle un paramètre de statut ?
        // Regardons PatientPreRegistrationController.java (la méthode getPreRegistrations).
        this.preRegistrations.set(
          res.content.filter(p => p.status === this.filter())
        );
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
        this.error.set("Impossible de télécharger le PDF de la fiche d'admission.");
      }
    });
  }
}
