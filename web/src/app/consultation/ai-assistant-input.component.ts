import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';

@Component({
  selector: 'app-ai-assistant-input',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="rounded-[6px] border border-[var(--app-border)] bg-[var(--app-surface)] p-3 shadow-sm">
      @if (showModePicker) {
        <div class="mb-3">
          <p class="mb-2 text-[10px] font-extrabold uppercase tracking-wider text-[var(--text-muted)]">
            {{ i18n.t('consultation.ai.voiceModeTitle', 'Mode audio') }}
          </p>

          <div class="grid grid-cols-1 gap-2 sm:grid-cols-2" role="group"
               [attr.aria-label]="i18n.t('consultation.ai.voiceModeTitle', 'Mode audio')">
            <button
              type="button"
              (click)="selectConversationMode(true)"
              [disabled]="busy || recording || speaking"
              [attr.aria-pressed]="conversationMode"
              class="flex min-h-14 items-center gap-3 rounded-[4px] border px-3 py-2.5 text-left transition disabled:cursor-not-allowed disabled:opacity-50"
              [ngClass]="conversationMode
                ? 'border-[var(--brand-primary)] bg-cyan-50/70 text-[var(--text-primary)] shadow-sm dark:bg-cyan-950/20'
                : 'border-[var(--app-border)] bg-[var(--app-surface)] text-[var(--text-primary)] hover:border-cyan-300 hover:bg-[var(--app-surface-muted)]'"
            >
              <span class="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-[4px]"
                    [ngClass]="conversationMode ? 'bg-[var(--brand-primary)] text-white' : 'bg-[var(--app-surface-muted)] text-[var(--text-secondary)]'">
                <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M12 18.75a6 6 0 006-6v-1.5m-12 0v1.5a6 6 0 006 6m0 0v3m-3 0h6M12 15.75a3 3 0 003-3V6a3 3 0 10-6 0v6.75a3 3 0 003 3z" />
                </svg>
              </span>
              <span class="min-w-0">
                <span class="flex items-center gap-2 text-xs font-extrabold">
                  {{ i18n.t('consultation.ai.realtimeMode', 'Temps réel') }}
                  @if (conversationMode) {
                    <span class="text-[9px] font-bold uppercase tracking-wider text-cyan-700 dark:text-cyan-300">
                      {{ i18n.t('consultation.ai.modeActive', 'Actif') }}
                    </span>
                  }
                </span>
                <span class="mt-0.5 block text-[10px] leading-4 text-[var(--text-muted)]">
                  {{ i18n.t('consultation.ai.realtimeModeHelp', 'Conversation continue avec Joprelys') }}
                </span>
              </span>
            </button>

            <button
              type="button"
              (click)="selectConversationMode(false)"
              [disabled]="busy || recording || speaking"
              [attr.aria-pressed]="!conversationMode"
              class="flex min-h-14 items-center gap-3 rounded-[4px] border px-3 py-2.5 text-left transition disabled:cursor-not-allowed disabled:opacity-50"
              [ngClass]="!conversationMode
                ? 'border-[var(--brand-primary)] bg-cyan-50/70 text-[var(--text-primary)] shadow-sm dark:bg-cyan-950/20'
                : 'border-[var(--app-border)] bg-[var(--app-surface)] text-[var(--text-primary)] hover:border-cyan-300 hover:bg-[var(--app-surface-muted)]'"
            >
              <span class="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-[4px]"
                    [ngClass]="!conversationMode ? 'bg-[var(--brand-primary)] text-white' : 'bg-[var(--app-surface-muted)] text-[var(--text-secondary)]'">
                <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M9 12h6m-6 4h6M8 3h8a2 2 0 012 2v14a2 2 0 01-2 2H8a2 2 0 01-2-2V5a2 2 0 012-2z" />
                </svg>
              </span>
              <span class="min-w-0">
                <span class="flex items-center gap-2 text-xs font-extrabold">
                  {{ i18n.t('consultation.ai.dictationMode', 'Dictée') }}
                  @if (!conversationMode) {
                    <span class="text-[9px] font-bold uppercase tracking-wider text-cyan-700 dark:text-cyan-300">
                      {{ i18n.t('consultation.ai.modeActive', 'Actif') }}
                    </span>
                  }
                </span>
                <span class="mt-0.5 block text-[10px] leading-4 text-[var(--text-muted)]">
                  {{ i18n.t('consultation.ai.dictationModeHelp', 'Un enregistrement ponctuel à relire') }}
                </span>
              </span>
            </button>
          </div>
        </div>
      }

      @if (!conversationMode && recording) {
        <div class="mb-3 rounded-[6px] border border-cyan-200 bg-cyan-50/70 p-3 shadow-sm dark:border-cyan-900 dark:bg-cyan-950/20">
          <div class="flex items-center justify-between gap-2">
            <div class="flex items-center gap-2">
              <span class="relative flex h-3 w-3 items-center justify-center">
                <span class="absolute inline-flex h-full w-full animate-ping rounded-full bg-rose-400 opacity-75"></span>
                <span class="relative inline-flex h-2.5 w-2.5 rounded-full bg-rose-500"></span>
              </span>
              <span class="text-sm font-extrabold text-[var(--text-primary)]">
                {{ i18n.t('consultation.ai.listening', 'Je vous écoute…') }}
              </span>
            </div>
            <span class="text-[11px] font-bold text-rose-700 dark:text-rose-300">
              {{ i18n.t('consultation.ai.recordingActive', 'Enregistrement en cours') }}
            </span>
          </div>

          <!-- Sound waves visualizer bar -->
          <div class="mt-2.5 flex h-9 items-center justify-center gap-1.5 rounded-[4px] border border-rose-200/60 bg-slate-900/90 px-3 shadow-inner dark:border-rose-900/60 dark:bg-slate-950"
               aria-label="Visualisateur d'ondes vocales">
            @for (bar of waveformBars; track $index) {
              <span
                class="w-1.5 rounded-full bg-gradient-to-t from-rose-500 to-amber-300 shadow-[0_0_6px_rgba(244,63,94,0.5)] transition-all duration-100"
                [style.height.px]="waveBarHeight($index)"
              ></span>
            }
          </div>
        </div>
      }

      @if (!conversationMode) {
        <div class="flex flex-col gap-2 sm:flex-row sm:items-center">
          <button
            type="button"
            (click)="toggleRecording.emit()"
            [disabled]="busy || speaking || !mediaRecorderSupported || blocked"
            class="inline-flex min-h-12 flex-1 items-center justify-center gap-2 rounded-[4px] px-4 py-3 text-sm font-bold text-white disabled:cursor-not-allowed disabled:opacity-50"
            [ngClass]="recording ? 'bg-rose-600 hover:bg-rose-700' : 'bg-[var(--brand-primary)] hover:bg-[var(--brand-primary-hover)]'"
          >
            @if (recording) {
              <span class="h-3 w-3 animate-pulse rounded-[2px] bg-white"></span>
              {{ i18n.t('consultation.ai.stopRecordingTranscribe', 'Arrêter et transcrire') }}
            } @else {
              <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 18.75a6 6 0 006-6v-1.5m-12 0v1.5a6 6 0 006 6m0 0v3m-3 0h6M12 15.75a3 3 0 003-3V6a3 3 0 10-6 0v6.75a3 3 0 003 3z" />
              </svg>
              {{ i18n.t('consultation.ai.record', 'Démarrer la dictée') }}
            }
          </button>

          @if (showEndSession) {
            <button
              type="button"
              (click)="endSession.emit()"
              [disabled]="busy || recording || speaking"
              class="inline-flex min-h-12 items-center justify-center rounded-[4px] border border-[var(--app-border)] bg-[var(--app-surface)] px-4 py-3 text-xs font-semibold text-[var(--text-secondary)] hover:bg-[var(--app-surface-muted)] disabled:opacity-50"
            >
              {{ i18n.t('consultation.ai.reset', 'Terminer la session') }}
            </button>
          }
        </div>
      }
    </div>

    @if (showTextFallback && !blocked && !conversationMode) {
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
  @Input() showModePicker = true;
  @Input() showTextFallback = true;
  @Input() showEndSession = true;
  @Output() readonly toggleRecording = new EventEmitter<void>();
  @Output() readonly toggleConversationMode = new EventEmitter<void>();
  @Output() readonly endSession = new EventEmitter<void>();
  @Output() readonly sendText = new EventEmitter<string>();

  readonly message = signal('');
  readonly waveformBars = Array.from({ length: 16 });
  private lastResetToken = 0;

  waveBarHeight(index: number): number {
    if (!this.recording) return 6;
    const level = Math.max(0.25, this.audioLevel || 0.35);
    const sinFactor = 0.35 + Math.abs(Math.sin((index + 1) * 0.85 + (index % 4) * 0.95)) * 0.65;
    return Math.round(6 + level * sinFactor * 26);
  }

  @Input()
  set resetToken(value: number) {
    if (value !== this.lastResetToken) {
      this.lastResetToken = value;
      this.message.set('');
    }
  }

  selectConversationMode(enabled: boolean): void {
    if (this.busy || this.recording || this.speaking || this.conversationMode === enabled) return;
    this.toggleConversationMode.emit();
  }

  onInput(event: Event): void {
    this.message.set((event.target as HTMLTextAreaElement).value);
  }

  submit(): void {
    const value = this.message().trim();
    if (this.busy || this.blocked || !value) return;
    this.sendText.emit(value);
  }
}
