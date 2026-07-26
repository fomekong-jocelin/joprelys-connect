import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, Subject, of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import {
  AiConsultationApiService,
  AiMessageResponse,
  AiSessionResponse,
} from './ai-consultation-api.service';
import { AmbientAudioCaptureService, AmbientCaptureState } from './ambient-audio-capture.service';
import {
  RealtimeTranscriptTurn,
  RealtimeVoiceBridgeService,
  RealtimeVoiceState,
} from './realtime-voice-bridge.service';
import { RealtimeVoiceControllerComponent } from './realtime-voice-controller.component';

const DISCONNECTED_STATE: RealtimeVoiceState = {
  connected: false,
  connecting: false,
  userSpeaking: false,
  assistantSpeaking: false,
  muted: false,
};

describe('RealtimeVoiceControllerComponent continuous conversation', () => {
  let fixture: ComponentFixture<RealtimeVoiceControllerComponent>;
  let component: RealtimeVoiceControllerComponent;
  let state: BehaviorSubject<RealtimeVoiceState>;
  let transcripts: Subject<RealtimeTranscriptTurn>;
  let ambientState: BehaviorSubject<AmbientCaptureState>;
  let bridge: {
    state$: BehaviorSubject<RealtimeVoiceState>;
    transcript$: Subject<RealtimeTranscriptTurn>;
    error$: Subject<string>;
    assistantTurnCompleted$: Subject<void>;
    connect: ReturnType<typeof vi.fn>;
    disconnect: ReturnType<typeof vi.fn>;
    isSupported: ReturnType<typeof vi.fn>;
    setMuted: ReturnType<typeof vi.fn>;
    speakApproved: ReturnType<typeof vi.fn>;
  };
  let ambient: {
    state$: BehaviorSubject<AmbientCaptureState>;
    start: ReturnType<typeof vi.fn>;
    stop: ReturnType<typeof vi.fn>;
  };
  let api: {
    sendRealtimeTranscript: ReturnType<typeof vi.fn>;
    answerRealtimeClarification: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    state = new BehaviorSubject<RealtimeVoiceState>({ ...DISCONNECTED_STATE });
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
    transcripts = new Subject<RealtimeTranscriptTurn>();
    bridge = {
      state$: state,
      transcript$: transcripts,
      error$: new Subject<string>(),
      assistantTurnCompleted$: new Subject<void>(),
      connect: vi.fn().mockRejectedValue(new Error('Realtime indisponible')),
      disconnect: vi.fn(() => state.next({ ...DISCONNECTED_STATE })),
      isSupported: vi.fn().mockReturnValue(true),
      setMuted: vi.fn(),
      speakApproved: vi.fn().mockReturnValue(false),
    };
    ambient = {
      state$: ambientState,
      start: vi.fn().mockImplementation(async () => {
        ambientState.next({
          ...ambientState.value,
          active: true,
          starting: false,
          recovering: false,
        });
      }),
      stop: vi.fn().mockImplementation(async () => {
        ambientState.next({
          ...ambientState.value,
          active: false,
          starting: false,
          recovering: false,
        });
      }),
    };
    api = {
      sendRealtimeTranscript: vi.fn().mockReturnValue(of(messageResponse('Je vous écoute.'))),
      answerRealtimeClarification: vi.fn().mockReturnValue(of(messageResponse('Merci pour la précision.'))),
    };

    await TestBed.configureTestingModule({
      imports: [RealtimeVoiceControllerComponent],
      providers: [
        { provide: RealtimeVoiceBridgeService, useValue: bridge },
        { provide: AmbientAudioCaptureService, useValue: ambient },
        { provide: AiConsultationApiService, useValue: api },
        {
          provide: I18nService,
          useValue: {
            currentLanguage: () => 'fr',
            t: (key: string, fallback?: string) => fallback ?? key,
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RealtimeVoiceControllerComponent);
    component = fixture.componentInstance;
    component.visitId = 'visit-1';
    component.enabled = true;
    component.session = activeSession('visit-1');
    (component as any).connectedVisitId = 'visit-1';
  });

  it('should start encrypted ambient safety capture before attempting realtime', async () => {
    bridge.connect.mockResolvedValue(undefined);
    (component as any).connectedVisitId = '';

    await (component as any).syncConnection(0, false);

    expect(ambient.start).toHaveBeenCalledWith('visit-1', 'fr');
    expect(bridge.connect).toHaveBeenCalledWith('visit-1');
    expect(ambient.start.mock.invocationCallOrder[0]).toBeLessThan(bridge.connect.mock.invocationCallOrder[0]);
  });

  it('should keep ambient capture alive when realtime connection fails', async () => {
    (component as any).connectedVisitId = '';

    await expect((component as any).syncConnection(0, false)).rejects.toThrow('Realtime indisponible');
    await expect((component as any).syncConnection(0, false)).rejects.toThrow('Realtime indisponible');

    expect(bridge.connect).toHaveBeenCalledTimes(2);
    expect(ambient.start).toHaveBeenCalledTimes(2);
    expect(ambient.stop).not.toHaveBeenCalled();
  });

  it('should refuse realtime when the required ambient safety capture cannot start', async () => {
    ambient.start.mockRejectedValue(new Error('vault unavailable'));
    (component as any).connectedVisitId = '';

    await expect((component as any).syncConnection(0, false)).rejects.toThrow('capture audio de sécurité');

    expect(bridge.connect).not.toHaveBeenCalled();
  });

  it('should stop and flush visit A before starting visit B', async () => {
    state.next({ ...DISCONNECTED_STATE, connected: true });
    bridge.connect.mockResolvedValue(undefined);
    component.visitId = 'visit-2';
    component.session = activeSession('visit-2');

    await (component as any).syncConnection(0, true);

    expect(bridge.disconnect).toHaveBeenCalled();
    expect(ambient.stop).toHaveBeenCalled();
    expect(ambient.start).toHaveBeenCalledWith('visit-2', 'fr');
    expect(bridge.connect).toHaveBeenCalledWith('visit-2');
    expect(ambient.stop.mock.invocationCallOrder[0]).toBeLessThan(ambient.start.mock.invocationCallOrder[0]);
    expect(ambient.start.mock.invocationCallOrder[0]).toBeLessThan(bridge.connect.mock.invocationCallOrder[0]);
    expect((component as any).connectedVisitId).toBe('visit-2');
  });

  it('should ignore a late transcript from visit A after navigation to visit B', () => {
    component.visitId = 'visit-2';
    component.session = activeSession('visit-2');
    (component as any).connectedVisitId = 'visit-1';

    transcripts.next(turn('Ancien tour de la visite A', 0.95, 'late-a'));

    expect(api.sendRealtimeTranscript).not.toHaveBeenCalled();
    expect(api.answerRealtimeClarification).not.toHaveBeenCalled();
  });

  it('should ignore an in-flight backend response from visit A after navigation to visit B', () => {
    const response = new Subject<AiMessageResponse>();
    api.sendRealtimeTranscript.mockReturnValue(response);
    const emitted = vi.spyOn(component.message, 'emit');

    transcripts.next(turn('Tour visite A', 0.95, 'event-a'));
    expect(api.sendRealtimeTranscript).toHaveBeenCalledWith('visit-1', 'Tour visite A', 0.95, 'event-a');

    component.visitId = 'visit-2';
    component.session = activeSession('visit-2');
    (component as any).connectedVisitId = '';
    response.next(messageResponse('Réponse tardive A'));
    response.complete();

    expect(emitted).not.toHaveBeenCalled();
  });

  it('should resynchronize realtime mute state when realtime reconnects without stopping ambient capture', () => {
    component.blocked = true;
    state.next({
      connected: true,
      connecting: false,
      userSpeaking: false,
      assistantSpeaking: false,
      muted: true,
    });

    expect(bridge.setMuted).toHaveBeenCalledWith(true);
    expect(ambient.stop).not.toHaveBeenCalled();
  });

  it('should analyze a verified realtime transcript with provenance while ambient capture remains active', () => {
    const emitted = vi.spyOn(component.message, 'emit');

    transcripts.next(turn(' Patient sans fièvre ', 0.91, 'event-1'));

    expect(api.sendRealtimeTranscript).toHaveBeenCalledWith(
      'visit-1',
      'Patient sans fièvre',
      0.91,
      'event-1',
    );
    expect(api.answerRealtimeClarification).not.toHaveBeenCalled();
    expect(emitted).toHaveBeenCalledWith(expect.objectContaining({
      assistantMessage: 'Je vous écoute.',
    }));
    expect(bridge.setMuted).toHaveBeenNthCalledWith(1, true);
    expect(bridge.setMuted).toHaveBeenLastCalledWith(false);
    expect(ambient.stop).not.toHaveBeenCalled();
  });

  it('should refuse a realtime transcript when ASR confidence is unavailable', () => {
    const emittedError = vi.spyOn(component.realtimeError, 'emit');

    transcripts.next(turn('texte incertain', null));

    expect(api.sendRealtimeTranscript).not.toHaveBeenCalled();
    expect(api.answerRealtimeClarification).not.toHaveBeenCalled();
    expect(emittedError).toHaveBeenCalledWith(expect.stringContaining('Transcription non vérifiable'));
  });

  it('should route a verified spoken answer to the pending clarification', () => {
    component.session = {
      ...activeSession('visit-1'),
      clarifications: [{
        id: 'clarification-1',
        field: 'symptoms',
        question: 'Depuis combien de temps ?',
        status: 'PENDING',
        options: [],
        createdAt: '2026-07-26T10:00:00Z',
        answer: null,
        resolvedAt: null,
      }],
    };

    transcripts.next(turn(' Depuis trois jours ', 0.88, 'event-2'));

    expect(api.answerRealtimeClarification).toHaveBeenCalledWith(
      'visit-1',
      'clarification-1',
      'Depuis trois jours',
      0.88,
      'event-2',
    );
    expect(api.sendRealtimeTranscript).not.toHaveBeenCalled();
    expect(bridge.setMuted).toHaveBeenLastCalledWith(false);
  });

  it('should keep the realtime microphone available while Joprelys is speaking', () => {
    state.next({
      connected: true,
      connecting: false,
      userSpeaking: false,
      assistantSpeaking: true,
      muted: false,
    });

    expect(component.effectiveMuted()).toBe(false);
    (component as any).syncMute();
    expect(bridge.setMuted).toHaveBeenLastCalledWith(false);
  });

  it('should pause realtime for a clinical decision while ambient safety capture continues', () => {
    api.sendRealtimeTranscript.mockReturnValue(of({
      ...messageResponse('Une proposition attend votre validation.'),
      revisions: [{
        id: 'revision-1',
        sequence: 1,
        status: 'PENDING',
        createdAt: '2026-07-26T10:00:00Z',
        proposals: [],
      }],
    }));

    transcripts.next(turn('Je confirme le contenu dicté.', 0.9));

    expect(bridge.setMuted).toHaveBeenLastCalledWith(true);
    expect(ambient.stop).not.toHaveBeenCalled();
  });

  it('should stop all microphone capture on an explicit clinician mute and restart it on unmute', async () => {
    state.next({
      connected: true,
      connecting: false,
      userSpeaking: false,
      assistantSpeaking: false,
      muted: false,
    });

    component.toggleMute();
    expect(ambient.stop).toHaveBeenCalledTimes(1);
    expect(bridge.setMuted).toHaveBeenLastCalledWith(true);

    component.toggleMute();
    await Promise.resolve();
    await Promise.resolve();
    expect(ambient.start).toHaveBeenCalledWith('visit-1', 'fr');
  });

  function turn(
    transcript: string,
    confidence: number | null,
    eventId?: string,
  ): RealtimeTranscriptTurn {
    return { transcript, confidence, eventId };
  }

  function activeSession(visitId: string): AiSessionResponse {
    return {
      sessionId: 'session-1',
      visitId,
      status: 'ACTIVE',
      expiresAt: '2026-07-26T10:30:00Z',
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

  function messageResponse(assistantMessage: string): AiMessageResponse {
    return {
      sessionId: 'session-1',
      transcript: 'Patient sans fièvre',
      draft: {},
      changedFields: [],
      assistantMessage,
      needsClarification: false,
      conversation: [],
      clarifications: [],
      revisions: [],
      expiresAt: '2026-07-26T10:30:00Z',
    };
  }
});
