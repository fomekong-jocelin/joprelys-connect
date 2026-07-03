import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { ButtonComponent } from '../shared/ui/button.component';
import { CardComponent } from '../shared/ui/card.component';
import { Patient, LabOrder, LabResult, PatientAllergy } from './patient.models';
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
import { PatientApiService } from './patient-api.service';
import { PatientMedicalInfoComponent } from './patient-medical-info.component';
import { PatientHospitalizationComponent } from './patient-hospitalization.component';

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  imports: [ButtonComponent, CardComponent, FormsModule, SlicePipe, DatePipe, PatientMedicalInfoComponent, PatientHospitalizationComponent],
  template: `
    <app-ui-card>
      <div class="space-y-6">
        
        <!-- En-tête : Nom du Patient, Numéros & Actions principales -->
        <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center pb-4 border-b border-slate-100 dark:border-slate-800/80 gap-4">
          <div>
            <div class="flex items-center gap-3">
              <h2 class="text-xl font-black text-slate-800 dark:text-white">{{ patient().fullName }}</h2>
              <span class="inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-extrabold bg-green-50 text-green-700 dark:bg-green-950/30 dark:text-green-300 uppercase tracking-wider">
                {{ patient().status }}
              </span>
            </div>
            <p class="text-xs text-slate-400 dark:text-slate-500 mt-1">
              DPU: <span class="font-mono font-bold text-slate-600 dark:text-slate-400">{{ patient().globalPatientNumber }}</span> 
              | Etablissement: <span class="font-mono text-slate-600 dark:text-slate-400">{{ patient().localPatientNumber }}</span>
            </p>
          </div>
          
          <div class="flex items-center gap-2 w-full sm:w-auto">
            <app-ui-button variant="secondary" (pressed)="back.emit()" class="grow sm:grow-0 text-xs">
              Retour
            </app-ui-button>
            
            @if (canStartConsultation()) {
              <app-ui-button variant="primary" (pressed)="goToConsultation()" class="grow sm:grow-0 text-xs">
                Démarrer la consultation
              </app-ui-button>
            } @else if (canAdmit()) {
              <app-ui-button variant="primary" (pressed)="openModal()" class="grow sm:grow-0 text-xs">
                Ouvrir une visite
              </app-ui-button>
            }
          </div>
        </div>

        @if (patient().emergencyAccessActive) {
          <div class="p-4 rounded-[var(--radius-brand-md)] bg-rose-50 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-800/40 text-rose-800 dark:text-rose-300 text-sm font-bold flex items-center gap-3 animate-pulse">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="w-5 h-5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <span>Procédure d'urgence "Brise-Glace" active : accès temporaire tracé dans le journal d'audit de sécurité.</span>
          </div>
        }

        @if (criticalAllergies().length > 0) {
          <div class="p-4 rounded-[var(--radius-brand-md)] bg-red-50 dark:bg-red-950/20 border border-red-200 dark:border-red-800/40 text-red-800 dark:text-red-300 text-sm font-bold flex items-center gap-3">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="w-5 h-5 animate-bounce">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <div>
              <span>Attention : allergies critiques ou sévères détectées pour ce patient :</span>
              <span class="ml-1 font-extrabold">{{ criticalAllergiesSubstances() }}</span>
            </div>
          </div>
        }

        <!-- Sélecteur d'onglets (Tabs) -->
        <div class="border-b border-slate-100 dark:border-slate-800/80">
          <nav class="flex space-x-6" aria-label="Tabs">
            <button
              (click)="setActiveTab('general')"
              [class]="activeTab() === 'general'
                ? 'border-indigo-500 text-indigo-600 dark:text-indigo-400 border-b-2 py-2 px-1 text-sm font-extrabold cursor-pointer transition-all'
                : 'border-transparent text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 border-b-2 py-2 px-1 text-sm font-bold cursor-pointer transition-all'"
            >
              Fiche Patient
            </button>
            <button
              (click)="setActiveTab('medical')"
              [class]="activeTab() === 'medical'
                ? 'border-indigo-500 text-indigo-600 dark:text-indigo-400 border-b-2 py-2 px-1 text-sm font-extrabold cursor-pointer transition-all'
                : 'border-transparent text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 border-b-2 py-2 px-1 text-sm font-bold cursor-pointer transition-all'"
            >
              Dossier Médical
            </button>
            @if (canViewAudit()) {
              <button
                (click)="setActiveTab('audit')"
                [class]="activeTab() === 'audit'
                  ? 'border-indigo-500 text-indigo-600 dark:text-indigo-400 border-b-2 py-2 px-1 text-sm font-extrabold cursor-pointer transition-all'
                  : 'border-transparent text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 border-b-2 py-2 px-1 text-sm font-bold cursor-pointer transition-all'"
              >
                Journal d'Audit
              </button>
            }
            <button
              (click)="setActiveTab('lab')"
              [class]="activeTab() === 'lab'
                ? 'border-indigo-500 text-indigo-600 dark:text-indigo-400 border-b-2 py-2 px-1 text-sm font-extrabold cursor-pointer transition-all'
                : 'border-transparent text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 border-b-2 py-2 px-1 text-sm font-bold cursor-pointer transition-all'"
            >
              Analyses & Labo
            </button>
            <button
              (click)="setActiveTab('hospitalization')"
              [class]="activeTab() === 'hospitalization'
                ? 'border-indigo-500 text-indigo-600 dark:text-indigo-400 border-b-2 py-2 px-1 text-sm font-extrabold cursor-pointer transition-all'
                : 'border-transparent text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 border-b-2 py-2 px-1 text-sm font-bold cursor-pointer transition-all'"
            >
              {{ i18n.t('patients.hospitalization') }}
            </button>
          </nav>
        </div>

        <!-- Onglet 1 : Général (Informations Administratives & Contacts) -->
        @if (activeTab() === 'general') {
          <div class="space-y-6 animate-fade-in">
            <div>
              <h3 class="text-xs font-black uppercase tracking-wider text-slate-400 dark:text-slate-500 mb-4">
                Informations Administratives
              </h3>
              <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                <div class="p-3 bg-slate-50/50 dark:bg-slate-800/30 border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
                  <span class="ui-label block text-[10px] text-slate-400 dark:text-slate-500">Sexe</span>
                  <span class="font-extrabold text-sm text-slate-800 dark:text-slate-200">{{ patient().gender }}</span>
                </div>
                <div class="p-3 bg-slate-50/50 dark:bg-slate-800/30 border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
                  <span class="ui-label block text-[10px] text-slate-400 dark:text-slate-500">Date de naissance (âge)</span>
                  <span class="font-extrabold text-sm text-slate-800 dark:text-slate-200">{{ patient().birthDate }} ({{ age() }} ans)</span>
                </div>
                <div class="p-3 bg-slate-50/50 dark:bg-slate-800/30 border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
                  <span class="ui-label block text-[10px] text-slate-400 dark:text-slate-500">Téléphone</span>
                  <span class="font-extrabold text-sm text-slate-800 dark:text-slate-200">{{ patient().phone || 'Non renseigné' }}</span>
                </div>
                <div class="p-3 bg-slate-50/50 dark:bg-slate-800/30 border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
                  <span class="ui-label block text-[10px] text-slate-400 dark:text-slate-500">Ville</span>
                  <span class="font-extrabold text-sm text-slate-800 dark:text-slate-200 capitalize">{{ patient().city }}</span>
                </div>
                <div class="p-3 bg-slate-50/50 dark:bg-slate-800/30 border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
                  <span class="ui-label block text-[10px] text-slate-400 dark:text-slate-500">Quartier / District</span>
                  <span class="font-extrabold text-sm text-slate-800 dark:text-slate-200">{{ patient().district || 'Non renseigné' }}</span>
                </div>
                <div class="p-3 bg-slate-50/50 dark:bg-slate-800/30 border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
                  <span class="ui-label block text-[10px] text-slate-400 dark:text-slate-500">Adresse Géographique</span>
                  <span class="font-extrabold text-sm text-slate-800 dark:text-slate-200">{{ patient().address || 'Non renseignée' }}</span>
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
                  <span class="font-extrabold text-sm text-slate-800 dark:text-slate-200">{{ patient().emergencyContactName || 'Non renseigné' }}</span>
                </div>
                <div class="p-3 bg-slate-50/50 dark:bg-slate-800/30 border border-slate-100/50 dark:border-slate-800/40 rounded-lg">
                  <span class="ui-label block text-[10px] text-slate-400 dark:text-slate-500">Téléphone</span>
                  <span class="font-extrabold text-sm text-slate-800 dark:text-slate-200">{{ patient().emergencyContactPhone || 'Non renseigné' }}</span>
                </div>
              </div>
            </div>
          </div>
        }

        <!-- Onglet 2 : Dossier Médical (Allergies, Antécédents & Consultations) -->
        @if (activeTab() === 'medical') {
          <div class="space-y-6 animate-fade-in">
            <app-patient-medical-info [patientId]="patient().id"></app-patient-medical-info>

            <div>
              <h3 class="text-xs font-black uppercase tracking-wider text-slate-400 dark:text-slate-500 mb-4">
                Historique Médical & Consultations
              </h3>

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
                <div class="py-12 text-center">
                  <div class="inline-block w-6 h-6 rounded-full border-2 border-indigo-200 border-t-indigo-600 animate-spin"></div>
                  <p class="mt-2 text-xs font-bold text-slate-400 dark:text-slate-500">Chargement de l'historique...</p>
                </div>
              } @else if (consultationHistory().length === 0) {
                <div class="p-8 text-center border border-dashed border-slate-200 dark:border-slate-800/80 rounded-xl">
                  <p class="text-sm font-semibold text-slate-400 dark:text-slate-500">Aucun antécédent de consultation enregistré.</p>
                </div>
              } @else {
                <div class="space-y-3">
                  @for (consult of consultationHistory(); track consult.id) {
                    <div class="rounded-xl bg-slate-50/50 dark:bg-slate-800/20 border border-slate-100 dark:border-slate-800/60 overflow-hidden transition-all duration-200">
                      <!-- En-tête cliquable de l'accordéon -->
                      <div
                        (click)="toggleConsultation(consult.id)"
                        class="p-4 flex items-center justify-between cursor-pointer hover:bg-slate-100/30 dark:hover:bg-slate-800/40 select-none transition-colors"
                      >
                        <div class="flex flex-wrap items-center gap-3">
                          <span class="inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-extrabold bg-indigo-50 text-indigo-700 dark:bg-indigo-950/30 dark:text-indigo-300 uppercase tracking-wider">
                            {{ consult.visitNumber }}
                          </span>
                          <span class="text-xs font-semibold text-slate-400 dark:text-slate-500">
                            {{ consult.createdAt | slice:0:10 }}
                          </span>
                          <p class="text-sm font-bold text-slate-800 dark:text-slate-200 line-clamp-1">
                            <span class="text-xs font-bold uppercase text-slate-400 dark:text-slate-500">Diag : </span>
                            {{ consult.diagnosis }}
                          </p>
                        </div>

                        <div class="flex items-center gap-3">
                          <span class="text-xs font-extrabold text-slate-500 dark:text-slate-400 hidden sm:inline">Dr. {{ consult.doctorName }}</span>
                          
                          <!-- Chevron de déploiement -->
                          <svg
                            class="w-4 h-4 text-slate-400 transform transition-transform duration-200"
                            [class.rotate-180]="expandedConsultations()[consult.id]"
                            fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"
                          >
                            <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
                          </svg>
                        </div>
                      </div>

                      <!-- Corps déplié de l'accordéon -->
                      @if (expandedConsultations()[consult.id]) {
                        <div class="px-4 pb-4 pt-2 border-t border-slate-100 dark:border-slate-800/40 space-y-4 animate-fade-in text-slate-700 dark:text-slate-300 text-xs">
                          
                          <!-- 1. Constantes Vitales en ligne de type barre médicale -->
                          @if (consult.vitals; as vitals) {
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
                              <div class="p-3 rounded-lg bg-amber-500/5 border border-amber-500/10 dark:border-amber-500/20">
                                <span class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[9px] font-bold bg-amber-500/10 text-amber-700 dark:text-amber-300 uppercase tracking-wider">
                                  Symptômes signalés
                                </span>
                                <p class="text-sm font-semibold text-slate-800 dark:text-slate-200 mt-1.5 leading-relaxed">{{ consult.symptoms }}</p>
                              </div>

                              <!-- Examen Clinique -->
                              @if (consult.clinicalExam) {
                                <div class="p-3 rounded-lg bg-indigo-500/5 border border-indigo-500/10 dark:border-indigo-500/20">
                                  <span class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[9px] font-bold bg-indigo-500/10 text-indigo-700 dark:text-indigo-300 uppercase tracking-wider">
                                    Examen Clinique
                                  </span>
                                  <p class="text-sm font-semibold text-slate-800 dark:text-slate-200 mt-1.5 leading-relaxed">{{ consult.clinicalExam }}</p>
                                </div>
                              }
                            </div>

                            <div class="space-y-3">
                              <!-- Conseils & Recommandations -->
                              @if (consult.advice) {
                                <div class="p-3 rounded-lg bg-emerald-500/5 border border-emerald-500/10 dark:border-emerald-500/20">
                                  <span class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[9px] font-bold bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 uppercase tracking-wider">
                                    Conseils & Recommandations
                                  </span>
                                  <p class="text-sm font-semibold text-slate-800 dark:text-slate-200 mt-1.5 leading-relaxed">{{ consult.advice }}</p>
                                </div>
                              }

                              <!-- Suivi -->
                              @if (consult.followUp) {
                                <div class="p-3 rounded-lg bg-cyan-500/5 border border-cyan-500/10 dark:border-cyan-500/20">
                                  <span class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[9px] font-bold bg-cyan-500/10 text-cyan-700 dark:text-cyan-300 uppercase tracking-wider">
                                    Suivi Clinique
                                  </span>
                                  <p class="text-sm font-semibold text-slate-800 dark:text-slate-200 mt-1.5 leading-relaxed">{{ consult.followUp }}</p>
                                </div>
                              }
                            </div>
                          </div>

                          <!-- 3. Prescription Médicale sous forme de lignes fluides épurées (sans tableau froid) -->
                          @if (consult.prescriptionItems && consult.prescriptionItems.length > 0) {
                            <div class="space-y-2 pt-2 border-t border-slate-100 dark:border-slate-800/40">
                              <h4 class="font-bold text-[10px] uppercase tracking-wider text-slate-400 dark:text-slate-500">Ordonnance Médicale</h4>
                              
                              <div class="space-y-2">
                                @for (item of consult.prescriptionItems; track item.id) {
                                  <div class="p-3 rounded-lg bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800/60 flex items-start gap-3">
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

                          <!-- Actions de document (Téléchargement et Révocation) -->
                          <div class="flex flex-wrap items-center justify-end gap-2 pt-2 border-t border-slate-100 dark:border-slate-800/40">
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
                                type="button"
                                (click)="downloadPdf(consult); $event.stopPropagation()"
                                class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-extrabold bg-indigo-50/50 text-indigo-700 dark:bg-indigo-950/30 dark:text-indigo-300 hover:bg-indigo-100 dark:hover:bg-indigo-950/50 cursor-pointer transition-colors shrink-0"
                              >
                                <svg class="w-3.5 h-3.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                                  <path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                                </svg>
                                {{ i18n.t('patients.downloadPdf') }}
                              </button>

                              @if (canRevoke() && consult.documentStatus === 'VALID') {
                                <button
                                  type="button"
                                  (click)="openRevokeModal(consult); $event.stopPropagation()"
                                  class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-extrabold bg-red-50/50 text-red-700 dark:bg-red-950/30 dark:text-red-300 hover:bg-red-100 dark:hover:bg-red-950/50 cursor-pointer transition-colors shrink-0"
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
                      }
                    </div>
                  }
                </div>
              }
            </div>
          </div>
        }

        <!-- Onglet 3 : Journal d'Audit & Sécurité -->
        @if (activeTab() === 'audit' && canViewAudit()) {
          <div class="space-y-4 animate-fade-in">
            <h3 class="text-xs font-black uppercase tracking-wider text-slate-400 dark:text-slate-500 mb-4">
              {{ i18n.t('patients.auditLogsTitle') }}
            </h3>

            @if (isLoadingAudit()) {
              <div class="py-12 text-center">
                <div class="inline-block w-6 h-6 rounded-full border-2 border-indigo-200 border-t-indigo-600 animate-spin"></div>
                <p class="mt-2 text-xs font-bold text-slate-400 dark:text-slate-500">{{ i18n.t('patients.auditLogsLoading') }}</p>
              </div>
            } @else if (auditLogs().length === 0) {
              <div class="p-8 text-center border border-dashed border-slate-200 dark:border-slate-800/80 rounded-xl">
                <p class="text-sm font-semibold text-slate-400 dark:text-slate-500">{{ i18n.t('patients.auditLogsEmpty') }}</p>
              </div>
            } @else {
              <div class="flow-root px-4">
                <ul role="list" class="-mb-8">
                  @for (log of auditLogs(); track log.id; let last = $last) {
                    <li>
                      <div class="relative pb-8">
                        @if (!last) {
                          <span class="absolute top-4 left-4 -ml-px h-full w-0.5 bg-slate-100 dark:bg-slate-800" aria-hidden="true"></span>
                        }
                        <div class="relative flex space-x-3">
                          <div>
                            <span 
                              [class]="log.status === 'SUCCESS' 
                                ? 'h-8 w-8 rounded-full bg-green-50 dark:bg-green-950/20 text-green-600 dark:text-green-400 flex items-center justify-center ring-8 ring-white dark:ring-slate-900'
                                : 'h-8 w-8 rounded-full bg-rose-50 dark:bg-rose-950/20 text-rose-600 dark:text-rose-400 flex items-center justify-center ring-8 ring-white dark:ring-slate-900'"
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

        <!-- Onglet 4 : Analyses & Labo (Résultats structurés et demandes) -->
        @if (activeTab() === 'lab') {
          <div class="space-y-6 animate-fade-in text-xs text-slate-700 dark:text-slate-300">
            
            @if (isLoadingLab()) {
              <div class="py-12 text-center">
                <div class="inline-block w-6 h-6 rounded-full border-2 border-indigo-200 border-t-indigo-600 animate-spin"></div>
                <p class="mt-2 text-xs font-bold text-slate-400 dark:text-slate-500">Chargement des données de laboratoire...</p>
              </div>
            } @else {
              
              <!-- 1. Graphique d'évolution si des résultats existent -->
              @if (analyteNames().length > 0) {
                <div class="p-4 bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800/80 rounded-xl space-y-4 shadow-xs">
                  <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 pb-3 border-b border-slate-100 dark:border-slate-800/40">
                    <div>
                      <h4 class="font-extrabold text-sm text-slate-800 dark:text-white">Évolution des Marqueurs Biologiques</h4>
                      <p class="text-[10px] text-slate-400 dark:text-slate-500">Sélectionnez un analyte pour afficher sa courbe de tendance.</p>
                    </div>
                    <select
                      [ngModel]="selectedAnalyte()"
                      (ngModelChange)="selectedAnalyte.set($event)"
                      class="ui-select max-w-xs focus:border-brand-primary transition-colors text-xs py-1.5"
                    >
                      @for (name of analyteNames(); track name) {
                        <option [value]="name">{{ name }}</option>
                      }
                    </select>
                  </div>

                  <!-- Tracé SVG réactif -->
                  @if (filteredResults().length > 0) {
                    <div class="space-y-2">
                      <div class="overflow-x-auto">
                        <svg viewBox="0 0 500 200" class="w-full min-w-[400px] h-48 bg-slate-50/50 dark:bg-slate-900/60 rounded-lg border border-slate-100 dark:border-slate-800/40 p-2">
                          <!-- Grille -->
                          <line x1="30" y1="30" x2="470" y2="30" stroke="#e2e8f0" stroke-dasharray="3 3" stroke-width="0.5" class="dark:stroke-slate-800" />
                          <line x1="30" y1="100" x2="470" y2="100" stroke="#e2e8f0" stroke-dasharray="3 3" stroke-width="0.5" class="dark:stroke-slate-800" />
                          <line x1="30" y1="170" x2="470" y2="170" stroke="#e2e8f0" stroke-dasharray="3 3" stroke-width="0.5" class="dark:stroke-slate-800" />

                          <!-- Ligne de tendance -->
                          @if (chartPoints().length > 1) {
                            <polyline
                              fill="none"
                              stroke="#6366f1"
                              stroke-width="2.5"
                              [attr.points]="getPolylinePoints()"
                            />
                          }

                          <!-- Points -->
                          @for (p of chartPoints(); track p.x) {
                            <circle
                              [attr.cx]="p.x"
                              [attr.cy]="p.y"
                              r="5"
                              [attr.fill]="getInterpretationColor(p.interpretation)"
                              stroke="white"
                              stroke-width="1.5"
                              class="cursor-pointer"
                            />
                            <!-- Valeur -->
                            <text
                              [attr.x]="p.x"
                              [attr.y]="p.y - 10"
                              text-anchor="middle"
                              class="text-[9px] font-extrabold fill-slate-800 dark:fill-slate-200"
                            >
                              {{ p.val }}
                            </text>
                            <!-- Date -->
                            <text
                              [attr.x]="p.x"
                              y="192"
                              text-anchor="middle"
                              class="text-[8px] font-bold fill-slate-400 dark:fill-slate-500"
                            >
                              {{ p.date }}
                            </text>
                          }
                        </svg>
                      </div>

                      <!-- Légende et interprétations -->
                      <div class="flex flex-wrap items-center gap-x-4 gap-y-1.5 text-[10px] text-slate-400">
                        <span class="font-bold">Interprétations :</span>
                        <span class="flex items-center gap-1 font-semibold text-slate-700 dark:text-slate-300">
                          <span class="w-2.5 h-2.5 rounded bg-[#10b981]"></span> Normal
                        </span>
                        <span class="flex items-center gap-1 font-semibold text-slate-700 dark:text-slate-300">
                          <span class="w-2.5 h-2.5 rounded bg-[#3b82f6]"></span> Bas
                        </span>
                        <span class="flex items-center gap-1 font-semibold text-slate-700 dark:text-slate-300">
                          <span class="w-2.5 h-2.5 rounded bg-[#f97316]"></span> Élevé
                        </span>
                        <span class="flex items-center gap-1 font-semibold text-slate-700 dark:text-slate-300">
                          <span class="w-2.5 h-2.5 rounded bg-[#ef4444]"></span> Critique
                        </span>
                      </div>
                    </div>
                  }
                </div>
              }

              <!-- 2. Liste des résultats structurés détaillés -->
              @if (labResults().length > 0) {
                <div class="space-y-3">
                  <h4 class="text-xs font-black uppercase tracking-wider text-slate-400 dark:text-slate-500 mb-2">Historique des Analyses</h4>
                  
                  <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    @for (res of labResults(); track res.id) {
                      <div class="p-4 rounded-xl bg-slate-50/50 dark:bg-slate-800/20 border border-slate-100 dark:border-slate-800/60 space-y-3 shadow-xs">
                        <div class="flex justify-between items-start">
                          <div>
                            <span class="inline-flex items-center px-1.5 py-0.5 rounded text-[9px] font-bold bg-indigo-50 text-indigo-700 dark:bg-indigo-950/30 dark:text-indigo-300 uppercase tracking-wider">
                              {{ res.resultNumber }}
                            </span>
                            <h5 class="text-sm font-bold text-slate-800 dark:text-white mt-1">{{ res.analyteName }}</h5>
                          </div>
                          
                          <!-- Badge interpretation -->
                          <span
                            class="inline-flex items-center px-2 py-0.5 rounded text-[9px] font-extrabold uppercase tracking-wider"
                            [style.background-color]="getInterpretationColor(res.interpretation) + '15'"
                            [style.color]="getInterpretationColor(res.interpretation)"
                          >
                            {{ res.interpretation }}
                          </span>
                        </div>

                        <div class="grid grid-cols-2 gap-2 text-xs">
                          <div class="p-2 rounded bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800/40">
                            <span class="block text-[9px] font-bold text-slate-400 uppercase">Valeur mesurée</span>
                            <span class="font-black text-sm text-slate-800 dark:text-white">{{ res.value }} {{ res.unit || '' }}</span>
                          </div>
                          <div class="p-2 rounded bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800/40">
                            <span class="block text-[9px] font-bold text-slate-400 uppercase">Valeurs de référence</span>
                            <span class="font-bold text-slate-700 dark:text-slate-300">{{ res.referenceRange || 'N/A' }}</span>
                          </div>
                        </div>

                        @if (res.comment) {
                          <p class="text-xs italic text-slate-500 dark:text-slate-400 leading-relaxed">
                            <strong>Note :</strong> {{ res.comment }}
                          </p>
                        }

                        <div class="flex justify-between items-center text-[10px] text-slate-400 dark:text-slate-500 pt-2 border-t border-slate-100 dark:border-slate-800/40">
                          <span>Validé par : <strong>{{ res.validatorName }}</strong></span>
                          <span>Le : <strong>{{ (res.validatedAt || res.createdAt) | slice:0:10 }}</strong></span>
                        </div>
                      </div>
                    }
                  </div>
                </div>
              }

              <!-- 3. Demandes d'analyses (LabOrders) -->
              <div class="space-y-3">
                <h4 class="text-xs font-black uppercase tracking-wider text-slate-400 dark:text-slate-500 mb-2">Demandes d'Examens Biologiques</h4>

                @if (labOrders().length === 0) {
                  <div class="p-8 text-center border border-dashed border-slate-200 dark:border-slate-800/80 rounded-xl">
                    <p class="text-sm font-semibold text-slate-400 dark:text-slate-500">Aucune demande d'examen enregistrée.</p>
                  </div>
                } @else {
                  <div class="space-y-2">
                    @for (order of labOrders(); track order.id) {
                      <div class="rounded-xl bg-slate-50/50 dark:bg-slate-800/20 border border-slate-100 dark:border-slate-800/60 overflow-hidden">
                        <!-- En-tête cliquable -->
                        <div
                           (click)="toggleLabOrder(order.id)"
                           class="p-4 flex items-center justify-between cursor-pointer hover:bg-slate-100/30 dark:hover:bg-slate-800/40 select-none transition-colors"
                        >
                          <div class="flex flex-wrap items-center gap-3">
                            <span class="inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-extrabold bg-indigo-50 text-indigo-700 dark:bg-indigo-950/30 dark:text-indigo-300 uppercase tracking-wider">
                              {{ order.examRequestNumber }}
                            </span>
                            <span class="text-xs font-semibold text-slate-400 dark:text-slate-500">
                              {{ order.createdAt | slice:0:10 }}
                            </span>
                            <span
                              class="inline-flex items-center px-2 py-0.5 rounded text-[9px] font-extrabold uppercase tracking-wider"
                              [class]="order.status === 'VALIDATED' 
                                ? 'bg-green-50 text-green-700 dark:bg-green-950/20 dark:text-green-300'
                                : 'bg-amber-50 text-amber-700 dark:bg-amber-950/20 dark:text-amber-300'"
                            >
                              {{ order.status }}
                            </span>
                          </div>

                          <div class="flex items-center gap-3">
                            <span class="text-xs font-bold text-slate-500">Prescrit par : Dr. {{ order.requesterPractitionerName }}</span>
                            <!-- Chevron -->
                            <svg
                              class="w-4 h-4 text-slate-400 transform transition-transform duration-200"
                              [class.rotate-180]="expandedLabOrders()[order.id]"
                              fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"
                            >
                              <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
                            </svg>
                          </div>
                        </div>

                        <!-- Corps -->
                        @if (expandedLabOrders()[order.id]) {
                          <div class="px-4 pb-4 pt-2 border-t border-slate-100 dark:border-slate-800/40 space-y-3 animate-fade-in text-xs text-slate-700 dark:text-slate-300">
                            <div>
                              <span class="block text-[9px] font-bold text-slate-400 uppercase">Analyses demandées</span>
                              <div class="flex flex-wrap gap-1.5 mt-1">
                                @for (exam of order.exams; track exam) {
                                  <span class="px-2 py-1 rounded bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800/40 font-semibold text-slate-700 dark:text-slate-300">{{ exam }}</span>
                                }
                              </div>
                            </div>
                            
                            @if (order.reason) {
                              <div>
                                <span class="block text-[9px] font-bold text-slate-400 uppercase">Indication clinique</span>
                                <p class="text-xs mt-0.5 font-semibold text-slate-800 dark:text-slate-200">{{ order.reason }}</p>
                              </div>
                            }
                            
                            <div>
                              <span class="block text-[9px] font-bold text-slate-400 uppercase">Priorité</span>
                              <span class="inline-flex items-center gap-1.5 text-xs mt-0.5 font-bold" [class]="order.priority === 'URGENTE' ? 'text-red-500' : 'text-slate-600 dark:text-slate-400'">
                                ⚠️ {{ order.priority }}
                              </span>
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
        }

        <!-- Onglet Hospitalisation -->
        @if (activeTab() === 'hospitalization') {
          <div class="space-y-6 animate-fade-in">
            <app-patient-hospitalization [patientId]="patient().id"></app-patient-hospitalization>
          </div>
        }

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
  expandedConsultations = signal<Record<string, boolean>>({});
  criticalAllergies = signal<PatientAllergy[]>([]);
  criticalAllergiesSubstances = computed(() => 
    this.criticalAllergies().map(a => a.substance).join(', ')
  );

  toggleConsultation(id: string): void {
    this.expandedConsultations.update(prev => ({ ...prev, [id]: !prev[id] }));
  }

  // STORY-0702 — Properties for Audit
  showAudit = signal(false);
  isLoadingAudit = signal(false);
  auditLogs = signal<AuditLog[]>([]);
  auditLoaded = false;

	// UX/UI Improvement — Active Tab state
	activeTab = signal<'general' | 'medical' | 'audit' | 'lab' | 'hospitalization'>('general');

	activeVisit = signal<Visit | null>(null);

	// STORY-0903 — Lab properties
	private readonly patientApi = inject(PatientApiService);
	private readonly consultationApi = inject(ConsultationApiService);
	private readonly auditApi = inject(AuditApiService);

	labOrders = signal<LabOrder[]>([]);
	labResults = signal<LabResult[]>([]);
	isLoadingLab = signal<boolean>(false);
	labLoaded = false;
	selectedAnalyte = signal<string>('');
	expandedLabOrders = signal<Record<string, boolean>>({});

	toggleLabOrder(id: string): void {
		this.expandedLabOrders.update(prev => ({ ...prev, [id]: !prev[id] }));
	}

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
    return (role === 'MEDECIN' || role === 'ADMIN_CLINIQUE') && this.activeVisit() !== null;
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
    const role = this.session()?.role;
    const canSeeActiveVisit = ['MEDECIN', 'ADMIN_CLINIQUE', 'AGENT_ACCUEIL', 'INFIRMIER'].includes(role || '');
    if (canSeeActiveVisit) {
      this.visitApi.getActiveVisits().subscribe({
        next: (visits) => {
          const found = visits.find(v => v.patientId === this.patient().id) ?? null;
          this.activeVisit.set(found);
        },
        error: () => { /* silencieux */ }
      });
    }

    this.patientApi.getAllergies(this.patient().id).subscribe({
      next: (allergies) => {
        const critical = allergies.filter(a => a.status === 'ACTIVE' && (a.severity === 'CRITICAL' || a.severity === 'HIGH'));
        this.criticalAllergies.set(critical);
      },
      error: () => { /* silencieux */ }
    });
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

	analyteNames = computed(() => {
		const results = this.labResults();
		return Array.from(new Set(results.map(r => r.analyteName)));
	});

	filteredResults = computed(() => {
		const results = this.labResults();
		const analyte = this.selectedAnalyte();
		if (!analyte) return [];
		return results
				.filter(r => r.analyteName === analyte)
				.sort((a, b) => {
					const dateA = new Date(a.validatedAt || a.createdAt).getTime();
					const dateB = new Date(b.validatedAt || b.createdAt).getTime();
					return dateA - dateB;
				});
	});

	chartPoints = computed(() => {
		const results = this.filteredResults();
		if (results.length === 0) return [];

		const values = results.map(r => {
			const v = parseFloat(r.value);
			return isNaN(v) ? 0 : v;
		});
		const minVal = Math.min(...values) * 0.9;
		const maxVal = Math.max(...values) * 1.1;
		const valRange = maxVal - minVal === 0 ? 1 : maxVal - minVal;

		const width = 500;
		const height = 200;
		const padding = 30;

		return results.map((r, i) => {
			const v = parseFloat(r.value);
			const val = isNaN(v) ? 0 : v;
			const x = padding + (i * (width - 2 * padding)) / (results.length === 1 ? 1 : results.length - 1);
			const y = height - padding - ((val - minVal) / valRange) * (height - 2 * padding);

			return {
				x,
				y,
				val,
				date: r.validatedAt ? r.validatedAt.slice(0, 10) : r.createdAt.slice(0, 10),
				interpretation: r.interpretation
			};
		});
	});

	getPolylinePoints(): string {
		return this.chartPoints().map(p => `${p.x},${p.y}`).join(' ');
	}

	getInterpretationColor(interpretation: string): string {
		switch (interpretation) {
			case 'ELEVE':
				return '#f97316';
			case 'CRITIQUE':
				return '#ef4444';
			case 'BAS':
				return '#3b82f6';
			default:
				return '#10b981';
		}
	}

	loadLab(): void {
		this.isLoadingLab.set(true);
		this.patientApi.getPatientLabOrders(this.patient().id).subscribe({
			next: (orders) => {
				this.labOrders.set(orders);
			},
			error: () => {}
		});

		this.patientApi.getPatientLabResults(this.patient().id).subscribe({
			next: (results) => {
				this.labResults.set(results);
				this.isLoadingLab.set(false);
				this.labLoaded = true;

				const analytes = Array.from(new Set(results.map(r => r.analyteName)));
				if (analytes.length > 0) {
					this.selectedAnalyte.set(analytes[0]);
				}
			},
			error: () => {
				this.isLoadingLab.set(false);
				this.labLoaded = true;
			}
		});
	}

	setActiveTab(tab: 'general' | 'medical' | 'audit' | 'lab' | 'hospitalization'): void {
		this.activeTab.set(tab);
		if (tab === 'medical' && !this.historyLoaded) {
			this.loadHistory();
		}
		if (tab === 'audit' && !this.auditLoaded) {
			this.loadAudit();
		}
		if (tab === 'lab' && !this.labLoaded) {
			this.loadLab();
		}
	}
}

