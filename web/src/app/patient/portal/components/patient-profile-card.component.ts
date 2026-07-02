import { Component, Input } from '@angular/core';
import { PatientPortalMeResponse } from '../services/patient-portal.service';

@Component({
  selector: 'app-patient-profile-card',
  standalone: true,
  template: `
    <div class="ui-card p-6 flex flex-col gap-6">
      <div class="flex items-center gap-4 border-b border-[var(--app-border)] pb-4">
        <div class="ui-avatar w-16 h-16 text-2xl flex items-center justify-center font-bold">
          {{ patient.fullName.charAt(0) }}
        </div>
        <div>
          <h2 class="font-display text-xl font-bold" style="color: var(--text-primary)">
            {{ patient.fullName }}
          </h2>
          <span class="text-xs font-bold px-2 py-0.5 rounded-[var(--radius-brand-sm)] uppercase tracking-wider bg-[var(--app-surface-muted)] text-[var(--text-secondary)]">
            N° DPU: {{ patient.globalPatientNumber }}
          </span>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
        <div>
          <span class="block text-xs font-semibold text-[var(--text-muted)] uppercase">Genre</span>
          <span class="font-semibold text-[var(--text-primary)]">{{ patient.gender }}</span>
        </div>
        <div>
          <span class="block text-xs font-semibold text-[var(--text-muted)] uppercase">Date de naissance</span>
          <span class="font-semibold text-[var(--text-primary)]">{{ patient.birthDate }}</span>
        </div>
        <div>
          <span class="block text-xs font-semibold text-[var(--text-muted)] uppercase">Téléphone</span>
          <span class="font-semibold text-[var(--text-primary)]">{{ patient.phone }}</span>
        </div>
        <div>
          <span class="block text-xs font-semibold text-[var(--text-muted)] uppercase">Adresse</span>
          <span class="font-semibold text-[var(--text-primary)]">
            {{ patient.address || '-' }}, {{ patient.city }}
          </span>
        </div>
      </div>

      <div class="border-t border-[var(--app-border)] pt-4 flex flex-col gap-4">
        <div>
          <span class="block text-xs font-semibold text-[var(--text-muted)] uppercase mb-1">Allergies connues</span>
          <div class="p-3 rounded-[var(--radius-brand-sm)] bg-red-50 dark:bg-red-950/20 border border-red-200 dark:border-red-800/40 text-red-700 dark:text-red-300 font-semibold text-xs">
            {{ patient.allergies || 'Aucune allergie signalée' }}
          </div>
        </div>
        <div>
          <span class="block text-xs font-semibold text-[var(--text-muted)] uppercase mb-1">Antécédents médicaux</span>
          <div class="p-3 rounded-[var(--radius-brand-sm)] bg-[var(--app-surface-muted)] text-[var(--text-secondary)] text-xs">
            {{ patient.medicalHistory || 'Aucun antécédent médical renseigné' }}
          </div>
        </div>
      </div>
    </div>
  `
})
export class PatientProfileCardComponent {
  @Input({ required: true }) patient!: PatientPortalMeResponse;
}
