import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, Subject, of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import {
  AiConsultationApiService,
  AiSessionResponse,
} from './ai-consultation-api.service';
import { RealtimeVoiceBridgeService, RealtimeVoiceState } from './realtime-voice-bridge.service';
import { RealtimeVoiceControllerComponent } from './realtime-voice-controller.component';

describe('RealtimeVoiceControllerComponent safety lifecycle', () => {
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
    stageRealtimeTranscript: ReturnType<typeof vi.fn>;
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
      stageRealtimeTranscript: vi.fn().mockReturnValue(of({
        sessionId: 'session-1',
        transcript: 'Patient sans fièvre',
        status: 'PENDING_REVIEW',
        expiresAt: '2026-07-26T10:00:00Z',
      })),
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

  it('should stage a realtime transcript for medical review without analysing it', () => {
    const staged = vi.spyOn(component.transcription, 'emit');

    transcripts.next(' Patient sans fièvre ');

    expect(api.stageRealtimeTranscript).toHaveBeenCalledWith(
      'visit-1',
      'Patient sans fièvre',
    );
    expect(staged).toHaveBeenCalledWith(expect.objectContaining({
      status: 'PENDING_REVIEW',
    }));
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
});
