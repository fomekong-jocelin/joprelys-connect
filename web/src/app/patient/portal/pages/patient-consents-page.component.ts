import { Component } from '@angular/core';
import { AppShellComponent } from '../../../shared/layout/app-shell.component';
import { PatientConsentsListComponent } from '../components/patient-consents-list.component';

@Component({
  selector: 'app-patient-consents-page',
  standalone: true,
  imports: [AppShellComponent, PatientConsentsListComponent],
  template: `
    <app-shell>
      <div class="app-container py-6">
        <app-patient-consents-list />
      </div>
    </app-shell>
  `
})
export class PatientConsentsPageComponent {}
