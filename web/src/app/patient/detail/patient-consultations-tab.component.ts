import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PatientDetailComponent } from '../patient-detail.component';
import { ConsultationApiService } from '../../consultation/consultation-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { Consultation } from '../../consultation/consultation.models';
import { ButtonComponent } from '../../shared/ui/button.component';
import { RbacApiService } from '../../clinic/rbac/rbac-api.service';

@Component({
  selector: 'app-patient-consultations-tab',
  standalone: true,
  imports: [CommonModule, FormsModule, ButtonComponent],
  template: `
    <div class="space-y-6 animate-fade-in">
      <div>
        <h3 class="text-xs font-black uppercase tracking-wider text-[var(--text-muted)] mb-4">
          Historique Médical & Consultations
        </h3>

        @if (downloadError()) {
          <div class="p-3 bg-[var(--brand-danger-subtle)] border border-[var(--brand-danger-border)] rounded-xl text-xs text-[var(--brand-danger-text)] font-semibold mb-3 leading-relaxed flex justify-between items-center">
            <span>{{ downloadError() }}</span>
            <button (click)="downloadError.set(null)" class="text-[var(--brand-danger)] hover:text-red-700 cursor-pointer ml-2">
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        }

        @if (isLoadingHistory()) {
          <div class="py-12 text-center">
            <div class="inline-block w-6 h-6 rounded-full border-2 border-indigo-200 border-t-indigo-600 animate-spin"></div>
            <p class="mt-2 text-xs font-bold text-[var(--text-muted)]">Chargement de l'historique...</p>
          </div>
        } @else if (consultationHistory().length === 0) {
          <div class="p-8 text-center border border-dashed border-[var(--app-border)]/80 rounded-xl">
            <p class="text-sm font-semibold text-[var(--text-muted)]">Aucun antécédent de consultation enregistré.</p>
          </div>
        } @else {
          <div class="space-y-3">
            @for (consult of consultationHistory(); track consult.id) {
              <div class="rounded-xl bg-[var(--app-surface-muted)] dark:bg-[var(--bg-input)]/20 border border-[var(--app-border)]/60 overflow-hidden transition-all duration-200">
                <!-- En-tête cliquable de l'accordéon -->
                <div
                  (click)="toggleConsultation(consult.id)"
                  class="p-4 flex items-center justify-between cursor-pointer hover:bg-slate-100/30 dark:hover:bg-slate-800/40 select-none transition-colors"
                >
                  <div class="flex flex-wrap items-center gap-3">
                    <span class="inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-extrabold bg-[var(--brand-info-subtle)] text-[var(--brand-info-text)]  dark:text-brand-cyan uppercase tracking-wider">
                      {{ consult.visitNumber }}
                    </span>
                    <span class="text-xs font-semibold text-[var(--text-muted)]">
                      {{ consult.createdAt | slice:0:10 }}
                    </span>
                    <p class="text-sm font-bold text-[var(--text-primary)] line-clamp-1">
                      <span class="text-xs font-bold uppercase text-[var(--text-muted)]">Diag : </span>
                      {{ consult.diagnosis }}
                    </p>
                  </div>

                  <div class="flex items-center gap-3">
                    <span class="text-xs font-extrabold text-[var(--text-muted)] hidden sm:inline">{{ formatDoctorName(consult.doctorName) }}</span>
                    
                    <!-- Chevron de déploiement -->
                    <svg
                      class="w-4 h-4 text-[var(--text-muted)] transform transition-transform duration-200"
                      [class.rotate-180]="expandedConsultations()[consult.id]"
                      fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
                    </svg>
                  </div>
                </div>

                <!-- Corps déplié de l'accordéon -->
                @if (expandedConsultations()[consult.id]) {
                  <div class="px-4 pb-4 pt-2 border-t border-[var(--app-border)]/40 space-y-4 animate-fade-in text-[var(--text-secondary)] text-xs">
                    
                    <!-- 1. Constantes Vitales en ligne de type barre médicale -->
                    @if (consult.vitals; as vitals) {
                      <div class="flex flex-wrap items-center gap-x-4 gap-y-2 p-3 rounded-lg bg-slate-100/50 dark:bg-[var(--overlay-bg)] border border-[var(--app-border)]/80 text-[11px]">
                        <span class="font-bold text-[var(--text-muted)] uppercase tracking-wider">Mesures :</span>
                        @if (vitals.temperature) {
                          <span class="flex items-center gap-1 font-semibold text-[var(--text-primary)]">
                            <span class="text-rose-500 text-xs">🌡️</span> Température : <strong class="text-rose-600 dark:text-rose-400">{{ vitals.temperature }} °C</strong>
                          </span>
                        }
                        @if (vitals.weight) {
                          <span class="flex items-center gap-1 font-semibold text-[var(--text-primary)]">
                            <span class="text-[var(--brand-info-text)] text-xs">⚖️</span> Poids : <strong>{{ vitals.weight }} kg</strong>
                          </span>
                        }
                        @if (vitals.height) {
                          <span class="flex items-center gap-1 font-semibold text-[var(--text-primary)]">
                            <span class="text-[var(--brand-info-text)] text-xs">📏</span> Taille : <strong>{{ vitals.height }} cm</strong>
                          </span>
                        }
                        @if (vitals.bmi) {
                          <span class="flex items-center gap-1 font-semibold text-[var(--text-primary)]">
                            IMC : <strong class="text-[var(--brand-info-text)]">{{ vitals.bmi }}</strong>
                          </span>
                        }
                        @if (vitals.systolic && vitals.diastolic) {
                          <span class="flex items-center gap-1 font-semibold text-[var(--text-primary)]">
                            <span class="text-emerald-500 text-xs">💓</span> Tension : <strong>{{ vitals.systolic }}/{{ vitals.diastolic }} mmHg</strong>
                          </span>
                        }
                        @if (vitals.pulse) {
                          <span class="flex items-center gap-1 font-semibold text-[var(--text-primary)]">
                            <span class="text-[var(--brand-danger)] text-xs">🫀</span> Pouls : <strong>{{ vitals.pulse }} bpm</strong>
                          </span>
                        }
                      </div>
                    }

                    <!-- 2. Bilan Clinique avec Blocs Aérés et Badges -->
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div class="space-y-3">
                        <!-- Symptômes -->
                        <div class="p-3 rounded-lg bg-[var(--brand-warning-subtle)] border border-[var(--brand-warning-border)] dark:border-amber-500/20">
                          <span class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[9px] font-bold bg-[var(--brand-warning-muted)] text-[var(--brand-warning-text)] dark:text-amber-300 uppercase tracking-wider">
                            Symptômes signalés
                          </span>
                          <p class="text-sm font-semibold text-[var(--text-primary)] mt-1.5 leading-relaxed">{{ consult.symptoms }}</p>
                        </div>

                        <!-- Examen Clinique -->
                        @if (consult.clinicalExam) {
                          <div class="p-3 rounded-lg bg-[var(--brand-primary-subtle)] border border-[var(--brand-primary-border)] dark:border-indigo-500/20">
                            <span class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[9px] font-bold bg-indigo-500/10 text-indigo-700 dark:text-brand-cyan uppercase tracking-wider">
                              Examen Clinique
                            </span>
                            <p class="text-sm font-semibold text-[var(--text-primary)] mt-1.5 leading-relaxed">{{ consult.clinicalExam }}</p>
                          </div>
                        }
                      </div>

                      <div class="space-y-3">
                        <!-- Conseils & Recommandations -->
                        @if (consult.advice) {
                          <div class="p-3 rounded-lg bg-[var(--brand-success-subtle)] border border-[var(--brand-success-muted)] dark:border-emerald-500/20">
                            <span class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[9px] font-bold bg-emerald-500/10 text-[var(--brand-success-text)] uppercase tracking-wider">
                              Conseils & Recommandations
                            </span>
                            <p class="text-sm font-semibold text-[var(--text-primary)] mt-1.5 leading-relaxed">{{ consult.advice }}</p>
                          </div>
                        }

                        <!-- Suivi -->
                        @if (consult.followUp) {
                          <div class="p-3 rounded-lg bg-cyan-500/5 border border-cyan-500/10 dark:border-cyan-500/20">
                            <span class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[9px] font-bold bg-cyan-500/10 text-cyan-700 dark:text-cyan-300 uppercase tracking-wider">
                              Suivi Clinique
                            </span>
                            <p class="text-sm font-semibold text-[var(--text-primary)] mt-1.5 leading-relaxed">{{ consult.followUp }}</p>
                          </div>
                        }
                      </div>
                    </div>

                    <!-- 3. Prescription Médicale -->
                    @if (consult.prescriptionItems && consult.prescriptionItems.length > 0) {
                      <div class="space-y-2 pt-2 border-t border-[var(--app-border)]/40">
                        <h4 class="font-bold text-[10px] uppercase tracking-wider text-[var(--text-muted)]">Ordonnance Médicale</h4>

                        @if (consult.prescriptionNumber) {
                          <div class="flex flex-col sm:flex-row sm:items-center justify-between p-3 rounded bg-indigo-50/30 dark:bg-indigo-950/10 border border-indigo-100/50 dark:border-indigo-900/30 gap-2 mb-2">
                            <div class="text-xs text-[var(--text-secondary)]">
                              <div>{{ i18n.t('patients.prescriptionNumber') }} : <strong class="font-mono text-[var(--brand-info-text)]">{{ consult.prescriptionNumber }}</strong></div>
                              @if (consult.pinCode) {
                                <div class="mt-1">{{ i18n.t('patients.pinCode') }} : <strong class="font-mono bg-indigo-100 dark:bg-indigo-900/50 px-1.5 py-0.5 rounded text-indigo-700 dark:text-brand-cyan">{{ consult.pinCode }}</strong></div>
                              }
                            </div>
                            @if (consult.prescriptionDocumentId) {
                              <button
                                type="button"
                                (click)="downloadPrescriptionPdf(consult); $event.stopPropagation()"
                                class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded text-xs font-extrabold bg-indigo-600 text-white hover:bg-indigo-700 cursor-pointer transition-colors shadow-xs"
                              >
                                <svg class="w-3.5 h-3.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                                  <path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                                </svg>
                                {{ i18n.t('patients.downloadPrescription') }}
                              </button>
                            }
                          </div>
                        }
                        
                        <div class="space-y-2">
                          @for (item of consult.prescriptionItems; track item.id) {
                            <div class="p-3 rounded-lg bg-[var(--app-surface)] border border-[var(--app-border)]/60 flex items-start gap-3">
                              <span class="p-2 rounded bg-indigo-50 dark:bg-indigo-950/40 text-[var(--brand-info-text)] shrink-0 text-base">💊</span>
                              <div class="flex-1 min-w-0">
                                <div class="flex flex-wrap items-baseline gap-x-2">
                                  <h5 class="text-sm font-bold text-[var(--text-primary)]">{{ item.drugName }}</h5>
                                  <span class="text-xs text-[var(--text-muted)]">
                                    {{ item.dosage }} @if(item.quantity){(x{{ item.quantity }})}
                                  </span>
                                </div>
                                <p class="text-xs font-semibold text-[var(--text-secondary)] mt-0.5">
                                  Posologie : <span class="text-[var(--text-primary)]">{{ item.posology }}</span> @if(item.duration){pendant {{ item.duration }}}
                                </p>
                                @if (item.instructions) {
                                  <p class="text-[11px] text-[var(--text-muted)] italic mt-0.5">
                                    Instructions : {{ item.instructions }}
                                  </p>
                                }
                              </div>
                            </div>
                          }
                        </div>
                      </div>
                    }

                    <!-- Actions de document (Téléchargement et Révocation) -->
                    <div class="flex flex-wrap items-center justify-end gap-2 pt-2 border-t border-[var(--app-border)]/40">
                      @if (consult.documentStatus === 'REVOQUE') {
                        <span class="inline-flex items-center px-2 py-1 rounded-md text-[10px] font-extrabold bg-[var(--brand-warning-subtle)] text-[var(--brand-warning-text)] dark:bg-amber-950/30 dark:text-amber-300 uppercase tracking-wider">
                          {{ i18n.t('verify.status.revoked') }}
                        </span>
                      } @else if (consult.documentStatus === 'ANNULE') {
                        <span class="inline-flex items-center px-2 py-1 rounded-md text-[10px] font-extrabold bg-rose-50 text-rose-700 dark:bg-rose-950/30 dark:text-rose-300 uppercase tracking-wider">
                          {{ i18n.t('verify.status.cancelled') }}
                        </span>
                      }

                      @if (consult.documentId) {
                        <button
                          type="button"
                          (click)="downloadPdf(consult); $event.stopPropagation()"
                          class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-extrabold bg-indigo-50/50 text-indigo-700  dark:text-brand-cyan hover:bg-indigo-100 dark:hover:bg-indigo-950/50 cursor-pointer transition-colors shrink-0"
                        >
                          <svg class="w-3.5 h-3.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                          </svg>
                          {{ i18n.t('patients.downloadConsultation') }}
                        </button>

                        @if (canRevoke() && consult.documentStatus === 'VALID') {
                          <button
                            type="button"
                            (click)="openRevokeModal(consult); $event.stopPropagation()"
                            class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-extrabold bg-[var(--brand-danger-subtle)]/50 text-red-700 dark:bg-red-950/30 dark:text-[var(--brand-danger-text)] hover:bg-red-100 dark:hover:bg-red-950/50 cursor-pointer transition-colors shrink-0"
                          >
                            <svg class="w-3.5 h-3.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                              <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                            </svg>
                            {{ i18n.t('patients.revokeDoc') }}
                          </button>
                        }
                      }
                    </div>

                    @if (consult.prescriptionId) {
                      <div class="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-[var(--app-border)]/40">
                        <div class="flex flex-col">
                          <span class="text-[10px] font-bold uppercase tracking-wider text-[var(--text-muted)]">
                            {{ i18n.t('patient.prescription.teletransmission') }}
                          </span>
                          @if (consult.prescriptionTransmittedAt) {
                            <span class="text-[10px] text-[var(--text-muted)] mt-0.5">
                              {{ i18n.t('patient.prescription.transmittedAt') }} : {{ consult.prescriptionTransmittedAt | date:'dd/MM/yyyy HH:mm' }}
                            </span>
                          }
                        </div>
                        <div class="flex items-center gap-2">
                          @if (consult.prescriptionTransmissionStatus === 'TRANSMITTED') {
                            <span class="inline-flex items-center px-2.5 py-1 rounded-md text-[10px] font-extrabold bg-[var(--brand-success-subtle)] text-[var(--brand-success-text)] dark:bg-emerald-950/25 dark:text-emerald-400 uppercase tracking-wider">
                              {{ i18n.t('patient.prescription.transmitted') }}
                            </span>
                          } @else if (consult.prescriptionTransmissionStatus === 'PENDING') {
                            <span class="inline-flex items-center px-2.5 py-1 rounded-md text-[10px] font-extrabold bg-[var(--brand-warning-subtle)] text-[var(--brand-warning-text)] dark:bg-amber-950/25 dark:text-amber-400 uppercase tracking-wider animate-pulse">
                              {{ i18n.t('patient.prescription.pending') }}
                            </span>
                          } @else if (consult.prescriptionTransmissionStatus === 'FAILED') {
                            <span class="inline-flex items-center px-2.5 py-1 rounded-md text-[10px] font-extrabold bg-rose-50 text-rose-700 dark:bg-rose-950/25 dark:text-rose-400 uppercase tracking-wider">
                              {{ i18n.t('patient.prescription.failed') }}
                            </span>
                          } @else {
                            <span class="inline-flex items-center px-2.5 py-1 rounded-md text-[10px] font-extrabold bg-slate-100 text-[var(--text-secondary)] dark:bg-[var(--bg-input)] dark:text-[var(--text-muted)] uppercase tracking-wider">
                              {{ i18n.t('patient.prescription.notTransmitted') }}
                            </span>
                          }

                          @if (canTransmitPrescription() && consult.prescriptionTransmissionStatus !== 'TRANSMITTED') {
                            <button
                              type="button"
                              (click)="transmitPrescription(consult); $event.stopPropagation()"
                              [disabled]="consult.prescriptionTransmissionStatus === 'PENDING'"
                              class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-extrabold bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer transition-colors shrink-0"
                            >
                              {{ i18n.t('patient.prescription.transmitBtn') }}
                            </button>
                          }
                        </div>
                      </div>
                    }
                  </div>
                }
              </div>
            }
          </div>
        }
      </div>
    </div>

    <!-- Revocation Modal Dialogue -->
    @if (showRevokeModal) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 animate-fade-in">
        <div class="bg-[var(--app-surface)] border border-[var(--app-border)]/80 rounded-2xl max-w-md w-full shadow-2xl p-6 relative">
          <!-- Close button -->
          <button (click)="closeRevokeModal()" class="absolute top-4 right-4 text-[var(--text-muted)] hover:text-[var(--text-secondary)] dark:hover:text-[var(--text-secondary)] cursor-pointer">
            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>

          <!-- Header -->
          <h3 class="font-display font-bold text-lg text-[var(--text-primary)] mb-2">{{ i18n.t('patients.revokeModalTitle') }}</h3>
          <p class="text-xs text-[var(--text-muted)] mb-6">
            {{ i18n.t('patients.revokeModalSubtitle') }} <strong>{{ selectedConsultation()?.documentNumber }}</strong>.
          </p>

          @if (revokeError()) {
            <div class="p-3 bg-[var(--brand-danger-subtle)] border border-[var(--brand-danger-border)] rounded-xl text-xs text-[var(--brand-danger-text)] font-semibold mb-4 leading-relaxed">
              {{ revokeError() }}
            </div>
          }

          <!-- Form -->
          <div class="space-y-4">
            <div class="space-y-2">
              <label class="ui-label font-bold block">{{ i18n.t('patients.revokeActionType') }} <span class="text-[var(--brand-danger)]">*</span></label>
              <div class="flex gap-4">
                <label class="flex items-center gap-2 text-sm font-semibold cursor-pointer" style="color: var(--text-primary)">
                  <input type="radio" name="actionType" value="REVOKE" [(ngModel)]="revokeActionType" class="text-[var(--brand-info-text)] focus:ring-indigo-500">
                  {{ i18n.t('patients.revokeTypeRevoke') }}
                </label>
                <label class="flex items-center gap-2 text-sm font-semibold cursor-pointer" style="color: var(--text-primary)">
                  <input type="radio" name="actionType" value="CANCEL" [(ngModel)]="revokeActionType" class="text-[var(--brand-info-text)] focus:ring-indigo-500">
                  {{ i18n.t('patients.revokeTypeCancel') }}
                </label>
              </div>
              <p class="text-[10px] text-[var(--text-muted)] leading-relaxed">
                {{ revokeActionType === 'REVOKE' ? i18n.t('patients.revokeDescRevoke') : i18n.t('patients.revokeDescCancel') }}
              </p>
            </div>

            <div class="space-y-1.5">
              <label class="ui-label font-bold block">{{ i18n.t('patients.revokeReason') }} <span class="text-[var(--brand-danger)]">*</span></label>
              <textarea 
                [(ngModel)]="revokeReason"
                [placeholder]="i18n.t('patients.revokeReasonPlaceholder')"
                class="ui-textarea min-h-[80px] p-3 text-sm focus:border-brand-primary transition-colors"
                [disabled]="isRevokingSubmitting()"
              ></textarea>
              <div class="flex justify-between text-[10px] text-[var(--text-muted)] mt-1">
                <span>{{ revokeReason.length }}/500 {{ i18n.t('common.characters') }}</span>
                @if (revokeReason.length > 0 && revokeReason.length < 5) {
                  <span class="text-[var(--brand-danger)] font-semibold">{{ i18n.t('patients.revokeReasonMinChar') }}</span>
                }
              </div>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex justify-end gap-3 mt-8">
            <app-ui-button variant="secondary" (pressed)="closeRevokeModal()" [disabled]="isRevokingSubmitting()">
              {{ i18n.t('common.cancel') }}
            </app-ui-button>
            <app-ui-button variant="primary" (pressed)="submitRevocation()" [disabled]="isRevokingSubmitting() || !revokeReason || revokeReason.length < 5 || revokeReason.length > 500">
              {{ isRevokingSubmitting() ? i18n.t('common.processing') : i18n.t('common.confirm') }}
            </app-ui-button>
          </div>
        </div>
      </div>
    }
  `
})
export class PatientConsultationsTabComponent implements OnInit {
  readonly parent = inject(PatientDetailComponent);
  private readonly consultationApi = inject(ConsultationApiService);
  private readonly rbacApi = inject(RbacApiService);
  readonly i18n = inject(I18nService);


