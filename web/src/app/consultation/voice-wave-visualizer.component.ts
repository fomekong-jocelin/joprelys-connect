import { CommonModule } from '@angular/common';
import { Component, Input, inject } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';

@Component({
  selector: 'app-voice-wave-visualizer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div
      class="relative flex h-14 w-full items-center justify-between overflow-hidden rounded-[var(--radius-brand-md)] border border-[var(--brand-primary-border)] bg-gradient-to-r from-[var(--brand-primary-subtle)] via-[var(--app-surface)] to-[var(--brand-primary-subtle)] px-4 py-2 shadow-xs transition-all duration-300"
      [class.ring-2]="active"
      [class.ring-[var(--brand-primary-border)]]="active"
    >
      <!-- Gauche : Statut réactif et micro -->
      <div class="flex items-center gap-3">
        <div
          class="relative flex h-9 w-9 shrink-0 items-center justify-center rounded-full transition-all duration-300"
          [ngClass]="active
            ? 'bg-[var(--brand-primary)] text-[var(--text-inverse)] shadow-md'
            : 'bg-[var(--app-surface-muted)] text-[var(--text-muted)]'"
        >
          @if (active) {
            <span class="absolute inline-flex h-full w-full animate-ping rounded-full bg-[var(--brand-primary)] opacity-40"></span>
          }
          <svg class="relative h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M12 18.75a6 6 0 006-6v-1.5m-12 0v1.5a6 6 0 006 6m0 0v3m-3 0h6M12 15.75a3 3 0 10-6 0v6.75a3 3 0 10-6 0" />
          </svg>
        </div>

        <div>
          <p class="text-xs font-bold text-[var(--text-primary)]">
            {{ active ? i18n.t('consultation.ai.listeningActive', 'Écoute active...') : i18n.t('consultation.ai.listeningPaused', 'Écoute en pause') }}
          </p>
          <p class="text-[10px] leading-4 text-[var(--text-muted)]">
            {{ active ? i18n.t('consultation.ai.speakNaturally', "Parlez naturellement, l'IA structure votre note") : i18n.t('consultation.ai.clickToResume', 'Cliquez pour reprendre') }}
          </p>
        </div>
      </div>

      <!-- Droite : Ondes sonores fluides et réactives (Waves) -->
      <div class="flex h-8 items-center gap-1" aria-label="Visualiseur d'ondes sonores">
        @for (wave of waves; track $index) {
          <span
            class="w-1 rounded-full transition-all duration-150 ease-out"
            [ngClass]="active ? 'bg-[var(--brand-primary)]' : 'bg-[var(--app-border)]'"
            [style.height.px]="waveHeight($index)"
            [style.opacity]="waveOpacity($index)"
          ></span>
        }
      </div>
    </div>
  `,
})
export class VoiceWaveVisualizerComponent {
  readonly i18n = inject(I18nService);

  @Input() active = false;
  @Input() audioLevel = 0;

  readonly waves = Array.from({ length: 20 });

  waveHeight(index: number): number {
    if (!this.active) return 6;
    const level = Math.min(1, Math.max(0.05, this.audioLevel));
    // Animation sinusoïdale fluide basée sur l'index et le niveau sonore audio
    const centerFactor = 1 - Math.abs(index - 9.5) / 10;
    const waveSin = Math.abs(Math.sin((index + 1) * 0.6 + Date.now() * 0.005));
    const dynamicHeight = 6 + (level * 22 + waveSin * 10) * centerFactor;
    return Math.round(Math.min(32, Math.max(4, dynamicHeight)));
  }

  waveOpacity(index: number): number {
    if (!this.active) return 0.35;
    const centerFactor = 0.5 + (1 - Math.abs(index - 9.5) / 10) * 0.5;
    return Math.min(1, Math.max(0.4, centerFactor));
  }
}
