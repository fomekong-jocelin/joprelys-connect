import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AlertComponent } from '../../shared/ui/alert.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { PatientDetailComponent } from '../patient-detail.component';
import { PatientMedicalInfoComponent } from '../patient-medical-info.component';
import { ProvisionalPatientRegularizationApiService } from '../provisional-patient-regularization-api.service';
import { IdentitySourceType } from '../regularize-provisional-patient.models';
import { PatientEmergencyContextComponent } from './patient-emergency-context.component';

@Component({
  selector: 'app-patient-profile-tab',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    PatientMedicalInfoComponent,
    PatientEmergencyContextComponent,
    AlertComponent,
    ButtonComponent,
  ],
  template: `
    @if (parent.patient(); as p) {
      <div class="space-y-6 animate-fade-in">
        @if (isProvisional()) {
          <section class="ui-card border-l-2 border-l-[var(--brand-warning)] p-4 sm:p-5">
            <div class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <div class="flex flex-wrap items-center gap-2">
                  <span class="rounded-sm bg-[var(--brand-warning-subtle)] px-2 py-1 text-[10px] font-black uppercase tracking-wider text-[var(--brand-warning-text)]">
                    Identité provisoire
                  </span>
                  <strong class="font-mono text-sm text-[var(--text-primary)]">{{ p.temporaryPatientNumber || p.displayName }}</strong>
                </div>
                <h3 class="mt-3 font-display text-base font-black text-[var(--text-primary)]">Patient à identifier</h3>
                <p class="mt-1 max-w-2xl text-sm text-[var(--text-secondary)]">
                  Le dossier clinique et tout l'historique d'urgence sont conservés. Complétez l'identité lorsque le patient reprend conscience ou qu'une preuve fiable est disponible.
                </p>
              </div>
              <app-ui-button variant="primary" class="w-full sm:w-auto" (pressed)="openIdentityModal()">
                Identifier le patient
              </app-ui-button>
            </div>

            <dl class="mt-4 grid grid-cols-1 gap-3 border-t border-[var(--divider-subtle)] pt-4 sm:grid-cols-2 lg:grid-cols-4">
              <div><dt class="ui-label">Sexe apparent</dt><dd class="mt-1 text-sm font-bold text-[var(--text-primary)]">{{ p.apparentGender || 'Non déterminé' }}</dd></div>
              <div><dt class="ui-label">Âge estimé</dt><dd class="mt-1 text-sm font-bold text-[var(--text-primary)]">{{ p.estimatedAgeRange || 'Non renseigné' }}</dd></div>
              <div><dt class="ui-label">Lieu de découverte</dt><dd class="mt-1 text-sm font-bold text-[var(--text-primary)]">{{ p.foundLocation || 'Non renseigné' }}</dd></div>
              <div><dt class="ui-label">Trouvé / pris en charge le</dt><dd class="mt-1 text-sm font-bold text-[var(--text-primary)]">{{ p.foundAt ? (p.foundAt | date:'dd/MM/yyyy HH:mm') : 'Non renseigné' }}</dd></div>
            </dl>
            @if (p.physicalDescription) {
              <div class="mt-3 rounded-sm border border-[var(--app-border)] bg-[var(--app-surface-muted)] p-3">
                <span class="ui-label">Description physique / signes distinctifs</span>
                <p class="mt-1 text-sm text-[var(--text-secondary)]">{{ p.physicalDescription }}</p>
              </div>
            }
          </section>
        }

        <section>
          <div class="mb-4 flex items-center justify-between gap-3">
            <h3 class="text-xs font-black uppercase tracking-wider text-[var(--text-muted)]">Informations administratives</h3>
            @if (!isProvisional()) {
              <span class="rounded-sm bg-[var(--brand-success-subtle)] px-2 py-1 text-[10px] font-black uppercase tracking-wider text-[var(--brand-success-text)]">Identité vérifiée</span>
            }
          </div>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <div class="ui-card-muted p-3"><span class="ui-label block">Sexe</span><span class="text-sm font-extrabold text-[var(--text-primary)]">{{ p.gender || 'Non renseigné' }}</span></div>
            <div class="ui-card-muted p-3"><span class="ui-label block">Date de naissance (âge)</span><span class="text-sm font-extrabold text-[var(--text-primary)]">{{ p.birthDate ? p.birthDate + ' (' + age() + ' ans)' : 'Non renseignée' }}</span></div>
            <div class="ui-card-muted p-3"><span class="ui-label block">Téléphone</span><span class="text-sm font-extrabold text-[var(--text-primary)]">{{ p.phone || 'Non renseigné' }}</span></div>
            <div class="ui-card-muted p-3"><span class="ui-label block">Groupe sanguin</span><span class="text-sm font-extrabold text-[var(--text-primary)]">{{ p.bloodGroup || 'Non renseigné' }}</span></div>
            <div class="ui-card-muted p-3"><span class="ui-label block">Adresse email</span><span class="text-sm font-extrabold text-[var(--text-primary)]">{{ p.email || 'Non renseigné' }}</span></div>
            <div class="ui-card-muted p-3"><span class="ui-label block">Ville</span><span class="text-sm font-extrabold text-[var(--text-primary)]">{{ p.city || 'Non renseignée' }}</span></div>
            <div class="ui-card-muted p-3"><span class="ui-label block">Quartier / District</span><span class="text-sm font-extrabold text-[var(--text-primary)]">{{ p.district || 'Non renseigné' }}</span></div>
            <div class="ui-card-muted p-3"><span class="ui-label block">Adresse géographique</span><span class="text-sm font-extrabold text-[var(--text-primary)]">{{ p.address || 'Non renseignée' }}</span></div>
          </div>
        </section>

        <section>
          <h3 class="mb-4 text-xs font-black uppercase tracking-wider text-[var(--text-muted)]">Contact d'urgence</h3>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div class="ui-card-muted p-3"><span class="ui-label block">Nom complet</span><span class="text-sm font-extrabold text-[var(--text-primary)]">{{ p.emergencyContactName || 'Non renseigné' }}</span></div>
            <div class="ui-card-muted p-3"><span class="ui-label block">Téléphone</span><span class="text-sm font-extrabold text-[var(--text-primary)]">{{ p.emergencyContactPhone || 'Non renseigné' }}</span></div>
          </div>
        </section>

        <app-patient-emergency-context [patientId]="p.id" />
        <app-patient-medical-info [patientId]="p.id" />
      </div>

      @if (showIdentityModal()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center bg-[var(--overlay-bg)] p-3 backdrop-blur-sm sm:p-4" (click)="closeIdentityModal()">
          <form class="ui-card flex max-h-[92vh] w-full max-w-2xl flex-col overflow-hidden" (submit)="submitIdentity($event)" (click)="$event.stopPropagation()">
            <header class="flex items-start justify-between gap-4 border-b border-[var(--app-border)] px-4 py-4 sm:px-5">
              <div>
                <h3 class="font-display text-lg font-black text-[var(--text-primary)]">Identifier le patient</h3>
                <p class="mt-1 text-xs text-[var(--text-muted)]">Le numéro {{ p.temporaryPatientNumber }} restera un alias permanent du dossier.</p>
              </div>
              <button type="button" class="rounded-sm p-2 text-[var(--text-muted)] hover:bg-[var(--app-surface-muted)]" (click)="closeIdentityModal()" aria-label="Fermer">✕</button>
            </header>

            <div class="min-h-0 flex-1 space-y-5 overflow-y-auto px-4 py-4 sm:px-5">
              @if (identityError(); as message) {
                <app-ui-alert tone="error">{{ message }}</app-ui-alert>
              }

              <div>
                <h4 class="ui-label mb-3">Identité confirmée</h4>
                <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <div class="sm:col-span-2"><label class="ui-label mb-1.5">Nom complet *</label><input class="ui-input" name="fullName" [(ngModel)]="identityFullName" required /></div>
                  <div><label class="ui-label mb-1.5">Sexe *</label><select class="ui-input" name="gender" [(ngModel)]="identityGender" required><option value="">Sélectionner</option><option value="MASCULIN">Masculin</option><option value="FEMININ">Féminin</option><option value="AUTRE">Autre</option></select></div>
                  <div><label class="ui-label mb-1.5">Date de naissance *</label><input class="ui-input" type="date" name="birthDate" [(ngModel)]="identityBirthDate" required /></div>
                  <div><label class="ui-label mb-1.5">Téléphone</label><input class="ui-input" name="phone" [(ngModel)]="identityPhone" /></div>
                  <div><label class="ui-label mb-1.5">Adresse email</label><input class="ui-input" type="email" name="email" [(ngModel)]="identityEmail" /></div>
                  <div><label class="ui-label mb-1.5">Ville *</label><input class="ui-input" name="city" [(ngModel)]="identityCity" required /></div>
                  <div><label class="ui-label mb-1.5">Quartier / district</label><input class="ui-input" name="district" [(ngModel)]="identityDistrict" /></div>
                  <div class="sm:col-span-2"><label class="ui-label mb-1.5">Adresse</label><input class="ui-input" name="address" [(ngModel)]="identityAddress" /></div>
                </div>
              </div>

              <div class="border-t border-[var(--divider-subtle)] pt-4">
                <h4 class="ui-label mb-3">Contact d'urgence</h4>
                <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <div><label class="ui-label mb-1.5">Nom</label><input class="ui-input" name="emergencyContactName" [(ngModel)]="identityEmergencyContactName" /></div>
                  <div><label class="ui-label mb-1.5">Téléphone</label><input class="ui-input" name="emergencyContactPhone" [(ngModel)]="identityEmergencyContactPhone" /></div>
                </div>
              </div>

              <div class="border-t border-[var(--divider-subtle)] pt-4">
                <h4 class="ui-label mb-3">Preuve et traçabilité</h4>
                <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <div><label class="ui-label mb-1.5">Source de l'identité *</label><select class="ui-input" name="sourceType" [(ngModel)]="identitySourceType" required><option value="PATIENT">Patient conscient</option><option value="DOCUMENT">Pièce d'identité</option><option value="ACCOMPANYING_PERSON">Accompagnant</option><option value="WITNESS">Témoin</option><option value="HEALTHCARE_PROFESSIONAL">Professionnel de santé</option><option value="OTHER">Autre</option></select></div>
                  <div><label class="ui-label mb-1.5">Preuve / référence *</label><input class="ui-input" name="sourceDetails" [(ngModel)]="identitySourceDetails" placeholder="Ex. CNI n°, déclaration du patient…" required /></div>
                  <div class="sm:col-span-2"><label class="ui-label mb-1.5">Motif de régularisation *</label><textarea class="ui-input min-h-20 py-2.5" name="reason" [(ngModel)]="identityReason" placeholder="Ex. Patient réveillé et identité confirmée sur présentation de sa CNI." required></textarea></div>
                </div>
                <p class="mt-3 text-xs text-[var(--text-muted)]">Aucun rapprochement avec un autre DPU n'est automatique. Les doublons éventuels seront proposés à la revue.</p>
              </div>
            </div>

            <footer class="flex flex-col-reverse gap-2 border-t border-[var(--app-border)] bg-[var(--app-surface)] px-4 py-3 sm:flex-row sm:justify-end sm:px-5">
              <app-ui-button type="button" variant="secondary" (pressed)="closeIdentityModal()" [disabled]="identitySubmitting()">Annuler</app-ui-button>
              <app-ui-button type="submit" variant="primary" [disabled]="identitySubmitting()">{{ identitySubmitting() ? 'Enregistrement…' : 'Confirmer l’identité' }}</app-ui-button>
            </footer>
          </form>
        </div>
      }
    }
  `,
})
export class PatientProfileTabComponent {
  readonly parent = inject(PatientDetailComponent);
  private readonly regularizationApi = inject(ProvisionalPatientRegularizationApiService);

