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
import { AiConversationThreadComponent } from './ai-conversation-thread.component';
import { AiDraftPreviewComponent } from './ai-draft-preview.component';
import {
  AiProposalDecisionRequest,
  AiProposalPanelComponent,
} from './ai-proposal-panel.component';
import { AiTranscriptReviewComponent } from './ai-transcript-review.component';
import {
  AiConsultationApiService,
  AiConsultationDraft,
  AiField,
  AiMessageResponse,
  AiSessionResponse,
} from './ai-consultation-api.service';

export type { AiConsultationDraft } from './ai-consultation-api.service';

@Component({
  selector: 'app-voice-assistant-panel',
  standalone: true,
  imports: [
    CommonModule,
    AiAssistantInputComponent,
    AiConversationThreadComponent,
    AiClarificationPanelComponent,
    AiProposalPanelComponent,
    AiTranscriptReviewComponent,
    AiDraftPreviewComponent,
  ],
  template: `
    <section class="overflow-hidden rounded-[6px] border border-[var(--app-border)] bg-[var(--app-surface)] shadow-sm">
      <header class="flex flex-col gap-3 border-b border-[var(--app-border)] bg-[var(--app-surface-muted)] px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
        <div class="flex items-center gap-3">
          <span class="inline-flex h-10 w-10 items-center justify-center rounded-[6px] bg-cyan-50 text-cyan-700 dark:bg-cyan-950/30 dark:text-cyan-300">
            <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 18.75a6 6 0 006-6v-1.5m-12 0v1.5a6 6 0 006 6m0 0v3m-3 0h6M12 15.75a3 3 0 003-3V6a3 3 0 10-6 0v6.75a3 3 0 003 3z" />
            </svg>
          </span>
          <div>
            <h2 class="text-sm font-bold text-[var(--text-primary)]">
              {{ i18n.t('consultation.ai.title', 'Assistant vocal IA') }}
            </h2>
            <p class="text-xs text-[var(--text-muted)]">
              {{ i18n.t('consultation.ai.subtitleControlled', 'Échangez, relisez, puis acceptez ou rejetez chaque modification proposée.') }}
            </p>
          </div>
        </div>
        <span
          class="inline-flex w-fit items-center gap-1.5 rounded-[4px] border px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider"
          [ngClass]="session()
            ? 'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-900 dark:bg-emerald-950/30 dark:text-emerald-300'
            : 'border-slate-200 bg-white text-slate-600 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300'"
        >
          <span class="h-1.5 w-1.5 rounded-full" [ngClass]="session() ? 'bg-emerald-500' : 'bg-slate-400'"></span>
          {{ session() ? i18n.t('consultation.ai.active', 'Session active') : i18n.t('consultation.ai.inactive', 'Session inactive') }}
        </span>
      </header>

      <div class="grid grid-cols-1 gap-5 p-5 md:grid-cols-[180px_1fr]">
        <aside class="hidden md:block">
          <div class="rounded-[6px] border border-[var(--app-border)] bg-[var(--app-surface)] p-3 text-center shadow-xs">
            @if (qrCodeUrl()) {
              <img [src]="qrCodeUrl()" alt="QR code de la consultation" class="mx-auto h-36 w-36" />
            } @else {
              <div class="mx-auto flex h-36 w-36 items-center justify-center bg-[var(--app-surface-muted)] text-xs text-[var(--text-muted)]">
                {{ i18n.t('consultation.ai.qrLoading', 'Chargement du QR…') }}
              </div>
            }
            <p class="mt-2 text-[11px] font-semibold text-[var(--text-primary)]">
              {{ i18n.t('consultation.ai.scan', 'Scannez avec votre téléphone') }}
            </p>
          </div>
        </aside>

        <div class="min-w-0 space-y-4">
          @if (errorMessage()) {
            <div class="rounded-[4px] border border-rose-200 bg-rose-50 p-3 text-xs font-semibold text-rose-700 dark:border-rose-900 dark:bg-rose-950/30 dark:text-rose-300">
              {{ errorMessage() }}
            </div>
          }

          @if (!session()) {
            <div class="rounded-[4px] border border-dashed border-[var(--app-border)] bg-[var(--app-surface-muted)]/40 p-4">
              <p class="text-sm font-semibold text-[var(--text-primary)]">
                {{ i18n.t('consultation.ai.startTitle', 'Démarrer une conversation clinique') }}
              </p>
              <p class="mt-1 text-xs leading-5 text-[var(--text-muted)]">
                {{ i18n.t('consultation.ai.startControlledHelp', 'Aucune proposition n’est acceptée ou sauvegardée automatiquement.') }}
              </p>
              <button
                type="button"
                (click)="startSession()"
                [disabled]="busy() || !visitId"
                class="mt-3 inline-flex items-center justify-center rounded-[4px] bg-[var(--brand-primary)] px-4 py-2 text-sm font-semibold text-white hover:bg-[var(--brand-primary-hover)] disabled:opacity-50"
              >
                {{ busy() ? i18n.t('consultation.ai.starting', 'Démarrage…') : i18n.t('consultation.ai.start', 'Activer l’assistant') }}
              </button>
            </div>
          } @else {
            <app-ai-assistant-input
              [busy]="busy()"
              [recording]="recording()"
              [mediaRecorderSupported]="mediaRecorderSupported"
              [blocked]="interactionBlocked()"
              [resetToken]="composerResetToken()"
              (toggleRecording)="toggleRecording()"
              (endSession)="deleteSession()"
              (sendText)="sendText($event)"
            />

            <app-ai-conversation-thread [messages]="session()?.conversation ?? []" />

            <app-ai-clarification-panel
              [clarifications]="session()?.clarifications ?? []"
              [disabled]="busy()"
              (answered)="answerClarification($event)"
            />

            <app-ai-proposal-panel
              [revisions]="session()?.revisions ?? []"
              [disabled]="busy()"
              (decided)="decideProposal($event)"
            />

            <app-ai-transcript-review
              [transcript]="session()?.pendingTranscript"
              [busy]="busy()"
              (analyze)="analyzeTranscript($event)"
              (discard)="discardPendingTranscript()"
            />

            <app-ai-draft-preview
              [draft]="session()?.draft ?? {}"
              [canApply]="!hasPendingRevision()"
              (apply)="applyCurrentDraft()"
            />
          }
        </div>
      </div>
    </section>
  `,
})
export class VoiceAssistantPanelComponent implements OnInit, OnDestroy {
  private readonly api = inject(AiConsultationApiService);
  readonly i18n = inject(I18nService);

