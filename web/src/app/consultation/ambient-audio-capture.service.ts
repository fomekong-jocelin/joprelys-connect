import { DOCUMENT } from '@angular/common';
import { Injectable, OnDestroy, inject } from '@angular/core';
import { BehaviorSubject, Subscription } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { AmbientAudioUploadService, AmbientUploadState } from './ambient-audio-upload.service';
import { AmbientAudioVaultService, AmbientCaptureTimeline } from './ambient-audio-vault.service';

const TARGET_SAMPLE_RATE = 16_000;
const CHUNK_SECONDS = 10;
const CHUNK_SAMPLES = TARGET_SAMPLE_RATE * CHUNK_SECONDS;
const RECONNECT_DELAYS_MS = [1_000, 2_000, 5_000, 10_000, 30_000] as const;

export interface AmbientCaptureState {
  supported: boolean;
  active: boolean;
  starting: boolean;
  recovering: boolean;
  pendingChunks: number;
  pendingBytes: number;
  uploading: boolean;
  online: boolean;
  storagePressure: boolean;
  lastError: string | null;
}

@Injectable({ providedIn: 'root' })
export class AmbientAudioCaptureService implements OnDestroy {
  private readonly document = inject(DOCUMENT);
  private readonly vault = inject(AmbientAudioVaultService);
  private readonly uploader = inject(AmbientAudioUploadService);
  private readonly i18n = inject(I18nService);
  private readonly stateSubject = new BehaviorSubject<AmbientCaptureState>({
    supported: this.isSupported(),
    active: false,
    starting: false,
    recovering: false,
    pendingChunks: 0,
    pendingBytes: 0,
    uploading: false,
    online: typeof navigator === 'undefined' || navigator.onLine !== false,
    storagePressure: false,
    lastError: null,
  });
  private readonly subscriptions = new Subscription();

  readonly state$ = this.stateSubject.asObservable();

  private desiredVisitId = '';
  private desiredLocale = 'fr';
  private shouldCapture = false;
  private timeline: AmbientCaptureTimeline | null = null;
  private audioContext: AudioContext | null = null;
  private mediaStream: MediaStream | null = null;
  private sourceNode: MediaStreamAudioSourceNode | null = null;
  private workletNode: AudioWorkletNode | null = null;
  private silentGain: GainNode | null = null;
  private currentChunk = new Int16Array(CHUNK_SAMPLES);
  private currentChunkOffset = 0;
  private currentChunkStartOffsetMs = 0;
  private captureStartOffsetMs = 0;
  private capturedTargetSamples = 0;
  private persistenceChain: Promise<void> = Promise.resolve();
  private reconnectAttempt = 0;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private flushResolver: (() => void) | null = null;
  private stopPromise: Promise<void> | null = null;
  private pageHideStopPromise: Promise<void> | null = null;
  private recoveryPromise: Promise<void> | null = null;

  constructor() {
    this.subscriptions.add(this.uploader.state$.subscribe(state => this.mergeUploadState(state)));
    this.document.addEventListener('visibilitychange', this.onVisibilityChange);
    this.document.defaultView?.addEventListener('pagehide', this.onPageHide);
    this.document.defaultView?.addEventListener('pageshow', this.onPageShow);
  }

  isSupported(): boolean {
    const windowRef = this.document?.defaultView;
    return !!windowRef
      && !!navigator.mediaDevices?.getUserMedia
      && typeof windowRef.AudioContext !== 'undefined'
      && typeof AudioWorkletNode !== 'undefined'
      && this.vault.isSupported();
  }

  async start(visitId: string, locale?: string): Promise<void> {
    const normalizedVisitId = visitId.trim();
    if (!normalizedVisitId) throw new Error('AMBIENT_VISIT_REQUIRED');
    this.desiredVisitId = normalizedVisitId;
    this.desiredLocale = this.normalizeLocale(locale || this.i18n.currentLanguage());
    this.shouldCapture = true;
    if (!this.isSupported()) {
      this.patchState({
        supported: false,
        active: false,
        starting: false,
        lastError: 'AMBIENT_CAPTURE_UNSUPPORTED',
      });
      throw new Error('AMBIENT_CAPTURE_UNSUPPORTED');
    }
    if (this.stateSubject.value.active && this.timeline?.visitId === normalizedVisitId) {
      return;
    }
    if (this.pageHideStopPromise) await this.pageHideStopPromise;
    if (this.audioContext || this.mediaStream) {
      await this.stopGraph(true);
    }
    await this.startGraph();
  }

