import { CommonModule } from '@angular/common';
import {
  Component,
  ComponentRef,
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
  AiClarification,
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
              Échange vocal clinique, questions ciblées, structuration et validation médicale explicite.
            </p>
          </div>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          @if (conversationMode()) {
            <span class="inline-flex items-center gap-1.5 rounded-[4px] border border-cyan-200 bg-cyan-50 px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-cyan-700 dark:border-cyan-900 dark:bg-cyan-950/30 dark:text-cyan-300">
              <span class="h-1.5 w-1.5 animate-pulse rounded-full bg-cyan-500"></span>
              Audio conversationnel
            </span>
          }
          <span
            class="inline-flex w-fit items-center gap-1.5 rounded-[4px] border px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider"
            [ngClass]="session()
              ? 'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-900 dark:bg-emerald-950/30 dark:text-emerald-300'
              : 'border-slate-200 bg-white text-slate-600 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300'"
          >
            <span class="h-1.5 w-1.5 rounded-full" [ngClass]="session() ? 'bg-emerald-500' : 'bg-slate-400'"></span>
            {{ session() ? i18n.t('consultation.ai.active', 'Session active') : i18n.t('consultation.ai.inactive', 'Session inactive') }}
          </span>
        </div>
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
                L’assistant écoute, structure et pose des questions. Aucune donnée clinique n’est acceptée ou sauvegardée automatiquement.
              </p>
              <button
                type="button"
                (click)="startSession()"
                [disabled]="busy() || !visitId"
                class="mt-3 inline-flex items-center justify-center rounded-[4px] bg-[var(--brand-primary)] px-4 py-2 text-sm font-semibold text-white hover:bg-[var(--brand-primary-hover)] disabled:opacity-50"
              >
                {{ busy() ? i18n.t('consultation.ai.starting', 'Démarrage…') : 'Activer le copilote vocal' }}
              </button>
            </div>
          } @else {
            <app-ai-assistant-input
              [busy]="busy()"
              [recording]="recording()"
              [speaking]="speaking()"
              [conversationMode]="conversationMode()"
              [audioLevel]="audioLevel()"
              [mediaRecorderSupported]="mediaRecorderSupported"
              [blocked]="recordingBlocked()"
              [resetToken]="composerResetToken()"
              (toggleRecording)="toggleRecording()"
              (toggleConversationMode)="toggleConversationMode()"
              (endSession)="deleteSession()"
              (sendText)="sendText($event)"
            />

            <app-ai-conversation-thread [messages]="session()?.conversation ?? []" />

            <app-ai-clarification-panel
              [clarifications]="session()?.clarifications ?? []"
              [disabled]="busy() || recording()"
              (answered)="answerClarification($event)"
            />

            <app-ai-proposal-panel
              [revisions]="session()?.revisions ?? []"
              [disabled]="busy() || recording() || speaking()"
              (decided)="decideProposal($event)"
            />

            @if (!conversationMode()) {
              <app-ai-transcript-review
                [transcript]="session()?.pendingTranscript"
                [busy]="busy()"
                (analyze)="analyzeTranscript($event)"
                (discard)="discardPendingTranscript()"
              />
            }

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
  readonly speaking = signal(false);
  readonly conversationMode = signal(true);
  readonly audioLevel = signal(0);
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
  private recordingClarificationId: string | null = null;
  private audioContext: AudioContext | null = null;
  private analyser: AnalyserNode | null = null;
  private meterFrame: number | null = null;
  private speechFrame: number | null = null;
  private assistantAudio: HTMLAudioElement | null = null;
  private assistantAudioUrl: string | null = null;
  private lastSpokenText = '';

  ngOnInit(): void {
    if (!this.visitId) return;
    this.loadQrCode();
    this.refreshSession();
    this.pollingSubscription = interval(4000).subscribe(() => {
      if (!this.recording() && !this.speaking() && !this.busy()) this.refreshSession(true);
    });
  }

  ngOnDestroy(): void {
    this.pollingSubscription?.unsubscribe();
    this.stopAssistantAudio();
    this.stopMediaStream();
    const qrCodeUrl = this.qrCodeUrl();
    if (qrCodeUrl) URL.revokeObjectURL(qrCodeUrl);
  }

  interactionBlocked(): boolean {
    return !!this.session()?.pendingTranscript
      || this.hasPendingClarification()
      || this.hasPendingRevision();
  }

  recordingBlocked(): boolean {
    return !!this.session()?.pendingTranscript || this.hasPendingRevision();
  }

  hasPendingClarification(): boolean {
    return this.pendingClarification() !== null;
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
      next: response => this.completeSessionUpdate(response, true),
      error: error => this.handleError(error, 'Impossible de démarrer la session IA.'),
    });
  }

  toggleConversationMode(): void {
    const enabled = !this.conversationMode();
    this.conversationMode.set(enabled);
    this.stopAssistantAudio();
    if (enabled) {
      this.speakCurrentAssistantTurn(true);
    }
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
      next: response => this.finishMessageResponse(response),
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
      next: response => this.finishMessageResponse(response),
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
      next: response => this.completeSessionUpdate(response, true),
      error: error => this.handleError(error, 'La décision n’a pas pu être enregistrée.'),
    });
  }

  analyzeTranscript(transcript: string): void {
    if (!transcript.trim() || this.busy()) return;
    this.startBusy();
    this.api.analyzeTranscript(this.visitId, transcript.trim()).subscribe({
      next: response => this.finishMessageResponse(response),
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
    this.stopAssistantAudio();
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
    if (!this.mediaRecorderSupported || this.busy() || this.recordingBlocked()) return;
    if (!this.session()) {
      this.startSession();
      this.errorMessage.set('Activez la session puis relancez la dictée.');
      return;
    }
    try {
      this.stopAssistantAudio();
      this.errorMessage.set('');
      this.recordingClarificationId = this.conversationMode()
        ? this.pendingClarification()?.id ?? null
        : null;
      this.mediaStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: 1,
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
      });
      this.startAudioMeter(this.mediaStream);
      const mimeType = this.preferredMimeType();
      this.audioChunks = [];
      const options: MediaRecorderOptions = { audioBitsPerSecond: 128000 };
      if (mimeType) options.mimeType = mimeType;
      this.mediaRecorder = new MediaRecorder(this.mediaStream, options);
      this.mediaRecorder.ondataavailable = event => {
        if (event.data.size > 0) this.audioChunks.push(event.data);
      };
      this.mediaRecorder.onstop = () => this.sendRecordedAudio();
      this.mediaRecorder.start(350);
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
    const clarificationId = this.recordingClarificationId;
    this.recordingClarificationId = null;
    this.stopMediaStream();
    if (audio.size === 0) {
      this.errorMessage.set('Aucun son n’a été enregistré.');
      return;
    }
    this.startBusy();

    if (this.conversationMode()) {
      const operation = clarificationId
        ? this.api.answerClarificationAudio(this.visitId, clarificationId, audio)
        : this.api.sendAudio(this.visitId, audio);
      operation.subscribe({
        next: response => this.finishMessageResponse(response),
        error: error => this.handleError(error, 'La réponse vocale n’a pas pu être analysée.'),
      });
      return;
    }

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

  private finishMessageResponse(response: AiMessageResponse): void {
    this.updateSessionFromMessage(response);
    this.composerResetToken.update(value => value + 1);
    this.busy.set(false);
    this.speakCurrentAssistantTurn(true);
  }

  private refreshSession(silent = false): void {
    this.api.getSession(this.visitId).subscribe({
      next: response => {
        if (!response) return;
        const previousMessage = this.session()?.assistantMessage;
        this.session.set(response);
        if (this.conversationMode() && response.assistantMessage !== previousMessage) {
          this.speakCurrentAssistantTurn(true);
        }
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

  private completeSessionUpdate(response: AiSessionResponse, speak = false): void {
    this.session.set(response);
    this.busy.set(false);
    if (speak) this.speakCurrentAssistantTurn(true);
  }

  private speakCurrentAssistantTurn(autoListen: boolean): void {
    if (!this.conversationMode() || this.busy() || this.recording()) return;
    const clarification = this.pendingClarification();
    const text = clarification?.question?.trim()
      || this.session()?.assistantMessage?.trim()
      || '';
    if (!text || text === this.lastSpokenText) {
      if (autoListen && this.canAutoListen()) void this.startRecording();
      return;
    }
    this.lastSpokenText = text;
    this.speak(text, autoListen);
  }

  private speak(text: string, autoListen: boolean): void {
    this.stopAssistantAudio();
    this.speaking.set(true);
    this.startSpeechAnimation();
    this.api.synthesizeSpeech(text).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        this.assistantAudioUrl = url;
        const audio = new Audio(url);
        this.assistantAudio = audio;
        audio.onended = () => this.finishSpeaking(autoListen);
        audio.onerror = () => this.finishSpeaking(autoListen);
        void audio.play().catch(() => this.finishSpeaking(false));
      },
      error: () => {
        this.errorMessage.set('La réponse vocale est indisponible ; le texte reste affiché.');
        this.finishSpeaking(autoListen);
      },
    });
  }

  private finishSpeaking(autoListen: boolean): void {
    this.speaking.set(false);
    this.stopSpeechAnimation();
    this.releaseAssistantAudio();
    if (autoListen && this.canAutoListen()) {
      setTimeout(() => void this.startRecording(), 300);
    }
  }

  private canAutoListen(): boolean {
    if (!this.conversationMode() || this.busy() || this.recording() || this.speaking()) return false;
    if (this.session()?.pendingTranscript || this.hasPendingRevision()) return false;
    return !!this.session();
  }

  private pendingClarification(): AiClarification | null {
    return this.session()?.clarifications.find(
      clarification => clarification.status === 'PENDING',
    ) ?? null;
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

    const prescription = this.currentDraft?.['prescription'];
    if (Array.isArray(prescription) && prescription.length > 0) {
      result.prescription = JSON.stringify(prescription);
    }
    const exams = this.currentDraft?.['exams'];
    if (Array.isArray(exams) && exams.length > 0) {
      result.labOrders = JSON.stringify(exams.filter(value => typeof value === 'string' && value.trim()));
    }
    const vitals = this.currentDraft?.['vitals'];
    if (vitals && typeof vitals === 'object' && Object.keys(vitals).length > 0) {
      result.vitals = JSON.stringify(vitals);
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

  private startAudioMeter(stream: MediaStream): void {
    if (typeof window === 'undefined') return;
    try {
      const AudioContextClass = window.AudioContext
        || (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
      if (!AudioContextClass) return;
      this.audioContext = new AudioContextClass();
      this.analyser = this.audioContext.createAnalyser();
      this.analyser.fftSize = 256;
      this.analyser.smoothingTimeConstant = 0.75;
      const source = this.audioContext.createMediaStreamSource(stream);
      source.connect(this.analyser);
      const data = new Uint8Array(this.analyser.fftSize);
      const tick = () => {
        if (!this.analyser || !this.recording()) return;
        this.analyser.getByteTimeDomainData(data);
        let sum = 0;
        for (const sample of data) {
          const normalized = (sample - 128) / 128;
          sum += normalized * normalized;
        }
        const rms = Math.sqrt(sum / data.length);
        this.audioLevel.set(Math.min(1, rms * 4.5));
        this.meterFrame = requestAnimationFrame(tick);
      };
      this.meterFrame = requestAnimationFrame(tick);
    } catch {
      this.audioLevel.set(0.25);
    }
  }

  private startSpeechAnimation(): void {
    const startedAt = performance.now();
    const tick = (time: number) => {
      if (!this.speaking()) return;
      const phase = (time - startedAt) / 180;
      this.audioLevel.set(0.35 + Math.abs(Math.sin(phase)) * 0.55);
      this.speechFrame = requestAnimationFrame(tick);
    };
    this.speechFrame = requestAnimationFrame(tick);
  }

  private stopSpeechAnimation(): void {
    if (this.speechFrame !== null) cancelAnimationFrame(this.speechFrame);
    this.speechFrame = null;
    this.audioLevel.set(0);
  }

  private stopMediaStream(): void {
    if (this.meterFrame !== null) cancelAnimationFrame(this.meterFrame);
    this.meterFrame = null;
    this.analyser = null;
    void this.audioContext?.close().catch(() => undefined);
    this.audioContext = null;
    this.mediaStream?.getTracks().forEach(track => track.stop());
    this.mediaStream = null;
    this.mediaRecorder = null;
    this.audioChunks = [];
    if (!this.speaking()) this.audioLevel.set(0);
  }

  private stopAssistantAudio(): void {
    if (this.assistantAudio) {
      this.assistantAudio.pause();
      this.assistantAudio.currentTime = 0;
    }
    this.speaking.set(false);
    this.stopSpeechAnimation();
    this.releaseAssistantAudio();
  }

  private releaseAssistantAudio(): void {
    this.assistantAudio = null;
    if (this.assistantAudioUrl) URL.revokeObjectURL(this.assistantAudioUrl);
    this.assistantAudioUrl = null;
  }

  private handleError(
    error: { status?: number; error?: { detail?: string; title?: string } },
    fallback: string,
  ): void {
    this.busy.set(false);
    this.recording.set(false);
    this.speaking.set(false);
    this.stopAssistantAudio();
    this.stopMediaStream();
    const detail = error.error?.detail || error.error?.title;
    this.errorMessage.set(detail && !detail.startsWith('AI_') ? detail : fallback);
  }
}
