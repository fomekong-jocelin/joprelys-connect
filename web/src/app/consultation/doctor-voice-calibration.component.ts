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
              'Calibrez explicitement votre voix pendant 3 secondes. Joprelys pourra reconnaître le médecin ; les autres voix restent non attribuées jusqu’à confirmation humaine.'
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
              {{ i18n.t('consultation.ai.calibrateDoctorVoice', 'Calibrer ma voix · 3 s') }}
            }
          </button>
        </div>
      </div>

      @if (!ambientActive()) {
        <p class="mt-2 rounded-[4px] border border-amber-200 bg-amber-50 px-2.5 py-2 text-[10px] font-bold text-amber-800 dark:border-amber-900 dark:bg-amber-950/20 dark:text-amber-200">
          {{ i18n.t(
            'consultation.ai.doctorVoiceCalibrationRequiresMic',
            'Activez d’abord la capture Realtime sécurisée : la calibration réutilise ce même microphone et n’en ouvre jamais un second.'
          ) }}
        </p>
      }
    </section>
  `,
})
export class DoctorVoiceCalibrationComponent implements OnDestroy {
  private readonly ambientCapture = inject(AmbientAudioCaptureService);
  private readonly calibration = inject(DoctorVoiceCalibrationService);
  readonly i18n = inject(I18nService);

  @Input({ required: true }) visitId = '';

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
        'Calibration impossible : la capture audio sécurisée de cette consultation n’est pas active.',
      ));
      return;
    }
    this.uiError.set('');
    try {
      await this.calibration.calibrate(visitId, stream);
    } catch {
      this.uiError.set(this.i18n.t(
        'consultation.ai.doctorVoiceCalibrationFailed',
        'La calibration n’a pas été validée. Gardez le microphone actif, rapprochez-vous et recommencez.',
      ));
    }
  }

  clearCalibration(): void {
    this.calibration.clear(this.visitId);
    this.uiError.set('');
  }

  statusLabel(): string {
    if (this.isCalibrating()) {
      return this.i18n.t('consultation.ai.doctorVoiceCalibrationInProgress', 'Calibration médecin en cours');
    }
    if (this.isCalibrated()) {
      return this.i18n.t('consultation.ai.doctorVoiceCalibrated', 'Voix médecin calibrée');
    }
    if (this.calibrationState().status === 'error'
      && this.calibrationState().visitId === this.visitId.trim()) {
      return this.i18n.t('consultation.ai.doctorVoiceCalibrationError', 'Calibration médecin à refaire');
    }
    return this.i18n.t('consultation.ai.doctorVoiceNotCalibrated', 'Voix médecin non calibrée');
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
