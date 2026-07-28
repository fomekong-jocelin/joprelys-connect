import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnDestroy, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../core/i18n/i18n.service';
import { AiVitalField, AiVitalsApiService, AiVitalsProposal } from './ai-vitals-api.service';
import { RealtimeVitalsControllerComponent } from './realtime-vitals-controller.component';
import { VoiceListeningSurfaceComponent } from './voice-listening-surface.component';

@Component({
  selector: 'app-smart-vitals-assistant',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RealtimeVitalsControllerComponent,
    VoiceListeningSurfaceComponent,
  ],
  templateUrl: './smart-vitals-assistant.component.html',
})
export class SmartVitalsAssistantComponent implements OnDestroy {
  private readonly api = inject(AiVitalsApiService);
  readonly i18n = inject(I18nService);

  @Input({ required: true }) visitId = '';
  @Input() currentVitals: Partial<Record<AiVitalField, number>> = {};
  @Input() disabled = false;
  @Input() patientName = '';
  @Input() embedded = false;
  @Output() readonly proposed = new EventEmitter<AiVitalsProposal>();

  readonly expanded = signal(this.initiallyExpanded());
  readonly busy = signal(false);
  readonly recording = signal(false);
  readonly realtimeEnabled = signal(false);
  readonly realtimeActive = signal(false);
  readonly audioLevel = signal(0);
  readonly lastTranscript = signal('');
  readonly assistantMessage = signal('');
  readonly needsConfirmation = signal(false);
  readonly confirmationReason = signal('');
  readonly lastProposal = signal<AiVitalsProposal | null>(null);
  readonly proposalApplied = signal(false);
  readonly errorMessage = signal('');

  textInput = '';
  correctionText = '';
  readonly mediaRecorderSupported =
    typeof window !== 'undefined' &&
    'MediaRecorder' in window &&
    !!navigator.mediaDevices?.getUserMedia;

  private recorder: MediaRecorder | null = null;
  private stream: MediaStream | null = null;
  private chunks: Blob[] = [];
  private audioContext: AudioContext | null = null;
  private analyser: AnalyserNode | null = null;
  private meterFrame: number | null = null;

  ngOnDestroy(): void {
    this.disableRealtime();
    this.stopStream();
  }

  toggleExpanded(): void {
    if (this.expanded()) {
      this.disableRealtime();
      this.stopStream();
      this.recording.set(false);
      this.expanded.set(false);
      return;
    }
    this.expanded.set(true);
  }

  enableRealtime(): void {
    if (this.disabled) return;
    this.stopStream();
    this.recording.set(false);
    this.errorMessage.set('');
    this.realtimeEnabled.set(true);
  }

  disableRealtime(): void {
    this.realtimeEnabled.set(false);
    this.realtimeActive.set(false);
  }

  toggleRecording(): void {
    if (this.realtimeEnabled()) return;
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
    this.api
      .analyzeText(this.visitId, text, this.i18n.currentLanguage(), this.currentVitals)
      .subscribe({
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
    return Object.entries(proposal.vitals).filter(
      (entry): entry is [AiVitalField, number] => typeof entry[1] === 'number',
    );
  }

  applyCurrentProposal(): void {
    const proposal = this.lastProposal();
    if (!proposal || this.disabled || this.proposalApplied()) return;
    if (Object.keys(proposal.vitals).length === 0) return;
    this.proposed.emit(proposal);
    this.proposalApplied.set(true);
  }

  reanalyzeCorrection(): void {
    const corrected = this.correctionText.trim();
    if (!corrected || !this.visitId || this.busy() || this.disabled) return;
    this.busy.set(true);
    this.errorMessage.set('');
    this.api
      .analyzeText(this.visitId, corrected, this.i18n.currentLanguage(), this.currentVitals)
      .subscribe({
        next: proposal => this.handleProposal({ ...proposal, transcript: corrected }),
        error: () => this.handleError(),
      });
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

  handleProposal(proposal: AiVitalsProposal): void {
    this.busy.set(false);
    this.lastProposal.set(proposal);
    this.proposalApplied.set(false);
    this.lastTranscript.set(proposal.transcript || '');
    this.correctionText = proposal.transcript || '';
    this.assistantMessage.set(proposal.assistantMessage || '');
    this.needsConfirmation.set(proposal.needsConfirmation);
    this.confirmationReason.set(proposal.confirmationReason || '');
    this.errorMessage.set('');
  }

  private initiallyExpanded(): boolean {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      return true;
    }
    return window.matchMedia('(min-width: 640px)').matches;
  }

  private async startRecording(): Promise<void> {
    if (
      this.realtimeEnabled()
      || !this.mediaRecorderSupported
      || this.busy()
      || this.disabled
      || !this.visitId
    ) return;
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
      this.errorMessage.set(
        this.i18n.t(
          'vitals.assistant.micError',
          'Impossible d’accéder au microphone. Vérifiez les autorisations du navigateur.',
        ),
      );
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
    this.api
      .analyzeAudio(this.visitId, blob, this.i18n.currentLanguage(), this.currentVitals)
      .subscribe({
        next: proposal => this.handleProposal(proposal),
        error: () => this.handleError(),
      });
  }

  private handleError(): void {
    this.busy.set(false);
    this.recording.set(false);
    this.stopMeter();
    this.stopStream();
    this.errorMessage.set(
      this.i18n.t(
        'vitals.assistant.error',
        'Joprelys n’a pas pu analyser ces constantes. Réessayez ou saisissez-les manuellement.',
      ),
    );
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
    this.stopMeter();
    this.stream?.getTracks().forEach(track => track.stop());
    this.stream = null;
    this.recorder = null;
  }

}
