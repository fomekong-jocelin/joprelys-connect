import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  inject,
  signal,
} from '@angular/core';
import { Subscription, interval } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { AiAssistantInputComponent } from './ai-assistant-input.component';
import {
  AiClarificationAnswer,
  AiClarificationPanelComponent,
} from './ai-clarification-panel.component';
import { AiDraftPreviewComponent } from './ai-draft-preview.component';
import {
  AiProposalDecisionRequest,
  AiProposalPanelComponent,
} from './ai-proposal-panel.component';
import { AiTranscriptReviewComponent } from './ai-transcript-review.component';
import {
  AiClarification,
  AiConsultationApiService,
  AiConsultationDraft,
  AiField,
  AiMessageResponse,
  AiSessionResponse,
  AiTranscriptionResponse,
} from './ai-consultation-api.service';
import { AmbientSafetyPanelComponent } from './ambient-safety-panel.component';
import {
  ClassicVoiceCapture,
  ClassicVoiceRecorderService,
} from './classic-voice-recorder.service';
import { RealtimeVoiceControllerComponent } from './realtime-voice-controller.component';

export type { AiConsultationDraft } from './ai-consultation-api.service';

@Component({
  selector: 'app-voice-assistant-panel',
  standalone: true,
  imports: [
    CommonModule,
    AiAssistantInputComponent,
    AiClarificationPanelComponent,
    AiProposalPanelComponent,
    AiTranscriptReviewComponent,
    AiDraftPreviewComponent,
    AmbientSafetyPanelComponent,
    RealtimeVoiceControllerComponent,
  ],
  templateUrl: './voice-assistant-panel.component.html',
})
export class VoiceAssistantPanelComponent implements OnInit, OnDestroy {
  private readonly api = inject(AiConsultationApiService);
  private readonly voiceRecorder = inject(ClassicVoiceRecorderService);
  readonly i18n = inject(I18nService);

  @Input({ required: true }) visitId = '';
  @Input() currentDraft: Record<string, unknown> | null = null;
  @Output() readonly applyDraft = new EventEmitter<AiConsultationDraft>();

  readonly session = signal<AiSessionResponse | null>(null);
  readonly busy = signal(false);
  readonly recording = signal(false);
  readonly speaking = signal(false);
  readonly conversationMode = signal(true);
  readonly realtimeActive = signal(false);
  readonly audioLevel = signal(0);
  readonly errorMessage = signal('');
  readonly composerResetToken = signal(0);

  readonly mediaRecorderSupported = this.voiceRecorder.supported;
  private pollingSubscription: Subscription | null = null;

  ngOnInit(): void {
    if (!this.visitId) return;
    this.refreshSession();
    this.pollingSubscription = interval(4000).subscribe(() => {
      // Realtime already owns its connection/session lifecycle. Polling while it is
      // connecting or connected used to replace the session input every 4 seconds
      // and could make the child controller tear down an in-flight WebRTC setup.
      // Keep polling only for the controlled dictation workflow.
      if (!this.conversationMode() && !this.recording() && !this.busy()) {
        this.refreshSession(true);
      }
    });
  }

  ngOnDestroy(): void {
    this.pollingSubscription?.unsubscribe();
    this.voiceRecorder.dispose();
  }

  interactionBlocked(): boolean {
    return !!this.session()?.pendingTranscript
      || this.hasPendingClarification()
      || this.hasPendingRevision();
  }

  recordingBlocked(): boolean {
    return this.realtimeActive()
      || !!this.session()?.pendingTranscript
      || this.hasPendingClarification()
      || this.hasPendingRevision();
  }

  hasPendingClarification(): boolean {
    return this.pendingClarification() !== null;
  }

  hasPendingRevision(): boolean {
    return this.session()?.revisions.some(revision => revision.status === 'PENDING') ?? false;
  }

  hasDraftContent(): boolean {
    const draft = this.session()?.draft;
    return !!draft && Object.values(draft).some(value => typeof value === 'string' && value.trim());
  }

  startRealtime(): void {
    if (this.busy()) return;
    this.conversationMode.set(true);
    if (this.session()) return;
    this.startSession();
  }

  startDictation(): void {
    if (this.busy() || !this.mediaRecorderSupported) return;
    this.conversationMode.set(false);
    this.realtimeActive.set(false);
    if (this.session()) return;
    this.startSession();
  }

  startSession(): void {
    if (!this.visitId || this.busy()) return;
    this.startBusy();
    this.api.startSession(this.visitId, this.sanitizedCurrentDraft()).subscribe({
      next: response => this.completeSessionUpdate(response),
      error: error => this.handleError(
        error,
        this.i18n.t('consultation.ai.errorStartSession'),
      ),
    });
  }