  readonly consultationHistory = signal<Consultation[]>([]);
  readonly isLoadingHistory = signal(false);
  readonly expandedConsultations = signal<Record<string, boolean>>({});
  readonly downloadError = signal<string | null>(null);

  // Revocation properties
  showRevokeModal = false;
  revokeReason = '';
  revokeActionType: 'REVOKE' | 'CANCEL' = 'REVOKE';
  revokeError = signal<string | null>(null);
  isRevokingSubmitting = signal(false);
  selectedConsultation = signal<Consultation | null>(null);

  formatDoctorName(name?: string): string {
    if (!name) return '';
    const trimmed = name.trim();
    return trimmed.toLowerCase().startsWith('dr') ? trimmed : `Dr. ${trimmed}`;
  }

  ngOnInit(): void {
    this.loadHistory();
  }

  loadHistory(): void {
    if (!this.rbacApi.hasPermission('CLINICAL_READ')) {
      return;
    }
    const patient = this.parent.patient();
    if (!patient) return;

    this.isLoadingHistory.set(true);
    this.consultationApi.getPatientConsultations(patient.id).subscribe({
      next: (data) => {
        this.consultationHistory.set(data);
        this.isLoadingHistory.set(false);
      },
      error: () => {
        this.isLoadingHistory.set(false);
      }
    });
  }

