import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';

@Component({
  selector: 'app-ai-assistant-input',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
      <button
        type="button"
        (click)="toggleRecording.emit()"
        [disabled]="busy || !mediaRecorderSupported || blocked"
        class="inline-flex min-h-12 flex-1 items-center justify-center gap-2 rounded-[6px] px-4 py-3 text-sm font-bold text-white disabled:cursor-not-allowed disabled:opacity-50"
        [ngClass]="recording ? 'bg-rose-600 hover:bg-rose-700' : 'bg-[var(--brand-primary)] hover:bg-[var(--brand-primary-hover)]'"
      >
        @if (recording) {
          <span class="h-3 w-3 animate-pulse rounded-[2px] bg-white"></span>
          {{ i18n.t('consultation.ai.stopRecordingTranscribe', 'Arrêter et préparer la transcription') }}
        } @else {
          {{ i18n.t('consultation.ai.record', 'Démarrer la dictée') }}
        }
      </button>
      <button
        type="button"
        (click)="endSession.emit()"
        [disabled]="busy || recording"
        class="inline-flex min-h-12 items-center justify-center rounded-[6px] border border-[var(--app-border)] bg-[var(--app-surface)] px-4 py-3 text-xs font-semibold text-[var(--text-secondary)] hover:bg-[var(--app-surface-muted)] disabled:opacity-50"
      >
        {{ i18n.t('consultation.ai.reset', 'Terminer la session') }}
      </button>
    </div>

    @if (!blocked) {
      <div class="mt-4 space-y-2 rounded-[6px] border border-[var(--app-border)] bg-[var(--app-surface)] p-3">
        <label class="ui-label text-xs font-semibold">
          {{ i18n.t('consultation.ai.textFallback', 'Votre message ou correction') }}
        </label>
        <textarea
          rows="3"
          [value]="message()"
          (input)="onInput($event)"
          [placeholder]="i18n.t('consultation.ai.textPlaceholder', 'Ex : Corrige, la douleur est à droite et non à gauche…')"
          class="ui-textarea w-full resize-y rounded-[4px] border-[var(--app-border)] bg-transparent p-2.5 text-sm text-[var(--text-primary)]"
        ></textarea>
        <button
          type="button"
          (click)="submit()"
          [disabled]="busy || !message().trim()"
          class="inline-flex items-center justify-center rounded-[4px] bg-[var(--brand-primary)] px-3 py-2 text-xs font-semibold text-white hover:bg-[var(--brand-primary-hover)] disabled:opacity-50"
        >
          {{ busy ? i18n.t('consultation.ai.processing', 'Traitement…') : i18n.t('consultation.ai.sendText', 'Envoyer à l’assistant') }}
        </button>
      </div>
    }
  `,
})
export class AiAssistantInputComponent {
  readonly i18n = inject(I18nService);

  @Input() busy = false;
  @Input() recording = false;
  @Input() mediaRecorderSupported = false;
  @Input() blocked = false;
  @Output() readonly toggleRecording = new EventEmitter<void>();
  @Output() readonly endSession = new EventEmitter<void>();
  @Output() readonly sendText = new EventEmitter<string>();

  readonly message = signal('');

  onInput(event: Event): void {
    this.message.set((event.target as HTMLTextAreaElement).value);
  }

  submit(): void {
    const value = this.message().trim();
    if (this.busy || this.blocked || !value) return;
    this.sendText.emit(value);
    this.message.set('');
  }
}
