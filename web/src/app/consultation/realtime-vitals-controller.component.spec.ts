import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, Subject, of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { AiVitalsApiService, AiVitalsProposal } from './ai-vitals-api.service';
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
  let assistantCompleted: Subject<void>;
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
  let api: { analyzeText: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    state = new BehaviorSubject<RealtimeVoiceState>({ ...CONNECTED });
    transcripts = new Subject<RealtimeTranscriptTurn>();
    assistantCompleted = new Subject<void>();
    bridge = {
      state$: state,
      transcript$: transcripts,
      error$: new Subject<string>(),
      assistantTurnCompleted$: assistantCompleted,
      connect: vi.fn().mockResolvedValue(undefined),
      disconnect: vi.fn(),
      isSupported: vi.fn().mockReturnValue(true),
      setMuted: vi.fn(),
      speakApproved: vi.fn().mockReturnValue(false),
    };
    api = {
      analyzeText: vi.fn().mockImplementation((_visitId: string, text: string) => of(proposal(text))),
    };

    TestBed.configureTestingModule({
      imports: [RealtimeVitalsControllerComponent],
      providers: [
        { provide: AiVitalsApiService, useValue: api },
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
    TestBed.resetTestingModule();
  });

  it('queues 21 turns received while the first analysis is slow and processes every turn in order', () => {
    const first = new Subject<AiVitalsProposal>();
    api.analyzeText
      .mockReturnValueOnce(first)
      .mockImplementation((_visitId: string, text: string) => of(proposal(text)));

    for (let index = 0; index < 21; index += 1) {
      transcripts.next(turn(`Constante ${index}`, 0.92, `event-${index}`, `item-${index}`));
    }

    expect(api.analyzeText).toHaveBeenCalledTimes(1);
    expect((component as any).transcriptQueue).toHaveLength(21);
    expect(component.processing()).toBe(true);
    expect(bridge.setMuted).not.toHaveBeenCalledWith(true);

    first.next(proposal('Constante 0'));
    first.complete();

    expect(api.analyzeText).toHaveBeenCalledTimes(21);
    expect(api.analyzeText.mock.calls.map(call => call[1])).toEqual(
      Array.from({ length: 21 }, (_, index) => `Constante ${index}`),
    );
    expect((component as any).transcriptQueue).toHaveLength(0);
  });

  it('keeps a non-empty transcript clinically reviewable when ASR confidence is null', () => {
    const warning = vi.spyOn(component.realtimeError, 'emit');

    transcripts.next(turn('Saturation quatre-vingt-seize', null, 'event-null', 'item-null'));

    expect(api.analyzeText).toHaveBeenCalledWith(
      'visit-a',
      'Saturation quatre-vingt-seize',
      'fr',
      {},
    );
    expect(warning).toHaveBeenCalled();
  });

  it('does not mute capture merely because a vitals analysis is in progress', () => {
    const slow = new Subject<AiVitalsProposal>();
    api.analyzeText.mockReturnValueOnce(slow);
    bridge.setMuted.mockClear();

    transcripts.next(turn('Pouls cent quatre', 0.91, 'event-live', 'item-live'));

    expect(component.processing()).toBe(true);
    expect(component.effectiveMuted()).toBe(false);
    expect(bridge.setMuted).not.toHaveBeenCalledWith(true);
  });

  it('disconnects the previous visit and ignores its late analysis response after navigation', async () => {
    const oldResponse = new Subject<AiVitalsProposal>();
    api.analyzeText.mockReturnValueOnce(oldResponse);
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
    expect((component as any).transcriptQueue).toHaveLength(0);

    oldResponse.next(proposal('Température trente-huit'));
    oldResponse.complete();

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
