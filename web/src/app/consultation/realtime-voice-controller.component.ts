import { CommonModule } from '@angular/common';
import {
  Component, EventEmitter, Input, OnChanges, OnDestroy, Output,
  SimpleChanges, inject, signal,
} from '@angular/core';
import { Subscription } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import {
  AiMessageResponse,
  AiSessionResponse,
  AiTranscriptionResponse,
} from './ai-consultation-api.service';
import {
  AmbientAudioCaptureService,
  AmbientCaptureState,
} from './ambient-audio-capture.service';
import {
  RealtimeClinicalIntakeAck,
  RealtimeClinicalIntakeApiService,
} from './realtime-clinical-intake-api.service';
import { RealtimeClinicalTurnCoordinator } from './realtime-clinical-turn-coordinator.service';
import {
  RealtimeVoiceBridgeService,
  RealtimeVoiceState,
} from './realtime-voice-bridge.service';
import {
  RealtimeTranscriptCorrection,
  RealtimeTranscriptEntry,
  RealtimeTranscriptHistoryComponent,
} from './realtime-transcript-history.component';
import { VoiceListeningSurfaceComponent } from './voice-listening-surface.component';

const LOW_CONFIDENCE_REVIEW_FLOOR = 0.35;

@Component({
  selector: 'app-realtime-voice-controller',
  standalone: true,
  imports: [CommonModule, RealtimeTranscriptHistoryComponent, VoiceListeningSurfaceComponent],
  providers: [RealtimeClinicalTurnCoordinator],
  templateUrl: './realtime-voice-controller.component.html',
})
export class RealtimeVoiceControllerComponent implements OnChanges, OnDestroy {
  private readonly bridge = inject(RealtimeVoiceBridgeService);
  private readonly ambientCapture = inject(AmbientAudioCaptureService);
  private readonly intakeApi = inject(RealtimeClinicalIntakeApiService);
  private readonly pipeline = inject(RealtimeClinicalTurnCoordinator);
  readonly i18n = inject(I18nService);

  @Input({ required: true }) visitId = '';
  @Input() session: AiSessionResponse | null = null;
  @Input() enabled = false;
  /** Kept for API compatibility; capture itself never blocks on AI review state. */
  @Input() blocked = false;
  @Output() readonly message = new EventEmitter<AiMessageResponse>();
  @Output() readonly transcriptionReview = new EventEmitter<AiTranscriptionResponse>();
  @Output() readonly activeChange = new EventEmitter<boolean>();
  @Output() readonly realtimeError = new EventEmitter<string>();
  @Output() readonly switchToDictation = new EventEmitter<void>();
  @Output() readonly endSession = new EventEmitter<void>();

  readonly state = signal<RealtimeVoiceState>({
    connected: false,
    connecting: false,
    userSpeaking: false,
    assistantSpeaking: false,
    muted: false,
  });
  readonly ambientState = signal<AmbientCaptureState>({
    supported: true,
    active: false,
    starting: false,
    recovering: false,
    pendingChunks: 0,
    pendingBytes: 0,
    uploading: false,
    online: true,
    storagePressure: false,
    lastError: null,
  });
  readonly processing = this.pipeline.processing;
  readonly lastTranscript = this.pipeline.lastTranscript;
  readonly lastTranscriptConfidence = this.pipeline.lastTranscriptConfidence;
  readonly transcriptHistory = signal<RealtimeTranscriptEntry[]>([]);
  readonly finishPending = signal(false);
  readonly audioLevel = signal(0.4);
  readonly durationSeconds = signal(0);

  private readonly subscriptions = new Subscription();
  private durationTimer: ReturnType<typeof setInterval> | null = null;
  manualMuted = false;
  private finishEmitted = false;
  private connectingForVisit = '';
  private connectedVisitId = '';
  private connectionGeneration = 0;
  private connectionTransition: Promise<void> = Promise.resolve();
  private destroyed = false;
  private historyLoadGeneration = 0;

