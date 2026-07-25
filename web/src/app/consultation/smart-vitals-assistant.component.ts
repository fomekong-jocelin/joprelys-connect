import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  Output,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../core/i18n/i18n.service';
import { AiConsultationApiService } from './ai-consultation-api.service';
import {
  AiVitalField,
  AiVitalsApiService,
  AiVitalsProposal,
} from './ai-vitals-api.service';

@Component({
  selector: 'app-smart-vitals-assistant',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section
      class="fixed inset-x-3 bottom-3 z-[80] sm:left-1/2 sm:right-auto sm:w-[560px] sm:-translate-x-1/2"
      aria-live="polite"
    >
      <div class="overflow-hidden rounded-[10px] border border-cyan-200/80 bg-[var(--app-surface)] shadow-2xl dark:border-cyan-900/70">
        <header class="flex items-center justify-between gap-3 border-b border-[var(--app-border)] bg-[var(--app-surface-muted)] px-3 py-2.5 sm:px-4">
          <button
            type="button"
            class="flex min-w-0 flex-1 items-center gap-2 text-left"
            (click)="expanded.set(!expanded())"
            [attr.aria-expanded]="expanded()"
          >
            <span class="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-[8px] bg-cyan-50 text-cyan-700 dark:bg-cyan-950/40 dark:text-cyan-300">
              <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 18.75a6 6 0 006-6v-1.5m-12 0v1.5a6 6 0 006 6m0 0v3m-3 0h6M12 15.75a3 3 0 003-3V6a3 3 0 10-6 0v6.75a3 3 0 003 3z" />
              </svg>
            </span>
            <span class="min-w-0">
              <span class="block truncate text-xs font-extrabold text-[var(--text-primary)] sm:text-sm">
                {{ i18n.t('vitals.assistant.title', 'Assistant de constantes') }}
              </span>
              <span class="block truncate text-[10px] font-medium text-[var(--text-muted)] sm:text-xs">
                @if (recording()) {
                  {{ i18n.t('vitals.assistant.listening', 'Je vous écoute…') }}
                } @else if (speaking()) {
                  {{ i18n.t('vitals.assistant.speaking', 'Joprelys vous répond…') }}
                } @else {
                  {{ patientName || i18n.t('vitals.assistant.subtitle', 'Dictez les paramètres, vérifiez, puis enregistrez.') }}
                }
              </span>
            </span>
          </button>

          <button
            type="button"
            class="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-[8px] text-[var(--text-muted)] hover:bg-[var(--app-surface)]"
            (click)="expanded.set(!expanded())"
            [attr.aria-label]="i18n.t('vitals.assistant.toggle', 'Afficher ou réduire l’assistant')"
          >
            <svg class="h-4 w-4 transition-transform" [class.rotate-180]="expanded()" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 15l7-7 7 7" />
            </svg>
          </button>
        </header>

        @if (expanded()) {
          <div class="max-h-[58dvh] space-y-3 overflow-y-auto p-3 sm:p-4">
            <div class="rounded-[8px] border border-cyan-100 bg-cyan-50/60 p-3 text-xs leading-5 text-cyan-950 dark:border-cyan-950 dark:bg-cyan-950/20 dark:text-cyan-100">
              <strong>{{ i18n.t('vitals.assistant.exampleTitle', 'Vous pouvez dire :') }}</strong>
              {{ i18n.t('vitals.assistant.example', '« Température 38,4, tension 132 sur 84, saturation 96, pouls 104, poids 73 kilos. »') }}
            </div>

            @if (recording() || speaking()) {
              <div class="flex h-12 items-center justify-center gap-1 rounded-[8px] bg-[var(--app-surface-muted)] px-3" aria-hidden="true">
                @for (bar of waveformBars; track $index) {
                  <span
                    class="w-1 rounded-full bg-cyan-500 transition-[height] duration-75"
                    [style.height.px]="waveHeight($index)"
                  ></span>
                }
              </div>
            }

            <div class="grid grid-cols-1 gap-2 sm:grid-cols-[auto_1fr_auto] sm:items-center">
              <button
                type="button"
                (click)="toggleRecording()"
                [disabled]="disabled || busy() || !mediaRecorderSupported"
                class="inline-flex min-h-12 items-center justify-center gap-2 rounded-[8px] px-4 text-sm font-bold text-white transition disabled:cursor-not-allowed disabled:opacity-50"
                [ngClass]="recording() ? 'bg-rose-600 hover:bg-rose-700' : 'bg-[var(--brand-primary)] hover:bg-[var(--brand-primary-hover)]'"
              >
                <span class="relative inline-flex h-6 w-6 items-center justify-center">
                  @if (recording()) {
                    <span class="absolute h-6 w-6 animate-ping rounded-full bg-white/25"></span>
                  }
                  <svg class="relative h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 18.75a6 6 0 006-6v-1.5m-12 0v1.5a6 6 0 006 6m0 0v3m-3 0h6M12 15.75a3 3 0 003-3V6a3 3 0 10-6 0v6.75a3 3 0 003 3z" />
                  </svg>
                </span>
                {{ recording()
                  ? i18n.t('vitals.assistant.stop', 'Terminer')
                  : i18n.t('vitals.assistant.record', 'Dicter les constantes') }}
              </button>

              <input
                type="text"
                [(ngModel)]="textInput"
                (keyup.enter)="sendText()"
                [disabled]="disabled || busy() || recording()"
                [placeholder]="i18n.t('vitals.assistant.textPlaceholder', 'Ou saisissez : SpO₂ 97, température 37,8…')"
                class="ui-input min-h-12 w-full px-3 text-sm"
              />

              <button
                type="button"
                (click)="sendText()"
                [disabled]="disabled || busy() || recording() || !textInput.trim()"
                class="inline-flex min-h-12 items-center justify-center rounded-[8px] border border-[var(--app-border)] px-4 text-sm font-bold text-[var(--text-primary)] hover:bg-[var(--app-surface-muted)] disabled:opacity-50"
              >
                {{ i18n.t('vitals.assistant.analyze', 'Analyser') }}
              </button>
            </div>

            @if (!mediaRecorderSupported) {
              <p class="text-xs font-semibold text-amber-700 dark:text-amber-300">
                {{ i18n.t('vitals.assistant.micUnsupported', 'Le microphone n’est pas disponible dans ce navigateur. Utilisez la saisie texte.') }}
              </p>
            }

            @if (busy()) {
              <div class="flex items-center gap-2 text-xs font-semibold text-[var(--text-muted)]">
                <span class="h-4 w-4 animate-spin rounded-full border-2 border-cyan-200 border-t-cyan-600"></span>
                {{ i18n.t('vitals.assistant.processing', 'Analyse des constantes…') }}
              </div>
            }

            @if (errorMessage()) {
              <div class="rounded-[8px] border border-rose-200 bg-rose-50 p-3 text-xs font-semibold text-rose-700 dark:border-rose-900 dark:bg-rose-950/30 dark:text-rose-300">
                {{ errorMessage() }}
              </div>
            }

            @if (lastTranscript()) {
              <div class="rounded-[8px] border border-[var(--app-border)] p-3">
                <p class="text-[10px] font-extrabold uppercase tracking-wider text-[var(--text-muted)]">
                  {{ i18n.t('vitals.assistant.heard', 'J’ai entendu') }}
                </p>
                <p class="mt-1 text-xs leading-5 text-[var(--text-primary)]">{{ lastTranscript() }}</p>
              </div>
            }

            @if (assistantMessage()) {
              <div
                class="rounded-[8px] border p-3"
                [ngClass]="needsConfirmation()
                  ? 'border-amber-200 bg-amber-50 dark:border-amber-900 dark:bg-amber-950/25'
                  : 'border-emerald-200 bg-emerald-50 dark:border-emerald-900 dark:bg-emerald-950/25'"
              >
                <div class="flex items-start gap-2">
                  <span class="mt-0.5 text-base">{{ needsConfirmation() ? '⚠️' : '✓' }}</span>
                  <div class="min-w-0">
                    <p class="text-xs font-bold text-[var(--text-primary)]">{{ assistantMessage() }}</p>
                    @if (needsConfirmation() && confirmationReason()) {
                      <p class="mt-1 text-[11px] leading-4 text-[var(--text-muted)]">{{ confirmationReason() }}</p>
                    }
                  </div>
                </div>
              </div>
            }

            @if (proposalEntries().length > 0) {
              <div>
                <p class="mb-2 text-[10px] font-extrabold uppercase tracking-wider text-[var(--text-muted)]">
                  {{ i18n.t('vitals.assistant.detected', 'Champs préremplis — à vérifier') }}
                </p>
                <div class="grid grid-cols-2 gap-2 sm:grid-cols-3">
                  @for (entry of proposalEntries(); track entry[0]) {
                    <div class="rounded-[8px] border border-[var(--app-border)] bg-[var(--app-surface-muted)] px-3 py-2">
                      <span class="block text-[9px] font-extrabold uppercase tracking-wide text-[var(--text-muted)]">{{ labelFor(entry[0]) }}</span>
                      <span class="mt-0.5 block text-sm font-black text-[var(--text-primary)]">{{ entry[1] }} {{ unitFor(entry[0]) }}</span>
                    </div>
                  }
                </div>
                <p class="mt-2 text-[10px] leading-4 text-[var(--text-muted)]">
                  {{ i18n.t('vitals.assistant.reviewNotice', 'Joprelys remplit les champs mais ne les enregistre jamais sans votre validation.') }}
                </p>
              </div>
            }
          </div>
        }
      </div>
    </section>
  `,
})
export class SmartVitalsAssistantComponent implements OnDestroy {
  private readonly api = inject(AiVitalsApiService);
  private readonly voiceApi = inject(AiConsultationApiService);
  readonly i18n = inject(I18nService);

  @Input({ required: true }) visitId = '';
  @Input() currentVitals: Partial<Record<AiVitalField, number>> = {};
  @Input() disabled = false;
  @Input() patientName = '';
  @Output() readonly proposed = new EventEmitter<AiVitalsProposal>();

  readonly expanded = signal(true);
  readonly busy = signal(false);
  readonly recording = signal(false);
  readonly speaking = signal(false);
  readonly audioLevel = signal(0);
  readonly lastTranscript = signal('');
  readonly assistantMessage = signal('');
  readonly needsConfirmation = signal(false);
  readonly confirmationReason = signal('');
  readonly lastProposal = signal<AiVitalsProposal | null>(null);
  readonly errorMessage = signal('');

  textInput = '';
  readonly waveformBars = Array.from({ length: 24 });
  readonly mediaRecorderSupported =
    typeof window !== 'undefined'
    && 'MediaRecorder' in window
    && !!navigator.mediaDevices?.getUserMedia;

  private recorder: MediaRecorder | null = null;
  private stream: MediaStream | null = null;
  private chunks: Blob[] = [];
  private audioContext: AudioContext | null = null;
  private analyser: AnalyserNode | null = null;
  private meterFrame: number | null = null;
  private assistantAudio: HTMLAudioElement | null = null;
  private assistantAudioUrl: string | null = null;

  ngOnDestroy(): void {
    this.stopStream();
    this.stopAssistantAudio();
  }

  toggleRecording(): void {
    if (this.recording()) {
      this.recorder?.stop();
      return;
    }
    void this.startRecording();
  }

  sendText(): void {
    const text = this.textInput.trim();
    if (!text || !this.visitId || this.busy() || this.disabled) return;
    this.busy.set(true);
    this.errorMessage.set('');
    this.api.analyzeText(
      this.visitId,
      text,
      this.i18n.currentLanguage(),
      this.currentVitals,
    ).subscribe({
      next: proposal => {
        this.textInput = '';
        this.handleProposal(proposal);
      },
      error: () => this.handleError(),
    });
  }

  proposalEntries(): Array<[AiVitalField, number]> {
    const proposal = this.lastProposal();
    if (!proposal) return [];
    return Object.entries(proposal.vitals)
      .filter((entry): entry is [AiVitalField, number] => typeof entry[1] === 'number');
  }

  waveHeight(index: number): number {
    const level = this.audioLevel();
    const oscillation = 0.45 + Math.abs(Math.sin((index + 1) * 1.7)) * 0.55;
    const active = this.recording() ? Math.max(0.16, level) : this.speaking() ? 0.65 : 0.12;
    return Math.round(6 + 32 * active * oscillation);
  }

  labelFor(field: AiVitalField): string {
    return this.i18n.t(`vitals.assistant.field.${field}`, field);
  }

  unitFor(field: AiVitalField): string {
    const units: Record<AiVitalField, string> = {
      temperature: '°C',
      weight: 'kg',
      height: 'cm',
      pulse: 'bpm',
      systolic: 'mmHg',
      diastolic: 'mmHg',
      spo2: '%',
      glycemia: 'g/L',
      respiratoryRate: this.i18n.t('vitals.assistant.unit.resp', 'resp/min'),
      painScale: '/10',
    };
    return units[field];
  }

  private async startRecording(): Promise<void> {
    if (!this.mediaRecorderSupported || this.busy() || this.disabled || !this.visitId) return;
    this.stopAssistantAudio();
    this.errorMessage.set('');
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: 1,
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
      });
      const mimeType = this.preferredMimeType();
      const recorder = mimeType
        ? new MediaRecorder(stream, { mimeType, audioBitsPerSecond: 128000 })
        : new MediaRecorder(stream, { audioBitsPerSecond: 128000 });
      this.stream = stream;
      this.recorder = recorder;
      this.chunks = [];
      recorder.ondataavailable = event => {
        if (event.data.size > 0) this.chunks.push(event.data);
      };
      recorder.onstop = () => this.finishRecording(recorder.mimeType || mimeType || 'audio/webm');
      recorder.start(250);
      this.recording.set(true);
      this.startMeter(stream);
    } catch {
      this.errorMessage.set(this.i18n.t(
        'vitals.assistant.micError',
        'Impossible d’accéder au microphone. Vérifiez les autorisations du navigateur.',
      ));
    }
  }

  private finishRecording(contentType: string): void {
    this.recording.set(false);
    this.stopMeter();
    this.stopStream();
    const blob = new Blob(this.chunks, { type: contentType || 'audio/webm' });
    this.chunks = [];
    if (!blob.size) {
      this.handleError();
      return;
    }
    this.busy.set(true);
    this.api.analyzeAudio(
      this.visitId,
      blob,
      this.i18n.currentLanguage(),
      this.currentVitals,
    ).subscribe({
      next: proposal => this.handleProposal(proposal),
      error: () => this.handleError(),
    });
  }

  private handleProposal(proposal: AiVitalsProposal): void {
    this.busy.set(false);
    this.lastProposal.set(proposal);
    this.lastTranscript.set(proposal.transcript || '');
    this.assistantMessage.set(proposal.assistantMessage || '');
    this.needsConfirmation.set(proposal.needsConfirmation);
    this.confirmationReason.set(proposal.confirmationReason || '');
    this.errorMessage.set('');
    if (Object.keys(proposal.vitals).length > 0) {
      this.proposed.emit(proposal);
    }
    if (proposal.assistantMessage) {
      this.speak(proposal.assistantMessage);
    }
  }

  private speak(text: string): void {
    this.stopAssistantAudio();
    this.speaking.set(true);
    this.voiceApi.synthesizeSpeech(text).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const audio = new Audio(url);
        this.assistantAudio = audio;
        this.assistantAudioUrl = url;
        audio.onended = () => this.stopAssistantAudio();
        audio.onerror = () => this.stopAssistantAudio();
        void audio.play().catch(() => this.stopAssistantAudio());
      },
      error: () => this.speaking.set(false),
    });
  }

  private handleError(): void {
    this.busy.set(false);
    this.recording.set(false);
    this.stopMeter();
    this.stopStream();
    this.errorMessage.set(this.i18n.t(
      'vitals.assistant.error',
      'Joprelys n’a pas pu analyser ces constantes. Réessayez ou saisissez-les manuellement.',
    ));
  }

  private preferredMimeType(): string {
    const candidates = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4'];
    return candidates.find(type => MediaRecorder.isTypeSupported(type)) ?? '';
  }

  private startMeter(stream: MediaStream): void {
    try {
      this.audioContext = new AudioContext();
      const source = this.audioContext.createMediaStreamSource(stream);
      this.analyser = this.audioContext.createAnalyser();
      this.analyser.fftSize = 256;
      source.connect(this.analyser);
      const data = new Uint8Array(this.analyser.frequencyBinCount);
      const tick = () => {
        if (!this.analyser || !this.recording()) return;
        this.analyser.getByteFrequencyData(data);
        const average = data.reduce((sum, value) => sum + value, 0) / Math.max(1, data.length);
        this.audioLevel.set(Math.min(1, average / 100));
        this.meterFrame = requestAnimationFrame(tick);
      };
      tick();
    } catch {
      this.audioLevel.set(0.3);
    }
  }

  private stopMeter(): void {
    if (this.meterFrame !== null) cancelAnimationFrame(this.meterFrame);
    this.meterFrame = null;
    this.analyser = null;
    this.audioLevel.set(0);
    if (this.audioContext) {
      void this.audioContext.close().catch(() => undefined);
      this.audioContext = null;
    }
  }

  private stopStream(): void {
    this.stream?.getTracks().forEach(track => track.stop());
    this.stream = null;
    this.recorder = null;
  }

  private stopAssistantAudio(): void {
    if (this.assistantAudio) {
      this.assistantAudio.pause();
      this.assistantAudio.src = '';
      this.assistantAudio = null;
    }
    if (this.assistantAudioUrl) {
      URL.revokeObjectURL(this.assistantAudioUrl);
      this.assistantAudioUrl = null;
    }
    this.speaking.set(false);
  }
}
