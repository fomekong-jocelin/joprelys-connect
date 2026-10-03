import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../core/i18n/i18n.service';
import { AiVitalField, AiVitalsProposal } from '../../consultation/ai-vitals-api.service';
import { SmartVitalsAssistantComponent } from '../../consultation/smart-vitals-assistant.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { VisitApiService } from '../../visit/visit-api.service';
import { Visit, Vitals } from '../../visit/visit.models';
import { bmiClass } from '../../visit/vitals-display.util';

/** Plages de saisie acceptées (identiques aux contraintes du backend). */
const VITAL_RANGES = {
  temperature: [30, 45],
  weight: [1, 500],
  height: [30, 250],
  pulse: [20, 250],
  systolic: [40, 250],
  diastolic: [30, 150],
  spo2: [50, 100],
  glycemia: [0.1, 10],
  respiratoryRate: [5, 100],
  painScale: [0, 10],
} as const;

/** Saisie d'une nouvelle mesure de constantes pour une visite active. */
@Component({
  selector: 'app-visit-vitals-form-modal',
  standalone: true,
  imports: [FormsModule, ButtonComponent, SmartVitalsAssistantComponent],
  templateUrl: './visit-vitals-form-modal.component.html',
})
export class VisitVitalsFormModalComponent implements OnInit {
  private readonly i18n = inject(I18nService);
  private readonly visitApi = inject(VisitApiService);

  readonly visit = input.required<Visit>();
  readonly saved = output<Vitals>();
  readonly closed = output<void>();

  readonly isSavingVitals = signal(false);
  readonly vitalsError = signal('');

  vitalsTemp?: number;
  vitalsWeight?: number;
  vitalsHeight?: number;
  vitalsPulse?: number;
  vitalsSystolic?: number;
  vitalsDiastolic?: number;
  vitalsSpo2?: number;
  vitalsGlycemia?: number;
  vitalsResp?: number;
  vitalsPain?: number;

