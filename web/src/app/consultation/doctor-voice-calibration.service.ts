import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { AmbientAudioVaultService } from './ambient-audio-vault.service';
import { downsamplePcm16, encodePcm16Wav } from './ambient-audio-capture.service';

const TARGET_SAMPLE_RATE = 16_000;
const CALIBRATION_SECONDS = 3;
const TARGET_SAMPLES = TARGET_SAMPLE_RATE * CALIBRATION_SECONDS;
const CALIBRATION_TIMEOUT_MS = 7_000;

export type DoctorVoiceCalibrationStatus = 'idle' | 'calibrating' | 'calibrated' | 'error';

export interface DoctorVoiceCalibrationState {
  status: DoctorVoiceCalibrationStatus;
  visitId: string | null;
  progress: number;
  error: string | null;
  calibratedAt: number | null;
}

@Injectable({ providedIn: 'root' })
export class DoctorVoiceCalibrationService {
  private readonly document = inject(DOCUMENT);
  private readonly vault = inject(AmbientAudioVaultService);
  private readonly stateSubject = new BehaviorSubject<DoctorVoiceCalibrationState>({
    status: 'idle',
    visitId: null,
    progress: 0,
    error: null,
    calibratedAt: null,
  });

  readonly state$ = this.stateSubject.asObservable();
  private activeCalibration: Promise<ArrayBuffer> | null = null;

  state(): DoctorVoiceCalibrationState {
    return this.stateSubject.value;
  }

  async calibrate(visitId: string, stream: MediaStream): Promise<ArrayBuffer> {
    const normalizedVisitId = visitId.trim();
    if (!normalizedVisitId) throw new Error('AMBIENT_VISIT_REQUIRED');
    if (this.activeCalibration) return this.activeCalibration;

    const track = stream?.getAudioTracks()[0];
    if (!track || track.readyState === 'ended') {
      throw new Error('AMBIENT_DOCTOR_CALIBRATION_STREAM_UNAVAILABLE');
    }

    this.patchState({
      status: 'calibrating',
      visitId: normalizedVisitId,
      progress: 0,
      error: null,
      calibratedAt: null,
    });

    this.activeCalibration = this.captureReference(normalizedVisitId, stream, track)
      .finally(() => {
        this.activeCalibration = null;
      });
    return this.activeCalibration;
  }

  clear(visitId?: string): void {
    const normalizedVisitId = visitId?.trim() || undefined;
    this.vault.clearDoctorReference(normalizedVisitId);
    if (!normalizedVisitId || this.stateSubject.value.visitId === normalizedVisitId) {
      this.stateSubject.next({
        status: 'idle',
        visitId: null,
        progress: 0,
        error: null,
        calibratedAt: null,
      });
    }
  }

  private captureReference(
    visitId: string,
    stream: MediaStream,
    track: MediaStreamTrack,
  ): Promise<ArrayBuffer> {
    const windowRef = this.document.defaultView;
    if (!windowRef || typeof windowRef.AudioContext === 'undefined' || typeof AudioWorkletNode === 'undefined') {
      return Promise.reject(new Error('AMBIENT_DOCTOR_CALIBRATION_UNSUPPORTED'));
    }

    return new Promise<ArrayBuffer>(async (resolve, reject) => {
      let context: AudioContext | null = null;
      let source: MediaStreamAudioSourceNode | null = null;
      let worklet: AudioWorkletNode | null = null;
      let silentGain: GainNode | null = null;
      let timeout: ReturnType<typeof setTimeout> | null = null;
      let completed = false;
      const samples = new Int16Array(TARGET_SAMPLES);
      let sampleOffset = 0;

      const cleanup = async (): Promise<void> => {
        if (timeout) clearTimeout(timeout);
        track.removeEventListener('ended', onTrackEnded);
        source?.disconnect();
        worklet?.disconnect();
        silentGain?.disconnect();
        if (context && context.state !== 'closed') {
          try { await context.close(); } catch { /* best effort */ }
        }
      };

      const fail = (error: unknown): void => {
        if (completed) return;
        completed = true;
        const message = error instanceof Error && error.message
          ? error.message
          : 'AMBIENT_DOCTOR_CALIBRATION_FAILED';
        this.patchState({ status: 'error', progress: 0, error: message, calibratedAt: null });
        void cleanup().finally(() => reject(error instanceof Error ? error : new Error(message)));
      };

      const succeed = (): void => {
        if (completed) return;
        completed = true;
        const wav = encodePcm16Wav(samples, TARGET_SAMPLE_RATE);
        const calibratedAt = Date.now();
        this.vault.activateDoctorReference(visitId, wav, calibratedAt);
        this.patchState({
          status: 'calibrated',
          visitId,
          progress: 1,
          error: null,
          calibratedAt,
        });
        void cleanup().finally(() => resolve(wav));
      };

      const onTrackEnded = (): void => fail(new Error('AMBIENT_DOCTOR_CALIBRATION_STREAM_ENDED'));

      try {
        context = new windowRef.AudioContext({ latencyHint: 'interactive' });
        if (!context.audioWorklet) throw new Error('AMBIENT_DOCTOR_CALIBRATION_UNSUPPORTED');
        await context.audioWorklet.addModule('/joprelys-ambient-capture-processor.js');
        if (context.state === 'suspended') await context.resume();
        if (track.readyState === 'ended') throw new Error('AMBIENT_DOCTOR_CALIBRATION_STREAM_ENDED');

        source = context.createMediaStreamSource(stream);
        worklet = new AudioWorkletNode(context, 'joprelys-ambient-capture', {
          numberOfInputs: 1,
          numberOfOutputs: 1,
          outputChannelCount: [1],
        });
        silentGain = context.createGain();
        silentGain.gain.value = 0;
        source.connect(worklet);
        worklet.connect(silentGain);
        silentGain.connect(context.destination);
        track.addEventListener('ended', onTrackEnded);

        worklet.port.onmessage = event => {
          if (completed) return;
          const payload = event.data as { type?: unknown; samples?: unknown } | null;
          if (payload?.type !== 'frame' || !(payload.samples instanceof Float32Array)) return;
          try {
            const converted = downsamplePcm16(payload.samples, context?.sampleRate ?? 0, TARGET_SAMPLE_RATE);
            const writable = Math.min(converted.length, TARGET_SAMPLES - sampleOffset);
            samples.set(converted.subarray(0, writable), sampleOffset);
            sampleOffset += writable;
            this.patchState({ progress: Math.min(1, sampleOffset / TARGET_SAMPLES) });
            if (sampleOffset >= TARGET_SAMPLES) succeed();
          } catch (error) {
            fail(error);
          }
        };

        timeout = setTimeout(
          () => fail(new Error('AMBIENT_DOCTOR_CALIBRATION_TIMEOUT')),
          CALIBRATION_TIMEOUT_MS,
        );
      } catch (error) {
        fail(error);
      }
    });
  }

  private patchState(patch: Partial<DoctorVoiceCalibrationState>): void {
    this.stateSubject.next({ ...this.stateSubject.value, ...patch });
  }
}
