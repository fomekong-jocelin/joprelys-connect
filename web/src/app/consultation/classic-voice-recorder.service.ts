import { Injectable } from '@angular/core';

export interface ClassicVoiceCapture {
  audio: Blob;
  hasSpeech: boolean;
}

type CaptureHandler = (capture: ClassicVoiceCapture) => void;
type LevelHandler = (level: number) => void;

const MIN_SPEECH_RMS = 0.015;
const MIN_SPEECH_FRAMES = 8;
const MAX_RECORDING_DURATION_MS = 120000;

@Injectable({ providedIn: 'root' })
export class ClassicVoiceRecorderService {
  readonly supported =
    typeof window !== 'undefined'
    && 'MediaRecorder' in window
    && !!navigator.mediaDevices?.getUserMedia;

  private mediaRecorder: MediaRecorder | null = null;
  private mediaStream: MediaStream | null = null;
  private audioChunks: Blob[] = [];
  private recordingTimeout: ReturnType<typeof setTimeout> | null = null;
  private audioContext: AudioContext | null = null;
  private analyser: AnalyserNode | null = null;
  private meterFrame: number | null = null;
  private speechFrames = 0;
  private voiceActivityAvailable = false;
  private captureHandler: CaptureHandler | null = null;
  private levelHandler: LevelHandler | null = null;

  get active(): boolean {
    return this.mediaRecorder?.state === 'recording';
  }

  async start(
    captureHandler: CaptureHandler,
    levelHandler: LevelHandler,
  ): Promise<void> {
    if (!this.supported || this.active) return;
    this.resetCaptureState(captureHandler, levelHandler);
    try {
      this.mediaStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: 1,
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
      });
      const options: MediaRecorderOptions = { audioBitsPerSecond: 128000 };
      const mimeType = this.preferredMimeType();
      if (mimeType) options.mimeType = mimeType;
      this.mediaRecorder = new MediaRecorder(this.mediaStream, options);
      this.mediaRecorder.ondataavailable = event => {
        if (event.data.size > 0) this.audioChunks.push(event.data);
      };
      this.mediaRecorder.onstop = () => this.finishCapture();
      this.mediaRecorder.start(350);
      this.startAudioMeter(this.mediaStream);
      this.recordingTimeout = setTimeout(
        () => this.stop(),
        MAX_RECORDING_DURATION_MS,
      );
    } catch (error) {
      this.cleanup();
      throw error;
    }
  }

  stop(): void {
    if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
      this.mediaRecorder.stop();
    }
  }

  dispose(): void {
    if (this.mediaRecorder) this.mediaRecorder.onstop = null;
    if (this.active) this.mediaRecorder?.stop();
    this.captureHandler = null;
    this.cleanup();
  }

  private resetCaptureState(
    captureHandler: CaptureHandler,
    levelHandler: LevelHandler,
  ): void {
    this.audioChunks = [];
    this.speechFrames = 0;
    this.voiceActivityAvailable = false;
    this.captureHandler = captureHandler;
    this.levelHandler = levelHandler;
  }

  private finishCapture(): void {
    const capture: ClassicVoiceCapture = {
      audio: new Blob(this.audioChunks, {
        type: this.mediaRecorder?.mimeType || 'audio/webm',
      }),
      hasSpeech: !this.voiceActivityAvailable
        || this.speechFrames >= MIN_SPEECH_FRAMES,
    };
    const handler = this.captureHandler;
    this.cleanup();
    handler?.(capture);
  }

  private preferredMimeType(): string | undefined {
    const candidates = [
      'audio/webm;codecs=opus',
      'audio/webm',
      'audio/mp4',
      'audio/wav',
    ];
    return candidates.find(type => MediaRecorder.isTypeSupported(type));
  }

  private startAudioMeter(stream: MediaStream): void {
    try {
      const AudioContextClass = window.AudioContext
        || (window as unknown as { webkitAudioContext?: typeof AudioContext })
          .webkitAudioContext;
      if (!AudioContextClass) return;
      this.voiceActivityAvailable = true;
      this.audioContext = new AudioContextClass();
      this.analyser = this.audioContext.createAnalyser();
      this.analyser.fftSize = 256;
      this.analyser.smoothingTimeConstant = 0.75;
      this.audioContext.createMediaStreamSource(stream).connect(this.analyser);
      const data = new Uint8Array(this.analyser.fftSize);
      const tick = () => {
        if (!this.analyser || !this.active) return;
        this.analyser.getByteTimeDomainData(data);
        const rms = this.rootMeanSquare(data);
        if (rms >= MIN_SPEECH_RMS) this.speechFrames++;
        this.levelHandler?.(Math.min(1, rms * 4.5));
        this.meterFrame = requestAnimationFrame(tick);
      };
      this.meterFrame = requestAnimationFrame(tick);
    } catch {
      this.voiceActivityAvailable = false;
      this.levelHandler?.(0.25);
    }
  }

  private rootMeanSquare(data: Uint8Array): number {
    let sum = 0;
    for (const sample of data) {
      const normalized = (sample - 128) / 128;
      sum += normalized * normalized;
    }
    return Math.sqrt(sum / data.length);
  }

  private cleanup(): void {
    if (this.recordingTimeout) clearTimeout(this.recordingTimeout);
    this.recordingTimeout = null;
    if (this.meterFrame !== null) cancelAnimationFrame(this.meterFrame);
    this.meterFrame = null;
    this.analyser = null;
    void this.audioContext?.close().catch(() => undefined);
    this.audioContext = null;
    this.mediaStream?.getTracks().forEach(track => track.stop());
    this.mediaStream = null;
    this.mediaRecorder = null;
    this.audioChunks = [];
    this.captureHandler = null;
    this.levelHandler?.(0);
    this.levelHandler = null;
  }
}
