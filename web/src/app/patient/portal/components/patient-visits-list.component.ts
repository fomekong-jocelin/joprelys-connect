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
            <div class="rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface)] overflow-hidden transition-all duration-200">
              <!-- En-tête de l'accordéon cliquable -->
              <div
                (click)="toggleConsultation(c.visitId)"
                class="p-4 flex items-center justify-between cursor-pointer hover:bg-[var(--app-surface-muted)]/40 transition-colors select-none"
              >
                <div class="flex-1 flex flex-col gap-1 min-w-0">
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
                  <div class="text-xs font-semibold text-[var(--text-secondary)]">
                    Médecin : <span>Dr. {{ c.doctorName }}</span>
                  </div>
                  <p class="text-sm font-bold text-slate-800 dark:text-slate-200 mt-1 line-clamp-1">
                    <span class="text-xs font-bold uppercase text-slate-400 dark:text-slate-500">Diagnostic : </span>
                    {{ c.diagnosis }}
                  </p>
                </div>

                <div class="flex items-center gap-3 shrink-0">
                  <!-- Chevron de déploiement -->
                  <svg
                    class="w-4 h-4 text-slate-400 transform transition-transform duration-200"
                    [class.rotate-180]="expandedConsultations[c.visitId]"
                    fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"
                  >
                    <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
                  </svg>
                </div>
              </div>

              <!-- Contenu déplié de l'accordéon -->
              @if (expandedConsultations[c.visitId]) {
                <div class="px-4 pb-4 pt-2 border-t border-[var(--app-border)]/50 space-y-4 animate-fade-in text-[var(--text-secondary)] text-xs">
                  
                  <!-- 1. Constantes Vitales en ligne de type barre médicale -->
                  @if (c.vitals; as vitals) {
                    <div class="flex flex-wrap items-center gap-x-4 gap-y-2 p-3 rounded-lg bg-slate-100/50 dark:bg-slate-900/60 border border-slate-100 dark:border-slate-800/80 text-[11px]">
                      <span class="font-bold text-slate-400 uppercase tracking-wider">Mesures :</span>
                      @if (vitals.temperature) {
                        <span class="flex items-center gap-1 font-semibold text-slate-800 dark:text-slate-200">
                          <span class="text-rose-500 text-xs">🌡️</span> Température : <strong class="text-rose-600 dark:text-rose-400">{{ vitals.temperature }} °C</strong>
                        </span>
                      }
                      @if (vitals.weight) {
                        <span class="flex items-center gap-1 font-semibold text-slate-800 dark:text-slate-200">
                          <span class="text-indigo-500 text-xs">⚖️</span> Poids : <strong>{{ vitals.weight }} kg</strong>
                        </span>
                      }
                      @if (vitals.height) {
                        <span class="flex items-center gap-1 font-semibold text-slate-800 dark:text-slate-200">
                          <span class="text-indigo-500 text-xs">📏</span> Taille : <strong>{{ vitals.height }} cm</strong>
                        </span>
                      }
                      @if (vitals.bmi) {
                        <span class="flex items-center gap-1 font-semibold text-slate-800 dark:text-slate-200">
                          IMC : <strong class="text-indigo-600 dark:text-indigo-400">{{ vitals.bmi }}</strong>
                        </span>
                      }
                      @if (vitals.systolic && vitals.diastolic) {
                        <span class="flex items-center gap-1 font-semibold text-slate-800 dark:text-slate-200">
                          <span class="text-emerald-500 text-xs">💓</span> Tension : <strong>{{ vitals.systolic }}/{{ vitals.diastolic }} mmHg</strong>
                        </span>
                      }
                      @if (vitals.pulse) {
                        <span class="flex items-center gap-1 font-semibold text-slate-800 dark:text-slate-200">
                          <span class="text-red-500 text-xs">🫀</span> Pouls : <strong>{{ vitals.pulse }} bpm</strong>
                        </span>
                      }
                    </div>
                  }

                  <!-- 2. Bilan Clinique avec Blocs Aérés et Badges -->
                  <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div class="space-y-3">
                      <!-- Symptômes -->
                      @if (c.symptoms) {
                        <div class="p-3 rounded-lg bg-amber-500/5 border border-amber-500/10 dark:border-amber-500/20">
                          <span class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[9px] font-bold bg-amber-500/10 text-amber-700 dark:text-amber-300 uppercase tracking-wider">
                            Symptômes signalés
                          </span>
                          <p class="text-sm font-semibold text-slate-800 dark:text-slate-200 mt-1.5 leading-relaxed">{{ c.symptoms }}</p>
                        </div>
                      }

                      <!-- Examen Clinique -->
                      @if (c.clinicalExam) {
                        <div class="p-3 rounded-lg bg-indigo-500/5 border border-indigo-500/10 dark:border-indigo-500/20">
                          <span class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[9px] font-bold bg-indigo-500/10 text-indigo-700 dark:text-indigo-300 uppercase tracking-wider">
                            Examen Clinique
                          </span>
                          <p class="text-sm font-semibold text-slate-800 dark:text-slate-200 mt-1.5 leading-relaxed">{{ c.clinicalExam }}</p>
                        </div>
                      }
                    </div>

                    <div class="space-y-3">
                      <!-- Conseils & Recommandations -->
                      @if (c.advice) {
                        <div class="p-3 rounded-lg bg-emerald-500/5 border border-emerald-500/10 dark:border-emerald-500/20">
                          <span class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[9px] font-bold bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 uppercase tracking-wider">
                            Conseils & Recommandations
                          </span>
                          <p class="text-sm font-semibold text-slate-800 dark:text-slate-200 mt-1.5 leading-relaxed">{{ c.advice }}</p>
                        </div>
                      }

                      <!-- Suivi -->
                      @if (c.followUp) {
                        <div class="p-3 rounded-lg bg-cyan-500/5 border border-cyan-500/10 dark:border-cyan-500/20">
                          <span class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[9px] font-bold bg-cyan-500/10 text-cyan-700 dark:text-cyan-300 uppercase tracking-wider">
                            Suivi Clinique
                          </span>
                          <p class="text-sm font-semibold text-slate-800 dark:text-slate-200 mt-1.5 leading-relaxed">{{ c.followUp }}</p>
                        </div>
                      }
                    </div>
                  </div>

                  <!-- 3. Prescription Médicale sous forme de lignes fluides épurées (sans tableau froid) -->
                  @if (c.prescriptionItems && c.prescriptionItems.length > 0) {
                    <div class="space-y-2 pt-2 border-t border-[var(--app-border)]/50">
                      <h4 class="font-bold text-[10px] uppercase tracking-wider text-slate-400 dark:text-slate-500">Médicaments prescrits</h4>
                      
                      <div class="space-y-2">
                        @for (item of c.prescriptionItems; track item.id) {
                          <div class="p-3 rounded-lg bg-white dark:bg-slate-900 border border-[var(--app-border)] flex items-start gap-3">
                            <span class="p-2 rounded bg-indigo-50 dark:bg-indigo-950/40 text-indigo-600 dark:text-indigo-400 shrink-0 text-base">💊</span>
                            <div class="flex-1 min-w-0">
                              <div class="flex flex-wrap items-baseline gap-x-2">
                                <h5 class="text-sm font-bold text-slate-800 dark:text-slate-200">{{ item.drugName }}</h5>
                                <span class="text-xs text-slate-400 dark:text-slate-500">
                                  {{ item.dosage }} @if(item.quantity){(x{{ item.quantity }})}
                                </span>
                              </div>
                              <p class="text-xs font-semibold text-slate-600 dark:text-slate-400 mt-0.5">
                                Posologie : <span class="text-slate-800 dark:text-slate-200">{{ item.posology }}</span> @if(item.duration){pendant {{ item.duration }}}
                              </p>
                              @if (item.instructions) {
                                <p class="text-[11px] text-slate-400 dark:text-slate-500 italic mt-0.5">
                                  Instructions : {{ item.instructions }}
                                </p>
                              }
                            </div>
                          </div>
                        }
                      </div>
                    </div>
                  }

                  <!-- Actions du document (Téléchargement) -->
                  <div class="flex flex-wrap items-center justify-end gap-2 pt-2 border-t border-[var(--app-border)]/50">
                    @if (c.documentId) {
                      @if (c.documentStatus === 'VALID') {
                        <button
                          type="button"
                          (click)="download.emit(c.visitId); $event.stopPropagation()"
                          class="inline-flex items-center justify-center gap-2 px-4 py-2 text-xs font-bold rounded-[var(--radius-brand-sm)] bg-[var(--brand-primary)] text-white hover:bg-[var(--brand-primary-hover)] transition-colors cursor-pointer"
                        >
                          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="w-4 h-4">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3" />
                          </svg>
                          Télécharger l'Ordonnance (PDF)
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

  expandedConsultations: Record<string, boolean> = {};

  toggleConsultation(visitId: string): void {
    this.expandedConsultations[visitId] = !this.expandedConsultations[visitId];
  }
}
