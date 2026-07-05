import { Component } from '@angular/core';
import { AppShellComponent } from '../../../shared/layout/app-shell.component';
import { PatientAuditListComponent } from '../components/patient-audit-list.component';

@Component({
  selector: 'app-patient-audit-page',
  standalone: true,
  imports: [AppShellComponent, PatientAuditListComponent],
  template: `
    <app-shell>
      <div class="app-container py-6">
        <app-patient-audit-list />
      </div>
    </app-shell>
  `
})
export class PatientAuditPageComponent {}
