import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { AiConsultationApiService, AiSessionResponse } from './ai-consultation-api.service';
import { ClassicVoiceRecorderService } from './classic-voice-recorder.service';
import {
  RealtimeClinicalIntakeAck,
  RealtimeClinicalIntakeApiService,
} from './realtime-clinical-intake-api.service';
import { VoiceAssistantPanelComponent } from './voice-assistant-panel.component';

describe('VoiceAssistantPanelComponent progressive consultation flow', () => {
  let fixture: ComponentFixture<VoiceAssistantPanelComponent>;
  let component: VoiceAssistantPanelComponent;
  let api: {
    startSession: ReturnType<typeof vi.fn>;
    rebuildCapture: ReturnType<typeof vi.fn>;
  };
  let intake: {
    list: ReturnType<typeof vi.fn>;
    correct: ReturnType<typeof vi.fn>;
    captureDictation: ReturnType<typeof vi.fn>;
  };
  let recorder: {
    supported: boolean;
    dispose: ReturnType<typeof vi.fn>;
    start: ReturnType<typeof vi.fn>;
    stop: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    api = {
      startSession: vi.fn().mockReturnValue(of(activeSession())),
      rebuildCapture: vi.fn().mockReturnValue(of({
        ...activeSession(),
        draft: { symptoms: 'Toux sèche depuis trois jours' },
      })),
    };
    intake = {
      list: vi.fn().mockReturnValue(of([])),
      correct: vi.fn(),
      captureDictation: vi.fn(),
    };
    recorder = {
      supported: true,
      dispose: vi.fn(),
      start: vi.fn(),
      stop: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [VoiceAssistantPanelComponent],
      providers: [
        { provide: AiConsultationApiService, useValue: api },
        { provide: RealtimeClinicalIntakeApiService, useValue: intake },
        { provide: ClassicVoiceRecorderService, useValue: recorder },
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
  });

  afterEach(() => TestBed.resetTestingModule());

  it('restores unsaved durable transcript instead of showing an empty consultation', () => {
    intake.list.mockReturnValue(of([
      captureEntry('capture-1', 1, 'Le patient présente une céphalée sévère.'),
      captureEntry('capture-2', 2, 'Je prescris du paracétamol 1000 mg matin et soir pendant quatre jours.'),
    ]));

    component.ngOnInit();

    expect(component.stage()).toBe('TRANSCRIPT_REVIEW');
    expect(component.captureEntries()).toHaveLength(2);
    expect(component.captureEntries()[1].transcript).toContain('paracétamol');
  });

  it('starts realtime only after its transport session is initialized', () => {
    component.startRealtime();

    expect(api.startSession).toHaveBeenCalledTimes(1);
    expect(component.session()?.sessionId).toBe('session-1');
    expect(component.stage()).toBe('CAPTURE_REALTIME');
  });

  it('does not apply a draft when realtime recording ends', () => {
    const applied = vi.fn();
    component.applyDraft.subscribe(applied);
    intake.list.mockReturnValue(of([
      captureEntry('capture-1', 1, 'Patient sans fièvre.'),
    ]));

    component.finishRealtime();

    expect(applied).not.toHaveBeenCalled();
    expect(component.stage()).toBe('TRANSCRIPT_REVIEW');
  });

  it('persists dictation into the same durable capture ledger', () => {
    const persisted = captureEntry(
      'dictation-1',
      1,
      'Le patient présente une douleur abdominale depuis trois jours.',
    );
    intake.captureDictation.mockReturnValue(of(persisted));

    (component as any).handleClassicCapture({
      audio: new Blob(['recorded-audio'], { type: 'audio/webm' }),
      hasSpeech: false,
    });

    expect(intake.captureDictation).toHaveBeenCalledTimes(1);
    expect(component.captureEntries()).toEqual([persisted]);
    expect(component.session()?.pendingTranscript).toBeUndefined();
  });

  it('corrects the durable transcript instead of sending a new AI message', () => {
    const original = captureEntry('capture-1', 1, 'Il a mal au bra.');
    const corrected = {
      ...original,
      transcript: 'Il a mal au bras.',
      originalTranscript: original.transcript,
      correctionCount: 1,
      correctedAt: '2026-07-29T01:00:10Z',
      reviewRequired: false,
    };
    component.captureEntries.set([original]);
    intake.correct.mockReturnValue(of(corrected));

    component.correctCapture({ id: original.id, transcript: corrected.transcript });

    expect(intake.correct).toHaveBeenCalledWith('visit-1', original.id, 'Il a mal au bras.');
    expect(component.captureEntries()[0].transcript).toBe('Il a mal au bras.');
    expect(component.captureEntries()[0].correctionCount).toBe(1);
  });

  it('generates one report from the complete durable capture before filling the form', () => {
    component.captureEntries.set([
      captureEntry('capture-1', 1, 'Le patient présente une céphalée sévère.'),
      captureEntry('capture-2', 2, 'Paracétamol 1000 mg matin et soir pendant quatre jours.'),
    ]);

    component.generateReport();

    expect(api.rebuildCapture).toHaveBeenCalledTimes(1);
    expect(component.stage()).toBe('REPORT_REVIEW');
    expect(component.session()?.draft.symptoms).toBe('Toux sèche depuis trois jours');
  });

  it('shows the clinical form only after an explicit report validation', () => {
    component.session.set({
      ...activeSession(),
      draft: { symptoms: 'Toux sèche depuis trois jours' },
    });
    const applied = vi.fn();
    const ready = vi.fn();
    component.applyDraft.subscribe(applied);
    component.formReadyChange.subscribe(ready);

    component.applyCurrentDraft();

    expect(applied).toHaveBeenCalledWith({ symptoms: 'Toux sèche depuis trois jours' });
    expect(component.stage()).toBe('FORM_READY');
    expect(ready).toHaveBeenCalledWith(true);
  });

  it('allows explicit manual entry without inventing an AI draft', () => {
    const applied = vi.fn();
    component.applyDraft.subscribe(applied);

    component.openManualForm();

    expect(applied).toHaveBeenCalledWith({});
    expect(component.stage()).toBe('FORM_READY');
  });

  it('preserves newer physician text and existing prescription when applying a rebuilt report', () => {
    component.currentDraft = {
      symptoms: 'Douleur abdominale',
      prescription: [{ drugName: 'Paracétamol', dosage: '1 g' }],
      exams: ['NFS'],
      vitals: { temperature: 37.2 },
    };
    (component as any).sessionBaseDraft = {
      symptoms: 'Douleur abdominale',
      prescription: JSON.stringify([{ drugName: 'Paracétamol', dosage: '1 g' }]),
      labOrders: JSON.stringify(['NFS']),
      vitals: JSON.stringify({ temperature: 37.2 }),
    };
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
  });

  function captureEntry(id: string, sequence: number, transcript: string): RealtimeClinicalIntakeAck {
    return {
      id,
      visitId: 'visit-1',
      source: 'CONSULTATION',
      sequence,
      eventId: `event-${sequence}`,
      itemId: null,
      transcript,
      originalTranscript: null,
      confidence: 0.95,
      reviewRequired: false,
      correctionCount: 0,
      correctedAt: null,
      captureStatus: 'PENDING',
      receivedAt: `2026-07-29T01:00:0${sequence}Z`,
    };
  }

  function activeSession(): AiSessionResponse {
    return {
      sessionId: 'session-1',
      visitId: 'visit-1',
      status: 'ACTIVE',
      expiresAt: '2026-07-29T02:00:00Z',
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
