import { Component, Input, OnInit, inject, signal, computed } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PatientApiService } from './patient-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { AuthTokenStorageService } from '../auth/auth-token-storage.service';
import { Hospitalization, HospitalizationNote } from './patient.models';
import { StaffApiService } from '../clinic/staff/staff-api.service';

@Component({
  selector: 'app-patient-hospitalization',
  standalone: true,
  imports: [DatePipe, FormsModule],
  template: `
    <div class="space-y-6">
      @if (activeHospitalization()) {
        <!-- Vue Hospitalisation Active -->
        <div class="bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800/60 rounded-xl p-5 shadow-xs transition-colors space-y-6">
          <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 pb-4 border-b border-slate-100 dark:border-slate-800/80">
            <div>
              <span class="inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-extrabold bg-teal-50 text-teal-700 dark:bg-teal-950/30 dark:text-teal-300 uppercase tracking-wider mb-2">
                {{ t('patients.hospitalization.status.EN_COURS') }}
              </span>
              <h4 class="text-base font-extrabold text-slate-800 dark:text-white">
                {{ activeHospitalization()?.serviceName }} — {{ t('patients.hospitalization.room') }} {{ activeHospitalization()?.roomNumber }} | {{ t('patients.hospitalization.bed') }} {{ activeHospitalization()?.bedNumber }}
              </h4>
              <p class="text-xs text-slate-400 dark:text-slate-500 mt-1">
                <strong>N° Séjour :</strong> <span class="font-mono font-bold text-slate-700 dark:text-slate-300">{{ activeHospitalization()?.hospitalizationNumber }}</span>
              </p>
              <p class="text-xs text-slate-400 dark:text-slate-500 mt-0.5">
                <strong>Médecin responsable :</strong> <span class="text-slate-700 dark:text-slate-300">{{ staffMap().get(activeHospitalization()?.responsiblePractitionerId || '') || 'Non spécifié' }}</span>
              </p>
              <p class="text-xs text-slate-400 dark:text-slate-500 mt-0.5">
                {{ t('patients.hospitalization.admittedAt') }} : {{ activeHospitalization()?.admittedAt | date:'dd/MM/yyyy HH:mm' }}
              </p>
            </div>

            @if (canModify()) {
              <button
                (click)="openDischargeModal()"
                class="px-4 py-2 bg-rose-50 hover:bg-rose-100 dark:bg-rose-950/20 dark:hover:bg-rose-900/30 text-rose-600 dark:text-rose-400 text-xs font-extrabold rounded-[var(--radius-brand-sm)] border border-rose-100 dark:border-rose-900/20 transition-all cursor-pointer flex items-center gap-1.5"
              >
                🚪 {{ t('patients.hospitalization.discharge') }}
              </button>
            }
          </div>

          <!-- Motif d'admission -->
          <div class="p-3 bg-slate-50/50 dark:bg-slate-950/10 border border-slate-100/50 dark:border-slate-800/40 rounded-lg text-xs leading-relaxed text-slate-700 dark:text-slate-300">
            <strong>{{ t('patients.hospitalization.reason') }} :</strong> {{ activeHospitalization()?.admissionReason }}
          </div>

          <!-- Section Notes d'évolution -->
          <div class="space-y-4 pt-2">
            <h5 class="text-xs font-black uppercase tracking-wider text-slate-400 dark:text-slate-500">
              💬 {{ t('patients.hospitalization.notes') }}
            </h5>

            @if (canModify()) {
              <form (submit)="saveNote($event)" class="flex gap-2">
                <input
                  type="text"
                  [(ngModel)]="newNoteContent"
                  name="note"
                  required
                  class="ui-input flex-1 text-xs"
                  [placeholder]="t('patients.hospitalization.notes.add') + '...'"
                />
                <button
                  type="submit"
                  class="px-4 py-2 rounded-[var(--radius-brand-sm)] text-xs font-semibold text-white bg-brand-cyan hover:bg-[#097b98] cursor-pointer"
                >
                  {{ t('common.save') }}
                </button>
              </form>
            }

            @if (loadingNotes()) {
              <div class="text-center text-xs text-slate-400">{{ t('common.loading') }}</div>
            } @else if (notes().length === 0) {
              <p class="text-xs text-slate-500 italic">{{ t('patients.hospitalization.notes.empty') }}</p>
            } @else {
              <div class="relative border-l border-slate-100 dark:border-slate-800 pl-4 space-y-4 mt-2">
                @for (note of notes(); track note.id) {
                  <div class="relative">
                    <span class="absolute -left-[21px] top-1.5 w-2 h-2 rounded-full bg-brand-cyan border-2 border-white dark:border-slate-900"></span>
                    <div class="text-xs">
                      <span class="font-bold text-slate-700 dark:text-slate-300">{{ note.authorName }}</span>
                      <span class="text-slate-400 ml-2">{{ note.createdAt | date:'dd/MM/yyyy HH:mm' }}</span>
                      <p class="text-slate-600 dark:text-slate-400 mt-1 leading-relaxed">{{ note.noteContent }}</p>
                    </div>
                  </div>
                }
              </div>
            }
          </div>
        </div>
      } @else {
        <!-- Aucun séjour actif -->
        <div class="bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800/60 rounded-xl p-8 shadow-xs text-center transition-colors">
          <p class="text-slate-500 dark:text-slate-400 italic mb-4">{{ t('patients.hospitalization.empty') }}</p>
          @if (canModify()) {
            <button
              (click)="openAdmitModal()"
              class="px-4 py-2 bg-brand-cyan hover:bg-[#097b98] text-white text-xs font-extrabold rounded-[var(--radius-brand-sm)] transition-all cursor-pointer inline-flex items-center gap-1.5"
            >
              🏥 {{ t('patients.hospitalization.admit') }}
            </button>
          }
        </div>
      }

      <!-- Historique des anciens séjours -->
      @if (pastHospitalizations().length > 0) {
        <div class="bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800/60 rounded-xl p-5 shadow-xs transition-colors">
          <h4 class="text-xs font-black uppercase tracking-wider text-slate-400 dark:text-slate-500 mb-4">
            📜 Historique des Hospitalisations
          </h4>
          <div class="divide-y divide-slate-100 dark:divide-slate-800/80">
            @for (hosp of pastHospitalizations(); track hosp.id) {
              <div class="py-3 first:pt-0 last:pb-0 flex justify-between items-start">
                <div class="space-y-1">
                  <h5 class="text-xs font-bold text-slate-800 dark:text-slate-200">
                    {{ hosp.serviceName }} — {{ t('patients.hospitalization.room') }} {{ hosp.roomNumber }} | {{ hosp.bedNumber }}
                  </h5>
                  <p class="text-[10px] text-slate-400 dark:text-slate-500 font-mono">
                    N° Séjour : {{ hosp.hospitalizationNumber }}
                  </p>
                  <p class="text-[11px] text-slate-500 dark:text-slate-400">
                    Médecin responsable : {{ staffMap().get(hosp.responsiblePractitionerId || '') || 'Non spécifié' }}
                  </p>
                  <p class="text-[11px] text-slate-400 dark:text-slate-500">
                    {{ hosp.admittedAt | date:'dd/MM/yyyy' }} @if (hosp.dischargedAt) { au {{ hosp.dischargedAt | date:'dd/MM/yyyy' }} }
                  </p>
                  @if (hosp.dischargeDiagnosis) {
                    <p class="text-xs text-slate-600 dark:text-slate-400">
                      <strong>Diag :</strong> {{ hosp.dischargeDiagnosis }}
                    </p>
                  }
                </div>

                @if (hosp.pdfFilePath) {
                  <button
                    (click)="downloadDischargePdf(hosp)"
                    class="px-2.5 py-1 text-[11px] bg-slate-50 hover:bg-slate-100 dark:bg-slate-800 dark:hover:bg-slate-700/80 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 font-semibold rounded-sm transition-colors flex items-center gap-1 cursor-pointer"
                  >
                    📄 {{ t('patients.hospitalization.downloadPdf') }}
                  </button>
                }
              </div>
            }
          </div>
        </div>
      }

      <!-- Modale Admission -->
      @if (showAdmitModal()) {
        <div class="fixed inset-0 bg-slate-950/40 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div class="bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800/80 rounded-xl w-full max-w-[420px] shadow-lg overflow-hidden">
            <header class="px-5 py-4 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
              <h3 class="font-display font-bold text-brand-night dark:text-white">{{ t('patients.hospitalization.admit') }}</h3>
              <button (click)="showAdmitModal.set(false)" class="p-1 text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg cursor-pointer">
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </header>
            <form (submit)="saveAdmission($event)" class="p-5 space-y-4">
              @if (admitError()) {
                <div class="p-2.5 bg-red-50 dark:bg-red-950/20 border border-red-100 dark:border-red-900/30 rounded-lg text-xs text-red-700 dark:text-red-300 font-semibold leading-relaxed">
                  {{ admitError() }}
                </div>
              }
              <div class="space-y-1">
                <label class="block text-[10px] font-bold text-slate-400 uppercase tracking-wider">{{ t('patients.hospitalization.visit') }}*</label>
                <select [(ngModel)]="visitId" name="visit" required class="ui-select">
                  <option value="">-- {{ t('patients.hospitalization.selectVisit') }} --</option>
                  @for (v of patientVisits(); track v.id) {
                    <option [value]="v.id">{{ v.visitNumber }} ({{ v.reason }} - {{ v.createdAt | date:'dd/MM/yyyy' }})</option>
                  }
                </select>
              </div>
              <div class="space-y-1">
                <label class="block text-[10px] font-bold text-slate-400 uppercase tracking-wider">{{ t('patients.hospitalization.responsiblePractitioner') }}*</label>
                <select [(ngModel)]="responsiblePractitionerId" name="practitioner" required class="ui-select">
                  <option value="">-- {{ t('patients.hospitalization.selectPractitioner') }} --</option>
                  @for (p of staffList(); track p.id) {
                    @if (hasRole(p.role, ['MEDECIN', 'ADMIN_CLINIQUE'])) {
                      <option [value]="p.id">{{ p.displayName }} ({{ p.role }})</option>
                    }
                  }
                </select>
              </div>
              <div class="space-y-1">
                <label class="block text-[10px] font-bold text-slate-400 uppercase tracking-wider">{{ t('patients.hospitalization.service') }}*</label>
                <select [(ngModel)]="serviceName" name="service" class="ui-select">
                  <option value="MÉDECINE GÉNÉRALE">Médecine Générale</option>
                  <option value="CHIRURGIE">Chirurgie</option>
                  <option value="PÉDIATRIE">Pédiatrie</option>
                  <option value="URGENCES">Urgences</option>
                  <option value="SOINS INTENSIFS">Soins Intensifs</option>
                </select>
              </div>
              <div class="grid grid-cols-2 gap-4">
                <div class="space-y-1">
                  <label class="block text-[10px] font-bold text-slate-400 uppercase tracking-wider">{{ t('patients.hospitalization.room') }}*</label>
                  <input type="text" [(ngModel)]="roomNumber" name="room" required class="ui-input" placeholder="Ex: Ch 101" />
                </div>
                <div class="space-y-1">
                  <label class="block text-[10px] font-bold text-slate-400 uppercase tracking-wider">{{ t('patients.hospitalization.bed') }}*</label>
                  <input type="text" [(ngModel)]="bedNumber" name="bed" required class="ui-input" placeholder="Ex: Lit A" />
                </div>
              </div>
              <div class="space-y-1">
                <label class="block text-[10px] font-bold text-slate-400 uppercase tracking-wider">{{ t('patients.hospitalization.reason') }}*</label>
                <textarea [(ngModel)]="admissionReason" name="reason" required rows="3" class="ui-textarea"></textarea>
              </div>
              <footer class="pt-4 border-t border-slate-100 dark:border-slate-800/80 flex justify-end gap-2">
                <button type="button" (click)="showAdmitModal.set(false)" class="px-4 py-2 border border-slate-200 dark:border-slate-800 rounded-[var(--radius-brand-sm)] text-xs font-semibold text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800/40 cursor-pointer">{{ t('common.cancel') }}</button>
                <button type="submit" class="px-5 py-2 rounded-[var(--radius-brand-sm)] text-xs font-semibold text-white bg-brand-cyan hover:bg-[#097b98] cursor-pointer">{{ t('common.save') }}</button>
              </footer>
            </form>
          </div>
        </div>
      }

      <!-- Modale Décharge / Sortie -->
      @if (showDischargeModal()) {
        <div class="fixed inset-0 bg-slate-950/40 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div class="bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800/80 rounded-xl w-full max-w-[420px] shadow-lg overflow-hidden">
            <header class="px-5 py-4 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
              <h3 class="font-display font-bold text-brand-night dark:text-white">{{ t('patients.hospitalization.discharge') }}</h3>
              <button (click)="showDischargeModal.set(false)" class="p-1 text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg cursor-pointer">
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </header>
            <form (submit)="saveDischarge($event)" class="p-5 space-y-4">
              <div class="space-y-1">
                <label class="block text-[10px] font-bold text-slate-400 uppercase tracking-wider">{{ t('patients.hospitalization.dischargeDiagnosis') }}*</label>
                <input type="text" [(ngModel)]="dischargeDiagnosis" name="diag" required class="ui-input" />
              </div>
              <div class="space-y-1">
                <label class="block text-[10px] font-bold text-slate-400 uppercase tracking-wider">{{ t('patients.hospitalization.dischargeInstructions') }}*</label>
                <textarea [(ngModel)]="dischargeInstructions" name="instr" required rows="3" class="ui-textarea"></textarea>
              </div>
              <footer class="pt-4 border-t border-slate-100 dark:border-slate-800/80 flex justify-end gap-2">
                <button type="button" (click)="showDischargeModal.set(false)" class="px-4 py-2 border border-slate-200 dark:border-slate-800 rounded-[var(--radius-brand-sm)] text-xs font-semibold text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800/40 cursor-pointer">{{ t('common.cancel') }}</button>
                <button type="submit" class="px-5 py-2 rounded-[var(--radius-brand-sm)] text-xs font-semibold text-white bg-brand-cyan hover:bg-[#097b98] cursor-pointer">{{ t('common.save') }}</button>
              </footer>
            </form>
          </div>
        </div>
      }
    </div>
  `
})
export class PatientHospitalizationComponent implements OnInit {
  @Input({ required: true }) patientId!: string;