  constructor() {
    this.pipeline.configure({
      visitId: () => this.visitId.trim(),
      enabled: () => this.enabled,
      connected: () => this.state().connected && this.connectedVisitId === this.visitId.trim(),
      manualMuted: () => this.manualMuted,
      onPersisted: entry => this.upsertTranscript(entry),
      onError: error => this.realtimeError.emit(error),
      onPipelineStateChange: () => this.tryCompleteFinish(),
      syncMute: () => this.syncMute(),
    });

    this.subscriptions.add(this.bridge.state$.subscribe(state => {
      const previous = this.state();
      this.state.set(state);
      if (state.connected && this.enabled && this.session && !this.manualMuted) {
        this.connectedVisitId = this.visitId.trim();
      }
      this.activeChange.emit(state.connected);
      if (state.connected && !previous.connected) {
        this.startTimer();
        this.pipeline.resume();
      }
      if (!state.connected && previous.connected) this.stopTimer();
    }));
    this.subscriptions.add(this.ambientCapture.state$.subscribe(state => this.ambientState.set(state)));
    this.subscriptions.add(this.bridge.transcript$.subscribe(turn => this.pipeline.enqueue(turn)));
    this.subscriptions.add(this.bridge.error$.subscribe(error => this.realtimeError.emit(error)));
  }

  ngOnChanges(changes: SimpleChanges): void {
    const visitChanged = !!changes['visitId']
      && !changes['visitId'].firstChange
      && changes['visitId'].previousValue !== changes['visitId'].currentValue;
    if (visitChanged) {
      this.finishPending.set(false);
      this.finishEmitted = false;
      this.pipeline.reset();
      this.transcriptHistory.set([]);
    }

    if (changes['visitId'] || changes['session']) this.loadDurableHistory();

    const sessionChange = changes['session'];
    const previousSession = sessionChange?.previousValue as AiSessionResponse | null | undefined;
    const currentSession = sessionChange?.currentValue as AiSessionResponse | null | undefined;
    const sessionIdentityChanged = !!sessionChange && (
      previousSession?.sessionId !== currentSession?.sessionId
      || previousSession?.visitId !== currentSession?.visitId
      || (!!previousSession !== !!currentSession)
    );

    if (changes['enabled'] || changes['visitId'] || sessionIdentityChanged) {
      this.queueConnectionSync(visitChanged);
    }
    if (changes['session'] && this.state().connected && this.connectedVisitId === this.visitId.trim()) {
      this.pipeline.resume();
      this.tryCompleteFinish();
    }
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.connectionGeneration += 1;
    this.historyLoadGeneration += 1;
    this.stopTimer();
    this.pipeline.destroy();
    this.subscriptions.unsubscribe();
    this.bridge.disconnect();
    void this.ambientCapture.stop();
  }

  toggleMute(): void {
    if (!this.canToggleMute()) return;
    this.manualMuted = !this.manualMuted;
    this.queueConnectionSync(false);
  }

  canToggleMute(): boolean {
    if (this.manualMuted) return true;
    return this.state().connected && this.connectedVisitId === this.visitId.trim();
  }

  effectiveMuted(): boolean {
    return this.manualMuted
      || this.finishPending()
      || this.pipeline.durableBlocked()
      || this.state().muted;
  }

  get backlogPaused(): boolean {
    return this.pipeline.backlogPaused();
  }

  queuedCount(): number {
    return this.pipeline.queuedCount();
  }

  transcriptNeedsReview(): boolean {
    return this.transcriptHistory().some(entry => entry.reviewRequired)
      || this.lastTranscriptConfidence() === null
      || (this.lastTranscriptConfidence() ?? 1) < LOW_CONFIDENCE_REVIEW_FLOOR;
  }

  listeningSurfaceStatus(): string {
    if (this.finishPending()) return this.i18n.t('consultation.ai.realtimeFinishing');
    if (this.manualMuted) return this.i18n.t('consultation.ai.listeningPaused');
    if (this.backlogPaused) return this.i18n.t('consultation.ai.realtimeCatchingUp');
    if (this.state().connecting || this.ambientState().active && !this.state().connected) {
      return this.i18n.t('consultation.ai.reconnectingSimple');
    }
    if (this.state().connected) {
      return this.i18n.t('consultation.ai.listenNaturally', 'Écoute en cours... Parlez naturellement');
    }
    return this.i18n.t('consultation.ai.audioUnavailableSimple');
  }

