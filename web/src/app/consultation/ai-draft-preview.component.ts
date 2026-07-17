import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';
import {
  AiConsultationDraft,
  AiField,
} from './ai-consultation-api.service';

@Component({
  selector: 'app-ai-draft-preview',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (entries().length > 0) {
      <section class="space-y-2">
        <div>
          <p class="text-xs font-bold uppercase tracking-wider text-[var(--text-muted)]">
            {{ i18n.t('consultation.ai.acceptedDraft', 'Brouillon accepté') }}
          </p>
          <p class="mt-1 text-[10px] text-[var(--text-muted)]">
            {{ i18n.t('consultation.ai.acceptedDraftHelp', 'Seules les propositions acceptées apparaissent ici.') }}
          </p>
        </div>
        <div class="grid grid-cols-1 gap-2 sm:grid-cols-2">
          @for (entry of entries(); track entry.key) {
            <div class="rounded-[4px] border border-[var(--app-border)] bg-[var(--app-surface-muted)]/30 p-3">
              <p class="text-[10px] font-bold uppercase tracking-wider text-[var(--text-muted)]">{{ fieldLabel(entry.key) }}</p>
              <p class="mt-1 line-clamp-4 whitespace-pre-wrap text-xs leading-5 text-[var(--text-primary)]">{{ entry.value }}</p>
            </div>
          }
        </div>
        <button
          type="button"
          (click)="apply.emit()"
          [disabled]="!canApply"
          class="inline-flex w-full items-center justify-center rounded-[6px] bg-emerald-600 px-4 py-3 text-sm font-bold text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-50 sm:w-auto"
        >
          {{ i18n.t('consultation.ai.apply', 'Appliquer au formulaire') }}
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
    return (Object.entries(this.draft) as Array<[AiField, string]>)
      .filter(([, value]) => !!value?.trim())
      .map(([key, value]) => ({ key, value }));
  }

  fieldLabel(field: AiField): string {
    const labels: Record<AiField, string> = {
      symptoms: this.i18n.t('consultation.symptoms.label', 'Symptômes'),
      clinicalExam: this.i18n.t('consultation.clinicalExam.label', 'Examen clinique'),
      suspectedDiagnosis: this.i18n.t('consultation.suspectedDiagnosis.label', 'Hypothèse diagnostique'),
      diagnosis: this.i18n.t('consultation.diagnosis.label', 'Diagnostic'),
      finalDiagnosis: this.i18n.t('consultation.finalDiagnosis.label', 'Diagnostic final'),
      conclusion: this.i18n.t('consultation.conclusion.label', 'Conclusion'),
      advice: this.i18n.t('consultation.advice.label', 'Conseils au patient'),
      followUp: this.i18n.t('consultation.followUp.label', 'Suivi recommandé'),
    };
    return labels[field];
  }
}
