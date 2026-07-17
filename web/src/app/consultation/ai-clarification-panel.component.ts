import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';
import { AiClarification, AiField } from './ai-consultation-api.service';

export interface AiClarificationAnswer {
  clarificationId: string;
  answer: string;
}

@Component({
  selector: 'app-ai-clarification-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (pendingClarification(); as clarification) {
      <section class="space-y-3 rounded-[6px] border border-amber-300 bg-amber-50/70 p-4 dark:border-amber-800 dark:bg-amber-950/20">
        <div class="flex flex-col gap-1 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <p class="text-[10px] font-bold uppercase tracking-wider text-amber-700 dark:text-amber-300">
              {{ i18n.t('consultation.ai.clarificationTitle', 'Précision demandée') }}
            </p>
            <p class="mt-1 text-sm font-semibold text-amber-950 dark:text-amber-100">
              {{ clarification.question }}
            </p>
          </div>
          <span class="w-fit rounded-[3px] border border-amber-300 px-2 py-1 text-[10px] font-semibold text-amber-800 dark:border-amber-700 dark:text-amber-200">
            {{ fieldLabel(clarification.field) }}
          </span>
        </div>

        @if (clarification.options.length > 0) {
          <div class="flex flex-wrap gap-2">
            @for (option of clarification.options; track option) {
              <button
                type="button"
                (click)="selectOption(option)"
                class="rounded-[4px] border border-amber-300 bg-[var(--app-surface)] px-3 py-1.5 text-xs font-semibold text-[var(--text-primary)] hover:bg-amber-100 dark:border-amber-700 dark:hover:bg-amber-950/50"
              >
                {{ option }}
              </button>
            }
          </div>
        }

        <textarea
          rows="2"
          [value]="answer()"
          (input)="onAnswerInput($event)"
          [placeholder]="i18n.t('consultation.ai.clarificationPlaceholder', 'Saisissez une réponse précise…')"
          class="ui-textarea w-full resize-y rounded-[4px] border-amber-300 bg-[var(--app-surface)] p-2.5 text-sm text-[var(--text-primary)] dark:border-amber-700"
        ></textarea>
        <button
          type="button"
          (click)="submitAnswer(clarification.id)"
          [disabled]="disabled || !answer().trim()"
          class="inline-flex items-center justify-center rounded-[4px] bg-amber-700 px-4 py-2 text-xs font-bold text-white hover:bg-amber-800 disabled:opacity-50"
        >
          {{ i18n.t('consultation.ai.clarificationSubmit', 'Répondre à cette question') }}
        </button>
      </section>
    }

    @if (resolvedClarifications().length > 0) {
      <details class="rounded-[6px] border border-[var(--app-border)] bg-[var(--app-surface-muted)]/20">
        <summary class="cursor-pointer px-3 py-2.5 text-xs font-semibold text-[var(--text-secondary)]">
          {{ i18n.t('consultation.ai.clarificationHistory', 'Précisions déjà apportées') }}
          ({{ resolvedClarifications().length }})
        </summary>
        <div class="space-y-2 border-t border-[var(--app-border)] p-3">
          @for (clarification of resolvedClarifications(); track clarification.id) {
            <article class="rounded-[4px] border border-[var(--app-border)] bg-[var(--app-surface)] p-3 text-xs">
              <div class="flex items-start justify-between gap-3">
                <p class="font-semibold text-[var(--text-primary)]">{{ clarification.question }}</p>
                <span class="shrink-0 text-[10px] text-[var(--text-muted)]">{{ fieldLabel(clarification.field) }}</span>
              </div>
              <p class="mt-1 text-[var(--text-secondary)]">
                <span class="font-semibold">{{ i18n.t('consultation.ai.clarificationAnswer', 'Réponse') }} :</span>
                {{ clarification.answer }}
              </p>
            </article>
          }
        </div>
      </details>
    }
  `,
})
export class AiClarificationPanelComponent {
  readonly i18n = inject(I18nService);

  @Input() clarifications: readonly AiClarification[] = [];
  @Input() disabled = false;
  @Output() readonly answered = new EventEmitter<AiClarificationAnswer>();

  readonly answer = signal('');

  pendingClarification(): AiClarification | null {
    return [...this.clarifications]
      .reverse()
      .find(clarification => clarification.status === 'PENDING') ?? null;
  }

  resolvedClarifications(): AiClarification[] {
    return this.clarifications
      .filter(clarification => clarification.status === 'RESOLVED')
      .slice(-5)
      .reverse();
  }

  selectOption(option: string): void {
    this.answer.set(option);
  }

  onAnswerInput(event: Event): void {
    this.answer.set((event.target as HTMLTextAreaElement).value);
  }

  submitAnswer(clarificationId: string): void {
    const value = this.answer().trim();
    if (this.disabled || !value) return;
    this.answered.emit({ clarificationId, answer: value });
    this.answer.set('');
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
