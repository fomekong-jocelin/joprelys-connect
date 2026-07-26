import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { throwError } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { RealtimeVoiceBridgeService, RealtimeVoiceState } from './realtime-voice-bridge.service';

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

  it('should wait for the WebRTC output buffer to drain after response.done', () => {
    const states: RealtimeVoiceState[] = [];
    let completedTurns = 0;
    const stateSubscription = service.state$.subscribe(current => states.push(current));
    const turnSubscription = service.assistantTurnCompleted$.subscribe(() => completedTurns++);

    serverEvent({ type: 'response.created', response: { id: 'response-1', status: 'in_progress' } });
    serverEvent({ type: 'output_audio_buffer.started', response_id: 'response-1' });
    serverEvent({ type: 'response.done', response: { id: 'response-1', status: 'completed' } });

    expect(states.at(-1)?.assistantSpeaking).toBe(true);
    expect(completedTurns).toBe(0);

    serverEvent({ type: 'output_audio_buffer.stopped', response_id: 'response-1' });

    expect(states.at(-1)?.assistantSpeaking).toBe(false);
    expect(completedTurns).toBe(1);
    stateSubscription.unsubscribe();
    turnSubscription.unsubscribe();
  });

  it('should clear buffered assistant audio when the clinician interrupts after response.done', () => {
    const channel = new FakeDataChannel();
    channel.readyState = 'open';
    (service as any).dataChannel = channel;
    let completedTurns = 0;
    service.assistantTurnCompleted$.subscribe(() => completedTurns++);

    serverEvent({ type: 'response.created', response: { id: 'response-1', status: 'in_progress' } });
    serverEvent({ type: 'output_audio_buffer.started', response_id: 'response-1' });
    serverEvent({ type: 'response.done', response: { id: 'response-1', status: 'completed' } });
    serverEvent({ type: 'input_audio_buffer.speech_started' });

    const sentEvents = channel.sent.map(payload => JSON.parse(payload) as { type: string });
    expect(sentEvents.some(event => event.type === 'output_audio_buffer.clear')).toBe(true);
    expect(completedTurns).toBe(1);
    expect((service as any).stateSubject.value.userSpeaking).toBe(true);
    expect((service as any).stateSubject.value.assistantSpeaking).toBe(false);
  });

  it('should cancel an in-progress response and clear audio on barge-in', () => {
    const channel = new FakeDataChannel();
    channel.readyState = 'open';
    (service as any).dataChannel = channel;

    serverEvent({ type: 'response.created', response: { id: 'response-2', status: 'in_progress' } });
    serverEvent({ type: 'output_audio_buffer.started', response_id: 'response-2' });
    serverEvent({ type: 'input_audio_buffer.speech_started' });

    const sentEvents = channel.sent.map(payload => JSON.parse(payload) as { type: string });
    expect(sentEvents.map(event => event.type)).toEqual([
      'response.cancel',
      'output_audio_buffer.clear',
    ]);
  });

  function serverEvent(event: Record<string, unknown>): void {
    (service as any).handleServerEvent(JSON.stringify(event));
  }
});

class FakeDataChannel extends EventTarget {
  readyState: RTCDataChannelState = 'connecting';
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: (() => void) | null = null;
  onclose: (() => void) | null = null;
  readonly sent: string[] = [];

  close(): void {
    this.readyState = 'closed';
    this.onclose?.();
  }

  send(data: string): void {
    this.sent.push(data);
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
