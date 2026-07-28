import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';
import {
  AiConsultationApiService,
  AiMessageResponse,
  AiSessionResponse,
  AiTranscriptionResponse,
} from './ai-consultation-api.service';
import { RealtimeClinicalIntakeApiService } from './realtime-clinical-intake-api.service';
import { RealtimeTranscriptTurn } from './realtime-voice-bridge.service';

const HIGH_WATER_MARK = 32;
const LOW_WATER_MARK = 8;
const REALTIME_CONFIDENCE_FLOOR = 0.35;
const MAX_SEEN_TRANSCRIPT_IDS = 512;
const RETRY_DELAY_MS = 1200;

interface DurableRealtimeTurn {
  transcript: string;
  confidence: number | null;
  eventId: string;
  itemId?: string;
}

export interface RealtimeClinicalTurnHost {
  visitId: () => string;
  session: () => AiSessionResponse | null;
  enabled: () => boolean;
  connected: () => boolean;
  manualMuted: () => boolean;
  blocked: () => boolean;
  onMessage: (response: AiMessageResponse) => void;
  onReview: (response: AiTranscriptionResponse) => void;
  onError: (message: string) => void;
  onPipelineStateChange: () => void;
  syncMute: () => void;
}

@Injectable()
export class RealtimeClinicalTurnCoordinator {
  private readonly intake = inject(RealtimeClinicalIntakeApiService);
  private readonly consultationApi = inject(AiConsultationApiService);
  private readonly i18n = inject(I18nService);

  readonly processing = signal(false);
  readonly lastTranscript = signal('');
  readonly lastTranscriptConfidence = signal<number | null>(null);
  readonly backlogPaused = signal(false);
  readonly durableBlocked = signal(false);

  private readonly intakeQueue: DurableRealtimeTurn[] = [];
  private readonly analysisQueue: DurableRealtimeTurn[] = [];
  private readonly seenTranscriptIds = new Set<string>();
  private readonly seenTranscriptOrder: string[] = [];
  private host: RealtimeClinicalTurnHost | null = null;
  private intakeBusy = false;
  private analysisBusy = false;
  private generation = 0;
  private fallbackEventSequence = 0;
  private retryTimer: ReturnType<typeof setTimeout> | null = null;
  private destroyed = false;

  configure(host: RealtimeClinicalTurnHost): void {
    this.host = host;
  }

  enqueue(turn: RealtimeTranscriptTurn): void {
    const host = this.host;
    const text = turn.transcript.trim();
    if (!host
      || !text
      || host.manualMuted()
      || !host.session()
      || !host.enabled()
      || !host.connected()) {
      return;
    }

    this.lastTranscript.set(text);
    this.lastTranscriptConfidence.set(turn.confidence);

    const confidence = turn.confidence !== null
      && Number.isFinite(turn.confidence)
      && turn.confidence >= 0
      && turn.confidence <= 1
      ? turn.confidence
      : null;
    const eventId = turn.eventId?.trim() || this.nextFallbackEventId();
    const itemId = turn.itemId?.trim() || undefined;
    const dedupeId = itemId ? `item:${itemId}` : `event:${eventId}`;
    if (this.seenTranscriptIds.has(dedupeId)) return;
    this.rememberTranscriptId(dedupeId);

    this.intakeQueue.push({
      transcript: text,
      confidence,
      eventId,
      itemId,
    });
    this.applyBackpressureIfNeeded();
    this.drainIntakeQueue();
  }

  resume(): void {
    this.drainIntakeQueue();
    this.drainAnalysisQueue();
  }

  reset(): void {
    this.generation += 1;
    this.clearRetry();
    this.intakeQueue.length = 0;
    this.analysisQueue.length = 0;
    this.seenTranscriptIds.clear();
    this.seenTranscriptOrder.length = 0;
    this.backlogPaused.set(false);
    this.durableBlocked.set(false);
    this.intakeBusy = false;
    this.analysisBusy = false;
    this.processing.set(false);
    this.lastTranscript.set('');
    this.lastTranscriptConfidence.set(null);
    this.host?.onPipelineStateChange();
  }

