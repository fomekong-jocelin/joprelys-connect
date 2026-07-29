import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, Subject, of, throwError } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
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
  'consultation.ai.realtimeUnavailable': 'Le temps réel est momentanément indisponible.',
  'consultation.ai.realtimeUnsupported': 'Ce navigateur ne prend pas en charge la connexion audio temps réel.',
  'consultation.ai.ambientRequired': 'La protection audio n’est pas disponible.',
  'consultation.ai.realtimeDurableIntakeBlocked': 'La sauvegarde sécurisée est interrompue.',
};

describe('RealtimeVoiceControllerComponent durable clinical capture', () => {
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
      ingest: vi.fn().mockImplementation(
        (visitId: string, text: string, confidence: number | null, eventId: string, itemId?: string) =>
          of(ack(visitId, text, confidence, eventId, itemId)),
      ),
      list: vi.fn().mockReturnValue(of([])),
      correct: vi.fn().mockImplementation((_visitId: string, id: string, text: string) =>
        of({ ...ack('visit-1', text, 0.95, `correct-${id}`), id, correctionCount: 1, originalTranscript: 'ancien texte' })),
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

  it('persists every final ASR turn and never waits for an AI proposal', () => {
    transcripts.next(turn('Première phrase clinique', 0.91, 'event-1', 'item-1'));
    transcripts.next(turn('Deuxième phrase clinique', 0.92, 'event-2', 'item-2'));
    transcripts.next(turn('Troisième phrase clinique', 0.93, 'event-3', 'item-3'));

    expect(intake.ingest).toHaveBeenCalledTimes(3);
    expect(intake.ingest.mock.calls.map(call => call[1])).toEqual([
      'Première phrase clinique',
      'Deuxième phrase clinique',
      'Troisième phrase clinique',
    ]);
    expect((component as any).pipeline.analysisQueueSize()).toBe(0);
    expect(component.queuedCount()).toBe(0);
    expect(component.transcriptHistory()).toHaveLength(3);
  });

  it('queues later phrases while the first durable ACK is slow and preserves their order', () => {
    const firstAck = new Subject<RealtimeClinicalIntakeAck>();
    intake.ingest
      .mockReturnValueOnce(firstAck)
      .mockImplementation(
        (visitId: string, text: string, confidence: number | null, eventId: string, itemId?: string) =>
          of(ack(visitId, text, confidence, eventId, itemId)),
      );

    transcripts.next(turn('Phrase une', 0.91, 'event-a', 'item-a'));
    transcripts.next(turn('Phrase deux', 0.92, 'event-b', 'item-b'));
    transcripts.next(turn('Phrase trois', 0.93, 'event-c', 'item-c'));

    expect(intake.ingest).toHaveBeenCalledTimes(1);
    expect(component.queuedCount()).toBe(3);

    firstAck.next(ack('visit-1', 'Phrase une', 0.91, 'event-a', 'item-a'));
    firstAck.complete();

    expect(intake.ingest).toHaveBeenCalledTimes(3);
    expect(intake.ingest.mock.calls.map(call => call[1])).toEqual(['Phrase une', 'Phrase deux', 'Phrase trois']);
    expect(component.queuedCount()).toBe(0);
  });

  it('keeps low or missing confidence transcript as reviewable evidence', () => {
    transcripts.next(turn('Le patient nie toute fièvre', null, 'event-null', 'item-null'));

    expect(intake.ingest).toHaveBeenCalledWith(
      'visit-1', 'Le patient nie toute fièvre', null, 'event-null', 'item-null',
    );
    expect(component.transcriptHistory()).toHaveLength(1);
    expect(component.transcriptHistory()[0].reviewRequired).toBe(true);
    expect(component.transcriptNeedsReview()).toBe(true);
  });

  it('generates a safe client event id when OpenAI omits event_id', () => {
    transcripts.next(turn('Saturation quatre-vingt-seize', 0.92, undefined, 'item-without-event'));

    expect(intake.ingest).toHaveBeenCalledTimes(1);
    expect(intake.ingest.mock.calls[0][3] as string).toMatch(/^client:\d+:\d+$/);
  });

  it('deduplicates the same OpenAI item even if a second event id is received', () => {
    transcripts.next(turn('Patient sans fièvre', 0.9, 'event-a', 'item-same'));
    transcripts.next(turn('Patient sans fièvre', 0.9, 'event-b', 'item-same'));

    expect(intake.ingest).toHaveBeenCalledTimes(1);
  });

  it('retries transient durable persistence failures without dropping the phrase', async () => {
    intake.ingest
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 503 })))
      .mockImplementation(
        (visitId: string, text: string, confidence: number | null, eventId: string, itemId?: string) =>
          of(ack(visitId, text, confidence, eventId, itemId)),
      );

    transcripts.next(turn('Tension cent vingt sur quatre-vingt', 0.94, 'event-retry', 'item-retry'));
    expect(component.queuedCount()).toBe(1);

    await vi.advanceTimersByTimeAsync(1200);

    expect(intake.ingest).toHaveBeenCalledTimes(2);
    expect(component.queuedCount()).toBe(0);
    expect(component.transcriptHistory()[0].text).toContain('Tension');
  });

  it('fails closed only when durable persistence cannot be guaranteed', () => {
    intake.ingest.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 422 })));
    const error = vi.spyOn(component.realtimeError, 'emit');

    transcripts.next(turn('Phrase à protéger', 0.9, 'event-hard', 'item-hard'));

    expect((component as any).pipeline.durableBlocked()).toBe(true);
    expect(bridge.setMuted).toHaveBeenCalledWith(true);
    expect(error).toHaveBeenCalled();
    expect(component.queuedCount()).toBe(1);
  });

  it('loads recoverable durable transcript when the consultation screen is reopened', () => {
    intake.list.mockReturnValue(of([
      ack('visit-1', 'Phrase sauvegardée avant navigation', 0.9, 'event-old', 'item-old'),
    ]));

    component.ngOnChanges({
      visitId: {
        previousValue: '',
        currentValue: 'visit-1',
        firstChange: true,
        isFirstChange: () => true,
      },
    });

    expect(intake.list).toHaveBeenCalledWith('visit-1');
    expect(component.transcriptHistory()[0].text).toBe('Phrase sauvegardée avant navigation');
  });

  it('corrects the exact durable segment instead of creating a new clinical utterance', () => {
    const existing = ack('visit-1', 'Il a mal au bra.', 0.5, 'event-old', 'item-old');
    intake.list.mockReturnValue(of([existing]));
    component.ngOnChanges({
      visitId: {
        previousValue: '',
        currentValue: 'visit-1',
        firstChange: true,
        isFirstChange: () => true,
      },
    });

    component.requestTranscriptCorrection({ id: existing.id, text: 'Il a mal au bras.' });

    expect(intake.correct).toHaveBeenCalledWith('visit-1', existing.id, 'Il a mal au bras.');
    expect(component.transcriptHistory()[0].text).toBe('Il a mal au bras.');
  });

  it('waits for the last durable ACK before ending the capture step', () => {
    const delayed = new Subject<RealtimeClinicalIntakeAck>();
    intake.ingest.mockReturnValueOnce(delayed);
    const ended = vi.spyOn(component.endSession, 'emit');

    transcripts.next(turn('Dernière phrase', 0.9, 'event-last', 'item-last'));
    component.requestFinish();
    expect(ended).not.toHaveBeenCalled();

    delayed.next(ack('visit-1', 'Dernière phrase', 0.9, 'event-last', 'item-last'));
    delayed.complete();

    expect(ended).toHaveBeenCalledTimes(1);
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
    confidence: number | null,
    eventId: string,
    itemId?: string,
  ): RealtimeClinicalIntakeAck {
    const normalizedConfidence = confidence ?? 0;
    return {
      id: `ack-${eventId}`,
      visitId,
      source: 'CONSULTATION',
      sequence: 1,
      eventId,
      itemId: itemId ?? null,
      transcript,
      originalTranscript: null,
      confidence: normalizedConfidence,
      reviewRequired: confidence === null || normalizedConfidence < 0.35,
      correctionCount: 0,
      correctedAt: null,
      captureStatus: 'PENDING',
      receivedAt: '2026-07-29T18:00:00Z',
    };
  }

  function activeSession(visitId: string) {
    return {
      sessionId: 'session-1',
      visitId,
      status: 'ACTIVE',
      expiresAt: '2026-07-29T20:00:00Z',
      draft: {},
      transcript: null,
      pendingTranscript: null,
      transcriptStatus: 'NONE' as const,
      conversation: [],
      clarifications: [],
      revisions: [],
      assistantMessage: null,
      needsClarification: false,
    };
  }
});
