import { Component } from '@angular/core';
import { AppShellComponent } from '../../../shared/layout/app-shell.component';
import { PatientRequestsListComponent } from '../components/patient-requests-list.component';

@Component({
  selector: 'app-patient-requests-page',
  standalone: true,
  imports: [AppShellComponent, PatientRequestsListComponent],
  template: `
    <app-shell>
      <div class="app-container py-6">
        <app-patient-requests-list />
      </div>
    </app-shell>
  `
})
export class PatientRequestsPageComponent {}