  showSafetyAlert(): boolean {
    if (this.manualMuted) return false;
    return this.pipeline.durableBlocked()
      || this.ambientState().storagePressure
      || (!this.ambientState().active && !this.ambientState().starting && !this.state().connected)
      || (!this.state().connected && this.ambientState().active);
  }

  connectionBannerText(): string {
    if (this.pipeline.durableBlocked()) return this.i18n.t('consultation.ai.realtimeDurableIntakeBlocked');
    if (this.finishPending()) return this.i18n.t('consultation.ai.realtimeFinishingHelp');
    if (this.ambientState().storagePressure) return this.i18n.t('consultation.ai.storageCriticalSimple');
    if (!this.state().connected && this.ambientState().active) {
      return this.i18n.t('consultation.ai.realtimeProtectedReconnectSimple');
    }
    if (this.backlogPaused) return this.i18n.t('consultation.ai.realtimeBackpressure');
    return this.i18n.t('consultation.ai.audioProtectionUnavailableSimple');
  }

  connectionBannerClasses(): string {
    if (this.ambientState().storagePressure || !this.ambientState().active) {
      return 'border-[var(--brand-danger-border)] bg-[var(--brand-danger-subtle)] text-[var(--brand-danger-text)]';
    }
    return 'border-[var(--brand-warning-border)] bg-[var(--brand-warning-subtle)] text-[var(--brand-warning-text)]';
  }

  requestFinish(): void {
    if (this.destroyed || this.finishPending()) return;
    this.finishPending.set(true);
    this.syncMute();
    this.pipeline.resume();
    this.tryCompleteFinish();
  }

  requestTranscriptCorrection(correction: RealtimeTranscriptCorrection): void {
    if (!correction.id || !correction.text.trim() || this.pipeline.processing()) return;
    this.pipeline.submitManualCorrection(correction.id, correction.text);
  }

  private loadDurableHistory(): void {
    const visitId = this.visitId.trim();
    if (!visitId) return;
    const generation = ++this.historyLoadGeneration;
    this.intakeApi.list(visitId).subscribe({
      next: entries => {
        if (generation !== this.historyLoadGeneration || visitId !== this.visitId.trim()) return;
        this.transcriptHistory.set(entries.map(entry => this.toTranscriptEntry(entry)));
      },
      error: () => {
        // Capture can still start. A hard persistence error will fail closed on the first ACK.
      },
    });
  }

  private upsertTranscript(entry: RealtimeClinicalIntakeAck): void {
    this.transcriptHistory.update(history => {
      const mapped = this.toTranscriptEntry(entry);
      const existing = history.findIndex(item => item.id === mapped.id);
      const next = existing >= 0
        ? history.map((item, index) => index === existing ? mapped : item)
        : [...history, mapped];
      return next.sort((a, b) => a.timestamp - b.timestamp);
    });
  }

  private toTranscriptEntry(entry: RealtimeClinicalIntakeAck): RealtimeTranscriptEntry {
    return {
      id: entry.id,
      text: entry.transcript,
      timestamp: Date.parse(entry.receivedAt) || Date.now(),
      confidence: entry.confidence,
      reviewRequired: entry.reviewRequired,
      correctionCount: entry.correctionCount,
      durable: true,
    };
  }

  private tryCompleteFinish(): void {
    if (!this.finishPending() || this.finishEmitted || this.destroyed || !this.pipeline.isIdle()) return;
    this.finishEmitted = true;
    this.endSession.emit();
  }

  private queueConnectionSync(forceVisitReset: boolean): void {
    const generation = ++this.connectionGeneration;
    this.connectionTransition = this.connectionTransition
      .catch(() => undefined)
      .then(() => this.syncConnection(generation, forceVisitReset))
      .catch(error => {
        if (generation !== this.connectionGeneration || this.destroyed) return;
        this.realtimeError.emit(
          error instanceof Error && error.message.trim()
            ? error.message
            : this.i18n.t('consultation.ai.realtimeUnavailable'),
        );
      });
  }

