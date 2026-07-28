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
import { ClinicalVoicePlaybackService } from './clinical-voice-playback.service';
import {
  RealtimeClinicalTurnCoordinator,
} from './realtime-clinical-turn-coordinator.service';
import {
  RealtimeVoiceBridgeService,
  RealtimeVoiceState,
} from './realtime-voice-bridge.service';
import {
  RealtimeTranscriptEntry,
  RealtimeTranscriptHistoryComponent,
} from './realtime-transcript-history.component';
import { VoiceListeningSurfaceComponent } from './voice-listening-surface.component';

const LOW_CONFIDENCE_REVIEW_FLOOR = 0.35;

@Component({
  selector: 'app-realtime-voice-controller',
  standalone: true,
  imports: [CommonModule, RealtimeTranscriptHistoryComponent, VoiceListeningSurfaceComponent],
  providers: [RealtimeClinicalTurnCoordinator, ClinicalVoicePlaybackService],
  templateUrl: './realtime-voice-controller.component.html',
})
export class RealtimeVoiceControllerComponent implements OnChanges, OnDestroy {
  private readonly bridge = inject(RealtimeVoiceBridgeService);
  private readonly ambientCapture = inject(AmbientAudioCaptureService);
  private readonly pipeline = inject(RealtimeClinicalTurnCoordinator);
  private readonly voicePlayback = inject(ClinicalVoicePlaybackService);
  readonly i18n = inject(I18nService);

  @Input({ required: true }) visitId = '';
  @Input() session: AiSessionResponse | null = null;
  @Input() enabled = false;
  /** Blocks clinical analysis only. Durable capture keeps running. */
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
  private durationTimer: ReturnType<typeof setInterval> | null = null;

  private readonly subscriptions = new Subscription();
  manualMuted = false;
  private finishEmitted = false;
  private connectingForVisit = '';
  private connectedVisitId = '';
  private connectionGeneration = 0;
  private connectionTransition: Promise<void> = Promise.resolve();
  private destroyed = false;