  async stop(): Promise<void> {
    this.shouldCapture = false;
    this.clearReconnect();
    if (this.stopPromise) return this.stopPromise;
    this.stopPromise = this.stopGraph(true)
      .finally(() => {
        this.stopPromise = null;
        this.patchState({ active: false, starting: false, recovering: false });
      });
    return this.stopPromise;
  }

  async flushPending(): Promise<void> {
    await this.persistenceChain;
    await this.uploader.flush(this.desiredVisitId || undefined);
  }

  ngOnDestroy(): void {
    this.shouldCapture = false;
    this.clearReconnect();
    this.document.removeEventListener('visibilitychange', this.onVisibilityChange);
    this.document.defaultView?.removeEventListener('pagehide', this.onPageHide);
    this.document.defaultView?.removeEventListener('pageshow', this.onPageShow);
    this.subscriptions.unsubscribe();
    void this.stopGraph(true);
    this.stateSubject.complete();
  }

  private async startGraph(): Promise<void> {
    this.patchState({ starting: true, recovering: this.reconnectAttempt > 0, lastError: null });
    try {
      this.timeline = await this.vault.getOrCreateTimeline(this.desiredVisitId);
      void this.vault.requestPersistentStorage();
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: 1,
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
        video: false,
      });
      if (!this.shouldCapture) {
        stream.getTracks().forEach(track => track.stop());
        return;
      }

      const windowRef = this.document.defaultView;
      if (!windowRef) throw new Error('AMBIENT_WINDOW_UNAVAILABLE');
      const context = new windowRef.AudioContext({ latencyHint: 'interactive' });
      if (!context.audioWorklet) throw new Error('AMBIENT_AUDIO_WORKLET_UNSUPPORTED');
      await context.audioWorklet.addModule('/joprelys-ambient-capture-processor.js');
      if (context.state === 'suspended') await context.resume();

      const source = context.createMediaStreamSource(stream);
      const worklet = new AudioWorkletNode(context, 'joprelys-ambient-capture', {
        numberOfInputs: 1,
        numberOfOutputs: 1,
        outputChannelCount: [1],
      });
      const silentGain = context.createGain();
      silentGain.gain.value = 0;
      source.connect(worklet);
      worklet.connect(silentGain);
      silentGain.connect(context.destination);

      worklet.port.onmessage = event => this.handleWorkletMessage(event, context.sampleRate);
      const track = stream.getAudioTracks()[0];
      if (!track) throw new Error('AMBIENT_MICROPHONE_TRACK_MISSING');
      track.addEventListener('ended', this.onTrackEnded);

