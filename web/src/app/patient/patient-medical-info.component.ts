import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PatientApiService } from './patient-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { PatientAllergy, PatientMedicalHistory, PatientVaccination } from './patient.models';

@Component({
  selector: 'app-patient-medical-info',
  standalone: true,
  imports: [DatePipe, FormsModule],
  template: `
    <div class="space-y-6">
      <!-- Section Allergies -->
      <div class="bg-[var(--app-surface)] border border-[var(--app-border)]/60 rounded-xl p-4 md:p-5 shadow-xs transition-colors">
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
          <h4 class="text-sm font-bold text-[var(--text-primary)] uppercase tracking-wider flex items-center gap-1.5">
            <span>🛡️</span> {{ t('patients.medicalInfo.allergies') }}
          </h4>
          <button
            (click)="openAllergyModal()"
            class="px-3 py-1 bg-rose-50 hover:bg-rose-100 dark:bg-rose-950/20 dark:hover:bg-rose-900/30 text-rose-600 dark:text-rose-400 text-xs font-bold rounded-[var(--radius-brand-sm)] border border-rose-100 dark:border-rose-900/20 transition-all cursor-pointer flex items-center gap-1 w-fit"
          >
            <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            {{ t('patients.medicalInfo.allergies.add') }}
          </button>
        </div>

        @if (loadingAllergies()) {
          <div class="py-4 text-center text-xs text-[var(--text-muted)]">{{ t('common.loading') }}</div>
        } @else if (allergies().length === 0) {
          <p class="text-xs text-[var(--text-muted)] italic bg-[var(--app-surface-muted)] dark:bg-[var(--app-bg)]/10 p-3 rounded-lg border border-slate-100/50 dark:border-slate-800/40">
            {{ t('patients.medicalInfo.allergies.empty') }}
          </p>
        } @else {
          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            @for (allergy of activeAllergies(); track allergy.id) {
              <div 
                [class]="allergy.severity === 'CRITICAL' || allergy.severity === 'HIGH'
                  ? 'p-3 bg-[var(--brand-danger-subtle)]/40 dark:bg-red-950/10 border border-[var(--brand-danger-border)] dark:border-red-900/20 rounded-lg flex items-start justify-between'
                  : 'p-3 bg-amber-50/30 dark:bg-amber-950/5 border border-amber-100/40 dark:border-amber-900/10 rounded-lg flex items-start justify-between'"
              >
                <div>
                  <div class="flex items-center gap-2">
                    <span class="font-extrabold text-sm text-[var(--text-primary)]">{{ allergy.substance }}</span>
                    <span [class]="getSeverityClass(allergy.severity)">
                      {{ t('patients.medicalInfo.allergies.severity.' + allergy.severity) }}
                    </span>
                  </div>
                  @if (allergy.reaction) {
                    <p class="text-xs text-[var(--text-muted)] mt-1"><strong>{{ t('patients.medicalInfo.allergies.reaction') }} :</strong> {{ allergy.reaction }}</p>
                  }
                  @if (allergy.comment) {
                    <p class="text-xs text-[var(--text-muted)] mt-1 italic">{{ allergy.comment }}</p>
                  }
                </div>
                <button
                  (click)="deleteAllergy(allergy)"
                  class="p-1 rounded-sm text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/20 hover:text-rose-600 transition-colors cursor-pointer"
                  title="Supprimer"
                >
                  <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            }
          </div>
        }
      </div>

      <!-- Section Antécédents -->
      <div class="bg-[var(--app-surface)] border border-[var(--app-border)]/60 rounded-xl p-4 md:p-5 shadow-xs transition-colors">
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
          <h4 class="text-sm font-bold text-[var(--text-primary)] uppercase tracking-wider flex items-center gap-1.5">
            <span>📋</span> {{ t('patients.medicalInfo.history') }}
          </h4>
          <button
            (click)="openHistoryModal()"
            class="px-3 py-1 bg-blue-50 hover:bg-blue-100 dark:bg-blue-950/20 dark:hover:bg-blue-900/30 text-blue-600 dark:text-blue-400 text-xs font-bold rounded-[var(--radius-brand-sm)] border border-blue-100 dark:border-blue-900/20 transition-all cursor-pointer flex items-center gap-1 w-fit"
          >
            <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            {{ t('patients.medicalInfo.history.add') }}
          </button>
        </div>

        @if (loadingHistory()) {
          <div class="py-4 text-center text-xs text-[var(--text-muted)]">{{ t('common.loading') }}</div>
        } @else if (history().length === 0) {
          <p class="text-xs text-[var(--text-muted)] italic bg-[var(--app-surface-muted)] dark:bg-[var(--app-bg)]/10 p-3 rounded-lg border border-slate-100/50 dark:border-slate-800/40">
            {{ t('patients.medicalInfo.history.empty') }}
          </p>
        } @else {
          <div class="space-y-4">
            @for (cat of historyCategories; track cat) {
              @if (getHistoryByCategory(cat).length > 0) {
                <div>
                  <h5 class="text-[10px] font-black uppercase tracking-wider text-[var(--text-muted)] mb-2">
                    {{ t('patients.medicalInfo.history.category.' + cat) }}
                  </h5>
                  <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                    @for (item of getHistoryByCategory(cat); track item.id) {
                      <div class="p-3 bg-[var(--app-surface-muted)] dark:bg-[var(--app-surface-muted)] border border-slate-100/50 dark:border-slate-800/40 rounded-lg flex items-start justify-between">
                        <div>
                          <div class="flex items-center gap-2">
                            <span class="font-extrabold text-sm text-[var(--text-primary)]">{{ item.description }}</span>
                            @if (item.isOngoing) {
                              <span class="px-1.5 py-0.5 rounded-sm text-[9px] font-bold bg-teal-50 text-teal-600 dark:bg-teal-950/30 dark:text-teal-400 border border-teal-100 dark:border-teal-900/20">
                                {{ t('patients.medicalInfo.history.ongoing') }}
                              </span>
                            }
                            @if (item.important) {
                              <span class="px-1.5 py-0.5 rounded-sm text-[9px] font-bold bg-rose-50 text-rose-600 dark:bg-rose-950/30 dark:text-rose-400 border border-rose-100 dark:border-rose-900/20 flex items-center gap-1 animate-pulse">
                                ⚠️ {{ t('patients.medicalInfo.history.important') }}
                              </span>
                            }
                          </div>
                          @if (item.onsetDate) {
                            <p class="text-xs text-[var(--text-muted)] mt-1">{{ t('patients.medicalInfo.history.onsetDate') }} : {{ item.onsetDate | date:'dd/MM/yyyy' }}</p>
                          }
                          @if (item.comment) {
                            <p class="text-xs text-[var(--text-muted)] mt-1 italic">{{ item.comment }}</p>
                          }
                        </div>
                        <div class="flex items-center gap-1">
                          <button
                            (click)="toggleOngoingHistory(item)"
                            class="p-1 rounded-sm text-[var(--text-muted)] hover:bg-[var(--app-surface-muted)] hover:text-[var(--text-secondary)] transition-colors cursor-pointer"
                            title="Modifier statut"
                          >
                            <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                            </svg>
                          </button>
                          <button
                            (click)="deleteHistory(item)"
                            class="p-1 rounded-sm text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/20 hover:text-rose-600 transition-colors cursor-pointer"
                            title="Supprimer"
                          >
                            <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                            </svg>
                          </button>
                        </div>
                      </div>
                    }
                  </div>
                </div>
              }
            }
          </div>
        }
      </div>

      <!-- Modale Ajout Allergie -->
      @if (showAllergyModal()) {
        <div class="fixed inset-0 bg-slate-950/40 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div class="bg-[var(--app-surface)] border border-[var(--app-border)]/80 rounded-xl w-full max-w-[420px] shadow-lg overflow-hidden">
            <header class="px-5 py-4 border-b border-[var(--app-border)] flex items-center justify-between">
              <h3 class="font-display font-bold text-[var(--text-primary)]">{{ t('patients.medicalInfo.allergies.add') }}</h3>
              <button (click)="showAllergyModal.set(false)" class="p-1 text-[var(--text-muted)] hover:bg-[var(--app-surface-muted)] rounded-lg cursor-pointer">
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </header>
            <form (submit)="saveAllergy($event)" class="p-5 space-y-4">
              <div class="space-y-1">
                <label class="block text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ t('patients.medicalInfo.allergies.substance') }}*</label>
                <input type="text" [(ngModel)]="allergySubstance" name="substance" required class="ui-input" placeholder="{{ t('patients.medicalInfo.allergies.substancePlaceholder') }}" />
              </div>
              <div class="space-y-1">
                <label class="block text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ t('patients.medicalInfo.allergies.severity') }}*</label>
                <select [(ngModel)]="allergySeverity" name="severity" class="ui-select">
                  <option value="LOW">{{ t('patients.medicalInfo.allergies.severity.LOW') }}</option>
                  <option value="MEDIUM">{{ t('patients.medicalInfo.allergies.severity.MEDIUM') }}</option>
                  <option value="HIGH">{{ t('patients.medicalInfo.allergies.severity.HIGH') }}</option>
                  <option value="CRITICAL">{{ t('patients.medicalInfo.allergies.severity.CRITICAL') }}</option>
                </select>
              </div>
              <div class="space-y-1">
                <label class="block text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ t('patients.medicalInfo.allergies.reaction') }}</label>
                <input type="text" [(ngModel)]="allergyReaction" name="reaction" class="ui-input" placeholder="{{ t('patients.medicalInfo.allergies.reactionPlaceholder') }}" />
              </div>
              <div class="space-y-1">
                <label class="block text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ t('patients.medicalInfo.allergies.comment') }}</label>
                <textarea [(ngModel)]="allergyComment" name="comment" rows="2" class="ui-textarea"></textarea>
              </div>
              <footer class="pt-4 border-t border-[var(--app-border)]/80 flex justify-end gap-2">
                <button type="button" (click)="showAllergyModal.set(false)" class="px-4 py-2 border border-[var(--app-border)] rounded-[var(--radius-brand-sm)] text-xs font-semibold text-[var(--text-secondary)] hover:bg-[var(--app-surface-muted)] dark:hover:bg-slate-800/40 cursor-pointer">{{ t('common.cancel') }}</button>
                <button type="submit" class="px-5 py-2 rounded-[var(--radius-brand-sm)] text-xs font-semibold text-white bg-brand-cyan hover:bg-[var(--brand-primary-hover)] cursor-pointer">{{ t('common.save') }}</button>
              </footer>
            </form>
          </div>
        </div>
      }

      <!-- Modale Ajout Antécédent -->
      @if (showHistoryModal()) {
        <div class="fixed inset-0 bg-slate-950/40 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div class="bg-[var(--app-surface)] border border-[var(--app-border)]/80 rounded-xl w-full max-w-[420px] shadow-lg overflow-hidden">
            <header class="px-5 py-4 border-b border-[var(--app-border)] flex items-center justify-between">
              <h3 class="font-display font-bold text-[var(--text-primary)]">{{ t('patients.medicalInfo.history.add') }}</h3>
              <button (click)="showHistoryModal.set(false)" class="p-1 text-[var(--text-muted)] hover:bg-[var(--app-surface-muted)] rounded-lg cursor-pointer">
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </header>
            <form (submit)="saveHistory($event)" class="p-5 space-y-4">
               <div class="space-y-1">
                <label class="block text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ t('patients.medicalInfo.history.category') }}*</label>
                <select [(ngModel)]="historyCategory" name="category" class="ui-select">
                  <option value="MEDICAL">{{ t('patients.medicalInfo.history.category.MEDICAL') }}</option>
                  <option value="SURGICAL">{{ t('patients.medicalInfo.history.category.SURGICAL') }}</option>
                  <option value="FAMILY">{{ t('patients.medicalInfo.history.category.FAMILY') }}</option>
                  <option value="OBSTETRICAL">{{ t('patients.medicalInfo.history.category.OBSTETRICAL') }}</option>
                  <option value="ALLERGIC">{{ t('patients.medicalInfo.history.category.ALLERGIC') }}</option>
                  <option value="SOCIAL">{{ t('patients.medicalInfo.history.category.SOCIAL') }}</option>
                  <option value="OTHER">{{ t('patients.medicalInfo.history.category.OTHER') }}</option>
                </select>
              </div>
              <div class="space-y-1">
                <label class="block text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ t('patients.medicalInfo.history.description') }}*</label>
                <input type="text" [(ngModel)]="historyDescription" name="description" required class="ui-input" placeholder="{{ t('patients.medicalInfo.history.descriptionPlaceholder') }}" />
              </div>
              <div class="grid grid-cols-3 gap-4">
                <div class="space-y-1 col-span-1">
                  <label class="block text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ t('patients.medicalInfo.history.onsetDate') }}</label>
                  <input type="date" [(ngModel)]="historyOnsetDate" name="onsetDate" class="ui-input" />
                </div>
                <div class="flex items-center pt-5 justify-center col-span-1">
                  <label class="flex items-center gap-2 text-xs font-semibold text-[var(--text-secondary)] cursor-pointer select-none">
                    <input type="checkbox" [(ngModel)]="historyIsOngoing" name="isOngoing" class="ui-checkbox" />
                    {{ t('patients.medicalInfo.history.isOngoing') }}
                  </label>
                </div>
                <div class="flex items-center pt-5 justify-end col-span-1">
                  <label class="flex items-center gap-2 text-xs font-semibold text-[var(--text-secondary)] cursor-pointer select-none">
                    <input type="checkbox" [(ngModel)]="historyIsImportant" name="important" class="ui-checkbox" />
                    {{ t('patients.medicalInfo.history.importantLabel') }}
                  </label>
                </div>
              </div>
              <div class="space-y-1">
                <label class="block text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ t('patients.medicalInfo.history.comment') }}</label>
                <textarea [(ngModel)]="historyComment" name="comment" rows="2" class="ui-textarea"></textarea>
              </div>
            </form>
          </div>
        </div>
      }

      <!-- Section Vaccinations -->
      <div class="bg-[var(--app-surface)] border border-[var(--app-border)]/60 rounded-xl p-4 md:p-5 shadow-xs transition-colors">
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
          <h4 class="text-sm font-bold text-[var(--text-primary)] uppercase tracking-wider flex items-center gap-1.5">
            <span>💉</span> {{ t('patients.medicalInfo.vaccinations.title') }}
          </h4>
          <button
            (click)="openVaccinationModal()"
            class="px-3 py-1 bg-teal-50 hover:bg-teal-100 dark:bg-teal-950/20 dark:hover:bg-teal-900/30 text-teal-600 dark:text-teal-400 text-xs font-bold rounded-[var(--radius-brand-sm)] border border-teal-100 dark:border-teal-900/20 transition-all cursor-pointer flex items-center gap-1 w-fit"
          >
            <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            {{ t('patients.medicalInfo.vaccinations.add') }}
          </button>
        </div>

        @if (loadingVaccinations()) {
          <div class="py-4 text-center text-xs text-[var(--text-muted)]">{{ t('common.loading') }}</div>
        } @else if (vaccinations().length === 0) {
          <p class="text-xs text-[var(--text-muted)] italic bg-[var(--app-surface-muted)] dark:bg-[var(--app-bg)]/10 p-3 rounded-lg border border-slate-100/50 dark:border-slate-800/40">
            {{ t('patients.medicalInfo.vaccinations.empty') }}
          </p>
        } @else {
          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            @for (vac of vaccinations(); track vac.id) {
              <div class="p-3 bg-[var(--app-surface-muted)] dark:bg-[var(--app-surface-muted)] border border-slate-100/50 dark:border-slate-800/40 rounded-lg animate-fade-in">
                <div class="flex justify-between items-start">
                  <div>
                    <span class="font-extrabold text-sm text-[var(--text-primary)] block">{{ vac.vaccineName }}</span>
                    @if (vac.batchNumber) {
                      <span class="text-[10px] bg-slate-100 text-[var(--text-secondary)] dark:bg-[var(--bg-input)] dark:text-[var(--text-muted)] px-1.5 py-0.5 rounded-sm mt-1 inline-block">{{ t('patients.medicalInfo.vaccinations.batchNumber') }}: {{ vac.batchNumber }}</span>
                    }
                    <div class="text-xs text-[var(--text-muted)] mt-2 space-y-1">
                      <p><strong>{{ t('patients.medicalInfo.vaccinations.administeredAt') }} :</strong> {{ vac.administeredAt | date:'dd/MM/yyyy' }}</p>
                      @if (vac.administeredBy) {
                        <p><strong>{{ t('patients.medicalInfo.vaccinations.administeredBy') }} :</strong> {{ vac.administeredBy }}</p>
                      }
                      @if (vac.nextDoseAt) {
                        <p class="text-brand-cyan dark:text-cyan-400 font-bold"><strong>{{ t('patients.medicalInfo.vaccinations.nextDoseAt') }} :</strong> {{ vac.nextDoseAt | date:'dd/MM/yyyy' }}</p>
                      }
                      @if (vac.notes) {
                        <p class="italic text-[var(--text-muted)] mt-1">"{{ vac.notes }}"</p>
                      }
                    </div>
                  </div>
                </div>
              </div>
            }
          </div>
        }
      </div>

      <!-- Modal Vaccinations -->
      @if (showVaccinationModal()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4">
          <div class="bg-[var(--app-surface)] w-full max-w-md p-6 rounded-xl border border-[var(--app-border)]/60 shadow-xl">
            <header class="mb-4">
              <h4 class="font-display font-black text-base text-[var(--text-primary)] uppercase tracking-wider">
                {{ t('patients.medicalInfo.vaccinations.add') }}
              </h4>
            </header>
            <form (submit)="saveVaccination($event)" class="space-y-4">
              <div class="space-y-1">
                <label class="block text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ t('patients.medicalInfo.vaccinations.vaccineName') }}*</label>
                <input type="text" [(ngModel)]="vaccineName" name="vaccineName" required class="ui-input" placeholder="{{ t('patients.medicalInfo.vaccinations.vaccineNamePlaceholder') }}" />
              </div>
              <div class="space-y-1">
                <label class="block text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ t('patients.medicalInfo.vaccinations.batchNumber') }}</label>
                <input type="text" [(ngModel)]="vaccineBatch" name="batchNumber" class="ui-input" placeholder="{{ t('patients.medicalInfo.vaccinations.batchNumberPlaceholder') }}" />
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div class="space-y-1">
                  <label class="block text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ t('patients.medicalInfo.vaccinations.administeredAt') }}*</label>
                  <input type="date" [(ngModel)]="vaccineDate" name="administeredAt" required class="ui-input" />
                </div>
                <div class="space-y-1">
                  <label class="block text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ t('patients.medicalInfo.vaccinations.nextDoseAt') }}</label>
                  <input type="date" [(ngModel)]="vaccineNextDate" name="nextDoseAt" class="ui-input" />
                </div>
              </div>
              <div class="space-y-1">
                <label class="block text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ t('patients.medicalInfo.vaccinations.administeredBy') }}</label>
                <input type="text" [(ngModel)]="vaccineAdministeredBy" name="administeredBy" class="ui-input" placeholder="{{ t('patients.medicalInfo.vaccinations.administeredByPlaceholder') }}" />
              </div>
              <div class="space-y-1">
                <label class="block text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider">{{ t('patients.medicalInfo.vaccinations.notes') }}</label>
                <textarea [(ngModel)]="vaccineNotes" name="notes" rows="2" class="ui-textarea" placeholder="{{ t('patients.medicalInfo.vaccinations.notesPlaceholder') }}"></textarea>
              </div>
              <footer class="pt-4 border-t border-[var(--app-border)]/80 flex justify-end gap-2">
                <button type="button" (click)="showVaccinationModal.set(false)" class="px-4 py-2 border border-[var(--app-border)] rounded-[var(--radius-brand-sm)] text-xs font-semibold text-[var(--text-secondary)] hover:bg-[var(--app-surface-muted)] dark:hover:bg-slate-800/40 cursor-pointer">{{ t('common.cancel') }}</button>
                <button type="submit" class="px-5 py-2 rounded-[var(--radius-brand-sm)] text-xs font-semibold text-white bg-brand-cyan hover:bg-[var(--brand-primary-hover)] cursor-pointer">{{ t('common.save') }}</button>
              </footer>
            </form>
          </div>
        </div>
      }
    </div>
  `
})
export class PatientMedicalInfoComponent implements OnInit {
  @Input({ required: true }) patientId!: string;

