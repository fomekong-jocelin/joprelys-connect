import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { throwError } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { RealtimeVoiceBridgeService } from './realtime-voice-bridge.service';

describe('RealtimeVoiceBridgeService connection lifecycle', () => {
  let service: RealtimeVoiceBridgeService;

  beforeEach(() => {
    vi.useFakeTimers();
    vi.stubGlobal('RTCPeerConnection', FakePeerConnection);
    vi.stubGlobal('Audio', FakeAudio);
    vi.stubGlobal('navigator', {
      mediaDevices: {
        getUserMedia: vi.fn().mockResolvedValue(fakeMediaStream()),
      },
    });

    TestBed.configureTestingModule({
      providers: [
        RealtimeVoiceBridgeService,
        {
          provide: HttpClient,
          useValue: {
            post: vi.fn().mockReturnValue(throwError(() => new HttpErrorResponse({
              status: 503,
              error: 'AI_REALTIME_MODEL_OR_CONFIG_UNAVAILABLE',
            }))),
          },
        },
        {
          provide: I18nService,
          useValue: {
            currentLanguage: () => 'fr',
            t: (key: string, fallback?: string) => fallback ?? key,
          },
        },
      ],
    });
    service = TestBed.inject(RealtimeVoiceBridgeService);
  });

  afterEach(() => {
    service.disconnect();
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('should not leave a data-channel timeout after the SDP request fails', async () => {
    await expect(service.connect('visit-1')).rejects.toThrow(
      'Le modèle Realtime ou sa configuration n’est pas disponible',
    );

    expect(vi.getTimerCount()).toBe(0);
  });
});

class FakeDataChannel extends EventTarget {
  readyState: RTCDataChannelState = 'connecting';
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: (() => void) | null = null;
  onclose: (() => void) | null = null;

  close(): void {
    this.readyState = 'closed';
    this.onclose?.();
  }

  send(): void {
    // No-op for the connection-failure scenario.
  }
}

class FakePeerConnection extends EventTarget {
  readonly channel = new FakeDataChannel();
  iceGatheringState: RTCIceGatheringState = 'complete';
  connectionState: RTCPeerConnectionState = 'new';
  localDescription: RTCSessionDescription | null = null;
  ontrack: ((event: RTCTrackEvent) => void) | null = null;
  onconnectionstatechange: (() => void) | null = null;

  addTrack(): void {
    // No-op for the connection-failure scenario.
  }

  createDataChannel(): RTCDataChannel {
    return this.channel as unknown as RTCDataChannel;
  }

  async createOffer(): Promise<RTCSessionDescriptionInit> {
    return { type: 'offer', sdp: 'offer-sdp' };
  }

  async setLocalDescription(description: RTCLocalSessionDescriptionInit): Promise<void> {
    this.localDescription = description as RTCSessionDescription;
  }

  async setRemoteDescription(): Promise<void> {
    // The failing HTTP request prevents this method from being called.
  }

  close(): void {
    this.connectionState = 'closed';
  }
}

class FakeAudio {
  autoplay = false;
  srcObject: MediaProvider | null = null;

  setAttribute(): void {
    // No-op.
  }

  pause(): void {
    // No-op.
  }

  async play(): Promise<void> {
    // No-op.
  }
}

function fakeMediaStream(): MediaStream {
  const track = { stop: vi.fn(), enabled: true } as unknown as MediaStreamTrack;
  return {
    getAudioTracks: () => [track],
    getTracks: () => [track],
  } as unknown as MediaStream;
}
