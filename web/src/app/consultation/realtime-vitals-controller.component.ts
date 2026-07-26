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

@Component({
  selector: 'app-realtime-vitals-controller',
  standalone: true,
  imports: [CommonModule],
  providers: [RealtimeVoiceBridgeService],
  template: `
    <div class="rounded-[8px] border border-cyan-200 bg-cyan-50/60 p-3 dark:border-cyan-900 dark:bg-cyan-950/20">
      <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div class="min-w-0">
          <div class="flex items-center gap-2">
            <span
              class="h-2.5 w-2.5 rounded-full"
              [ngClass]="state().connected ? 'animate-pulse bg-emerald-500' : state().connecting ? 'animate-pulse bg-amber-500' : 'bg-rose-500'"
            ></span>
            <p class="text-xs font-black text-[var(--text-primary)]">
              {{ state().connected
                ? i18n.t('vitals.assistant.realtimeConnected', 'Constantes Realtime')
                : state().connecting
                  ? i18n.t('vitals.assistant.realtimeRecovering', 'Reconnexion Realtime…')
                  : i18n.t('vitals.assistant.realtimeDisconnected', 'Audio Realtime interrompu') }}
            </p>
          </div>
          <p class="mt-1 text-[11px] leading-4 text-[var(--text-muted)]">
            @if (state().connected) {
              {{ i18n.t(
                'vitals.assistant.realtimeHelp',
                'Dictez plusieurs paramètres naturellement. Joprelys préremplit uniquement les valeurs sûres.'
              ) }}
            } @else {
              {{ i18n.t(
                'vitals.assistant.realtimeRecoveryHelp',
                'Attendez le retour du voyant vert avant de dicter : le microphone est en cours de rétablissement.'
              ) }}
            }
          </p>
        </div>

        @if (state().connected) {
          <button
            type="button"
            (click)="toggleMute()"
            [disabled]="disabled || processing()"
            class="inline-flex min-h-11 shrink-0 items-center justify-center gap-2 rounded-[6px] border border-cyan-300 bg-white px-4 py-2 text-xs font-bold text-cyan-800 disabled:opacity-50 dark:border-cyan-800 dark:bg-slate-950 dark:text-cyan-200"
          >
            <span class="h-2 w-2 rounded-full" [ngClass]="effectiveMuted() ? 'bg-slate-400' : 'animate-pulse bg-rose-500'"></span>
            {{ effectiveMuted()
              ? i18n.t('vitals.assistant.realtimeUnmute', 'Réactiver le micro')
              : i18n.t('vitals.assistant.realtimeMute', 'Couper le micro') }}
          </button>
        }
      </div>

      @if (state().connected) {
        <div class="mt-3 flex h-10 items-center justify-center gap-[3px]" aria-hidden="true">
          @for (bar of bars; track $index) {
            <span
              class="w-[3px] rounded-full bg-cyan-600/80 transition-all dark:bg-cyan-300/80"
              [class.animate-pulse]="state().userSpeaking || state().assistantSpeaking"
              [style.height.px]="barHeight($index)"
            ></span>
          }
        </div>
        <p class="mt-1 text-center text-[10px] font-bold uppercase tracking-wider text-[var(--text-muted)]">
          @if (processing()) {
            {{ i18n.t('vitals.assistant.processing', 'Analyse des constantes…') }}
          } @else if (state().assistantSpeaking) {
            {{ i18n.t('vitals.assistant.speaking', 'Joprelys vous répond…') }}
          } @else if (state().userSpeaking) {
            {{ i18n.t('vitals.assistant.listening', 'Je vous écoute…') }}
          } @else if (pendingConfirmationContext()) {
            {{ i18n.t('vitals.assistant.realtimeAwaitingConfirmation', 'Répondez à la clarification ou redictez la valeur') }}
          } @else {
            {{ i18n.t('vitals.assistant.realtimeReady', 'Micro en direct — dictez les paramètres') }}
          }
        </p>
      } @else {
        <div class="mt-3 rounded-[4px] border border-amber-200 bg-amber-50 px-3 py-2 text-[11px] font-bold text-amber-800 dark:border-amber-900 dark:bg-amber-950/20 dark:text-amber-200">
          {{ state().connecting
            ? i18n.t('vitals.assistant.realtimeRecovering', 'Reconnexion Realtime…')
            : i18n.t('vitals.assistant.realtimeDisconnectedHelp', 'N’enregistrez pas les constantes vocalement tant que le voyant n’est pas vert.') }}
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
  readonly bars = Array.from({ length: 24 });

  private readonly subscriptions = new Subscription();
  private manualMuted = false;
  private connectingForVisit = '';

  constructor() {
    this.subscriptions.add(this.bridge.state$.subscribe(state => {
      const wasConnected = this.state().connected;
      this.state.set(state);
      this.activeChange.emit(state.connected);
      if (state.connected && !wasConnected) this.syncMute();
    }));
    this.subscriptions.add(this.bridge.transcript$.subscribe(turn => {
      this.processTranscript(turn);
    }));
    this.subscriptions.add(this.bridge.error$.subscribe(message => {
      this.realtimeError.emit(message);
    }));
    this.subscriptions.add(this.bridge.assistantTurnCompleted$.subscribe(() => {
      setTimeout(() => this.syncMute(), 120);
    }));
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['enabled'] || changes['visitId']) {
      void this.syncConnection();
    }
    if (changes['disabled']) this.syncMute();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
    this.bridge.disconnect();
  }

  toggleMute(): void {
    if (!this.state().connected || this.disabled || this.processing()) return;
    this.manualMuted = !this.manualMuted;
    this.syncMute();
  }

  effectiveMuted(): boolean {
    return this.disabled
      || this.processing()
      || this.manualMuted
      || this.state().assistantSpeaking
      || this.state().muted;
  }

  barHeight(index: number): number {
    const active = this.state().userSpeaking || this.state().assistantSpeaking;
    const wave = 0.35 + Math.abs(Math.sin((index + 1) * 0.9)) * 0.65;
    return Math.round(6 + wave * (active ? 27 : 8));
  }

  private async syncConnection(): Promise<void> {
    if (!this.enabled || !this.visitId) {
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
      this.realtimeError.emit(this.i18n.t(
        'vitals.assistant.realtimeUnsupported',
        'Ce navigateur ne prend pas en charge la connexion audio temps réel.',
      ));
      return;
    }

    this.connectingForVisit = this.visitId;
    try {
      await this.bridge.connect(this.visitId, 'vitals');
      this.syncMute();
    } catch (error) {
      this.realtimeError.emit(
        error instanceof Error && error.message.trim()
          ? error.message
          : this.i18n.t(
              'vitals.assistant.realtimeUnavailable',
              'Le mode Realtime est momentanément indisponible. Reconnexion automatique en cours.',
            ),
      );
    } finally {
      this.connectingForVisit = '';
    }
  }

  private processTranscript(turn: RealtimeTranscriptTurn): void {
    const transcript = turn.transcript.trim();
    if (!transcript || this.processing() || this.disabled || !this.state().connected) return;
    if (turn.confidence === null
      || !Number.isFinite(turn.confidence)
      || turn.confidence < REALTIME_CONFIDENCE_FLOOR) {
      this.realtimeError.emit(this.i18n.t(
        'vitals.assistant.realtimeLowConfidence',
        'Valeur vocale trop incertaine : aucune constante n’a été proposée. Répétez la mesure.',
      ));
      return;
    }

    const confirmationContext = this.pendingConfirmationContext();
    const modelText = confirmationContext
      ? `${confirmationContext}\nClinician confirmation or correction: ${transcript}`
      : transcript;

    this.processing.set(true);
    this.bridge.setMuted(true);
    this.api.analyzeText(
      this.visitId,
      modelText,
      this.i18n.currentLanguage(),
      this.currentVitals,
    ).subscribe({
      next: proposal => {
        this.processing.set(false);
        const userFacingProposal: AiVitalsProposal = {
          ...proposal,
          transcript,
        };
        if (proposal.needsConfirmation) {
          this.pendingConfirmationContext.set([
            `Previous ambiguous vitals utterance: ${transcript}`,
            `Assistant clarification: ${proposal.assistantMessage}`,
            proposal.confirmationReason
              ? `Reason: ${proposal.confirmationReason}`
              : '',
          ].filter(Boolean).join('\n'));
        } else {
          this.pendingConfirmationContext.set('');
        }
        this.proposed.emit(userFacingProposal);
        if (proposal.assistantMessage) {
          this.bridge.setMuted(true);
          const started = this.bridge.speakApproved(proposal.assistantMessage);
          if (!started) this.syncMute();
        } else {
          this.syncMute();
        }
      },
      error: () => {
        this.processing.set(false);
        this.realtimeError.emit(this.i18n.t(
          'vitals.assistant.error',
          'Joprelys n’a pas pu analyser ces constantes. Attendez le rétablissement ou saisissez-les manuellement.',
        ));
        this.syncMute();
      },
    });
  }

  private syncMute(): void {
    this.bridge.setMuted(
      this.disabled
      || this.processing()
      || this.manualMuted
      || this.state().assistantSpeaking,
    );
  }
}