  toggleConsultation(id: string): void {
    this.expandedConsultations.update(prev => ({ ...prev, [id]: !prev[id] }));
  }

  downloadPdf(consultation: Consultation): void {
    this.downloadError.set(null);
    this.consultationApi.downloadDocument(consultation.visitId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Consultation_Visite_${consultation.visitNumber}.pdf`;
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

  downloadPrescriptionPdf(consultation: Consultation): void {
    if (!consultation.prescriptionDocumentId) return;
    this.downloadError.set(null);
    this.consultationApi.downloadDocumentById(consultation.prescriptionDocumentId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Ordonnance_${consultation.prescriptionNumber || consultation.visitNumber}.pdf`;
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

  transmitPrescription(consultation: Consultation): void {
    if (!consultation.prescriptionId) return;
    consultation.prescriptionTransmissionStatus = 'PENDING';
    this.consultationApi.transmitPrescription(consultation.prescriptionId).subscribe({
      next: () => {
        this.loadHistory();
      },
      error: (err) => {
        consultation.prescriptionTransmissionStatus = 'FAILED';
        alert(err.error?.detail || this.i18n.t('patient.prescription.transmitError'));
      }
    });
  }

  canRevoke(): boolean {
    return this.rbacApi.hasPermission('DOCUMENT_MANAGE');
  }

  canTransmitPrescription(): boolean {
    return this.rbacApi.hasPermission('CLINICAL_WRITE');
  }

  openRevokeModal(consultation: Consultation): void {
    this.selectedConsultation.set(consultation);
    this.revokeReason = '';
    this.revokeActionType = 'REVOKE';
    this.revokeError.set(null);
    this.showRevokeModal = true;
  }

  closeRevokeModal(): void {
    if (!this.isRevokingSubmitting()) {
      this.showRevokeModal = false;
      this.selectedConsultation.set(null);
    }
  }

  submitRevocation(): void {
    const consultation = this.selectedConsultation();
    if (!consultation || !consultation.documentId || this.isRevokingSubmitting()) {
      return;
    }

    this.isRevokingSubmitting.set(true);
    this.revokeError.set(null);

    const apiCall = this.revokeActionType === 'REVOKE'
      ? this.consultationApi.revokeDocument(consultation.documentId, this.revokeReason)
      : this.consultationApi.cancelDocument(consultation.documentId, this.revokeReason);

    apiCall.subscribe({
      next: (res) => {
        this.isRevokingSubmitting.set(false);
        this.showRevokeModal = false;

        this.consultationHistory.update(list =>
          list.map(c => c.id === consultation.id ? { ...c, documentStatus: res.status } : c)
        );

        this.selectedConsultation.set(null);
      },
      error: (err) => {
        this.isRevokingSubmitting.set(false);
        this.revokeError.set(err.error?.detail || err.error?.title || 'Une erreur est survenue lors de la révocation du document.');
      }
    });
  }
}
