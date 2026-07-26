import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Subject, of, throwError } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import {
  AiConsultationApiService,
  AiMessageResponse,
  AiSessionResponse,
} from './ai-consultation-api.service';
import { AmbientAudioCaptureService, AmbientCaptureState } from './ambient-audio-capture.service';
import {
  RealtimeClinicalIntakeAck,
  RealtimeClinicalIntakeApiService,
} from './realtime-clinical-intake-api.service';
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

const FR: Record<string, string> = {
  'consultation.ai.realtimeTranscriptUnverified': 'Une phrase a été entendue mais sa confiance est trop incertaine pour être utilisée cliniquement. L’audio protégé est conservé.',
  'consultation.ai.realtimeTranscriptLowConfidence': 'Une phrase reste trop incertaine pour être utilisée cliniquement. L’audio protégé est conservé.',
  'consultation.ai.realtimeBackpressure': 'Joprelys sécurise les dernières secondes avant de reprendre automatiquement.',
  'consultation.ai.realtimeClinicalError': 'La phrase a été entendue, mais son analyse clinique a échoué.',
  'consultation.ai.realtimeConversationAnalysisFailed': 'La phrase a bien été sauvegardée, mais l’assistant n’a pas pu l’analyser.',
  'consultation.ai.realtimeUnavailable': 'Le temps réel est momentanément indisponible. Reconnexion automatique en cours.',
  'consultation.ai.realtimeUnsupported': 'Ce navigateur ne prend pas en charge la connexion audio temps réel.',
  'consultation.ai.ambientRequired': 'La protection audio n’est pas disponible.',
};

