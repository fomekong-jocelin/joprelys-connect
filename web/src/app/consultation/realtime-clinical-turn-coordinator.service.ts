import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';
import {
  RealtimeClinicalIntakeAck,
  RealtimeClinicalIntakeApiService,
} from './realtime-clinical-intake-api.service';
import { RealtimeTranscriptTurn } from './realtime-voice-bridge.service';

const HIGH_WATER_MARK = 32;
const LOW_WATER_MARK = 8;
const MAX_SEEN_TRANSCRIPT_IDS = 1024;
const RETRY_DELAY_MS = 1200;

interface DurableRealtimeTurn {
  transcript: string;
  confidence: number | null;
  eventId: string;
  itemId?: string;
}

export interface RealtimeClinicalTurnHost {
  visitId: () => string;
  enabled: () => boolean;
  connected: () => boolean;
  manualMuted: () => boolean;
  onPersisted: (entry: RealtimeClinicalIntakeAck) => void;
  onError: (message: string) => void;
  onPipelineStateChange: () => void;
  syncMute: () => void;
}

/**
 * Realtime is a capture pipeline, not a turn-by-turn approval wizard.
 *
 * Every phrase is durably persisted first. Clinical structuring is rebuilt later
 * from the durable corpus, so a slow model, a pending proposal or low ASR
 * confidence can never freeze subsequent microphone input.
 */
@Injectable()
export class RealtimeClinicalTurnCoordinator {
  private readonly intake = inject(RealtimeClinicalIntakeApiService);
  private readonly i18n = inject(I18nService);

  readonly processing = signal(false);
  readonly lastTranscript = signal('');
  readonly lastTranscriptConfidence = signal<number | null>(null);
  readonly backlogPaused = signal(false);
  readonly durableBlocked = signal(false);

  private readonly intakeQueue: DurableRealtimeTurn[] = [];
  private readonly seenTranscriptIds = new Set<string>();
  private readonly seenTranscriptOrder: string[] = [];
  private host: RealtimeClinicalTurnHost | null = null;
  private intakeBusy = false;
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
    if (!host || !text || host.manualMuted() || !host.enabled() || !host.connected()) return;

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

    this.intakeQueue.push({ transcript: text, confidence, eventId, itemId });
    this.applyBackpressureIfNeeded();
    this.drainIntakeQueue();
  }

  resume(): void {
    this.drainIntakeQueue();
  }

  reset(): void {
    this.generation += 1;
    this.clearRetry();
    this.intakeQueue.length = 0;
    this.seenTranscriptIds.clear();
    this.seenTranscriptOrder.length = 0;
    this.backlogPaused.set(false);
    this.durableBlocked.set(false);
    this.intakeBusy = false;
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
    // The active turn remains at index 0 until its durable ACK arrives, so adding
    // intakeBusy would double-count the same clinical phrase.
    return this.intakeQueue.length;
  }

  isIdle(): boolean {
    return !this.intakeBusy && this.intakeQueue.length === 0;
  }

  submitManualCorrection(intakeId: string, correction: string): void {
    const host = this.host;
    const normalized = correction.trim();
    if (!host || !intakeId || !normalized || this.processing()) return;

    const visitId = host.visitId();
    const generation = this.generation;
    this.intakeBusy = true;
    this.updateProcessing();
    this.intake.correct(visitId, intakeId, normalized).subscribe({
      next: entry => {
        if (!this.isCurrentVisit(visitId, generation)) return;
        this.intakeBusy = false;
        host.onPersisted(entry);
        this.updateProcessing();
        this.drainIntakeQueue();
      },
      error: () => {
        if (!this.isCurrentVisit(visitId, generation)) return;
        this.intakeBusy = false;
        this.updateProcessing();
        host.onError(this.i18n.t(
          'consultation.ai.realtimeCorrectionFailed',
          'La correction n’a pas pu être enregistrée. La transcription précédente reste conservée.',
        ));
        this.drainIntakeQueue();
      },
    });
  }

  intakeQueueSize(): number {
    return this.intakeQueue.length;
  }

  /** Kept for diagnostics compatibility: clinical analysis no longer queues per phrase. */
  analysisQueueSize(): number {
    return 0;
  }

  private drainIntakeQueue(): void {
    const host = this.host;
    if (!host
      || this.intakeBusy
      || this.durableBlocked()
      || host.manualMuted()
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
    this.intake.ingest(
      visitId,
      turn.transcript,
      turn.confidence,
      turn.eventId,
      turn.itemId,
    ).subscribe({
      next: entry => {
        if (!this.isCurrentVisit(visitId, generation)) return;
        if (this.intakeQueue[0] === turn) this.intakeQueue.shift();
        this.intakeBusy = false;
        host.onPersisted(entry);
        this.updateProcessing();
        this.releaseBackpressureIfPossible();
        this.drainIntakeQueue();
      },
      error: error => {
        if (!this.isCurrentVisit(visitId, generation)) return;
        this.intakeBusy = false;
        this.updateProcessing();
        if (this.isTransient(error)) {
          this.scheduleRetry();
          return;
        }
        // Fail closed only for durable persistence. Never pretend a non-ACKed phrase is safe.
        this.durableBlocked.set(true);
        host.syncMute();
        host.onError(this.i18n.t(
          'consultation.ai.realtimeDurableIntakeBlocked',
          'La sauvegarde sécurisée est interrompue. Le micro est mis en pause sans supprimer les phrases en attente.',
        ));
      },
    });
  }

  private isCurrentVisit(visitId: string, generation: number): boolean {
    const host = this.host;
    return !this.destroyed
      && !!host
      && generation === this.generation
      && visitId === host.visitId();
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
    if (this.intakeQueue.length < HIGH_WATER_MARK || this.backlogPaused()) return;
    this.backlogPaused.set(true);
    this.host?.syncMute();
  }

  private releaseBackpressureIfPossible(): void {
    if (!this.backlogPaused() || this.intakeQueue.length > LOW_WATER_MARK) return;
    this.backlogPaused.set(false);
    this.host?.syncMute();
  }

  private scheduleRetry(): void {
    if (this.retryTimer) return;
    this.retryTimer = setTimeout(() => {
      this.retryTimer = null;
      this.drainIntakeQueue();
    }, RETRY_DELAY_MS);
  }

  private clearRetry(): void {
    if (!this.retryTimer) return;
    clearTimeout(this.retryTimer);
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

  private updateProcessing(): void {
    this.processing.set(this.intakeBusy);
    this.host?.onPipelineStateChange();
  }
}
