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
  AiVitalField,
  AiVitalsApiService,
  AiVitalsProposal,
} from './ai-vitals-api.service';
import {
  RealtimeTranscriptTurn,
  RealtimeVoiceBridgeService,
  RealtimeVoiceState,
} from './realtime-voice-bridge.service';

const REALTIME_CONFIDENCE_FLOOR = 0.35;
const MAX_SEEN_TRANSCRIPT_IDS = 512;
const RETRY_DELAY_MS = 1200;

@Component({
  selector: 'app-realtime-vitals-controller',
  standalone: true,
  imports: [CommonModule],
  providers: [RealtimeVoiceBridgeService],
  template: `
    <div class="rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] p-3">
      <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div class="min-w-0">
          <div class="flex items-center gap-2">
            <span
              class="h-2.5 w-2.5 rounded-full"
              [ngClass]="state().connected ? 'bg-[var(--brand-success)]' : state().connecting ? 'bg-[var(--brand-warning)]' : 'bg-[var(--brand-danger)]'"
            ></span>
            <p class="text-xs font-bold text-[var(--text-primary)]">
              {{ state().connected
                ? i18n.t('vitals.assistant.realtimeConnected', 'Joprelys écoute')
                : state().connecting
                  ? i18n.t('vitals.assistant.realtimeRecovering', 'Connexion audio…')
                  : i18n.t('vitals.assistant.realtimeDisconnected', 'Audio indisponible') }}
            </p>
          </div>
          <p class="mt-1 text-[11px] leading-4 text-[var(--text-muted)]">
            @if (state().connected) {
              {{ processing()
                ? i18n.t('vitals.assistant.realtimeProcessingBackground', 'Traitement en arrière-plan — continuez à dicter.')
                : i18n.t('vitals.assistant.realtimeHelp', 'Dictez naturellement les constantes. Les valeurs restent à valider.') }}
            } @else {
              {{ i18n.t(
                'vitals.assistant.realtimeRecoveryHelp',
                'Attendez le retour de l’écoute avant de dicter de nouvelles mesures.'
              ) }}
            }
          </p>
        </div>

        @if (state().connected) {
          <button
            type="button"
            (click)="toggleMute()"
            [disabled]="disabled"
            class="ui-button ui-button-secondary min-h-11 shrink-0"
          >
            <span class="h-2 w-2 rounded-full" [ngClass]="effectiveMuted() ? 'bg-[var(--text-muted)]' : 'bg-[var(--brand-success)]'"></span>
            {{ effectiveMuted()
              ? i18n.t('vitals.assistant.realtimeUnmute', 'Reprendre')
              : i18n.t('vitals.assistant.realtimeMute', 'Pause') }}
          </button>
        }
      </div>

      @if (state().connected) {
        <div class="mt-3 flex min-h-9 items-center justify-between gap-3 rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface)] px-3 py-2">
          <p class="min-w-0 truncate text-[11px] font-semibold text-[var(--text-secondary)]">
            @if (state().assistantSpeaking) {
              {{ i18n.t('vitals.assistant.speaking', 'Joprelys vous répond…') }}
            } @else if (state().userSpeaking) {
              {{ i18n.t('vitals.assistant.listening', 'Je vous écoute…') }}
            } @else if (pendingConfirmationContext()) {
              {{ i18n.t('vitals.assistant.realtimeAwaitingConfirmation', 'Une précision est attendue') }}
            } @else {
              {{ i18n.t('vitals.assistant.realtimeReady', 'Micro actif') }}
            }
          </p>
          @if (transcriptQueue.length > 0) {
            <span class="shrink-0 text-[10px] font-bold text-[var(--brand-primary)]">
              {{ transcriptQueue.length }} {{ i18n.t('vitals.assistant.realtimeQueued', 'en attente') }}
            </span>
          }
        </div>
      } @else {
        <div class="mt-3 rounded-[var(--radius-brand-sm)] border border-[var(--brand-warning-border)] bg-[var(--brand-warning-subtle)] px-3 py-2 text-[11px] font-semibold text-[var(--brand-warning-text)]">
          {{ state().connecting
            ? i18n.t('vitals.assistant.realtimeRecovering', 'Connexion audio…')
            : i18n.t('vitals.assistant.realtimeDisconnectedHelp', 'Utilisez la saisie manuelle tant que l’écoute n’est pas disponible.') }}
        </div>
      }
    </div>
  `,
})
export class RealtimeVitalsControllerComponent implements OnChanges, OnDestroy {
  private readonly api = inject(AiVitalsApiService);
  private readonly bridge = inject(RealtimeVoiceBridgeService);
  readonly i18n = inject(I18nService);

  @Input({ required: true }) visitId = '';
  @Input() currentVitals: Partial<Record<AiVitalField, number>> = {};
  @Input() disabled = false;
  @Input() enabled = true;
  @Output() readonly proposed = new EventEmitter<AiVitalsProposal>();
  @Output() readonly activeChange = new EventEmitter<boolean>();
  @Output() readonly realtimeError = new EventEmitter<string>();

