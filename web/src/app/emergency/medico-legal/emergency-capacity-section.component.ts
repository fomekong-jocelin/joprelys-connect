import { CommonModule, DatePipe } from '@angular/common';
import { Component, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { I18nService } from '../../core/i18n/i18n.service';
import { ButtonComponent } from '../../shared/ui/button.component';
import {
  EmergencyCapacityEvent,
  EmergencyCapacityStatus,
  RecordEmergencyCapacityRequest,
} from './emergency-medico-legal.models';

@Component({
  selector: 'app-emergency-capacity-section',
  standalone: true,
  imports: [CommonModule, DatePipe, ReactiveFormsModule, ButtonComponent],
  templateUrl: './emergency-capacity-section.component.html',
})
export class EmergencyCapacitySectionComponent {
  private readonly fb = inject(FormBuilder);
  readonly i18n = inject(I18nService);

  readonly history = input.required<EmergencyCapacityEvent[]>();
  readonly current = input<EmergencyCapacityEvent>();
  readonly disabled = input(false);
  readonly submitted = output<RecordEmergencyCapacityRequest>();
  readonly showForm = signal(false);

  readonly form = this.fb.nonNullable.group({
    status: ['INCAPABLE'],
    consciousnessLevel: ['UNCONSCIOUS'],
    clinicalReason: ['', [Validators.required, Validators.maxLength(1000)]],
  });

  toggleForm(): void {
    this.showForm.update(value => !value);
  }

  submit(): void {
    if (this.form.invalid || this.disabled()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.submitted.emit({
      status: value.status as EmergencyCapacityStatus,
      consciousnessLevel: value.consciousnessLevel || undefined,
      clinicalReason: value.clinicalReason.trim(),
    });
    this.form.reset({ status: 'INCAPABLE', consciousnessLevel: 'UNCONSCIOUS', clinicalReason: '' });
    this.showForm.set(false);
  }

  t(key: string): string {
    return this.i18n.t(key);
  }
}
