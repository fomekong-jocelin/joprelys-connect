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
      <section class="relative min-h-[560px] overflow-hidden rounded-[8px] bg-slate-950 text-white shadow-xl sm:min-h-[620px]">
        <div class="pointer-events-none absolute left-1/2 top-16 h-72 w-72 -translate-x-1/2 rounded-full bg-cyan-500/10 blur-3xl"></div>
        <div class="pointer-events-none absolute bottom-8 left-1/2 h-52 w-52 -translate-x-1/2 rounded-full bg-emerald-500/5 blur-3xl"></div>

        <div class="relative flex min-h-[560px] flex-col px-4 pb-5 pt-4 sm:min-h-[620px] sm:px-8 sm:pb-7 sm:pt-6">
          <div class="flex items-center justify-between gap-3">
            <div class="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-[11px] font-bold text-slate-200 backdrop-blur">
              <span class="h-2 w-2 rounded-full" [ngClass]="statusDotClasses()"></span>
              {{ state().connected
                ? i18n.t('consultation.ai.focusSecureListening', 'Écoute sécurisée')
                : state().connecting
                  ? i18n.t('consultation.ai.focusConnecting', 'Connexion du microphone')
                  : i18n.t('consultation.ai.focusAudioUnavailable', 'Audio non confirmé') }}
            </div>

            @if (ambientState().active) {
              <span class="text-[10px] font-semibold text-slate-400">
                {{ i18n.t('consultation.ai.focusSafetyActive', 'Sauvegarde audio active') }}
              </span>
            }
          </div>

          <div class="flex flex-1 flex-col items-center justify-center py-8 text-center sm:py-10">
            <div class="relative flex h-44 w-44 items-center justify-center sm:h-52 sm:w-52">
              @if (state().userSpeaking || state().assistantSpeaking) {
                <span class="absolute inset-0 animate-ping rounded-full border border-cyan-300/20"></span>
                <span class="absolute inset-4 animate-pulse rounded-full border border-cyan-300/25"></span>
              }
              <div class="absolute inset-2 rounded-full border border-white/5 bg-white/[0.02]"></div>
              <div class="absolute inset-7 rounded-full border border-white/10 bg-white/[0.03]"></div>
              <div
                class="relative flex h-28 w-28 items-center justify-center rounded-full shadow-2xl transition-all duration-300 sm:h-32 sm:w-32"
                [ngClass]="orbClasses()"
              >
                @if (state().assistantSpeaking) {
                  <svg class="h-12 w-12 text-white sm:h-14 sm:w-14" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7" d="M8 9h.01M16 9h.01M9 15h6m5-3a8 8 0 10-16 0 8 8 0 0016 0z" />
                  </svg>
                } @else {
                  <svg class="h-12 w-12 text-white sm:h-14 sm:w-14" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.7" d="M12 18.75a6 6 0 006-6v-1.5m-12 0v1.5a6 6 0 006 6m0 0v3m-3 0h6M12 15.75a3 3 0 003-3V6a3 3 0 10-6 0v6.75a3 3 0 003 3z" />
                  </svg>
                }
              </div>
            </div>

            <h2 class="mt-6 text-2xl font-black tracking-tight text-white sm:text-3xl">
              {{ statusLabel() }}
            </h2>
            <p class="mt-2 max-w-lg text-sm leading-6 text-slate-400">
              {{ statusHelp() }}
            </p>

            <div class="mt-6 w-full max-w-2xl rounded-[8px] border border-white/10 bg-white/[0.04] px-4 py-4 text-left backdrop-blur sm:px-5">
              <div class="flex items-center justify-between gap-3">
                <p class="text-[10px] font-extrabold uppercase tracking-[0.16em] text-slate-500">
                  {{ i18n.t('consultation.ai.focusTranscriptTitle', 'Transcription en direct') }}
                </p>
                @if (processing()) {
                  <span class="text-[10px] font-bold text-cyan-300">
                    {{ i18n.t('consultation.ai.focusSecuring', 'Sécurisation…') }}
                  </span>
                }
              </div>

              @if (lastTranscript()) {
                <p class="mt-2 text-sm font-medium leading-6 text-slate-100 sm:text-base">
                  “{{ lastTranscript() }}”
                </p>
                @if (lastTranscriptConfidence() === null) {
                  <p class="mt-2 text-[10px] leading-4 text-amber-300/90">
                    {{ i18n.t('consultation.ai.focusTranscriptUnverified', 'Texte entendu mais confiance non vérifiable : il reste visible sans être utilisé automatiquement comme fait clinique.') }}
                  </p>
                }
              } @else if (state().userSpeaking) {
                <p class="mt-2 text-sm font-semibold text-cyan-100">
                  {{ i18n.t('consultation.ai.focusTranscribing', 'Voix détectée — transcription en cours…') }}
                </p>
              } @else {
                <p class="mt-2 text-sm leading-6 text-slate-500">
                  {{ state().connected
                    ? i18n.t('consultation.ai.focusTranscriptWaiting', 'Parlez normalement. La dernière phrase réellement reconnue apparaîtra ici.')
                    : i18n.t('consultation.ai.focusTranscriptNoChannel', 'La transcription commencera dès que le canal audio sera confirmé.') }}
                </p>
              }
            </div>
          </div>

          @if (showSafetyAlert()) {
            <div
              class="mb-5 rounded-[6px] border px-3 py-2.5 text-xs font-bold"
              [ngClass]="safetyAlertClasses()"
            >
              {{ safetyAlertText() }}
            </div>
          }

          <div class="flex items-end justify-center gap-10 sm:gap-14">
            <div class="flex flex-col items-center gap-2">
              <button
                type="button"
                (click)="toggleMute()"
                [disabled]="!canToggleMute()"
                class="inline-flex h-14 w-14 items-center justify-center rounded-full border border-white/10 bg-white/10 text-white shadow-lg transition hover:bg-white/15 disabled:cursor-not-allowed disabled:opacity-40 sm:h-16 sm:w-16"
                [attr.aria-label]="manualMuted ? i18n.t('consultation.ai.resumeListening', 'Reprendre') : i18n.t('consultation.ai.pauseListening', 'Pause')"
              >
                @if (manualMuted) {
                  <svg class="h-6 w-6" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M8 5v14l11-7z" />
                  </svg>
                } @else {
                  <svg class="h-6 w-6" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M6 5h4v14H6zm8 0h4v14h-4z" />
                  </svg>
                }
              </button>
              <span class="text-[11px] font-semibold text-slate-400">
                {{ manualMuted
                  ? i18n.t('consultation.ai.resumeListening', 'Reprendre')
                  : i18n.t('consultation.ai.pauseListeningShort', 'Pause') }}
              </span>
            </div>

            <div class="flex flex-col items-center gap-2">
              <button
                type="button"
                (click)="endSession.emit()"
                class="inline-flex h-14 w-14 items-center justify-center rounded-full bg-rose-500 text-white shadow-lg shadow-rose-950/30 transition hover:bg-rose-400 sm:h-16 sm:w-16"
                [attr.aria-label]="i18n.t('consultation.ai.finishListening', 'Terminer')"
              >
                <span class="h-5 w-5 rounded-[4px] bg-white"></span>
              </button>
              <span class="text-[11px] font-semibold text-slate-400">
                {{ i18n.t('consultation.ai.finishListening', 'Terminer') }}
              </span>
            </div>
          </div>

          <div class="mt-5 text-center">
            <button
              type="button"
              (click)="switchToDictation.emit()"
              [disabled]="processing()"
              class="text-xs font-semibold text-slate-500 underline decoration-slate-700 underline-offset-4 transition hover:text-slate-300 disabled:opacity-40"
            >
              {{ i18n.t('consultation.ai.switchToDictation', 'Passer en dictée') }}
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
    if (this.manualMuted) return this.i18n.t('consultation.ai.listeningPaused', 'En pause');
    if (this.state().assistantSpeaking) return this.i18n.t('consultation.ai.assistantSpeakingSimple', 'Joprelys répond');
    if (this.state().userSpeaking) return this.i18n.t('consultation.ai.focusHearingYou', 'Je vous entends');
    if (this.backlogPaused) return this.i18n.t('consultation.ai.realtimeCatchingUp', 'Un instant…');
    if (this.state().connecting) return this.i18n.t('consultation.ai.connectingAudio', 'Connexion audio…');
    if (this.state().connected) return this.i18n.t('consultation.ai.simpleListening', 'Je vous écoute');
    if (this.ambientState().active) return this.i18n.t('consultation.ai.reconnectingSimple', 'Reconnexion audio…');
    return this.i18n.t('consultation.ai.audioUnavailableSimple', 'Audio indisponible');
  }

  statusHelp(): string {
    if (this.manualMuted) {
      return this.i18n.t('consultation.ai.pausedHelp', 'L’écoute est arrêtée. Appuyez sur Reprendre pour continuer.');
    }
    if (this.state().assistantSpeaking) {
      return this.i18n.t('consultation.ai.assistantSpeakingHelp', 'Écoutez Joprelys puis répondez naturellement.');
    }
    if (this.state().userSpeaking) {
      return this.i18n.t('consultation.ai.focusHearingHelp', 'Votre voix est détectée par le canal Realtime. Continuez à parler normalement.');
    }
    if (this.backlogPaused) {
      return this.i18n.t('consultation.ai.catchingUpHelp', 'Joprelys sécurise les dernières secondes avant de reprendre automatiquement.');
    }
    if (this.state().connected) {
      return this.i18n.t('consultation.ai.simpleListeningHelp', 'Parlez naturellement avec le patient. La transcription apparaît dès qu’une phrase est finalisée.');
    }
    if (this.ambientState().active) {
      return this.i18n.t('consultation.ai.reconnectingProtectedHelp', 'Votre consultation reste protégée pendant la reconnexion.');
    }
    return this.i18n.t('consultation.ai.audioUnavailableHelp', 'Ne poursuivez pas la consultation vocale avant le rétablissement de l’audio.');
  }

  statusDotClasses(): string {
    if (this.manualMuted) return 'bg-slate-500';
    if (this.state().connected && !this.backlogPaused) return this.state().userSpeaking ? 'animate-pulse bg-emerald-300' : 'bg-emerald-400';
    if (this.state().connecting || this.ambientState().active || this.backlogPaused) return 'animate-pulse bg-amber-400';
    return 'bg-rose-400';
  }

  orbClasses(): string {
    if (this.manualMuted) return 'bg-slate-700 shadow-slate-950/50';
    if (this.state().assistantSpeaking) {
      return 'animate-pulse bg-gradient-to-br from-indigo-400 via-violet-500 to-purple-600 shadow-violet-950/50';
    }
    if (this.state().userSpeaking) {
      return 'animate-pulse bg-gradient-to-br from-cyan-300 via-cyan-500 to-emerald-500 shadow-cyan-950/50';
    }
    if (this.state().connected && !this.backlogPaused) {
      return 'bg-gradient-to-br from-cyan-400 via-cyan-600 to-blue-700 shadow-cyan-950/50';
    }
    if (this.state().connecting || this.ambientState().active || this.backlogPaused) {
      return 'animate-pulse bg-gradient-to-br from-amber-300 via-amber-500 to-orange-600 shadow-amber-950/50';
    }
    return 'bg-gradient-to-br from-rose-400 to-rose-700 shadow-rose-950/50';
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
      return 'border-rose-400/30 bg-rose-500/10 text-rose-100';
    }
    return 'border-amber-300/20 bg-amber-400/10 text-amber-100';
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

    // Le transcript reçu est d'abord rendu visible. Les règles de confiance
    // décident ensuite s'il peut alimenter le journal clinique automatique.
    this.lastTranscript.set(text);
    this.lastTranscriptConfidence.set(turn.confidence);

    if (turn.confidence === null || !Number.isFinite(turn.confidence)) {
      this.realtimeError.emit(this.i18n.t(
        'consultation.ai.realtimeTranscriptUnverified',
        'Une phrase a été entendue mais sa confiance est trop incertaine pour être utilisée cliniquement. L’audio protégé est conservé.',
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