  private readonly patientApi = inject(PatientApiService);
  private readonly i18n = inject(I18nService);

  readonly t = (key: string) => this.i18n.t(key);

  readonly allergies = signal<PatientAllergy[]>([]);
  readonly history = signal<PatientMedicalHistory[]>([]);
  readonly vaccinations = signal<PatientVaccination[]>([]);

  readonly loadingAllergies = signal<boolean>(false);
  readonly loadingHistory = signal<boolean>(false);
  readonly loadingVaccinations = signal<boolean>(false);

  // Modales
  readonly showAllergyModal = signal<boolean>(false);
  readonly showHistoryModal = signal<boolean>(false);
  readonly showVaccinationModal = signal<boolean>(false);

  // Formulaire Allergies
  allergySubstance = '';
  allergySeverity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' = 'MEDIUM';
  allergyReaction = '';
  allergyComment = '';

  // Formulaire Antécédents
  historyCategory: 'MEDICAL' | 'SURGICAL' | 'FAMILY' | 'OBSTETRICAL' | 'OTHER' | 'ALLERGIC' | 'SOCIAL' = 'MEDICAL';
  historyDescription = '';
  historyOnsetDate = '';
  historyIsOngoing = true;
  historyIsImportant = false;
  historyComment = '';

  // Formulaire Vaccinations
  vaccineName = '';
  vaccineBatch = '';
  vaccineDate = '';
  vaccineNextDate = '';
  vaccineAdministeredBy = '';
  vaccineNotes = '';

