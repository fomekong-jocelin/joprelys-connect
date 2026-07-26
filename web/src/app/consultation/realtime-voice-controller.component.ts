import { CommonModule } from '@angular/common';
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
              <span class="h-2.5 w-2.5 rounded-full"
                [ngClass]="state().connected ? 'animate-pulse bg-emerald-500' : state().connecting ? 'animate-pulse bg-amber-500' : 'bg-rose-500'">
              </span>
              <p class="text-xs font-black text-[var(--text-primary)]">{{ statusLabel() }}</p>
            </div>
            <p class="mt-1 text-[11px] leading-4 text-[var(--text-muted)]">
              @if (state().connected) {
                {{ i18n.t('consultation.ai.realtimeGovernedHelp', 'Le micro reste en direct, mais chaque donnée clinique passe par les validations Joprelys.') }}
              } @else {
                {{ i18n.t('consultation.ai.realtimeRecoveryHelp', 'Ne poursuivez pas la dictée tant que la reconnexion n’est pas terminée : Joprelys rétablit automatiquement le microphone.') }}
              }
            </p>
          </div>
          <button type="button" (click)="toggleMute()" [disabled]="!state().connected || blocked"
            class="inline-flex min-h-11 shrink-0 items-center justify-center gap-2 rounded-[6px] border border-cyan-300 bg-white px-4 py-2 text-xs font-bold text-cyan-800 shadow-sm hover:bg-cyan-50 disabled:cursor-not-allowed disabled:opacity-50 dark:border-cyan-800 dark:bg-slate-950 dark:text-cyan-200">
            <span class="h-2 w-2 rounded-full" [ngClass]="effectiveMuted() ? 'bg-slate-400' : 'animate-pulse bg-rose-500'"></span>
            {{ effectiveMuted() ? i18n.t('consultation.ai.realtimeUnmute', 'Réactiver le micro') : i18n.t('consultation.ai.realtimeMute', 'Couper le micro') }}
          </button>
        </div>
        @if (state().connected) {
          <div class="mt-3 flex h-9 items-center justify-center gap-[3px]" aria-hidden="true">
            @for (bar of bars; track $index) {
              <span class="w-[3px] rounded-full bg-cyan-600/75 transition-all dark:bg-cyan-300/80"
                [class.animate-pulse]="state().userSpeaking || state().assistantSpeaking"
                [style.height.px]="barHeight($index)"></span>
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
        } @else {
          <div class="mt-3 rounded-[4px] border border-amber-200 bg-amber-50 px-3 py-2 text-[11px] font-bold text-amber-800 dark:border-amber-900 dark:bg-amber-950/20 dark:text-amber-200">
            {{ state().connecting
              ? i18n.t('consultation.ai.realtimeRecovering', 'Reconnexion audio en cours…')
              : i18n.t('consultation.ai.realtimeDisconnected', 'Audio temps réel interrompu — n’enregistrez pas tant que le voyant n’est pas vert.') }}
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

  readonly state = signal<RealtimeVoiceState>({ connected: false, connecting: false, userSpeaking: false, assistantSpeaking: false, muted: false });
  readonly processing = signal(false);
  readonly bars = Array.from({ length: 24 });

  private readonly subscriptions = new Subscription();
  private manualMuted = false;
  private lastSpokenMessage = '';
  private connectingForVisit = '';

  constructor() {
    this.subscriptions.add(this.bridge.state$.subscribe(state => {
      const wasConnected = this.state().connected;
      this.state.set(state);
      this.activeChange.emit(state.connected);
      if (state.connected && !wasConnected) {
        this.syncMute();
        this.speakCurrentApprovedTurn();
      }
    }));
    this.subscriptions.add(this.bridge.transcript$.subscribe(transcript => this.processTranscript(transcript)));
    this.subscriptions.add(this.bridge.error$.subscribe(message => this.realtimeError.emit(message)));
    this.subscriptions.add(this.bridge.assistantTurnCompleted$.subscribe(() => {
      setTimeout(() => this.syncMute(), 120);
    }));
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['enabled'] || changes['visitId'] || changes['session']) void this.syncConnection();
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
    return this.blocked || this.manualMuted || this.processing() || this.state().muted;
  }

  statusLabel(): string {
    if (this.state().connecting) return this.i18n.t('consultation.ai.realtimeRecovering', 'Reconnexion audio en cours…');
    if (this.state().connected) return this.i18n.t('consultation.ai.realtimeConnected', 'Copilote Realtime sécurisé');
    return this.i18n.t('consultation.ai.realtimeDisconnected', 'Audio temps réel interrompu');
  }

  barHeight(index: number): number {
    const active = this.state().userSpeaking || this.state().assistantSpeaking;
    const wave = 0.35 + Math.abs(Math.sin((index + 1) * 0.82)) * 0.65;
    return Math.round(6 + wave * (active ? 25 : 8));
  }

  private async syncConnection(): Promise<void> {
    if (!this.enabled || !this.visitId || !this.session) {
      this.connectingForVisit = '';
      this.bridge.disconnect();
      return;
    }
    if (this.state().connected || this.state().connecting || this.connectingForVisit === this.visitId) {
      this.syncMute();
      return;
    }
    if (!this.bridge.isSupported()) {
      this.activeChange.emit(false);
      this.realtimeError.emit(this.i18n.t('consultation.ai.realtimeUnsupported', 'Ce navigateur ne prend pas en charge la connexion audio temps réel.'));
      return;
    }
    this.connectingForVisit = this.visitId;
    try {
      await this.bridge.connect(this.visitId);
      const spoken = this.speakCurrentApprovedTurn();
      if (!spoken) this.syncMute();
    } catch (error) {
      this.realtimeError.emit(error instanceof Error && error.message.trim()
        ? error.message
        : this.i18n.t('consultation.ai.realtimeUnavailable', 'Le temps réel est indisponible. Reconnexion automatique en cours.'));
    } finally {
      this.connectingForVisit = '';
    }
  }

  private processTranscript(transcript: string): void {
    const text = transcript.trim();
    if (!text || this.processing() || this.blocked || !this.session || !this.enabled) return;
    const pendingClarification = this.session.clarifications.find(item => item.status === 'PENDING');
    this.processing.set(true);
    this.bridge.setMuted(true);
    this.lastSpokenMessage = '';
    const request = pendingClarification
      ? this.api.answerClarification(this.visitId, pendingClarification.id, text)
      : this.api.sendText(this.visitId, text);
    request.subscribe({
      next: response => {
        this.processing.set(false);
        const requiresDecision = response.revisions.some(revision => revision.status === 'PENDING');
        this.bridge.setMuted(this.manualMuted || requiresDecision);
        this.message.emit(response);
      },
      error: () => {
        this.processing.set(false);
        this.realtimeError.emit(this.i18n.t(
          'consultation.ai.realtimeClinicalError',
          'La phrase a été entendue, mais son analyse clinique a échoué. Ne poursuivez pas tant que la connexion applicative n’est pas rétablie.',
        ));
        this.syncMute();
      },
    });
  }

  private speakCurrentApprovedTurn(): boolean {
    const pendingQuestion = this.session?.clarifications.find(item => item.status === 'PENDING')?.question?.trim();
    return this.speakApproved(pendingQuestion || this.session?.assistantMessage || '');
  }

  private speakApproved(message: string): boolean {
    const text = message.trim();
    if (!text || text === this.lastSpokenMessage || !this.state().connected) return false;
    this.lastSpokenMessage = text;
    const started = this.bridge.speakApproved(text);
    if (!started) this.syncMute();
    return started;
  }

  private syncMute(): void {
    this.bridge.setMuted(this.blocked || this.manualMuted || this.processing());
  }
}
