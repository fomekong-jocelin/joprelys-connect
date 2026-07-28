import { CommonModule } from '@angular/common';
import { Component, Input, inject } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';

@Component({
  selector: 'app-voice-wave-visualizer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex flex-col items-center justify-center space-y-3 py-1">
      <!-- Grand micro circulaire central avec auras de vagues pulsatiles (Soft UI Style pasted-image-4.png) -->
      <div class="relative flex items-center justify-center">
        @if (active) {
          <span class="absolute inline-flex h-24 w-24 animate-ping rounded-full bg-[var(--brand-primary)] opacity-15"></span>
          <span class="absolute inline-flex h-20 w-20 animate-pulse rounded-full bg-[var(--brand-primary)] opacity-25"></span>
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

      <!-- Ondes sinusoïdales fluides et douces (Sine Wave Ribbons - pasted-image-4.png) -->
      <div class="flex h-10 w-full max-w-sm items-center justify-center gap-1.5 px-2" aria-label="Visualiseur d'ondes sonores fluides">
        @for (wave of waves; track $index) {
          <span
            class="w-1.5 rounded-full transition-all duration-150 ease-out"
            [ngClass]="active ? 'bg-gradient-to-b from-[#22a8c8] via-[var(--brand-primary)] to-[#00b4d8]' : 'bg-[var(--app-border)]'"
            [style.height.px]="waveHeight($index)"
            [style.opacity]="waveOpacity($index)"
          ></span>
        }
      </div>

      <!-- Message principal centré : Écoute en cours... Parlez naturellement -->
      <div class="text-center">
        <p class="text-sm font-bold text-[var(--brand-primary)] sm:text-base">
          {{ active
            ? i18n.t('consultation.ai.listenNaturally', 'Écoute en cours... Parlez naturellement')
            : i18n.t('consultation.ai.listeningPaused', 'Écoute en pause') }}
        </p>
        <span class="sr-only">
          {{ active ? i18n.t('consultation.ai.optimalLevel', 'Niveau optimal · Écoute active') : i18n.t('consultation.ai.listeningPaused', 'Écoute en pause') }}
        </span>
      </div>
    </div>
  `,
})
export class VoiceWaveVisualizerComponent {
  readonly i18n = inject(I18nService);

  @Input() active = false;
  @Input() audioLevel = 0;

  readonly waves = Array.from({ length: 28 });

  waveHeight(index: number): number {
    if (!this.active) return 5;
    const level = Math.min(1, Math.max(0.12, this.audioLevel));
    // Calcul de courbe sinusoïdale fluide identique au modèle soft
    const centerFactor = 1 - Math.abs(index - 13.5) / 14;
    const waveSin = Math.abs(Math.sin((index + 1) * 0.45 + Date.now() * 0.007));
    const dynamicHeight = 5 + (level * 24 + waveSin * 12) * centerFactor;
    return Math.round(Math.min(36, Math.max(4, dynamicHeight)));
  }

  waveOpacity(index: number): number {
    if (!this.active) return 0.2;
    const centerFactor = 0.5 + (1 - Math.abs(index - 13.5) / 14) * 0.5;
    return Math.min(1, Math.max(0.35, centerFactor));
  }
}