  private readonly patientApi = inject(PatientApiService);
  private readonly tokenStorage = inject(AuthTokenStorageService);
  private readonly i18n = inject(I18nService);
  private readonly staffApi = inject(StaffApiService);

  readonly t = (key: string) => this.i18n.t(key);

  readonly list = signal<Hospitalization[]>([]);
  readonly notes = signal<HospitalizationNote[]>([]);
  readonly loadingNotes = signal<boolean>(false);
  readonly staffList = signal<any[]>([]);
  readonly patientVisits = signal<any[]>([]);

  // Modales
  readonly showAdmitModal = signal<boolean>(false);
  readonly showDischargeModal = signal<boolean>(false);
  readonly admitError = signal<string | null>(null);

  // Formulaire d'admission
  serviceName = 'MÉDECINE GÉNÉRALE';
  roomNumber = '';
  bedNumber = '';
  admissionReason = '';
  visitId = '';
  responsiblePractitionerId = '';

  // Formulaire de sortie
  dischargeDiagnosis = '';
  dischargeInstructions = '';

  // Formulaire de note
  newNoteContent = '';

  readonly session = this.tokenStorage.session;

  readonly activeHospitalization = computed(() => 
    this.list().find(h => h.status === 'EN_COURS') ?? null
  );

  readonly pastHospitalizations = computed(() => 
    this.list().filter(h => h.status !== 'EN_COURS')
  );

