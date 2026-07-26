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
      <section class="rounded-[6px] border border-cyan-200 bg-cyan-50/45 p-4 shadow-sm dark:border-cyan-900 dark:bg-cyan-950/15">
        <div class="flex items-center justify-between gap-3">
          <div class="flex items-center gap-3 min-w-0">
            <span class="relative flex h-3.5 w-3.5 shrink-0 items-center justify-center">
              @if (state().connected && !manualMuted) {
                <span class="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75"></span>
              }
              <span class="relative inline-flex h-3 w-3 rounded-full" [ngClass]="statusDotClasses()"></span>
            </span>
            <div class="min-w-0 flex-1">
              <p class="text-base font-black text-[var(--text-primary)] flex items-center flex-wrap gap-2">
                {{ statusLabel() }}
                @if (state().userSpeaking) {
                  <span class="inline-flex items-center rounded-[4px] bg-emerald-100 px-2 py-0.5 text-[10px] font-extrabold text-emerald-800 dark:bg-emerald-950 dark:text-emerald-200">
                    {{ i18n.t('consultation.ai.userSpeakingBadge', 'Vous parlez') }}
                  </span>
                } @else if (state().assistantSpeaking) {
                  <span class="inline-flex items-center rounded-[4px] bg-indigo-100 px-2 py-0.5 text-[10px] font-extrabold text-indigo-800 dark:bg-indigo-950 dark:text-indigo-200">
                    {{ i18n.t('consultation.ai.assistantSpeakingBadge', 'Joprelys répond') }}
                  </span>
                }
              </p>
              <p class="mt-0.5 text-xs leading-5 text-[var(--text-muted)]">{{ statusHelp() }}</p>
            </div>
          </div>
        </div>

        <!-- Animated Audio Waveform Equalizer -->
        <div class="my-3 flex h-10 items-center justify-center gap-1.5 rounded-[6px] border border-cyan-200/60 bg-slate-900/90 px-4 shadow-inner dark:border-cyan-900/60 dark:bg-slate-950"
             aria-label="Visualisateur d'ondes vocales">
          @for (bar of waveformBars; track $index) {
            <span
              class="w-1.5 rounded-full transition-all duration-150"
              [ngClass]="waveBarClasses($index)"
              [style.height.px]="waveBarHeight($index)"
            ></span>
          }
        </div>

        @if (showSafetyAlert()) {
          <div
            class="mt-3 rounded-[4px] border px-3 py-2 text-xs font-bold"
            [ngClass]="safetyAlertClasses()"
          >
            {{ safetyAlertText() }}
          </div>
        }

        <div class="mt-4 grid grid-cols-1 gap-2 sm:grid-cols-3">
          <button
            type="button"
            (click)="toggleMute()"
            [disabled]="!canToggleMute()"
            class="min-h-11 rounded-[5px] border border-cyan-300 bg-white px-4 py-2 text-sm font-bold text-cyan-800 shadow-sm hover:bg-cyan-50 disabled:cursor-not-allowed disabled:opacity-50 dark:border-cyan-800 dark:bg-slate-950 dark:text-cyan-200"
          >
            {{ manualMuted
              ? i18n.t('consultation.ai.resumeListening', 'Reprendre')
              : i18n.t('consultation.ai.pauseListening', 'Mettre en pause') }}
          </button>

          <button
            type="button"
            (click)="switchToDictation.emit()"
            [disabled]="processing()"
            class="min-h-11 rounded-[5px] border border-[var(--app-border)] bg-[var(--app-surface)] px-4 py-2 text-sm font-bold text-[var(--text-secondary)] hover:bg-[var(--app-surface-muted)] disabled:opacity-50"
          >
            {{ i18n.t('consultation.ai.switchToDictation', 'Passer en dictée') }}
          </button>

          <button
            type="button"
            (click)="endSession.emit()"
            class="min-h-11 rounded-[5px] border border-[var(--app-border)] bg-[var(--app-surface)] px-4 py-2 text-sm font-bold text-[var(--text-secondary)] hover:bg-[var(--app-surface-muted)]"
          >
            {{ i18n.t('consultation.ai.finishListening', 'Terminer') }}
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
  readonly waveformBars = Array.from({ length: 16 });

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
    if (this.manualMuted) return this.i18n.t('consultation.ai.listeningPaused', 'En pause');
    if (this.state().assistantSpeaking) return this.i18n.t('consultation.ai.assistantSpeakingSimple', 'Joprelys parle');
    if (this.backlogPaused) return this.i18n.t('consultation.ai.realtimeCatchingUp', 'Un instant…');
    if (this.state().connecting) return this.i18n.t('consultation.ai.connectingAudio', 'Connexion audio…');
    if (this.state().connected) return this.i18n.t('consultation.ai.simpleListening', 'Je vous écoute');
    if (this.ambientState().active) return this.i18n.t('consultation.ai.reconnectingSimple', 'Reconnexion audio…');
    return this.i18n.t('consultation.ai.audioUnavailableSimple', 'Audio indisponible');
  }

  statusHelp(): string {
    if (this.manualMuted) {
      return this.i18n.t('consultation.ai.pausedHelp', 'Appuyez sur Reprendre lorsque vous souhaitez continuer la consultation.');
    }
    if (this.state().assistantSpeaking) {
      return this.i18n.t('consultation.ai.assistantSpeakingHelp', 'Écoutez la question puis répondez naturellement.');
    }
    if (this.backlogPaused) {
      return this.i18n.t('consultation.ai.catchingUpHelp', 'Joprelys sécurise les dernières secondes avant de reprendre automatiquement.');
    }
    if (this.state().connected) {
      return this.i18n.t('consultation.ai.simpleListeningHelp', 'Parlez naturellement avec le patient. Vous n’avez rien d’autre à faire.');
    }
    if (this.ambientState().active) {
      return this.i18n.t('consultation.ai.reconnectingProtectedHelp', 'Votre consultation reste protégée pendant la reconnexion.');
    }
    return this.i18n.t('consultation.ai.audioUnavailableHelp', 'Ne poursuivez pas la dictée avant le rétablissement de l’audio.');
  }

  statusDotClasses(): string {
    if (this.manualMuted) return 'bg-slate-400';
    if (this.state().connected && !this.backlogPaused) return 'animate-pulse bg-emerald-500';
    if (this.state().connecting || this.ambientState().active || this.backlogPaused) return 'animate-pulse bg-amber-500';
    return 'bg-rose-500';
  }

  waveBarHeight(index: number): number {
    if (this.manualMuted || (!this.state().connected && !this.ambientState().active)) return 6;
    const isUser = this.state().userSpeaking;
    const isAssistant = this.state().assistantSpeaking;
    const active = isUser || isAssistant;
    const sinFactor = 0.3 + Math.abs(Math.sin((index + 1) * 0.75 + (index % 3) * 1.2)) * 0.7;
    const baseHeight = active ? 28 : 14;
    return Math.round(6 + sinFactor * baseHeight);
  }

  waveBarClasses(index: number): string {
    if (this.manualMuted) return 'bg-slate-500/40 dark:bg-slate-600/40';
    if (this.state().userSpeaking) {
      return 'bg-gradient-to-t from-emerald-500 to-cyan-400 shadow-[0_0_8px_rgba(16,185,129,0.6)] animate-pulse';
    }
    if (this.state().assistantSpeaking) {
      return 'bg-gradient-to-t from-indigo-500 to-purple-400 shadow-[0_0_8px_rgba(99,102,241,0.6)] animate-pulse';
    }
    if (this.state().connected && !this.backlogPaused) {
      return 'bg-gradient-to-t from-cyan-500 to-cyan-300 shadow-[0_0_6px_rgba(6,182,212,0.4)] animate-pulse';
    }
    if (this.state().connecting || this.ambientState().active || this.backlogPaused) {
      return 'bg-gradient-to-t from-amber-500 to-yellow-300 animate-pulse';
    }
    return 'bg-rose-500/50';
  }

  showSafetyAlert(): boolean {
    if (this.manualMuted) return false;
    return this.ambientState().storagePressure
      || (!this.ambientState().active && !this.ambientState().starting && !this.state().connected)
      || (!this.state().connected && this.ambientState().active);
  }

  safetyAlertText(): string {
    if (this.ambientState().storagePressure) {
      return this.i18n.t(
        'consultation.ai.storageCriticalSimple',
        'Espace de sécurité presque saturé. Rétablissez la connexion ou libérez de l’espace avant de poursuivre.',
      );
    }
    if (!this.state().connected && this.ambientState().active) {
      return this.i18n.t(
        'consultation.ai.realtimeProtectedReconnectSimple',
        'Temps réel interrompu. La consultation reste enregistrée localement et sera synchronisée automatiquement.',
      );
    }
    return this.i18n.t(
      'consultation.ai.audioProtectionUnavailableSimple',
      'La protection audio n’est pas garantie. Attendez le rétablissement avant de poursuivre.',
    );
  }

  safetyAlertClasses(): string {
    if (this.ambientState().storagePressure || !this.ambientState().active) {
      return 'border-rose-300 bg-rose-50 text-rose-900 dark:border-rose-800 dark:bg-rose-950/30 dark:text-rose-100';
    }
    return 'border-amber-200 bg-amber-50 text-amber-800 dark:border-amber-900 dark:bg-amber-950/20 dark:text-amber-200';
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
            : this.i18n.t(
                'consultation.ai.realtimeUnavailable',
                'Le temps réel est indisponible. Reconnexion automatique en cours.',
              ),
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
      this.realtimeError.emit(
        this.i18n.t(
          'consultation.ai.realtimeUnsupported',
          'Ce navigateur ne prend pas en charge la connexion audio temps réel.',
        ),
      );
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
      throw new Error(this.i18n.t(
        'consultation.ai.ambientRequired',
        'La protection audio n’est pas disponible. Le mode temps réel reste désactivé pour éviter toute perte silencieuse.',
      ));
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
    if (turn.confidence === null || !Number.isFinite(turn.confidence)) {
      this.realtimeError.emit(this.i18n.t(
        'consultation.ai.realtimeTranscriptUnverified',
        'Une phrase est trop incertaine pour être utilisée cliniquement. L’audio protégé est conservé.',
      ));
      return;
    }
    const eventId = turn.eventId?.trim();
    if (!eventId) {
      this.realtimeError.emit(this.i18n.t(
        'consultation.ai.realtimeTranscriptUnverified',
        'Un passage audio n’a pas pu être tracé. Il reste protégé et sera retraité automatiquement.',
      ));
      return;
    }

    const dedupeId = turn.itemId?.trim() ? `item:${turn.itemId.trim()}` : `event:${eventId}`;
    if (this.seenTranscriptIds.has(dedupeId)) return;
    this.rememberTranscriptId(dedupeId);
    this.transcriptQueue.push({ ...turn, transcript: text, eventId });

    if (this.transcriptQueue.length >= HIGH_WATER_MARK && !this.backlogPaused) {
      this.backlogPaused = true;
      this.syncMute();
      this.realtimeError.emit(this.i18n.t(
        'consultation.ai.realtimeBackpressure',
        'Joprelys sécurise les dernières secondes avant de reprendre automatiquement.',
      ));
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
        this.transcriptQueue.shift();
        this.processing.set(false);
        this.releaseBackpressureIfPossible();
        this.syncMute();
        this.drainTranscriptQueue();
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
          this.realtimeError.emit(this.i18n.t(
            'consultation.ai.realtimeTranscriptLowConfidence',
            'Une phrase reste trop incertaine pour être utilisée cliniquement. L’audio protégé est conservé.',
          ));
          this.drainTranscriptQueue();
          return;
        }
        if (this.isTransient(error)) {
          this.scheduleRetry();
          return;
        }
        this.transcriptQueue.shift();
        this.realtimeError.emit(this.i18n.t(
          'consultation.ai.realtimeClinicalError',
          'Un passage n’a pas pu être synchronisé. L’audio protégé reste conservé pour retraitement.',
        ));
        this.releaseBackpressureIfPossible();
        this.syncMute();
        this.drainTranscriptQueue();
      },
    });
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
