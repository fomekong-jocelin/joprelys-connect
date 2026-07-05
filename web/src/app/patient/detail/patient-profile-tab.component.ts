import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PatientDetailComponent } from '../patient-detail.component';
import { PatientMedicalInfoComponent } from '../patient-medical-info.component';

@Component({
  selector: 'app-patient-profile-tab',
  standalone: true,
  imports: [CommonModule, PatientMedicalInfoComponent],
  template: `
    @if (parent.patient(); as p) {
      <div class="space-y-6 animate-fade-in">
        <div>
          <h3 class="text-xs font-black uppercase tracking-wider text-slate-400 dark:text-slate-500 mb-4">
            Informations Administratives
          </h3>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <div class="p-3 bg-slate-50/50 dark:bg-slate-800/30 border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
              <span class="ui-label block text-[10px] text-slate-400 dark:text-slate-500">Sexe</span>
              <span class="font-extrabold text-sm text-slate-800 dark:text-slate-200">{{ p.gender }}</span>
            </div>
            <div class="p-3 bg-slate-50/50 dark:bg-slate-800/30 border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
              <span class="ui-label block text-[10px] text-slate-400 dark:text-slate-500">Date de naissance (âge)</span>
              <span class="font-extrabold text-sm text-slate-800 dark:text-slate-200">{{ p.birthDate }} ({{ age() }} ans)</span>
            </div>
            <div class="p-3 bg-slate-50/50 dark:bg-slate-800/30 border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
              <span class="ui-label block text-[10px] text-slate-400 dark:text-slate-500">Téléphone</span>
              <span class="font-extrabold text-sm text-slate-800 dark:text-slate-200">{{ p.phone || 'Non renseigné' }}</span>
            </div>
            <div class="p-3 bg-slate-50/50 dark:bg-slate-800/30 border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
              <span class="ui-label block text-[10px] text-slate-400 dark:text-slate-500">Ville</span>
              <span class="font-extrabold text-sm text-slate-800 dark:text-slate-200 capitalize">{{ p.city }}</span>
            </div>
            <div class="p-3 bg-slate-50/50 dark:bg-slate-800/30 border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
              <span class="ui-label block text-[10px] text-slate-400 dark:text-slate-500">Quartier / District</span>
              <span class="font-extrabold text-sm text-slate-800 dark:text-slate-200">{{ p.district || 'Non renseigné' }}</span>
            </div>
            <div class="p-3 bg-slate-50/50 dark:bg-slate-800/30 border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
              <span class="ui-label block text-[10px] text-slate-400 dark:text-slate-500">Adresse Géographique</span>
              <span class="font-extrabold text-sm text-slate-800 dark:text-slate-200">{{ p.address || 'Non renseignée' }}</span>
            </div>
          </div>
        </div>

        <div>
          <h3 class="text-xs font-black uppercase tracking-wider text-slate-400 dark:text-slate-500 mb-4">
            Contact d'Urgence
          </h3>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div class="p-3 bg-slate-50/50 dark:bg-slate-800/30 border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
              <span class="ui-label block text-[10px] text-slate-400 dark:text-slate-500">Nom Complet</span>
              <span class="font-extrabold text-sm text-slate-800 dark:text-slate-200">{{ p.emergencyContactName || 'Non renseigné' }}</span>
            </div>
            <div class="p-3 bg-slate-50/50 dark:bg-slate-800/30 border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
              <span class="ui-label block text-[10px] text-slate-400 dark:text-slate-500">Téléphone</span>
              <span class="font-extrabold text-sm text-slate-800 dark:text-slate-200">{{ p.emergencyContactPhone || 'Non renseigné' }}</span>
            </div>
          </div>
        </div>

        <app-patient-medical-info [patientId]="p.id" />
      </div>
    }
  `
})
export class PatientProfileTabComponent {
  readonly parent = inject(PatientDetailComponent);

  readonly age = computed(() => {
    const p = this.parent.patient();
    if (!p) return 0;
    try {
      const birth = new Date(p.birthDate);
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
