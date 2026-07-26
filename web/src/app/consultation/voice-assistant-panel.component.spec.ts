import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { AiConsultationApiService, AiSessionResponse } from './ai-consultation-api.service';
import { VoiceAssistantPanelComponent } from './voice-assistant-panel.component';

describe('VoiceAssistantPanelComponent conversational fallback', () => {
  let fixture: ComponentFixture<VoiceAssistantPanelComponent>;
  let component: VoiceAssistantPanelComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VoiceAssistantPanelComponent],
      providers: [
        {
          provide: AiConsultationApiService,
          useValue: {
            getQrCode: vi.fn().mockReturnValue(of(new Blob())),
            getSession: vi.fn().mockReturnValue(of(null)),
          },
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

  it('should restart classic recording after the assistant finishes a clarification', () => {
    vi.useFakeTimers();
    const startRecording = vi
      .spyOn(component as any, 'startRecording')
      .mockResolvedValue(undefined);

    (component as any).finishSpeaking(true);

    vi.advanceTimersByTime(299);
    expect(startRecording).not.toHaveBeenCalled();
    vi.advanceTimersByTime(1);
    expect(startRecording).toHaveBeenCalledTimes(1);
  });

  it('should not restart while a clinical revision is awaiting validation', () => {
    vi.useFakeTimers();
    component.session.set({
      pendingTranscript: null,
      clarifications: [],
      revisions: [{ status: 'PENDING' }],
    } as unknown as AiSessionResponse);
    const startRecording = vi
      .spyOn(component as any, 'startRecording')
      .mockResolvedValue(undefined);

    (component as any).finishSpeaking(true);
    vi.advanceTimersByTime(500);

    expect(startRecording).not.toHaveBeenCalled();
  });
});
