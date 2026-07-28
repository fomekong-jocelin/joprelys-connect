import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';
import { VoiceListeningSurfaceComponent } from './voice-listening-surface.component';

@Component({
  selector: 'app-ai-assistant-input',
  standalone: true,
  imports: [CommonModule, VoiceListeningSurfaceComponent],
  template: `
    <div class="ui-card-subtle p-3 sm:p-4">
      @if (showModePicker) {
        <div class="mb-3">
          <p class="ui-label mb-2">
            {{ i18n.t('consultation.ai.voiceModeTitle') }}
          </p>

          <div class="grid grid-cols-1 gap-2 sm:grid-cols-2" role="group"
               [attr.aria-label]="i18n.t('consultation.ai.voiceModeTitle')">
            <button
              type="button"
              (click)="selectConversationMode(true)"
              [disabled]="busy || recording || speaking"
              [attr.aria-pressed]="conversationMode"
              class="flex min-h-14 items-center gap-3 rounded-[var(--radius-brand-sm)] border px-3 py-2.5 text-left transition disabled:cursor-not-allowed disabled:opacity-50"
              [ngClass]="conversationMode
                ? 'border-[var(--brand-primary)] bg-[var(--brand-primary-subtle)] text-[var(--text-primary)]'
                : 'border-[var(--app-border)] bg-[var(--app-surface)] text-[var(--text-primary)] hover:bg-[var(--app-surface-muted)]'"
            >
              <span class="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-[var(--radius-brand-sm)]"
                    [ngClass]="conversationMode
                      ? 'bg-[var(--brand-primary)] text-[var(--text-inverse)]'
                      : 'bg-[var(--app-surface-muted)] text-[var(--text-secondary)]'">
                <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M12 18.75a6 6 0 006-6v-1.5m-12 0v1.5a6 6 0 006 6m0 0v3m-3 0h6M12 15.75a3 3 0 003-3V6a3 3 0 10-6 0v6.75a3 3 0 003 3z" />
                </svg>
              </span>
              <span class="min-w-0">
                <span class="flex items-center gap-2 text-xs font-bold">
                  {{ i18n.t('consultation.ai.realtimeMode') }}
                  @if (conversationMode) {
                    <span class="text-[9px] font-bold uppercase tracking-wider text-[var(--brand-primary)]">
                      {{ i18n.t('consultation.ai.modeActive') }}
                    </span>
                  }
                </span>
                <span class="mt-0.5 block text-[10px] leading-4 text-[var(--text-muted)]">
                  {{ i18n.t('consultation.ai.realtimeModeHelp') }}
                </span>
              </span>
            </button>

            <button
              type="button"
              (click)="selectConversationMode(false)"
              [disabled]="busy || recording || speaking"
              [attr.aria-pressed]="!conversationMode"
              class="flex min-h-14 items-center gap-3 rounded-[var(--radius-brand-sm)] border px-3 py-2.5 text-left transition disabled:cursor-not-allowed disabled:opacity-50"
              [ngClass]="!conversationMode
                ? 'border-[var(--brand-primary)] bg-[var(--brand-primary-subtle)] text-[var(--text-primary)]'
                : 'border-[var(--app-border)] bg-[var(--app-surface)] text-[var(--text-primary)] hover:bg-[var(--app-surface-muted)]'"
            >
              <span class="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-[var(--radius-brand-sm)]"
                    [ngClass]="!conversationMode
                      ? 'bg-[var(--brand-primary)] text-[var(--text-inverse)]'
                      : 'bg-[var(--app-surface-muted)] text-[var(--text-secondary)]'">
                <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M9 12h6m-6 4h6M8 3h8a2 2 0 012 2v14a2 2 0 01-2 2H8a2 2 0 01-2-2V5a2 2 0 012-2z" />
                </svg>
              </span>
              <span class="min-w-0">
                <span class="flex items-center gap-2 text-xs font-bold">
                  {{ i18n.t('consultation.ai.dictationMode') }}
                  @if (!conversationMode) {
                    <span class="text-[9px] font-bold uppercase tracking-wider text-[var(--brand-primary)]">
                      {{ i18n.t('consultation.ai.modeActive') }}
                    </span>
                  }
                </span>
                <span class="mt-0.5 block text-[10px] leading-4 text-[var(--text-muted)]">
                  {{ i18n.t('consultation.ai.dictationModeHelp') }}
                </span>
              </span>
            </button>
          </div>
        </div>
      }

      @if (!conversationMode && recording) {
        <app-voice-listening-surface
          [active]="true"
          [audioLevel]="audioLevel"
          [badgeText]="i18n.t('consultation.ai.aiInProgress', 'IA en cours...')"
          [stopText]="i18n.t('consultation.ai.stopListening', 'Arrêter')"
          [statusText]="i18n.t('consultation.ai.listenNaturally', 'Écoute en cours... Parlez naturellement')"
          [tipText]="i18n.t('consultation.ai.tipDictateNaturally', 'Conseil : Vous pouvez dicter vos notes de consultation de façon naturelle.')"
          [accessibleLabel]="i18n.t('consultation.ai.microphoneLevelLabel', 'Niveau réel du microphone')"
          (stop)="toggleRecording.emit()"
        />
      }

      @if (!conversationMode && !recording) {
        <div class="flex flex-col gap-2 sm:flex-row sm:items-center">
          <button
            type="button"
            (click)="toggleRecording.emit()"
            [disabled]="busy || speaking || !mediaRecorderSupported || blocked"
            class="ui-button ui-button-primary min-h-11 flex-1"
          >
            @if (recording) {
              <span class="h-3 w-3 rounded-[2px] bg-[var(--text-inverse)]" aria-hidden="true"></span>
              {{ i18n.t('consultation.ai.stopRecordingTranscribe') }}
            } @else {
              <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 18.75a6 6 0 006-6v-1.5m-12 0v1.5a6 6 0 006 6m0 0v3m-3 0h6M12 15.75a3 3 0 003-3V6a3 3 0 10-6 0v6.75a3 3 0 003 3z" />
              </svg>
              {{ i18n.t('consultation.ai.record') }}
            }
          </button>

          @if (showEndSession) {
            <button
              type="button"
              (click)="endSession.emit()"
              [disabled]="busy || recording || speaking"
              class="ui-button ui-button-secondary min-h-11 w-full sm:w-auto"
            >
              {{ i18n.t('consultation.ai.reset') }}
            </button>
          }
        </div>
      }
    </div>

    @if (showTextFallback && !blocked && !conversationMode) {
      <div class="ui-card-subtle mt-4 space-y-2 p-3">
        <label class="ui-label">
          {{ i18n.t('consultation.ai.textFallback') }}
        </label>
        <textarea
          rows="3"
          [value]="message()"
          (input)="onInput($event)"
          [placeholder]="i18n.t('consultation.ai.textPlaceholder')"
          class="ui-textarea resize-y"
        ></textarea>
        <button
          type="button"
          (click)="submit()"
          [disabled]="busy || !message().trim()"
          class="ui-button ui-button-primary w-full sm:w-auto"
        >
          {{ busy ? i18n.t('consultation.ai.processing') : i18n.t('consultation.ai.sendText') }}
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
  private lastResetToken = 0;

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