  readonly historyCategories: Array<'MEDICAL' | 'SURGICAL' | 'FAMILY' | 'OBSTETRICAL' | 'ALLERGIC' | 'SOCIAL' | 'OTHER'> = [
    'MEDICAL', 'SURGICAL', 'FAMILY', 'OBSTETRICAL', 'ALLERGIC', 'SOCIAL', 'OTHER'
  ];

  ngOnInit(): void {
    if (this.patientId) {
      this.loadAllergies();
      this.loadHistory();
      this.loadVaccinations();
    }
  }

  loadAllergies(): void {
    this.loadingAllergies.set(true);
    this.patientApi.getAllergies(this.patientId).subscribe({
      next: (data) => {
        this.allergies.set(data);
        this.loadingAllergies.set(false);
      },
      error: () => this.loadingAllergies.set(false)
    });
  }

  loadHistory(): void {
    this.loadingHistory.set(true);
    this.patientApi.getMedicalHistory(this.patientId).subscribe({
      next: (data) => {
        this.history.set(data);
        this.loadingHistory.set(false);
      },
      error: () => this.loadingHistory.set(false)
    });
  }

  loadVaccinations(): void {
    this.loadingVaccinations.set(true);
    this.patientApi.getVaccinations(this.patientId).subscribe({
      next: (data) => {
        this.vaccinations.set(data);
        this.loadingVaccinations.set(false);
      },
      error: () => this.loadingVaccinations.set(false)
    });
  }

