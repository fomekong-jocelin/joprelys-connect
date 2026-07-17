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
  imports: [CommonModule],
  template: `
    <section class="overflow-hidden rounded-[6px] border border-[var(--app-border)] bg-[var(--app-surface)] shadow-sm">
      <header class="flex flex-col gap-3 border-b border-[var(--app-border)] bg-[var(--app-surface-muted)] px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
        <div class="flex items-center gap-3">
          <span class="inline-flex h-10 w-10 items-center justify-center rounded-[6px] bg-cyan-50 text-cyan-700 dark:bg-cyan-950/30 dark:text-cyan-300">
            <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 18.75a6 6 0 006-6v-1.5m-12 0v1.5a6 6 0 006 6m0 0v3m-3 0h6M12 15.75a3 3 0 003-3V6a3 3 0 10-6 0v6.75a3 3 0 003 3z" />
            </svg>
          </span>
          <div>
            <h2 class="text-sm font-bold text-[var(--text-primary)]">
              {{ i18n.t('consultation.ai.title', 'Assistant vocal IA') }}
            </h2>
            <p class="text-xs text-[var(--text-muted)]">
              {{ i18n.t('consultation.ai.subtitleReview', 'Dictez, corrigez la transcription, puis lancez l’analyse clinique.') }}
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
                {{ i18n.t('consultation.ai.startTitle', 'Démarrer une session de dictée') }}
              </p>
              <p class="mt-1 text-xs leading-5 text-[var(--text-muted)]">
                {{ i18n.t('consultation.ai.startReviewHelp', 'La transcription doit être relue avant toute analyse. Aucune donnée n’est appliquée automatiquement.') }}
              </p>
              <button
                type="button"
                (click)="startSession()"
                [disabled]="busy() || !visitId"
                class="mt-3 inline-flex items-center justify-center rounded-[4px] bg-[var(--brand-primary)] px-4 py-2 text-sm font-semibold text-white hover:bg-[var(--brand-primary-hover)] disabled:opacity-50"
              >
                {{ busy() ? i18n.t('consultation.ai.starting', 'Démarrage…') : i18n.t('consultation.ai.start', 'Activer l’assistant vocal') }}
              </button>
            </div>
          } @else {
            <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
              <button
                type="button"
                (click)="toggleRecording()"
                [disabled]="busy() || !mediaRecorderSupported || !!pendingTranscript()"
                class="inline-flex min-h-12 flex-1 items-center justify-center gap-2 rounded-[6px] px-4 py-3 text-sm font-bold text-white disabled:cursor-not-allowed disabled:opacity-50"
                [ngClass]="recording() ? 'bg-rose-600 hover:bg-rose-700' : 'bg-[var(--brand-primary)] hover:bg-[var(--brand-primary-hover)]'"
              >
                @if (recording()) {
                  <span class="h-3 w-3 animate-pulse rounded-[2px] bg-white"></span>
                  {{ i18n.t('consultation.ai.stopRecordingTranscribe', 'Arrêter et préparer la transcription') }}
                } @else {
                  {{ i18n.t('consultation.ai.record', 'Démarrer la dictée') }}
                }
              </button>
              <button
                type="button"
                (click)="deleteSession()"
                [disabled]="busy() || recording()"
                class="inline-flex min-h-12 items-center justify-center rounded-[6px] border border-[var(--app-border)] bg-[var(--app-surface)] px-4 py-3 text-xs font-semibold text-[var(--text-secondary)] hover:bg-[var(--app-surface-muted)] disabled:opacity-50"
              >
                {{ i18n.t('consultation.ai.reset', 'Terminer la session') }}
              </button>
            </div>

            @if (pendingTranscript()) {
              <div class="space-y-3 rounded-[6px] border border-amber-200 bg-amber-50/70 p-4 dark:border-amber-900 dark:bg-amber-950/20">
                <div>
                  <p class="text-xs font-bold uppercase tracking-wider text-amber-800 dark:text-amber-300">
                    {{ i18n.t('consultation.ai.pendingTranscript', 'Transcription à relire') }}
                  </p>
                  <p class="mt-1 text-xs leading-5 text-amber-700 dark:text-amber-400">
                    {{ i18n.t('consultation.ai.pendingTranscriptHelp', 'Corrigez les noms, nombres, doses, négations et côtés avant de lancer l’analyse.') }}
                  </p>
                </div>
                <textarea
                  rows="6"
                  [value]="editableTranscript()"
                  (input)="onTranscriptInput($event)"
                  class="ui-textarea w-full resize-y rounded-[4px] border-amber-300 bg-[var(--app-surface)] p-3 text-sm text-[var(--text-primary)] focus:border-[var(--brand-primary)] focus:outline-none"
                ></textarea>
                <div class="flex flex-col gap-2 sm:flex-row">
                  <button
                    type="button"
                    (click)="analyzeTranscript()"
                    [disabled]="busy() || !editableTranscript().trim()"
                    class="inline-flex items-center justify-center rounded-[4px] bg-[var(--brand-primary)] px-4 py-2.5 text-sm font-semibold text-white hover:bg-[var(--brand-primary-hover)] disabled:opacity-50"
                  >
                    {{ busy() ? i18n.t('consultation.ai.processing', 'Traitement…') : i18n.t('consultation.ai.confirmAnalyze', 'Confirmer et analyser') }}
                  </button>
                  <button
                    type="button"
                    (click)="discardPendingTranscript()"
                    [disabled]="busy()"
                    class="inline-flex items-center justify-center rounded-[4px] border border-[var(--app-border)] bg-[var(--app-surface)] px-4 py-2.5 text-sm font-semibold text-[var(--text-secondary)] hover:bg-[var(--app-surface-muted)] disabled:opacity-50"
                  >
                    {{ i18n.t('consultation.ai.discardTranscript', 'Abandonner cette transcription') }}
                  </button>
                </div>
              </div>
            } @else {
              <div class="space-y-2">
                <label class="ui-label text-xs font-semibold">
                  {{ i18n.t('consultation.ai.textFallback', 'Message ou correction à transmettre à l’IA') }}
                </label>
                <textarea
                  rows="2"
                  [value]="textMessage()"
                  (input)="onTextInput($event)"
                  [placeholder]="i18n.t('consultation.ai.textPlaceholder', 'Ex : Corrige, la douleur est à droite et non à gauche…')"
                  class="ui-textarea w-full resize-none rounded-[4px] border-[var(--app-border)] bg-transparent p-2.5 text-sm text-[var(--text-primary)]"
                ></textarea>
                <button
                  type="button"
                  (click)="sendText()"
                  [disabled]="busy() || !textMessage().trim()"
                  class="inline-flex items-center justify-center rounded-[4px] border border-[var(--app-border)] bg-[var(--app-surface-muted)] px-3 py-2 text-xs font-semibold text-[var(--text-primary)] hover:bg-slate-100 disabled:opacity-50 dark:hover:bg-slate-800"
                >
                  {{ busy() ? i18n.t('consultation.ai.processing', 'Traitement…') : i18n.t('consultation.ai.sendText', 'Envoyer à l’assistant') }}
                </button>
              </div>
            }

            @if (session()?.transcript && !pendingTranscript()) {
              <div class="rounded-[4px] border border-[var(--app-border)] bg-[var(--app-surface-muted)]/40 p-3">
                <p class="text-[10px] font-bold uppercase tracking-wider text-[var(--text-muted)]">
                  {{ i18n.t('consultation.ai.validatedTranscript', 'Dernière transcription validée') }}
                </p>
                <p class="mt-1 whitespace-pre-wrap text-xs leading-5 text-[var(--text-primary)]">{{ session()?.transcript }}</p>
              </div>
            }

            @if (session()?.assistantMessage) {
              <div
                class="rounded-[4px] border p-3 text-xs leading-5"
                [ngClass]="session()?.needsClarification
                  ? 'border-amber-200 bg-amber-50 text-amber-800 dark:border-amber-900 dark:bg-amber-950/30 dark:text-amber-300'
                  : 'border-cyan-200 bg-cyan-50 text-cyan-800 dark:border-cyan-900 dark:bg-cyan-950/30 dark:text-cyan-300'"
              >
                {{ session()?.assistantMessage }}
              </div>
            }

            @if (draftEntries().length > 0) {
              <div class="space-y-2">
                <p class="text-xs font-bold uppercase tracking-wider text-[var(--text-muted)]">
                  {{ i18n.t('consultation.ai.proposal', 'Brouillon proposé') }}
                </p>
                <div class="grid grid-cols-1 gap-2 sm:grid-cols-2">
                  @for (entry of draftEntries(); track entry.key) {
                    <div class="rounded-[4px] border border-[var(--app-border)] bg-[var(--app-surface-muted)]/30 p-3">
                      <p class="text-[10px] font-bold uppercase tracking-wider text-[var(--text-muted)]">{{ fieldLabel(entry.key) }}</p>
                      <p class="mt-1 line-clamp-4 whitespace-pre-wrap text-xs leading-5 text-[var(--text-primary)]">{{ entry.value }}</p>
                    </div>
                  }
                </div>
                <button
                  type="button"
                  (click)="applyCurrentDraft()"
                  class="inline-flex w-full items-center justify-center rounded-[6px] bg-emerald-600 px-4 py-3 text-sm font-bold text-white hover:bg-emerald-700 sm:w-auto"
                >
                  {{ i18n.t('consultation.ai.apply', 'Appliquer au formulaire') }}
                </button>
              </div>
            }
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
  readonly textMessage = signal('');
  readonly editableTranscript = signal('');
  readonly errorMessage = signal('');

  readonly mediaRecorderSupported =
    typeof window !== 'undefined' && 'MediaRecorder' in window && !!navigator.mediaDevices?.getUserMedia;

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
    if (this.qrCodeUrl()) URL.revokeObjectURL(this.qrCodeUrl()!);
  }

  pendingTranscript(): string {
    return this.session()?.pendingTranscript?.trim() ?? '';
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

  sendText(): void {
    const text = this.textMessage().trim();
    if (!text || this.busy()) return;
    this.startBusy();
    this.api.sendText(this.visitId, text).subscribe({
      next: response => {
        this.updateSessionFromMessage(response);
        this.textMessage.set('');
        this.busy.set(false);
      },
      error: error => this.handleError(error, 'Le message n’a pas pu être analysé.'),
    });
  }

  analyzeTranscript(): void {
    const transcript = this.editableTranscript().trim();
    if (!transcript || this.busy()) return;
    this.startBusy();
    this.api.analyzeTranscript(this.visitId, transcript).subscribe({
      next: response => {
        this.updateSessionFromMessage(response);
        this.editableTranscript.set('');
        this.busy.set(false);
      },
      error: error => this.handleError(error, 'La transcription n’a pas pu être analysée.'),
    });
  }

  discardPendingTranscript(): void {
    if (!this.pendingTranscript() || this.busy()) return;
    this.startBusy();
    this.api.discardPendingTranscript(this.visitId).subscribe({
      next: () => {
        this.session.update(current => current ? {
          ...current,
          pendingTranscript: null,
          transcriptStatus: 'NONE',
        } : current);
        this.editableTranscript.set('');
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
        this.editableTranscript.set('');
        this.busy.set(false);
      },
      error: error => this.handleError(error, 'Impossible de terminer la session IA.'),
    });
  }

  applyCurrentDraft(): void {
    const draft = this.session()?.draft;
    if (draft) this.applyDraft.emit({ ...draft });
  }

  onTextInput(event: Event): void {
    this.textMessage.set((event.target as HTMLTextAreaElement).value);
  }

  onTranscriptInput(event: Event): void {
    this.editableTranscript.set((event.target as HTMLTextAreaElement).value);
  }

  draftEntries(): Array<{ key: AiField; value: string }> {
    const draft = this.session()?.draft ?? {};
    return (Object.entries(draft) as Array<[AiField, string]>)
      .filter(([, value]) => !!value?.trim())
      .map(([key, value]) => ({ key, value }));
  }

  fieldLabel(field: AiField): string {
    const labels: Record<AiField, string> = {
      symptoms: this.i18n.t('consultation.symptoms.label', 'Symptômes'),
      clinicalExam: this.i18n.t('consultation.clinicalExam.label', 'Examen clinique'),
      suspectedDiagnosis: this.i18n.t('consultation.suspectedDiagnosis.label', 'Hypothèse diagnostique'),
      diagnosis: this.i18n.t('consultation.diagnosis.label', 'Diagnostic'),
      finalDiagnosis: this.i18n.t('consultation.finalDiagnosis.label', 'Diagnostic final'),
      conclusion: this.i18n.t('consultation.conclusion.label', 'Conclusion'),
      advice: this.i18n.t('consultation.advice.label', 'Conseils au patient'),
      followUp: this.i18n.t('consultation.followUp.label', 'Suivi recommandé'),
    };
    return labels[field];
  }

  private async startRecording(): Promise<void> {
    if (!this.mediaRecorderSupported || this.busy() || this.pendingTranscript()) return;
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
    const audio = new Blob(this.audioChunks, { type: this.mediaRecorder?.mimeType || 'audio/webm' });
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
        this.editableTranscript.set(response.transcript);
        this.busy.set(false);
      },
      error: error => this.handleError(error, 'La dictée n’a pas pu être transcrite.'),
    });
  }

  private refreshSession(silent = false): void {
    this.api.getSession(this.visitId).subscribe({
      next: response => {
        if (!response) return;
        this.session.set(response);
        if (response.pendingTranscript && !this.editableTranscript()) {
          this.editableTranscript.set(response.pendingTranscript);
        }
      },
      error: error => {
        if (!silent && error.status !== 404) this.handleError(error, 'Assistant IA indisponible.');
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
      transcriptStatus: response.transcript ? 'ANALYZED' : previous?.transcriptStatus ?? 'NONE',
      assistantMessage: response.assistantMessage,
      needsClarification: response.needsClarification,
    });
  }

  private completeSessionUpdate(response: AiSessionResponse): void {
    this.session.set(response);
    this.editableTranscript.set(response.pendingTranscript ?? '');
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

  private handleError(error: { status?: number; error?: { detail?: string; title?: string } }, fallback: string): void {
    this.busy.set(false);
    this.recording.set(false);
    this.stopMediaStream();
    const detail = error.error?.detail || error.error?.title;
    this.errorMessage.set(detail && !detail.startsWith('AI_') ? detail : fallback);
  }
}
