import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { AiConsultationApiService, AiSessionResponse } from './ai-consultation-api.service';
import { VoiceAssistantPanelComponent } from './voice-assistant-panel.component';

describe('VoiceAssistantPanelComponent focused consultation flow', () => {
  let fixture: ComponentFixture<VoiceAssistantPanelComponent>;
  let component: VoiceAssistantPanelComponent;
  let api: {
    getSession: ReturnType<typeof vi.fn>;
    transcribeAudio: ReturnType<typeof vi.fn>;
    startSession: ReturnType<typeof vi.fn>;
    synthesizeSpeech: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    api = {
      getSession: vi.fn().mockReturnValue(of(null)),
      transcribeAudio: vi.fn().mockReturnValue(of({
        sessionId: 'session-1',
        transcript: 'Patient sans fièvre',
        status: 'PENDING_REVIEW',
        expiresAt: '2026-07-26T10:00:00Z',
      })),
      startSession: vi.fn().mockReturnValue(of(activeSession())),
      synthesizeSpeech: vi.fn().mockReturnValue(of(new Blob())),
    };

    await TestBed.configureTestingModule({
      imports: [VoiceAssistantPanelComponent],
      providers: [
        { provide: AiConsultationApiService, useValue: api },
        {
          provide: I18nService,
          useValue: {
            t: (_key: string, fallback?: string) => fallback ?? _key,
            currentLanguage: () => 'fr',
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VoiceAssistantPanelComponent);
    component = fixture.componentInstance;
    component.visitId = 'visit-1';
    component.session.set(activeSession());
  });

  afterEach(() => TestBed.resetTestingModule());

  it('should never synthesize the generic greeting from the parent panel', () => {
    (component as any).refreshSession();

    expect(api.synthesizeSpeech).not.toHaveBeenCalled();
  });

  it('should switch from realtime to dictation without deleting the session', () => {
    component.conversationMode.set(true);
    component.realtimeActive.set(true);

    component.finishRealtime();

    expect(component.conversationMode()).toBe(false);
    expect(component.realtimeActive()).toBe(false);
    expect(component.session()).not.toBeNull();
  });

  it('should block classic recording while a clarification is pending', () => {
    component.session.set({
      ...activeSession(),
      clarifications: [{ status: 'PENDING' }],
    } as unknown as AiSessionResponse);

    expect(component.recordingBlocked()).toBe(true);
  });

  it('should discard a non-empty audio container when no speech was detected', () => {
    (component as any).handleClassicCapture({
      audio: new Blob(['encoded-silence'], { type: 'audio/webm' }),
      hasSpeech: false,
    });

    expect(api.transcribeAudio).not.toHaveBeenCalled();
    expect(component.errorMessage()).toContain('Aucune parole détectée');
  });

  it('should stage classic speech for explicit review', () => {
    component.conversationMode.set(false);
    (component as any).handleClassicCapture({
      audio: new Blob(['encoded-speech'], { type: 'audio/webm' }),
      hasSpeech: true,
    });

    expect(api.transcribeAudio).toHaveBeenCalledTimes(1);
    expect(component.session()?.pendingTranscript).toBe('Patient sans fièvre');
    expect(component.session()?.transcriptStatus).toBe('PENDING_REVIEW');
  });

  function activeSession(): AiSessionResponse {
    return {
      sessionId: 'session-1',
      visitId: 'visit-1',
      status: 'ACTIVE',
      expiresAt: '2026-07-26T22:00:00Z',
      draft: {},
      transcript: null,
      pendingTranscript: null,
      transcriptStatus: 'NONE',
      conversation: [],
      clarifications: [],
      revisions: [],
      assistantMessage: 'Bonjour docteur. Je vous écoute.',
      needsClarification: false,
    };
  }
});
