import { Component, Input, OnDestroy, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { AmbientSpeakerReviewComponent } from './ambient-speaker-review.component';
import { DoctorVoiceCalibrationComponent } from './doctor-voice-calibration.component';
import {
  RealtimeTranscriptTurn,
  RealtimeVoiceBridgeService,
  RealtimeVoiceState,
} from './realtime-voice-bridge.service';

const INITIAL_REALTIME_STATE: RealtimeVoiceState = {
  connected: false,
  connecting: false,
  userSpeaking: false,
  assistantSpeaking: false,
  muted: false,
};

@Component({
  selector: 'app-ambient-safety-panel',
  standalone: true,
  imports: [DoctorVoiceCalibrationComponent, AmbientSpeakerReviewComponent],
  template: `
    <div class="space-y-3">
      <section class="rounded-[6px] border border-[var(--app-border)] bg-[var(--app-surface)] p-3 shadow-sm">
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0">
            <p class="text-xs font-extrabold uppercase tracking-wide text-[var(--text-muted)]">
              {{ i18n.t('consultation.ai.realtimeHearingTitle', 'Ce que Joprelys entend réellement') }}
            </p>
            <p class="mt-1 text-sm font-black text-[var(--text-primary)]">
              {{ hearingStatus() }}
            </p>
          </div>

          <span
            class="inline-flex shrink-0 items-center gap-1.5 rounded-[4px] px-2 py-1 text-[10px] font-extrabold"
            [class.bg-emerald-100]="state().connected"
            [class.text-emerald-800]="state().connected"
            [class.bg-amber-100]="!state().connected"
            [class.text-amber-800]="!state().connected"
          >
            <span
              class="h-2 w-2 rounded-full"
              [class.animate-pulse]="state().userSpeaking"
              [class.bg-emerald-500]="state().connected"
              [class.bg-amber-500]="!state().connected"
            ></span>
            {{ state().userSpeaking
              ? i18n.t('consultation.ai.realtimeVoiceDetected', 'Voix détectée')
              : state().connected
                ? i18n.t('consultation.ai.realtimeListeningConfirmed', 'Micro reçu par OpenAI')
                : i18n.t('consultation.ai.realtimeWaitingConnection', 'Connexion en cours') }}
          </span>
        </div>

        <div class="mt-3 rounded-[5px] border border-[var(--app-border)] bg-[var(--app-surface-muted)] px-3 py-3">
          @if (lastTranscript()) {
            <p class="text-[10px] font-extrabold uppercase tracking-wide text-[var(--text-muted)]">
              {{ i18n.t('consultation.ai.realtimeLastHeard', 'Dernier texte reconnu') }}
            </p>
            <p class="mt-1 text-sm font-semibold leading-6 text-[var(--text-primary)]">
              {{ lastTranscript() }}
            </p>
            @if (lastConfidence() === null) {
              <p class="mt-1 text-[10px] font-semibold text-[var(--text-muted)]">
                {{ i18n.t(
                  'consultation.ai.realtimeConfidenceUnavailable',
                  'Transcription reçue. Le score de confiance n’est pas disponible : elle reste visible mais n’est pas validée cliniquement automatiquement.'
                ) }}
              </p>
            }
          } @else if (state().userSpeaking) {
            <p class="text-sm font-semibold text-[var(--text-primary)]">
              {{ i18n.t('consultation.ai.realtimeTranscribingNow', 'Voix reçue — transcription en cours…') }}
            </p>
          } @else if (state().connected) {
            <p class="text-sm text-[var(--text-muted)]">
              {{ i18n.t('consultation.ai.realtimeSpeakToTest', 'Parlez normalement. Dès qu’OpenAI finalise une phrase, elle s’affiche ici.') }}
            </p>
          } @else {
            <p class="text-sm text-[var(--text-muted)]">
              {{ i18n.t('consultation.ai.realtimeNoAudioYet', 'Le canal audio n’est pas encore confirmé.') }}
            </p>
          }
        </div>
      </section>

      <app-ambient-speaker-review
        [visitId]="visitId"
        (ambiguityChange)="pendingAmbiguities.set($event)"
      />
      @if (pendingAmbiguities() > 0) {
        <app-doctor-voice-calibration [visitId]="visitId" [compact]="true" />
      }
    </div>
  `,
})
export class AmbientSafetyPanelComponent implements OnDestroy {
  private readonly bridge = inject(RealtimeVoiceBridgeService);
  readonly i18n = inject(I18nService);

  @Input({ required: true }) visitId = '';
  readonly pendingAmbiguities = signal(0);
  readonly state = signal<RealtimeVoiceState>({ ...INITIAL_REALTIME_STATE });
  readonly lastTranscript = signal('');
  readonly lastConfidence = signal<number | null>(null);

  private readonly subscriptions = new Subscription();

  constructor() {
    this.subscriptions.add(
      this.bridge.state$.subscribe(state => this.state.set(state)),
    );
    this.subscriptions.add(
      this.bridge.transcript$.subscribe(turn => this.rememberTranscript(turn)),
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  hearingStatus(): string {
    if (this.state().userSpeaking) {
      return this.i18n.t('consultation.ai.realtimeHearingSpeech', 'Joprelys reçoit votre voix');
    }
    if (this.state().connected) {
      return this.i18n.t('consultation.ai.realtimeHearingReady', 'Canal audio prêt');
    }
    if (this.state().connecting) {
      return this.i18n.t('consultation.ai.realtimeHearingConnecting', 'Connexion du microphone…');
    }
    return this.i18n.t('consultation.ai.realtimeHearingUnavailable', 'Audio non confirmé');
  }

  private rememberTranscript(turn: RealtimeTranscriptTurn): void {
    const text = turn.transcript.trim();
    if (!text) return;
    this.lastTranscript.set(text);
    this.lastConfidence.set(turn.confidence);
  }
}
