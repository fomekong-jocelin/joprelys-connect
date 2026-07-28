import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { IconComponent } from '../shared/ui/icon.component';
import { VoiceWaveVisualizerComponent } from './voice-wave-visualizer.component';

@Component({
  selector: 'app-voice-listening-surface',
  standalone: true,
  imports: [IconComponent, VoiceWaveVisualizerComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section
      class="rounded-[var(--radius-brand-md)] border border-[var(--app-border)] px-3 py-3 sm:px-4"
      [attr.aria-label]="accessibleLabel"
      data-testid="voice-listening-surface"
    >
      <header class="flex items-center justify-between gap-3">
        <div class="inline-flex min-h-8 items-center gap-2 text-xs font-semibold text-[var(--brand-primary)] sm:text-sm">
          <app-ui-icon name="sparkles" class="text-base" aria-hidden="true" />
          <span>{{ badgeText }}</span>
        </div>

        <button
          type="button"
          (click)="stop.emit()"
          [disabled]="stopDisabled"
          class="inline-flex min-h-9 shrink-0 items-center justify-center gap-1.5 rounded-[var(--radius-brand-xs)] border border-[var(--app-border)] px-3 py-1.5 text-xs font-bold text-[var(--text-primary)] transition hover:border-[var(--brand-primary-border)] hover:bg-[var(--app-surface-muted)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--brand-primary)] disabled:cursor-not-allowed disabled:opacity-50"
          [attr.aria-label]="stopText"
          data-testid="voice-listening-stop"
        >
          <app-ui-icon name="stop" class="text-[var(--brand-primary)]" aria-hidden="true" />
          <span>{{ stopText }}</span>
        </button>
      </header>

      <div class="mt-2">
        <app-voice-wave-visualizer
          [active]="active"
          [audioLevel]="audioLevel"
          [statusText]="statusText"
          [accessibleLabel]="accessibleLabel"
        />
      </div>

      @if (tipText) {
        <p class="mt-2 text-[10px] leading-4 text-[var(--text-muted)]">{{ tipText }}</p>
      }
    </section>
  `,
})
export class VoiceListeningSurfaceComponent {
  @Input() active = false;
  @Input() audioLevel = 0;
  @Input() badgeText = '';
  @Input() stopText = '';
  @Input() statusText = '';
  @Input() tipText = '';
  @Input() accessibleLabel = '';
  @Input() stopDisabled = false;
  @Output() readonly stop = new EventEmitter<void>();
}