  private async syncConnection(generation: number, forceVisitReset: boolean): Promise<void> {
    if (generation !== this.connectionGeneration || this.destroyed) return;
    const targetVisitId = this.visitId.trim();
    const activeBelongsToAnotherVisit = !!this.connectedVisitId && this.connectedVisitId !== targetVisitId;
    const connectionInFlightForAnotherVisit = !!this.connectingForVisit && this.connectingForVisit !== targetVisitId;

    if (forceVisitReset || activeBelongsToAnotherVisit || connectionInFlightForAnotherVisit) {
      this.pipeline.reset();
      this.connectingForVisit = '';
      this.connectedVisitId = '';
      this.finishPending.set(false);
      this.finishEmitted = false;
      this.bridge.disconnect();
      await this.ambientCapture.stop();
      if (generation !== this.connectionGeneration || this.destroyed) return;
    }

    if (!this.enabled || !targetVisitId || !this.session) {
      this.connectingForVisit = '';
      this.connectedVisitId = '';
      this.bridge.disconnect();
      await this.ambientCapture.stop();
      return;
    }

    if (this.manualMuted) {
      this.connectingForVisit = '';
      this.connectedVisitId = '';
      this.bridge.disconnect();
      await this.ambientCapture.stop();
      return;
    }

    if (this.state().connected && this.connectedVisitId === targetVisitId) {
      if (!this.ambientState().active && !this.ambientState().starting) {
        await this.startAmbientSafetyCapture(targetVisitId, generation);
      }
      this.syncMute();
      this.pipeline.resume();
      return;
    }

    if (this.state().connecting && this.connectingForVisit === targetVisitId) return;

    if (this.state().connected || this.state().connecting) {
      this.bridge.disconnect();
      await this.ambientCapture.stop();
      if (generation !== this.connectionGeneration || this.destroyed) return;
    }

    if (!this.bridge.isSupported()) {
      this.activeChange.emit(false);
      this.realtimeError.emit(this.i18n.t('consultation.ai.realtimeUnsupported'));
      return;
    }

    this.connectingForVisit = targetVisitId;
    try {
      await this.startAmbientSafetyCapture(targetVisitId, generation);
      if (generation !== this.connectionGeneration || this.visitId.trim() !== targetVisitId || this.destroyed) {
        await this.ambientCapture.stop();
        return;
      }
      await this.bridge.connect(
        targetVisitId,
        'consultation',
        () => this.ambientCapture.mediaStreamForVisit(targetVisitId),
      );
      if (generation !== this.connectionGeneration || this.visitId.trim() !== targetVisitId || this.destroyed) {
        this.bridge.disconnect();
        await this.ambientCapture.stop();
        return;
      }
      this.connectedVisitId = targetVisitId;
      this.syncMute();
      this.pipeline.resume();
    } catch (error) {
      if (generation !== this.connectionGeneration || this.destroyed) return;
      this.connectedVisitId = '';
      throw error;
    } finally {
      if (this.connectingForVisit === targetVisitId) this.connectingForVisit = '';
    }
  }

  private async startAmbientSafetyCapture(targetVisitId: string, generation: number): Promise<void> {
    try {
      await this.ambientCapture.start(targetVisitId, this.i18n.currentLanguage());
      if (generation !== this.connectionGeneration || this.visitId.trim() !== targetVisitId || this.destroyed) {
        await this.ambientCapture.stop();
      }
    } catch {
      throw new Error(this.i18n.t('consultation.ai.ambientRequired'));
    }
  }

  private startTimer(): void {
    this.stopTimer();
    this.durationSeconds.set(0);
    this.durationTimer = setInterval(() => {
      if (this.state().connected && !this.manualMuted) this.durationSeconds.update(seconds => seconds + 1);
    }, 1000);
  }

  private stopTimer(): void {
    if (!this.durationTimer) return;
    clearInterval(this.durationTimer);
    this.durationTimer = null;
  }

  private syncMute(): void {
    this.bridge.setMuted(
      this.manualMuted
      || this.finishPending()
      || this.pipeline.durableBlocked(),
    );
  }
}
