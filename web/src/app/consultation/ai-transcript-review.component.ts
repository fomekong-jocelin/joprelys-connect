import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';

@Component({
  selector: 'app-ai-transcript-review',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (sourceTranscript) {
      <section class="space-y-3 rounded-[6px] border border-amber-200 bg-amber-50/70 p-4 dark:border-amber-900 dark:bg-amber-950/20">
        <div>
          <p class="text-xs font-bold uppercase tracking-wider text-amber-800 dark:text-amber-300">
            {{ i18n.t('consultation.ai.pendingTranscript', 'Transcription à relire') }}
          </p>
          <p class="mt-1 text-xs leading-5 text-amber-700 dark:text-amber-400">
            {{ i18n.t('consultation.ai.pendingTranscriptHelp', 'Corrigez les noms, nombres, doses, négations et côtés avant de lancer l’analyse.') }}
          </p>
        </div>
        <textarea
          rows="6"
          [value]="editableTranscript()"
          (input)="onInput($event)"
          class="ui-textarea w-full resize-y rounded-[4px] border-amber-300 bg-[var(--app-surface)] p-3 text-sm text-[var(--text-primary)] focus:border-[var(--brand-primary)] focus:outline-none"
        ></textarea>
        <div class="flex flex-col gap-2 sm:flex-row">
          <button
            type="button"
            (click)="submit()"
            [disabled]="busy || !editableTranscript().trim()"
            class="inline-flex items-center justify-center rounded-[4px] bg-[var(--brand-primary)] px-4 py-2.5 text-sm font-semibold text-white hover:bg-[var(--brand-primary-hover)] disabled:opacity-50"
          >
            {{ busy ? i18n.t('consultation.ai.processing', 'Traitement…') : i18n.t('consultation.ai.confirmAnalyze', 'Confirmer et analyser') }}
          </button>
          <button
            type="button"
            (click)="discard.emit()"
            [disabled]="busy"
            class="inline-flex items-center justify-center rounded-[4px] border border-[var(--app-border)] bg-[var(--app-surface)] px-4 py-2.5 text-sm font-semibold text-[var(--text-secondary)] hover:bg-[var(--app-surface-muted)] disabled:opacity-50"
          >
            {{ i18n.t('consultation.ai.discardTranscript', 'Abandonner cette transcription') }}
          </button>
        </div>
      </section>
    }
  `,
})
export class AiTranscriptReviewComponent {
  readonly i18n = inject(I18nService);

  @Input() busy = false;
  @Output() readonly analyze = new EventEmitter<string>();
  @Output() readonly discard = new EventEmitter<void>();

  readonly editableTranscript = signal('');
  sourceTranscript = '';

  @Input()
  set transcript(value: string | null | undefined) {
    const normalized = value?.trim() ?? '';
    if (normalized !== this.sourceTranscript) {
      this.sourceTranscript = normalized;
      this.editableTranscript.set(normalized);
    }
  }

  onInput(event: Event): void {
    this.editableTranscript.set((event.target as HTMLTextAreaElement).value);
  }

  submit(): void {
    const value = this.editableTranscript().trim();
    if (this.busy || !value) return;
    this.analyze.emit(value);
  }
}
