import { Component, inject, Input, Output, EventEmitter } from '@angular/core';
import { PatientPortalConsultation } from '../services/patient-portal.service';
import { DatePipe } from '@angular/common';
import { I18nService } from '../../../core/i18n/i18n.service';

@Component({
  selector: 'app-patient-visits-list',
  standalone: true,
  imports: [DatePipe],
  template: `
    <div class="ui-card-subtle p-5 lg:p-6 flex flex-col gap-4">
      <div class="flex flex-col gap-1 border-b border-[var(--app-border)] pb-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p class="ui-label">{{ i18n.t('patient.dashboard.medicalDocuments') }}</p>
          <h3 class="font-display text-xl font-extrabold" style="color: var(--text-primary)">
            {{ i18n.t('patient.visits.history') }}
          </h3>
        </div>
        @if (consultations && consultations.length > 0) {
          <span class="text-xs font-semibold text-[var(--text-muted)]">
            {{ consultations.length }} {{ i18n.t(consultations.length > 1 ? 'patient.dashboard.consultationCountPlural' : 'patient.dashboard.consultationCount') }}
          </span>
        }
      </div>

      @if (consultations && consultations.length > 0) {
        <div class="flex flex-col gap-3">
          @for (c of consultations; track c.visitId) {
            <article class="rounded-[var(--radius-brand-lg)] border border-[var(--app-border)] bg-[var(--app-surface)] overflow-hidden transition-colors">
              <button
                type="button"
                (click)="toggleConsultation(c.visitId)"
                class="w-full p-4 flex items-start justify-between gap-4 text-left hover:bg-[var(--app-surface-muted)]/45 transition-colors cursor-pointer"
                [attr.aria-expanded]="expandedConsultations[c.visitId]"
              >
                <div class="min-w-0 flex-1 space-y-2">
                  <div class="flex flex-wrap items-center gap-x-2 gap-y-1">
                    <span class="text-sm font-extrabold text-[var(--brand-primary)]">
                      {{ c.clinicName }}
                    </span>
                    <span class="text-xs text-[var(--text-muted)]">{{ c.visitDate | date:'dd/MM/yyyy' }}</span>
                    <code class="rounded-[var(--radius-brand-sm)] bg-[var(--app-surface-muted)] px-2 py-0.5 font-mono text-[11px] font-bold text-[var(--text-muted)]">
                      {{ c.visitNumber }}
                    </code>
                  </div>
                  <div class="text-xs font-semibold text-[var(--text-secondary)]">
                    {{ i18n.t('patient.visits.doctor') }} :
                    <span class="text-[var(--text-primary)]">{{ i18n.t('patient.visits.doctorPrefix') }} {{ c.doctorName }}</span>
                  </div>
                  <p class="text-sm font-semibold text-[var(--text-primary)] line-clamp-1">
                    <span class="ui-label inline normal-case tracking-normal">{{ i18n.t('patient.visits.diagnosis') }} :</span>
                    {{ c.diagnosis }}
                  </p>
                </div>

                <svg
                  class="mt-1 w-4 h-4 shrink-0 text-[var(--text-muted)] transform transition-transform duration-200"
                  [class.rotate-180]="expandedConsultations[c.visitId]"
                  fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
                </svg>
              </button>

              @if (expandedConsultations[c.visitId]) {
                <div class="px-4 pb-4 pt-3 border-t border-[var(--app-border)]/70 space-y-4 text-[var(--text-secondary)] text-xs">

                  @if (c.vitals; as vitals) {
                    <div class="rounded-[var(--radius-brand-md)] bg-[var(--app-surface-muted)]/65 border border-[var(--app-border)] p-3">
                      <span class="ui-label">{{ i18n.t('patient.visits.measures') }}</span>
                      <div class="mt-2 grid grid-cols-2 gap-2 sm:grid-cols-3 xl:grid-cols-6">
                        @if (vitals.temperature) {
                          <div class="rounded-[var(--radius-brand-sm)] bg-[var(--app-surface)] border border-[var(--app-border)] p-2">
                            <span class="block text-[10px] font-bold uppercase text-[var(--text-muted)]">{{ i18n.t('patient.visits.temperature') }}</span>
                            <strong class="text-sm text-[var(--text-primary)]">{{ vitals.temperature }} °C</strong>
                          </div>
                        }
                        @if (vitals.weight) {
                          <div class="rounded-[var(--radius-brand-sm)] bg-[var(--app-surface)] border border-[var(--app-border)] p-2">
                            <span class="block text-[10px] font-bold uppercase text-[var(--text-muted)]">{{ i18n.t('patient.visits.weight') }}</span>
                            <strong class="text-sm text-[var(--text-primary)]">{{ vitals.weight }} kg</strong>
                          </div>
                        }
                        @if (vitals.height) {
                          <div class="rounded-[var(--radius-brand-sm)] bg-[var(--app-surface)] border border-[var(--app-border)] p-2">
                            <span class="block text-[10px] font-bold uppercase text-[var(--text-muted)]">{{ i18n.t('patient.visits.height') }}</span>
                            <strong class="text-sm text-[var(--text-primary)]">{{ vitals.height }} cm</strong>
                          </div>
                        }
                        @if (vitals.bmi) {
                          <div class="rounded-[var(--radius-brand-sm)] bg-[var(--app-surface)] border border-[var(--app-border)] p-2">
                            <span class="block text-[10px] font-bold uppercase text-[var(--text-muted)]">{{ i18n.t('patient.visits.bmi') }}</span>
                            <strong class="text-sm text-[var(--brand-primary)]">{{ vitals.bmi }}</strong>
                          </div>
                        }
                        @if (vitals.systolic && vitals.diastolic) {
                          <div class="rounded-[var(--radius-brand-sm)] bg-[var(--app-surface)] border border-[var(--app-border)] p-2">
                            <span class="block text-[10px] font-bold uppercase text-[var(--text-muted)]">{{ i18n.t('patient.visits.bloodPressure') }}</span>
                            <strong class="text-sm text-[var(--text-primary)]">{{ vitals.systolic }}/{{ vitals.diastolic }}</strong>
                          </div>
                        }
                        @if (vitals.pulse) {
                          <div class="rounded-[var(--radius-brand-sm)] bg-[var(--app-surface)] border border-[var(--app-border)] p-2">
                            <span class="block text-[10px] font-bold uppercase text-[var(--text-muted)]">{{ i18n.t('patient.visits.pulse') }}</span>
                            <strong class="text-sm text-[var(--text-primary)]">{{ vitals.pulse }} bpm</strong>
                          </div>
                        }
                      </div>
                    </div>
                  }

                  <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                    @if (c.symptoms) {
                      <section class="rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface)] p-3">
                        <span class="ui-label text-amber-700 dark:text-amber-300">{{ i18n.t('patient.visits.symptoms') }}</span>
                        <p class="mt-1.5 text-sm font-semibold text-[var(--text-primary)] leading-relaxed">{{ c.symptoms }}</p>
                      </section>
                    }
                    @if (c.clinicalExam) {
                      <section class="rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface)] p-3">
                        <span class="ui-label">{{ i18n.t('patient.visits.clinicalExam') }}</span>
                        <p class="mt-1.5 text-sm font-semibold text-[var(--text-primary)] leading-relaxed">{{ c.clinicalExam }}</p>
                      </section>
                    }
                    @if (c.advice) {
                      <section class="rounded-[var(--radius-brand-md)] border border-emerald-200 dark:border-emerald-900/40 bg-emerald-50/40 dark:bg-emerald-950/10 p-3">
                        <span class="ui-label text-[var(--brand-success-text)]">{{ i18n.t('patient.visits.advice') }}</span>
                        <p class="mt-1.5 text-sm font-semibold text-[var(--text-primary)] leading-relaxed">{{ c.advice }}</p>
                      </section>
                    }
                    @if (c.followUp) {
                      <section class="rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface)] p-3">
                        <span class="ui-label">{{ i18n.t('patient.visits.followUp') }}</span>
                        <p class="mt-1.5 text-sm font-semibold text-[var(--text-primary)] leading-relaxed">{{ c.followUp }}</p>
                      </section>
                    }
                  </div>

                  @if (c.prescriptionItems && c.prescriptionItems.length > 0) {
                    <div class="space-y-2 pt-1">
                      <h4 class="ui-label">{{ i18n.t('patient.visits.prescribedDrugs') }}</h4>
                      
                      @if (c.prescriptionNumber) {
                        <div class="flex flex-col sm:flex-row sm:items-center justify-between p-3 rounded-[var(--radius-brand-md)] bg-indigo-50/30 dark:bg-indigo-950/10 border border-indigo-100/50 dark:border-indigo-900/30 gap-2 mb-2">
                          <div class="text-xs text-[var(--text-secondary)]">
                            <div>{{ i18n.t('patients.prescriptionNumber') }} : <strong class="font-mono text-[var(--brand-primary)]">{{ c.prescriptionNumber }}</strong></div>
                            @if (c.pinCode) {
                              <div class="mt-1">{{ i18n.t('patients.pinCode') }} : <strong class="font-mono bg-indigo-100 dark:bg-indigo-900/50 px-1.5 py-0.5 rounded text-indigo-700 dark:text-brand-cyan">{{ c.pinCode }}</strong></div>
                            }
                          </div>
                          @if (c.prescriptionDocumentId) {
                            <button
                              type="button"
                              [disabled]="downloadingId === c.prescriptionDocumentId"
                              (click)="downloadPrescription.emit(c.prescriptionDocumentId); $event.stopPropagation()"
                              class="inline-flex items-center justify-center gap-2 px-3 py-2 text-xs font-bold rounded-[var(--radius-brand-sm)] bg-[var(--brand-primary)] text-white hover:bg-[var(--brand-primary)]/90 disabled:opacity-50 transition-colors cursor-pointer shrink-0"
                            >
                              <svg class="w-3.5 h-3.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                              </svg>
                              {{ downloadingId === c.prescriptionDocumentId ? i18n.t('patient.summary.downloading') || 'Téléchargement...' : i18n.t('patients.downloadPrescription') }}
                            </button>
                          }
                        </div>
                      }

                      <div class="grid gap-2">
                        @for (item of c.prescriptionItems; track item.id) {
                          <div class="rounded-[var(--radius-brand-md)] bg-[var(--app-surface)] border border-[var(--app-border)] p-3 flex items-start gap-3">
                            <span class="shrink-0 rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] px-2 py-1 text-[10px] font-extrabold text-[var(--brand-primary)]">RX</span>
                            <div class="flex-1 min-w-0">
                              <div class="flex flex-wrap items-baseline gap-x-2">
                                <h5 class="text-sm font-bold text-[var(--text-primary)]">{{ item.drugName }}</h5>
                                <span class="text-xs text-[var(--text-muted)]">
                                  {{ item.dosage }} @if(item.quantity){(x{{ item.quantity }})}
                                </span>
                              </div>
                              <p class="mt-0.5 text-xs font-semibold text-[var(--text-secondary)]">
                                {{ i18n.t('patient.visits.posology') }} :
                                <span class="text-[var(--text-primary)]">{{ item.posology }}</span>
                                @if(item.duration){ {{ i18n.t('patient.visits.durationPrefix') }} {{ item.duration }}}
                              </p>
                              @if (item.instructions) {
                                <p class="mt-0.5 text-[11px] text-[var(--text-muted)]">
                                  {{ i18n.t('patient.visits.instructions') }} : {{ item.instructions }}
                                </p>
                              }
                            </div>
                          </div>
                        }
                      </div>
                    </div>
                  }

                  <div class="flex flex-wrap items-center justify-between gap-2 pt-3 border-t border-[var(--app-border)]/70">
                    <span class="text-[11px] font-semibold text-[var(--text-muted)]">{{ i18n.t('patient.visits.consultationDocument') }}</span>
                    @if (c.documentId) {
                      @if (c.documentStatus === 'VALID') {
                        <button
                          type="button"
                          [disabled]="downloadingId === c.visitId"
                          (click)="download.emit(c.visitId); $event.stopPropagation()"
                          class="inline-flex items-center justify-center gap-2 px-3 py-2 text-xs font-bold rounded-[var(--radius-brand-sm)] border border-[var(--brand-primary)] text-[var(--brand-primary)] hover:bg-[var(--brand-primary)] hover:text-white disabled:opacity-50 transition-colors cursor-pointer"
                        >
                          {{ downloadingId === c.visitId ? i18n.t('patient.summary.downloading') || 'Téléchargement...' : i18n.t('patients.downloadPdf') }}
                        </button>
                      } @else if (c.documentStatus === 'REVOQUE') {
                        <span class="text-[10px] font-extrabold uppercase tracking-wide px-3 py-1.5 rounded-[var(--radius-brand-sm)] bg-amber-50 dark:bg-amber-950/20 border border-amber-200 dark:border-amber-900/40 text-[var(--brand-warning-text)]">
                          {{ i18n.t('patient.visits.revoked') }}
                        </span>
                      } @else if (c.documentStatus === 'ANNULE') {
                        <span class="text-[10px] font-extrabold uppercase tracking-wide px-3 py-1.5 rounded-[var(--radius-brand-sm)] bg-rose-50 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-900/40 text-rose-700 dark:text-rose-400">
                          {{ i18n.t('patient.visits.cancelled') }}
                        </span>
                      }
                    } @else {
                      <span class="text-xs text-[var(--text-muted)] italic">
                        {{ i18n.t('patient.visits.noDocument') }}
                      </span>
                    }
                  </div>

                  @if (c.prescriptionId) {
                    <div class="flex flex-wrap items-center justify-between gap-2 pt-3 border-t border-[var(--app-border)]/70">
                      <div class="flex flex-col">
                        <span class="text-[11px] font-semibold text-[var(--text-muted)]">{{ i18n.t('patient.prescription.teletransmission') }}</span>
                        @if (c.prescriptionTransmittedAt) {
                          <span class="text-[10px] text-[var(--text-muted)] mt-0.5">
                            {{ i18n.t('patient.prescription.transmittedAt') }} : {{ c.prescriptionTransmittedAt | date:'dd/MM/yyyy HH:mm' }}
                          </span>
                        }
                      </div>
                      <div class="flex items-center gap-2">
                        @if (c.prescriptionTransmissionStatus === 'TRANSMITTED') {
                          <span class="text-[10px] font-extrabold uppercase tracking-wide px-3 py-1.5 rounded-[var(--radius-brand-sm)] bg-emerald-50 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-900/40 text-emerald-700 dark:text-emerald-400">
                            {{ i18n.t('patient.prescription.transmitted') }}
                          </span>
                        } @else if (c.prescriptionTransmissionStatus === 'PENDING') {
                          <span class="text-[10px] font-extrabold uppercase tracking-wide px-3 py-1.5 rounded-[var(--radius-brand-sm)] bg-amber-50 dark:bg-amber-950/20 border border-amber-200 dark:border-amber-900/40 text-[var(--brand-warning-text)] animate-pulse">
                            {{ i18n.t('patient.prescription.pending') }}
                          </span>
                        } @else if (c.prescriptionTransmissionStatus === 'FAILED') {
                          <span class="text-[10px] font-extrabold uppercase tracking-wide px-3 py-1.5 rounded-[var(--radius-brand-sm)] bg-rose-50 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-900/40 text-rose-700 dark:text-rose-400">
                            {{ i18n.t('patient.prescription.failed') }}
                          </span>
                        } @else {
                          <span class="text-[10px] font-extrabold uppercase tracking-wide px-3 py-1.5 rounded-[var(--radius-brand-sm)] bg-[var(--app-surface-muted)] dark:bg-[var(--brand-danger-subtle)] border border-[var(--app-border)] dark:border-slate-900/40 text-[var(--text-secondary)] dark:text-[var(--text-muted)]">
                            {{ i18n.t('patient.prescription.notTransmitted') }}
                          </span>
                        }

                        @if (c.prescriptionTransmissionStatus !== 'TRANSMITTED') {
                          <button
                            type="button"
                            (click)="transmit.emit(c.prescriptionId); $event.stopPropagation()"
                            [disabled]="c.prescriptionTransmissionStatus === 'PENDING'"
                            class="inline-flex items-center justify-center gap-2 px-3 py-2 text-xs font-bold rounded-[var(--radius-brand-sm)] border border-[var(--brand-primary)] text-[var(--brand-primary)] hover:bg-[var(--brand-primary)] hover:text-white disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-transparent disabled:hover:text-[var(--brand-primary)] transition-colors cursor-pointer"
                          >
                            {{ i18n.t('patient.prescription.transmitBtn') }}
                          </button>
                        }
                      </div>
                    </div>
                  }

                </div>
              }
            </article>
          }
        </div>
      } @else {
        <div class="flex flex-col items-center justify-center p-8 border border-dashed border-[var(--app-border)] rounded-[var(--radius-brand-md)] text-[var(--text-muted)] bg-[var(--app-surface-muted)]">
          <p class="text-sm font-semibold">{{ i18n.t('patient.visits.empty') }}</p>
        </div>
      }
    </div>
  `
})
export class PatientVisitsListComponent {
  readonly i18n = inject(I18nService);

  @Input({ required: true }) consultations: PatientPortalConsultation[] = [];
  @Input() downloadingId = '';
  @Output() download = new EventEmitter<string>();
  @Output() downloadPrescription = new EventEmitter<string>();
  @Output() transmit = new EventEmitter<string>();

  expandedConsultations: Record<string, boolean> = {};

  toggleConsultation(visitId: string): void {
    this.expandedConsultations[visitId] = !this.expandedConsultations[visitId];
  }
}