  activeAllergies(): PatientAllergy[] {
    return this.allergies().filter(a => a.status === 'ACTIVE');
  }

  getHistoryByCategory(category: string): PatientMedicalHistory[] {
    return this.history().filter(h => h.category === category);
  }

  getSeverityClass(severity: string): string {
    switch (severity) {
      case 'CRITICAL':
        return 'px-1.5 py-0.5 rounded-sm text-[10px] font-black bg-red-100 text-red-700 dark:bg-red-950/40 dark:text-red-400 border border-red-200/50 dark:border-red-900/20';
      case 'HIGH':
        return 'px-1.5 py-0.5 rounded-sm text-[10px] font-bold bg-orange-100 text-orange-700 dark:bg-orange-950/40 dark:text-orange-400 border border-orange-200/50 dark:border-orange-900/20';
      case 'MEDIUM':
        return 'px-1.5 py-0.5 rounded-sm text-[10px] font-semibold bg-amber-100 text-amber-700 dark:bg-amber-950/30 dark:text-amber-400';
      default:
        return 'px-1.5 py-0.5 rounded-sm text-[10px] font-semibold bg-slate-100 text-[var(--text-secondary)] dark:bg-[var(--bg-input)] dark:text-[var(--text-muted)]';
    }
  }

  openAllergyModal(): void {
    this.allergySubstance = '';
    this.allergySeverity = 'MEDIUM';
    this.allergyReaction = '';
    this.allergyComment = '';
    this.showAllergyModal.set(true);
  }