  readonly staffMap = computed(() => {
    const map = new Map<string, string>();
    for (const p of this.staffList()) {
      map.set(p.id, p.displayName);
    }
    return map;
  });

  ngOnInit(): void {
    if (this.patientId) {
      this.loadHospitalizations();
      this.loadStaff();
      this.loadVisits();
    }
  }

  loadStaff(): void {
    this.staffApi.list().subscribe({
      next: (data) => this.staffList.set(data)
    });
  }

  loadVisits(): void {
    this.patientApi.getPatientVisits(this.patientId).subscribe({
      next: (data) => this.patientVisits.set(data)
    });
  }

  loadHospitalizations(): void {
    this.patientApi.getHospitalizations(this.patientId).subscribe({
      next: (data) => {
        this.list.set(data);
        const active = this.activeHospitalization();
        if (active) {
          this.loadNotes(active.id);
        }
      }
    });
  }

  loadNotes(hospId: string): void {
    this.loadingNotes.set(true);
    this.patientApi.getHospitalizationNotes(hospId).subscribe({
      next: (data) => {
        this.notes.set(data);
        this.loadingNotes.set(false);
      },
      error: () => this.loadingNotes.set(false)
    });
  }

  hasRole(roleStr: string | undefined, allowedRoles: string[] | string): boolean {
    if (!roleStr) return false;
    const roles = roleStr.split(',').map((r) => r.trim());
    if (Array.isArray(allowedRoles)) {
      return roles.some((r) => allowedRoles.includes(r));
    }
    return roles.includes(allowedRoles);
  }

