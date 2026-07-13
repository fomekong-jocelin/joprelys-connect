import { CommonModule, DatePipe } from '@angular/common';
import { Component, effect, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiErrorI18nService } from '../../core/i18n/api-error-i18n.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { AlertComponent } from '../../shared/ui/alert.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state.component';
import { EmergencyApiService } from '../emergency-api.service';
import {
  CreateEmergencyTriageAssessmentRequest,
  EmergencyAirwayStatus,
  EmergencyBreathingStatus,
  EmergencyCirculationStatus,
  EmergencyDisabilityStatus,
  EmergencyExposureStatus,
  EmergencyRecommendedOrientation,
  EmergencyTriageAssessment,
} from './emergency-triage.models';

@Component({
  selector: 'app-emergency-triage-panel',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DatePipe,
    AlertComponent,
    ButtonComponent,
    EmptyStateComponent,
  ],
  templateUrl: './emergency-triage-panel.component.html',
})
export class EmergencyTriagePanelComponent {
  private readonly api = inject(EmergencyApiService);
  private readonly fb = inject(FormBuilder);
  private readonly apiErrors = inject(ApiErrorI18nService);
  readonly i18n = inject(I18nService);

  readonly emergencyId = input.required<string>();
  readonly assessments = signal<EmergencyTriageAssessment[]>([]);
  readonly isLoading = signal(false);
  readonly isSaving = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  readonly airwayOptions: readonly EmergencyAirwayStatus[] = [
    'PATENT', 'AT_RISK', 'OBSTRUCTED',
  ];
  readonly breathingOptions: readonly EmergencyBreathingStatus[] = [
    'ADEQUATE', 'DISTRESS', 'FAILURE',
  ];
  readonly circulationOptions: readonly EmergencyCirculationStatus[] = [
    'STABLE', 'COMPROMISED', 'SHOCK',
  ];
  readonly disabilityOptions: readonly EmergencyDisabilityStatus[] = [
    'ALERT', 'RESPONDS_TO_VOICE', 'RESPONDS_TO_PAIN', 'UNRESPONSIVE',
  ];
  readonly exposureOptions: readonly EmergencyExposureStatus[] = [
    'NO_CRITICAL_FINDING', 'TRAUMA', 'HYPOTHERMIA', 'HYPERTHERMIA', 'OTHER',
  ];
  readonly orientationOptions: readonly EmergencyRecommendedOrientation[] = [
    'RESUSCITATION',
    'OPERATING_ROOM',
    'HOSPITALIZATION',
    'CONSULTATION',
    'TRANSFER',
    'DISCHARGE',
    'DEATH',
  ];

  readonly form = this.fb.group({
    triageLevel: this.fb.nonNullable.control<'RED' | 'ORANGE' | 'YELLOW' | 'GREEN'>('RED'),
    hemodynamicStatus: this.fb.nonNullable.control<'SHOCK' | 'UNSTABLE' | 'STABLE'>('SHOCK'),
    airwayStatus: this.fb.nonNullable.control<EmergencyAirwayStatus>('PATENT'),
    breathingStatus: this.fb.nonNullable.control<EmergencyBreathingStatus>('ADEQUATE'),
    circulationStatus: this.fb.nonNullable.control<EmergencyCirculationStatus>('STABLE'),
    disabilityStatus: this.fb.nonNullable.control<EmergencyDisabilityStatus>('ALERT'),
    exposureStatus: this.fb.nonNullable.control<EmergencyExposureStatus>('NO_CRITICAL_FINDING'),
    bpSystolic: this.fb.control<number | null>(null, [Validators.min(30), Validators.max(300)]),
    bpDiastolic: this.fb.control<number | null>(null, [Validators.min(20), Validators.max(200)]),
    heartRate: this.fb.control<number | null>(null, [Validators.min(20), Validators.max(300)]),
    respiratoryRate: this.fb.control<number | null>(null, [Validators.min(0), Validators.max(100)]),
    oxygenSaturation: this.fb.control<number | null>(null, [Validators.min(0), Validators.max(100)]),
    temperature: this.fb.control<number | null>(null, [Validators.min(25), Validators.max(45)]),
    gcsScore: this.fb.control<number | null>(null, [Validators.min(3), Validators.max(15)]),
    painScore: this.fb.control<number | null>(null, [Validators.min(0), Validators.max(10)]),
    recommendedOrientation: this.fb.control<EmergencyRecommendedOrientation | null>(null),
    clinicalNotes: this.fb.nonNullable.control('', [Validators.maxLength(4000)]),
  });