  switchToDictation(): void {
    if (this.recording() || this.busy()) return;
    this.conversationMode.set(false);
    this.realtimeActive.set(false);
    this.errorMessage.set('');
  }

  switchToRealtime(): void {
    if (this.recording() || this.busy()) return;
    this.conversationMode.set(true);
    this.errorMessage.set('');
  }

  finishRealtime(): void {
    this.switchToDictation();
  }

  toggleConversationMode(): void {
    if (this.conversationMode()) this.switchToDictation();
    else this.switchToRealtime();
  }

  toggleRecording(): void {
    if (this.realtimeActive()) return;
    if (this.recording()) {
      this.voiceRecorder.stop();
      return;
    }
    void this.startRecording();
  }

  sendText(text: string): void {
    if (!text.trim() || this.busy() || this.interactionBlocked()) return;
    this.startBusy();
    this.api.sendText(this.visitId, text.trim()).subscribe({
      next: response => this.finishMessageResponse(response),
      error: error => this.handleError(
        error,
        this.i18n.t('consultation.ai.errorAnalyzeMessage'),
      ),
    });
  }

  answerClarification(request: AiClarificationAnswer): void {
    if (this.busy()) return;
    this.startBusy();
    this.api.answerClarification(
      this.visitId,
      request.clarificationId,
      request.answer,
    ).subscribe({
      next: response => this.finishMessageResponse(response),
      error: error => this.handleError(
        error,
        this.i18n.t('consultation.ai.errorAnalyzeClarification'),
      ),
    });
  }

  decideProposal(request: AiProposalDecisionRequest): void {
    if (this.busy()) return;
    this.startBusy();
    const operation = request.scope === 'PROPOSAL' && request.proposalId
      ? this.api.decideProposal(
          this.visitId,
          request.revisionId,
          request.proposalId,
          request.decision,
        )
      : this.api.decideRevision(
          this.visitId,
          request.revisionId,
          request.decision,
        );
    operation.subscribe({
      next: response => this.completeSessionUpdate(response),
      error: error => this.handleError(
        error,
        this.i18n.t('consultation.ai.errorSaveDecision'),
      ),
    });
  }

  analyzeTranscript(transcript: string): void {
    if (!transcript.trim() || this.busy()) return;
    this.startBusy();
    this.api.analyzeTranscript(this.visitId, transcript.trim()).subscribe({
      next: response => this.finishMessageResponse(response),
      error: error => this.handleError(
        error,
        this.i18n.t('consultation.ai.errorAnalyzeTranscript'),
      ),
    });
  }

  discardPendingTranscript(): void {
    if (!this.session()?.pendingTranscript || this.busy()) return;
    this.startBusy();
    this.api.discardPendingTranscript(this.visitId).subscribe({
      next: () => {
        this.session.update(current => current ? {
          ...current,
          pendingTranscript: null,
          transcriptStatus: 'NONE',
        } : current);
        this.busy.set(false);
      },
      error: error => this.handleError(
        error,
        this.i18n.t('consultation.ai.errorDiscardTranscript'),
      ),
    });
  }

  deleteSession(): void {
    if (!this.session() || this.busy()) return;
    this.startBusy();
    this.api.deleteSession(this.visitId).subscribe({
      next: () => {
        this.session.set(null);
        this.realtimeActive.set(false);
        this.recording.set(false);
        this.composerResetToken.update(value => value + 1);
        this.busy.set(false);
      },
      error: error => this.handleError(
        error,
        this.i18n.t('consultation.ai.errorEndSession'),
      ),
    });
  }

  applyCurrentDraft(): void {
    const draft = this.session()?.draft;
    if (draft && !this.hasPendingRevision()) this.applyDraft.emit({ ...draft });
  }

  finishRealtimeTranscription(response: AiTranscriptionResponse): void {
    this.session.update(current => current ? {
      ...current,
      pendingTranscript: response.transcript,
      transcriptStatus: response.status,
      expiresAt: response.expiresAt,
    } : current);
  }

  finishRealtimeMessage(response: AiMessageResponse): void {
    this.finishMessageResponse(response);
  }

  handleRealtimeError(message: string): void {
    if (!message.trim()) return;
    this.errorMessage.set(message.trim());
  }

