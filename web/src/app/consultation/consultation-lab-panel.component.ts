import { Component, inject, input, output } from '@angular/core';
import { FormArray, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { I18nService } from '../core/i18n/i18n.service';
@Component({
  selector: 'app-consultation-lab-panel',
  imports: [ReactiveFormsModule],
  templateUrl: './consultation-lab-panel.component.html',
})
export class ConsultationLabPanelComponent {
  readonly form = input.required<FormGroup>();
  readonly commonExams = input<readonly { code: string; labelKey: string }[]>([]);
  readonly added = output<string>();
  readonly removed = output<number>();
  readonly i18n = inject(I18nService);
  get labExams(): FormArray { return this.form().get('exams') as FormArray; }
  commonExamLabel(exam: { code: string; labelKey: string }): string { return this.i18n.t(exam.labelKey); }
}