  ngOnInit(): void {
    // Une nouvelle mesure part de la précédente : l'infirmier corrige ce qui a changé.
    const previous = this.visit().vitals;
    this.vitalsTemp = previous?.temperature;
    this.vitalsWeight = previous?.weight;
    this.vitalsHeight = previous?.height;
    this.vitalsPulse = previous?.pulse;
    this.vitalsSystolic = previous?.systolic;
    this.vitalsDiastolic = previous?.diastolic;
    this.vitalsSpo2 = previous?.spo2;
    this.vitalsGlycemia = previous?.glycemia;
    this.vitalsResp = previous?.respiratoryRate;
    this.vitalsPain = previous?.painScale;
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  get computedBmi(): number | null {
    if (!this.vitalsWeight || !this.vitalsHeight || this.vitalsHeight <= 0) return null;
    const heightM = this.vitalsHeight / 100;
    return parseFloat((this.vitalsWeight / (heightM * heightM)).toFixed(2));
  }

  getBmiClass(bmi?: number): string {
    return bmiClass(bmi);
  }

  isTempInvalid(): boolean { return this.outOfRange(this.vitalsTemp, VITAL_RANGES.temperature); }
  isWeightInvalid(): boolean { return this.outOfRange(this.vitalsWeight, VITAL_RANGES.weight); }
  isHeightInvalid(): boolean { return this.outOfRange(this.vitalsHeight, VITAL_RANGES.height); }
  isPulseInvalid(): boolean { return this.outOfRange(this.vitalsPulse, VITAL_RANGES.pulse); }
  isSystolicInvalid(): boolean { return this.outOfRange(this.vitalsSystolic, VITAL_RANGES.systolic); }
  isDiastolicInvalid(): boolean { return this.outOfRange(this.vitalsDiastolic, VITAL_RANGES.diastolic); }
  isSpo2Invalid(): boolean { return this.outOfRange(this.vitalsSpo2, VITAL_RANGES.spo2); }
  isGlycemiaInvalid(): boolean { return this.outOfRange(this.vitalsGlycemia, VITAL_RANGES.glycemia); }
  isRespInvalid(): boolean { return this.outOfRange(this.vitalsResp, VITAL_RANGES.respiratoryRate); }
  isPainInvalid(): boolean { return this.outOfRange(this.vitalsPain, VITAL_RANGES.painScale); }

  isBloodPressureInconsistent(): boolean {
    return (
      typeof this.vitalsSystolic === 'number' &&
      typeof this.vitalsDiastolic === 'number' &&
      this.vitalsSystolic <= this.vitalsDiastolic
    );
  }

  /** Une glycémie > 3 g/L est rare : souvent une saisie en mmol/L dans le champ g/L. */
  isGlycemiaUnitSuspicious(): boolean {
    return typeof this.vitalsGlycemia === 'number' && this.vitalsGlycemia > 3;
  }

  isAnyVitalInvalid(): boolean {
    return (
      this.isBloodPressureInconsistent() ||
      this.isTempInvalid() ||
      this.isWeightInvalid() ||
      this.isHeightInvalid() ||
      this.isPulseInvalid() ||
      this.isSystolicInvalid() ||
      this.isDiastolicInvalid() ||
      this.isSpo2Invalid() ||
      this.isGlycemiaInvalid() ||
      this.isRespInvalid() ||
      this.isPainInvalid()
    );
  }

  close(): void {
    if (!this.isSavingVitals()) this.closed.emit();
  }

  submitVitals(): void {
    if (this.isSavingVitals() || this.isAnyVitalInvalid()) return;
    this.isSavingVitals.set(true);
    this.vitalsError.set('');

    this.visitApi.saveVitals(this.visit().id, {
      temperature: this.vitalsTemp,
      weight: this.vitalsWeight,
      height: this.vitalsHeight,
      pulse: this.vitalsPulse,
      systolic: this.vitalsSystolic,
      diastolic: this.vitalsDiastolic,
      spo2: this.vitalsSpo2,
      glycemia: this.vitalsGlycemia,
      respiratoryRate: this.vitalsResp,
      painScale: this.vitalsPain,
    }).subscribe({
      next: (vitals) => {
        this.isSavingVitals.set(false);
        this.saved.emit(vitals);
      },
      error: (err) => {
        this.isSavingVitals.set(false);
        this.vitalsError.set(err.error?.detail || err.error?.title || this.t('dashboard.vitals.saveError'));
      },
    });
  }

  applyVitalsAssistantProposal(proposal: AiVitalsProposal): void {
    const vitals = proposal.vitals;
    if (typeof vitals.temperature === 'number') this.vitalsTemp = vitals.temperature;
    if (typeof vitals.weight === 'number') this.vitalsWeight = vitals.weight;
    if (typeof vitals.height === 'number') this.vitalsHeight = vitals.height;
    if (typeof vitals.pulse === 'number') this.vitalsPulse = vitals.pulse;
    if (typeof vitals.systolic === 'number') this.vitalsSystolic = vitals.systolic;
    if (typeof vitals.diastolic === 'number') this.vitalsDiastolic = vitals.diastolic;
    if (typeof vitals.spo2 === 'number') this.vitalsSpo2 = vitals.spo2;
    if (typeof vitals.glycemia === 'number') this.vitalsGlycemia = vitals.glycemia;
    if (typeof vitals.respiratoryRate === 'number') this.vitalsResp = vitals.respiratoryRate;
    if (typeof vitals.painScale === 'number') this.vitalsPain = vitals.painScale;
  }

  currentVitalsForAssistant(): Partial<Record<AiVitalField, number>> {
    const values: Array<[AiVitalField, number | undefined]> = [
      ['temperature', this.vitalsTemp],
      ['weight', this.vitalsWeight],
      ['height', this.vitalsHeight],
      ['pulse', this.vitalsPulse],
      ['systolic', this.vitalsSystolic],
      ['diastolic', this.vitalsDiastolic],
      ['spo2', this.vitalsSpo2],
      ['glycemia', this.vitalsGlycemia],
      ['respiratoryRate', this.vitalsResp],
      ['painScale', this.vitalsPain],
    ];
    return Object.fromEntries(
      values.filter((entry): entry is [AiVitalField, number] => typeof entry[1] === 'number'),
    );
  }

  private outOfRange(value: number | undefined | null, [min, max]: readonly [number, number]): boolean {
    return value !== undefined && value !== null && (value < min || value > max);
  }
}