  destroy(): void {
    this.destroyed = true;
    this.reset();
    this.host = null;
  }

  queuedCount(): number {
    return this.intakeQueue.length + this.analysisQueue.length;
  }

  isIdle(): boolean {
    return !this.intakeBusy
      && !this.analysisBusy
      && this.intakeQueue.length === 0
      && this.analysisQueue.length === 0;
  }

  submitManualCorrection(correction: string): void {
    const host = this.host;
    const normalized = correction.trim();
    if (!host
      || !normalized
      || !this.isIdle()
      || host.blocked()
      || host.manualMuted()
      || !host.enabled()
      || !host.connected()) {
      return;
    }

    const visitId = host.visitId();
    const generation = this.generation;
    const message = this.i18n.currentLanguage() === 'en'
      ? `I am correcting my last transcribed statement: ${normalized}`
      : `Je corrige mon dernier énoncé transcrit : ${normalized}`;
    this.analysisBusy = true;
    this.updateProcessing();
    this.consultationApi.sendText(visitId, message).subscribe({
      next: response => {
        if (!this.isCurrent(visitId, generation)) return;
        this.analysisBusy = false;
        host.onMessage(response);
        this.updateProcessing();
        this.drainAnalysisQueue();
      },
      error: () => {
        if (!this.isCurrent(visitId, generation)) return;
        this.analysisBusy = false;
        this.updateProcessing();
        host.onError(this.i18n.t('consultation.ai.realtimeCorrectionFailed'));
        this.drainAnalysisQueue();
      },
    });
  }

  /** Test/diagnostic only: number of turns waiting for durable ACK. */
  intakeQueueSize(): number {
    return this.intakeQueue.length;
  }

  /** Test/diagnostic only: durable turns waiting for clinical analysis. */
  analysisQueueSize(): number {
    return this.analysisQueue.length;
  }

  private drainIntakeQueue(): void {
    const host = this.host;
    if (!host
      || this.intakeBusy
      || this.durableBlocked()
      || host.manualMuted()
      || !host.session()
      || !host.enabled()
      || !host.connected()) {
      return;
    }
    const turn = this.intakeQueue[0];
    if (!turn) return;

    const visitId = host.visitId();
    const generation = this.generation;
    this.intakeBusy = true;
    this.updateProcessing();
    this.intake.ingest(visitId, turn.transcript, turn.confidence, turn.eventId, turn.itemId).subscribe({
      next: () => {
        if (!this.isCurrent(visitId, generation)) return;
        this.shiftQueue(this.intakeQueue, turn);
        this.intakeBusy = false;
        this.analysisQueue.push(turn);
        this.updateProcessing();
        this.releaseBackpressureIfPossible();
        this.drainIntakeQueue();
        this.drainAnalysisQueue();
      },
      error: error => {
        if (!this.isCurrent(visitId, generation)) return;
        this.intakeBusy = false;
        this.updateProcessing();
        if (this.isTransient(error)) {
          this.scheduleRetry();
          return;
        }
        // Never silently throw away a turn that has not received a durable ACK.
        this.durableBlocked.set(true);
        host.syncMute();
        host.onError(this.i18n.t(
          'consultation.ai.realtimeDurableIntakeBlocked',
          'La sauvegarde sécurisée est interrompue. Joprelys met le Realtime en pause sans supprimer les phrases en attente.',
        ));
      },
    });
  }

