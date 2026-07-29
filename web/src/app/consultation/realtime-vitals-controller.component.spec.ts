import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, Subject, of, throwError } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { AiVitalsApiService, AiVitalsProposal } from './ai-vitals-api.service';
import { ClinicalVoicePlaybackService } from './clinical-voice-playback.service';
import {
  RealtimeClinicalIntakeAck,
  RealtimeClinicalIntakeApiService,
} from './realtime-clinical-intake-api.service';
import {
  RealtimeTranscriptTurn,
  RealtimeVoiceBridgeService,
  RealtimeVoiceState,
} from './realtime-voice-bridge.service';
import { RealtimeVitalsControllerComponent } from './realtime-vitals-controller.component';

const CONNECTED: RealtimeVoiceState = {
  connected: true,
  connecting: false,
  userSpeaking: false,
  assistantSpeaking: false,
  muted: false,
};

describe('RealtimeVitalsControllerComponent clinical queue safety', () => {
  let fixture: ComponentFixture<RealtimeVitalsControllerComponent>;
  let component: RealtimeVitalsControllerComponent;
  let state: BehaviorSubject<RealtimeVoiceState>;
  let transcripts: Subject<RealtimeTranscriptTurn>;
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
  let intake: { ingestVitals: ReturnType<typeof vi.fn> };
  let api: { analyzeText: ReturnType<typeof vi.fn> };
  let playback: { play: ReturnType<typeof vi.fn>; stop: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    vi.useFakeTimers();
    state = new BehaviorSubject<RealtimeVoiceState>({ ...CONNECTED });
    transcripts = new Subject<RealtimeTranscriptTurn>();
    bridge = {
      state$: state,
      transcript$: transcripts,
      error$: new Subject<string>(),
      assistantTurnCompleted$: new Subject<void>(),
      connect: vi.fn().mockResolvedValue(undefined),
      disconnect: vi.fn(),
      isSupported: vi.fn().mockReturnValue(true),
      setMuted: vi.fn(),
    };
    intake = {
      ingestVitals: vi.fn().mockImplementation(
        (visitId: string, text: string, confidence: number | null, eventId: string, itemId?: string) =>
          of(ack(visitId, text, confidence, eventId, itemId)),
      ),
    };
    api = {
      analyzeText: vi.fn().mockImplementation((_visitId: string, text: string) => of(proposal(text))),
    };
    playback = { play: vi.fn(), stop: vi.fn() };

    TestBed.configureTestingModule({
      imports: [RealtimeVitalsControllerComponent],
      providers: [
        { provide: AiVitalsApiService, useValue: api },
        { provide: RealtimeClinicalIntakeApiService, useValue: intake },
        { provide: ClinicalVoicePlaybackService, useValue: playback },
        {
          provide: I18nService,
          useValue: {
            currentLanguage: () => 'fr',
            t: (_key: string, fallback?: string) => fallback ?? _key,
          },
        },
      ],
    });
    TestBed.overrideComponent(RealtimeVitalsControllerComponent, {
      set: { providers: [{ provide: RealtimeVoiceBridgeService, useValue: bridge }] },
    });
    await TestBed.compileComponents();

    fixture = TestBed.createComponent(RealtimeVitalsControllerComponent);
    component = fixture.componentInstance;
    component.visitId = 'visit-a';
    component.enabled = true;
    fixture.detectChanges();
    (component as any).connectedVisitId = 'visit-a';
    state.next({ ...CONNECTED });
  });

  afterEach(() => {
    fixture.destroy();
    vi.useRealTimers();
    TestBed.resetTestingModule();
  });

  it('durably persists 21 turns while the first clinical analysis is slow, then analyzes all in order', () => {
    const firstAnalysis = new Subject<AiVitalsProposal>();
    api.analyzeText
      .mockReturnValueOnce(firstAnalysis)
      .mockImplementation((_visitId: string, text: string) => of(proposal(text)));

    for (let index = 0; index < 21; index += 1) {
      transcripts.next(turn(`Constante ${index}`, 0.92, `event-${index}`, `item-${index}`));
    }

    expect(intake.ingestVitals).toHaveBeenCalledTimes(21);
    expect(intake.ingestVitals.mock.calls.map(call => call[1])).toEqual(
      Array.from({ length: 21 }, (_, index) => `Constante ${index}`),
    );
    expect(api.analyzeText).toHaveBeenCalledTimes(1);
    expect((component as any).intakeQueue).toHaveLength(0);
    expect((component as any).analysisQueue).toHaveLength(21);
    expect(component.processing()).toBe(true);
    expect(bridge.setMuted).not.toHaveBeenCalledWith(true);

    firstAnalysis.next(proposal('Constante 0'));
    firstAnalysis.complete();

    expect(api.analyzeText).toHaveBeenCalledTimes(21);
    expect(api.analyzeText.mock.calls.map(call => call[1])).toEqual(
      Array.from({ length: 21 }, (_, index) => `Constante ${index}`),
    );
    expect(component.queuedCount()).toBe(0);
  });

  it('persists before analysis for every realtime vitals turn', () => {
    transcripts.next(turn('Pouls cent quatre', 0.91, 'event-order', 'item-order'));

    expect(intake.ingestVitals).toHaveBeenCalledTimes(1);
    expect(api.analyzeText).toHaveBeenCalledTimes(1);
    expect(intake.ingestVitals.mock.invocationCallOrder[0])
      .toBeLessThan(api.analyzeText.mock.invocationCallOrder[0]);
  });

  it('speaks a realtime clinical response once without muting the microphone', () => {
    api.analyzeText.mockReturnValueOnce(of({
      ...proposal('Tension douze sur huit'),
      assistantMessage: 'Confirmez-vous une tension de 120 sur 80 mmHg ?',
      needsConfirmation: true,
    }));
    bridge.setMuted.mockClear();

    transcripts.next(turn('Tension douze sur huit', 0.91, 'event-question', 'item-question'));

    expect(playback.play)
      .toHaveBeenCalledWith('Confirmez-vous une tension de 120 sur 80 mmHg ?');
    expect(bridge.setMuted).not.toHaveBeenCalledWith(true);
  });

  it('uses the shared listening surface and delegates its stop action', () => {
    fixture.detectChanges();
    const stopped = vi.spyOn(component.stopListening, 'emit');

    expect(fixture.nativeElement.querySelector('app-voice-listening-surface')).not.toBeNull();
    fixture.nativeElement.querySelector('[data-testid="voice-listening-stop"]').click();

    expect(stopped).toHaveBeenCalledTimes(1);
  });

  it('keeps a non-empty null-confidence transcript reviewable and durable', () => {
    transcripts.next(turn('Saturation quatre-vingt-seize', null, 'event-null', 'item-null'));

    expect(intake.ingestVitals).toHaveBeenCalledWith(
      'visit-a',
      'Saturation quatre-vingt-seize',
      null,
      'event-null',
      'item-null',
    );
    expect(api.analyzeText).toHaveBeenCalledWith(
      'visit-a',
      'Saturation quatre-vingt-seize',
      'fr',
      {},
    );
  });

  it('does not mute capture merely because persistence or analysis is in progress', () => {
    const slowAck = new Subject<RealtimeClinicalIntakeAck>();
    intake.ingestVitals.mockReturnValueOnce(slowAck);
    bridge.setMuted.mockClear();

    transcripts.next(turn('Pouls cent quatre', 0.91, 'event-live', 'item-live'));

    expect(component.processing()).toBe(true);
    expect(component.effectiveMuted()).toBe(false);
    expect(bridge.setMuted).not.toHaveBeenCalledWith(true);
  });

  it('retries a transient durable-intake failure without analyzing the turn twice', async () => {
    intake.ingestVitals
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 503 })))
      .mockImplementation(
        (visitId: string, text: string, confidence: number | null, eventId: string, itemId?: string) =>
          of(ack(visitId, text, confidence, eventId, itemId)),
      );

    transcripts.next(turn('Température trente-huit', 0.9, 'event-retry', 'item-retry'));

    expect(intake.ingestVitals).toHaveBeenCalledTimes(1);
    expect(api.analyzeText).not.toHaveBeenCalled();
    expect((component as any).intakeQueue).toHaveLength(1);

    await vi.advanceTimersByTimeAsync(1200);

    expect(intake.ingestVitals).toHaveBeenCalledTimes(2);
    expect(api.analyzeText).toHaveBeenCalledTimes(1);
    expect(component.queuedCount()).toBe(0);
  });

  it('keeps an unacknowledged turn and pauses realtime on a non-transient persistence failure', () => {
    intake.ingestVitals.mockReturnValueOnce(
      throwError(() => new HttpErrorResponse({ status: 403 })),
    );
    const error = vi.spyOn(component.realtimeError, 'emit');

    transcripts.next(turn('Tension cent vingt sur quatre-vingt', 0.9, 'event-denied', 'item-denied'));

    expect(component.durableBlocked()).toBe(true);
    expect((component as any).intakeQueue).toHaveLength(1);
    expect(api.analyzeText).not.toHaveBeenCalled();
    expect(bridge.setMuted).toHaveBeenCalledWith(true);
    expect(error).toHaveBeenCalled();
  });

  it('never mutes the clinician because an assistant audio state is reported', () => {
    bridge.setMuted.mockClear();

    state.next({ ...CONNECTED, assistantSpeaking: true });

    expect(component.effectiveMuted()).toBe(false);
    expect(bridge.setMuted).not.toHaveBeenCalledWith(true);
    expect((bridge as any).speakApproved).toBeUndefined();
  });

  it('deduplicates the same OpenAI item before durable persistence', () => {
    transcripts.next(turn('Poids soixante-dix kilos', 0.9, 'event-a', 'item-same'));
    transcripts.next(turn('Poids soixante-dix kilos', 0.9, 'event-b', 'item-same'));

    expect(intake.ingestVitals).toHaveBeenCalledTimes(1);
    expect(api.analyzeText).toHaveBeenCalledTimes(1);
  });

  it('generates a safe event id when OpenAI omits event_id', () => {
    transcripts.next(turn('Température trente-sept huit', 0.9, undefined, 'item-no-event'));

    expect(intake.ingestVitals).toHaveBeenCalledTimes(1);
    expect(intake.ingestVitals.mock.calls[0][3] as string).toMatch(/^vitals:\d+:\d+$/);
  });

  it('disconnects the previous visit, clears queued turns and ignores a late old response', async () => {
    const oldAnalysis = new Subject<AiVitalsProposal>();
    api.analyzeText.mockReturnValueOnce(oldAnalysis);
    const proposed = vi.spyOn(component.proposed, 'emit');

    transcripts.next(turn('Température trente-huit', 0.93, 'event-old', 'item-old'));
    expect(component.processing()).toBe(true);

    component.visitId = 'visit-b';
    component.ngOnChanges({
      visitId: {
        previousValue: 'visit-a',
        currentValue: 'visit-b',
        firstChange: false,
        isFirstChange: () => false,
      },
    });
    await Promise.resolve();

    expect(bridge.disconnect).toHaveBeenCalled();
    expect(component.queuedCount()).toBe(0);

    oldAnalysis.next(proposal('Température trente-huit'));
    oldAnalysis.complete();
    expect(proposed).not.toHaveBeenCalled();
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
      source: 'VITALS',
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
      receivedAt: '2026-07-27T21:00:00Z',
    };
  }

  function proposal(transcript: string): AiVitalsProposal {
    return {
      transcript,
      vitals: { pulse: 80 },
      assistantMessage: '',
      needsConfirmation: false,
      confirmationReason: '',
    };
  }
});
