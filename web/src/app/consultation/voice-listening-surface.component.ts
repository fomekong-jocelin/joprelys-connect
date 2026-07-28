import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { IconComponent } from '../shared/ui/icon.component';
import { VoiceWaveVisualizerComponent } from './voice-wave-visualizer.component';

@Component({
  selector: 'app-voice-listening-surface',
  standalone: true,
  imports: [IconComponent, VoiceWaveVisualizerComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="space-y-2.5" data-testid="voice-listening-surface">
      <section
        class="overflow-hidden rounded-[var(--radius-brand-lg)] border border-[var(--app-border)] bg-[var(--app-surface)] shadow-[var(--shadow-panel)]"
        [attr.aria-label]="accessibleLabel"
      >
        <header class="flex items-center justify-between gap-3 px-3 py-3 sm:px-5 sm:py-4">
          <div class="inline-flex min-h-9 items-center gap-2 rounded-[var(--radius-brand-md)] bg-[var(--brand-primary-subtle)] px-3 py-1.5 text-xs font-semibold text-[var(--brand-primary)] sm:text-sm">
            <app-ui-icon name="sparkles" class="text-base" aria-hidden="true" />
            <span>{{ badgeText }}</span>
          </div>

          <button
            type="button"
            (click)="stop.emit()"
            [disabled]="stopDisabled"
            class="inline-flex min-h-11 shrink-0 items-center justify-center gap-2 rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface)] px-3.5 py-2 text-xs font-bold text-[var(--text-primary)] shadow-[var(--shadow-panel-subtle)] transition hover:border-[var(--brand-primary-border)] hover:bg-[var(--app-surface-muted)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--brand-primary)] active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50 sm:text-sm"
            [attr.aria-label]="stopText"
            data-testid="voice-listening-stop"
          >
            <app-ui-icon name="stop" class="text-[var(--brand-primary)]" aria-hidden="true" />
            <span>{{ stopText }}</span>
          </button>
        </header>

        <div class="px-3 pb-4 sm:px-6 sm:pb-5">
          <app-voice-wave-visualizer
            [active]="active"
            [audioLevel]="audioLevel"
            [statusText]="statusText"
            [accessibleLabel]="accessibleLabel"
          />
        </div>
      </section>

      <div class="flex min-h-11 items-center gap-2 rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] px-3 py-2 text-xs text-[var(--text-secondary)] sm:px-4 sm:text-sm">
        <app-ui-icon name="light-bulb" class="shrink-0 text-base text-[var(--text-muted)]" aria-hidden="true" />
        <p class="min-w-0 leading-5">{{ tipText }}</p>
      </div>
    </div>
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