  private drainAnalysisQueue(): void {
    const host = this.host;
    if (!host
      || this.analysisBusy
      || this.analysisBlocked(host)
      || host.manualMuted()
      || !host.enabled()
      || !host.connected()) {
      return;
    }
    const session = host.session();
    const turn = this.analysisQueue[0];
    if (!session || !turn) return;

    const visitId = host.visitId();
    const generation = this.generation;
    if (turn.confidence === null || turn.confidence < REALTIME_CONFIDENCE_FLOOR) {
      this.analysisBusy = true;
      this.updateProcessing();
      this.stageForHumanReview(visitId, turn, generation);
      return;
    }
    const pendingClarification = session.clarifications.find(item => item.status === 'PENDING');
    const request = pendingClarification
      ? this.consultationApi.answerRealtimeClarification(
          visitId,
          pendingClarification.id,
          turn.transcript,
          turn.confidence,
          turn.eventId,
        )
      : this.consultationApi.sendRealtimeTranscript(
          visitId,
          turn.transcript,
          turn.confidence,
          turn.eventId,
        );

    this.analysisBusy = true;
    this.updateProcessing();
    request.subscribe({
      next: response => {
        if (!this.isCurrent(visitId, generation)) return;
        this.shiftQueue(this.analysisQueue, turn);
        this.analysisBusy = false;
        host.onMessage(response);
        this.updateProcessing();
        this.releaseBackpressureIfPossible();
        if (!this.analysisBlocked(host)) this.drainAnalysisQueue();
      },
      error: error => {
        if (!this.isCurrent(visitId, generation)) return;
        const reason = this.backendReason(error);
        if (reason === 'AI_TRANSCRIPTION_LOW_CONFIDENCE' || reason === 'AI_REALTIME_TRANSCRIPTION_UNVERIFIED') {
          this.stageForHumanReview(visitId, turn, generation);
          return;
        }
        if (reason === 'AI_REVISION_DECISION_REQUIRED'
          || reason === 'AI_TRANSCRIPT_REVIEW_REQUIRED'
          || reason === 'AI_CLARIFICATION_REQUIRED') {
          this.analysisBusy = false;
          this.updateProcessing();
          return;
        }

        // The turn is already durable. Do not retry an uncertain clinical mutation,
        // because an upstream response may have been committed even if the client lost it.
        this.shiftQueue(this.analysisQueue, turn);
        this.analysisBusy = false;
        this.updateProcessing();
        host.onError(this.i18n.t('consultation.ai.realtimeConversationAnalysisFailed'));
        this.releaseBackpressureIfPossible();
        host.syncMute();
        this.drainAnalysisQueue();
      },
    });
  }

  private stageForHumanReview(
    visitId: string,
    turn: DurableRealtimeTurn,
    generation: number,
  ): void {
    const host = this.host;
    if (!host) return;
    this.consultationApi.stageRealtimeTranscript(visitId, turn.transcript).subscribe({
      next: response => {
        if (!this.isCurrent(visitId, generation)) return;
        this.shiftQueue(this.analysisQueue, turn);
        this.analysisBusy = false;
        host.onReview(response);
        this.updateProcessing();
        this.releaseBackpressureIfPossible();
        host.syncMute();
      },
      error: () => {
        if (!this.isCurrent(visitId, generation)) return;
        this.shiftQueue(this.analysisQueue, turn);
        this.analysisBusy = false;
        this.updateProcessing();
        host.onError(this.i18n.t('consultation.ai.realtimeReviewStagingFailed'));
        this.releaseBackpressureIfPossible();
        host.syncMute();
        this.drainAnalysisQueue();
      },
    });
  }

  private analysisBlocked(host: RealtimeClinicalTurnHost): boolean {
    const session = host.session();
    return host.blocked()
      || !!session?.pendingTranscript
      || (session?.revisions.some(revision => revision.status === 'PENDING') ?? false);
  }

  private isCurrent(visitId: string, generation: number): boolean {
    const host = this.host;
    return !this.destroyed
      && !!host
      && generation === this.generation
      && visitId === host.visitId()
      && host.connected();
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
    const host = this.host;
    if (!host || this.queuedCount() < HIGH_WATER_MARK || this.backlogPaused()) return;
    this.backlogPaused.set(true);
    host.onError(this.i18n.t('consultation.ai.realtimeBackpressure'));
  }

  private releaseBackpressureIfPossible(): void {
    const host = this.host;
    if (!host || !this.backlogPaused() || this.queuedCount() > LOW_WATER_MARK) return;
    this.backlogPaused.set(false);
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

  private updateProcessing(): void {
    this.processing.set(this.intakeBusy || this.analysisBusy);
    this.host?.onPipelineStateChange();
  }

  private shiftQueue<T>(queue: T[], item: T): void {
    if (queue[0] === item) queue.shift();
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
}
