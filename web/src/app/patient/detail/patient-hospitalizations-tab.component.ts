import { Component, inject } from '@angular/core';
import { PatientDetailComponent } from '../patient-detail.component';
import { PatientHospitalizationComponent } from '../patient-hospitalization.component';

@Component({
  selector: 'app-patient-hospitalizations-tab',
  standalone: true,
  imports: [PatientHospitalizationComponent],
  template: `
    @if (parent.patient(); as p) {
      <div class="space-y-6 animate-fade-in">
        <app-patient-hospitalization [patientId]="p.id" />
      </div>
    }
  `
})
export class PatientHospitalizationsTabComponent {
  readonly parent = inject(PatientDetailComponent);
}