  constructor() {
    this.pipeline.configure({
      visitId: () => this.visitId.trim(),
      session: () => this.session,
      enabled: () => this.enabled,
      connected: () => this.state().connected && this.connectedVisitId === this.visitId.trim(),
      manualMuted: () => this.manualMuted,
      blocked: () => this.blocked,
      onMessage: response => {
        this.applyRealtimeResponseLocally(response);
        this.message.emit(response);
        this.playAssistantResponse(response);
      },
      onReview: response => {
        this.applyRealtimeReviewLocally(response);
        this.transcriptionReview.emit(response);
      },
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
        this.playPendingClarification();
        this.pipeline.resume();
      }
      if (!state.connected && previous.connected) {
        this.stopTimer();
      }
      if (state.userSpeaking && !previous.userSpeaking) this.voicePlayback.stop();
    }));
    this.subscriptions.add(this.ambientCapture.state$.subscribe(state => this.ambientState.set(state)));
    this.subscriptions.add(this.bridge.transcript$.subscribe(turn => this.pipeline.enqueue(turn)));
    this.subscriptions.add(this.bridge.transcript$.subscribe(turn => {
      const text = turn.transcript.trim();
      if (text) {
        this.transcriptHistory.update(history => {
          const timestamp = Date.now();
          return [...history, {
            id: turn.itemId?.trim() || turn.eventId?.trim() || `local-${timestamp}-${history.length}`,
            text,
            timestamp,
          }];
        });
      }
    }));
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
    if (changes['blocked'] && !this.blocked) this.pipeline.resume();
    if (changes['session'] && this.state().connected && this.connectedVisitId === this.visitId.trim()) {
      this.pipeline.resume();
      this.tryCompleteFinish();
    }
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.connectionGeneration += 1;
    this.stopTimer();
    this.pipeline.destroy();
    this.subscriptions.unsubscribe();
    this.voicePlayback.stop();
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
    const confidence = this.lastTranscriptConfidence();
    return confidence === null || confidence < LOW_CONFIDENCE_REVIEW_FLOOR;
  }

  listeningSurfaceStatus(): string {
    if (this.finishPending()) return this.i18n.t('consultation.ai.realtimeFinishing');
    if (this.manualMuted) return this.i18n.t('consultation.ai.listeningPaused');
    if (this.backlogPaused) return this.i18n.t('consultation.ai.realtimeCatchingUp');
    if (this.state().connecting || this.ambientState().active && !this.state().connected) {
      return this.i18n.t('consultation.ai.reconnectingSimple');
    }
    if (this.state().connected) {
      return this.i18n.t(
        'consultation.ai.listenNaturally',
        'Écoute en cours... Parlez naturellement',
      );
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
    if (this.pipeline.durableBlocked()) {
      return this.i18n.t('consultation.ai.realtimeDurableIntakeBlocked');
    }
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
      // The bridge owns its bounded reconnect policy. Do not cancel it here.
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

  private applyRealtimeResponseLocally(response: AiMessageResponse): void {
    if (!this.session) return;
    this.session = {
      ...this.session,
      expiresAt: response.expiresAt,
      draft: response.draft,
      transcript: response.transcript ?? this.session.transcript,
      transcriptStatus: response.transcript ? 'ANALYZED' : this.session.transcriptStatus,
      conversation: response.conversation,
      clarifications: response.clarifications,
      revisions: response.revisions,
      assistantMessage: response.assistantMessage,
      needsClarification: response.needsClarification,
    };
  }

  private applyRealtimeReviewLocally(response: AiTranscriptionResponse): void {
    if (!this.session) return;
    this.session = {
      ...this.session,
      pendingTranscript: response.transcript,
      transcriptStatus: response.status,
      expiresAt: response.expiresAt,
    };
  }

  private startTimer(): void {
    this.stopTimer();
    this.durationSeconds.set(0);
    this.durationTimer = setInterval(() => {
      if (this.state().connected && !this.manualMuted) {
        this.durationSeconds.update(s => s + 1);
      }
    }, 1000);
  }

  private stopTimer(): void {
    if (this.durationTimer) {
      clearInterval(this.durationTimer);
      this.durationTimer = null;
    }
  }

  requestFinish(): void {
    if (this.finishPending() || this.destroyed) return;
    this.finishPending.set(true);
    this.voicePlayback.stop();
    this.syncMute();
    this.pipeline.resume();
    this.tryCompleteFinish();
  }

  requestTranscriptCorrection(correction: string): void {
    const normalized = correction.trim();
    if (!normalized || this.finishPending() || this.blocked || this.pipeline.processing()) return;
    this.transcriptHistory.update(history => history.map((entry, index) =>
      index === history.length - 1 ? { ...entry, text: normalized } : entry));
    this.pipeline.submitManualCorrection(normalized);
  }

  private tryCompleteFinish(): void {
    if (!this.finishPending() || this.finishEmitted || this.destroyed || !this.pipeline.isIdle()) return;
    const hasPendingReview = !!this.session?.pendingTranscript;
    const hasPendingRevision = this.session?.revisions.some(item => item.status === 'PENDING') ?? false;
    const hasPendingClarification = this.session?.clarifications.some(item => item.status === 'PENDING') ?? false;
    if (this.blocked || hasPendingReview || hasPendingRevision || hasPendingClarification) return;
    this.finishEmitted = true;
    this.endSession.emit();
  }

  private playAssistantResponse(response: AiMessageResponse): void {
    const pendingQuestion = response.clarifications
      .find(item => item.status === 'PENDING')
      ?.question
      ?.trim();
    this.voicePlayback.play(pendingQuestion || response.assistantMessage?.trim() || '');
  }

  private playPendingClarification(): void {
    const pendingQuestion = this.session?.clarifications
      .find(item => item.status === 'PENDING')
      ?.question
      ?.trim();
    this.voicePlayback.play(pendingQuestion || '');
  }

  private syncMute(): void {
    this.bridge.setMuted(
      this.manualMuted
      || this.finishPending()
      || this.pipeline.durableBlocked(),
    );
  }
}
