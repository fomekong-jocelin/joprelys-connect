import { CommonModule } from '@angular/common';
import { Component, Input, OnDestroy, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { AmbientAudioCaptureService } from './ambient-audio-capture.service';
import {
  DoctorVoiceCalibrationService,
  DoctorVoiceCalibrationState,
} from './doctor-voice-calibration.service';

@Component({
  selector: 'app-doctor-voice-calibration',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (compact) {
      @if (!isCalibrated() && ambientActive()) {
        <section class="rounded-[5px] border border-cyan-200 bg-cyan-50/50 p-3 dark:border-cyan-900 dark:bg-cyan-950/15">
          <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p class="text-xs font-black text-[var(--text-primary)]">
                {{ i18n.t('consultation.ai.helpRecognizeDoctorVoiceTitle', 'Aider Joprelys à reconnaître ma voix') }}
              </p>
              <p class="mt-1 text-[11px] leading-4 text-[var(--text-muted)]">
                {{ i18n.t('consultation.ai.helpRecognizeDoctorVoiceHelp', 'Optionnel : parlez 3 secondes. Cette aide sert uniquement à mieux distinguer le médecin des autres voix.') }}
              </p>
              @if (isCalibrating()) {
                <div class="mt-2 h-1.5 overflow-hidden rounded-[3px] bg-slate-200 dark:bg-slate-800" aria-hidden="true">
                  <div class="h-full bg-cyan-600 transition-[width] duration-150" [style.width.%]="progressPercent()"></div>
                </div>
              }
              @if (uiError()) {
                <p class="mt-2 text-[11px] font-bold text-rose-700 dark:text-rose-300">{{ uiError() }}</p>
              }
            </div>
            <button
              type="button"
              (click)="calibrate()"
              [disabled]="!canCalibrate()"
              class="min-h-10 shrink-0 rounded-[5px] border border-cyan-300 bg-white px-3 text-xs font-black text-cyan-800 hover:bg-cyan-50 disabled:opacity-50 dark:border-cyan-800 dark:bg-slate-950 dark:text-cyan-200"
            >
              {{ isCalibrating()
                ? i18n.t('consultation.ai.doctorVoiceCalibrating', 'Parlez naturellement…')
                : i18n.t('consultation.ai.helpRecognizeDoctorVoiceAction', 'Parler 3 s') }}
            </button>
          </div>
        </section>
      }
    } @else {
      <section class="rounded-[6px] border border-[var(--app-border)] bg-[var(--app-surface)] p-3 shadow-sm">
        <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div class="min-w-0">
            <div class="flex items-center gap-2">
              <span class="h-2.5 w-2.5 rounded-full" [ngClass]="statusDotClasses()"></span>
              <p class="text-xs font-black text-[var(--text-primary)]">{{ statusLabel() }}</p>
            </div>
            <p class="mt-1 text-[11px] leading-4 text-[var(--text-muted)]">
              {{ i18n.t(
                'consultation.ai.doctorVoiceCalibrationHelp',
                'Cette aide de 3 secondes permet uniquement à Joprelys de mieux reconnaître la voix du médecin.'
              ) }}
            </p>
            @if (isCalibrating()) {
              <div class="mt-2 h-1.5 overflow-hidden rounded-[3px] bg-slate-200 dark:bg-slate-800" aria-hidden="true">
                <div class="h-full bg-cyan-600 transition-[width] duration-150" [style.width.%]="progressPercent()"></div>
              </div>
            }
            @if (uiError()) {
              <p class="mt-2 text-[11px] font-bold text-rose-700 dark:text-rose-300">{{ uiError() }}</p>
            }
          </div>

          <div class="flex shrink-0 flex-col gap-2 sm:flex-row">
            @if (isCalibrated()) {
              <button
                type="button"
                (click)="clearCalibration()"
                [disabled]="isCalibrating()"
                class="min-h-10 rounded-[5px] border border-slate-300 bg-white px-3 text-xs font-bold text-slate-700 shadow-sm hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-200"
              >
                {{ i18n.t('consultation.ai.clearDoctorCalibration', 'Effacer') }}
              </button>
            }
            <button
              type="button"
              (click)="calibrate()"
              [disabled]="!canCalibrate()"
              class="min-h-10 rounded-[5px] border border-cyan-300 bg-cyan-50 px-4 text-xs font-black text-cyan-800 shadow-sm hover:bg-cyan-100 disabled:cursor-not-allowed disabled:opacity-50 dark:border-cyan-800 dark:bg-cyan-950/30 dark:text-cyan-200"
            >
              @if (isCalibrating()) {
                {{ i18n.t('consultation.ai.doctorVoiceCalibrating', 'Parlez naturellement…') }}
              } @else if (isCalibrated()) {
                {{ i18n.t('consultation.ai.recalibrateDoctorVoice', 'Recalibrer 3 s') }}
              } @else {
                {{ i18n.t('consultation.ai.helpRecognizeDoctorVoiceActionLong', 'Aider Joprelys à reconnaître ma voix · 3 s') }}
              }
            </button>
          </div>
        </div>
      </section>
    }
  `,
})
export class DoctorVoiceCalibrationComponent implements OnDestroy {
  private readonly ambientCapture = inject(AmbientAudioCaptureService);
  private readonly calibration = inject(DoctorVoiceCalibrationService);
  readonly i18n = inject(I18nService);

  @Input({ required: true }) visitId = '';
  @Input() compact = false;

  readonly ambientActive = signal(false);
  readonly calibrationState = signal<DoctorVoiceCalibrationState>(this.calibration.state());
  readonly uiError = signal('');
  private readonly subscriptions = new Subscription();

  constructor() {
    this.subscriptions.add(
      this.ambientCapture.state$.subscribe(state => this.ambientActive.set(state.active)),
    );
    this.subscriptions.add(
      this.calibration.state$.subscribe(state => {
        this.calibrationState.set(state);
        if (state.status !== 'error') this.uiError.set('');
      }),
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  canCalibrate(): boolean {
    if (!this.visitId.trim() || this.isCalibrating() || !this.ambientActive()) return false;
    return this.ambientCapture.mediaStreamForVisit(this.visitId) !== null;
  }

  isCalibrating(): boolean {
    const state = this.calibrationState();
    return state.status === 'calibrating' && state.visitId === this.visitId.trim();
  }

  isCalibrated(): boolean {
    const state = this.calibrationState();
    return state.status === 'calibrated' && state.visitId === this.visitId.trim();
  }

  progressPercent(): number {
    return Math.round(this.calibrationState().progress * 100);
  }

  async calibrate(): Promise<void> {
    const visitId = this.visitId.trim();
    const stream = this.ambientCapture.mediaStreamForVisit(visitId);
    if (!visitId || !stream || !this.ambientActive()) {
      this.uiError.set(this.i18n.t(
        'consultation.ai.doctorVoiceCalibrationUnavailable',
        'L’aide vocale n’est disponible que pendant une écoute Realtime active.',
      ));
      return;
    }
    this.uiError.set('');
    try {
      await this.calibration.calibrate(visitId, stream);
    } catch {
      this.uiError.set(this.i18n.t(
        'consultation.ai.doctorVoiceCalibrationFailed',
        'La voix n’a pas pu être reconnue. Rapprochez-vous du microphone puis réessayez.',
      ));
    }
  }

  clearCalibration(): void {
    this.calibration.clear(this.visitId);
    this.uiError.set('');
  }

  statusLabel(): string {
    if (this.isCalibrating()) {
      return this.i18n.t('consultation.ai.doctorVoiceCalibrationInProgress', 'Reconnaissance de votre voix en cours');
    }
    if (this.isCalibrated()) {
      return this.i18n.t('consultation.ai.doctorVoiceCalibrated', 'Reconnaissance de votre voix active');
    }
    if (this.calibrationState().status === 'error'
      && this.calibrationState().visitId === this.visitId.trim()) {
      return this.i18n.t('consultation.ai.doctorVoiceCalibrationError', 'Reconnaissance de votre voix à refaire');
    }
    return this.i18n.t('consultation.ai.doctorVoiceNotCalibrated', 'Reconnaissance de votre voix non activée');
  }

  statusDotClasses(): string {
    if (this.isCalibrating()) return 'animate-pulse bg-amber-500';
    if (this.isCalibrated()) return 'bg-emerald-500';
    if (this.calibrationState().status === 'error'
      && this.calibrationState().visitId === this.visitId.trim()) {
      return 'bg-rose-500';
    }
    return 'bg-slate-400';
  }
}
