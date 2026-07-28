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
import { ClinicalVoicePlaybackService } from './clinical-voice-playback.service';
import { RealtimeClinicalIntakeApiService } from './realtime-clinical-intake-api.service';
import {
  RealtimeTranscriptTurn,
  RealtimeVoiceBridgeService,
  RealtimeVoiceState,
} from './realtime-voice-bridge.service';
import { VoiceListeningSurfaceComponent } from './voice-listening-surface.component';

const MAX_SEEN_TRANSCRIPT_IDS = 512;
const RETRY_DELAY_MS = 1200;

interface QueuedVitalsTurn {
  transcript: string;
  confidence: number | null;
  eventId: string;
  itemId?: string;
}

@Component({
  selector: 'app-realtime-vitals-controller',
  standalone: true,
  imports: [CommonModule, VoiceListeningSurfaceComponent],
  providers: [RealtimeVoiceBridgeService, ClinicalVoicePlaybackService],
  templateUrl: './realtime-vitals-controller.component.html',
})
export class RealtimeVitalsControllerComponent implements OnChanges, OnDestroy {
  private readonly api = inject(AiVitalsApiService);
  private readonly intake = inject(RealtimeClinicalIntakeApiService);
  private readonly bridge = inject(RealtimeVoiceBridgeService);
  private readonly voicePlayback = inject(ClinicalVoicePlaybackService);
  readonly i18n = inject(I18nService);

  @Input({ required: true }) visitId = '';
  @Input() currentVitals: Partial<Record<AiVitalField, number>> = {};
  @Input() disabled = false;
  @Input() enabled = true;
  @Output() readonly proposed = new EventEmitter<AiVitalsProposal>();
  @Output() readonly activeChange = new EventEmitter<boolean>();
  @Output() readonly realtimeError = new EventEmitter<string>();
  @Output() readonly stopListening = new EventEmitter<void>();

  readonly state = signal<RealtimeVoiceState>({
    connected: false,
    connecting: false,
    userSpeaking: false,
    assistantSpeaking: false,
    muted: false,
  });
  readonly processing = signal(false);
  readonly pendingConfirmationContext = signal('');
  readonly durableBlocked = signal(false);

