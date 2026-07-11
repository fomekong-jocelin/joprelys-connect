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
      <div class="space-y-6 animate-fade-in">
        @if (isProvisional()) {
          <app-patient-provisional-identity-card
            [patient]="patient"
            (identifyRequested)="openIdentityDialog()"
          />
        }

        <app-patient-administrative-summary [patient]="patient" />
        <app-patient-emergency-context [patientId]="patient.id" />
        <app-patient-medical-info [patientId]="patient.id" />
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
