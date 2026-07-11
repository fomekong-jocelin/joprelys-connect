import { CommonModule, DatePipe } from '@angular/common';
import { Component, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { I18nService } from '../../core/i18n/i18n.service';
import { ButtonComponent } from '../../shared/ui/button.component';
import {
  CreateEmergencyLegalBasisRequest,
  EmergencyCapacityEvent,
  EmergencyLegalBasis,
  EmergencyLegalBasisType,
} from './emergency-medico-legal.models';

@Component({
  selector: 'app-emergency-legal-basis-section',
  standalone: true,
  imports: [CommonModule, DatePipe, ReactiveFormsModule, ButtonComponent],
  templateUrl: './emergency-legal-basis-section.component.html',
})
export class EmergencyLegalBasisSectionComponent {
  private readonly fb = inject(FormBuilder);
  readonly i18n = inject(I18nService);

  readonly legalBases = input.required<EmergencyLegalBasis[]>();
  readonly currentCapacity = input<EmergencyCapacityEvent>();
  readonly disabled = input(false);
  readonly submitted = output<CreateEmergencyLegalBasisRequest>();
  readonly showForm = signal(false);

  readonly form = this.fb.nonNullable.group({
    basisType: ['VITAL_EMERGENCY'],
    justification: ['', [Validators.required, Validators.maxLength(1500)]],
    coveredActs: ['', Validators.required],
    expiresAt: [''],
  });

  toggleForm(): void {
    this.showForm.update(value => !value);
  }

  requiresIncapacity(): boolean {
    const type = this.form.controls.basisType.value;
    return type === 'VITAL_EMERGENCY' || type === 'PRESUMED_CONSENT';
  }

  canSubmit(): boolean {
    if (!this.requiresIncapacity()) return true;
    return this.currentCapacity()?.status === 'INCAPABLE';
  }

  submit(): void {
    if (this.form.invalid || !this.canSubmit() || this.disabled()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const coveredActs = value.coveredActs
      .split(/[\n,;]+/)
      .map(item => item.trim())
      .filter(Boolean);
    if (coveredActs.length === 0) return;

    this.submitted.emit({
      basisType: value.basisType as EmergencyLegalBasisType,
      justification: value.justification.trim(),
      expiresAt: value.expiresAt ? new Date(value.expiresAt).toISOString() : undefined,
      coveredActs,
    });
    this.form.reset({
      basisType: 'VITAL_EMERGENCY',
      justification: '',
      coveredActs: '',
      expiresAt: '',
    });
    this.showForm.set(false);
  }

  t(key: string): string {
    return this.i18n.t(key);
  }
}