  private async startRecording(): Promise<void> {
    if (this.realtimeActive() || !this.mediaRecorderSupported || this.busy() || this.recordingBlocked()) return;
    if (!this.session()) {
      this.startDictation();
      this.errorMessage.set(this.i18n.t('consultation.ai.errorStartBeforeDictation'));
      return;
    }
    try {
      this.errorMessage.set('');
      await this.voiceRecorder.start(
        capture => this.handleClassicCapture(capture),
        level => this.audioLevel.set(level),
      );
      this.recording.set(true);
    } catch {
      this.errorMessage.set(this.i18n.t('consultation.ai.errorMicrophoneUnavailable'));
      this.voiceRecorder.dispose();
    }
  }

  private handleClassicCapture(capture: ClassicVoiceCapture): void {
    this.recording.set(false);
    if (capture.audio.size === 0) {
      this.errorMessage.set(this.i18n.t('consultation.ai.errorEmptyRecording'));
      return;
    }

    // Browser-side VAD is only a UX hint. It must never veto a non-empty recording:
    // the speech model is better placed to decide what was said, and even a low-
    // confidence transcript is now shown to the clinician for correction/review.
    this.startBusy();
    this.api.transcribeAudio(this.visitId, capture.audio).subscribe({
      next: response => {
        this.session.update(current => current ? {
          ...current,
          pendingTranscript: response.transcript,
          transcriptStatus: response.status,
          expiresAt: response.expiresAt,
        } : current);
        this.busy.set(false);
      },
      error: error => this.handleError(
        error,
        this.i18n.t('consultation.ai.errorTranscribeDictation'),
      ),
    });
  }

  private finishMessageResponse(response: AiMessageResponse): void {
    this.updateSessionFromMessage(response);
    this.composerResetToken.update(value => value + 1);
    this.busy.set(false);
  }

  private refreshSession(silent = false): void {
    this.api.getSession(this.visitId).subscribe({
      next: response => {
        if (response) this.session.set(response);
      },
      error: error => {
        if (!silent && error.status !== 404) {
          this.handleError(error, this.i18n.t('consultation.ai.errorAssistantUnavailable'));
        }
      },
    });
  }

  private updateSessionFromMessage(response: AiMessageResponse): void {
    const previous = this.session();
    this.session.set({
      sessionId: response.sessionId,
      visitId: this.visitId,
      status: 'ACTIVE',
      expiresAt: response.expiresAt,
      draft: response.draft,
      transcript: response.transcript ?? previous?.transcript ?? null,
      pendingTranscript: null,
      transcriptStatus: response.transcript ? 'ANALYZED' : previous?.transcriptStatus ?? 'NONE',
      conversation: response.conversation,
      clarifications: response.clarifications,
      revisions: response.revisions,
      assistantMessage: response.assistantMessage,
      needsClarification: response.needsClarification,
    });
  }

  private completeSessionUpdate(response: AiSessionResponse): void {
    this.session.set(response);
    this.busy.set(false);
  }

  private pendingClarification(): AiClarification | null {
    return this.session()?.clarifications.find(clarification => clarification.status === 'PENDING') ?? null;
  }

  private sanitizedCurrentDraft(): AiConsultationDraft {
    const fields: AiField[] = [
      'symptoms',
      'clinicalExam',
      'suspectedDiagnosis',
      'diagnosis',
      'finalDiagnosis',
      'conclusion',
      'advice',
      'followUp',
    ];
    const result: AiConsultationDraft = {};
    for (const field of fields) {
      const value = this.currentDraft?.[field];
      if (typeof value === 'string' && value.trim()) result[field] = value.trim();
    }
    const prescription = this.currentDraft?.['prescription'];
    if (Array.isArray(prescription) && prescription.length > 0) {
      result.prescription = JSON.stringify(prescription);
    }
    const exams = this.currentDraft?.['exams'];
    if (Array.isArray(exams) && exams.length > 0) {
      result.labOrders = JSON.stringify(
        exams.filter(value => typeof value === 'string' && value.trim()),
      );
    }
    const vitals = this.currentDraft?.['vitals'];
    if (vitals && typeof vitals === 'object' && Object.keys(vitals).length > 0) {
      result.vitals = JSON.stringify(vitals);
    }
    return result;
  }

  private startBusy(): void {
    this.busy.set(true);
    this.errorMessage.set('');
  }

  private handleError(
    error: { status?: number; error?: { detail?: string; title?: string } },
    fallback: string,
  ): void {
    this.busy.set(false);
    this.recording.set(false);
    this.voiceRecorder.dispose();
    const detail = error.error?.detail || error.error?.title;
    this.errorMessage.set(detail && !detail.startsWith('AI_') ? detail : fallback);
  }
}
