import { Component, Input, Output, EventEmitter } from '@angular/core';
import { PatientPortalConsultation } from '../services/patient-portal.service';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-patient-visits-list',
  standalone: true,
  imports: [DatePipe],
  template: `
    <div class="ui-card p-6 flex flex-col gap-4">
      <h3 class="font-display text-lg font-bold border-b border-[var(--app-border)] pb-3" style="color: var(--text-primary)">
        Historique des Consultations
      </h3>

      @if (consultations && consultations.length > 0) {
        <div class="flex flex-col gap-4">
          @for (c of consultations; track c.visitId) {
            <div class="flex flex-col md:flex-row md:items-center justify-between p-4 rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface)] gap-4 transition-shadow hover:shadow-sm">
              <div class="flex-1 flex flex-col gap-1">
                <div class="flex items-center gap-2 flex-wrap">
                  <span class="text-sm font-bold text-[var(--brand-primary)]">
                    {{ c.clinicName }}
                  </span>
                  <span class="text-xs text-[var(--text-muted)]">
                    • {{ c.visitDate | date:'dd/MM/yyyy' }}
                  </span>
                  <span class="text-xs font-bold px-2 py-0.5 rounded-[var(--radius-brand-sm)] bg-[var(--app-surface-muted)] text-[var(--text-muted)]">
                    {{ c.visitNumber }}
                  </span>
                </div>
                <div class="text-sm font-semibold text-[var(--text-primary)]">
                  Médecin : <span class="text-[var(--text-secondary)]">{{ c.doctorName }}</span>
                </div>
                @if (c.diagnosis) {
                  <div class="text-xs text-[var(--text-secondary)] bg-[var(--app-surface-muted)] p-2 rounded-[var(--radius-brand-xs)] mt-1 border border-dashed border-[var(--app-border)]">
                    <span class="font-bold text-[var(--text-primary)]">Diagnostic :</span> {{ c.diagnosis }}
                  </div>
                }
              </div>

              <div class="flex items-center gap-2">
                @if (c.documentId) {
                  @if (c.documentStatus === 'VALID') {
                    <button
                      (click)="download.emit(c.visitId)"
                      class="inline-flex items-center justify-center gap-2 px-4 py-2 text-xs font-bold rounded-[var(--radius-brand-sm)] bg-[var(--brand-primary)] text-white hover:bg-[var(--brand-primary-hover)] transition-colors cursor-pointer"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3" />
                      </svg>
                      Télécharger l'Ordonnance
                    </button>
                  } @else if (c.documentStatus === 'REVOQUE') {
                    <span class="text-[10px] font-extrabold uppercase tracking-wide px-3 py-1.5 rounded-[var(--radius-brand-sm)] bg-amber-50 dark:bg-amber-950/20 border border-amber-200 dark:border-amber-900/40 text-amber-700 dark:text-amber-400">
                      Ordonnance Révoquée
                    </span>
                  } @else if (c.documentStatus === 'ANNULE') {
                    <span class="text-[10px] font-extrabold uppercase tracking-wide px-3 py-1.5 rounded-[var(--radius-brand-sm)] bg-rose-50 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-900/40 text-rose-700 dark:text-rose-400">
                      Ordonnance Annulée
                    </span>
                  }
                } @else {
                  <span class="text-xs text-[var(--text-muted)] italic">
                    Aucun document disponible
                  </span>
                }
              </div>
            </div>
          }
        </div>
      } @else {
        <div class="flex flex-col items-center justify-center p-8 border border-dashed border-[var(--app-border)] rounded-[var(--radius-brand-md)] text-[var(--text-muted)] bg-[var(--app-surface-muted)]">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-12 h-12 mb-2 text-[var(--text-muted)]">
            <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
          </svg>
          <p class="text-sm font-semibold">Aucune visite ou consultation enregistrée</p>
        </div>
      }
    </div>
  `
})
export class PatientVisitsListComponent {
  @Input({ required: true }) consultations: PatientPortalConsultation[] = [];
  @Output() download = new EventEmitter<string>();
}
