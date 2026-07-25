import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';

@Component({
  selector: 'app-ai-assistant-input',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="rounded-[8px] border border-[var(--app-border)] bg-[var(--app-surface-muted)]/35 p-3">
      <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
        <div>
          <p class="text-xs font-bold text-[var(--text-primary)]">
            {{ conversationMode
              ? i18n.t('consultation.ai.modeConversation', 'Conversation vocale')
              : i18n.t('consultation.ai.modeControlled', 'Dictée contrôlée') }}
          </p>
          <p class="text-[10px] text-[var(--text-muted)]">
            {{ conversationMode
              ? i18n.t('consultation.ai.modeConversationHelp', 'L’assistant parle, écoute votre réponse et poursuit la consultation.')
              : i18n.t('consultation.ai.modeControlledHelp', 'Relisez la transcription avant de demander son analyse.') }}
          </p>
        </div>
        <button
          type="button"
          (click)="toggleConversationMode.emit()"
          [disabled]="busy || recording || speaking"
          class="rounded-[999px] border border-[var(--app-border)] bg-[var(--app-surface)] px-3 py-1.5 text-[10px] font-bold text-[var(--text-secondary)] hover:bg-[var(--app-surface-muted)] disabled:opacity-50"
        >
          {{ conversationMode
            ? i18n.t('consultation.ai.backToDictation', 'Revenir à la dictée')
            : i18n.t('consultation.ai.enableConversation', 'Activer le mode conversation') }}
        </button>
      </div>

      @if (recording || speaking) {
        <div class="mb-3 overflow-hidden rounded-[8px] border border-cyan-200 bg-cyan-50/70 px-3 py-3 dark:border-cyan-900 dark:bg-cyan-950/20">
          <div class="flex items-center justify-between gap-3">
            <div class="flex items-center gap-2">
              <span
                class="h-2.5 w-2.5 rounded-full"
                [ngClass]="recording ? 'animate-pulse bg-rose-500' : 'animate-pulse bg-cyan-500'"
              ></span>
              <span class="text-xs font-bold text-[var(--text-primary)]">
                {{ recording
                  ? i18n.t('consultation.ai.listening', 'Je vous écoute…')
                  : i18n.t('consultation.ai.speaking', 'Joprelys vous répond…') }}
              </span>
            </div>
            <span class="text-[10px] font-semibold uppercase tracking-wider text-[var(--text-muted)]">
              {{ recording
                ? i18n.t('consultation.ai.micActive', 'micro actif')
                : i18n.t('consultation.ai.aiAudio', 'audio IA') }}
            </span>
          </div>
          <div class="mt-3 flex h-12 items-center justify-center gap-[3px]" aria-hidden="true">
            @for (bar of waveformBars; track $index) {
              <span
                class="w-[3px] rounded-full bg-cyan-600/80 transition-[height] duration-75 dark:bg-cyan-300/80"
                [style.height.px]="barHeight($index)"
              ></span>
            }
          </div>
        </div>
      }

      <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
        <button
          type="button"
          (click)="toggleRecording.emit()"
          [disabled]="busy || speaking || !mediaRecorderSupported || blocked"
          class="inline-flex min-h-12 flex-1 items-center justify-center gap-2 rounded-[6px] px-4 py-3 text-sm font-bold text-white disabled:cursor-not-allowed disabled:opacity-50"
          [ngClass]="recording ? 'bg-rose-600 hover:bg-rose-700' : 'bg-[var(--brand-primary)] hover:bg-[var(--brand-primary-hover)]'"
        >
          @if (recording) {
            <span class="h-3 w-3 animate-pulse rounded-[2px] bg-white"></span>
            {{ conversationMode
              ? i18n.t('consultation.ai.finishAnswer', 'Terminer ma réponse')
              : i18n.t('consultation.ai.stopRecordingTranscribe', 'Arrêter et préparer la transcription') }}
          } @else {
            <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 18.75a6 6 0 006-6v-1.5m-12 0v1.5a6 6 0 006 6m0 0v3m-3 0h6M12 15.75a3 3 0 003-3V6a3 3 0 10-6 0v6.75a3 3 0 003 3z" />
            </svg>
            {{ conversationMode
              ? i18n.t('consultation.ai.talkToJoprelys', 'Parler à Joprelys')
              : i18n.t('consultation.ai.record', 'Démarrer la dictée') }}
          }
        </button>
        <button
          type="button"
          (click)="endSession.emit()"
          [disabled]="busy || recording || speaking"
          class="inline-flex min-h-12 items-center justify-center rounded-[6px] border border-[var(--app-border)] bg-[var(--app-surface)] px-4 py-3 text-xs font-semibold text-[var(--text-secondary)] hover:bg-[var(--app-surface-muted)] disabled:opacity-50"
        >
          {{ i18n.t('consultation.ai.reset', 'Terminer la session') }}
        </button>
      </div>
    </div>

    @if (!blocked && !conversationMode) {
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
  @Input() speaking = false;
  @Input() conversationMode = false;
  @Input() audioLevel = 0;
  @Input() mediaRecorderSupported = false;
  @Input() blocked = false;
  @Output() readonly toggleRecording = new EventEmitter<void>();
  @Output() readonly toggleConversationMode = new EventEmitter<void>();
  @Output() readonly endSession = new EventEmitter<void>();
  @Output() readonly sendText = new EventEmitter<string>();

  readonly message = signal('');
  readonly waveformBars = Array.from({ length: 28 });
  private lastResetToken = 0;

  @Input()
  set resetToken(value: number) {
    if (value !== this.lastResetToken) {
      this.lastResetToken = value;
      this.message.set('');
    }
  }

  onInput(event: Event): void {
    this.message.set((event.target as HTMLTextAreaElement).value);
  }

  barHeight(index: number): number {
    const normalized = Math.max(0, Math.min(1, this.audioLevel));
    const phase = (index % 7) / 6;
    const shape = 0.35 + Math.sin(phase * Math.PI) * 0.65;
    const active = this.recording || this.speaking ? normalized : 0;
    return Math.round(6 + shape * (10 + active * 28));
  }

  submit(): void {
    const value = this.message().trim();
    if (this.busy || this.blocked || !value) return;
    this.sendText.emit(value);
  }
}
