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

  it('should apply the current safe draft before leaving realtime', () => {
    component.session.set({
      ...activeSession(),
      draft: { symptoms: 'Toux sèche depuis trois jours' },
    });
    const applied = vi.fn();
    component.applyDraft.subscribe(applied);

    component.finishRealtime();

    expect(applied).toHaveBeenCalledWith({ symptoms: 'Toux sèche depuis trois jours' });
    expect(component.conversationMode()).toBe(false);
  });

  it('should keep an uncertain realtime transcript editable instead of analyzing it immediately', () => {
    component.conversationMode.set(true);
    const analyze = vi.spyOn(component, 'analyzeTranscript');

    component.finishRealtimeTranscription({
      sessionId: 'session-1',
      transcript: 'Le patient nie toute fièvre',
      status: 'PENDING_REVIEW',
      expiresAt: '2026-07-26T22:00:00Z',
    });

    expect(analyze).not.toHaveBeenCalled();
    expect(component.session()?.pendingTranscript).toBe('Le patient nie toute fièvre');
  });

  it('should block classic recording while a clarification is pending', () => {
    component.session.set({
      ...activeSession(),
      clarifications: [{ status: 'PENDING' }],
    } as unknown as AiSessionResponse);

    expect(component.recordingBlocked()).toBe(true);
  });

  it('should send a non-empty audio container to transcription even when browser VAD is uncertain', () => {
    (component as any).handleClassicCapture({
      audio: new Blob(['encoded-silence'], { type: 'audio/webm' }),
      hasSpeech: false,
    });

    expect(api.transcribeAudio).toHaveBeenCalledTimes(1);
    expect(component.session()?.pendingTranscript).toBe('Patient sans fièvre');
    expect(component.session()?.transcriptStatus).toBe('PENDING_REVIEW');
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

  it('should preserve newer physician text and existing prescription when applying an older AI draft', () => {
    component.session.set(null);
    component.currentDraft = {
      symptoms: 'Douleur abdominale',
      prescription: [{ drugName: 'Paracétamol', dosage: '1 g' }],
      exams: ['NFS'],
      vitals: { temperature: 37.2 },
    };
    component.startSession();

    component.currentDraft = {
      symptoms: 'Douleur abdominale irradiant en fosse iliaque droite',
      prescription: [
        { drugName: 'Paracétamol', dosage: '1 g' },
        { drugName: 'Amoxicilline', dosage: '500 mg' },
      ],
      exams: ['NFS', 'Créatinine'],
      vitals: { temperature: 37.2 },
    };
    component.session.set({
      ...activeSession(),
      draft: {
        symptoms: 'Douleur abdominale avec nausées',
        prescription: JSON.stringify([
          { drugName: 'Paracétamol', dosage: '1 g' },
          { drugName: 'Spasfon', dosage: '80 mg' },
        ]),
        labOrders: JSON.stringify(['NFS', 'CRP']),
        vitals: JSON.stringify({ temperature: 38.4 }),
      },
    });
    const emitted = vi.fn();
    component.applyDraft.subscribe(emitted);

    component.applyCurrentDraft();

    expect(emitted).toHaveBeenCalledTimes(1);
    const safeDraft = emitted.mock.calls[0][0];
    expect(safeDraft.symptoms).toBeUndefined();
    expect(JSON.parse(safeDraft.prescription)).toEqual([
      { drugName: 'Paracétamol', dosage: '1 g' },
      { drugName: 'Amoxicilline', dosage: '500 mg' },
      expect.objectContaining({ drugName: 'Spasfon', dosage: '80 mg' }),
    ]);
    expect(JSON.parse(safeDraft.labOrders)).toEqual(['NFS', 'Créatinine', 'CRP']);
    expect(safeDraft.vitals).toBeUndefined();
    expect(component.errorMessage()).toContain('saisies plus récentes');
    expect(component.vitalsWarning()).toContain('constantes');
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
