import { SimpleChange } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { BehaviorSubject, Subject, of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { AiSessionResponse } from './ai-consultation-api.service';
import { AmbientAudioCaptureService, AmbientCaptureState } from './ambient-audio-capture.service';
import { RealtimeClinicalIntakeApiService } from './realtime-clinical-intake-api.service';
import {
  RealtimeVoiceBridgeService,
  RealtimeVoiceState,
} from './realtime-voice-bridge.service';
import { RealtimeVoiceControllerComponent } from './realtime-voice-controller.component';

const DISCONNECTED: RealtimeVoiceState = {
  connected: false,
  connecting: false,
  userSpeaking: false,
  assistantSpeaking: false,
  muted: false,
};

describe('RealtimeVoiceControllerComponent connection stability', () => {
  let state: BehaviorSubject<RealtimeVoiceState>;
  let ambientState: BehaviorSubject<AmbientCaptureState>;
  let bridge: {
    state$: BehaviorSubject<RealtimeVoiceState>;
    transcript$: Subject<any>;
    error$: Subject<string>;
    assistantTurnCompleted$: Subject<void>;
    connect: ReturnType<typeof vi.fn>;
    disconnect: ReturnType<typeof vi.fn>;
    isSupported: ReturnType<typeof vi.fn>;
    setMuted: ReturnType<typeof vi.fn>;
  };
  let ambient: {
    state$: BehaviorSubject<AmbientCaptureState>;
    start: ReturnType<typeof vi.fn>;
    stop: ReturnType<typeof vi.fn>;
    mediaStreamForVisit: ReturnType<typeof vi.fn>;
  };
  let intake: {
    ingest: ReturnType<typeof vi.fn>;
    list: ReturnType<typeof vi.fn>;
    correct: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    state = new BehaviorSubject<RealtimeVoiceState>({ ...DISCONNECTED });
    ambientState = new BehaviorSubject<AmbientCaptureState>({
      supported: true,
      active: true,
      starting: false,
      recovering: false,
      pendingChunks: 0,
      pendingBytes: 0,
      uploading: false,
      online: true,
      storagePressure: false,
      lastError: null,
    });
    bridge = {
      state$: state,
      transcript$: new Subject(),
      error$: new Subject(),
      assistantTurnCompleted$: new Subject(),
      connect: vi.fn().mockResolvedValue(undefined),
      disconnect: vi.fn(),
      isSupported: vi.fn().mockReturnValue(true),
      setMuted: vi.fn(),
    };
    ambient = {
      state$: ambientState,
      start: vi.fn().mockResolvedValue(undefined),
      stop: vi.fn().mockResolvedValue(undefined),
      mediaStreamForVisit: vi.fn().mockReturnValue({} as MediaStream),
    };
    intake = {
      ingest: vi.fn(),
      list: vi.fn().mockReturnValue(of([])),
      correct: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [RealtimeVoiceControllerComponent],
      providers: [
        { provide: RealtimeVoiceBridgeService, useValue: bridge },
        { provide: AmbientAudioCaptureService, useValue: ambient },
        { provide: RealtimeClinicalIntakeApiService, useValue: intake },
        {
          provide: I18nService,
          useValue: {
            currentLanguage: () => 'fr',
            t: (key: string, fallback?: string) => fallback ?? key,
          },
        },
      ],
    }).compileComponents();
  });

  afterEach(() => TestBed.resetTestingModule());

  it('does not restart WebRTC when only the transient session expiry changes', () => {
    const fixture = TestBed.createComponent(RealtimeVoiceControllerComponent);
    const component = fixture.componentInstance;
    component.visitId = 'visit-1';
    component.enabled = true;
    const previous = activeSession('visit-1');
    component.session = { ...previous };
    const queueSpy = vi.spyOn(component as any, 'queueConnectionSync');

    component.ngOnChanges({
      session: new SimpleChange(previous, { ...previous, expiresAt: '2026-07-27T20:05:00Z' }, false),
    });

    expect(queueSpy).not.toHaveBeenCalled();
    expect(intake.list).toHaveBeenCalledWith('visit-1');
    component.ngOnDestroy();
  });

  it('does not cancel bridge-owned reconnect after an initial connection failure', async () => {
    const fixture = TestBed.createComponent(RealtimeVoiceControllerComponent);
    const component = fixture.componentInstance;
    component.visitId = 'visit-1';
    component.enabled = true;
    component.session = activeSession('visit-1');
    bridge.connect.mockRejectedValueOnce(new Error('temporary realtime failure'));

    await expect((component as any).syncConnection(0, false)).rejects.toThrow('temporary realtime failure');

    expect(bridge.disconnect).not.toHaveBeenCalled();
    expect(ambient.stop).not.toHaveBeenCalled();
    component.ngOnDestroy();
  });

  it('recognizes a successful bridge-owned reconnect for the current visit', () => {
    const fixture = TestBed.createComponent(RealtimeVoiceControllerComponent);
    const component = fixture.componentInstance;
    component.visitId = 'visit-1';
    component.enabled = true;
    component.session = activeSession('visit-1');

    state.next({ ...DISCONNECTED, connected: true });

    expect((component as any).connectedVisitId).toBe('visit-1');
    component.ngOnDestroy();
  });

  function activeSession(visitId: string): AiSessionResponse {
    return {
      sessionId: 'session-1',
      visitId,
      status: 'ACTIVE',
      expiresAt: '2026-07-27T20:00:00Z',
      draft: {},
      transcript: null,
      pendingTranscript: null,
      transcriptStatus: 'NONE',
      conversation: [],
      clarifications: [],
      revisions: [],
      assistantMessage: null,
      needsClarification: false,
    };
  }
});
