import { Component, computed, inject, input, OnInit, OnDestroy, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { PatientApiService } from './patient-api.service';
import { ActivePatientService } from './active-patient.service';
import { AuthTokenStorageService } from '../auth/auth-token-storage.service';
import { VisitApiService } from '../visit/visit-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { Patient, PatientAllergy } from './patient.models';
import { CreateVisitRequest, Visit } from '../visit/visit.models';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { AlertComponent } from '../shared/ui/alert.component';
import { ButtonComponent } from '../shared/ui/button.component';
import { CardComponent } from '../shared/ui/card.component';
import { StaffApiService } from '../clinic/staff/staff-api.service';
import { StaffMember } from '../clinic/staff/staff.models';

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    AppShellComponent,
    AlertComponent,
    ButtonComponent,
    CardComponent,
  ],
  styles: [`
    /* Scrollbar thin and elegant on mobile */
    .mobile-tab-scroll::-webkit-scrollbar {
      height: 4px;
    }
    .mobile-tab-scroll::-webkit-scrollbar-track {
      background: transparent;
    }
    .mobile-tab-scroll::-webkit-scrollbar-thumb {
      background: var(--app-border);
      border-radius: 2px;
    }
    .mobile-tab-scroll {
      scrollbar-width: thin;
      scrollbar-color: var(--app-border) transparent;
      -webkit-overflow-scrolling: touch;
    }
  `],
  template: `
    <app-shell>
      <div class="app-container py-8">
        @if (loading()) {
          <div class="py-12 text-center">
            <div class="inline-block w-8 h-8 rounded-full border-4 border-indigo-200 border-t-indigo-600 animate-spin"></div>
            <p class="mt-2 text-sm font-bold text-[var(--text-muted)]">Chargement du dossier patient...</p>
          </div>
        } @else if (error(); as err) {
          <app-ui-card>
            <app-ui-alert tone="error" class="mb-6">{{ err }}</app-ui-alert>
            <div class="flex justify-end">
              <app-ui-button variant="secondary" (pressed)="goBack()">
                Retour
              </app-ui-button>
            </div>
          </app-ui-card>
        } @else if (consentRequiredPatient(); as consentPatient) {
          <!-- Consent/Break-Glass UI -->
          <app-ui-card>
            <div class="max-w-md mx-auto text-center py-8 flex flex-col items-center gap-6">
              <div class="p-4 rounded-full bg-rose-50 dark:bg-rose-950/20 border border-rose-100 dark:border-rose-900/30 text-rose-600 dark:text-rose-400">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-12 h-12">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z" />
                </svg>
              </div>

              <div>
                <h3 class="font-display font-black text-xl text-[var(--text-primary)]">
                  Consentement d'accès requis
                </h3>
                <p class="text-xs text-[var(--text-muted)] mt-1 whitespace-nowrap">
                  DPU : <span class="font-mono font-bold text-[var(--brand-info-text)] dark:text-indigo-400">{{ consentPatient.globalPatientNumber }}</span>
                </p>
                <p class="text-sm text-[var(--text-secondary)] mt-4 leading-relaxed">
                  Le patient <strong>{{ consentPatient.fullName }}</strong> n'a pas accordé d'accès DPU actif pour votre clinique.
                  Vous ne pouvez pas consulter ses données médicales.
                </p>
              </div>

              @if (emergencyError(); as err) {
                <app-ui-alert tone="error" class="w-full text-left">{{ err }}</app-ui-alert>
              }

              <div class="w-full border-t border-[var(--app-border)]/80 pt-6 flex flex-col gap-4">
                <div class="text-left w-full">
                  <label for="emergency-reason" class="ui-label block mb-2">Justification d'accès d'urgence (obligatoire)</label>
                  <textarea
                    id="emergency-reason"
                    class="ui-input w-full min-h-[80px]"
                    placeholder="Ex: Patient inconscient, arrêt cardiaque, accident nécessitant l'accès immédiat au dossier médical..."
                    [value]="emergencyReason()"
                    (input)="emergencyReason.set($any($event.target).value)"
                    style="color: var(--text-primary); background-color: var(--bg-card);"
                  ></textarea>
                </div>

                <div class="flex items-center justify-between gap-3 w-full">
                  <app-ui-button variant="secondary" (pressed)="goBack()" class="grow">
                    Annuler
                  </app-ui-button>
                  <app-ui-button
                    variant="primary"
                    class="grow"
                    [disabled]="!emergencyReason().trim() || emergencyLoading()"
                    (pressed)="triggerEmergencyAccess()"
                  >
                    {{ emergencyLoading() ? 'Activation...' : 'Procédure d\'urgence (Brise-Glace)' }}
                  </app-ui-button>
                </div>
              </div>
            </div>
          </app-ui-card>
        } @else if (patient(); as p) {
          <app-ui-card>
            <div class="space-y-6">
              
              <!-- En-tête : Nom du Patient, Numéros & Actions principales -->
              <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center pb-4 border-b border-[var(--app-border)]/80 gap-4">
                <div>
                  <div class="flex items-center gap-3">
                    <h2 class="text-xl font-black text-[var(--text-primary)]">{{ p.fullName }}</h2>
                    <span class="inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-extrabold bg-[var(--brand-success-subtle)] text-[var(--brand-success-text)] dark:bg-green-950/30 dark:text-green-300 uppercase tracking-wider">
                      {{ p.status }}
                    </span>
                  </div>
                  <p class="text-xs text-[var(--text-muted)] mt-1 flex flex-wrap gap-x-2 gap-y-1">
                    <span class="whitespace-nowrap">DPU: <strong class="font-mono font-extrabold text-[var(--brand-info-text)] dark:text-indigo-400">{{ p.globalPatientNumber }}</strong></span>
                    <span class="text-slate-300 dark:text-[var(--text-secondary)] hidden sm:inline">|</span>
                    <span class="whitespace-nowrap">Etablissement: <strong class="font-mono font-bold text-[var(--text-secondary)]">{{ p.localPatientNumber }}</strong></span>
                  </p>
                </div>
                
                <div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 w-full sm:w-auto">
                  <app-ui-button variant="secondary" (pressed)="goBack()" class="grow sm:grow-0 text-xs">
                    Retour
                  </app-ui-button>

                  @if (canDownloadSummary()) {
                    <button
                      (click)="downloadSummaryPdf()"
                      class="grow sm:grow-0 text-xs px-3 py-1.5 rounded-lg border border-[var(--app-border)] text-[var(--text-secondary)] hover:bg-[var(--app-surface-muted)] dark:hover:bg-slate-800/50 flex items-center justify-center gap-1.5 font-bold transition-all cursor-pointer shadow-xs"
                      title="Télécharger la synthèse médicale (PDF)"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4 text-brand-primary">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3" />
                      </svg>
                      <span>Synthèse PDF</span>
                    </button>
                  }
                  
                  @if (canStartConsultation()) {
                    <app-ui-button variant="primary" (pressed)="goToConsultation()" class="grow sm:grow-0 text-xs">
                      Démarrer la consultation
                    </app-ui-button>
                  } @else if (canAdmit()) {
                    <app-ui-button variant="primary" (pressed)="openModal()" class="grow sm:grow-0 text-xs">
                      Ouvrir une visite
                    </app-ui-button>
                  }
                </div>
              </div>

              @if (p.emergencyAccessActive) {
                <div class="p-4 rounded-sm bg-rose-50 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-800/40 text-rose-800 dark:text-rose-300 text-sm font-bold flex items-center gap-3 animate-pulse">
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="w-5 h-5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                  </svg>
                  <span>Procédure d'urgence "Brise-Glace" active : accès temporaire tracé dans le journal d'audit de sécurité.</span>
                </div>
              }

              @if (criticalAllergies().length > 0) {
                <div class="p-4 rounded-sm bg-[var(--brand-danger-subtle)] border border-red-200 dark:border-red-800/40 text-red-800 dark:text-[var(--brand-danger-text)] text-sm font-bold flex items-center gap-3">
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="w-5 h-5 animate-bounce">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                  </svg>
                  <div>
                    <span>Attention : allergies critiques ou sévères détectées pour ce patient :</span>
                    <span class="ml-1 font-extrabold">{{ criticalAllergiesSubstances() }}</span>
                  </div>
                </div>
              }

              <!-- Mobile Tab Navigation (visible only on mobile/tablet) -->
              <div class="block md:hidden border-b border-[var(--app-border)]/80 mb-4 overflow-x-auto mobile-tab-scroll">
                <nav class="flex space-x-6 pb-2 min-w-max px-1">
                  <a
                    [routerLink]="['/patients', p.id, 'profile']"
                    routerLinkActive="border-[var(--brand-primary)] text-[var(--brand-primary)] font-bold active-mobile-tab"
                    [routerLinkActiveOptions]="{ exact: true }"
                    class="border-b-2 border-transparent pb-2 text-sm font-semibold text-[var(--text-muted)] hover:text-slate-900 dark:hover:text-slate-100 no-underline transition-all"
                  >
                    {{ i18n.t('menu.patientDetail.profile') || 'Profil' }}
                  </a>
                  <a
                    [routerLink]="['/patients', p.id, 'consultations']"
                    routerLinkActive="border-[var(--brand-primary)] text-[var(--brand-primary)] font-bold active-mobile-tab"
                    class="border-b-2 border-transparent pb-2 text-sm font-semibold text-[var(--text-muted)] hover:text-slate-900 dark:hover:text-slate-100 no-underline transition-all"
                  >
                    {{ i18n.t('menu.patientDetail.consultations') || 'Consultations' }}
                  </a>
                  <a
                    [routerLink]="['/patients', p.id, 'lab-orders']"
                    routerLinkActive="border-[var(--brand-primary)] text-[var(--brand-primary)] font-bold active-mobile-tab"
                    class="border-b-2 border-transparent pb-2 text-sm font-semibold text-[var(--text-muted)] hover:text-slate-900 dark:hover:text-slate-100 no-underline transition-all"
                  >
                    {{ i18n.t('menu.patientDetail.labOrders') || 'Analyses' }}
                  </a>
                  <a
                    [routerLink]="['/patients', p.id, 'hospitalizations']"
                    routerLinkActive="border-[var(--brand-primary)] text-[var(--brand-primary)] font-bold active-mobile-tab"
                    class="border-b-2 border-transparent pb-2 text-sm font-semibold text-[var(--text-muted)] hover:text-slate-900 dark:hover:text-slate-100 no-underline transition-all"
                  >
                    {{ i18n.t('menu.patientDetail.hospitalization') || 'Hospitalisations' }}
                  </a>
                  @if (canViewAudit()) {
                    <a
                      [routerLink]="['/patients', p.id, 'audit-trail']"
                      routerLinkActive="border-[var(--brand-primary)] text-[var(--brand-primary)] font-bold active-mobile-tab"
                      class="border-b-2 border-transparent pb-2 text-sm font-semibold text-[var(--text-muted)] hover:text-slate-900 dark:hover:text-slate-100 no-underline transition-all"
                    >
                      {{ i18n.t('menu.patientDetail.audit') || 'Sécurité/Audit' }}
                    </a>
                  }
                </nav>
              </div>

              <!-- Child Tab Views direct rendering without nested tabs -->
              <div class="mt-2">
                <router-outlet></router-outlet>
              </div>

            </div>
          </app-ui-card>
        }
      </div>
    </app-shell>

    <!-- Admission Modal Dialogue -->
    @if (showVisitModal) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 animate-fade-in">
        <div class="bg-[var(--app-surface)] border border-[var(--app-border)]/80 rounded-2xl max-w-md w-full shadow-2xl p-6 relative">
          <!-- Close button -->
          <button (click)="closeModal()" class="absolute top-4 right-4 text-[var(--text-muted)] hover:text-[var(--text-secondary)] dark:hover:text-[var(--text-secondary)] cursor-pointer">
            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>

          <!-- Header -->
          <h3 class="font-display font-bold text-lg text-[var(--text-primary)] mb-2">Admettre le Patient</h3>
          <p class="text-xs text-[var(--text-muted)] mb-6">
            Ouvrir une visite clinique pour <strong>{{ patient()?.fullName }}</strong> et l'orienter.
          </p>

          @if (visitError) {
            <div class="p-3 bg-[var(--brand-danger-subtle)] border border-[var(--brand-danger-border)] rounded-xl text-xs text-[var(--brand-danger-text)] font-semibold mb-4 leading-relaxed">
              {{ visitError }}
            </div>
          }

          <!-- Form -->
          <div class="space-y-4">
            <div class="space-y-1.5">
              <label class="ui-label">Motif de visite <span class="text-[var(--brand-danger)]">*</span></label>
              <textarea
                [(ngModel)]="visitReason"
                placeholder="Ex: Fièvre et toux sèche depuis 2 jours"
                class="ui-textarea min-h-[80px] p-3 text-sm focus:border-brand-primary transition-colors"
                [disabled]="isSubmitting()"
              ></textarea>
            </div>

            <div class="space-y-1.5">
              <label class="ui-label">Orientation <span class="text-[var(--brand-danger)]">*</span></label>
              <select
                [(ngModel)]="visitOrientation"
                (ngModelChange)="onOrientationChange()"
                class="ui-select focus:border-brand-primary transition-colors"
                [disabled]="isSubmitting()"
              >
                <option value="" disabled selected>Choisir une orientation...</option>
                <option value="Médecine générale">Médecine générale</option>
                <option value="Tri / Urgences">Tri / Urgences</option>
                <option value="Pédiatrie">Pédiatrie</option>
                <option value="Gynécologie">Gynécologie</option>
                <option value="Pharmacie">Pharmacie</option>
                <option value="Autre">Autre</option>
              </select>
            </div>

            <div class="space-y-1.5">
              <label class="ui-label">Service clinique</label>
              <select
                [(ngModel)]="selectedVisitService"
                (ngModelChange)="onServiceChange($event)"
                class="ui-select focus:border-brand-primary transition-colors"
                [disabled]="isSubmitting()"
              >
                <option value="">Sélectionner un service...</option>
                @for (d of getDepartments(); track d) {
                  <option [value]="d">{{ d }}</option>
                }
                <option value="Autre">Autre (Saisir...)</option>
              </select>

              @if (selectedVisitService === 'Autre') {
                <input
                  type="text"
                  [(ngModel)]="customVisitService"
                  (ngModelChange)="onCustomServiceInput($event)"
                  placeholder="Saisir le nom du service..."
                  class="ui-input w-full mt-2 p-3 text-sm focus:border-brand-primary transition-colors"
                  [disabled]="isSubmitting()"
                />
              }
            </div>

            <div class="space-y-1.5">
              <label class="ui-label">Praticien responsable</label>
              <select
                [(ngModel)]="visitMainPractitionerId"
                class="ui-select focus:border-brand-primary transition-colors"
                [disabled]="isSubmitting()"
              >
                <option value="">Sélectionner un praticien (optionnel)</option>
                @for (p of getFilteredPractitioners(); track p.id) {
                  <option [value]="p.id">{{ p.displayName }}</option>
                }
              </select>
            </div>

            <div class="space-y-1.5">
              <label class="ui-label">Date / Heure d'arrivée</label>
              <input
                type="datetime-local"
                [(ngModel)]="visitArrivalAt"
                class="ui-input w-full p-3 text-sm focus:border-brand-primary transition-colors"
                [disabled]="isSubmitting()"
              />
            </div>
          </div>

          <!-- Actions -->
          <div class="flex justify-end gap-3 mt-8">
            <app-ui-button variant="secondary" (pressed)="closeModal()" [disabled]="isSubmitting()">
              Annuler
            </app-ui-button>
            <app-ui-button variant="primary" (pressed)="submitVisit()" [disabled]="isSubmitting() || !visitReason || !visitOrientation">
              {{ submitLabel() }}
            </app-ui-button>
          </div>
        </div>
      </div>
    }
  `,
})
export class PatientDetailComponent implements OnInit, OnDestroy {
  // Inputs/Outputs kept for backward compatibility with specs and parents
  readonly patientInput = input<Patient | null>(null, { alias: 'patient' });
  readonly back = output<void>();

