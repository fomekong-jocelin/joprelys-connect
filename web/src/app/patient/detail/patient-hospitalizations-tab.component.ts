import { Component, DestroyRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { EmergencyHospitalizationContinuationComponent } from '../emergency-hospitalization-continuation.component';
import { PatientDetailComponent } from '../patient-detail.component';
import { PatientHospitalizationComponent } from '../patient-hospitalization.component';

@Component({
  selector: 'app-patient-hospitalizations-tab',
  standalone: true,
  imports: [EmergencyHospitalizationContinuationComponent, PatientHospitalizationComponent],
  template: `
    @if (parent.patient(); as p) {
      <div class="space-y-6 animate-fade-in">
        @if (emergencyId(); as sourceEmergencyId) {
          <app-emergency-hospitalization-continuation
            [patientId]="p.id"
            [emergencyId]="sourceEmergencyId"
            [identityStatus]="p.identityStatus"
            [temporaryPatientNumber]="p.temporaryPatientNumber"
            (admitted)="onAdmitted()"
            (cancelled)="clearEmergencyContext()"
          />
        }
        <app-patient-hospitalization [patientId]="p.id" />
      </div>
    }
  `,
})
export class PatientHospitalizationsTabComponent {
  readonly parent = inject(PatientDetailComponent);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly hospitalization = viewChild(PatientHospitalizationComponent);

  readonly emergencyId = signal<string | null>(
    this.route.snapshot.queryParamMap.get('emergencyId'),
  );

  constructor() {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      this.emergencyId.set(params.get('emergencyId'));
    });
  }

  onAdmitted(): void {
    this.hospitalization()?.loadHospitalizations();
    this.clearEmergencyContext();
  }

  clearEmergencyContext(): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { emergencyId: null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }
}
