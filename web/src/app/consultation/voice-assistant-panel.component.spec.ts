import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { AiConsultationApiService, AiSessionResponse } from './ai-consultation-api.service';
import { VoiceAssistantPanelComponent } from './voice-assistant-panel.component';

describe('VoiceAssistantPanelComponent conversational fallback', () => {
  let fixture: ComponentFixture<VoiceAssistantPanelComponent>;
  let component: VoiceAssistantPanelComponent;
  let api: {
    loadQrCode: ReturnType<typeof vi.fn>;
    getSession: ReturnType<typeof vi.fn>;
    transcribeAudio: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    api = {
      loadQrCode: vi.fn().mockReturnValue(of(new Blob())),
      getSession: vi.fn().mockReturnValue(of(null)),
      transcribeAudio: vi.fn().mockReturnValue(of({
        sessionId: 'session-1',
        transcript: 'Patient sans fièvre',
        status: 'PENDING_REVIEW',
        expiresAt: '2026-07-26T10:00:00Z',
      })),
    };
    await TestBed.configureTestingModule({
      imports: [VoiceAssistantPanelComponent],
      providers: [
        {
          provide: AiConsultationApiService,
          useValue: api,
        },
        {
          provide: I18nService,
          useValue: {
            t: (key: string, fallback?: string) => fallback ?? key,
            currentLanguage: () => 'fr',
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VoiceAssistantPanelComponent);
    component = fixture.componentInstance;
    component.visitId = 'visit-1';
    component.conversationMode.set(true);
    component.realtimeActive.set(false);
    component.session.set({
      pendingTranscript: null,
      revisions: [],
      clarifications: [],
    } as unknown as AiSessionResponse);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should never restart classic recording after assistant speech', () => {
    const startRecording = vi
      .spyOn(component as any, 'startRecording')
      .mockResolvedValue(undefined);

    (component as any).finishSpeaking();

    expect(startRecording).not.toHaveBeenCalled();
  });

  it('should block classic recording while a clarification is pending', () => {
    component.session.set({
      pendingTranscript: null,
      clarifications: [{ status: 'PENDING' }],
      revisions: [],
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

  it('should stage classic speech for review even in conversation mode', () => {
    (component as any).handleClassicCapture({
      audio: new Blob(['encoded-speech'], { type: 'audio/webm' }),
      hasSpeech: true,
    });

    expect(api.transcribeAudio).toHaveBeenCalledTimes(1);
    expect(component.session()?.pendingTranscript).toBe('Patient sans fièvre');
    expect(component.session()?.transcriptStatus).toBe('PENDING_REVIEW');
  });
});
