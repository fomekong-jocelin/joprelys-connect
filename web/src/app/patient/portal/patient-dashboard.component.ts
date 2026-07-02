import { Component, inject, OnInit, signal } from '@angular/core';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { PatientProfileCardComponent } from './components/patient-profile-card.component';
import { PatientVisitsListComponent } from './components/patient-visits-list.component';
import { PatientPortalMeResponse, PatientPortalService } from './services/patient-portal.service';
import { PatientConsentsListComponent } from './components/patient-consents-list.component';
import { PatientAuditListComponent } from './components/patient-audit-list.component';

@Component({
  selector: 'app-patient-dashboard',
  standalone: true,
  imports: [AppShellComponent, PatientProfileCardComponent, PatientVisitsListComponent, PatientConsentsListComponent, PatientAuditListComponent],
  template: `
    <app-shell>
      <div class="app-container py-6 flex flex-col gap-5">

        <!-- Header -->
        <div class="flex flex-col gap-0.5">
          <h1 class="font-display text-2xl font-extrabold" style="color: var(--text-primary)">
            @if (patientData()) {
              Bonjour, {{ (patientData()!.fullName.split(' ')[0]) || patientData()!.fullName }} 👋
            } @else {
              Mon Espace Santé
            }
          </h1>
          <p class="text-xs text-[var(--text-secondary)]">Votre espace santé connecté Joprelys.</p>
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

          <!-- Bannière profil compact (mobile-first) -->
          <div class="ui-card p-4 flex items-center gap-4">
            <div class="ui-avatar w-12 h-12 text-lg flex items-center justify-center font-bold shrink-0">
              {{ patientData()!.fullName.charAt(0) }}
            </div>
            <div class="min-w-0 flex-1">
              <p class="font-display font-bold text-sm text-[var(--text-primary)] truncate">{{ patientData()!.fullName }}</p>
              <p class="text-[11px] text-[var(--text-muted)] font-mono truncate">N° {{ patientData()!.globalPatientNumber }}</p>
            </div>
            <!-- Infos clés masquées sur très petit écran, visibles à partir de sm -->
            <div class="hidden sm:flex items-center gap-4 text-xs text-[var(--text-secondary)]">
              <span class="hidden md:block">{{ patientData()!.birthDate }}</span>
              <span>{{ patientData()!.phone }}</span>
            </div>
          </div>

          <!-- Cartes de navigation : scroll horizontal sur mobile, grille 3 col sur md+ -->
          <div class="flex gap-3 overflow-x-auto pb-1 -mx-4 px-4 md:mx-0 md:px-0 md:grid md:grid-cols-3 snap-x snap-mandatory">
            <!-- Ordonnances -->
            <div
              (click)="activeTab.set('visits')"
              [class.border-[var(--brand-primary)]]="activeTab() === 'visits'"
              class="ui-card p-3.5 flex items-center gap-3 transition-all duration-200 hover:border-[var(--brand-primary)] hover:shadow-sm cursor-pointer snap-start shrink-0 w-[220px] md:w-auto"
            >
              <div class="p-2 rounded-[var(--radius-brand-sm)] bg-[var(--brand-primary)]/10 text-[var(--brand-primary)] shrink-0">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
                </svg>
              </div>
              <div class="min-w-0">
                <div class="flex items-center gap-1.5">
                  <span class="font-display font-bold text-sm text-[var(--text-primary)] truncate">Ordonnances</span>
                  <span class="text-[10px] font-bold px-1.5 py-0.5 rounded-[var(--radius-brand-xs)] bg-[var(--brand-primary)]/10 text-[var(--brand-primary)] shrink-0">
                    {{ patientData()?.consultations?.length || 0 }}
                  </span>
                </div>
                <p class="text-[11px] text-[var(--text-muted)] truncate">Documents médicaux</p>
              </div>
            </div>

            <!-- Consentements -->
            <div
              (click)="activeTab.set('consents')"
              [class.border-[var(--brand-primary)]]="activeTab() === 'consents'"
              class="ui-card p-3.5 flex items-center gap-3 transition-all duration-200 hover:border-[var(--brand-primary)] hover:shadow-sm cursor-pointer snap-start shrink-0 w-[220px] md:w-auto"
            >
              <div class="p-2 rounded-[var(--radius-brand-sm)] bg-[var(--brand-primary)]/10 text-[var(--brand-primary)] shrink-0">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285z" />
                </svg>
              </div>
              <div class="min-w-0">
                <span class="font-display font-bold text-sm text-[var(--text-primary)]">Consentements</span>
                <p class="text-[11px] text-[var(--text-muted)] truncate">Partage de données</p>
              </div>
            </div>

            <!-- Sécurité & Audit -->
            <div
              (click)="activeTab.set('audit')"
              [class.border-[var(--brand-primary)]]="activeTab() === 'audit'"
              class="ui-card p-3.5 flex items-center gap-3 transition-all duration-200 hover:border-[var(--brand-primary)] hover:shadow-sm cursor-pointer snap-start shrink-0 w-[220px] md:w-auto"
            >
              <div class="p-2 rounded-[var(--radius-brand-sm)] bg-[var(--brand-primary)]/10 text-[var(--brand-primary)] shrink-0">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </div>
              <div class="min-w-0">
                <span class="font-display font-bold text-sm text-[var(--text-primary)]">Sécurité & Audit</span>
                <p class="text-[11px] text-[var(--text-muted)] truncate">Accès à votre DPU</p>
              </div>
            </div>
          </div>

          <!-- Contenu principal : plein écran sur mobile, 3 colonnes sur lg -->
          <div class="grid grid-cols-1 lg:grid-cols-3 gap-5 items-start">
            <!-- Profil détaillé : masqué sur mobile (déjà en bannière), visible sur lg -->
            <div class="hidden lg:block lg:col-span-1">
              <app-patient-profile-card [patient]="patientData()!" />
            </div>
            <!-- Contenu onglet actif : plein écran mobile, 2/3 desktop -->
            <div class="col-span-1 lg:col-span-2">
              @if (activeTab() === 'visits') {
                <app-patient-visits-list
                  [consultations]="patientData()!.consultations"
                  (download)="onDownloadDocument($event)"
                />
              } @else if (activeTab() === 'consents') {
                <app-patient-consents-list />
              } @else if (activeTab() === 'audit') {
                <app-patient-audit-list />
              }
            </div>
          </div>

          <!-- Profil étendu sur mobile uniquement (en bas, dépliable) -->
          <div class="lg:hidden">
            <details class="ui-card">
              <summary class="p-4 font-display font-bold text-sm text-[var(--text-primary)] cursor-pointer flex items-center justify-between gap-2">
                <span>Mon profil complet</span>
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4 text-[var(--text-muted)]">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                </svg>
              </summary>
              <div class="px-4 pb-4">
                <app-patient-profile-card [patient]="patientData()!" />
              </div>
            </details>
          </div>
        }
      </div>
    </app-shell>
  `
})
export class PatientDashboardComponent implements OnInit {
  private readonly portalService = inject(PatientPortalService);

  readonly activeTab = signal<'visits' | 'consents' | 'audit'>('visits');
  readonly patientData = signal<PatientPortalMeResponse | null>(null);
  readonly isLoading = signal(false);
  readonly error = signal('');
  readonly expandedConsultations = signal<Record<string, boolean>>({});

  toggleConsultation(id: string): void {
    this.expandedConsultations.update(prev => ({ ...prev, [id]: !prev[id] }));
  }

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