      this.audioContext = context;
      this.mediaStream = stream;
      this.sourceNode = source;
      this.workletNode = worklet;
      this.silentGain = silentGain;
      this.currentChunk = new Int16Array(CHUNK_SAMPLES);
      this.currentChunkOffset = 0;
      this.captureStartOffsetMs = Math.max(0, Date.now() - this.timeline.originEpochMs);
      this.capturedTargetSamples = 0;
      this.currentChunkStartOffsetMs = this.captureStartOffsetMs;
      this.reconnectAttempt = 0;
      this.patchState({ active: true, starting: false, recovering: false, lastError: null });
      await this.uploader.refreshState(this.desiredVisitId);
      void this.uploader.flush(this.desiredVisitId);
    } catch (error) {
      await this.teardownGraph();
      const reason = this.describeError(error);
      this.patchState({ active: false, starting: false, lastError: reason });
      if (this.shouldCapture) this.scheduleReconnect();
      throw error;
    }
  }

  private handleWorkletMessage(event: MessageEvent<unknown>, sourceSampleRate: number): void {
    const payload = event.data as { type?: unknown; samples?: unknown } | null;
    if (payload?.type === 'flushed') {
      this.flushResolver?.();
      this.flushResolver = null;
      return;
    }
    if (payload?.type !== 'frame' || !(payload.samples instanceof Float32Array)) return;
    try {
      const downsampled = downsamplePcm16(payload.samples, sourceSampleRate, TARGET_SAMPLE_RATE);
      this.appendSamples(downsampled);
    } catch (error) {
      void this.recoverFromCaptureLoss(this.describeError(error));
    }
  }

  private appendSamples(samples: Int16Array): void {
    let offset = 0;
    while (offset < samples.length) {
      if (this.currentChunkOffset === 0) {
        this.currentChunkStartOffsetMs = this.captureStartOffsetMs
          + Math.round(this.capturedTargetSamples * 1000 / TARGET_SAMPLE_RATE);
      }
      const writable = Math.min(this.currentChunk.length - this.currentChunkOffset, samples.length - offset);
      this.currentChunk.set(samples.subarray(offset, offset + writable), this.currentChunkOffset);
      this.currentChunkOffset += writable;
      this.capturedTargetSamples += writable;
      offset += writable;
      if (this.currentChunkOffset === this.currentChunk.length) {
        const completed = this.currentChunk;
        const startOffset = this.currentChunkStartOffsetMs;
        this.currentChunk = new Int16Array(CHUNK_SAMPLES);
        this.currentChunkOffset = 0;
        this.queuePersistence(completed, startOffset);
      }
    }
  }

  private queuePersistence(samples: Int16Array, startOffsetMs: number): void {
    const copy = new Int16Array(samples.length);
    copy.set(samples);
    const durationMs = Math.round(copy.length * 1000 / TARGET_SAMPLE_RATE);
    this.persistenceChain = this.persistenceChain
      .then(async () => {
        const wav = encodePcm16Wav(copy, TARGET_SAMPLE_RATE);
        await this.vault.storeEncryptedChunk(
          this.desiredVisitId,
          startOffsetMs,
          durationMs,
          'audio/wav',
          this.desiredLocale,
          wav,
        );
        await this.uploader.refreshState(this.desiredVisitId);
        void this.uploader.flush(this.desiredVisitId);
      })
      .catch(error => {
        this.handlePersistenceFailure(error);
      });
  }

  private async persistPartialChunk(): Promise<void> {
    if (this.currentChunkOffset <= 0) return;
    const partial = this.currentChunk.slice(0, this.currentChunkOffset);
    const startOffset = this.currentChunkStartOffsetMs;
    this.currentChunk = new Int16Array(CHUNK_SAMPLES);
    this.currentChunkOffset = 0;
    this.queuePersistence(partial, startOffset);
    await this.persistenceChain;
  }

  private async flushWorklet(): Promise<void> {
    const worklet = this.workletNode;
    if (!worklet) return;
    await new Promise<void>(resolve => {
      let settled = false;
      const finish = () => {
        if (settled) return;
        settled = true;
        this.flushResolver = null;
        resolve();
      };
      this.flushResolver = finish;
      worklet.port.postMessage({ type: 'flush' });
      setTimeout(finish, 750);
    });
  }

  private async stopGraph(flush: boolean): Promise<void> {
    if (flush) {
      try {
        await this.flushWorklet();
        await this.persistPartialChunk();
        await this.persistenceChain;
      } catch (error) {
        this.patchState({ lastError: this.describeError(error) });
      }
    }
    await this.teardownGraph();
    if (this.desiredVisitId) void this.uploader.flush(this.desiredVisitId);
  }

  private async teardownGraph(): Promise<void> {
    const stream = this.mediaStream;
    stream?.getAudioTracks().forEach(track => track.removeEventListener('ended', this.onTrackEnded));
    this.sourceNode?.disconnect();
    this.workletNode?.disconnect();
    this.silentGain?.disconnect();
    stream?.getTracks().forEach(track => track.stop());
    const context = this.audioContext;
    this.audioContext = null;
    this.mediaStream = null;
    this.sourceNode = null;
    this.workletNode = null;
    this.silentGain = null;
    if (context && context.state !== 'closed') {
      try { await context.close(); } catch { /* best effort */ }
    }
  }

  private handlePersistenceFailure(error: unknown): void {
    const reason = this.describeError(error);
    this.shouldCapture = false;
    this.patchState({ active: false, starting: false, recovering: false, lastError: reason });
    void this.teardownGraph();
  }

  private scheduleReconnect(): void {
    this.clearReconnect();
    const index = Math.min(this.reconnectAttempt, RECONNECT_DELAYS_MS.length - 1);
    const delay = RECONNECT_DELAYS_MS[index];
    this.reconnectAttempt += 1;
    this.patchState({ recovering: true });
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      if (this.shouldCapture) void this.startGraph().catch(() => undefined);
    }, delay);
  }

  private clearReconnect(): void {
    if (!this.reconnectTimer) return;
    clearTimeout(this.reconnectTimer);
    this.reconnectTimer = null;
  }

  private mergeUploadState(upload: AmbientUploadState): void {
    this.patchState({
      pendingChunks: upload.pendingChunks,
      pendingBytes: upload.pendingBytes,
      uploading: upload.uploading,
      online: upload.online,
      storagePressure: upload.storagePressure,
      lastError: this.stateSubject.value.lastError ?? upload.lastError,
    });
  }

  private normalizeLocale(locale: string): string {
    const normalized = (locale || 'fr').trim().toLowerCase().replace('_', '-');
    return /^[a-z]{2,3}(?:-[a-z]{2})?$/.test(normalized) ? normalized : 'fr';
  }

  private describeError(error: unknown): string {
    return error instanceof Error && error.message ? error.message : 'AMBIENT_CAPTURE_FAILED';
  }

  private patchState(patch: Partial<AmbientCaptureState>): void {
    this.stateSubject.next({ ...this.stateSubject.value, ...patch });
  }

  private readonly onTrackEnded = (): void => {
    if (!this.shouldCapture) return;
    void this.recoverFromCaptureLoss('AMBIENT_MICROPHONE_TRACK_ENDED');
  };

  private recoverFromCaptureLoss(reason: string): Promise<void> {
    if (this.recoveryPromise) return this.recoveryPromise;
    this.recoveryPromise = (async () => {
      this.patchState({ active: false, recovering: true, lastError: reason });
      try {
        await this.stopGraph(true);
      } finally {
        if (this.shouldCapture) this.scheduleReconnect();
      }
    })().finally(() => {
      this.recoveryPromise = null;
    });
    return this.recoveryPromise;
  }

  private readonly onVisibilityChange = (): void => {
    if (!this.shouldCapture || !this.audioContext) return;
    if (this.document.visibilityState === 'visible' && this.audioContext.state === 'suspended') {
      void this.audioContext.resume().catch(() => this.recoverFromCaptureLoss('AMBIENT_AUDIO_CONTEXT_SUSPENDED'));
    }
  };

  private readonly onPageHide = (): void => {
    if (!this.shouldCapture) return;
    this.patchState({ active: false, recovering: true });
    this.pageHideStopPromise = this.stopGraph(true)
      .finally(() => {
        this.pageHideStopPromise = null;
      });
  };

  private readonly onPageShow = (): void => {
    if (!this.shouldCapture || !this.desiredVisitId) return;
    void (async () => {
      if (this.pageHideStopPromise) await this.pageHideStopPromise;
      if (!this.shouldCapture || this.audioContext || this.mediaStream) return;
      await this.startGraph();
    })().catch(() => undefined);
  };
}

