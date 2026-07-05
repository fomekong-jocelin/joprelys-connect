import { Component } from '@angular/core';
import { AppShellComponent } from '../../../shared/layout/app-shell.component';
import { PatientNotificationsComponent } from '../components/patient-notifications.component';

@Component({
  selector: 'app-patient-notifications-page',
  standalone: true,
  imports: [AppShellComponent, PatientNotificationsComponent],
  template: `
    <app-shell>
      <div class="app-container py-6">
        <app-patient-notifications />
      </div>
    </app-shell>
  `
})
export class PatientNotificationsPageComponent {}