  openHistoryModal(): void {
    this.historyCategory = 'MEDICAL';
    this.historyDescription = '';
    this.historyOnsetDate = '';
    this.historyIsOngoing = true;
    this.historyIsImportant = false;
    this.historyComment = '';
    this.showHistoryModal.set(true);
  }

  saveAllergy(event: Event): void {
    event.preventDefault();
    if (!this.allergySubstance.trim()) return;

    this.patientApi.addAllergy(this.patientId, {
      substance: this.allergySubstance.trim(),
      severity: this.allergySeverity,
      reaction: this.allergyReaction.trim() || undefined,
      comment: this.allergyComment.trim() || undefined,
      status: 'ACTIVE'
    }).subscribe({
      next: () => {
        this.showAllergyModal.set(false);
        this.loadAllergies();
      }
    });
  }

  deleteAllergy(allergy: PatientAllergy): void {
    if (!allergy.id) return;
    if (confirm(this.i18n.t('patients.medicalInfo.allergies.deleteConfirm'))) {
      this.patientApi.deleteAllergy(this.patientId, allergy.id).subscribe({
        next: () => this.loadAllergies()
      });
    }
  }

  saveHistory(event: Event): void {
    event.preventDefault();
    if (!this.historyDescription.trim()) return;

    this.patientApi.addMedicalHistory(this.patientId, {
      category: this.historyCategory,
      description: this.historyDescription.trim(),
      onsetDate: this.historyOnsetDate || undefined,
      isOngoing: this.historyIsOngoing,
      comment: this.historyComment.trim() || undefined,
      important: this.historyIsImportant
    }).subscribe({
      next: () => {
        this.showHistoryModal.set(false);
        this.loadHistory();
      }
    });
  }

