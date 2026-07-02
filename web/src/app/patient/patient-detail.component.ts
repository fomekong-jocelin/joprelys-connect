import { Component, computed, inject, input, output, signal } from '@angular/core';
import { ButtonComponent } from '../shared/ui/button.component';
import { CardComponent } from '../shared/ui/card.component';
import { Patient } from './patient.models';
import { AuthTokenStorageService } from '../auth/auth-token-storage.service';
import { VisitApiService } from '../visit/visit-api.service';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  imports: [ButtonComponent, CardComponent, FormsModule],
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

        <div class="flex justify-end gap-3 pt-4 border-t" style="border-color: var(--border-color)">
          <app-ui-button variant="secondary" (pressed)="back.emit()">
            Retour à la liste
          </app-ui-button>
          
          @if (canAdmit()) {
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
                class="ui-textarea min-h-[80px] p-3 text-sm focus:border-brand-primary focus:ring-4 focus:ring-brand-primary/10 transition-colors"
                [disabled]="isSubmitting()"
              ></textarea>
            </div>

            <div class="space-y-1.5">
              <label class="ui-label">Service / Médecin d'orientation <span class="text-red-500">*</span></label>
              <select 
                [(ngModel)]="visitOrientation"
                class="ui-select focus:border-brand-primary focus:ring-4 focus:ring-brand-primary/10 transition-colors"
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
export class PatientDetailComponent {
  readonly patient = input.required<Patient>();
  readonly back = output<void>();

  private readonly tokenStorage = inject(AuthTokenStorageService);
  private readonly visitApi = inject(VisitApiService);
  private readonly router = inject(Router);

  showVisitModal = false;
  isSubmitting = signal(false);
  visitReason = '';
  visitOrientation = '';
  visitError = '';

  readonly session = this.tokenStorage.session;

  readonly submitLabel = computed(() => this.isSubmitting() ? 'Enregistrement...' : "Valider l'admission");

  readonly canAdmit = computed(() => {
    const role = this.session()?.role;
    return role === 'AGENT_ACCUEIL' || role === 'INFIRMIER' || role === 'MEDECIN' || role === 'ADMIN_CLINIQUE';
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
}
