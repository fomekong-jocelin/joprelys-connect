import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  inject,
  signal,
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
  RealtimeClinicalTurnCoordinator,
} from './realtime-clinical-turn-coordinator.service';
import {
  RealtimeVoiceBridgeService,
  RealtimeVoiceState,
} from './realtime-voice-bridge.service';

interface TranscriptEntry { text: string; timestamp: number; }

const LOW_CONFIDENCE_REVIEW_FLOOR = 0.35;

@Component({
  selector: 'app-realtime-voice-controller',
  standalone: true,
  imports: [CommonModule],
  providers: [RealtimeClinicalTurnCoordinator],
  templateUrl: './realtime-voice-controller.component.html',
})
export class RealtimeVoiceControllerComponent implements OnChanges, OnDestroy {
  private readonly bridge = inject(RealtimeVoiceBridgeService);
  private readonly ambientCapture = inject(AmbientAudioCaptureService);
  private readonly pipeline = inject(RealtimeClinicalTurnCoordinator);
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
  readonly transcriptHistory = signal<TranscriptEntry[]>([]);

  private readonly subscriptions = new Subscription();
  manualMuted = false;
  private lastSpokenMessage = '';
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
      assistantSpeaking: () => this.state().assistantSpeaking,
      blocked: () => this.blocked,
      onMessage: response => {
        this.applyRealtimeResponseLocally(response);
        this.message.emit(response);
      },
      onReview: response => {
        this.applyRealtimeReviewLocally(response);
        this.transcriptionReview.emit(response);
      },
      onError: error => this.realtimeError.emit(error),
      speakApproved: text => this.speakApproved(text),
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
        this.speakPendingClarification();
        this.pipeline.resume();
      }
      if (state.assistantSpeaking !== previous.assistantSpeaking) this.syncMute();
    }));
    this.subscriptions.add(this.ambientCapture.state$.subscribe(state => this.ambientState.set(state)));
    this.subscriptions.add(this.bridge.transcript$.subscribe(turn => this.pipeline.enqueue(turn)));
    this.subscriptions.add(this.bridge.transcript$.subscribe(turn => { const text = turn.transcript.trim(); if (text) { this.transcriptHistory.update(h => [...h, { text, timestamp: Date.now() }]); } }));
    this.subscriptions.add(this.bridge.error$.subscribe(error => this.realtimeError.emit(error)));
    this.subscriptions.add(this.bridge.assistantTurnCompleted$.subscribe(() => {
      setTimeout(() => {
        this.syncMute();
        this.pipeline.resume();
      }, 120);
    }));
  }

  ngOnChanges(changes: SimpleChanges): void {
    const visitChanged = !!changes['visitId']
      && !changes['visitId'].firstChange
      && changes['visitId'].previousValue !== changes['visitId'].currentValue;
    if (visitChanged) this.pipeline.reset();
    if (visitChanged) this.transcriptHistory.set([]);

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
      this.speakPendingClarification();
      this.pipeline.resume();
    }
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.connectionGeneration += 1;
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
      || this.backlogPaused
      || this.state().assistantSpeaking
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

  statusLabel(): string {
    if (this.manualMuted) return this.i18n.t('consultation.ai.listeningPaused');
    if (this.backlogPaused) return this.i18n.t('consultation.ai.realtimeCatchingUp');
    if (this.state().connecting) return this.i18n.t('consultation.ai.connectingAudio');
    if (this.state().connected) return this.i18n.t('consultation.ai.simpleListening');
    if (this.ambientState().active) return this.i18n.t('consultation.ai.reconnectingSimple');
    return this.i18n.t('consultation.ai.audioUnavailableSimple');
  }

  statusHelp(): string {
    if (this.manualMuted) return this.i18n.t('consultation.ai.pausedHelp');
    if (this.backlogPaused) return this.i18n.t('consultation.ai.catchingUpHelp');
    if (this.blocked && this.state().connected) {
      return this.i18n.t('consultation.ai.captureContinuesWhileReviewing');
    }
    if (this.processing() && this.state().connected) {
      return this.i18n.t('consultation.ai.processingBackgroundHelp');
    }
    if (this.state().connected) return this.i18n.t('consultation.ai.simpleListeningHelp');
    if (this.ambientState().active) return this.i18n.t('consultation.ai.reconnectingProtectedHelp');
    return this.i18n.t('consultation.ai.audioUnavailableHelp');
  }

  statusDotClasses(): string {
    if (this.manualMuted) return 'bg-[var(--text-muted)]';
    if (this.state().connected && !this.backlogPaused) return 'bg-[var(--brand-success)]';
    if (this.state().connecting || this.ambientState().active || this.backlogPaused) {
      return 'bg-[var(--brand-warning)]';
    }
    return 'bg-[var(--brand-danger)]';
  }

  formatTime(ts: number): string { const d = new Date(ts); return d.getHours().toString().padStart(2, '0') + ':' + d.getMinutes().toString().padStart(2, '0'); }

  showSafetyAlert(): boolean {
    if (this.manualMuted) return false;
    return this.ambientState().storagePressure
      || (!this.ambientState().active && !this.ambientState().starting && !this.state().connected)
      || (!this.state().connected && this.ambientState().active);
  }

  connectionBannerText(): string {
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
      this.lastSpokenMessage = '';
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
      const spoken = this.speakPendingClarification();
      if (!spoken) this.syncMute();
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

  private speakPendingClarification(): boolean {
    const pendingQuestion = this.session?.clarifications
      .find(item => item.status === 'PENDING')
      ?.question
      ?.trim();
    return this.speakApproved(pendingQuestion || '');
  }

  private speakApproved(message: string): boolean {
    const text = message.trim();
    if (!text
      || text === this.lastSpokenMessage
      || !this.state().connected
      || this.connectedVisitId !== this.visitId.trim()) {
      return false;
    }
    this.lastSpokenMessage = text;
    const started = this.bridge.speakApproved(text);
    if (!started) this.syncMute();
    return started;
  }

  private syncMute(): void {
    this.bridge.setMuted(this.manualMuted || this.backlogPaused || this.state().assistantSpeaking);
  }
}
