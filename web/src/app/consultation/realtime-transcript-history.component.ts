import { CommonModule } from '@angular/common';
import {
  AfterViewChecked,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  Output,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';

export interface RealtimeTranscriptEntry {
  id: string;
  text: string;
  timestamp: number;
}

@Component({
  selector: 'app-realtime-transcript-history',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section
      class="h-32 max-h-32 overflow-hidden rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface-muted)]"
      aria-live="polite"
    >
      <header class="flex items-center justify-between gap-3 border-b border-[var(--app-border)] px-3 py-2">
        <p class="text-xs font-bold text-[var(--text-primary)]">
          {{ i18n.t('consultation.ai.focusTranscriptTitle') }}
        </p>
        @if (queuedCount > 0 || processing) {
          <span class="inline-flex items-center gap-1.5 text-[10px] font-bold text-[var(--brand-primary)]">
            @if (processing) {
              <span class="h-3 w-3 animate-spin rounded-full border-2 border-[var(--brand-primary-border)] border-t-[var(--brand-primary)]"></span>
            }
            {{ queuedCount }} {{ i18n.t('consultation.ai.queuedShort') }}
          </span>
        }
      </header>

      <div
        #transcriptContainer
        class="h-[calc(8rem-2.25rem)] space-y-1.5 overflow-y-auto overscroll-contain p-2.5 text-left scroll-smooth"
      >
        @if (entries.length > 0) {
          @for (entry of entries; track entry.id; let last = $last) {
            <div class="flex items-start gap-2 text-xs leading-5">
              <span class="shrink-0 text-[10px] tabular-nums text-[var(--text-muted)]">
                {{ formatTime(entry.timestamp) }}
              </span>
              <div class="min-w-0 flex-1">
                <p class="font-medium text-[var(--text-primary)]">{{ entry.text }}</p>

                @if (last && editingId() !== entry.id) {
                  <button
                    type="button"
                    (click)="startCorrection(entry)"
                    [disabled]="correctionDisabled"
                    class="mt-1 min-h-11 text-xs font-bold text-[var(--brand-primary)] underline-offset-2 hover:underline disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {{ i18n.t('consultation.ai.correctTranscript') }}
                  </button>
                }

                @if (last && editingId() === entry.id) {
                  <div class="mt-2 rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface)] p-2">
                    <label class="ui-label" [for]="'realtime-transcript-correction-' + entry.id">
                      {{ i18n.t('consultation.ai.correctTranscriptLabel') }}
                    </label>
                    <textarea
                      [id]="'realtime-transcript-correction-' + entry.id"
                      rows="3"
                      [value]="correctionText()"
                      (input)="onCorrectionInput($event)"
                      class="ui-textarea mt-1.5 w-full resize-y"
                    ></textarea>
                    <div class="mt-2 flex flex-col gap-2 sm:flex-row">
                      <button
                        type="button"
                        (click)="submitCorrection()"
                        [disabled]="!correctionText().trim()"
                        class="ui-button ui-button-primary min-h-11 w-full sm:w-auto"
                      >
                        {{ i18n.t('consultation.ai.submitTranscriptCorrection') }}
                      </button>
                      <button
                        type="button"
                        (click)="cancelCorrection()"
                        class="ui-button ui-button-secondary min-h-11 w-full sm:w-auto"
                      >
                        {{ i18n.t('consultation.ai.cancelTranscriptCorrection') }}
                      </button>
                    </div>
                  </div>
                }
              </div>
            </div>
          }
        } @else {
          <p class="text-xs italic leading-5 text-[var(--text-muted)]">
            {{ connected
              ? i18n.t('consultation.ai.focusTranscriptWaiting')
              : i18n.t('consultation.ai.focusTranscriptNoChannel') }}
          </p>
        }
      </div>
    </section>
  `,
})
export class RealtimeTranscriptHistoryComponent implements AfterViewChecked {
  readonly i18n = inject(I18nService);

  @Input() entries: readonly RealtimeTranscriptEntry[] = [];
  @Input() processing = false;
  @Input() queuedCount = 0;
  @Input() connected = false;
  @Input() correctionDisabled = false;
  @Output() readonly correct = new EventEmitter<string>();

  readonly editingId = signal<string | null>(null);
  readonly correctionText = signal('');
  private renderedEntryCount = 0;

  @ViewChild('transcriptContainer') transcriptContainer?: ElementRef<HTMLDivElement>;

  ngAfterViewChecked(): void {
    if (this.entries.length === this.renderedEntryCount) return;
    this.renderedEntryCount = this.entries.length;
    const container = this.transcriptContainer?.nativeElement;
    if (container) container.scrollTop = container.scrollHeight;
  }

  startCorrection(entry: RealtimeTranscriptEntry): void {
    if (this.correctionDisabled) return;
    this.editingId.set(entry.id);
    this.correctionText.set(entry.text);
  }

  onCorrectionInput(event: Event): void {
    this.correctionText.set((event.target as HTMLTextAreaElement).value);
  }

  submitCorrection(): void {
    const correction = this.correctionText().trim();
    if (!correction) return;
    this.correct.emit(correction);
    this.cancelCorrection();
  }

  cancelCorrection(): void {
    this.editingId.set(null);
    this.correctionText.set('');
  }

  formatTime(timestamp: number): string {
    return new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }
}
