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

@Component({
  selector: 'app-realtime-voice-controller',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (enabled) {
      <section class="ui-card overflow-hidden">
        <header class="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--app-border)] bg-[var(--app-surface-muted)] px-4 py-3 sm:px-5">
          <div class="inline-flex items-center gap-2 rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface)] px-2.5 py-1.5 text-[11px] font-bold text-[var(--text-secondary)]">
            <span class="h-2 w-2 rounded-full" [ngClass]="statusDotClasses()" aria-hidden="true"></span>
            {{ state().connected
              ? i18n.t('consultation.ai.focusSecureListening')
              : state().connecting
                ? i18n.t('consultation.ai.focusConnecting')
                : i18n.t('consultation.ai.focusAudioUnavailable') }}
          </div>

          @if (ambientState().active) {
            <span class="text-[10px] font-semibold text-[var(--text-muted)]">
              {{ i18n.t('consultation.ai.focusSafetyActive') }}
            </span>
          }
        </header>

        <div class="p-4 sm:p-5">
          <div class="flex flex-col items-center text-center" aria-live="polite">
            <div class="relative flex h-32 w-32 items-center justify-center sm:h-36 sm:w-36">
              @if (state().userSpeaking || state().assistantSpeaking) {
                <span class="absolute inset-0 animate-pulse rounded-full border border-[var(--brand-primary-border)]"></span>
              }
              <div class="absolute inset-3 rounded-full border border-[var(--app-border)] bg-[var(--app-surface-muted)]"></div>
              <div
                class="relative flex h-20 w-20 items-center justify-center rounded-full border border-[var(--app-border)] transition-colors duration-200 sm:h-24 sm:w-24"
                [ngClass]="orbClasses()"
              >
                @if (state().assistantSpeaking) {
                  <svg class="h-9 w-9 sm:h-10 sm:w-10" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M8 9h.01M16 9h.01M9 15h6m5-3a8 8 0 10-16 0 8 8 0 0016 0z" />
                  </svg>
                } @else {
                  <svg class="h-9 w-9 sm:h-10 sm:w-10" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M12 18.75a6 6 0 006-6v-1.5m-12 0v1.5a6 6 0 006 6m0 0v3m-3 0h6M12 15.75a3 3 0 003-3V6a3 3 0 10-6 0v6.75a3 3 0 003 3z" />
                  </svg>
                }
              </div>
            </div>

            <h2 class="ui-title mt-3 text-lg sm:text-xl">{{ statusLabel() }}</h2>
            <p class="mt-1 max-w-xl text-xs leading-5 text-[var(--text-muted)] sm:text-sm">
              {{ statusHelp() }}
            </p>

            <div class="ui-card-muted mt-4 w-full max-w-2xl p-3 text-left sm:p-4">
              <div class="flex items-center justify-between gap-3">
                <p class="ui-label">{{ i18n.t('consultation.ai.focusTranscriptTitle') }}</p>
                @if (processing()) {
                  <span class="text-[10px] font-bold text-[var(--brand-primary)]">
                    {{ i18n.t('consultation.ai.focusSecuring') }}
                  </span>
                }
              </div>

              @if (lastTranscript()) {
                <p class="mt-2 text-sm font-medium leading-6 text-[var(--text-primary)]">
                  “{{ lastTranscript() }}”
                </p>
                @if (lastTranscriptConfidence() === null) {
                  <p class="mt-2 text-[10px] leading-4 text-[var(--brand-warning-text)]">
                    {{ i18n.t('consultation.ai.focusTranscriptUnverified') }}
                  </p>
                }
              } @else if (state().userSpeaking) {
                <p class="mt-2 text-sm font-semibold text-[var(--brand-primary)]">
                  {{ i18n.t('consultation.ai.focusTranscribing') }}
                </p>
              } @else {
                <p class="mt-2 text-xs leading-5 text-[var(--text-muted)] sm:text-sm">
                  {{ state().connected
                    ? i18n.t('consultation.ai.focusTranscriptWaiting')
                    : i18n.t('consultation.ai.focusTranscriptNoChannel') }}
                </p>
              }
            </div>
          </div>

          @if (showSafetyAlert()) {
            <div
              class="mt-4 rounded-[var(--radius-brand-sm)] border px-3 py-2.5 text-xs font-semibold"
              [ngClass]="safetyAlertClasses()"
              role="status"
            >
              {{ safetyAlertText() }}
            </div>
          }

          <div class="mx-auto mt-4 grid w-full max-w-md grid-cols-2 gap-2">
            <button
              type="button"
              (click)="toggleMute()"
              [disabled]="!canToggleMute()"
              class="ui-button ui-button-secondary min-h-11 w-full"
            >
              @if (manualMuted) {
                <svg class="h-4 w-4" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M8 5v14l11-7z" />
                </svg>
                {{ i18n.t('consultation.ai.resumeListening') }}
              } @else {
                <svg class="h-4 w-4" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M6 5h4v14H6zm8 0h4v14h-4z" />
                </svg>
                {{ i18n.t('consultation.ai.pauseListeningShort') }}
              }
            </button>

            <button
              type="button"
              (click)="endSession.emit()"
              class="ui-button ui-button-primary min-h-11 w-full"
            >
              <span class="h-3 w-3 rounded-[2px] bg-[var(--text-inverse)]" aria-hidden="true"></span>
              {{ i18n.t('consultation.ai.finishListening') }}
            </button>
          </div>

          <div class="mt-3 text-center">
            <button
              type="button"
              (click)="switchToDictation.emit()"
              [disabled]="processing()"
              class="ui-link text-xs disabled:cursor-not-allowed disabled:opacity-50"
            >
              {{ i18n.t('consultation.ai.switchToDictation') }}
            </button>
          </div>
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
  @Input() blocked = false;
  @Output() readonly message = new EventEmitter<AiMessageResponse>();
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
  private readonly transcriptQueue: RealtimeTranscriptTurn[] = [];
  private readonly seenTranscriptIds = new Set<string>();
  private readonly seenTranscriptOrder: string[] = [];
  manualMuted = false;
  backlogPaused = false;
  private lastSpokenMessage = '';
  private connectingForVisit = '';
  private connectedVisitId = '';
  private connectionGeneration = 0;
  private connectionTransition: Promise<void> = Promise.resolve();
  private retryTimer: ReturnType<typeof setTimeout> | null = null;
  private destroyed = false;

  constructor() {
    this.subscriptions.add(this.bridge.state$.subscribe(state => {
      const wasConnected = this.state().connected;
      this.state.set(state);
      this.activeChange.emit(state.connected);
      if (state.connected && !wasConnected) {
        this.syncMute();
        this.speakPendingClarification();
        this.drainTranscriptQueue();
      }
    }));
    this.subscriptions.add(
      this.ambientCapture.state$.subscribe(state => this.ambientState.set(state)),
    );
    this.subscriptions.add(
      this.bridge.transcript$.subscribe(turn => this.enqueueTranscript(turn)),
    );
    this.subscriptions.add(
      this.bridge.error$.subscribe(message => this.realtimeError.emit(message)),
    );
    this.subscriptions.add(this.bridge.assistantTurnCompleted$.subscribe(() => {
      setTimeout(() => this.syncMute(), 120);
    }));
  }

  ngOnChanges(changes: SimpleChanges): void {
    const visitChanged = !!changes['visitId']
      && !changes['visitId'].firstChange
      && changes['visitId'].previousValue !== changes['visitId'].currentValue;
    if (visitChanged) this.resetTranscriptPipeline();
    if (changes['enabled'] || changes['visitId'] || changes['session']) {
      this.queueConnectionSync(visitChanged);
    }
    if (changes['blocked']) {
      this.syncMute();
      if (!this.blocked) this.drainTranscriptQueue();
    }
    if (changes['session'] && this.state().connected && this.connectedVisitId === this.visitId) {
      this.speakPendingClarification();
    }
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.connectionGeneration += 1;
    this.clearRetry();
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
    if (this.blocked) return false;
    if (this.manualMuted) return true;
    return this.state().connected && this.connectedVisitId === this.visitId.trim();
  }

  effectiveMuted(): boolean {
    return this.blocked || this.manualMuted || this.backlogPaused || this.state().muted;
  }

  statusLabel(): string {
    if (this.manualMuted) return this.i18n.t('consultation.ai.listeningPaused');
    if (this.state().assistantSpeaking) return this.i18n.t('consultation.ai.assistantSpeakingSimple');
    if (this.state().userSpeaking) return this.i18n.t('consultation.ai.focusHearingYou');
    if (this.backlogPaused) return this.i18n.t('consultation.ai.realtimeCatchingUp');
    if (this.state().connecting) return this.i18n.t('consultation.ai.connectingAudio');
    if (this.state().connected) return this.i18n.t('consultation.ai.simpleListening');
    if (this.ambientState().active) return this.i18n.t('consultation.ai.reconnectingSimple');
    return this.i18n.t('consultation.ai.audioUnavailableSimple');
  }

  statusHelp(): string {
    if (this.manualMuted) return this.i18n.t('consultation.ai.pausedHelp');
    if (this.state().assistantSpeaking) return this.i18n.t('consultation.ai.assistantSpeakingHelp');
    if (this.state().userSpeaking) return this.i18n.t('consultation.ai.focusHearingHelp');
    if (this.backlogPaused) return this.i18n.t('consultation.ai.catchingUpHelp');
    if (this.state().connected) return this.i18n.t('consultation.ai.simpleListeningHelp');
    if (this.ambientState().active) return this.i18n.t('consultation.ai.reconnectingProtectedHelp');
    return this.i18n.t('consultation.ai.audioUnavailableHelp');
  }

  statusDotClasses(): string {
    if (this.manualMuted) return 'bg-[var(--text-muted)]';
    if (this.state().connected && !this.backlogPaused) {
      return this.state().userSpeaking
        ? 'animate-pulse bg-[var(--brand-success)]'
        : 'bg-[var(--brand-success)]';
    }
    if (this.state().connecting || this.ambientState().active || this.backlogPaused) {
      return 'animate-pulse bg-[var(--brand-warning)]';
    }
    return 'bg-[var(--brand-danger)]';
  }

  orbClasses(): string {
    if (this.manualMuted) {
      return 'bg-[var(--app-surface-muted)] text-[var(--text-muted)]';
    }
    if (this.state().assistantSpeaking) {
      return 'animate-pulse bg-[var(--brand-primary-active)] text-[var(--text-inverse)]';
    }
    if (this.state().userSpeaking) {
      return 'animate-pulse bg-[var(--brand-success)] text-[var(--text-inverse)]';
    }
    if (this.state().connected && !this.backlogPaused) {
      return 'bg-[var(--brand-primary)] text-[var(--text-inverse)]';
    }
    if (this.state().connecting || this.ambientState().active || this.backlogPaused) {
      return 'animate-pulse bg-[var(--brand-warning)] text-[var(--text-inverse)]';
    }
    return 'bg-[var(--brand-danger)] text-[var(--text-inverse)]';
  }

  showSafetyAlert(): boolean {
    if (this.manualMuted) return false;
    return this.ambientState().storagePressure
      || (!this.ambientState().active && !this.ambientState().starting && !this.state().connected)
      || (!this.state().connected && this.ambientState().active);
  }

  safetyAlertText(): string {
    if (this.ambientState().storagePressure) {
      return this.i18n.t('consultation.ai.storageCriticalSimple');
    }
    if (!this.state().connected && this.ambientState().active) {
      return this.i18n.t('consultation.ai.realtimeProtectedReconnectSimple');
    }
    return this.i18n.t('consultation.ai.audioProtectionUnavailableSimple');
  }

  safetyAlertClasses(): string {
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
      this.drainTranscriptQueue();
      return;
    }

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
      this.drainTranscriptQueue();
    } catch (error) {
      if (generation !== this.connectionGeneration || this.destroyed) return;
      this.connectedVisitId = '';
      this.bridge.disconnect();
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

    if (turn.confidence === null || !Number.isFinite(turn.confidence)) {
      this.realtimeError.emit(this.i18n.t('consultation.ai.realtimeTranscriptUnverified'));
      return;
    }
    const eventId = turn.eventId?.trim();
    if (!eventId) {
      this.realtimeError.emit(this.i18n.t('consultation.ai.realtimeTranscriptUnverified'));
      return;
    }

    const dedupeId = turn.itemId?.trim() ? `item:${turn.itemId.trim()}` : `event:${eventId}`;
    if (this.seenTranscriptIds.has(dedupeId)) return;
    this.rememberTranscriptId(dedupeId);
    this.transcriptQueue.push({ ...turn, transcript: text, eventId });

    if (this.transcriptQueue.length >= HIGH_WATER_MARK && !this.backlogPaused) {
      this.backlogPaused = true;
      this.syncMute();
      this.realtimeError.emit(this.i18n.t('consultation.ai.realtimeBackpressure'));
    }
    this.drainTranscriptQueue();
  }

  private drainTranscriptQueue(): void {
    if (this.processing()
      || this.blocked
      || this.manualMuted
      || !this.session
      || !this.enabled
      || !this.state().connected
      || !this.connectedVisitId
      || this.connectedVisitId !== this.visitId.trim()) {
      return;
    }
    const turn = this.transcriptQueue[0];
    if (!turn || turn.confidence === null || !turn.eventId) {
      this.releaseBackpressureIfPossible();
      return;
    }

    const transcriptVisitId = this.connectedVisitId;
    this.processing.set(true);
    this.intake.ingest(
      transcriptVisitId,
      turn.transcript,
      turn.confidence,
      turn.eventId,
      turn.itemId,
    ).subscribe({
      next: () => {
        if (transcriptVisitId !== this.visitId.trim() || transcriptVisitId !== this.connectedVisitId) {
          this.processing.set(false);
          return;
        }
        this.analyzeDurableRealtimeTurn(transcriptVisitId, turn);
      },
      error: error => {
        if (transcriptVisitId !== this.visitId.trim() || transcriptVisitId !== this.connectedVisitId) {
          this.processing.set(false);
          return;
        }
        this.processing.set(false);
        const reason = this.backendReason(error);
        if (reason === 'AI_TRANSCRIPTION_LOW_CONFIDENCE' || reason === 'AI_REALTIME_TRANSCRIPTION_UNVERIFIED') {
          this.transcriptQueue.shift();
          this.realtimeError.emit(this.i18n.t('consultation.ai.realtimeTranscriptLowConfidence'));
          this.drainTranscriptQueue();
          return;
        }
        if (this.isTransient(error)) {
          this.scheduleRetry();
          return;
        }
        this.transcriptQueue.shift();
        this.realtimeError.emit(this.i18n.t('consultation.ai.realtimeClinicalError'));
        this.releaseBackpressureIfPossible();
        this.syncMute();
        this.drainTranscriptQueue();
      },
    });
  }

  private analyzeDurableRealtimeTurn(
    transcriptVisitId: string,
    turn: RealtimeTranscriptTurn,
  ): void {
    if (turn.confidence === null || !turn.eventId) {
      this.finishTurnWithoutAnalysis();
      return;
    }

    const pendingClarification = this.session?.clarifications
      .find(item => item.status === 'PENDING');
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

    request.subscribe({
      next: response => {
        if (transcriptVisitId !== this.visitId.trim() || transcriptVisitId !== this.connectedVisitId) {
          this.processing.set(false);
          return;
        }

        this.transcriptQueue.shift();
        this.processing.set(false);
        this.applyRealtimeResponseLocally(response);
        this.message.emit(response);

        const pendingQuestion = response.clarifications
          .find(item => item.status === 'PENDING')
          ?.question
          ?.trim();
        const approvedVoice = pendingQuestion || response.assistantMessage?.trim() || '';
        if (approvedVoice) this.speakApproved(approvedVoice);

        const requiresDecision = response.revisions
          .some(revision => revision.status === 'PENDING');
        this.releaseBackpressureIfPossible();
        if (requiresDecision) {
          this.bridge.setMuted(true);
          return;
        }
        this.syncMute();
        this.drainTranscriptQueue();
      },
      error: () => {
        if (transcriptVisitId !== this.visitId.trim() || transcriptVisitId !== this.connectedVisitId) {
          this.processing.set(false);
          return;
        }

        this.transcriptQueue.shift();
        this.processing.set(false);
        this.realtimeError.emit(this.i18n.t('consultation.ai.realtimeConversationAnalysisFailed'));
        this.releaseBackpressureIfPossible();
        this.syncMute();
        this.drainTranscriptQueue();
      },
    });
  }

  private finishTurnWithoutAnalysis(): void {
    this.transcriptQueue.shift();
    this.processing.set(false);
    this.releaseBackpressureIfPossible();
    this.syncMute();
    this.drainTranscriptQueue();
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

  private rememberTranscriptId(id: string): void {
    this.seenTranscriptIds.add(id);
    this.seenTranscriptOrder.push(id);
    while (this.seenTranscriptOrder.length > MAX_SEEN_TRANSCRIPT_IDS) {
      const oldest = this.seenTranscriptOrder.shift();
      if (oldest) this.seenTranscriptIds.delete(oldest);
    }
  }

  private releaseBackpressureIfPossible(): void {
    if (this.backlogPaused && this.transcriptQueue.length <= LOW_WATER_MARK) {
      this.backlogPaused = false;
    }
  }

  private scheduleRetry(): void {
    if (this.retryTimer || this.destroyed) return;
    this.retryTimer = setTimeout(() => {
      this.retryTimer = null;
      this.drainTranscriptQueue();
    }, 1200);
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

  private resetTranscriptPipeline(): void {
    this.clearRetry();
    this.transcriptQueue.length = 0;
    this.seenTranscriptIds.clear();
    this.seenTranscriptOrder.length = 0;
    this.backlogPaused = false;
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
    this.bridge.setMuted(this.blocked || this.manualMuted || this.backlogPaused);
  }
}
