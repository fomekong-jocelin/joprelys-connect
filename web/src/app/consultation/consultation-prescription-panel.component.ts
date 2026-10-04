import { CommonModule } from '@angular/common';
import { Component, inject, input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { I18nService } from '../core/i18n/i18n.service';
import { ConsultationPrescriptionFacade } from './consultation-prescription.facade';
@Component({
  selector: 'app-consultation-prescription-panel',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './consultation-prescription-panel.component.html',
})
export class ConsultationPrescriptionPanelComponent {
  readonly form = input.required<FormGroup>();
  readonly prescriptions = inject(ConsultationPrescriptionFacade);
  readonly i18n = inject(I18nService);
}
