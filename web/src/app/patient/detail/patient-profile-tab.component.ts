import { Component, computed, inject, signal } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';
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
      <div class="grid gap-6 animate-fade-in">
        @if (isProvisional()) {
          <app-patient-provisional-identity-card
            class="block"
            [patient]="patient"
            (identifyRequested)="openIdentityDialog()"
          />
        }

        <!-- The identity route owns these details: no redundant administrative accordion. -->
        <section class="ui-card p-4 sm:p-5" data-testid="patient-identity-details">
          <app-patient-administrative-summary class="block" [patient]="patient" />
        </section>

        <!-- Emergency history stays independently collapsible and auto-opens only when safety requires it. -->
        <app-patient-emergency-context class="block" [patientId]="patient.id" />

        <!-- Longitudinal medical information remains secondary and folded by default. -->
        <button
          type="button"
          class="ui-card flex min-h-14 w-full items-center justify-between gap-3 p-4 text-left"
          [attr.aria-expanded]="medicalExpanded()"
          (click)="toggleMedicalInformation()"
          data-testid="patient-medical-information-toggle"
        >
          <h3 class="min-w-0 whitespace-nowrap font-display text-sm font-extrabold text-[var(--text-primary)] sm:text-base">
            {{ i18n.t('patient.urgTemp.profile.medicalSection') }}
          </h3>
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
          <div class="animate-fade-in">
            <app-patient-medical-info class="block" [patientId]="patient.id" />
          </div>
        }
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
  readonly i18n = inject(I18nService);
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
