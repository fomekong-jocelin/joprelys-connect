import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
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
  AiConsultationApiService,
  AiMessageResponse,
  AiSessionResponse,
  AiTranscriptionResponse,
} from './ai-consultation-api.service';
import {
  AmbientAudioCaptureService,
  AmbientCaptureState,
} from './ambient-audio-capture.service';
import { RealtimeClinicalIntakeApiService } from './realtime-clinical-intake-api.service';
import {
  RealtimeTranscriptTurn,
  RealtimeVoiceBridgeService,
  RealtimeVoiceState,
} from './realtime-voice-bridge.service';

const HIGH_WATER_MARK = 32;
const LOW_WATER_MARK = 8;
const MAX_SEEN_TRANSCRIPT_IDS = 512;
const RETRY_DELAY_MS = 1200;

interface DurableRealtimeTurn {
  transcript: string;
  confidence: number;
  originalConfidence: number | null;
  eventId: string;
  itemId?: string;
}

@Component({
  selector: 'app-realtime-voice-controller',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (enabled) {
      <section class="ui-card overflow-hidden">
        <header class="flex min-h-12 items-center justify-between gap-3 border-b border-[var(--app-border)] bg-[var(--app-surface-muted)] px-3 py-2.5 sm:px-4">
          <div class="flex min-w-0 items-center gap-2">
            <span class="h-2.5 w-2.5 shrink-0 rounded-full" [ngClass]="statusDotClasses()" aria-hidden="true"></span>
            <div class="min-w-0">
              <p class="truncate text-sm font-bold text-[var(--text-primary)]">{{ statusLabel() }}</p>
              <p class="truncate text-[10px] text-[var(--text-muted)] sm:text-[11px]">{{ statusHelp() }}</p>
            </div>
          </div>
          @if (queuedCount() > 0) {
            <span class="shrink-0 rounded-[var(--radius-brand-xs)] border border-[var(--app-border)] bg-[var(--app-surface)] px-2 py-1 text-[10px] font-bold text-[var(--text-secondary)]">
              {{ queuedCount() }} {{ i18n.t('consultation.ai.realtimeQueued', 'en attente') }}
            </span>
          }
        </header>

        <div class="space-y-3 p-3 sm:p-4">
          <div class="min-h-16 rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] p-3" aria-live="polite">
            <div class="flex items-center justify-between gap-3">
              <p class="ui-label">{{ i18n.t('consultation.ai.focusTranscriptTitle') }}</p>
              @if (processing()) {
                <span class="text-[10px] font-bold text-[var(--brand-primary)]">
                  {{ i18n.t('consultation.ai.processingBackground', 'Traitement en arrière-plan') }}
                </span>
              }
            </div>
            @if (lastTranscript()) {
              <p class="mt-1 line-clamp-3 text-sm leading-5 text-[var(--text-primary)]">“{{ lastTranscript() }}”</p>
              @if (lastTranscriptConfidence() === null) {
                <p class="mt-1 text-[10px] leading-4 text-[var(--brand-warning-text)]">
                  {{ i18n.t('consultation.ai.focusTranscriptUnverified') }}
                </p>
              }
            } @else {
              <p class="mt-1 text-xs leading-5 text-[var(--text-muted)]">
                {{ state().connected
                  ? i18n.t('consultation.ai.focusTranscriptWaiting')
                  : i18n.t('consultation.ai.focusTranscriptNoChannel') }}
              </p>
            }
          </div>

          @if (showSafetyAlert() || backlogPaused) {
            <div
              class="rounded-[var(--radius-brand-sm)] border px-3 py-2 text-xs font-semibold leading-5"
              [ngClass]="connectionBannerClasses()"
              role="status"
            >
              {{ connectionBannerText() }}
            </div>
          }

          <div class="grid grid-cols-2 gap-2">
            <button
              type="button"
              (click)="toggleMute()"
              [disabled]="!canToggleMute()"
              class="ui-button ui-button-secondary min-h-11 w-full"
            >
              {{ manualMuted
                ? i18n.t('consultation.ai.resumeListening')
                : i18n.t('consultation.ai.pauseListeningShort') }}
            </button>
            <button
              type="button"
              (click)="endSession.emit()"
              class="ui-button ui-button-primary min-h-11 w-full"
            >
              {{ i18n.t('consultation.ai.finishListening') }}
            </button>
          </div>

          <button
            type="button"
            (click)="switchToDictation.emit()"
            class="ui-link min-h-10 w-full text-xs"
          >
            {{ i18n.t('consultation.ai.switchToDictation') }}
          </button>
        </div>
      </section>
    }
  `,
})
export class RealtimeVoiceControllerComponent implements OnChanges, OnDestroy {
  private readonly bridge = inject(RealtimeVoiceBridgeService);
  private readonly ambientCapture = inject(AmbientAudioCaptureService);
  private readonly intake = inject(RealtimeClinicalIntakeApiService);
  private readonly consultationApi = inject(AiConsultationApiService);
  readonly i18n = inject(I18nService);

  @Input({ required: true }) visitId = '';
  @Input() session: AiSessionResponse | null = null;
  @Input() enabled = false;
  /** Blocks clinical analysis only. Durable capture must continue. */
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
  readonly processing = signal(false);
  readonly lastTranscript = signal('');
  readonly lastTranscriptConfidence = signal<number | null>(null);

  private readonly subscriptions = new Subscription();
  private readonly intakeQueue: DurableRealtimeTurn[] = [];
  private readonly analysisQueue: DurableRealtimeTurn[] = [];
  private readonly seenTranscriptIds = new Set<string>();
  private readonly seenTranscriptOrder: string[] = [];
  manualMuted = false;
  backlogPaused = false;
  private intakeBusy = false;
  private analysisBusy = false;
  private lastSpokenMessage = '';
  private connectingForVisit = '';
  private connectedVisitId = '';
  private connectionGeneration = 0;
  private pipelineGeneration = 0;
  private fallbackEventSequence = 0;
  private connectionTransition: Promise<void> = Promise.resolve();
  private retryTimer: ReturnType<typeof setTimeout> | null = null;
  private destroyed = false;

  constructor() {
    this.subscriptions.add(this.bridge.state$.subscribe(state => {
      const previous = this.state();
      this.state.set(state);
      if (state.connected && this.enabled && this.session && !this.manualMuted) {
        this.connectedVisitId = this.visitId.trim();
      }
      this.activeChange.emit(state.connected);
      if (state.connected && !previous.connected) {
        this.speakPendingClarification();
        this.drainIntakeQueue();
        this.drainAnalysisQueue();
      }
      if (state.assistantSpeaking !== previous.assistantSpeaking) this.syncMute();
    }));
    this.subscriptions.add(this.ambientCapture.state$.subscribe(state => this.ambientState.set(state)));
    this.subscriptions.add(this.bridge.transcript$.subscribe(turn => this.enqueueTranscript(turn)));
    this.subscriptions.add(this.bridge.error$.subscribe(message => this.realtimeError.emit(message)));
    this.subscriptions.add(this.bridge.assistantTurnCompleted$.subscribe(() => {
      setTimeout(() => {
        this.syncMute();
        this.drainIntakeQueue();
        this.drainAnalysisQueue();
      }, 120);
    }));
  }

  ngOnChanges(changes: SimpleChanges): void {
    const visitChanged = !!changes['visitId']
      && !changes['visitId'].firstChange
      && changes['visitId'].previousValue !== changes['visitId'].currentValue;
    if (visitChanged) this.resetTranscriptPipeline();

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
    if (changes['blocked'] && !this.blocked) {
      this.drainAnalysisQueue();
    }
    if (changes['session'] && this.state().connected && this.connectedVisitId === this.visitId.trim()) {
      this.speakPendingClarification();
      this.drainAnalysisQueue();
    }
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.connectionGeneration += 1;
    this.resetTranscriptPipeline();
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
    return this.manualMuted || this.backlogPaused || this.state().assistantSpeaking || this.state().muted;
  }

  queuedCount(): number {
    return this.intakeQueue.length + this.analysisQueue.length;
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
      return this.i18n.t(
        'consultation.ai.captureContinuesWhileReviewing',
        'La capture continue. Les propositions attendent votre validation.',
      );
    }
    if (this.processing() && this.state().connected) {
      return this.i18n.t(
        'consultation.ai.processingBackgroundHelp',
        'Traitement en arrière-plan — continuez la consultation.',
      );
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
      this.resetTranscriptPipeline();
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
      this.drainIntakeQueue();
      this.drainAnalysisQueue();
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
      this.drainIntakeQueue();
      this.drainAnalysisQueue();
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

  private enqueueTranscript(turn: RealtimeTranscriptTurn): void {
    const text = turn.transcript.trim();
    if (!text
      || this.manualMuted
      || !this.session
      || !this.enabled
      || !this.connectedVisitId
      || this.connectedVisitId !== this.visitId.trim()) {
      return;
    }

    this.lastTranscript.set(text);
    this.lastTranscriptConfidence.set(turn.confidence);

    const confidence = turn.confidence !== null
      && Number.isFinite(turn.confidence)
      && turn.confidence >= 0
      && turn.confidence <= 1
      ? turn.confidence
      : 0;
    const eventId = turn.eventId?.trim() || this.nextFallbackEventId();
    const itemId = turn.itemId?.trim() || undefined;
    const dedupeId = itemId ? `item:${itemId}` : `event:${eventId}`;
    if (this.seenTranscriptIds.has(dedupeId)) return;
    this.rememberTranscriptId(dedupeId);

    this.intakeQueue.push({
      transcript: text,
      confidence,
      originalConfidence: turn.confidence,
      eventId,
      itemId,
    });
    this.applyBackpressureIfNeeded();
    this.drainIntakeQueue();
  }

  private drainIntakeQueue(): void {
    if (this.intakeBusy
      || this.manualMuted
      || !this.session
      || !this.enabled
      || !this.state().connected
      || !this.connectedVisitId
      || this.connectedVisitId !== this.visitId.trim()) {
      return;
    }
    const turn = this.intakeQueue[0];
    if (!turn) return;

    const transcriptVisitId = this.connectedVisitId;
    const generation = this.pipelineGeneration;
    this.intakeBusy = true;
    this.updateProcessing();
    this.intake.ingest(
      transcriptVisitId,
      turn.transcript,
      turn.confidence,
      turn.eventId,
      turn.itemId,
    ).subscribe({
      next: () => {
        if (!this.isCurrentPipeline(transcriptVisitId, generation)) return;
        this.shiftQueue(this.intakeQueue, turn);
        this.intakeBusy = false;
        this.analysisQueue.push(turn);
        this.updateProcessing();
        this.releaseBackpressureIfPossible();
        this.drainIntakeQueue();
        this.drainAnalysisQueue();
      },
      error: error => {
        if (!this.isCurrentPipeline(transcriptVisitId, generation)) return;
        this.intakeBusy = false;
        this.updateProcessing();
        if (this.isTransient(error)) {
          this.scheduleRetry();
          return;
        }
        this.shiftQueue(this.intakeQueue, turn);
        this.realtimeError.emit(this.i18n.t('consultation.ai.realtimeClinicalError'));
        this.releaseBackpressureIfPossible();
        this.drainIntakeQueue();
      },
    });
  }

  private drainAnalysisQueue(): void {
    if (this.analysisBusy
      || this.blocked
      || this.manualMuted
      || !this.session
      || !this.enabled
      || !this.state().connected
      || this.state().assistantSpeaking
      || !this.connectedVisitId
      || this.connectedVisitId !== this.visitId.trim()) {
      return;
    }
    const turn = this.analysisQueue[0];
    if (!turn) return;

    const transcriptVisitId = this.connectedVisitId;
    const generation = this.pipelineGeneration;
    const pendingClarification = this.session.clarifications.find(item => item.status === 'PENDING');
    const request = pendingClarification
      ? this.consultationApi.answerRealtimeClarification(
          transcriptVisitId,
          pendingClarification.id,
          turn.transcript,
          turn.confidence,
          turn.eventId,
        )
      : this.consultationApi.sendRealtimeTranscript(
          transcriptVisitId,
          turn.transcript,
          turn.confidence,
          turn.eventId,
        );

    this.analysisBusy = true;
    this.updateProcessing();
    request.subscribe({
      next: response => {
        if (!this.isCurrentPipeline(transcriptVisitId, generation)) return;
        this.shiftQueue(this.analysisQueue, turn);
        this.analysisBusy = false;
        this.applyRealtimeResponseLocally(response);
        this.message.emit(response);
        this.updateProcessing();
        this.releaseBackpressureIfPossible();

        const pendingQuestion = response.clarifications
          .find(item => item.status === 'PENDING')
          ?.question
          ?.trim();
        const approvedVoice = pendingQuestion || response.assistantMessage?.trim() || '';
        const spoken = approvedVoice ? this.speakApproved(approvedVoice) : false;
        const requiresDecision = response.revisions.some(revision => revision.status === 'PENDING');
        if (!spoken && !requiresDecision) {
          this.syncMute();
          this.drainAnalysisQueue();
        }
      },
      error: error => {
        if (!this.isCurrentPipeline(transcriptVisitId, generation)) return;
        const reason = this.backendReason(error);
        if (reason === 'AI_TRANSCRIPTION_LOW_CONFIDENCE' || reason === 'AI_REALTIME_TRANSCRIPTION_UNVERIFIED') {
          this.stageForHumanReview(transcriptVisitId, turn, generation);
          return;
        }

        this.shiftQueue(this.analysisQueue, turn);
        this.analysisBusy = false;
        this.updateProcessing();
        this.realtimeError.emit(this.i18n.t('consultation.ai.realtimeConversationAnalysisFailed'));
        this.releaseBackpressureIfPossible();
        this.syncMute();
        this.drainAnalysisQueue();
      },
    });
  }

  private stageForHumanReview(
    transcriptVisitId: string,
    turn: DurableRealtimeTurn,
    generation: number,
  ): void {
    this.consultationApi.stageRealtimeTranscript(transcriptVisitId, turn.transcript).subscribe({
      next: response => {
        if (!this.isCurrentPipeline(transcriptVisitId, generation)) return;
        this.shiftQueue(this.analysisQueue, turn);
        this.analysisBusy = false;
        this.updateProcessing();
        this.transcriptionReview.emit(response);
        this.releaseBackpressureIfPossible();
        this.syncMute();
      },
      error: () => {
        if (!this.isCurrentPipeline(transcriptVisitId, generation)) return;
        this.shiftQueue(this.analysisQueue, turn);
        this.analysisBusy = false;
        this.updateProcessing();
        this.realtimeError.emit(this.i18n.t(
          'consultation.ai.realtimeReviewStagingFailed',
          'La phrase est sauvegardée, mais sa revue n’a pas pu être ouverte. Continuez la consultation ; elle reste dans le journal sécurisé.',
        ));
        this.releaseBackpressureIfPossible();
        this.syncMute();
        this.drainAnalysisQueue();
      },
    });
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

  private isCurrentPipeline(visitId: string, generation: number): boolean {
    return !this.destroyed
      && generation === this.pipelineGeneration
      && visitId === this.visitId.trim()
      && visitId === this.connectedVisitId;
  }

  private shiftQueue<T>(queue: T[], item: T): void {
    if (queue[0] === item) queue.shift();
  }

  private nextFallbackEventId(): string {
    this.fallbackEventSequence += 1;
    return `client:${Date.now()}:${this.fallbackEventSequence}`;
  }

  private rememberTranscriptId(id: string): void {
    this.seenTranscriptIds.add(id);
    this.seenTranscriptOrder.push(id);
    while (this.seenTranscriptOrder.length > MAX_SEEN_TRANSCRIPT_IDS) {
      const oldest = this.seenTranscriptOrder.shift();
      if (oldest) this.seenTranscriptIds.delete(oldest);
    }
  }

  private applyBackpressureIfNeeded(): void {
    if (this.queuedCount() < HIGH_WATER_MARK || this.backlogPaused) return;
    this.backlogPaused = true;
    this.syncMute();
    this.realtimeError.emit(this.i18n.t('consultation.ai.realtimeBackpressure'));
  }

  private releaseBackpressureIfPossible(): void {
    if (this.backlogPaused && this.queuedCount() <= LOW_WATER_MARK) {
      this.backlogPaused = false;
      this.syncMute();
    }
  }

  private scheduleRetry(): void {
    if (this.retryTimer || this.destroyed) return;
    this.retryTimer = setTimeout(() => {
      this.retryTimer = null;
      this.drainIntakeQueue();
    }, RETRY_DELAY_MS);
  }

  private clearRetry(): void {
    if (this.retryTimer) clearTimeout(this.retryTimer);
    this.retryTimer = null;
  }

  private isTransient(error: unknown): boolean {
    if (!(error instanceof HttpErrorResponse)) return true;
    return error.status === 0
      || error.status === 408
      || error.status === 425
      || error.status === 429
      || error.status >= 500;
  }

  private backendReason(error: unknown): string {
    if (!error || typeof error !== 'object') return '';
    const payload = (error as { error?: unknown }).error;
    if (payload && typeof payload === 'object') {
      const detail = (payload as { detail?: unknown; title?: unknown }).detail
        ?? (payload as { detail?: unknown; title?: unknown }).title;
      return typeof detail === 'string' ? detail : '';
    }
    return typeof payload === 'string' ? payload : '';
  }

  private updateProcessing(): void {
    this.processing.set(this.intakeBusy || this.analysisBusy);
  }

  private resetTranscriptPipeline(): void {
    this.pipelineGeneration += 1;
    this.clearRetry();
    this.intakeQueue.length = 0;
    this.analysisQueue.length = 0;
    this.seenTranscriptIds.clear();
    this.seenTranscriptOrder.length = 0;
    this.backlogPaused = false;
    this.intakeBusy = false;
    this.analysisBusy = false;
    this.processing.set(false);
    this.lastTranscript.set('');
    this.lastTranscriptConfidence.set(null);
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