  constructor() {
    effect(() => {
      const id = this.emergencyId();
      if (id) this.load(id);
    });
  }

  load(id = this.emergencyId()): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.api.getTriageAssessments(id).pipe(
      finalize(() => this.isLoading.set(false)),
    ).subscribe({
      next: history => this.assessments.set(history),
      error: err => this.error.set(this.apiErrors.message(
        err,
        'emergency.triage.error',
        'emergency.triage.error.load',
      )),
    });
  }

  submit(): void {
    if (this.form.invalid || this.isSaving()) {
      this.form.markAllAsTouched();
      this.error.set(this.text('invalid'));
      return;
    }

    this.isSaving.set(true);
    this.error.set(null);
    this.success.set(null);
    this.api.addTriageAssessment(this.emergencyId(), this.payload()).pipe(
      finalize(() => this.isSaving.set(false)),
    ).subscribe({
      next: assessment => {
        this.assessments.update(items => [...items, assessment]);
        this.form.controls.clinicalNotes.reset('');
        this.success.set(this.text('saved'));
      },
      error: err => this.error.set(this.apiErrors.message(
        err,
        'emergency.triage.error',
        'emergency.triage.error.save',
      )),
    });
  }

  text(key: string): string {
    return this.i18n.t(`emergency.triage.${key}`);
  }

  assessmentTypeLabel(value: string): string {
    return this.text(`type.${value.toLowerCase()}`);
  }

  statusLabel(axis: string, value: string): string {
    return this.text(`${axis}.${value.toLowerCase()}`);
  }

  orientationLabel(value?: string): string {
    return value ? this.text(`orientation.${value.toLowerCase()}`) : this.text('orientation.none');
  }

  triageLabel(value: string): string {
    return this.i18n.t(`emergency.detail.triage.${value.toLowerCase()}`, value);
  }

  hemodynamicLabel(value: string): string {
    return this.i18n.t(`emergency.detail.hemodynamic.${value.toLowerCase()}`, value);
  }

  private payload(): CreateEmergencyTriageAssessmentRequest {
    const value = this.form.getRawValue();
    return {
      triageLevel: value.triageLevel,
      hemodynamicStatus: value.hemodynamicStatus,
      bpSystolic: this.optionalNumber(value.bpSystolic),
      bpDiastolic: this.optionalNumber(value.bpDiastolic),
      heartRate: this.optionalNumber(value.heartRate),
      temperature: this.optionalNumber(value.temperature),
      abcdeAssessment: {
        airwayStatus: value.airwayStatus,
        breathingStatus: value.breathingStatus,
        circulationStatus: value.circulationStatus,
        disabilityStatus: value.disabilityStatus,
        exposureStatus: value.exposureStatus,
        respiratoryRate: this.optionalNumber(value.respiratoryRate),
        oxygenSaturation: this.optionalNumber(value.oxygenSaturation),
        gcsScore: this.optionalNumber(value.gcsScore),
        painScore: this.optionalNumber(value.painScore),
        recommendedOrientation: value.recommendedOrientation ?? undefined,
        clinicalNotes: value.clinicalNotes.trim() || undefined,
      },
    };
  }

  private optionalNumber(value: number | null): number | undefined {
    return value === null ? undefined : value;
  }
}