import { Injectable, inject } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';

@Injectable()
export class ConsultationUiLabelsService {
  private readonly i18n = inject(I18nService);

  painLabel(pain?: number | null): string {
    if (pain === undefined || pain === null) return '';
    if (pain === 0) return this.i18n.t('consultation.vitals.painNone');
    if (pain <= 3) return this.i18n.t('consultation.vitals.painMild');
    if (pain <= 6) return this.i18n.t('consultation.vitals.painModerate');
    return this.i18n.t('consultation.vitals.painSevere');
  }

  painClasses(pain?: number | null): string {
    const value = pain ?? 0;
    if (value <= 3) {
      return 'border-[var(--brand-success-muted)] bg-[var(--brand-success-subtle)] text-[var(--brand-success-text)]';
    }
    if (value <= 6) {
      return 'border-[var(--brand-warning-border)] bg-[var(--brand-warning-subtle)] text-[var(--brand-warning-text)]';
    }
    return 'border-[var(--brand-danger-border)] bg-[var(--brand-danger-subtle)] text-[var(--brand-danger-text)]';
  }

  vitalsFieldLabel(field: string): string {
    return this.i18n.t(`vitals.assistant.field.${field}`, field);
  }

  vitalsFieldUnit(field: string): string {
    const units: Record<string, string> = {
      temperature: '°C',
      pulse: 'bpm',
      respiratoryRate: 'resp/min',
      systolic: 'mmHg',
      diastolic: 'mmHg',
      spo2: '%',
      weight: 'kg',
      height: 'cm',
      glycemia: 'g/L',
      painScale: '/10',
    };
    return units[field] ?? '';
  }
}
