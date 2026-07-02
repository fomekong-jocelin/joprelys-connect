import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { ButtonComponent } from '../shared/ui/button.component';
import { CardComponent } from '../shared/ui/card.component';
import { Patient } from './patient.models';
import { AuthTokenStorageService } from '../auth/auth-token-storage.service';
import { VisitApiService } from '../visit/visit-api.service';
import { Visit } from '../visit/visit.models';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { SlicePipe } from '@angular/common';
import { ConsultationApiService } from '../consultation/consultation-api.service';
import { Consultation } from '../consultation/consultation.models';
import { I18nService } from '../core/i18n/i18n.service';

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  imports: [ButtonComponent, CardComponent, FormsModule, SlicePipe],
  template: `
    <app-ui-card [title]="patient().fullName">
      <div class="space-y-6">
        
        <!-- Headers with numbers -->
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 border-b pb-4" style="border-color: var(--border-color)">
          <div>
            <span class="ui-label font-bold block">Dossier Patient Unique (DPU)</span>
            <span class="text-lg font-black tracking-wider text-indigo-500 dark:text-indigo-400">{{ patient().globalPatientNumber }}</span>
          </div>
          <div>
            <span class="ui-label font-bold block">N° Local Etablissement</span>
            <span class="text-lg font-black tracking-wider text-emerald-500 dark:text-emerald-400">{{ patient().localPatientNumber }}</span>
          </div>
          <div>
            <span class="ui-label font-bold block">Statut du Dossier</span>
            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200 mt-1">
              {{ patient().status }}
            </span>
          </div>
        </div>

        <!-- Section 1: Informations Administratives -->
        <div>
          <h3 class="text-sm font-extrabold uppercase tracking-wider mb-3" style="color: var(--text-muted)">
            Informations Administratives
          </h3>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <div>
              <span class="ui-label block">Sexe</span>
              <span class="font-bold" style="color: var(--text-primary)">{{ patient().gender }}</span>
            </div>
            <div>
              <span class="ui-label block">Date de naissance (Âge)</span>
              <span class="font-bold" style="color: var(--text-primary)">
                {{ patient().birthDate }} ({{ age() }} ans)
              </span>
            </div>
            <div>
              <span class="ui-label block">Téléphone</span>
              <span class="font-bold" style="color: var(--text-primary)">{{ patient().phone }}</span>
            </div>
            <div>
              <span class="ui-label block">Ville</span>
              <span class="font-bold" style="color: var(--text-primary)">{{ patient().city }}</span>
            </div>
            <div>
              <span class="ui-label block">Quartier / District</span>
              <span class="font-bold" style="color: var(--text-primary)">{{ patient().district || '-' }}</span>
            </div>
            <div>
              <span class="ui-label block">Adresse géographique</span>
              <span class="font-bold" style="color: var(--text-primary)">{{ patient().address || '-' }}</span>
            </div>
          </div>
        </div>

        <!-- Section 2: Contact d'Urgence -->
        <div class="pt-4 border-t" style="border-color: var(--border-color)">
          <h3 class="text-sm font-extrabold uppercase tracking-wider mb-3" style="color: var(--text-muted)">
            Contact d'Urgence
          </h3>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <span class="ui-label block">Nom complet</span>
              <span class="font-bold" style="color: var(--text-primary)">{{ patient().emergencyContactName || '-' }}</span>
            </div>
            <div>
              <span class="ui-label block">Téléphone</span>
              <span class="font-bold" style="color: var(--text-primary)">{{ patient().emergencyContactPhone || '-' }}</span>
            </div>
          </div>
        </div>

        <!-- Section 3: Antécédents & Clinique -->
        <div class="pt-4 border-t" style="border-color: var(--border-color)">
          <h3 class="text-sm font-extrabold uppercase tracking-wider mb-3" style="color: var(--text-muted)">
            Dossier Clinique
          </h3>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div class="p-4 rounded-lg bg-rose-50 dark:bg-rose-950/20 border border-rose-100 dark:border-rose-900/30">
              <span class="ui-label block font-bold text-rose-800 dark:text-rose-300">Allergies signalées</span>
              <p class="mt-2 text-sm font-semibold text-rose-900 dark:text-rose-200 whitespace-pre-line">
                {{ patient().allergies || 'Aucune allergie signalée' }}
              </p>
            </div>
            <div class="p-4 rounded-lg bg-blue-50 dark:bg-blue-950/20 border border-blue-100 dark:border-blue-900/30">
              <span class="ui-label block font-bold text-blue-800 dark:text-blue-300">Antécédents médicaux</span>
              <p class="mt-2 text-sm font-semibold text-blue-900 dark:text-blue-200 whitespace-pre-line">
                {{ patient().medicalHistory || 'Aucun antécédent médical signalé' }}
              </p>
            </div>
          </div>
        </div>

        <!-- Section 4: Historique Médical -->
        <div class="pt-4 border-t" style="border-color: var(--border-color)">
          <button
            (click)="toggleHistory()"
            class="flex items-center justify-between w-full text-left group cursor-pointer"
          >
            <h3 class="text-sm font-extrabold uppercase tracking-wider" style="color: var(--text-muted)">
              Historique Médical
            </h3>
            <svg
              class="w-4 h-4 transition-transform duration-200"
              [class.rotate-180]="showHistory()"
              style="color: var(--text-muted)"
              fill="none" viewBox="0 0 24 24" stroke="currentColor"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
          </button>

          @if (showHistory()) {
            <div class="mt-4 space-y-3">
              @if (downloadError()) {
                <div class="p-3 bg-red-50 dark:bg-red-950/20 border border-red-100 dark:border-red-900/30 rounded-xl text-xs text-red-700 dark:text-red-300 font-semibold mb-3 leading-relaxed flex justify-between items-center">
                  <span>{{ downloadError() }}</span>
                  <button (click)="downloadError.set(null)" class="text-red-500 hover:text-red-700 cursor-pointer ml-2">
                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>
              }

              @if (isLoadingHistory()) {
                <div class="py-6 text-center">
                  <div class="inline-block w-5 h-5 rounded-full border-2 border-indigo-200 border-t-indigo-600 animate-spin"></div>
                  <p class="mt-2 text-xs font-semibold" style="color: var(--text-muted)">Chargement de l'historique...</p>
                </div>
              } @else if (consultationHistory().length === 0) {
                <div class="p-4 text-center border border-dashed border-slate-200 dark:border-slate-700 rounded-xl">
                  <p class="text-sm font-semibold" style="color: var(--text-muted)">Aucun antécédent de consultation enregistré.</p>
                </div>
              } @else {
                @for (consult of consultationHistory(); track consult.id) {
                  <div class="p-4 rounded-xl bg-slate-50 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-700/50 space-y-2">
                    <div class="flex items-start justify-between gap-2">
                      <div>
                        <span class="inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-extrabold bg-indigo-50 text-indigo-700 dark:bg-indigo-950/30 dark:text-indigo-300 uppercase tracking-wider">
                          {{ consult.visitNumber }}
                        </span>
                        <span class="ml-2 text-xs font-semibold" style="color: var(--text-muted)">
                          {{ consult.createdAt | slice:0:10 }}
                        </span>
                      </div>
                      <span class="text-xs font-semibold" style="color: var(--text-muted)">Dr. {{ consult.doctorName }}</span>
                    </div>
                    <div class="flex justify-between items-center pt-2 border-t border-slate-100/50 dark:border-slate-800/40 gap-4">
                      <p class="text-sm font-semibold" style="color: var(--text-primary)">
                        <span class="text-xs font-bold uppercase" style="color: var(--text-muted)">Diagnostic : </span>
                        {{ consult.diagnosis }}
                      </p>
                      <button 
                        (click)="downloadPdf(consult)"
                        class="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg text-xs font-extrabold bg-indigo-50 text-indigo-700 dark:bg-indigo-950/30 dark:text-indigo-300 hover:bg-indigo-100 dark:hover:bg-indigo-950/50 cursor-pointer transition-colors shrink-0"
                      >
                        <svg class="w-3.5 h-3.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                          <path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                        </svg>
                        {{ i18n.t('patients.downloadPdf') }}
                      </button>
                    </div>
                  </div>
                }
              }
            </div>
          }
        </div>

        <div class="flex justify-end gap-3 pt-4 border-t" style="border-color: var(--border-color)">
          <app-ui-button variant="secondary" (pressed)="back.emit()">
            Retour à la liste
          </app-ui-button>

          @if (canStartConsultation()) {
            <app-ui-button variant="primary" (pressed)="goToConsultation()">
              Démarrer la consultation
            </app-ui-button>
          } @else if (canAdmit()) {
            <app-ui-button variant="primary" (pressed)="openModal()">
              Ouvrir une visite
            </app-ui-button>
          }
        </div>

      </div>
    </app-ui-card>

    <!-- Admission Modal Dialogue -->
    @if (showVisitModal) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 animate-fade-in">
        <div class="bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800/80 rounded-2xl max-w-md w-full shadow-2xl p-6 relative">
          <!-- Close button -->
          <button (click)="closeModal()" class="absolute top-4 right-4 text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 cursor-pointer">
            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>

          <!-- Header -->
          <h3 class="font-display font-bold text-lg text-brand-night dark:text-white mb-2">Admettre le Patient</h3>
          <p class="text-xs text-slate-500 dark:text-slate-400 mb-6">
            Ouvrir une visite clinique pour <strong>{{ patient().fullName }}</strong> et l'orienter.
          </p>

          @if (visitError) {
            <div class="p-3 bg-red-50 dark:bg-red-950/20 border border-red-100 dark:border-red-900/30 rounded-xl text-xs text-red-700 dark:text-red-300 font-semibold mb-4 leading-relaxed">
              {{ visitError }}
            </div>
          }

          <!-- Form -->
          <div class="space-y-4">
            <div class="space-y-1.5">
              <label class="ui-label">Motif de visite <span class="text-red-500">*</span></label>
              <textarea 
                [(ngModel)]="visitReason"
                placeholder="Ex: Fièvre et toux sèche depuis 2 jours"
                class="ui-textarea min-h-[80px] p-3 text-sm focus:border-brand-primary transition-colors"
                [disabled]="isSubmitting()"
              ></textarea>
            </div>

            <div class="space-y-1.5">
              <label class="ui-label">Service / Médecin d'orientation <span class="text-red-500">*</span></label>
              <select 
                [(ngModel)]="visitOrientation"
                class="ui-select focus:border-brand-primary transition-colors"
                [disabled]="isSubmitting()"
              >
                <option value="" disabled selected>Choisir un service...</option>
                <option value="Médecine générale">Médecine générale</option>
                <option value="Tri / Urgences">Tri / Urgences</option>
                <option value="Pédiatrie">Pédiatrie</option>
                <option value="Gynécologie">Gynécologie</option>
                <option value="Pharmacie">Pharmacie</option>
                <option value="Autre">Autre</option>
              </select>
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
export class PatientDetailComponent implements OnInit {
  readonly patient = input.required<Patient>();
  readonly back = output<void>();

  private readonly tokenStorage = inject(AuthTokenStorageService);
  private readonly visitApi = inject(VisitApiService);
  private readonly router = inject(Router);
  readonly i18n = inject(I18nService);

  showVisitModal = false;
  downloadError = signal<string | null>(null);
  isSubmitting = signal(false);
  visitReason = '';
  visitOrientation = '';
  visitError = '';

  consultationHistory = signal<Consultation[]>([]);
  showHistory = signal(false);
  isLoadingHistory = signal(false);
  historyLoaded = false; // garde pour éviter double chargement

  activeVisit = signal<Visit | null>(null);

  private readonly consultationApi = inject(ConsultationApiService);

  readonly session = this.tokenStorage.session;

  readonly submitLabel = computed(() => this.isSubmitting() ? 'Enregistrement...' : "Valider l'admission");

  readonly canAdmit = computed(() => {
    const role = this.session()?.role;
    const allowedRoles = ['AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE'];
    return allowedRoles.includes(role || '') && this.activeVisit() === null;
  });

  // Un médecin/admin peut démarrer une consultation si une visite active existe pour ce patient
  readonly canStartConsultation = computed(() => {
    const role = this.session()?.role;
    return (role === 'MEDECIN' || role === 'ADMIN_CLINIQUE') && this.activeVisit() !== null;
  });

  readonly age = computed(() => {
    try {
      const birth = new Date(this.patient().birthDate);
      const today = new Date();
      let age = today.getFullYear() - birth.getFullYear();
      const m = today.getMonth() - birth.getMonth();
      if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) {
        age--;
      }
      return age >= 0 ? age : 0;
    } catch {
      return 0;
    }
  });

  openModal(): void {
    this.showVisitModal = true;
    this.visitReason = '';
    this.visitOrientation = '';
    this.visitError = '';
  }

  closeModal(): void {
    if (!this.isSubmitting()) {
      this.showVisitModal = false;
    }
  }

  submitVisit(): void {
    if (!this.visitReason || !this.visitOrientation || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);
    this.visitError = '';

    this.visitApi.create({
      patientId: this.patient().id,
      reason: this.visitReason,
      orientation: this.visitOrientation
    }).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.showVisitModal = false;
        // Redirection vers le dashboard
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.visitError = err.error?.detail || err.error?.title || 'Une erreur est survenue lors de l\'ouverture de la visite.';
      }
    });
  }

  ngOnInit(): void {
    // Chercher la visite active du patient pour les médecins
    const role = this.session()?.role;
    if (role === 'MEDECIN' || role === 'ADMIN_CLINIQUE') {
      this.visitApi.getActiveVisits().subscribe({
        next: (visits) => {
          const found = visits.find(v => v.patientId === this.patient().id) ?? null;
          this.activeVisit.set(found);
        },
        error: () => { /* silencieux */ }
      });
    }
  }

  loadHistory(): void {
    const role = this.session()?.role;
    if (role !== 'MEDECIN' && role !== 'ADMIN_CLINIQUE' && role !== 'INFIRMIER') {
      return;
    }
    this.isLoadingHistory.set(true);
    this.consultationApi.getPatientConsultations(this.patient().id).subscribe({
      next: (data) => {
        this.consultationHistory.set(data);
        this.isLoadingHistory.set(false);
        this.historyLoaded = true;
      },
      error: () => {
        // 403 or other errors ignored silently
        this.isLoadingHistory.set(false);
        this.historyLoaded = true;
      }
    });
  }

  toggleHistory(): void {
    const newState = !this.showHistory();
    this.showHistory.set(newState);
    // Charger l'historique uniquement au premier clic (lazy)
    if (newState && !this.historyLoaded) {
      this.loadHistory();
    }
  }

  goToConsultation(): void {
    const visit = this.activeVisit();
    if (visit) {
      this.router.navigate(['/clinic/consultation', visit.id]);
    }
  }

  downloadPdf(consultation: Consultation): void {
    this.downloadError.set(null);
    this.consultationApi.downloadDocument(consultation.visitId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Ordonnance_Visite_${consultation.visitNumber}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.downloadError.set(this.i18n.t('patients.downloadPdfError'));
      }
    });
  }
}