  private readonly subscriptions = new Subscription();
  private readonly intakeQueue: QueuedVitalsTurn[] = [];
  private readonly analysisQueue: QueuedVitalsTurn[] = [];
  private readonly seenTranscriptIds = new Set<string>();
  private readonly seenTranscriptOrder: string[] = [];
  private manualMuted = false;
  private connectingForVisit = '';
  private connectedVisitId = '';
  private pipelineGeneration = 0;
  private fallbackEventSequence = 0;
  private intakeBusy = false;
  private analysisBusy = false;
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
        this.resumePipeline();
      }
      if (state.userSpeaking) this.voicePlayback.stop();
    }));
    this.subscriptions.add(this.bridge.transcript$.subscribe(turn => this.enqueueTranscript(turn)));
    this.subscriptions.add(this.bridge.error$.subscribe(message => this.realtimeError.emit(message)));
  }

  ngOnChanges(changes: SimpleChanges): void {
    const visitChanged = !!changes['visitId']
      && !changes['visitId'].firstChange
      && changes['visitId'].previousValue !== changes['visitId'].currentValue;
    if (visitChanged) {
      this.resetTranscriptPipeline();
      this.manualMuted = false;
    }
    if (changes['enabled'] || changes['visitId']) void this.syncConnection(visitChanged);
    if (changes['disabled']) {
      this.syncMute();
      if (!this.disabled) this.resumePipeline();
    }
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.resetTranscriptPipeline();
    this.subscriptions.unsubscribe();
    this.voicePlayback.stop();
    this.bridge.disconnect();
  }

  toggleMute(): void {
    if (!this.state().connected || this.disabled || this.durableBlocked()) return;
    this.manualMuted = !this.manualMuted;
    this.syncMute();
    if (!this.manualMuted) this.resumePipeline();
  }

  effectiveMuted(): boolean {
    return this.disabled
      || this.manualMuted
      || this.durableBlocked()
      || this.state().muted;
  }

  queuedCount(): number {
    return this.intakeQueue.length + this.analysisQueue.length;
  }

  listeningSurfaceStatus(): string {
    if (this.durableBlocked()) return this.i18n.t('vitals.assistant.realtimeDurableBlocked');
    if (this.state().connecting) return this.i18n.t('vitals.assistant.realtimeRecovering');
    if (!this.state().connected) return this.i18n.t('vitals.assistant.realtimeDisconnected');
    if (this.effectiveMuted()) return this.i18n.t('consultation.ai.listeningPaused');
    return this.i18n.t(
      'consultation.ai.listenNaturally',
      'Écoute en cours... Parlez naturellement',
    );
  }

  private async syncConnection(forceVisitReset = false): Promise<void> {
    const targetVisitId = this.visitId.trim();
    const activeBelongsToAnotherVisit = !!this.connectedVisitId && this.connectedVisitId !== targetVisitId;
    const inFlightForAnotherVisit = !!this.connectingForVisit && this.connectingForVisit !== targetVisitId;

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
      this.resumePipeline();
      return;
    }
    if (this.state().connecting && this.connectingForVisit === targetVisitId) return;
    if (this.state().connected || this.state().connecting) this.bridge.disconnect();
    if (!this.bridge.isSupported()) {
      this.activeChange.emit(false);
      this.realtimeError.emit(this.i18n.t('vitals.assistant.realtimeUnsupported'));
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
      this.resumePipeline();
    } catch (error) {
      if (this.destroyed || this.visitId.trim() !== targetVisitId) return;
      this.realtimeError.emit(
        error instanceof Error && error.message.trim()
          ? error.message
          : this.i18n.t('vitals.assistant.realtimeUnavailable'),
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
      || this.durableBlocked()
      || !this.enabled
      || !this.state().connected
      || !this.connectedVisitId
      || this.connectedVisitId !== this.visitId.trim()) {
      return;
    }

    const eventId = turn.eventId?.trim() || this.nextFallbackEventId();
    const itemId = turn.itemId?.trim() || undefined;
    const dedupeId = itemId ? `item:${itemId}` : `event:${eventId}`;
    if (this.seenTranscriptIds.has(dedupeId)) return;
    this.rememberTranscriptId(dedupeId);

    const confidence = turn.confidence !== null
      && Number.isFinite(turn.confidence)
      && turn.confidence >= 0
      && turn.confidence <= 1
      ? turn.confidence
      : null;
    this.intakeQueue.push({ transcript, confidence, eventId, itemId });
    this.drainIntakeQueue();
  }

  private drainIntakeQueue(): void {
    if (this.intakeBusy
      || this.disabled
      || this.manualMuted
      || this.durableBlocked()
      || !this.enabled
      || !this.state().connected
      || !this.connectedVisitId
      || this.connectedVisitId !== this.visitId.trim()) {
      return;
    }
    const turn = this.intakeQueue[0];
    if (!turn) return;

    const visitId = this.connectedVisitId;
    const generation = this.pipelineGeneration;
    this.intakeBusy = true;
    this.updateProcessing();
    this.intake.ingestVitals(visitId, turn.transcript, turn.confidence, turn.eventId, turn.itemId).subscribe({
      next: () => {
        if (!this.isCurrentTurnContext(visitId, generation)) return;
        this.shiftQueue(this.intakeQueue, turn);
        this.intakeBusy = false;
        this.analysisQueue.push(turn);
        this.updateProcessing();
        this.drainIntakeQueue();
        this.drainAnalysisQueue();
      },
      error: error => {
        if (!this.isCurrentTurnContext(visitId, generation)) return;
        this.intakeBusy = false;
        this.updateProcessing();
        if (this.isTransient(error)) {
          this.scheduleRetry();
          return;
        }
        // Keep every unacknowledged turn and stop new Realtime capture rather than dropping it.
        this.durableBlocked.set(true);
        this.syncMute();
        this.realtimeError.emit(this.i18n.t('vitals.assistant.realtimeDurableBlocked'));
      },
    });
  }

  private drainAnalysisQueue(): void {
    if (this.analysisBusy
      || this.disabled
      || this.manualMuted
      || this.durableBlocked()
      || !this.enabled
      || !this.state().connected
      || !this.connectedVisitId
      || this.connectedVisitId !== this.visitId.trim()) {
      return;
    }
    const turn = this.analysisQueue[0];
    if (!turn) return;

    const visitId = this.connectedVisitId;
    const generation = this.pipelineGeneration;
    const confirmationContext = this.pendingConfirmationContext();
    const modelText = confirmationContext
      ? `${confirmationContext}\nClinician confirmation or correction: ${turn.transcript}`
      : turn.transcript;

    this.analysisBusy = true;
    this.updateProcessing();
    this.api.analyzeText(
      visitId,
      modelText,
      this.i18n.currentLanguage(),
      this.currentVitals,
    ).subscribe({
      next: proposal => {
        if (!this.isCurrentTurnContext(visitId, generation)) return;
        this.shiftQueue(this.analysisQueue, turn);
        this.analysisBusy = false;
        this.updateProcessing();
        const userFacingProposal: AiVitalsProposal = { ...proposal, transcript: turn.transcript };
        if (proposal.needsConfirmation) {
          this.pendingConfirmationContext.set([
            `Previous ambiguous vitals utterance: ${turn.transcript}`,
            `Assistant clarification: ${proposal.assistantMessage}`,
            proposal.confirmationReason ? `Reason: ${proposal.confirmationReason}` : '',
          ].filter(Boolean).join('\n'));
        } else {
          this.pendingConfirmationContext.set('');
        }
        this.proposed.emit(userFacingProposal);
        this.voicePlayback.play(proposal.assistantMessage || '');
        this.syncMute();
        this.drainAnalysisQueue();
      },
      error: () => {
        if (!this.isCurrentTurnContext(visitId, generation)) return;
        // The transcript is already durable. Do not retry a potentially committed model mutation.
        this.shiftQueue(this.analysisQueue, turn);
        this.analysisBusy = false;
        this.updateProcessing();
        this.realtimeError.emit(this.i18n.t('vitals.assistant.error'));
        this.syncMute();
        this.drainAnalysisQueue();
      },
    });
  }

  private resumePipeline(): void {
    this.drainIntakeQueue();
    this.drainAnalysisQueue();
  }

  private isCurrentTurnContext(visitId: string, generation: number): boolean {
    return !this.destroyed
      && generation === this.pipelineGeneration
      && visitId === this.visitId.trim()
      && visitId === this.connectedVisitId;
  }

  private shiftQueue(queue: QueuedVitalsTurn[], turn: QueuedVitalsTurn): void {
    if (queue[0] === turn) queue.shift();
  }

  private nextFallbackEventId(): string {
    this.fallbackEventSequence += 1;
    return `vitals:${Date.now()}:${this.fallbackEventSequence}`;
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
      this.drainIntakeQueue();
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

  private updateProcessing(): void {
    this.processing.set(this.intakeBusy || this.analysisBusy);
  }

  private resetTranscriptPipeline(): void {
    this.pipelineGeneration += 1;
    if (this.retryTimer) clearTimeout(this.retryTimer);
    this.retryTimer = null;
    this.intakeQueue.length = 0;
    this.analysisQueue.length = 0;
    this.seenTranscriptIds.clear();
    this.seenTranscriptOrder.length = 0;
    this.pendingConfirmationContext.set('');
    this.intakeBusy = false;
    this.analysisBusy = false;
    this.processing.set(false);
    this.durableBlocked.set(false);
  }

  private syncMute(): void {
    this.bridge.setMuted(
      this.disabled
      || this.manualMuted
      || this.durableBlocked(),
    );
  }
}