  @Input({ required: true }) visitId = '';
  @Input() currentDraft: Record<string, unknown> | null = null;
  @Output() readonly applyDraft = new EventEmitter<AiConsultationDraft>();

  readonly session = signal<AiSessionResponse | null>(null);
  readonly qrCodeUrl = signal<string | null>(null);
  readonly busy = signal(false);
  readonly recording = signal(false);
  readonly errorMessage = signal('');
  readonly composerResetToken = signal(0);

  readonly mediaRecorderSupported =
    typeof window !== 'undefined'
    && 'MediaRecorder' in window
    && !!navigator.mediaDevices?.getUserMedia;

  private mediaRecorder: MediaRecorder | null = null;
  private mediaStream: MediaStream | null = null;
  private audioChunks: Blob[] = [];
  private recordingTimeout: ReturnType<typeof setTimeout> | null = null;
  private pollingSubscription: Subscription | null = null;

  ngOnInit(): void {
    if (!this.visitId) return;
    this.loadQrCode();
    this.refreshSession();
    this.pollingSubscription = interval(4000).subscribe(() => {
      if (!this.recording() && !this.busy()) this.refreshSession(true);
    });
  }

  ngOnDestroy(): void {
    this.pollingSubscription?.unsubscribe();
    this.stopMediaStream();
    const qrCodeUrl = this.qrCodeUrl();
    if (qrCodeUrl) URL.revokeObjectURL(qrCodeUrl);
  }

  interactionBlocked(): boolean {
    return !!this.session()?.pendingTranscript
      || this.hasPendingClarification()
      || this.hasPendingRevision();
  }

  hasPendingClarification(): boolean {
    return this.session()?.clarifications.some(
      clarification => clarification.status === 'PENDING',
    ) ?? false;
  }

  hasPendingRevision(): boolean {
    return this.session()?.revisions.some(
      revision => revision.status === 'PENDING',
    ) ?? false;
  }

  startSession(): void {
    if (!this.visitId || this.busy()) return;
    this.startBusy();
    this.api.startSession(this.visitId, this.sanitizedCurrentDraft()).subscribe({
      next: response => this.completeSessionUpdate(response),
      error: error => this.handleError(error, 'Impossible de démarrer la session IA.'),
    });
  }

  toggleRecording(): void {
    if (this.recording()) {
      this.mediaRecorder?.stop();
      return;
    }
    void this.startRecording();
  }

