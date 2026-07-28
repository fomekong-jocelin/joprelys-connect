import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { IconComponent } from '../shared/ui/icon.component';

@Component({
  selector: 'app-voice-wave-visualizer',
  standalone: true,
  imports: [IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      class="relative mx-auto min-h-48 w-full max-w-4xl overflow-hidden text-[var(--brand-primary)] sm:min-h-52"
      [attr.aria-label]="accessibleLabel"
      aria-live="polite"
      role="status"
    >
      <canvas
        #waveCanvas
        class="pointer-events-none absolute inset-x-0 top-12 h-28 w-full sm:top-14 sm:h-32"
        aria-hidden="true"
      ></canvas>

      <div class="relative z-10 mx-auto flex h-28 w-28 items-center justify-center sm:h-32 sm:w-32">
        <span class="voice-halo voice-halo-outer absolute h-28 w-28 rounded-full border border-[var(--brand-primary-border)] sm:h-32 sm:w-32"></span>
        <span class="voice-halo voice-halo-middle absolute h-24 w-24 rounded-full border border-[var(--brand-primary-border)] sm:h-28 sm:w-28"></span>
        <span class="voice-halo voice-halo-inner absolute h-20 w-20 rounded-full bg-[var(--brand-primary-subtle)] sm:h-24 sm:w-24"></span>
        <div
          class="voice-micro relative flex h-16 w-16 items-center justify-center rounded-full text-[var(--text-inverse)] shadow-[var(--shadow-panel)] transition sm:h-20 sm:w-20"
          [class.voice-micro-active]="active"
          [class.voice-micro-idle]="!active"
        >
          <app-ui-icon name="microphone" class="text-4xl sm:text-5xl" aria-hidden="true" />
        </div>
      </div>

      <p
        class="absolute inset-x-2 bottom-2 z-10 text-center text-sm font-bold leading-5 text-[var(--text-primary)] sm:bottom-1 sm:text-base"
      >
        {{ statusText }}
      </p>
    </div>
  `,
  styles: `
    .voice-micro {
      background: var(--app-surface-muted);
      border: 1px solid var(--app-border);
      color: var(--text-muted);
    }

    .voice-micro-active {
      background:
        linear-gradient(
          145deg,
          var(--brand-primary-hover),
          var(--brand-primary)
        );
      border-color: color-mix(in srgb, var(--brand-primary) 68%, transparent);
      color: var(--text-inverse);
      box-shadow:
        0 10px 28px color-mix(in srgb, var(--brand-primary) 28%, transparent),
        inset 0 1px 0 color-mix(in srgb, var(--app-surface) 55%, transparent);
    }

    .voice-micro-idle {
      box-shadow: var(--shadow-panel-subtle);
    }

    .voice-halo {
      transform-origin: center;
    }

    .voice-halo-outer {
      animation: voice-halo-pulse 2.8s ease-out infinite;
      opacity: 0.42;
    }

    .voice-halo-middle {
      animation: voice-halo-pulse 2.8s 0.45s ease-out infinite;
      opacity: 0.58;
    }

    .voice-halo-inner {
      animation: voice-halo-breathe 2.2s ease-in-out infinite;
    }

    @keyframes voice-halo-pulse {
      0%, 100% { opacity: 0.26; transform: scale(0.94); }
      50% { opacity: 0.66; transform: scale(1); }
    }

    @keyframes voice-halo-breathe {
      0%, 100% { opacity: 0.56; transform: scale(0.96); }
      50% { opacity: 0.9; transform: scale(1.03); }
    }

    @media (prefers-reduced-motion: reduce) {
      .voice-halo {
        animation: none;
      }
    }
  `,
})
export class VoiceWaveVisualizerComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() active = false;
  @Input() audioLevel = 0;
  @Input() statusText = '';
  @Input() accessibleLabel = '';

  @ViewChild('waveCanvas', { static: true })
  private readonly waveCanvas?: ElementRef<HTMLCanvasElement>;

  private animationFrame: number | null = null;
  private viewReady = false;
  private phase = 0;

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.refreshAnimation();
  }

  ngOnChanges(_changes: SimpleChanges): void {
    if (this.viewReady) this.refreshAnimation();
  }

  ngOnDestroy(): void {
    this.stopAnimation();
  }

  private refreshAnimation(): void {
    this.stopAnimation();
    if (typeof CanvasRenderingContext2D === 'undefined') return;
    this.drawFrame();
    if (!this.active || this.prefersReducedMotion()) return;
    this.animate();
  }

  private animate(): void {
    const tick = () => {
      this.phase += 0.055 + this.normalizedLevel() * 0.04;
      this.drawFrame();
      this.animationFrame = requestAnimationFrame(tick);
    };
    this.animationFrame = requestAnimationFrame(tick);
  }

  private stopAnimation(): void {
    if (this.animationFrame !== null) cancelAnimationFrame(this.animationFrame);
    this.animationFrame = null;
  }

  private drawFrame(): void {
    const canvas = this.waveCanvas?.nativeElement;
    if (!canvas) return;
    let context: CanvasRenderingContext2D | null = null;
    try {
      context = canvas.getContext('2d');
    } catch {
      return;
    }
    if (!context) return;

    const width = Math.max(1, Math.round(canvas.clientWidth));
    const height = Math.max(1, Math.round(canvas.clientHeight));
    const ratio = Math.max(1, globalThis.devicePixelRatio || 1);
    if (canvas.width !== width * ratio || canvas.height !== height * ratio) {
      canvas.width = width * ratio;
      canvas.height = height * ratio;
    }
    context.setTransform(ratio, 0, 0, ratio, 0, 0);
    context.clearRect(0, 0, width, height);
    context.strokeStyle = getComputedStyle(canvas).color;
    this.drawBaseline(context, width, height);
    this.drawWaveRibbons(context, width, height);
  }

  private drawBaseline(context: CanvasRenderingContext2D, width: number, height: number): void {
    context.save();
    context.globalAlpha = this.active ? 0.28 : 0.18;
    context.lineWidth = 1;
    context.setLineDash([2, 6]);
    context.beginPath();
    context.moveTo(0, height * 0.56);
    context.lineTo(width, height * 0.56);
    context.stroke();
    context.restore();
  }

  private drawWaveRibbons(context: CanvasRenderingContext2D, width: number, height: number): void {
    const level = this.normalizedLevel();
    const baseline = height * 0.56;
    const ribbonCount = 5;
    for (let ribbon = 0; ribbon < ribbonCount; ribbon += 1) {
      context.save();
      context.globalAlpha = this.active ? 0.2 + ribbon * 0.11 : 0.12;
      context.lineWidth = ribbon === ribbonCount - 1 ? 2 : 1.15;
      context.setLineDash([]);
      context.beginPath();
      for (let x = 0; x <= width; x += 3) {
        const progress = x / width;
        const envelope = Math.pow(Math.sin(Math.PI * progress), 0.72);
        const amplitude = (6 + level * (18 + ribbon * 2.5)) * envelope;
        const frequency = 4.4 + ribbon * 0.44;
        const wave = Math.sin(progress * Math.PI * frequency + this.phase + ribbon * 0.72);
        const detail = Math.sin(progress * Math.PI * 11.5 - this.phase * 0.7 + ribbon) * 0.28;
        const y = baseline + (wave + detail) * amplitude;
        if (x === 0) context.moveTo(x, y);
        else context.lineTo(x, y);
      }
      context.stroke();
      context.restore();
    }
  }

  private normalizedLevel(): number {
    if (!this.active) return 0.08;
    return Math.min(1, Math.max(0.22, this.audioLevel));
  }

  private prefersReducedMotion(): boolean {
    return typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches;
  }
}
