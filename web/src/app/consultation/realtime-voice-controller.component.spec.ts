import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, Subject, of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import {
  AiConsultationApiService,
  AiMessageResponse,
  AiSessionResponse,
} from './ai-consultation-api.service';
import { RealtimeVoiceBridgeService, RealtimeVoiceState } from './realtime-voice-bridge.service';
import { RealtimeVoiceControllerComponent } from './realtime-voice-controller.component';

describe('RealtimeVoiceControllerComponent continuous conversation', () => {
  let fixture: ComponentFixture<RealtimeVoiceControllerComponent>;
  let component: RealtimeVoiceControllerComponent;
  let state: BehaviorSubject<RealtimeVoiceState>;
  let transcripts: Subject<string>;
  let bridge: {
    state$: BehaviorSubject<RealtimeVoiceState>;
    transcript$: Subject<string>;
    error$: Subject<string>;
    assistantTurnCompleted$: Subject<void>;
    connect: ReturnType<typeof vi.fn>;
    disconnect: ReturnType<typeof vi.fn>;
    isSupported: ReturnType<typeof vi.fn>;
    setMuted: ReturnType<typeof vi.fn>;
    speakApproved: ReturnType<typeof vi.fn>;
  };
  let api: {
    sendText: ReturnType<typeof vi.fn>;
    answerClarification: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    state = new BehaviorSubject<RealtimeVoiceState>({
      connected: false,
      connecting: false,
      userSpeaking: false,
      assistantSpeaking: false,
      muted: false,
    });
    transcripts = new Subject<string>();
    bridge = {
      state$: state,
      transcript$: transcripts,
      error$: new Subject<string>(),
      assistantTurnCompleted$: new Subject<void>(),
      connect: vi.fn().mockRejectedValue(new Error('Realtime indisponible')),
      disconnect: vi.fn(),
      isSupported: vi.fn().mockReturnValue(true),
      setMuted: vi.fn(),
      speakApproved: vi.fn().mockReturnValue(false),
    };
    api = {
      sendText: vi.fn().mockReturnValue(of(messageResponse('Je vous écoute.'))),
      answerClarification: vi.fn().mockReturnValue(of(messageResponse('Merci pour la précision.'))),
    };

    await TestBed.configureTestingModule({
      imports: [RealtimeVoiceControllerComponent],
      providers: [
        { provide: RealtimeVoiceBridgeService, useValue: bridge },
        { provide: AiConsultationApiService, useValue: api },
        {
          provide: I18nService,
          useValue: {
            t: (key: string, fallback?: string) => fallback ?? key,
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RealtimeVoiceControllerComponent);
    component = fixture.componentInstance;
    component.visitId = 'visit-1';
    component.enabled = true;
    component.session = activeSession();
  });

  it('should attempt realtime only once per visit after a connection failure', async () => {
    await (component as any).syncConnection();
    await (component as any).syncConnection();

    expect(bridge.connect).toHaveBeenCalledTimes(1);
  });

  it('should analyze each realtime transcript immediately without pending review', () => {
    const emitted = vi.spyOn(component.message, 'emit');

    transcripts.next(' Patient sans fièvre ');

    expect(api.sendText).toHaveBeenCalledWith('visit-1', 'Patient sans fièvre');
    expect(api.answerClarification).not.toHaveBeenCalled();
    expect(emitted).toHaveBeenCalledWith(expect.objectContaining({
      assistantMessage: 'Je vous écoute.',
    }));
    expect(bridge.setMuted).toHaveBeenNthCalledWith(1, true);
    expect(bridge.setMuted).toHaveBeenLastCalledWith(false);
  });

  it('should route the next spoken turn to the pending clarification automatically', () => {
    component.session = {
      ...activeSession(),
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

    transcripts.next(' Depuis trois jours ');

    expect(api.answerClarification).toHaveBeenCalledWith(
      'visit-1',
      'clarification-1',
      'Depuis trois jours',
    );
    expect(api.sendText).not.toHaveBeenCalled();
    expect(bridge.setMuted).toHaveBeenLastCalledWith(false);
  });

  it('should keep the microphone available while Joprelys is speaking', () => {
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

  it('should keep the microphone paused when a clinical revision requires a decision', () => {
    api.sendText.mockReturnValue(of({
      ...messageResponse('Une proposition attend votre validation.'),
      revisions: [{
        id: 'revision-1',
        sequence: 1,
        status: 'PENDING',
        createdAt: '2026-07-26T10:00:00Z',
        proposals: [],
      }],
    }));

    transcripts.next('Je prescris le traitement indiqué.');

    expect(bridge.setMuted).toHaveBeenLastCalledWith(true);
  });

  function activeSession(): AiSessionResponse {
    return {
      sessionId: 'session-1',
      visitId: 'visit-1',
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
