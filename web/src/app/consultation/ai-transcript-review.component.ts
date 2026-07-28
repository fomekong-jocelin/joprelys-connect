import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';

@Component({
  selector: 'app-ai-transcript-review',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (sourceTranscript) {
      <section class="border-l-2 border-[var(--brand-warning-border)] py-1 pl-3 sm:pl-4">
        <p class="text-[10px] font-bold uppercase tracking-wide text-[var(--brand-warning-text)]">
          {{ i18n.t('consultation.ai.pendingTranscript') }}
        </p>
        <p class="mt-1 text-xs leading-5 text-[var(--text-secondary)]">
          {{ i18n.t('consultation.ai.pendingTranscriptHelp') }}
        </p>

        <textarea
          rows="4"
          [value]="editableTranscript()"
          (input)="onInput($event)"
          class="ui-textarea mt-2.5 w-full resize-y"
          [attr.aria-label]="i18n.t('consultation.ai.pendingTranscript')"
        ></textarea>

        <div class="mt-2 flex flex-col gap-2 sm:flex-row">
          <button
            type="button"
            (click)="submit()"
            [disabled]="busy || !editableTranscript().trim()"
            class="ui-button ui-button-primary min-h-10 w-full sm:w-auto"
          >
            {{ busy ? i18n.t('consultation.ai.processing') : i18n.t('consultation.ai.confirmAnalyze') }}
          </button>
          <button
            type="button"
            (click)="discard.emit()"
            [disabled]="busy"
            class="ui-button ui-button-secondary min-h-10 w-full sm:w-auto"
          >
            {{ i18n.t('consultation.ai.discardTranscript') }}
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