describe('RealtimeVoiceControllerComponent durable clinical listening', () => {
  let fixture: ComponentFixture<RealtimeVoiceControllerComponent>;
  let component: RealtimeVoiceControllerComponent;
  let state: BehaviorSubject<RealtimeVoiceState>;
  let transcripts: Subject<RealtimeTranscriptTurn>;
  let ambientState: BehaviorSubject<AmbientCaptureState>;
  let sharedStream: MediaStream;
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
    mediaStreamForVisit: ReturnType<typeof vi.fn>;
  };
  let intake: {
    ingest: ReturnType<typeof vi.fn>;
  };
  let consultationApi: {
    sendRealtimeTranscript: ReturnType<typeof vi.fn>;
    answerRealtimeClarification: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    vi.useFakeTimers();
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
    sharedStream = {} as MediaStream;
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
        ambientState.next({ ...ambientState.value, active: true, starting: false, recovering: false });
      }),
      stop: vi.fn().mockImplementation(async () => {
        ambientState.next({ ...ambientState.value, active: false, starting: false, recovering: false });
      }),
      mediaStreamForVisit: vi.fn().mockReturnValue(sharedStream),
    };
    intake = {
      ingest: vi.fn().mockImplementation((visitId: string, text: string, confidence: number, eventId: string, itemId?: string) =>
        of(ack(visitId, text, confidence, eventId, itemId))),
    };
    consultationApi = {
      sendRealtimeTranscript: vi.fn().mockImplementation((_visitId: string, text: string) =>
        of(messageResponse(text))),
      answerRealtimeClarification: vi.fn().mockImplementation((_visitId: string, _clarificationId: string, text: string) =>
        of(messageResponse(text))),
    };

    await TestBed.configureTestingModule({
      imports: [RealtimeVoiceControllerComponent],
      providers: [
        { provide: RealtimeVoiceBridgeService, useValue: bridge },
        { provide: AmbientAudioCaptureService, useValue: ambient },
        { provide: RealtimeClinicalIntakeApiService, useValue: intake },
        { provide: AiConsultationApiService, useValue: consultationApi },
        {
          provide: I18nService,
          useValue: {
            currentLanguage: () => 'fr',
            t: (key: string, fallback?: string) => FR[key] ?? fallback ?? key,
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
    state.next({ ...DISCONNECTED_STATE, connected: true });
  });

  afterEach(() => {
    component.ngOnDestroy();
    vi.useRealTimers();
    TestBed.resetTestingModule();
  });

  it('should start ambient safety capture before realtime and reuse its exact stream', async () => {
    bridge.connect.mockResolvedValue(undefined);
    (component as any).connectedVisitId = '';
    state.next({ ...DISCONNECTED_STATE });

    await (component as any).syncConnection(0, false);

    expect(ambient.start).toHaveBeenCalledWith('visit-1', 'fr');
    expect(bridge.connect).toHaveBeenCalledWith('visit-1', 'consultation', expect.any(Function));
    const provider = bridge.connect.mock.calls[0][2] as () => MediaStream | null;
    expect(provider()).toBe(sharedStream);
    expect(ambient.start.mock.invocationCallOrder[0]).toBeLessThan(bridge.connect.mock.invocationCallOrder[0]);
  });

  it('should persist a realtime turn before asking the clinical assistant to analyze it', () => {
    transcripts.next(turn('Douleur depuis trois jours', 0.91, 'event-order', 'item-order'));

    expect(intake.ingest).toHaveBeenCalledTimes(1);
    expect(consultationApi.sendRealtimeTranscript).toHaveBeenCalledTimes(1);
    expect(intake.ingest.mock.invocationCallOrder[0])
      .toBeLessThan(consultationApi.sendRealtimeTranscript.mock.invocationCallOrder[0]);
    expect(consultationApi.sendRealtimeTranscript).toHaveBeenCalledWith(
      'visit-1',
      'Douleur depuis trois jours',
      0.91,
      'event-order',
    );
  });

  it('should queue three final ASR turns while the first server ACK is slow and persist all three in order', () => {
    const firstAck = new Subject<RealtimeClinicalIntakeAck>();
    intake.ingest
      .mockReturnValueOnce(firstAck)
      .mockImplementation((visitId: string, text: string, confidence: number, eventId: string, itemId?: string) =>
        of(ack(visitId, text, confidence, eventId, itemId)));

    transcripts.next(turn('Première phrase clinique', 0.91, 'event-1', 'item-1'));
    transcripts.next(turn('Deuxième phrase clinique', 0.92, 'event-2', 'item-2'));
    transcripts.next(turn('Troisième phrase clinique', 0.93, 'event-3', 'item-3'));

    expect(intake.ingest).toHaveBeenCalledTimes(1);
    expect((component as any).transcriptQueue).toHaveLength(3);
    expect(component.processing()).toBe(true);
    expect(bridge.setMuted).not.toHaveBeenCalledWith(true);
    expect(consultationApi.sendRealtimeTranscript).not.toHaveBeenCalled();

    firstAck.next(ack('visit-1', 'Première phrase clinique', 0.91, 'event-1', 'item-1'));
    firstAck.complete();

    expect(intake.ingest).toHaveBeenCalledTimes(3);
    expect(intake.ingest.mock.calls.map(call => call[1])).toEqual([
      'Première phrase clinique',
      'Deuxième phrase clinique',
      'Troisième phrase clinique',
    ]);
    expect(consultationApi.sendRealtimeTranscript).toHaveBeenCalledTimes(3);
    expect((component as any).transcriptQueue).toHaveLength(0);
  });

  it('should route the next durable turn to an outstanding realtime clarification', () => {
    component.session = {
      ...activeSession('visit-1'),
      clarifications: [{
        id: 'clar-1',
        field: 'symptoms',
        question: 'Depuis combien de jours ?',
        status: 'PENDING',
        options: [],
        createdAt: '2026-07-26T18:00:00Z',
        answer: null,
        resolvedAt: null,
      }],
    };

    transcripts.next(turn('Depuis trois jours', 0.95, 'event-clar', 'item-clar'));

    expect(intake.ingest).toHaveBeenCalledTimes(1);
    expect(consultationApi.answerRealtimeClarification).toHaveBeenCalledWith(
      'visit-1',
      'clar-1',
      'Depuis trois jours',
      0.95,
      'event-clar',
    );
    expect(consultationApi.sendRealtimeTranscript).not.toHaveBeenCalled();
  });

  it('should vocalize only the backend-approved assistant response after durable intake', () => {
    bridge.speakApproved.mockReturnValue(true);
    consultationApi.sendRealtimeTranscript.mockReturnValue(of({
      ...messageResponse('Texte utilisateur'),
      assistantMessage: 'Pouvez-vous préciser la localisation de la douleur ?',
    }));

    transcripts.next(turn('J’ai mal depuis hier', 0.93, 'event-voice', 'item-voice'));

    expect(bridge.speakApproved).toHaveBeenCalledWith(
      'Pouvez-vous préciser la localisation de la douleur ?',
    );
  });

  it('should deduplicate the same OpenAI item even if a second event id is received', () => {
    transcripts.next(turn('Patient sans fièvre', 0.9, 'event-a', 'item-same'));
    transcripts.next(turn('Patient sans fièvre', 0.9, 'event-b', 'item-same'));

    expect(intake.ingest).toHaveBeenCalledTimes(1);
  });

  it('should keep a transiently failed turn at the head of the queue and retry durable intake', async () => {
    intake.ingest
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 503 })))
      .mockImplementation((visitId: string, text: string, confidence: number, eventId: string, itemId?: string) =>
        of(ack(visitId, text, confidence, eventId, itemId)));

    transcripts.next(turn('Tension cent vingt sur quatre-vingt', 0.94, 'event-retry', 'item-retry'));

    expect((component as any).transcriptQueue).toHaveLength(1);
    expect(intake.ingest).toHaveBeenCalledTimes(1);
    expect(consultationApi.sendRealtimeTranscript).not.toHaveBeenCalled();

    await vi.advanceTimersByTimeAsync(1200);

    expect(intake.ingest).toHaveBeenCalledTimes(2);
    expect(consultationApi.sendRealtimeTranscript).toHaveBeenCalledTimes(1);
    expect((component as any).transcriptQueue).toHaveLength(0);
  });

  it('should not mute realtime merely because a durable ACK or analysis is in progress', () => {
    const response = new Subject<RealtimeClinicalIntakeAck>();
    intake.ingest.mockReturnValue(response);
    bridge.setMuted.mockClear();

    transcripts.next(turn('Douleur depuis trois jours', 0.9, 'event-live', 'item-live'));

    expect(component.processing()).toBe(true);
    expect(bridge.setMuted).not.toHaveBeenCalledWith(true);
    expect(component.effectiveMuted()).toBe(false);
  });

  it('should pause only realtime on queue backpressure while ambient safety continues', () => {
    const slow = new Subject<RealtimeClinicalIntakeAck>();
    intake.ingest.mockReturnValue(slow);
    bridge.setMuted.mockClear();

    for (let index = 0; index < 32; index += 1) {
      transcripts.next(turn(`Phrase ${index}`, 0.9, `event-${index}`, `item-${index}`));
    }

    expect(component.backlogPaused).toBe(true);
    expect(bridge.setMuted).toHaveBeenCalledWith(true);
    expect(ambient.stop).not.toHaveBeenCalled();
    expect((component as any).transcriptQueue).toHaveLength(32);
  });

  it('should reset queued turns and dedupe memory when navigating from visit A to B', async () => {
    const slow = new Subject<RealtimeClinicalIntakeAck>();
    intake.ingest.mockReturnValue(slow);
    transcripts.next(turn('Tour A', 0.9, 'event-a', 'item-a'));
    transcripts.next(turn('Tour A deux', 0.9, 'event-a2', 'item-a2'));
    expect((component as any).transcriptQueue).toHaveLength(2);

    bridge.connect.mockResolvedValue(undefined);
    component.visitId = 'visit-2';
    component.session = activeSession('visit-2');
    state.next({ ...DISCONNECTED_STATE, connected: true });

    await (component as any).syncConnection(0, true);

    expect((component as any).transcriptQueue).toHaveLength(0);
    expect((component as any).seenTranscriptIds.size).toBe(0);
    expect((component as any).connectedVisitId).toBe('visit-2');
  });

  it('should display but not clinically analyze an unverifiable turn', () => {
    const emittedError = vi.spyOn(component.realtimeError, 'emit');

    transcripts.next(turn('texte incertain', null, 'event-low', 'item-low'));

    expect(component.lastTranscript()).toBe('texte incertain');
    expect(intake.ingest).not.toHaveBeenCalled();
    expect(consultationApi.sendRealtimeTranscript).not.toHaveBeenCalled();
    expect(emittedError).toHaveBeenCalledWith(expect.stringContaining('trop incertaine'));
    expect(ambient.stop).not.toHaveBeenCalled();
  });

  it('should hard-stop realtime and ambient on explicit clinician mute, then reconnect both on unmute', async () => {
    bridge.connect.mockResolvedValue(undefined);

    component.toggleMute();
    await (component as any).connectionTransition;

    expect(component.manualMuted).toBe(true);
    expect(bridge.disconnect).toHaveBeenCalled();
    expect(ambient.stop).toHaveBeenCalled();

    component.toggleMute();
    await (component as any).connectionTransition;

    expect(component.manualMuted).toBe(false);
    expect(ambient.start).toHaveBeenCalledWith('visit-1', 'fr');
    expect(bridge.connect).toHaveBeenCalledWith('visit-1', 'consultation', expect.any(Function));
  });

  function turn(
    transcript: string,
    confidence: number | null,
    eventId?: string,
    itemId?: string,
  ): RealtimeTranscriptTurn {
    return { transcript, confidence, eventId, itemId };
  }

  function ack(
    visitId: string,
    transcript: string,
    confidence: number,
    eventId: string,
    itemId?: string,
  ): RealtimeClinicalIntakeAck {
    return {
      id: `ack-${eventId}`,
      visitId,
      sequence: 1,
      eventId,
      itemId: itemId ?? null,
      transcript,
      confidence,
      receivedAt: '2026-07-26T18:00:00Z',
    };
  }

  function messageResponse(transcript: string): AiMessageResponse {
    return {
      sessionId: 'session-1',
      transcript,
      draft: {},
      changedFields: [],
      assistantMessage: '',
      needsClarification: false,
      conversation: [],
      clarifications: [],
      revisions: [],
      expiresAt: '2026-07-26T20:00:00Z',
    };
  }

  function activeSession(visitId: string): AiSessionResponse {
    return {
      sessionId: 'session-1',
      visitId,
      status: 'ACTIVE',
      expiresAt: '2026-07-26T20:00:00Z',
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
