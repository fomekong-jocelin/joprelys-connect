import { Component, computed, input, output } from '@angular/core';
import { ButtonComponent } from '../shared/ui/button.component';
import { CardComponent } from '../shared/ui/card.component';
import { Patient } from './patient.models';

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  imports: [ButtonComponent, CardComponent],
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
        </div>

      </div>
    </app-ui-card>
  `,
})
export class PatientDetailComponent {
  readonly patient = input.required<Patient>();
  readonly back = output<void>();

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
}
