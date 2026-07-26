import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { AmbientAudioVaultService } from './ambient-audio-vault.service';
import { DoctorVoiceCalibrationService } from './doctor-voice-calibration.service';

describe('DoctorVoiceCalibrationService', () => {
  let service: DoctorVoiceCalibrationService;
  let vault: {
    activateDoctorReference: ReturnType<typeof vi.fn>;
    clearDoctorReference: ReturnType<typeof vi.fn>;
  };
  let track: FakeTrack;
  let stream: MediaStream;
  let getUserMedia: ReturnType<typeof vi.fn>;
  let originalAudioWorkletNode: unknown;
  let originalMediaDevices: PropertyDescriptor | undefined;

  beforeEach(() => {
    FakeAudioWorkletNode.lastInstance = null;
    vault = {
      activateDoctorReference: vi.fn(),
      clearDoctorReference: vi.fn(),
    };
    track = new FakeTrack();
    stream = { getAudioTracks: () => [track] } as unknown as MediaStream;
    getUserMedia = vi.fn();

    originalAudioWorkletNode = (globalThis as any).AudioWorkletNode;
    Object.defineProperty(globalThis, 'AudioWorkletNode', {
      configurable: true,
      value: FakeAudioWorkletNode,
    });
    originalMediaDevices = Object.getOwnPropertyDescriptor(navigator, 'mediaDevices');
    Object.defineProperty(navigator, 'mediaDevices', {
      configurable: true,
      value: { getUserMedia },
    });

    TestBed.configureTestingModule({
      providers: [
        DoctorVoiceCalibrationService,
        { provide: AmbientAudioVaultService, useValue: vault },
        {
          provide: DOCUMENT,
          useValue: {
            defaultView: { AudioContext: FakeAudioContext },
          },
        },
      ],
    });
    service = TestBed.inject(DoctorVoiceCalibrationService);
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    Object.defineProperty(globalThis, 'AudioWorkletNode', {
      configurable: true,
      value: originalAudioWorkletNode,
    });
    if (originalMediaDevices) {
      Object.defineProperty(navigator, 'mediaDevices', originalMediaDevices);
    } else {
      delete (navigator as any).mediaDevices;
    }
  });

  it('should capture three seconds from the supplied stream and never call getUserMedia', async () => {
    const calibration = service.calibrate('visit-1', stream);
    await settle();
    const worklet = FakeAudioWorkletNode.lastInstance;
    expect(worklet).not.toBeNull();

    for (let index = 0; index < 6; index += 1) {
      worklet!.emitFrame(new Float32Array(24_000).fill(0.25));
    }

    const wav = await calibration;

    expect(getUserMedia).not.toHaveBeenCalled();
    expect(vault.activateDoctorReference).toHaveBeenCalledTimes(1);
    expect(vault.activateDoctorReference).toHaveBeenCalledWith(
      'visit-1',
      wav,
      expect.any(Number),
    );
    const view = new DataView(wav);
    expect(ascii(view, 0, 4)).toBe('RIFF');
    expect(ascii(view, 8, 4)).toBe('WAVE');
    expect(view.getUint32(24, true)).toBe(16_000);
    expect(view.getUint16(22, true)).toBe(1);
    expect(view.getUint32(40, true)).toBe(96_000);
    expect(service.state().status).toBe('calibrated');
    expect(service.state().progress).toBe(1);
    expect(track.stopped).toBe(false);
  });

  it('should fail closed when the shared microphone stream ends during calibration', async () => {
    const calibration = service.calibrate('visit-1', stream);
    await settle();

    track.end();

    await expect(calibration).rejects.toThrow('AMBIENT_DOCTOR_CALIBRATION_STREAM_ENDED');
    expect(vault.activateDoctorReference).not.toHaveBeenCalled();
    expect(service.state().status).toBe('error');
    expect(getUserMedia).not.toHaveBeenCalled();
  });

  it('should clear only the requested visit calibration', () => {
    service.clear('visit-1');

    expect(vault.clearDoctorReference).toHaveBeenCalledWith('visit-1');
    expect(service.state().status).toBe('idle');
  });
});

class FakeTrack {
  readyState: MediaStreamTrackState = 'live';
  stopped = false;
  private readonly listeners = new Set<() => void>();

  addEventListener(type: string, listener: EventListenerOrEventListenerObject): void {
    if (type !== 'ended') return;
    const callback = typeof listener === 'function'
      ? () => listener(new Event('ended'))
      : () => listener.handleEvent(new Event('ended'));
    (listener as any).__fakeCallback = callback;
    this.listeners.add(callback);
  }

  removeEventListener(_type: string, listener: EventListenerOrEventListenerObject): void {
    const callback = (listener as any).__fakeCallback as (() => void) | undefined;
    if (callback) this.listeners.delete(callback);
  }

  stop(): void {
    this.stopped = true;
    this.readyState = 'ended';
  }

  end(): void {
    this.readyState = 'ended';
    [...this.listeners].forEach(listener => listener());
  }
}

class FakeAudioContext {
  readonly sampleRate = 48_000;
  readonly state: AudioContextState = 'running';
  readonly destination = {} as AudioDestinationNode;
  readonly audioWorklet = { addModule: vi.fn().mockResolvedValue(undefined) };

  createMediaStreamSource(_stream: MediaStream): MediaStreamAudioSourceNode {
    return node() as unknown as MediaStreamAudioSourceNode;
  }

  createGain(): GainNode {
    return {
      ...node(),
      gain: { value: 1 },
    } as unknown as GainNode;
  }

  async resume(): Promise<void> {}
  async close(): Promise<void> {}
}

class FakeAudioWorkletNode {
  static lastInstance: FakeAudioWorkletNode | null = null;
  readonly port: { onmessage: ((event: MessageEvent<unknown>) => void) | null } = { onmessage: null };

  constructor(..._args: unknown[]) {
    FakeAudioWorkletNode.lastInstance = this;
  }

  connect(_destination: unknown): void {}
  disconnect(): void {}

  emitFrame(samples: Float32Array): void {
    this.port.onmessage?.({
      data: { type: 'frame', samples },
    } as MessageEvent<unknown>);
  }
}

function node(): { connect: ReturnType<typeof vi.fn>; disconnect: ReturnType<typeof vi.fn> } {
  return { connect: vi.fn(), disconnect: vi.fn() };
}

function ascii(view: DataView, offset: number, length: number): string {
  return Array.from({ length }, (_, index) => String.fromCharCode(view.getUint8(offset + index))).join('');
}

async function settle(): Promise<void> {
  await Promise.resolve();
  await Promise.resolve();
  await Promise.resolve();
}
