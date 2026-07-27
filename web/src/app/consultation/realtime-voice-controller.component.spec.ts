import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, Subject, of, throwError } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import {
  AiConsultationApiService,
  AiMessageResponse,
  AiSessionResponse,
  AiTranscriptionResponse,
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
  'consultation.ai.realtimeBackpressure': 'Joprelys sécurise les dernières secondes avant de reprendre automatiquement.',
  'consultation.ai.realtimeClinicalError': 'La phrase a été entendue, mais sa sauvegarde a échoué.',
  'consultation.ai.realtimeConversationAnalysisFailed': 'La phrase a bien été sauvegardée, mais l’assistant n’a pas pu l’analyser.',
  'consultation.ai.realtimeUnavailable': 'Le temps réel est momentanément indisponible.',
  'consultation.ai.realtimeUnsupported': 'Ce navigateur ne prend pas en charge la connexion audio temps réel.',
  'consultation.ai.ambientRequired': 'La protection audio n’est pas disponible.',
};

describe('RealtimeVoiceControllerComponent clinical safety pipeline', () => {
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
  let intake: { ingest: ReturnType<typeof vi.fn> };
  let consultationApi: {
    sendRealtimeTranscript: ReturnType<typeof vi.fn>;
    answerRealtimeClarification: ReturnType<typeof vi.fn>;
    stageRealtimeTranscript: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    vi.useFakeTimers();
    state = new BehaviorSubject<RealtimeVoiceState>({ ...DISCONNECTED_STATE });
    transcripts = new Subject<RealtimeTranscriptTurn>();
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
      sendRealtimeTranscript: vi.fn().mockImplementation((_visitId: string, text: string) => of(messageResponse(text))),
      answerRealtimeClarification: vi.fn().mockImplementation((_visitId: string, _clarificationId: string, text: string) => of(messageResponse(text))),
      stageRealtimeTranscript: vi.fn().mockImplementation((_visitId: string, text: string) => of(transcriptionResponse(text))),
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

  it('starts ambient safety capture before realtime and reuses its exact stream', async () => {
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

  it('persists a realtime turn before asking the clinical assistant to analyze it', () => {
    transcripts.next(turn('Douleur depuis trois jours', 0.91, 'event-order', 'item-order'));

    expect(intake.ingest).toHaveBeenCalledTimes(1);
    expect(consultationApi.sendRealtimeTranscript).toHaveBeenCalledTimes(1);
    expect(intake.ingest.mock.invocationCallOrder[0])
      .toBeLessThan(consultationApi.sendRealtimeTranscript.mock.invocationCallOrder[0]);
  });

  it('queues three final ASR turns while the first durable ACK is slow and persists all in order', () => {
    const firstAck = new Subject<RealtimeClinicalIntakeAck>();
    intake.ingest
      .mockReturnValueOnce(firstAck)
      .mockImplementation((visitId: string, text: string, confidence: number, eventId: string, itemId?: string) =>
        of(ack(visitId, text, confidence, eventId, itemId)));

    transcripts.next(turn('Première phrase clinique', 0.91, 'event-1', 'item-1'));
    transcripts.next(turn('Deuxième phrase clinique', 0.92, 'event-2', 'item-2'));
    transcripts.next(turn('Troisième phrase clinique', 0.93, 'event-3', 'item-3'));

    expect(intake.ingest).toHaveBeenCalledTimes(1);
    expect((component as any).intakeQueue).toHaveLength(3);
    expect(consultationApi.sendRealtimeTranscript).not.toHaveBeenCalled();
    expect(bridge.setMuted).not.toHaveBeenCalledWith(true);

    firstAck.next(ack('visit-1', 'Première phrase clinique', 0.91, 'event-1', 'item-1'));
    firstAck.complete();

    expect(intake.ingest).toHaveBeenCalledTimes(3);
    expect(intake.ingest.mock.calls.map(call => call[1])).toEqual([
      'Première phrase clinique',
      'Deuxième phrase clinique',
      'Troisième phrase clinique',
    ]);
    expect(consultationApi.sendRealtimeTranscript).toHaveBeenCalledTimes(3);
    expect(component.queuedCount()).toBe(0);
  });

  it('persists 21 turns while clinical review blocks analysis, then analyzes all in order after unblock', () => {
    component.blocked = true;
    bridge.setMuted.mockClear();

    for (let index = 0; index < 21; index += 1) {
      transcripts.next(turn(`Phrase ${index}`, 0.9, `event-${index}`, `item-${index}`));
    }

    expect(intake.ingest).toHaveBeenCalledTimes(21);
    expect(intake.ingest.mock.calls.map(call => call[1])).toEqual(
      Array.from({ length: 21 }, (_, index) => `Phrase ${index}`),
    );
    expect(consultationApi.sendRealtimeTranscript).not.toHaveBeenCalled();
    expect((component as any).analysisQueue).toHaveLength(21);
    expect(bridge.setMuted).not.toHaveBeenCalledWith(true);

    component.blocked = false;
    component.ngOnChanges({
      blocked: {
        previousValue: true,
        currentValue: false,
        firstChange: false,
        isFirstChange: () => false,
      },
    });

    expect(consultationApi.sendRealtimeTranscript).toHaveBeenCalledTimes(21);
    expect(consultationApi.sendRealtimeTranscript.mock.calls.map(call => call[1])).toEqual(
      Array.from({ length: 21 }, (_, index) => `Phrase ${index}`),
    );
    expect(component.queuedCount()).toBe(0);
  });

  it('persists a non-empty null-confidence transcript and stages it for human review instead of dropping it', () => {
    consultationApi.sendRealtimeTranscript.mockReturnValueOnce(
      throwError(() => ({ status: 422, error: { detail: 'AI_TRANSCRIPTION_LOW_CONFIDENCE' } })),
    );
    const review = vi.spyOn(component.transcriptionReview, 'emit');

    transcripts.next(turn('Le patient nie toute fièvre', null, 'event-null', 'item-null'));

    expect(component.lastTranscript()).toBe('Le patient nie toute fièvre');
    expect(intake.ingest).toHaveBeenCalledWith(
      'visit-1',
      'Le patient nie toute fièvre',
      0,
      'event-null',
      'item-null',
    );
    expect(consultationApi.stageRealtimeTranscript).toHaveBeenCalledWith(
      'visit-1',
      'Le patient nie toute fièvre',
    );
    expect(review).toHaveBeenCalledWith(expect.objectContaining({
      transcript: 'Le patient nie toute fièvre',
      status: 'PENDING_REVIEW',
    }));
    expect(component.queuedCount()).toBe(0);
  });

  it('generates a safe client event id when OpenAI omits event_id instead of losing the transcript', () => {
    transcripts.next(turn('Saturation quatre-vingt-seize', 0.92, undefined, 'item-without-event'));

    expect(intake.ingest).toHaveBeenCalledTimes(1);
    const eventId = intake.ingest.mock.calls[0][3] as string;
    expect(eventId).toMatch(/^client:\d+:\d+$/);
    expect(consultationApi.sendRealtimeTranscript).toHaveBeenCalledTimes(1);
  });

  it('deduplicates the same OpenAI item even if a second event id is received', () => {
    transcripts.next(turn('Patient sans fièvre', 0.9, 'event-a', 'item-same'));
    transcripts.next(turn('Patient sans fièvre', 0.9, 'event-b', 'item-same'));

    expect(intake.ingest).toHaveBeenCalledTimes(1);
  });

  it('keeps a transiently failed intake at the head and retries without duplicate clinical analysis', async () => {
    intake.ingest
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 503 })))
      .mockImplementation((visitId: string, text: string, confidence: number, eventId: string, itemId?: string) =>
        of(ack(visitId, text, confidence, eventId, itemId)));

    transcripts.next(turn('Tension cent vingt sur quatre-vingt', 0.94, 'event-retry', 'item-retry'));

    expect((component as any).intakeQueue).toHaveLength(1);
    expect(intake.ingest).toHaveBeenCalledTimes(1);
    expect(consultationApi.sendRealtimeTranscript).not.toHaveBeenCalled();

    await vi.advanceTimersByTimeAsync(1200);

    expect(intake.ingest).toHaveBeenCalledTimes(2);
    expect(consultationApi.sendRealtimeTranscript).toHaveBeenCalledTimes(1);
    expect(component.queuedCount()).toBe(0);
  });

  it('does not mute realtime merely because durable intake or analysis is in progress', () => {
    const slow = new Subject<RealtimeClinicalIntakeAck>();
    intake.ingest.mockReturnValue(slow);
    bridge.setMuted.mockClear();

    transcripts.next(turn('Douleur depuis trois jours', 0.9, 'event-live', 'item-live'));

    expect(component.processing()).toBe(true);
    expect(bridge.setMuted).not.toHaveBeenCalledWith(true);
    expect(component.effectiveMuted()).toBe(false);
  });

  it('pauses only realtime on queue backpressure while ambient safety continues', () => {
    const slow = new Subject<RealtimeClinicalIntakeAck>();
    intake.ingest.mockReturnValue(slow);
    bridge.setMuted.mockClear();

    for (let index = 0; index < 32; index += 1) {
      transcripts.next(turn(`Phrase ${index}`, 0.9, `event-${index}`, `item-${index}`));
    }

    expect(component.backlogPaused).toBe(true);
    expect(bridge.setMuted).toHaveBeenCalledWith(true);
    expect(ambient.stop).not.toHaveBeenCalled();
    expect((component as any).intakeQueue).toHaveLength(32);
  });

  it('resets queues and ignores late responses when navigating from visit A to B', async () => {
    const slow = new Subject<RealtimeClinicalIntakeAck>();
    intake.ingest.mockReturnValue(slow);
    transcripts.next(turn('Tour A', 0.9, 'event-a', 'item-a'));
    transcripts.next(turn('Tour A deux', 0.9, 'event-a2', 'item-a2'));
    expect((component as any).intakeQueue).toHaveLength(2);

    bridge.connect.mockResolvedValue(undefined);
    component.visitId = 'visit-2';
    component.session = activeSession('visit-2');
    await (component as any).syncConnection(0, true);

    expect(component.queuedCount()).toBe(0);
    expect((component as any).seenTranscriptIds.size).toBe(0);
    expect((component as any).connectedVisitId).toBe('visit-2');

    slow.next(ack('visit-1', 'Tour A', 0.9, 'event-a', 'item-a'));
    slow.complete();
    expect(consultationApi.sendRealtimeTranscript).not.toHaveBeenCalledWith(
      'visit-2',
      'Tour A',
      expect.anything(),
      expect.anything(),
    );
  });

  it('hard-stops realtime and ambient only on explicit clinician pause, then reconnects both', async () => {
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

  function transcriptionResponse(transcript: string): AiTranscriptionResponse {
    return {
      sessionId: 'session-1',
      transcript,
      status: 'PENDING_REVIEW',
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
