import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { AiConsultationApiService, AiSessionResponse } from './ai-consultation-api.service';
import { ClassicVoiceRecorderService } from './classic-voice-recorder.service';
import { VoiceAssistantPanelComponent } from './voice-assistant-panel.component';

describe('VoiceAssistantPanelComponent stability', () => {
  let api: {
    getSession: ReturnType<typeof vi.fn>;
    transcribeAudio: ReturnType<typeof vi.fn>;
  };
  let recorder: {
    supported: boolean;
    dispose: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    api = {
      getSession: vi.fn().mockReturnValue(of(activeSession())),
      transcribeAudio: vi.fn().mockReturnValue(of({
        sessionId: 'session-1',
        transcript: 'Le patient présente une douleur abdominale depuis trois jours.',
        status: 'PENDING_REVIEW',
        expiresAt: '2026-07-27T20:00:00Z',
      })),
    };
    recorder = {
      supported: true,
      dispose: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: AiConsultationApiService, useValue: api },
        { provide: ClassicVoiceRecorderService, useValue: recorder },
        {
          provide: I18nService,
          useValue: {
            currentLanguage: () => 'fr',
            t: (key: string, fallback?: string) => fallback ?? key,
          },
        },
      ],
    });
  });

  afterEach(() => {
    vi.useRealTimers();
    TestBed.resetTestingModule();
  });

  it('sends every non-empty dictation to transcription even if browser VAD says no speech', () => {
    const component = TestBed.runInInjectionContext(() => new VoiceAssistantPanelComponent());
    component.visitId = 'visit-1';
    component.session.set(activeSession());

    (component as any).handleClassicCapture({
      audio: new Blob(['recorded-audio'], { type: 'audio/webm' }),
      hasSpeech: false,
    });

    expect(api.transcribeAudio).toHaveBeenCalledTimes(1);
    expect(component.session()?.pendingTranscript)
      .toBe('Le patient présente une douleur abdominale depuis trois jours.');
    expect(component.session()?.transcriptStatus).toBe('PENDING_REVIEW');
  });

  it('does not poll the server session while realtime mode owns the connection lifecycle', async () => {
    vi.useFakeTimers();
    const component = TestBed.runInInjectionContext(() => new VoiceAssistantPanelComponent());
    component.visitId = 'visit-1';
    component.conversationMode.set(true);

    component.ngOnInit();
    expect(api.getSession).toHaveBeenCalledTimes(1);

    await vi.advanceTimersByTimeAsync(12000);

    expect(api.getSession).toHaveBeenCalledTimes(1);
    component.ngOnDestroy();
  });

  function activeSession(): AiSessionResponse {
    return {
      sessionId: 'session-1',
      visitId: 'visit-1',
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