  sendText(text: string): void {
    if (!text.trim() || this.busy() || this.interactionBlocked()) return;
    this.startBusy();
    this.api.sendText(this.visitId, text.trim()).subscribe({
      next: response => {
        this.updateSessionFromMessage(response);
        this.composerResetToken.update(value => value + 1);
        this.busy.set(false);
      },
      error: error => this.handleError(error, 'Le message n’a pas pu être analysé.'),
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
      next: response => {
        this.updateSessionFromMessage(response);
        this.busy.set(false);
      },
      error: error => this.handleError(
        error,
        'La réponse à la clarification n’a pas pu être analysée.',
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
      error: error => this.handleError(error, 'La décision n’a pas pu être enregistrée.'),
    });
  }

  analyzeTranscript(transcript: string): void {
    if (!transcript.trim() || this.busy()) return;
    this.startBusy();
    this.api.analyzeTranscript(this.visitId, transcript.trim()).subscribe({
      next: response => {
        this.updateSessionFromMessage(response);
        this.busy.set(false);
      },
      error: error => this.handleError(error, 'La transcription n’a pas pu être analysée.'),
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
      error: error => this.handleError(error, 'La transcription n’a pas pu être abandonnée.'),
    });
  }

  deleteSession(): void {
    if (!this.session() || this.busy()) return;
    this.startBusy();
    this.api.deleteSession(this.visitId).subscribe({
      next: () => {
        this.session.set(null);
        this.composerResetToken.update(value => value + 1);
        this.busy.set(false);
      },
      error: error => this.handleError(error, 'Impossible de terminer la session IA.'),
    });
  }

  applyCurrentDraft(): void {
    const draft = this.session()?.draft;
    if (draft && !this.hasPendingRevision()) {
      this.applyDraft.emit({ ...draft });
    }
  }

  private async startRecording(): Promise<void> {
    if (!this.mediaRecorderSupported || this.busy() || this.interactionBlocked()) return;
    if (!this.session()) {
      this.startSession();
      this.errorMessage.set('Activez la session puis relancez la dictée.');
      return;
    }
    try {
      this.errorMessage.set('');
      this.mediaStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: 1,
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
      });
      const mimeType = this.preferredMimeType();
      this.audioChunks = [];
      const options: MediaRecorderOptions = { audioBitsPerSecond: 128000 };
      if (mimeType) options.mimeType = mimeType;
      this.mediaRecorder = new MediaRecorder(this.mediaStream, options);
      this.mediaRecorder.ondataavailable = event => {
        if (event.data.size > 0) this.audioChunks.push(event.data);
      };
      this.mediaRecorder.onstop = () => this.sendRecordedAudio();
      this.mediaRecorder.start(500);
      this.recording.set(true);
      this.recordingTimeout = setTimeout(() => this.mediaRecorder?.stop(), 120000);
    } catch {
      this.errorMessage.set('Accès au microphone refusé ou indisponible.');
      this.stopMediaStream();
    }
  }

  private sendRecordedAudio(): void {
    this.recording.set(false);
    if (this.recordingTimeout) clearTimeout(this.recordingTimeout);
    const audio = new Blob(this.audioChunks, {
      type: this.mediaRecorder?.mimeType || 'audio/webm',
    });
    this.stopMediaStream();
    if (audio.size === 0) {
      this.errorMessage.set('Aucun son n’a été enregistré.');
      return;
    }
    this.startBusy();
    this.api.transcribeAudio(this.visitId, audio).subscribe({
      next: response => {
        this.session.update(current => current ? {
          ...current,
          pendingTranscript: response.transcript,
          transcriptStatus: response.status,
          expiresAt: response.expiresAt,
        } : current);
        this.busy.set(false);
      },
      error: error => this.handleError(error, 'La dictée n’a pas pu être transcrite.'),
    });
  }

  private refreshSession(silent = false): void {
    this.api.getSession(this.visitId).subscribe({
      next: response => {
        if (response) this.session.set(response);
      },
      error: error => {
        if (!silent && error.status !== 404) {
          this.handleError(error, 'Assistant IA indisponible.');
        }
      },
    });
  }

  private loadQrCode(): void {
    this.api.loadQrCode(this.visitId).subscribe({
      next: blob => {
        const previous = this.qrCodeUrl();
        if (previous) URL.revokeObjectURL(previous);
        this.qrCodeUrl.set(URL.createObjectURL(blob));
      },
      error: () => this.qrCodeUrl.set(null),
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
      transcriptStatus: response.transcript
        ? 'ANALYZED'
        : previous?.transcriptStatus ?? 'NONE',
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

  private sanitizedCurrentDraft(): AiConsultationDraft {
    const fields: AiField[] = [
      'symptoms', 'clinicalExam', 'suspectedDiagnosis', 'diagnosis',
      'finalDiagnosis', 'conclusion', 'advice', 'followUp',
    ];
    const result: AiConsultationDraft = {};
    for (const field of fields) {
      const value = this.currentDraft?.[field];
      if (typeof value === 'string' && value.trim()) result[field] = value.trim();
    }
    return result;
  }

  private preferredMimeType(): string | undefined {
    const candidates = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4', 'audio/wav'];
    return candidates.find(type => MediaRecorder.isTypeSupported(type));
  }

  private startBusy(): void {
    this.busy.set(true);
    this.errorMessage.set('');
  }

  private stopMediaStream(): void {
    this.mediaStream?.getTracks().forEach(track => track.stop());
    this.mediaStream = null;
    this.mediaRecorder = null;
    this.audioChunks = [];
  }

  private handleError(
    error: { status?: number; error?: { detail?: string; title?: string } },
    fallback: string,
  ): void {
    this.busy.set(false);
    this.recording.set(false);
    this.stopMediaStream();
    const detail = error.error?.detail || error.error?.title;
    this.errorMessage.set(detail && !detail.startsWith('AI_') ? detail : fallback);
  }
}
