import { Component, inject, OnInit, signal } from '@angular/core';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { PatientProfileCardComponent } from './components/patient-profile-card.component';
import { PatientVisitsListComponent } from './components/patient-visits-list.component';
import { PatientPortalMeResponse, PatientPortalService } from './services/patient-portal.service';

@Component({
  selector: 'app-patient-dashboard',
  standalone: true,
  imports: [AppShellComponent, PatientProfileCardComponent, PatientVisitsListComponent],
  template: `
    <app-shell>
      <div class="app-container py-8 flex flex-col gap-6">
        <div>
          <h1 class="font-display text-2xl font-extrabold" style="color: var(--text-primary)">
            Mon Espace Santé
          </h1>
          <p class="text-sm text-[var(--text-secondary)]">
            Consultez vos informations médicales et téléchargez vos ordonnances sécurisées.
          </p>
        </div>

        @if (isLoading()) {
          <div class="flex items-center justify-center p-12">
            <div class="w-8 h-8 border-4 border-[var(--brand-primary)] border-t-transparent rounded-full animate-spin"></div>
          </div>
        } @else if (error()) {
          <div class="p-4 rounded-[var(--radius-brand-md)] bg-rose-50 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-800/40 text-rose-700 dark:text-rose-300 text-sm font-semibold">
            {{ error() }}
          </div>
        } @else if (patientData()) {
          <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
            <div class="lg:col-span-1">
              <app-patient-profile-card [patient]="patientData()!" />
            </div>
            <div class="lg:col-span-2">
              <app-patient-visits-list
                [consultations]="patientData()!.consultations"
                (download)="onDownloadDocument($event)"
              />
            </div>
          </div>
        }
      </div>
    </app-shell>
  `
})
export class PatientDashboardComponent implements OnInit {
  private readonly portalService = inject(PatientPortalService);

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
        this.error.set(err.error?.detail || "Impossible de charger les données du portail patient.");
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
        alert("Erreur lors du téléchargement de l'ordonnance.");
      }
    });
  }
}