  readonly state = signal<RealtimeVoiceState>({
    connected: false,
    connecting: false,
    userSpeaking: false,
    assistantSpeaking: false,
    muted: false,
  });
  readonly processing = signal(false);
  readonly pendingConfirmationContext = signal('');
  readonly transcriptQueue: RealtimeTranscriptTurn[] = [];

  private readonly subscriptions = new Subscription();
  private readonly seenTranscriptIds = new Set<string>();
  private readonly seenTranscriptOrder: string[] = [];
  private manualMuted = false;
  private connectingForVisit = '';
  private connectedVisitId = '';
  private pipelineGeneration = 0;
  private retryTimer: ReturnType<typeof setTimeout> | null = null;
  private destroyed = false;

  constructor() {
    this.subscriptions.add(this.bridge.state$.subscribe(state => {
      const wasConnected = this.state().connected;
      this.state.set(state);
      if (state.connected && this.enabled && this.visitId.trim()) {
        this.connectedVisitId = this.visitId.trim();
      }
      this.activeChange.emit(state.connected);
      if (state.connected && !wasConnected) {
        this.syncMute();
        this.drainTranscriptQueue();
      }
    }));
    this.subscriptions.add(this.bridge.transcript$.subscribe(turn => this.enqueueTranscript(turn)));
    this.subscriptions.add(this.bridge.error$.subscribe(message => this.realtimeError.emit(message)));
    this.subscriptions.add(this.bridge.assistantTurnCompleted$.subscribe(() => {
      setTimeout(() => {
        this.syncMute();
        this.drainTranscriptQueue();
      }, 120);
    }));
  }

  ngOnChanges(changes: SimpleChanges): void {
    const visitChanged = !!changes['visitId']
      && !changes['visitId'].firstChange
      && changes['visitId'].previousValue !== changes['visitId'].currentValue;
    if (visitChanged) {
      this.resetTranscriptPipeline();
      this.manualMuted = false;
    }
    if (changes['enabled'] || changes['visitId']) {
      void this.syncConnection(visitChanged);
    }
    if (changes['disabled']) {
      this.syncMute();
      if (!this.disabled) this.drainTranscriptQueue();
    }
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.resetTranscriptPipeline();
    this.subscriptions.unsubscribe();
    this.bridge.disconnect();
  }

  toggleMute(): void {
    if (!this.state().connected || this.disabled) return;
    this.manualMuted = !this.manualMuted;
    this.syncMute();
    if (!this.manualMuted) this.drainTranscriptQueue();
  }

  effectiveMuted(): boolean {
    return this.disabled
      || this.manualMuted
      || this.state().assistantSpeaking
      || this.state().muted;
  }

  private async syncConnection(forceVisitReset = false): Promise<void> {
    const targetVisitId = this.visitId.trim();
    const activeBelongsToAnotherVisit = !!this.connectedVisitId
      && this.connectedVisitId !== targetVisitId;
    const inFlightForAnotherVisit = !!this.connectingForVisit
      && this.connectingForVisit !== targetVisitId;

    if (forceVisitReset || activeBelongsToAnotherVisit || inFlightForAnotherVisit) {
      this.bridge.disconnect();
      this.connectedVisitId = '';
      this.connectingForVisit = '';
    }

    if (!this.enabled || !targetVisitId) {
      this.connectedVisitId = '';
      this.connectingForVisit = '';
      this.bridge.disconnect();
      return;
    }
    if (this.state().connected && this.connectedVisitId === targetVisitId) {
      this.syncMute();
      this.drainTranscriptQueue();
      return;
    }
    if (this.state().connecting && this.connectingForVisit === targetVisitId) return;
    if (this.state().connected || this.state().connecting) {
      this.bridge.disconnect();
    }
    if (!this.bridge.isSupported()) {
      this.activeChange.emit(false);
      this.realtimeError.emit(this.i18n.t(
        'vitals.assistant.realtimeUnsupported',
        'Ce navigateur ne prend pas en charge la connexion audio temps réel.',
      ));
      return;
    }

    this.connectingForVisit = targetVisitId;
    try {
      await this.bridge.connect(targetVisitId, 'vitals');
      if (this.destroyed || this.visitId.trim() !== targetVisitId) {
        this.bridge.disconnect();
        return;
      }
      this.connectedVisitId = targetVisitId;
      this.syncMute();
      this.drainTranscriptQueue();
    } catch (error) {
      if (this.destroyed || this.visitId.trim() !== targetVisitId) return;
      this.realtimeError.emit(
        error instanceof Error && error.message.trim()
          ? error.message
          : this.i18n.t(
              'vitals.assistant.realtimeUnavailable',
              'Le mode Realtime est momentanément indisponible. Reconnexion automatique en cours.',
            ),
      );
    } finally {
      if (this.connectingForVisit === targetVisitId) this.connectingForVisit = '';
    }
  }

