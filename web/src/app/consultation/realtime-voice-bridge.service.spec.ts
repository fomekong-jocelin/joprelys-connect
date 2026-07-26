import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { throwError } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import {
  RealtimeTranscriptTurn,
  RealtimeVoiceBridgeService,
  RealtimeVoiceState,
} from './realtime-voice-bridge.service';

describe('RealtimeVoiceBridgeService connection lifecycle', () => {
  let service: RealtimeVoiceBridgeService;
  let getUserMedia: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    vi.useFakeTimers();
    vi.stubGlobal('RTCPeerConnection', FakePeerConnection);
    vi.stubGlobal('Audio', FakeAudio);
    getUserMedia = vi.fn().mockResolvedValue(fakeMediaStream());
    vi.stubGlobal('navigator', {
      mediaDevices: {
        getUserMedia,
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

  it('should schedule recovery after the SDP request fails without leaking a data-channel timeout', async () => {
    await expect(service.connect('visit-1')).rejects.toThrow(
      'Le modèle Realtime ou sa configuration n’est pas disponible',
    );

    expect((service as any).reconnectTimer).not.toBeNull();
    expect((service as any).disconnectedTimer).toBeNull();
    expect((service as any).stateSubject.value.connecting).toBe(true);
  });

  it('should automatically retry after the realtime channel closes', async () => {
    (service as any).shouldStayConnected = true;
    (service as any).desiredVisitId = 'visit-1';
    (service as any).desiredPurpose = 'consultation';
    (service as any).dataChannel = new FakeDataChannel();

    (service as any).handleConnectionClosed();

    expect((service as any).stateSubject.value.connected).toBe(false);
    expect((service as any).stateSubject.value.connecting).toBe(true);

    await vi.advanceTimersByTimeAsync(1000);

    expect(getUserMedia).toHaveBeenCalledTimes(1);
    expect((service as any).reconnectTimer).not.toBeNull();
  });

  it('should recover when the current microphone track ends', () => {
    const track = new FakeMediaStreamTrack();
    const stream = fakeMediaStream(track);
    (service as any).shouldStayConnected = true;
    (service as any).desiredVisitId = 'visit-1';
    (service as any).desiredPurpose = 'consultation';
    (service as any).mediaStream = stream;
    (service as any).watchMicrophoneTrack(track as unknown as MediaStreamTrack);

    track.dispatchEvent(new Event('ended'));

    expect((service as any).stateSubject.value.connecting).toBe(true);
    expect((service as any).reconnectTimer).not.toBeNull();
  });

  it('should use a supplied ambient stream without opening or stopping another microphone', async () => {
    const sharedTrack = new FakeMediaStreamTrack();
    const sharedStream = fakeMediaStream(sharedTrack);

    await expect(service.connect(
      'visit-1',
      'consultation',
      () => sharedStream,
    )).rejects.toThrow('Le modèle Realtime ou sa configuration n’est pas disponible');

    expect(getUserMedia).not.toHaveBeenCalled();
    expect(sharedTrack.stopCalls).toBe(0);
    service.disconnect();
    expect(sharedTrack.stopCalls).toBe(0);
  });

  it('should pause only the WebRTC sender while leaving the shared track enabled', async () => {
    const sharedTrack = new FakeMediaStreamTrack();
    const sharedStream = fakeMediaStream(sharedTrack);
    const sender = new FakeRtpSender(sharedTrack as unknown as MediaStreamTrack);
    (service as any).mediaStream = sharedStream;
    (service as any).mediaStreamOwned = false;
    (service as any).audioSender = sender;
    (service as any).shouldStayConnected = true;

    service.setMuted(true);
    await Promise.resolve();

    expect(sender.replacements).toEqual([null]);
    expect(sharedTrack.enabled).toBe(true);
    expect(sharedTrack.stopCalls).toBe(0);

    service.setMuted(false);
    await Promise.resolve();

    expect(sender.replacements.at(-1)).toBe(sharedTrack);
    expect(sharedTrack.enabled).toBe(true);
  });

  it('should still stop a microphone opened by realtime itself for non-shared uses', async () => {
    const ownedTrack = new FakeMediaStreamTrack();
    getUserMedia.mockResolvedValue(fakeMediaStream(ownedTrack));

    await expect(service.connect('visit-vitals', 'vitals')).rejects.toThrow();

    expect(getUserMedia).toHaveBeenCalledTimes(1);
    expect(ownedTrack.stopCalls).toBeGreaterThan(0);
  });

  it('should emit transcript with lower-quintile confidence and provenance', () => {
    const turns: RealtimeTranscriptTurn[] = [];
    const subscription = service.transcript$.subscribe(turn => turns.push(turn));

    serverEvent({
      type: 'conversation.item.input_audio_transcription.completed',
      event_id: 'event-42',
      item_id: 'item-9',
      transcript: 'Patient sans fièvre',
      logprobs: [
        { token: 'Patient', logprob: Math.log(0.98) },
        { token: 'sans', logprob: Math.log(0.72) },
        { token: 'fièvre', logprob: Math.log(0.91) },
        { token: '.', logprob: Math.log(0.99) },
        { token: ' ', logprob: Math.log(0.95) },
      ],
    });

    expect(turns).toHaveLength(1);
    expect(turns[0].transcript).toBe('Patient sans fièvre');
    expect(turns[0].eventId).toBe('event-42');
    expect(turns[0].itemId).toBe('item-9');
    expect(turns[0].confidence).toBeCloseTo(0.72, 5);
    subscription.unsubscribe();
  });

  it('should mark transcript confidence unverifiable when logprobs are absent', () => {
    const turns: RealtimeTranscriptTurn[] = [];
    const subscription = service.transcript$.subscribe(turn => turns.push(turn));

    serverEvent({
      type: 'conversation.item.input_audio_transcription.completed',
      event_id: 'event-no-confidence',
      transcript: 'Texte reconnu',
    });

    expect(turns).toHaveLength(1);
    expect(turns[0].confidence).toBeNull();
    subscription.unsubscribe();
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

class FakeRtpSender {
  readonly replacements: Array<MediaStreamTrack | null> = [];

  constructor(public track: MediaStreamTrack | null) {}

  async replaceTrack(track: MediaStreamTrack | null): Promise<void> {
    this.track = track;
    this.replacements.push(track);
  }
}

class FakePeerConnection extends EventTarget {
  readonly channel = new FakeDataChannel();
  readonly senders: FakeRtpSender[] = [];
  iceGatheringState: RTCIceGatheringState = 'complete';
  iceConnectionState: RTCIceConnectionState = 'new';
  connectionState: RTCPeerConnectionState = 'new';
  localDescription: RTCSessionDescription | null = null;
  ontrack: ((event: RTCTrackEvent) => void) | null = null;
  onconnectionstatechange: (() => void) | null = null;
  oniceconnectionstatechange: (() => void) | null = null;

  addTrack(track: MediaStreamTrack): RTCRtpSender {
    const sender = new FakeRtpSender(track);
    this.senders.push(sender);
    return sender as unknown as RTCRtpSender;
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
    // The failing HTTP request prevents this method from being called in these tests.
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

class FakeMediaStreamTrack extends EventTarget {
  enabled = true;
  muted = false;
  readyState: MediaStreamTrackState = 'live';
  stopCalls = 0;

  stop(): void {
    this.stopCalls += 1;
    this.readyState = 'ended';
  }
}

function fakeMediaStream(track = new FakeMediaStreamTrack()): MediaStream {
  return {
    getAudioTracks: () => [track as unknown as MediaStreamTrack],
    getTracks: () => [track as unknown as MediaStreamTrack],
  } as unknown as MediaStream;
}
