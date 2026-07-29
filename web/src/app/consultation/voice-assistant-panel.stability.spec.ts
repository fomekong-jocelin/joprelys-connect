import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { AiConsultationApiService } from './ai-consultation-api.service';
import { ClassicVoiceRecorderService } from './classic-voice-recorder.service';
import {
  RealtimeClinicalIntakeAck,
  RealtimeClinicalIntakeApiService,
} from './realtime-clinical-intake-api.service';
import { VoiceAssistantPanelComponent } from './voice-assistant-panel.component';

describe('VoiceAssistantPanelComponent durable capture stability', () => {
  let aiApi: {
    startSession: ReturnType<typeof vi.fn>;
    rebuildCapture: ReturnType<typeof vi.fn>;
  };
  let intakeApi: {
    list: ReturnType<typeof vi.fn>;
    captureDictation: ReturnType<typeof vi.fn>;
    correct: ReturnType<typeof vi.fn>;
  };
  let recorder: {
    supported: boolean;
    dispose: ReturnType<typeof vi.fn>;
    start: ReturnType<typeof vi.fn>;
    stop: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    aiApi = {
      startSession: vi.fn().mockReturnValue(of({
        sessionId: 'session-1',
        visitId: 'visit-1',
        status: 'ACTIVE',
        expiresAt: '2026-07-29T20:00:00Z',
        draft: {},
        transcript: null,
        pendingTranscript: null,
        transcriptStatus: 'NONE',
        conversation: [],
        clarifications: [],
        revisions: [],
        assistantMessage: null,
        needsClarification: false,
      })),
      rebuildCapture: vi.fn(),
    };
    intakeApi = {
      list: vi.fn().mockReturnValue(of([])),
      captureDictation: vi.fn(),
      correct: vi.fn(),
    };
    recorder = {
      supported: true,
      dispose: vi.fn(),
      start: vi.fn(),
      stop: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: AiConsultationApiService, useValue: aiApi },
        { provide: RealtimeClinicalIntakeApiService, useValue: intakeApi },
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

  afterEach(() => TestBed.resetTestingModule());

  it('sends every non-empty dictation to durable capture even if browser VAD says no speech', () => {
    const component = TestBed.runInInjectionContext(() => new VoiceAssistantPanelComponent());
    component.visitId = 'visit-1';
    const entry = captureEntry('dictation-1', 'Le patient présente une douleur abdominale depuis trois jours.');
    intakeApi.captureDictation.mockReturnValue(of(entry));

    (component as any).handleClassicCapture({
      audio: new Blob(['recorded-audio'], { type: 'audio/webm' }),
      hasSpeech: false,
    });

    expect(intakeApi.captureDictation).toHaveBeenCalledTimes(1);
    expect(component.captureEntries()).toEqual([entry]);
  });

  it('loads durable recovery state once instead of polling a transient AI session', () => {
    const component = TestBed.runInInjectionContext(() => new VoiceAssistantPanelComponent());
    component.visitId = 'visit-1';
    intakeApi.list.mockReturnValue(of([
      captureEntry('capture-1', 'Patient sans fièvre.'),
    ]));

    component.ngOnInit();

    expect(intakeApi.list).toHaveBeenCalledTimes(1);
    expect(component.stage()).toBe('TRANSCRIPT_REVIEW');
    expect(aiApi.startSession).not.toHaveBeenCalled();
    component.ngOnDestroy();
  });

  function captureEntry(id: string, transcript: string): RealtimeClinicalIntakeAck {
    return {
      id,
      visitId: 'visit-1',
      source: 'CONSULTATION',
      sequence: 1,
      eventId: 'event-1',
      itemId: null,
      transcript,
      originalTranscript: null,
      confidence: 0.9,
      reviewRequired: false,
      correctionCount: 0,
      correctedAt: null,
      captureStatus: 'PENDING',
      receivedAt: '2026-07-29T10:00:00Z',
    };
  }
});