  private readonly tokenStorage = inject(AuthTokenStorageService);
  private readonly visitApi = inject(VisitApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly patientApi = inject(PatientApiService);
  private readonly activePatientService = inject(ActivePatientService);
  private readonly staffApi = inject(StaffApiService);
  readonly i18n = inject(I18nService);

  readonly loadedPatient = signal<Patient | null>(null);
  private routerSub?: any;
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  // Consent required state for Break-Glass
  readonly consentRequiredPatient = signal<Patient | null>(null);
  readonly emergencyReason = signal('');
  readonly emergencyLoading = signal(false);
  readonly emergencyError = signal<string | null>(null);

  // Computed patient that works either via input binding (for tests) or route loading
  readonly patient = computed(() => {
    const inputVal = this.patientInput();
    if (inputVal) return inputVal;
    return this.loadedPatient();
  });

  showVisitModal = false;
  isSubmitting = signal(false);
  visitReason = '';
  visitOrientation = '';
  visitService = '';
  selectedVisitService = '';
  customVisitService = '';
  visitMainPractitionerId = '';
  visitArrivalAt = '';
  visitError = '';

  activeVisit = signal<Visit | null>(null);
  criticalAllergies = signal<PatientAllergy[]>([]);
  criticalAllergiesSubstances = computed(() => 
    this.criticalAllergies().map(a => a.substance).join(', ')
  );
  readonly staffList = signal<StaffMember[]>([]);

  readonly session = this.tokenStorage.session;
  readonly submitLabel = computed(() => this.isSubmitting() ? 'Enregistrement...' : "Valider l'admission");

  readonly canAdmit = computed(() => {
    const role = this.session()?.role;
    if (!role) return false;
    const roles = role.split(',').map((r) => r.trim());
    const allowedRoles = ['AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE'];
    return roles.some((r) => allowedRoles.includes(r)) && this.activeVisit() === null;
  });

  readonly canStartConsultation = computed(() => {
    const role = this.session()?.role;
    if (!role) return false;
    const roles = role.split(',').map((r) => r.trim());
    return roles.some((r) => r === 'MEDECIN' || r === 'ADMIN_CLINIQUE') && this.activeVisit() !== null;
  });

  readonly canViewAudit = computed(() => {
    const role = this.session()?.role;
    if (!role) return false;
    const roles = role.split(',').map((r) => r.trim());
    return roles.some((r) => r === 'MEDECIN' || r === 'ADMIN_CLINIQUE' || r === 'AUDITEUR');
  });

  readonly canDownloadSummary = computed(() => {
    const role = this.session()?.role;
    if (!role) return false;
    const roles = role.split(',').map((r) => r.trim());
    const allowedRoles = ['MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE'];
    return roles.some((r) => allowedRoles.includes(r));
  });

  downloadSummaryPdf(): void {
    const currentPatient = this.patient();
    if (!currentPatient) return;
    this.patientApi.downloadSummaryPdf(currentPatient.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `patient-summary-${currentPatient.id}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Error downloading summary pdf', err);
        alert('Erreur lors du téléchargement du PDF de synthèse');
      }
    });
  }

  ngOnInit(): void {
    // Load staff list for practitioners dropdown
    this.staffApi.list().subscribe({
      next: (list) => this.staffList.set(list.filter(s => s.enabled)),
      error: () => {}
    });

    // If navigation details were supplied via Router state, use them immediately
    const statePatient = window.history.state?.patient as Patient | undefined;
    if (statePatient) {
      this.loadedPatient.set(statePatient);
      this.initializeDetails();
    }

    // Subscribe to route params to fetch or refresh patient
    this.route.params.subscribe(params => {
      const id = params['id'];
      if (id) {
        if (!this.patientInput()) {
          this.loadPatient(id);
        } else {
          this.initializeDetails();
        }
      } else if (this.patientInput()) {
        this.initializeDetails();
      }
    });

    // Scroll active mobile tab into view on route change
    this.routerSub = this.router.events.subscribe(() => {
      this.scrollActiveTabIntoView();
    });
    this.scrollActiveTabIntoView();
  }

  ngOnDestroy(): void {
    this.activePatientService.patient.set(null);
    this.routerSub?.unsubscribe();
  }

  scrollActiveTabIntoView(): void {
    setTimeout(() => {
      const activeTabEl = document.querySelector('.active-mobile-tab');
      if (activeTabEl) {
        activeTabEl.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
      }
    }, 150);
  }

  loadPatient(id: string): void {
    // If we already have patient info in state, don't show full spinner unless needed
    if (!this.patient()) {
      this.loading.set(true);
    }
    this.error.set(null);
    this.consentRequiredPatient.set(null);

    this.patientApi.getById(id).subscribe({
      next: (fullPatient) => {
        this.loadedPatient.set(fullPatient);
        this.loading.set(false);
        this.initializeDetails();
      },
      error: (err) => {
        this.loading.set(false);
        if (err && err.status === 403) {
          if (!this.patient()) {
            const fallbackPatient: Patient = {
              id: id,
              fullName: err.error?.fullName || 'Patient',
              globalPatientNumber: err.error?.globalPatientNumber || 'Inconnu',
              localPatientNumber: 'Inconnu',
              gender: 'Inconnu',
              birthDate: '',
              phone: '',
              city: '',
              status: 'INACTIVE',
              organizationId: '',
              createdAt: '',
              updatedAt: ''
            };
            this.loadedPatient.set(fallbackPatient);
          }
          this.consentRequiredPatient.set(this.patient());
        } else {
          this.error.set(err.error?.detail || err.error?.title || "Impossible de charger le dossier patient.");
        }
      }
    });
  }

  initializeDetails(): void {
    const patientObj = this.patient();
    if (!patientObj) return;

    this.activePatientService.patient.set(patientObj);

    const role = this.session()?.role;
    const canSeeActiveVisit = ['MEDECIN', 'ADMIN_CLINIQUE', 'AGENT_ACCUEIL', 'INFIRMIER'].includes(role || '');
    if (canSeeActiveVisit) {
      this.visitApi.getActiveVisits().subscribe({
        next: (visits) => {
          const found = visits.find(v => v.patientId === patientObj.id) ?? null;
          this.activeVisit.set(found);
        },
        error: () => { /* silencieux */ }
      });
    }

    this.patientApi.getAllergies(patientObj.id).subscribe({
      next: (allergies) => {
        const critical = allergies.filter(a => a.status === 'ACTIVE' && (a.severity === 'CRITICAL' || a.severity === 'HIGH'));
        this.criticalAllergies.set(critical);
      },
      error: () => { /* silencieux */ }
    });
  }

  triggerEmergencyAccess(): void {
    const patientObj = this.consentRequiredPatient();
    if (!patientObj || !this.emergencyReason().trim()) return;

    this.emergencyLoading.set(true);
    this.emergencyError.set(null);

    this.patientApi.triggerEmergencyAccess(patientObj.id, this.emergencyReason().trim()).subscribe({
      next: () => {
        this.emergencyLoading.set(false);
        this.consentRequiredPatient.set(null);
        this.emergencyReason.set('');
        this.loadPatient(patientObj.id);
      },
      error: (err) => {
        this.emergencyLoading.set(false);
        this.emergencyError.set(err.error?.detail || err.error?.title || "Erreur lors du déclenchement de l'accès d'urgence.");
      }
    });
  }

  goBack(): void {
    this.back.emit();
    this.router.navigate(['/patients']);
  }

  goToConsultation(): void {
    const visit = this.activeVisit();
    if (visit) {
      this.router.navigate(['/clinic/consultation', visit.id]);
    }
  }

  getFilteredPractitioners(): StaffMember[] {
    const list = this.staffList();
    const service = this.visitService;
    const orientation = this.visitOrientation;
    
    const hasRole = (member: StaffMember, role: string) => {
      if (!member.role) return false;
      return member.role.split(',').map(r => r.trim()).includes(role);
    };

    // Filter by selected service first if set
    if (service) {
      const filteredByService = list.filter(s => 
        s.department?.toLowerCase() === service.toLowerCase() &&
        (hasRole(s, 'MEDECIN') || hasRole(s, 'INFIRMIER') || hasRole(s, 'PHARMACIEN') || hasRole(s, 'BIOLOGISTE'))
      );
      if (filteredByService.length > 0) {
        return filteredByService;
      }
    }

    // Fallback to orientation
    if (orientation === 'Médecine générale' || orientation === 'Pédiatrie' || orientation === 'Gynécologie') {
      const filtered = list.filter(s => hasRole(s, 'MEDECIN'));
      if (filtered.length > 0) return filtered;
    } else if (orientation === 'Pharmacie') {
      const filtered = list.filter(s => hasRole(s, 'PHARMACIEN'));
      if (filtered.length > 0) return filtered;
    } else if (orientation === 'Tri / Urgences') {
      const filtered = list.filter(s => hasRole(s, 'INFIRMIER') || hasRole(s, 'MEDECIN'));
      if (filtered.length > 0) return filtered;
    }
    
    return list.filter(s => hasRole(s, 'MEDECIN') || hasRole(s, 'INFIRMIER') || hasRole(s, 'PHARMACIEN') || hasRole(s, 'BIOLOGISTE'));
  }

  onOrientationChange(): void {
    // Sync service with orientation if matched
    if (this.visitOrientation) {
      const matchedDept = this.getDepartments().find(d => d.toLowerCase() === this.visitOrientation.toLowerCase());
      if (matchedDept) {
        this.selectedVisitService = matchedDept;
        this.visitService = matchedDept;
        this.customVisitService = '';
      } else if (this.visitOrientation === 'Tri / Urgences') {
        const urgenDept = this.getDepartments().find(d => d.toLowerCase().includes('urgence'));
        if (urgenDept) {
          this.selectedVisitService = urgenDept;
          this.visitService = urgenDept;
          this.customVisitService = '';
        }
      }
    }
    this.updateAvailablePractitioners();
  }

  onServiceChange(val: string): void {
    this.selectedVisitService = val;
    if (val !== 'Autre') {
      this.visitService = val;
      this.customVisitService = '';
    } else {
      this.visitService = this.customVisitService;
    }
    this.updateAvailablePractitioners();
  }

  onCustomServiceInput(val: string): void {
    this.customVisitService = val;
    this.visitService = val;
    this.updateAvailablePractitioners();
  }

  updateAvailablePractitioners(): void {
    const available = this.getFilteredPractitioners();
    if (!available.some(p => p.id === this.visitMainPractitionerId)) {
      this.visitMainPractitionerId = '';
    }
  }

  getDepartments(): string[] {
    const defaultDepts = [
      'Médecine générale',
      'Pédiatrie',
      'Gynécologie',
      'Urgences',
      'Pharmacie',
      'Laboratoire',
      'Cardiologie'
    ];
    const depts = new Set<string>(defaultDepts);
    this.staffList().forEach(member => {
      if (member.department) {
        depts.add(member.department);
      }
    });
    return Array.from(depts).sort();
  }

  openModal(): void {
    this.showVisitModal = true;
    this.visitReason = '';
    this.visitOrientation = '';
    this.visitService = '';
    this.selectedVisitService = '';
    this.customVisitService = '';
    this.visitMainPractitionerId = '';
    
    const now = new Date();
    const offsetMs = now.getTimezoneOffset() * 60000;
    const localISODate = new Date(now.getTime() - offsetMs).toISOString().slice(0, 16);
    this.visitArrivalAt = localISODate;
    
    this.visitError = '';
  }

  closeModal(): void {
    if (!this.isSubmitting()) {
      this.showVisitModal = false;
    }
  }

  submitVisit(): void {
    const patientObj = this.patient();
    if (!patientObj || !this.visitReason || !this.visitOrientation || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);
    this.visitError = '';

    const request: CreateVisitRequest = {
      patientId: patientObj.id,
      reason: this.visitReason,
      orientation: this.visitOrientation,
      service: this.visitService?.trim() || undefined,
      mainPractitionerId: this.visitMainPractitionerId?.trim() || undefined,
      arrivalAt: this.visitArrivalAt ? new Date(this.visitArrivalAt).toISOString() : undefined
    };

    this.visitApi.create(request).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.showVisitModal = false;
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.visitError = err.error?.detail || err.error?.title || 'Une erreur est survenue lors de l\'ouverture de la visite.';
      }
    });
  }
}