  readonly showIdentityModal = signal(false);
  readonly identitySubmitting = signal(false);
  readonly identityError = signal<string | null>(null);

  identityFullName = '';
  identityGender = '';
  identityBirthDate = '';
  identityPhone = '';
  identityCity = '';
  identityDistrict = '';
  identityAddress = '';
  identityEmail = '';
  identityEmergencyContactName = '';
  identityEmergencyContactPhone = '';
  identitySourceType: IdentitySourceType = 'PATIENT';
  identitySourceDetails = '';
  identityReason = '';

  readonly isProvisional = computed(() => {
    const status = this.parent.patient()?.identityStatus;
    return status === 'PROVISIONAL_URGENCY' || status === 'DECLARED';
  });

  readonly age = computed(() => {
    const birthDate = this.parent.patient()?.birthDate;
    if (!birthDate) return 0;
    const birth = new Date(birthDate);
    if (Number.isNaN(birth.getTime())) return 0;
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const month = today.getMonth() - birth.getMonth();
    if (month < 0 || (month === 0 && today.getDate() < birth.getDate())) age--;
    return Math.max(age, 0);
  });

  openIdentityModal(): void {
    const patient = this.parent.patient();
    if (!patient) return;
    this.identityFullName = patient.identityStatus === 'VERIFIED' ? patient.fullName : '';
    this.identityGender = patient.gender || '';
    this.identityBirthDate = patient.birthDate || '';
    this.identityPhone = patient.phone || '';
    this.identityCity = patient.city || '';
    this.identityDistrict = patient.district || '';
    this.identityAddress = patient.address || '';
    this.identityEmail = patient.email || '';
    this.identityEmergencyContactName = patient.emergencyContactName || '';
    this.identityEmergencyContactPhone = patient.emergencyContactPhone || '';
    this.identitySourceType = 'PATIENT';
    this.identitySourceDetails = '';
    this.identityReason = '';
    this.identityError.set(null);
    this.showIdentityModal.set(true);
  }