  canModify(): boolean {
    const role = this.session()?.role;
    return this.hasRole(role, ['MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE']);
  }

  openAdmitModal(): void {
    this.roomNumber = '';
    this.bedNumber = '';
    this.admissionReason = '';
    this.visitId = '';
    this.responsiblePractitionerId = '';
    this.admitError.set(null);
    this.loadVisits();
    this.showAdmitModal.set(true);
  }

  saveAdmission(event: Event): void {
    event.preventDefault();
    if (!this.roomNumber.trim() || !this.bedNumber.trim() || !this.admissionReason.trim() || !this.visitId || !this.responsiblePractitionerId) return;

    this.admitError.set(null);
    this.patientApi.admitPatient({
      patientId: this.patientId,
      serviceName: this.serviceName,
      roomNumber: this.roomNumber.trim(),
      bedNumber: this.bedNumber.trim(),
      admissionReason: this.admissionReason.trim(),
      visitId: this.visitId,
      responsiblePractitionerId: this.responsiblePractitionerId
    }).subscribe({
      next: () => {
        this.showAdmitModal.set(false);
        this.loadHospitalizations();
      },
      error: (err) => {
        this.admitError.set(err.error?.detail || err.error?.title || this.t('patients.hospitalization.bedOccupied'));
      }
    });
  }

