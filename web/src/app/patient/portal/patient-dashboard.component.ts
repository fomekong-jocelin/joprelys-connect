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
        <div class="flex flex-col gap-1">
          <h1 class="font-display text-3xl font-extrabold" style="color: var(--text-primary)">
            @if (patientData()) {
              Bonjour, {{ patientData()?.fullName }} ! 👋
            } @else {
              Mon Espace Santé
            }
          </h1>
          <p class="text-sm text-[var(--text-secondary)]">
            Bienvenue dans votre espace santé connecté Joprelys.
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
          <!-- Cartes de services rapides -->
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <!-- Carte 1 -->
            <div class="ui-card p-4 flex items-start gap-4 transition-all duration-200 hover:border-[var(--brand-primary)] hover:shadow-md cursor-pointer">
              <div class="p-2.5 rounded-[var(--radius-brand-md)] bg-[var(--brand-primary)]/10 text-[var(--brand-primary)]">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-6 h-6">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
                </svg>
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center justify-between gap-2">
                  <h4 class="font-display font-bold text-sm text-[var(--text-primary)]">Mes Ordonnances</h4>
                  <span class="text-[10px] font-bold px-2 py-0.5 rounded-[var(--radius-brand-xs)] bg-[var(--brand-primary)]/10 text-[var(--brand-primary)]">
                    {{ patientData()?.consultations?.length || 0 }}
                  </span>
                </div>
                <p class="text-xs text-[var(--text-secondary)] mt-1">Consultez et téléchargez vos documents médicaux.</p>
              </div>
            </div>

            <!-- Carte 2 -->
            <div class="ui-card p-4 flex items-start gap-4 transition-all duration-200 hover:border-[var(--brand-primary)] hover:shadow-md cursor-pointer">
              <div class="p-2.5 rounded-[var(--radius-brand-md)] bg-[var(--brand-primary)]/10 text-[var(--brand-primary)]">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-6 h-6">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285z" />
                </svg>
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center justify-between gap-2">
                  <h4 class="font-display font-bold text-sm text-[var(--text-primary)]">Consentements</h4>
                  <span class="text-[9px] font-bold px-1.5 py-0.5 rounded-[var(--radius-brand-xs)] bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400 uppercase tracking-wide">
                    Bientôt
                  </span>
                </div>
                <p class="text-xs text-[var(--text-secondary)] mt-1">Gérez le partage de vos données de santé.</p>
              </div>
            </div>

            <!-- Carte 3 -->
            <div class="ui-card p-4 flex items-start gap-4 transition-all duration-200 hover:border-[var(--brand-primary)] hover:shadow-md cursor-pointer">
              <div class="p-2.5 rounded-[var(--radius-brand-md)] bg-[var(--brand-primary)]/10 text-[var(--brand-primary)]">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-6 h-6">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center justify-between gap-2">
                  <h4 class="font-display font-bold text-sm text-[var(--text-primary)]">Sécurité & Audit</h4>
                  <span class="text-[9px] font-bold px-1.5 py-0.5 rounded-[var(--radius-brand-xs)] bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400 uppercase tracking-wide">
                    Bientôt
                  </span>
                </div>
                <p class="text-xs text-[var(--text-secondary)] mt-1">Suivez les accès et les consultations de votre DPU.</p>
              </div>
            </div>
          </div>

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