export function downsamplePcm16(
  input: Float32Array,
  sourceSampleRate: number,
  targetSampleRate = TARGET_SAMPLE_RATE,
): Int16Array {
  if (!input.length) return new Int16Array();
  if (!Number.isFinite(sourceSampleRate) || sourceSampleRate <= 0 || targetSampleRate <= 0) {
    throw new Error('AMBIENT_SAMPLE_RATE_INVALID');
  }
  if (sourceSampleRate < targetSampleRate) {
    throw new Error('AMBIENT_SAMPLE_RATE_TOO_LOW');
  }
  if (sourceSampleRate === targetSampleRate) {
    const direct = new Int16Array(input.length);
    for (let i = 0; i < input.length; i += 1) direct[i] = floatToPcm16(input[i]);
    return direct;
  }

  const ratio = sourceSampleRate / targetSampleRate;
  const outputLength = Math.max(1, Math.floor(input.length / ratio));
  const output = new Int16Array(outputLength);
  for (let out = 0; out < outputLength; out += 1) {
    const start = Math.floor(out * ratio);
    const end = Math.min(input.length, Math.max(start + 1, Math.floor((out + 1) * ratio)));
    let sum = 0;
    for (let i = start; i < end; i += 1) sum += input[i];
    output[out] = floatToPcm16(sum / (end - start));
  }
  return output;
}

export function encodePcm16Wav(samples: Int16Array, sampleRate: number): ArrayBuffer {
  if (!samples.length || !Number.isFinite(sampleRate) || sampleRate <= 0) {
    throw new Error('AMBIENT_WAV_INPUT_INVALID');
  }
  const bytesPerSample = 2;
  const dataSize = samples.length * bytesPerSample;
  const buffer = new ArrayBuffer(44 + dataSize);
  const view = new DataView(buffer);
  writeAscii(view, 0, 'RIFF');
  view.setUint32(4, 36 + dataSize, true);
  writeAscii(view, 8, 'WAVE');
  writeAscii(view, 12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, 1, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * bytesPerSample, true);
  view.setUint16(32, bytesPerSample, true);
  view.setUint16(34, 16, true);
  writeAscii(view, 36, 'data');
  view.setUint32(40, dataSize, true);
  for (let i = 0; i < samples.length; i += 1) {
    view.setInt16(44 + i * 2, samples[i], true);
  }
  return buffer;
}

function floatToPcm16(value: number): number {
  const clamped = Math.max(-1, Math.min(1, Number.isFinite(value) ? value : 0));
  return clamped < 0 ? Math.round(clamped * 0x8000) : Math.round(clamped * 0x7fff);
}

function writeAscii(view: DataView, offset: number, text: string): void {
  for (let i = 0; i < text.length; i += 1) view.setUint8(offset + i, text.charCodeAt(i));
}