  saveNote(event: Event): void {
    event.preventDefault();
    const active = this.activeHospitalization();
    if (!active || !this.newNoteContent.trim()) return;

    this.patientApi.addHospitalizationNote(active.id, this.newNoteContent.trim()).subscribe({
      next: () => {
        this.newNoteContent = '';
        this.loadNotes(active.id);
      }
    });
  }

  openDischargeModal(): void {
    this.dischargeDiagnosis = '';
    this.dischargeInstructions = '';
    this.showDischargeModal.set(true);
  }

  saveDischarge(event: Event): void {
    event.preventDefault();
    const active = this.activeHospitalization();
    if (!active || !this.dischargeDiagnosis.trim() || !this.dischargeInstructions.trim()) return;

    this.patientApi.dischargePatient(active.id, {
      dischargeDiagnosis: this.dischargeDiagnosis.trim(),
      dischargeInstructions: this.dischargeInstructions.trim()
    }).subscribe({
      next: () => {
        this.showDischargeModal.set(false);
        this.loadHospitalizations();
      }
    });
  }

  downloadDischargePdf(hosp: Hospitalization): void {
    this.patientApi.downloadDischargePdf(hosp.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `fiche-sortie-${hosp.id}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Error downloading pdf', err);
        alert('Erreur lors du téléchargement du PDF');
      }
    });
  }
}