  private enqueueTranscript(turn: RealtimeTranscriptTurn): void {
    const transcript = turn.transcript.trim();
    if (!transcript
      || this.disabled
      || this.manualMuted
      || !this.enabled
      || !this.state().connected
      || !this.connectedVisitId
      || this.connectedVisitId !== this.visitId.trim()) {
      return;
    }

    const dedupeId = turn.itemId?.trim()
      ? `item:${turn.itemId.trim()}`
      : turn.eventId?.trim()
        ? `event:${turn.eventId.trim()}`
        : '';
    if (dedupeId && this.seenTranscriptIds.has(dedupeId)) return;
    if (dedupeId) this.rememberTranscriptId(dedupeId);

    this.transcriptQueue.push({ ...turn, transcript });
    if (turn.confidence === null
      || !Number.isFinite(turn.confidence)
      || turn.confidence < REALTIME_CONFIDENCE_FLOOR) {
      this.realtimeError.emit(this.i18n.t(
        'vitals.assistant.realtimeLowConfidenceReview',
        'Transcription incertaine : les valeurs restent proposées pour vérification, rien n’est enregistré automatiquement.',
      ));
    }
    this.drainTranscriptQueue();
  }

  private drainTranscriptQueue(): void {
    if (this.processing()
      || this.disabled
      || this.manualMuted
      || !this.enabled
      || !this.state().connected
      || this.state().assistantSpeaking
      || !this.connectedVisitId
      || this.connectedVisitId !== this.visitId.trim()) {
      return;
    }
    const turn = this.transcriptQueue[0];
    if (!turn) return;

    const transcript = turn.transcript.trim();
    const confirmationContext = this.pendingConfirmationContext();
    const modelText = confirmationContext
      ? `${confirmationContext}\nClinician confirmation or correction: ${transcript}`
      : transcript;
    const turnVisitId = this.connectedVisitId;
    const generation = this.pipelineGeneration;

    this.processing.set(true);
    this.api.analyzeText(
      turnVisitId,
      modelText,
      this.i18n.currentLanguage(),
      this.currentVitals,
    ).subscribe({
      next: proposal => {
        if (!this.isCurrentTurnContext(turnVisitId, generation)) return;
        this.shiftTurn(turn);
        this.processing.set(false);
        const userFacingProposal: AiVitalsProposal = { ...proposal, transcript };
        if (proposal.needsConfirmation) {
          this.pendingConfirmationContext.set([
            `Previous ambiguous vitals utterance: ${transcript}`,
            `Assistant clarification: ${proposal.assistantMessage}`,
            proposal.confirmationReason ? `Reason: ${proposal.confirmationReason}` : '',
          ].filter(Boolean).join('\n'));
        } else {
          this.pendingConfirmationContext.set('');
        }
        this.proposed.emit(userFacingProposal);
        if (proposal.assistantMessage) {
          this.bridge.setMuted(true);
          const started = this.bridge.speakApproved(proposal.assistantMessage);
          if (!started) {
            this.syncMute();
            this.drainTranscriptQueue();
          }
        } else {
          this.syncMute();
          this.drainTranscriptQueue();
        }
      },
      error: error => {
        if (!this.isCurrentTurnContext(turnVisitId, generation)) return;
        this.processing.set(false);
        if (this.isTransient(error)) {
          this.scheduleRetry();
          return;
        }
        this.shiftTurn(turn);
        this.realtimeError.emit(this.i18n.t(
          'vitals.assistant.error',
          'Joprelys n’a pas pu analyser cette phrase. Elle reste visible ; vérifiez les constantes manuellement.',
        ));
        this.syncMute();
        this.drainTranscriptQueue();
      },
    });
  }

  private isCurrentTurnContext(visitId: string, generation: number): boolean {
    return !this.destroyed
      && generation === this.pipelineGeneration
      && visitId === this.visitId.trim()
      && visitId === this.connectedVisitId;
  }

  private shiftTurn(turn: RealtimeTranscriptTurn): void {
    if (this.transcriptQueue[0] === turn) this.transcriptQueue.shift();
  }

  private rememberTranscriptId(id: string): void {
    this.seenTranscriptIds.add(id);
    this.seenTranscriptOrder.push(id);
    while (this.seenTranscriptOrder.length > MAX_SEEN_TRANSCRIPT_IDS) {
      const oldest = this.seenTranscriptOrder.shift();
      if (oldest) this.seenTranscriptIds.delete(oldest);
    }
  }

  private scheduleRetry(): void {
    if (this.retryTimer || this.destroyed) return;
    this.retryTimer = setTimeout(() => {
      this.retryTimer = null;
      this.drainTranscriptQueue();
    }, RETRY_DELAY_MS);
  }

  private isTransient(error: unknown): boolean {
    if (!(error instanceof HttpErrorResponse)) return true;
    return error.status === 0
      || error.status === 408
      || error.status === 425
      || error.status === 429
      || error.status >= 500;
  }

  private resetTranscriptPipeline(): void {
    this.pipelineGeneration += 1;
    if (this.retryTimer) clearTimeout(this.retryTimer);
    this.retryTimer = null;
    this.transcriptQueue.length = 0;
    this.seenTranscriptIds.clear();
    this.seenTranscriptOrder.length = 0;
    this.pendingConfirmationContext.set('');
    this.processing.set(false);
  }

  private syncMute(): void {
    this.bridge.setMuted(
      this.disabled
      || this.manualMuted
      || this.state().assistantSpeaking,
    );
  }
}
