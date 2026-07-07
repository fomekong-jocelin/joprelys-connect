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
          <h3 class="text-xs font-black uppercase tracking-wider text-[var(--text-muted)] mb-4">
            Informations Administratives
          </h3>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <div class="p-3 bg-[var(--app-surface-muted)] dark:bg-[var(--app-surface-muted)] border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
              <span class="ui-label block text-[10px] text-[var(--text-muted)]">Sexe</span>
              <span class="font-extrabold text-sm text-[var(--text-primary)]">{{ p.gender }}</span>
            </div>
            <div class="p-3 bg-[var(--app-surface-muted)] dark:bg-[var(--app-surface-muted)] border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
              <span class="ui-label block text-[10px] text-[var(--text-muted)]">Date de naissance (âge)</span>
              <span class="font-extrabold text-sm text-[var(--text-primary)]">{{ p.birthDate }} ({{ age() }} ans)</span>
            </div>
            <div class="p-3 bg-[var(--app-surface-muted)] dark:bg-[var(--app-surface-muted)] border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
              <span class="ui-label block text-[10px] text-[var(--text-muted)]">Téléphone</span>
              <span class="font-extrabold text-sm text-[var(--text-primary)]">{{ p.phone || 'Non renseigné' }}</span>
            </div>
            <div class="p-3 bg-[var(--app-surface-muted)] dark:bg-[var(--app-surface-muted)] border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
              <span class="ui-label block text-[10px] text-[var(--text-muted)]">Groupe sanguin</span>
              <span class="font-extrabold text-sm text-[var(--text-primary)]">{{ p.bloodGroup || 'Non renseigné' }}</span>
            </div>
            <div class="p-3 bg-[var(--app-surface-muted)] dark:bg-[var(--app-surface-muted)] border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
              <span class="ui-label block text-[10px] text-[var(--text-muted)]">Adresse email</span>
              <span class="font-extrabold text-sm text-[var(--text-primary)]">{{ p.email || 'Non renseigné' }}</span>
            </div>
            <div class="p-3 bg-[var(--app-surface-muted)] dark:bg-[var(--app-surface-muted)] border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
              <span class="ui-label block text-[10px] text-[var(--text-muted)]">Ville</span>
              <span class="font-extrabold text-sm text-[var(--text-primary)] capitalize">{{ p.city }}</span>
            </div>
            <div class="p-3 bg-[var(--app-surface-muted)] dark:bg-[var(--app-surface-muted)] border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
              <span class="ui-label block text-[10px] text-[var(--text-muted)]">Quartier / District</span>
              <span class="font-extrabold text-sm text-[var(--text-primary)]">{{ p.district || 'Non renseigné' }}</span>
            </div>
            <div class="p-3 bg-[var(--app-surface-muted)] dark:bg-[var(--app-surface-muted)] border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
              <span class="ui-label block text-[10px] text-[var(--text-muted)]">Adresse Géographique</span>
              <span class="font-extrabold text-sm text-[var(--text-primary)]">{{ p.address || 'Non renseignée' }}</span>
            </div>
          </div>
        </div>

        <div>
          <h3 class="text-xs font-black uppercase tracking-wider text-[var(--text-muted)] mb-4">
            Contact d'Urgence
          </h3>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div class="p-3 bg-[var(--app-surface-muted)] dark:bg-[var(--app-surface-muted)] border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
              <span class="ui-label block text-[10px] text-[var(--text-muted)]">Nom Complet</span>
              <span class="font-extrabold text-sm text-[var(--text-primary)]">{{ p.emergencyContactName || 'Non renseigné' }}</span>
            </div>
            <div class="p-3 bg-[var(--app-surface-muted)] dark:bg-[var(--app-surface-muted)] border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
              <span class="ui-label block text-[10px] text-[var(--text-muted)]">Téléphone</span>
              <span class="font-extrabold text-sm text-[var(--text-primary)]">{{ p.emergencyContactPhone || 'Non renseigné' }}</span>
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
