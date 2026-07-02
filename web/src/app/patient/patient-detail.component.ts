import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { ButtonComponent } from '../shared/ui/button.component';
import { CardComponent } from '../shared/ui/card.component';
import { Patient } from './patient.models';
import { AuthTokenStorageService } from '../auth/auth-token-storage.service';
import { VisitApiService } from '../visit/visit-api.service';
import { Visit } from '../visit/visit.models';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { SlicePipe, DatePipe } from '@angular/common';
import { ConsultationApiService } from '../consultation/consultation-api.service';
import { Consultation } from '../consultation/consultation.models';
import { I18nService } from '../core/i18n/i18n.service';
import { AuditApiService } from '../audit/audit-api.service';
import { AuditLog } from '../audit/audit.models';

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  imports: [ButtonComponent, CardComponent, FormsModule, SlicePipe, DatePipe],
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
                    <div class="flex flex-wrap justify-between items-center pt-2 border-t border-slate-100/50 dark:border-slate-800/40 gap-4">
                      <p class="text-sm font-semibold" style="color: var(--text-primary)">
                        <span class="text-xs font-bold uppercase" style="color: var(--text-muted)">Diagnostic : </span>
                        {{ consult.diagnosis }}
                      </p>
                      <div class="flex items-center gap-2">
                        @if (consult.documentStatus === 'REVOQUE') {
                          <span class="inline-flex items-center px-2 py-1 rounded-md text-[10px] font-extrabold bg-amber-50 text-amber-700 dark:bg-amber-950/30 dark:text-amber-300 uppercase tracking-wider">
                            {{ i18n.t('verify.status.revoked') }}
                          </span>
                        } @else if (consult.documentStatus === 'ANNULE') {
                          <span class="inline-flex items-center px-2 py-1 rounded-md text-[10px] font-extrabold bg-rose-50 text-rose-700 dark:bg-rose-950/30 dark:text-rose-300 uppercase tracking-wider">
                            {{ i18n.t('verify.status.cancelled') }}
                          </span>
                        }

                        @if (consult.documentId) {
                          <button 
                            (click)="downloadPdf(consult)"
                            class="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg text-xs font-extrabold bg-indigo-50 text-indigo-700 dark:bg-indigo-950/30 dark:text-indigo-300 hover:bg-indigo-100 dark:hover:bg-indigo-950/50 cursor-pointer transition-colors shrink-0"
                          >
                            <svg class="w-3.5 h-3.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                              <path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                            </svg>
                            {{ i18n.t('patients.downloadPdf') }}
                          </button>

                          @if (canRevoke() && consult.documentStatus === 'VALID') {
                            <button 
                              (click)="openRevokeModal(consult)"
                              class="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg text-xs font-extrabold bg-red-50 text-red-700 dark:bg-red-950/30 dark:text-red-300 hover:bg-red-100 dark:hover:bg-red-950/50 cursor-pointer transition-colors shrink-0"
                            >
                              <svg class="w-3.5 h-3.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                              </svg>
                              {{ i18n.t('patients.revokeDoc') }}
                            </button>
                          }
                        }
                      </div>
                    </div>
                  </div>
                }
              }
            </div>
          }
        </div>

        <!-- Section 5: Journal d'Audit & Sécurité -->
        @if (canViewAudit()) {
          <div class="pt-4 border-t" style="border-color: var(--border-color)">
            <button
              (click)="toggleAudit()"
              class="flex items-center justify-between w-full text-left group cursor-pointer"
            >
              <h3 class="text-sm font-extrabold uppercase tracking-wider" style="color: var(--text-muted)">
                {{ i18n.t('patients.auditLogsTitle') }}
              </h3>
              <svg
                class="w-4 h-4 transition-transform duration-200"
                [class.rotate-180]="showAudit()"
                style="color: var(--text-muted)"
                fill="none" viewBox="0 0 24 24" stroke="currentColor"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
            </button>

            @if (showAudit()) {
              <div class="mt-4 space-y-3">
                @if (isLoadingAudit()) {
                  <div class="py-6 text-center">
                    <div class="inline-block w-5 h-5 rounded-full border-2 border-indigo-200 border-t-indigo-600 animate-spin"></div>
                    <p class="mt-2 text-xs font-semibold" style="color: var(--text-muted)">{{ i18n.t('patients.auditLogsLoading') }}</p>
                  </div>
                } @else if (auditLogs().length === 0) {
                  <div class="p-4 text-center border border-dashed border-slate-200 dark:border-slate-700 rounded-xl">
                    <p class="text-sm font-semibold" style="color: var(--text-muted)">{{ i18n.t('patients.auditLogsEmpty') }}</p>
                  </div>
                } @else {
                  <div class="flow-root">
                    <ul role="list" class="-mb-8">
                      @for (log of auditLogs(); track log.id; let last = $last) {
                        <li>
                          <div class="relative pb-8">
                            @if (!last) {
                              <span class="absolute top-4 left-4 -ml-px h-full w-0.5 bg-slate-200 dark:bg-slate-700" aria-hidden="true"></span>
                            }
                            <div class="relative flex space-x-3">
                              <div>
                                <span 
                                  [class]="log.status === 'SUCCESS' 
                                    ? 'h-8 w-8 rounded-full bg-green-50 dark:bg-green-950/30 text-green-600 dark:text-green-400 flex items-center justify-center ring-8 ring-white dark:ring-slate-900'
                                    : 'h-8 w-8 rounded-full bg-rose-50 dark:bg-rose-950/30 text-rose-600 dark:text-rose-400 flex items-center justify-center ring-8 ring-white dark:ring-slate-900'"
                                >
                                  @if (log.status === 'SUCCESS') {
                                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                                      <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                                    </svg>
                                  } @else {
                                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                                      <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                                    </svg>
                                  }
                                </span>
                              </div>
                              <div class="flex-1 min-w-0 pt-1.5 flex justify-between space-x-4">
                                <div>
                                  <p class="text-sm font-semibold text-slate-800 dark:text-slate-200">
                                    {{ log.reason || log.action }}
                                  </p>
                                  <p class="text-xs text-slate-400 dark:text-slate-500 mt-0.5">
                                    {{ i18n.t('patients.auditLogsUser') }} <span class="font-bold text-indigo-600 dark:text-indigo-400">{{ log.actorName || 'Système' }}</span>
                                    | {{ i18n.t('patients.auditLogsAction') }} <span class="font-mono text-[10px] font-bold">{{ log.action }}</span> 
                                    @if (log.ipAddress) {
                                      | {{ i18n.t('patients.auditLogsIp') }} <span class="font-mono text-[10px]">{{ log.ipAddress }}</span>
                                    }
                                  </p>
                                </div>
                                <div class="text-right text-xs whitespace-nowrap text-slate-400 dark:text-slate-500">
                                  <time [dateTime]="log.createdAt">{{ log.createdAt | date:'short' }}</time>
                                </div>
                              </div>
                            </div>
                          </div>
                        </li>
                      }
                    </ul>
                  </div>
                }
              </div>
            }
          </div>
        }

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

    <!-- Revocation Modal Dialogue -->
    @if (showRevokeModal) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 animate-fade-in">
        <div class="bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800/80 rounded-2xl max-w-md w-full shadow-2xl p-6 relative">
          <!-- Close button -->
          <button (click)="closeRevokeModal()" class="absolute top-4 right-4 text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 cursor-pointer">
            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>

          <!-- Header -->
          <h3 class="font-display font-bold text-lg text-brand-night dark:text-white mb-2">{{ i18n.t('patients.revokeModalTitle') }}</h3>
          <p class="text-xs text-slate-500 dark:text-slate-400 mb-6">
            {{ i18n.t('patients.revokeModalSubtitle') }} <strong>{{ selectedConsultation()?.documentNumber }}</strong>.
          </p>

          @if (revokeError()) {
            <div class="p-3 bg-red-50 dark:bg-red-950/20 border border-red-100 dark:border-red-900/30 rounded-xl text-xs text-red-700 dark:text-red-300 font-semibold mb-4 leading-relaxed">
              {{ revokeError() }}
            </div>
          }

          <!-- Form -->
          <div class="space-y-4">
            <div class="space-y-2">
              <label class="ui-label font-bold block">{{ i18n.t('patients.revokeActionType') }} <span class="text-red-500">*</span></label>
              <div class="flex gap-4">
                <label class="flex items-center gap-2 text-sm font-semibold cursor-pointer" style="color: var(--text-primary)">
                  <input type="radio" name="actionType" value="REVOKE" [(ngModel)]="revokeActionType" class="text-indigo-600 focus:ring-indigo-500">
                  {{ i18n.t('patients.revokeTypeRevoke') }}
                </label>
                <label class="flex items-center gap-2 text-sm font-semibold cursor-pointer" style="color: var(--text-primary)">
                  <input type="radio" name="actionType" value="CANCEL" [(ngModel)]="revokeActionType" class="text-indigo-600 focus:ring-indigo-500">
                  {{ i18n.t('patients.revokeTypeCancel') }}
                </label>
              </div>
              <p class="text-[10px] text-slate-400 leading-relaxed">
                {{ revokeActionType === 'REVOKE' ? i18n.t('patients.revokeDescRevoke') : i18n.t('patients.revokeDescCancel') }}
              </p>
            </div>

            <div class="space-y-1.5">
              <label class="ui-label font-bold block">{{ i18n.t('patients.revokeReason') }} <span class="text-red-500">*</span></label>
              <textarea 
                [(ngModel)]="revokeReason"
                [placeholder]="i18n.t('patients.revokeReasonPlaceholder')"
                class="ui-textarea min-h-[80px] p-3 text-sm focus:border-brand-primary transition-colors"
                [disabled]="isRevokingSubmitting()"
              ></textarea>
              <div class="flex justify-between text-[10px] text-slate-400 mt-1">
                <span>{{ revokeReason.length }}/500 {{ i18n.t('common.characters') }}</span>
                @if (revokeReason.length > 0 && revokeReason.length < 5) {
                  <span class="text-red-500 font-semibold">{{ i18n.t('patients.revokeReasonMinChar') }}</span>
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

  // STORY-0604 — Properties for revocation
  showRevokeModal = false;
  revokeReason = '';
  revokeActionType: 'REVOKE' | 'CANCEL' = 'REVOKE';
  revokeError = signal<string | null>(null);
  isRevokingSubmitting = signal(false);
  selectedConsultation = signal<Consultation | null>(null);

  consultationHistory = signal<Consultation[]>([]);
  showHistory = signal(false);
  isLoadingHistory = signal(false);
  historyLoaded = false; // garde pour éviter double chargement

  // STORY-0702 — Properties for Audit
  showAudit = signal(false);
  isLoadingAudit = signal(false);
  auditLogs = signal<AuditLog[]>([]);
  auditLoaded = false;

  activeVisit = signal<Visit | null>(null);

  private readonly consultationApi = inject(ConsultationApiService);
  private readonly auditApi = inject(AuditApiService);

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
    return (role === 'MEDECIN' || role === 'ADMIN_CLINIQUE' && this.activeVisit() !== null);
  });

  readonly canRevoke = computed(() => {
    const role = this.session()?.role;
    return role === 'MEDECIN' || role === 'ADMIN_CLINIQUE';
  });

  readonly canViewAudit = computed(() => {
    const role = this.session()?.role;
    return role === 'MEDECIN' || role === 'ADMIN_CLINIQUE' || role === 'AUDITEUR';
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

  // STORY-0604 — Revocation methods
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

        // Update document status locally in the list
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

  loadAudit(): void {
    if (!this.canViewAudit()) {
      return;
    }
    this.isLoadingAudit.set(true);
    this.auditApi.getPatientLogs(this.patient().id).subscribe({
      next: (data) => {
        this.auditLogs.set(data);
        this.isLoadingAudit.set(false);
        this.auditLoaded = true;
      },
      error: () => {
        this.isLoadingAudit.set(false);
        this.auditLoaded = true;
      }
    });
  }

  toggleAudit(): void {
    const newState = !this.showAudit();
    this.showAudit.set(newState);
    if (newState && !this.auditLoaded) {
      this.loadAudit();
    }
  }
}