  closeIdentityModal(): void {
    if (!this.identitySubmitting()) this.showIdentityModal.set(false);
  }

  submitIdentity(event: Event): void {
    event.preventDefault();
    const patient = this.parent.patient();
    if (!patient || !this.identityFullName.trim() || !this.identityGender || !this.identityBirthDate || !this.identityCity.trim() || !this.identitySourceDetails.trim() || !this.identityReason.trim()) {
      this.identityError.set('Renseignez tous les champs obligatoires.');
      return;
    }

    this.identitySubmitting.set(true);
    this.identityError.set(null);
    this.regularizationApi.regularize(patient.id, {
      fullName: this.identityFullName.trim(),
      gender: this.identityGender,
      birthDate: this.identityBirthDate,
      phone: this.identityPhone.trim() || undefined,
      city: this.identityCity.trim(),
      district: this.identityDistrict.trim() || undefined,
      address: this.identityAddress.trim() || undefined,
      email: this.identityEmail.trim() || undefined,
      emergencyContactName: this.identityEmergencyContactName.trim() || undefined,
      emergencyContactPhone: this.identityEmergencyContactPhone.trim() || undefined,
      sourceType: this.identitySourceType,
      sourceDetails: this.identitySourceDetails.trim(),
      reason: this.identityReason.trim(),
    }).subscribe({
      next: () => {
        this.identitySubmitting.set(false);
        this.showIdentityModal.set(false);
        this.parent.loadPatient(patient.id);
      },
      error: err => {
        this.identitySubmitting.set(false);
        this.identityError.set(err.error?.detail || err.error?.title || "Impossible de régulariser l'identité du patient.");
      },
    });
  }
}
