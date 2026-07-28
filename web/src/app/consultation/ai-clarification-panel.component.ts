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
      <section class="border-l-2 border-amber-400 py-1 pl-3 sm:pl-4">
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0">
            <p class="text-[10px] font-bold uppercase tracking-wider text-amber-700 dark:text-amber-300">
              {{ i18n.t('consultation.ai.clarificationTitle', 'Précision demandée') }}
            </p>
            <p class="mt-1 text-sm font-semibold leading-5 text-[var(--text-primary)]">
              {{ clarification.question }}
            </p>
          </div>
          <span class="shrink-0 text-[10px] font-semibold text-[var(--text-muted)]">
            {{ fieldLabel(clarification.field) }}
          </span>
        </div>

        @if (clarification.options.length > 0) {
          <div class="mt-3 flex flex-wrap gap-2">
            @for (option of clarification.options; track option) {
              <button
                type="button"
                (click)="selectOption(option)"
                class="rounded-[var(--radius-brand-xs)] border border-[var(--app-border)] px-3 py-1.5 text-xs font-semibold text-[var(--text-primary)] transition hover:border-[var(--brand-primary-border)] hover:bg-[var(--app-surface-muted)]"
              >
                {{ option }}
              </button>
            }
          </div>
        }

        <div class="mt-3 flex flex-col gap-2 sm:flex-row sm:items-end">
          <textarea
            rows="2"
            [value]="answer()"
            (input)="onAnswerInput($event)"
            [placeholder]="i18n.t('consultation.ai.clarificationPlaceholder', 'Saisissez une réponse précise…')"
            class="ui-textarea min-h-20 flex-1 resize-y"
          ></textarea>
          <button
            type="button"
            (click)="submitAnswer(clarification.id)"
            [disabled]="disabled || !answer().trim()"
            class="ui-button ui-button-primary min-h-11 w-full sm:w-auto"
          >
            {{ i18n.t('consultation.ai.clarificationSubmit', 'Répondre') }}
          </button>
        </div>

        <p class="mt-2 text-[10px] leading-4 text-[var(--text-muted)]">
          {{ i18n.t('consultation.ai.clarificationVoiceHelp', 'En conversation, vous pouvez aussi répondre simplement à voix haute.') }}
        </p>
      </section>
    }

    @if (resolvedClarifications().length > 0) {
      <details class="mt-3 border-t border-[var(--app-border)] pt-1">
        <summary class="cursor-pointer py-2 text-xs font-medium text-[var(--text-muted)] hover:text-[var(--text-primary)]">
          {{ i18n.t('consultation.ai.clarificationHistory', 'Précisions déjà apportées') }}
          ({{ resolvedClarifications().length }})
        </summary>
        <div class="divide-y divide-[var(--app-border)]">
          @for (clarification of resolvedClarifications(); track clarification.id) {
            <article class="py-2.5 text-xs">
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
    return (
      [...this.clarifications]
        .reverse()
        .find((clarification) => clarification.status === 'PENDING') ?? null
    );
  }

  resolvedClarifications(): AiClarification[] {
    return this.clarifications
      .filter((clarification) => clarification.status === 'RESOLVED')
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
      suspectedDiagnosis: this.i18n.t(
        'consultation.suspectedDiagnosis.label',
        'Hypothèse diagnostique',
      ),
      diagnosis: this.i18n.t('consultation.diagnosis.label', 'Diagnostic'),
      finalDiagnosis: this.i18n.t('consultation.finalDiagnosis.label', 'Diagnostic final'),
      conclusion: this.i18n.t('consultation.conclusion.label', 'Conclusion'),
      advice: this.i18n.t('consultation.advice.label', 'Conseils au patient'),
      followUp: this.i18n.t('consultation.followUp.label', 'Suivi recommandé'),
      prescription: this.i18n.t('consultation.ai.field.prescription', 'Ordonnance'),
      labOrders: this.i18n.t('consultation.ai.field.labOrders', 'Examens biologiques'),
      vitals: this.i18n.t('consultation.ai.field.vitals', 'Constantes vitales'),
    };
    return labels[field];
  }
}
