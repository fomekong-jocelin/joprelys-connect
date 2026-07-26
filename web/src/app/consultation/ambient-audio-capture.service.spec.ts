import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { I18nService } from '../core/i18n/i18n.service';
import {
  AmbientAudioCaptureService,
  downsamplePcm16,
  encodePcm16Wav,
} from './ambient-audio-capture.service';
import { AmbientAudioUploadService, AmbientUploadState } from './ambient-audio-upload.service';
import { AmbientAudioVaultService } from './ambient-audio-vault.service';

describe('ambient audio safety encoding', () => {
  it('should downsample 48 kHz mono PCM to 16 kHz without changing duration', () => {
    const input = new Float32Array(48_000);
    input.fill(0.5);

    const output = downsamplePcm16(input, 48_000, 16_000);

    expect(output.length).toBe(16_000);
    expect(output[0]).toBeGreaterThan(16_000);
    expect(output[0]).toBeLessThan(17_000);
    expect(output.at(-1)).toBe(output[0]);
  });

  it('should clamp out-of-range samples before PCM16 conversion', () => {
    const output = downsamplePcm16(new Float32Array([-2, 2]), 16_000, 16_000);

    expect(output[0]).toBe(-32_768);
    expect(output[1]).toBe(32_767);
  });

  it('should create a standalone mono PCM16 WAV with correct duration metadata', () => {
    const samples = new Int16Array(16_000);
    const buffer = encodePcm16Wav(samples, 16_000);
    const view = new DataView(buffer);

    expect(ascii(view, 0, 4)).toBe('RIFF');
    expect(ascii(view, 8, 4)).toBe('WAVE');
    expect(ascii(view, 12, 4)).toBe('fmt ');
    expect(view.getUint16(20, true)).toBe(1);
    expect(view.getUint16(22, true)).toBe(1);
    expect(view.getUint32(24, true)).toBe(16_000);
    expect(view.getUint16(34, true)).toBe(16);
    expect(ascii(view, 36, 4)).toBe('data');
    expect(view.getUint32(40, true)).toBe(32_000);
    expect(buffer.byteLength).toBe(32_044);
  });
});

describe('AmbientAudioCaptureService visit isolation', () => {
  let service: AmbientAudioCaptureService;
  let vault: {
    isSupported: ReturnType<typeof vi.fn>;
    storeEncryptedChunk: ReturnType<typeof vi.fn>;
  };
  let uploader: {
    state$: BehaviorSubject<AmbientUploadState>;
    refreshState: ReturnType<typeof vi.fn>;
    flush: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    vault = {
      isSupported: vi.fn().mockReturnValue(true),
      storeEncryptedChunk: vi.fn().mockResolvedValue(undefined),
    };
    uploader = {
      state$: new BehaviorSubject<AmbientUploadState>({
        uploading: false,
        online: true,
        pendingChunks: 0,
        pendingBytes: 0,
        storagePressure: false,
        lastError: null,
      }),
      refreshState: vi.fn().mockResolvedValue(undefined),
      flush: vi.fn().mockResolvedValue(undefined),
    };
    const fakeWindow = {
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    };
    const fakeDocument = {
      defaultView: fakeWindow,
      visibilityState: 'visible',
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        AmbientAudioCaptureService,
        { provide: AmbientAudioVaultService, useValue: vault },
        { provide: AmbientAudioUploadService, useValue: uploader },
        { provide: DOCUMENT, useValue: fakeDocument },
        {
          provide: I18nService,
          useValue: {
            currentLanguage: () => 'fr',
          },
        },
      ],
    });
    service = TestBed.inject(AmbientAudioCaptureService);
  });

  afterEach(() => {
    service.ngOnDestroy();
    TestBed.resetTestingModule();
  });

  it('persists a queued chunk with the visit snapshot even if desiredVisitId changes before the promise executes', async () => {
    const capture = Object.freeze({
      visitId: 'visit-A',
      locale: 'fr',
      generationId: 7,
      timeline: {
        visitId: 'visit-A',
        sessionId: 'session-A',
        originEpochMs: 1_000,
        nextSequence: 0,
        updatedAt: 1_000,
      },
    });

    (service as any).queuePersistence(new Int16Array([10, 20, 30]), 250, capture);
    (service as any).desiredVisitId = 'visit-B';
    (service as any).desiredLocale = 'en';

    await (service as any).persistenceChain;

    expect(vault.storeEncryptedChunk).toHaveBeenCalledTimes(1);
    expect(vault.storeEncryptedChunk).toHaveBeenCalledWith(
      'visit-A',
      250,
      expect.any(Number),
      'audio/wav',
      'fr',
      expect.any(ArrayBuffer),
    );
    expect(uploader.refreshState).toHaveBeenCalledWith('visit-A');
    expect(uploader.flush).toHaveBeenCalledWith('visit-A');
  });

  it('ignores late worklet frames from a superseded capture generation', () => {
    const appendSamples = vi.spyOn(service as any, 'appendSamples');
    (service as any).activeCapture = {
      visitId: 'visit-B',
      locale: 'fr',
      generationId: 9,
      timeline: {
        visitId: 'visit-B',
        sessionId: 'session-B',
        originEpochMs: 1_000,
        nextSequence: 0,
        updatedAt: 1_000,
      },
    };

    (service as any).handleWorkletMessage(
      { data: { type: 'frame', samples: new Float32Array([0.1, 0.2]) } },
      48_000,
      8,
    );

    expect(appendSamples).not.toHaveBeenCalled();
  });
});

function ascii(view: DataView, offset: number, length: number): string {
  return Array.from({ length }, (_, index) => String.fromCharCode(view.getUint8(offset + index))).join('');
}
