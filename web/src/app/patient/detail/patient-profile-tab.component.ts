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
        <app-patient-medical-info class="block" [patientId]="patient.id" />
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
