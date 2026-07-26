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
      <div class="rounded-[8px] border border-cyan-200 bg-cyan-50/60 p-3 dark:border-cyan-900 dark:bg-cyan-950/20">
        <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div class="min-w-0">
            <div class="flex items-center gap-2">
              <span class="h-2.5 w-2.5 rounded-full"
                [ngClass]="state().connected ? 'animate-pulse bg-emerald-500' : state().connecting ? 'animate-pulse bg-amber-500' : 'bg-rose-500'">
              </span>
              <p class="text-xs font-black text-[var(--text-primary)]">{{ statusLabel() }}</p>
            </div>
            <p class="mt-1 text-[11px] leading-4 text-[var(--text-muted)]">
              @if (state().connected) {
                {{ i18n.t('consultation.ai.realtimeGovernedHelp', 'Écoute clinique continue. Les tours entendus sont journalisés dans l’ordre ; la capture de sécurité chiffrée reste indépendante.') }}
              } @else if (ambientState().active) {
                {{ i18n.t('consultation.ai.realtimeRecoveryWithSafety', 'Le temps réel est interrompu, mais la capture locale chiffrée continue. Joprelys renverra l’audio après reconnexion.') }}
              } @else if (manualMuted) {
                {{ i18n.t('consultation.ai.microphoneExplicitlyStopped', 'Microphone coupé par le clinicien. Aucune capture audio n’est active.') }}
              } @else {
                {{ i18n.t('consultation.ai.realtimeRecoveryHelp', 'Ne poursuivez pas la dictée : ni le temps réel ni la capture de sécurité ne sont actuellement garantis.') }}
              }
            </p>
          </div>
          <button type="button" (click)="toggleMute()" [disabled]="!canToggleMute()"
            class="inline-flex min-h-11 shrink-0 items-center justify-center gap-2 rounded-[6px] border border-cyan-300 bg-white px-4 py-2 text-xs font-bold text-cyan-800 shadow-sm hover:bg-cyan-50 disabled:cursor-not-allowed disabled:opacity-50 dark:border-cyan-800 dark:bg-slate-950 dark:text-cyan-200">
            <span class="h-2 w-2 rounded-full" [ngClass]="effectiveMuted() ? 'bg-slate-400' : 'animate-pulse bg-rose-500'"></span>
            {{ effectiveMuted() ? i18n.t('consultation.ai.realtimeUnmute', 'Réactiver le micro') : i18n.t('consultation.ai.realtimeMute', 'Couper le micro') }}
          </button>
        </div>

        <div class="mt-3 grid gap-2 sm:grid-cols-2">
          <div class="rounded-[5px] border px-3 py-2 text-[11px]"
            [ngClass]="ambientState().active
              ? 'border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-900 dark:bg-emerald-950/20 dark:text-emerald-200'
              : ambientState().starting || ambientState().recovering
                ? 'border-amber-200 bg-amber-50 text-amber-800 dark:border-amber-900 dark:bg-amber-950/20 dark:text-amber-200'
                : 'border-rose-200 bg-rose-50 text-rose-800 dark:border-rose-900 dark:bg-rose-950/20 dark:text-rose-200'">
            <div class="font-black">{{ ambientStatusLabel() }}</div>
            <div class="mt-0.5 opacity-90">
              @if (!ambientState().online) {
                Audio sécurisé conservé localement · en attente du réseau
              } @else if (ambientState().uploading || ambientState().pendingChunks > 0) {
                Sauvegarde clinique chiffrée · synchronisation en arrière-plan
              } @else {
                Sauvegarde clinique chiffrée · à jour
              }
            </div>
          </div>
          <div class="rounded-[5px] border border-slate-200 bg-white px-3 py-2 text-[11px] text-[var(--text-muted)] dark:border-slate-800 dark:bg-slate-950">
            <div class="font-black text-[var(--text-primary)]">
              {{ i18n.t('consultation.ai.ambientEvidence', 'Preuve clinique ambient') }}
            </div>
            <div class="mt-0.5">
              {{ i18n.t('consultation.ai.ambientEvidenceHelp', 'Audio local chiffré → diarisation → transcript final auditable. Aucun audio non confirmé n’est supprimé.') }}
            </div>
          </div>
        </div>

        @if (ambientState().storagePressure) {
          <div class="mt-2 rounded-[5px] border border-rose-300 bg-rose-50 px-3 py-2 text-[11px] font-bold text-rose-900 dark:border-rose-800 dark:bg-rose-950/30 dark:text-rose-100">
            {{ i18n.t('consultation.ai.ambientStoragePressure', 'Stockage local presque saturé. Joprelys ne supprimera aucun audio non confirmé : rétablissez la connexion ou libérez de l’espace avant de poursuivre.') }}
          </div>
        }

        @if (state().connected) {
          <div class="mt-3 flex h-9 items-center justify-center gap-[3px]" aria-hidden="true">
            @for (bar of bars; track $index) {
              <span class="w-[3px] rounded-full bg-cyan-600/75 transition-all dark:bg-cyan-300/80"
                [class.animate-pulse]="state().userSpeaking || state().assistantSpeaking"
                [style.height.px]="barHeight($index)"></span>
            }
          </div>
          <div class="mt-1 text-center text-[10px] font-bold uppercase tracking-wider text-[var(--text-muted)]">
            @if (blocked || backlogPaused) {
              {{ ambientState().active
                ? i18n.t('consultation.ai.realtimeValidationPauseSafety', 'Écoute Realtime en pause — capture sécurisée continue')
                : i18n.t('consultation.ai.realtimeValidationPause', 'Micro en pause') }}
            } @else if (processing()) {
              {{ i18n.t('consultation.ai.realtimeIntakeSaving', 'Écoute continue — journalisation clinique en cours') }}
            } @else if (state().assistantSpeaking) {
              {{ i18n.t('consultation.ai.realtimeAssistantSpeaking', 'Joprelys vous répond…') }}
            } @else if (state().userSpeaking) {
              {{ i18n.t('consultation.ai.realtimeListening', 'Je vous écoute…') }}
            } @else {
              {{ i18n.t('consultation.ai.realtimeReady', 'Micro en direct — parlez naturellement') }}
            }
          </div>
        } @else {
          <div class="mt-3 rounded-[4px] border border-amber-200 bg-amber-50 px-3 py-2 text-[11px] font-bold text-amber-800 dark:border-amber-900 dark:bg-amber-950/20 dark:text-amber-200">
            {{ manualMuted
              ? i18n.t('consultation.ai.microphoneExplicitlyStopped', 'Microphone coupé par le clinicien. Aucune capture audio n’est active.')
              : state().connecting
                ? i18n.t('consultation.ai.realtimeRecovering', 'Reconnexion audio temps réel en cours…')
                : ambientState().active
                  ? i18n.t('consultation.ai.realtimeDisconnectedSafety', 'Temps réel interrompu — capture chiffrée locale toujours active.')
                  : i18n.t('consultation.ai.realtimeDisconnected', 'Audio temps réel interrompu — n’enregistrez pas tant que la sécurité audio n’est pas rétablie.') }}
          </div>
        }
      </div>
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

  readonly state = signal<RealtimeVoiceState>({ connected: false, connecting: false, userSpeaking: false, assistantSpeaking: false, muted: false });
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
  readonly bars = Array.from({ length: 24 });

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
        this.speakCurrentApprovedTurn();
        this.drainTranscriptQueue();
      }
    }));
    this.subscriptions.add(this.ambientCapture.state$.subscribe(state => this.ambientState.set(state)));
    this.subscriptions.add(this.bridge.transcript$.subscribe(turn => this.enqueueTranscript(turn)));
    this.subscriptions.add(this.bridge.error$.subscribe(message => this.realtimeError.emit(message)));
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
      this.speakCurrentApprovedTurn();
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
    if (this.manualMuted) return this.i18n.t('consultation.ai.microphoneStopped', 'Microphone coupé');
    if (this.state().connecting) return this.i18n.t('consultation.ai.realtimeRecovering', 'Reconnexion audio en cours…');
    if (this.state().connected) return this.i18n.t('consultation.ai.realtimeConnected', 'Copilote Realtime sécurisé');
    return this.i18n.t('consultation.ai.realtimeDisconnected', 'Audio temps réel interrompu');
  }

  ambientStatusLabel(): string {
    const ambient = this.ambientState();
    if (ambient.active) return this.i18n.t('consultation.ai.ambientActive', 'Capture de sécurité active');
    if (ambient.starting) return this.i18n.t('consultation.ai.ambientStarting', 'Initialisation de la capture sécurisée…');
    if (ambient.recovering) return this.i18n.t('consultation.ai.ambientRecovering', 'Récupération de la capture sécurisée…');
    return this.i18n.t('consultation.ai.ambientInactive', 'Capture de sécurité inactive');
  }

  barHeight(index: number): number {
    const active = this.state().userSpeaking || this.state().assistantSpeaking;
    const wave = 0.35 + Math.abs(Math.sin((index + 1) * 0.82)) * 0.65;
    return Math.round(6 + wave * (active ? 25 : 8));
  }

  private queueConnectionSync(forceVisitReset: boolean): void {
    const generation = ++this.connectionGeneration;
    this.connectionTransition = this.connectionTransition
      .catch(() => undefined)
      .then(() => this.syncConnection(generation, forceVisitReset))
      .catch(error => {
        if (generation !== this.connectionGeneration || this.destroyed) return;
        this.realtimeError.emit(error instanceof Error && error.message.trim()
          ? error.message
          : this.i18n.t('consultation.ai.realtimeUnavailable', 'Le temps réel est indisponible. Reconnexion automatique en cours.'));
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
      this.realtimeError.emit(this.i18n.t('consultation.ai.realtimeUnsupported', 'Ce navigateur ne prend pas en charge la connexion audio temps réel.'));
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
      const spoken = this.speakCurrentApprovedTurn();
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
        'La capture audio de sécurité chiffrée n’est pas disponible. Le mode temps réel est désactivé pour éviter une perte silencieuse de consultation.',
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
        'Transcription non vérifiable : Joprelys n’en déduira aucune donnée clinique. Répétez la phrase.',
      ));
      return;
    }
    const eventId = turn.eventId?.trim();
    if (!eventId) {
      this.realtimeError.emit(this.i18n.t(
        'consultation.ai.realtimeTranscriptUnverified',
        'Tour audio non traçable : il reste protégé par la capture de sécurité mais ne sera pas utilisé en Realtime.',
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
        'Joprelys a temporairement mis l’écoute Realtime en pause pour vider la file clinique. La capture chiffrée continue sans perte.',
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
            'La transcription est trop incertaine pour être utilisée cliniquement. L’audio sécurisé est conservé pour la transcription finale.',
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
          'Un tour Realtime n’a pas pu être journalisé. L’audio sécurisé reste conservé et sera traité par la chaîne ambient.',
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

  private speakCurrentApprovedTurn(): boolean {
    const pendingQuestion = this.session?.clarifications.find(item => item.status === 'PENDING')?.question?.trim();
    return this.speakApproved(pendingQuestion || this.session?.assistantMessage || '');
  }

  private speakApproved(message: string): boolean {
    const text = message.trim();
    if (!text || text === this.lastSpokenMessage || !this.state().connected || this.connectedVisitId !== this.visitId.trim()) return false;
    this.lastSpokenMessage = text;
    const started = this.bridge.speakApproved(text);
    if (!started) this.syncMute();
    return started;
  }

  private syncMute(): void {
    this.bridge.setMuted(this.blocked || this.manualMuted || this.backlogPaused);
  }
}
