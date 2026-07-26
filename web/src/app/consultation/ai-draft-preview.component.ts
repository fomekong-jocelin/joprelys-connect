import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';
import { AiConsultationDraft, AiField } from './ai-consultation-api.service';

@Component({
  selector: 'app-ai-draft-preview',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (entries().length > 0) {
      <section class="ui-card-subtle space-y-3 p-3 sm:p-4">
        <div>
          <p class="ui-label">{{ i18n.t('consultation.ai.acceptedDraft') }}</p>
          <p class="mt-1 text-[10px] leading-4 text-[var(--text-muted)]">
            {{ i18n.t('consultation.ai.acceptedDraftGovernanceHelp') }}
          </p>
        </div>

        <div class="grid grid-cols-1 gap-2 sm:grid-cols-2">
          @for (entry of entries(); track entry.key) {
            <div class="rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] p-3">
              <p class="ui-label">{{ fieldLabel(entry.key) }}</p>
              <p class="mt-1 line-clamp-5 whitespace-pre-wrap text-xs leading-5 text-[var(--text-primary)]">
                {{ formatValue(entry.key, entry.value) }}
              </p>
            </div>
          }
        </div>

        <button
          type="button"
          (click)="apply.emit()"
          [disabled]="!canApply"
          class="ui-button ui-button-primary w-full sm:w-auto"
        >
          {{ i18n.t('consultation.ai.apply') }}
        </button>
      </section>
    }
  `,
})
export class AiDraftPreviewComponent {
  readonly i18n = inject(I18nService);

  @Input() draft: AiConsultationDraft = {};
  @Input() canApply = true;
  @Output() readonly apply = new EventEmitter<void>();

  entries(): Array<{ key: AiField; value: string }> {
    const diagnosis = this.draft.diagnosis?.trim();
    return (Object.entries(this.draft) as Array<[AiField, string]>)
      .filter(([key, value]) => {
        if (!value?.trim()) return false;
        if (key === 'finalDiagnosis' && diagnosis && value.trim() === diagnosis) return false;
        return true;
      })
      .map(([key, value]) => ({ key, value }));
  }

  formatValue(field: AiField, value: string): string {
    if (!['prescription', 'labOrders', 'vitals'].includes(field)) return value;
    try {
      const parsed = JSON.parse(value);
      if (field === 'prescription' && Array.isArray(parsed)) {
        return parsed
          .map((line: Record<string, unknown>) =>
            [line['drugName'], line['dosage'], line['frequency'], line['duration']]
              .filter(Boolean)
              .join(' · '),
          )
          .join('\n');
      }
      if (field === 'labOrders' && Array.isArray(parsed)) {
        return parsed.join(', ');
      }
      if (field === 'vitals' && parsed && typeof parsed === 'object') {
        return Object.entries(parsed as Record<string, unknown>)
          .map(([key, item]) => `${this.vitalLabel(key)}: ${this.vitalValue(key, item)}`)
          .join(' · ');
      }
    } catch {
      return value;
    }
    return value;
  }

  fieldLabel(field: AiField): string {
    const labels: Record<AiField, string> = {
      symptoms: this.i18n.t('consultation.symptoms.label'),
      clinicalExam: this.i18n.t('consultation.clinicalExam.label'),
      suspectedDiagnosis: this.i18n.t('consultation.suspectedDiagnosis.label'),
      diagnosis: this.i18n.t('consultation.diagnosis.label'),
      finalDiagnosis: this.i18n.t('consultation.finalDiagnosis.label'),
      conclusion: this.i18n.t('consultation.conclusion.label'),
      advice: this.i18n.t('consultation.advice.label'),
      followUp: this.i18n.t('consultation.followUp.label'),
      prescription: this.i18n.t('consultation.ai.field.prescription'),
      labOrders: this.i18n.t('consultation.ai.field.labOrders'),
      vitals: this.i18n.t('consultation.ai.field.vitals'),
    };
    return labels[field];
  }

  private vitalLabel(key: string): string {
    return this.i18n.t(`consultation.ai.vital.${key}`, key);
  }

  private vitalValue(key: string, value: unknown): string {
    const unit = this.i18n.t(`consultation.ai.vitalUnit.${key}`, '');
    return `${String(value)}${unit ? ` ${unit}` : ''}`;
  }
}
