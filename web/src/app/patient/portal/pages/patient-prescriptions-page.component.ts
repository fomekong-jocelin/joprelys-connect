import { Component, inject, OnInit, signal } from '@angular/core';
import { AppShellComponent } from '../../../shared/layout/app-shell.component';
import { PatientVisitsListComponent } from '../components/patient-visits-list.component';
import { PatientPortalMeResponse, PatientPortalService } from '../services/patient-portal.service';
import { I18nService } from '../../../core/i18n/i18n.service';

@Component({
  selector: 'app-patient-prescriptions-page',
  standalone: true,
  imports: [AppShellComponent, PatientVisitsListComponent],
  template: `
    <app-shell>
      <div class="app-container py-6">
        @if (isLoading()) {
          <div class="flex items-center justify-center py-12">
            <div class="w-8 h-8 border-4 border-[var(--brand-primary)] border-t-transparent rounded-full animate-spin"></div>
          </div>
        } @else if (error()) {
          <div class="p-4 rounded-[var(--radius-brand-md)] bg-rose-50 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-800/40 text-rose-700 dark:text-rose-300 text-sm font-semibold">
            {{ error() }}
          </div>
        } @else if (patientData()) {
          <app-patient-visits-list
            [consultations]="patientData()!.consultations"
            (download)="onDownloadDocument($event)"
            (transmit)="onTransmitPrescription($event)"
          />
        }
      </div>
    </app-shell>
  `
})
export class PatientPrescriptionsPageComponent implements OnInit {
  private readonly portalService = inject(PatientPortalService);
  readonly i18n = inject(I18nService);

  readonly patientData = signal<PatientPortalMeResponse | null>(null);
  readonly isLoading = signal(false);
  readonly error = signal('');

  ngOnInit(): void {
    this.loadPatientData();
  }

  loadPatientData(): void {
    this.isLoading.set(true);
    this.error.set('');
    this.portalService.getMe().subscribe({
      next: (data) => {
        this.patientData.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.error.set(err.error?.detail || this.i18n.t('patients.loadError'));
      }
    });
  }

  onDownloadDocument(visitId: string): void {
    this.portalService.downloadDocument(visitId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `ordonnance-${visitId}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        alert(this.i18n.t('patients.downloadPdfError'));
      }
    });
  }

  onTransmitPrescription(prescriptionId: string): void {
    const consultations = this.patientData()?.consultations || [];
    const consult = consultations.find(c => c.prescriptionId === prescriptionId);
    if (consult) {
      consult.prescriptionTransmissionStatus = 'PENDING';
    }
    this.portalService.transmitPrescription(prescriptionId).subscribe({
      next: () => {
        this.loadPatientData();
      },
      error: (err) => {
        if (consult) {
          consult.prescriptionTransmissionStatus = 'FAILED';
        }
        alert(err.error?.detail || this.i18n.t('patient.prescription.transmitError'));
      }
    });
  }
}
