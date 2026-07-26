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
  AiConsultationApiService,
  AiMessageResponse,
  AiSessionResponse,
} from './ai-consultation-api.service';
import {
  RealtimeVoiceBridgeService,
  RealtimeVoiceState,
} from './realtime-voice-bridge.service';

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
              <span
                class="h-2.5 w-2.5 rounded-full"
                [ngClass]="state().connected ? 'animate-pulse bg-emerald-500' : state().connecting ? 'animate-pulse bg-amber-500' : 'bg-slate-400'"
              ></span>
              <p class="text-xs font-black text-[var(--text-primary)]">
                {{ statusLabel() }}
              </p>
            </div>
            <p class="mt-1 text-[11px] leading-4 text-[var(--text-muted)]">
              {{ i18n.t(
                'consultation.ai.realtimeGovernedHelp',
                'Le micro reste en direct, mais chaque donnée clinique passe par les validations Joprelys.'
              ) }}
            </p>
          </div>

          <button
            type="button"
            (click)="toggleMute()"
            [disabled]="!state().connected || blocked"
            class="inline-flex min-h-11 shrink-0 items-center justify-center gap-2 rounded-[6px] border border-cyan-300 bg-white px-4 py-2 text-xs font-bold text-cyan-800 shadow-sm hover:bg-cyan-50 disabled:cursor-not-allowed disabled:opacity-50 dark:border-cyan-800 dark:bg-slate-950 dark:text-cyan-200"
          >
            <span class="h-2 w-2 rounded-full" [ngClass]="effectiveMuted() ? 'bg-slate-400' : 'animate-pulse bg-rose-500'"></span>
            {{ effectiveMuted()
              ? i18n.t('consultation.ai.realtimeUnmute', 'Réactiver le micro')
              : i18n.t('consultation.ai.realtimeMute', 'Couper le micro') }}
          </button>
        </div>

        @if (state().connected) {
          <div class="mt-3 flex h-9 items-center justify-center gap-[3px]" aria-hidden="true">
            @for (bar of bars; track $index) {
              <span
                class="w-[3px] rounded-full bg-cyan-600/75 transition-all dark:bg-cyan-300/80"
                [class.animate-pulse]="state().userSpeaking || state().assistantSpeaking"
                [style.height.px]="barHeight($index)"
              ></span>
            }
          </div>
          <div class="mt-1 text-center text-[10px] font-bold uppercase tracking-wider text-[var(--text-muted)]">
            @if (blocked) {
              {{ i18n.t('consultation.ai.realtimeValidationPause', 'Micro en pause — validation requise') }}
            } @else if (processing()) {
              {{ i18n.t('consultation.ai.realtimeClinicalAnalysis', 'Analyse clinique sécurisée…') }}
            } @else if (state().assistantSpeaking) {
              {{ i18n.t('consultation.ai.realtimeAssistantSpeaking', 'Joprelys vous répond…') }}
            } @else if (state().userSpeaking) {
              {{ i18n.t('consultation.ai.realtimeListening', 'Je vous écoute…') }}
            } @else {
              {{ i18n.t('consultation.ai.realtimeReady', 'Micro en direct — parlez naturellement') }}
            }
          </div>
        }
      </div>
    }
  `,
})
export class RealtimeVoiceControllerComponent implements OnChanges, OnDestroy {
  private readonly api = inject(AiConsultationApiService);
  private readonly bridge = inject(RealtimeVoiceBridgeService);
  readonly i18n = inject(I18nService);

  @Input({ required: true }) visitId = '';
  @Input() session: AiSessionResponse | null = null;
  @Input() enabled = false;
  @Input() blocked = false;
  @Output() readonly message = new EventEmitter<AiMessageResponse>();
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
  readonly bars = Array.from({ length: 24 });

  private readonly subscriptions = new Subscription();
  private manualMuted = false;
  private lastSpokenMessage = '';
  private connectingForVisit = '';
  private assistantWasSpeaking = false;
  private awaitingAssistantPlayback = false;

  constructor() {
    this.subscriptions.add(this.bridge.state$.subscribe(state => {
      const assistantJustFinished = this.assistantWasSpeaking && !state.assistantSpeaking;
      this.assistantWasSpeaking = state.assistantSpeaking;
      this.state.set(state);
      this.activeChange.emit(state.connected);

      if (assistantJustFinished) {
        this.awaitingAssistantPlayback = false;
        queueMicrotask(() => this.syncMute());
      }
    }));
    this.subscriptions.add(this.bridge.transcript$.subscribe(transcript => {
      this.processTranscript(transcript);
    }));
    this.subscriptions.add(this.bridge.error$.subscribe(message => {
      this.awaitingAssistantPlayback = false;
      this.realtimeError.emit(message);
      this.syncMute();
    }));
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['enabled'] || changes['visitId'] || changes['session']) {
      void this.syncConnection();
    }
    if (changes['blocked']) this.syncMute();
    if (changes['session'] && this.state().connected) this.speakCurrentApprovedTurn();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
    this.bridge.disconnect();
  }

  toggleMute(): void {
    if (!this.state().connected || this.blocked) return;
    this.manualMuted = !this.manualMuted;
    this.syncMute();
  }

  effectiveMuted(): boolean {
    return this.blocked
      || this.manualMuted
      || this.processing()
      || this.awaitingAssistantPlayback
      || this.state().assistantSpeaking
      || this.state().muted;
  }

  statusLabel(): string {
    if (this.state().connecting) {
      return this.i18n.t('consultation.ai.realtimeConnecting', 'Connexion temps réel…');
    }
    if (this.state().connected) {
      return this.i18n.t('consultation.ai.realtimeConnected', 'Copilote Realtime sécurisé');
    }
    return this.i18n.t('consultation.ai.realtimeFallback', 'Mode audio classique disponible');
  }

  barHeight(index: number): number {
    const active = this.state().userSpeaking || this.state().assistantSpeaking;
    const wave = 0.35 + Math.abs(Math.sin((index + 1) * 0.82)) * 0.65;
    return Math.round(6 + wave * (active ? 25 : 8));
  }

  private async syncConnection(): Promise<void> {
    if (!this.enabled || !this.visitId || !this.session) {
      this.connectingForVisit = '';
      this.awaitingAssistantPlayback = false;
      this.bridge.disconnect();
      return;
    }
    if (this.state().connected || this.state().connecting || this.connectingForVisit === this.visitId) {
      this.syncMute();
      return;
    }
    if (!this.bridge.isSupported()) {
      this.activeChange.emit(false);
      this.realtimeError.emit(this.i18n.t(
        'consultation.ai.realtimeBrowserUnsupported',
        'Ce navigateur ne prend pas en charge le mode Realtime. Le mode audio classique reste disponible.',
      ));
      return;
    }

    this.connectingForVisit = this.visitId;
    try {
      await this.bridge.connect(this.visitId);
      this.syncMute();
      this.speakCurrentApprovedTurn();
    } catch (error) {
      this.awaitingAssistantPlayback = false;
      this.bridge.disconnect();
      this.realtimeError.emit(this.realtimeConnectionError(error));
    } finally {
      this.connectingForVisit = '';
    }
  }

  private processTranscript(transcript: string): void {
    const text = transcript.trim();
    if (!text || this.processing() || this.blocked || !this.session || !this.enabled) return;

    this.processing.set(true);
    this.awaitingAssistantPlayback = false;
    this.bridge.setMuted(true);
    const clarification = this.session.clarifications.find(item => item.status === 'PENDING');
    const operation = clarification
      ? this.api.answerClarification(this.visitId, clarification.id, text)
      : this.api.sendText(this.visitId, text);

    operation.subscribe({
      next: response => {
        this.processing.set(false);
        const requiresValidation = response.revisions.some(revision => revision.status === 'PENDING');
        this.message.emit(response);
        const nextQuestion = response.clarifications.find(item => item.status === 'PENDING')?.question?.trim();
        const spokenText = nextQuestion || response.assistantMessage?.trim() || '';

        if (requiresValidation) {
          this.awaitingAssistantPlayback = false;
          this.bridge.setMuted(true);
          this.speakApproved(spokenText, false);
          return;
        }

        if (!this.speakApproved(spokenText, true)) {
          this.awaitingAssistantPlayback = false;
          this.syncMute();
        }
      },
      error: () => {
        this.processing.set(false);
        this.awaitingAssistantPlayback = false;
        this.realtimeError.emit(this.i18n.t(
          'consultation.ai.realtimeClinicalError',
          'La phrase a été entendue, mais son analyse clinique a échoué.',
        ));
        this.syncMute();
      },
    });
  }

  private speakCurrentApprovedTurn(): void {
    const pendingQuestion = this.session?.clarifications.find(
      item => item.status === 'PENDING',
    )?.question?.trim();
    const text = pendingQuestion || this.session?.assistantMessage || '';
    const resumeListening = !this.blocked && !this.hasPendingRevision();
    this.speakApproved(text, resumeListening);
  }

  private speakApproved(message: string, resumeListening: boolean): boolean {
    const text = message.trim();
    if (!text || text === this.lastSpokenMessage || !this.state().connected) return false;
    this.lastSpokenMessage = text;
    if (resumeListening) {
      this.awaitingAssistantPlayback = true;
      this.bridge.setMuted(true);
    }
    this.bridge.speakApproved(text);
    return true;
  }

  private hasPendingRevision(): boolean {
    return this.session?.revisions.some(revision => revision.status === 'PENDING') ?? false;
  }

  private syncMute(): void {
    if (!this.state().connected) return;
    this.bridge.setMuted(
      this.blocked
      || this.manualMuted
      || this.processing()
      || this.awaitingAssistantPlayback
      || this.state().assistantSpeaking,
    );
  }

  private realtimeConnectionError(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return this.i18n.t(
        'consultation.ai.realtimeUnavailable',
        'Le temps réel est indisponible. Joprelys conserve le mode audio classique.',
      );
    }

    const detail = this.errorDetail(error);
    switch (detail) {
      case 'AI_REALTIME_NOT_CONFIGURED':
        return this.i18n.t(
          'consultation.ai.realtimeNotConfigured',
          'OpenAI Realtime n’est pas configuré sur cet environnement. Le mode audio classique reste disponible.',
        );
      case 'AI_REALTIME_QUOTA_OR_BUDGET':
        return this.i18n.t(
          'consultation.ai.realtimeQuota',
          'OpenAI Realtime refuse la connexion pour quota ou budget. Vérifiez le projet et la facturation OpenAI.',
        );
      case 'AI_REALTIME_UPSTREAM_AUTH':
        return this.i18n.t(
          'consultation.ai.realtimeAuth',
          'La clé ou le projet OpenAI n’autorise pas Realtime. Vérifiez les droits de la clé configurée.',
        );
      case 'AI_REALTIME_CONFIG_REJECTED':
        return this.i18n.t(
          'consultation.ai.realtimeConfigRejected',
          'OpenAI a refusé la configuration Realtime de cette session. Le détail est journalisé côté serveur.',
        );
      case 'AI_REALTIME_MODEL_OR_ENDPOINT_UNAVAILABLE':
        return this.i18n.t(
          'consultation.ai.realtimeModelUnavailable',
          'Le modèle Realtime configuré n’est pas disponible pour ce projet OpenAI.',
        );
      case 'AI_SESSION_EXPIRED':
        return this.i18n.t(
          'consultation.ai.realtimeSessionExpired',
          'La session IA a expiré. Relancez le copilote pour rétablir le temps réel.',
        );
      default:
        if (error.status === 403) {
          return this.i18n.t(
            'consultation.ai.realtimeForbidden',
            'Votre rôle n’autorise pas le canal Realtime de cette consultation.',
          );
        }
        if (error.status === 0) {
          return this.i18n.t(
            'consultation.ai.realtimeNetworkError',
            'La connexion Realtime n’a pas atteint le serveur. Vérifiez le réseau, le proxy HTTPS et WebRTC.',
          );
        }
        return this.i18n.t(
          'consultation.ai.realtimeUnavailable',
          'Le temps réel est indisponible. Joprelys conserve le mode audio classique.',
        );
    }
  }

  private errorDetail(error: HttpErrorResponse): string {
    const body = error.error;
    if (typeof body === 'string') return body;
    if (!body || typeof body !== 'object') return '';
    const value = (body as { detail?: unknown; title?: unknown; message?: unknown }).detail
      ?? (body as { title?: unknown }).title
      ?? (body as { message?: unknown }).message;
    return typeof value === 'string' ? value : '';
  }
}