  toggleOngoingHistory(item: PatientMedicalHistory): void {
    if (!item.id) return;
    this.patientApi.updateMedicalHistory(this.patientId, item.id, {
      category: item.category,
      description: item.description,
      onsetDate: item.onsetDate,
      isOngoing: !item.isOngoing,
      comment: item.comment,
      important: item.important
    }).subscribe({
      next: () => this.loadHistory()
    });
  }

  deleteHistory(item: PatientMedicalHistory): void {
    if (!item.id) return;
    if (confirm(this.i18n.t('patients.medicalInfo.history.deleteConfirm'))) {
      this.patientApi.deleteMedicalHistory(this.patientId, item.id).subscribe({
        next: () => this.loadHistory()
      });
    }
  }

  openVaccinationModal(): void {
    this.vaccineName = '';
    this.vaccineBatch = '';
    this.vaccineDate = new Date().toISOString().split('T')[0];
    this.vaccineNextDate = '';
    this.vaccineAdministeredBy = '';
    this.vaccineNotes = '';
    this.showVaccinationModal.set(true);
  }

  saveVaccination(event: Event): void {
    event.preventDefault();
    if (!this.vaccineName.trim() || !this.vaccineDate) return;

    this.patientApi.addVaccination(this.patientId, {
      vaccineName: this.vaccineName.trim(),
      batchNumber: this.vaccineBatch.trim() || undefined,
      administeredAt: this.vaccineDate,
      administeredBy: this.vaccineAdministeredBy.trim() || undefined,
      notes: this.vaccineNotes.trim() || undefined,
      nextDoseAt: this.vaccineNextDate || undefined
    }).subscribe({
      next: () => {
        this.showVaccinationModal.set(false);
        this.loadVaccinations();
      }
    });
  }
}
