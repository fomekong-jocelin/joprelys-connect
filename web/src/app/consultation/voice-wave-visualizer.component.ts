import { CommonModule } from '@angular/common';
import { Component, Input, inject } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';

@Component({
  selector: 'app-voice-wave-visualizer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex flex-col items-center justify-center space-y-4 py-2">
      <!-- Grand micro circulaire central avec auras de vagues pulsatiles (MediVoice Style) -->
      <div class="relative flex items-center justify-center">
        @if (active) {
          <span class="absolute inline-flex h-24 w-24 animate-ping rounded-full bg-[var(--brand-primary)] opacity-20"></span>
          <span class="absolute inline-flex h-20 w-20 animate-pulse rounded-full bg-[var(--brand-primary)] opacity-35"></span>
        }
        <div
          class="relative flex h-16 w-16 items-center justify-center rounded-full transition-all duration-300 shadow-md"
          [ngClass]="active
            ? 'bg-gradient-to-tr from-[var(--brand-primary)] to-[#00b4d8] text-[var(--text-inverse)] scale-105'
            : 'bg-[var(--app-surface-muted)] text-[var(--text-muted)] border border-[var(--app-border)]'"
        >
          <svg class="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M12 18.75a6 6 0 006-6v-1.5m-12 0v1.5a6 6 0 006 6m0 0v3m-3 0h6M12 15.75a3 3 0 10-6 0v6.75a3 3 0 10-6 0" />
          </svg>
        </div>
      </div>

      <!-- Ondes sonores horizontales fluides (MediVoice waveform) -->
      <div class="flex h-12 w-full max-w-md items-center justify-center gap-1 px-4" aria-label="Visualiseur d'ondes sonores">
        @for (wave of waves; track $index) {
          <span
            class="w-1 rounded-full transition-all duration-100 ease-out"
            [ngClass]="active ? 'bg-gradient-to-b from-[var(--brand-primary)] to-[#00b4d8]' : 'bg-[var(--app-border)]'"
            [style.height.px]="waveHeight($index)"
            [style.opacity]="waveOpacity($index)"
          ></span>
        }
      </div>

      <!-- Indicateur de niveau sonore -->
      <div class="flex items-center gap-2 text-xs font-semibold text-[var(--text-muted)]">
        <svg class="h-3.5 w-3.5 text-[var(--brand-primary)]" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z" />
        </svg>
        <span>{{ active ? i18n.t('consultation.ai.optimalLevel', 'Niveau optimal · Écoute active') : i18n.t('consultation.ai.listeningPaused', 'Écoute en pause') }}</span>
      </div>
    </div>
  `,
})
export class VoiceWaveVisualizerComponent {
  readonly i18n = inject(I18nService);

  @Input() active = false;
  @Input() audioLevel = 0;

  readonly waves = Array.from({ length: 32 });

  waveHeight(index: number): number {
    if (!this.active) return 6;
    const level = Math.min(1, Math.max(0.08, this.audioLevel));
    // Calcul de courbe sinusoïdale fluide inspiré de MediVoice
    const centerFactor = 1 - Math.abs(index - 15.5) / 16;
    const waveSin = Math.abs(Math.sin((index + 1) * 0.45 + Date.now() * 0.008));
    const dynamicHeight = 6 + (level * 30 + waveSin * 14) * centerFactor;
    return Math.round(Math.min(42, Math.max(4, dynamicHeight)));
  }

  waveOpacity(index: number): number {
    if (!this.active) return 0.25;
    const centerFactor = 0.4 + (1 - Math.abs(index - 15.5) / 16) * 0.6;
    return Math.min(1, Math.max(0.4, centerFactor));
  }
}
