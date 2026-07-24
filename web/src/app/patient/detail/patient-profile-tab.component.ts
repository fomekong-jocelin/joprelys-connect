import { Component, computed, inject, signal } from '@angular/core';
import { PatientDetailComponent } from '../patient-detail.component';
import { PatientMedicalInfoComponent } from '../patient-medical-info.component';
import { Patient } from '../patient.models';
import { PatientAdministrativeSummaryComponent } from './patient-administrative-summary.component';
import { PatientEmergencyContextComponent } from './patient-emergency-context.component';
import { PatientIdentityRegularizationDialogComponent } from './patient-identity-regularization-dialog.component';
import { PatientProvisionalIdentityCardComponent } from './patient-provisional-identity-card.component';

@Component({
  selector: 'app-patient-profile-tab',
  standalone: true,
  imports: [
    PatientAdministrativeSummaryComponent,
    PatientEmergencyContextComponent,
    PatientIdentityRegularizationDialogComponent,
    PatientMedicalInfoComponent,
    PatientProvisionalIdentityCardComponent,
  ],
  template: `
    @if (parent.patient(); as patient) {
      <!--
        Grid is intentional here instead of space-y-*.
        Angular component hosts are custom elements and can otherwise behave like
        inline boxes, making vertical margins between profile cards unreliable.
        DESIGN.md defines 24 px as the large section spacing.
      -->
      <div class="grid gap-6 animate-fade-in">
        @if (isProvisional()) {
          <app-patient-provisional-identity-card
            class="block"
            [patient]="patient"
            (identifyRequested)="openIdentityDialog()"
          />
        }

        <app-patient-administrative-summary class="block" [patient]="patient" />
        <app-patient-emergency-context class="block" [patientId]="patient.id" />

        <!--
          Longitudinal medical details are secondary on the identity/profile view.
          They stay folded until requested; the critical-allergy warning remains
          permanently visible in PatientDetailComponent when applicable.
        -->
        <div class="grid gap-4">
          <button
            type="button"
            class="ui-card flex w-full items-center justify-between gap-4 p-4 text-left sm:p-5"
            [attr.aria-expanded]="medicalExpanded()"
            (click)="toggleMedicalInformation()"
          >
            <div>
              <h3 class="font-display text-sm font-black uppercase tracking-wider text-[var(--text-primary)]">
                {{ parent.i18n.t('patients.medicalInfo.allergies') }} ·
                {{ parent.i18n.t('patients.medicalInfo.history') }} ·
                {{ parent.i18n.t('patients.medicalInfo.vaccinations.title') }}
              </h3>
            </div>
            <svg
              class="h-4 w-4 shrink-0 text-[var(--text-muted)] transition-transform"
              [class.rotate-180]="medicalExpanded()"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              aria-hidden="true"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="m6 9 6 6 6-6" />
            </svg>
          </button>

          @if (medicalExpanded()) {
            <app-patient-medical-info class="block animate-fade-in" [patientId]="patient.id" />
          }
        </div>
      </div>

      @if (identityDialogOpen()) {
        <app-patient-identity-regularization-dialog
          [patient]="patient"
          (cancelled)="closeIdentityDialog()"
          (saved)="onIdentitySaved($event)"
        />
      }
    }
  `,
})
export class PatientProfileTabComponent {
  readonly parent = inject(PatientDetailComponent);
  readonly identityDialogOpen = signal(false);
  readonly medicalExpanded = signal(false);

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
    const monthDifference = today.getMonth() - birth.getMonth();
    if (monthDifference < 0 || (monthDifference === 0 && today.getDate() < birth.getDate())) {
      age--;
    }
    return Math.max(age, 0);
  });

  toggleMedicalInformation(): void {
    this.medicalExpanded.update(value => !value);
  }

  openIdentityDialog(): void {
    this.identityDialogOpen.set(true);
  }

  closeIdentityDialog(): void {
    this.identityDialogOpen.set(false);
  }

  onIdentitySaved(patient: Patient): void {
    this.identityDialogOpen.set(false);
    this.parent.loadPatient(patient.id);
  }
}
