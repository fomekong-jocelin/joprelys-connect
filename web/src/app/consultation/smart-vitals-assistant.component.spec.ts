import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { AiVitalsApiService } from './ai-vitals-api.service';
import { SmartVitalsAssistantComponent } from './smart-vitals-assistant.component';

describe('SmartVitalsAssistantComponent', () => {
  let fixture: ComponentFixture<SmartVitalsAssistantComponent>;
  let component: SmartVitalsAssistantComponent;
  let vitalsApi: { analyzeText: ReturnType<typeof vi.fn>; analyzeAudio: ReturnType<typeof vi.fn> };
  const originalMatchMedia = window.matchMedia;

  beforeEach(async () => {
    Object.defineProperty(window, 'matchMedia', {
      configurable: true,
      value: vi.fn().mockImplementation((query: string) => ({
        matches: query === '(min-width: 640px)',
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    });
    vitalsApi = {
      analyzeText: vi.fn(),
      analyzeAudio: vi.fn(),
    };
    await TestBed.configureTestingModule({
      imports: [SmartVitalsAssistantComponent],
      providers: [
        provideHttpClient(),
        { provide: AiVitalsApiService, useValue: vitalsApi },
        {
          provide: I18nService,
          useValue: {
            t: (key: string, fallback?: string) => fallback ?? key,
            currentLanguage: () => 'fr',
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SmartVitalsAssistantComponent);
    component = fixture.componentInstance;
    component.visitId = 'visit-1';
    fixture.detectChanges();
  });

  afterEach(() => {
    Object.defineProperty(window, 'matchMedia', {
      configurable: true,
      value: originalMatchMedia,
    });
  });

  it('should start collapsed on a mobile viewport', () => {
    fixture.destroy();
    Object.defineProperty(window, 'matchMedia', {
      configurable: true,
      value: vi.fn().mockReturnValue({
        matches: false,
        media: '(min-width: 640px)',
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      }),
    });

    const mobileFixture = TestBed.createComponent(SmartVitalsAssistantComponent);
    mobileFixture.componentInstance.visitId = 'visit-mobile';
    mobileFixture.detectChanges();

    expect(mobileFixture.componentInstance.expanded()).toBe(false);
    expect(mobileFixture.nativeElement.querySelector('input')).toBeNull();
    mobileFixture.destroy();
  });

  it('should require explicit consent before enabling realtime listening', () => {
    expect(component.realtimeEnabled()).toBe(false);
    expect(component.realtimeActive()).toBe(false);

    component.enableRealtime();

    expect(component.realtimeEnabled()).toBe(true);
  });

  it('should disable continuous listening when the assistant is collapsed', () => {
    component.enableRealtime();
    component.realtimeActive.set(true);

    component.toggleExpanded();

    expect(component.expanded()).toBe(false);
    expect(component.realtimeEnabled()).toBe(false);
    expect(component.realtimeActive()).toBe(false);
  });

  it('should keep detected vitals as a proposal until the clinician explicitly applies them', () => {
    const proposal = {
      transcript: 'Température 38,4, saturation 96',
      vitals: { temperature: 38.4, spo2: 96 },
      assistantMessage: 'Deux constantes détectées. Vérifiez-les avant l’enregistrement.',
      needsConfirmation: false,
      confirmationReason: '',
    };
    vitalsApi.analyzeText.mockReturnValue(of(proposal));
    const emitted = vi.fn();
    component.proposed.subscribe(emitted);

    component.textInput = proposal.transcript;
    component.sendText();

    expect(vitalsApi.analyzeText).toHaveBeenCalledWith('visit-1', proposal.transcript, 'fr', {});
    expect(emitted).not.toHaveBeenCalled();
    expect(component.lastTranscript()).toBe(proposal.transcript);
    expect(component.proposalEntries()).toHaveLength(2);
    expect(component.proposalApplied()).toBe(false);

    component.applyCurrentProposal();

    expect(emitted).toHaveBeenCalledTimes(1);
    expect(emitted).toHaveBeenCalledWith(proposal);
    expect(component.proposalApplied()).toBe(true);

    component.applyCurrentProposal();
    expect(emitted).toHaveBeenCalledTimes(1);
  });

  it('should not emit an empty proposal when clarification is required', () => {
    vitalsApi.analyzeText.mockReturnValue(
      of({
        transcript: 'Tension douze sur huit',
        vitals: {},
        assistantMessage: 'Pouvez-vous confirmer la tension en mmHg ?',
        needsConfirmation: true,
        confirmationReason: 'Valeur abrégée ambiguë.',
      }),
    );
    const emitted = vi.fn();
    component.proposed.subscribe(emitted);

    component.textInput = 'Tension douze sur huit';
    component.sendText();
    component.applyCurrentProposal();

    expect(emitted).not.toHaveBeenCalled();
    expect(component.needsConfirmation()).toBe(true);
  });

  it('should keep a realtime proposal pending without triggering legacy TTS or parent mutation', () => {
    const proposal = {
      transcript: 'Saturation 97',
      vitals: { spo2: 97 },
      assistantMessage: 'Saturation détectée.',
      needsConfirmation: false,
      confirmationReason: '',
    };
    const emitted = vi.fn();
    component.proposed.subscribe(emitted);

    component.handleProposal(proposal);

    expect(emitted).not.toHaveBeenCalled();
    expect(component.lastProposal()).toEqual(proposal);

    component.applyCurrentProposal();
    expect(emitted).toHaveBeenCalledWith(proposal);
  });

  it('should let the clinician correct the transcript and analyze the corrected sentence again', () => {
    const initial = {
      transcript: 'Température trente huit quatre',
      vitals: { temperature: 38.4 },
      assistantMessage: 'Température détectée.',
      needsConfirmation: false,
      confirmationReason: '',
    };
    const corrected = { ...initial, transcript: 'Température 38,4 degrés' };
    vitalsApi.analyzeText.mockReturnValue(of(corrected));
    component.handleProposal(initial);

    component.correctionText = corrected.transcript;
    component.reanalyzeCorrection();

    expect(vitalsApi.analyzeText).toHaveBeenCalledWith(
      'visit-1',
      corrected.transcript,
      'fr',
      {},
    );
    expect(component.lastTranscript()).toBe(corrected.transcript);
    expect(component.proposalApplied()).toBe(false);
  });

  it('uses the shared listening surface while classic vitals dictation records', () => {
    component.recording.set(true);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-voice-listening-surface')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain(
      'Écoute en cours... Parlez naturellement',
    );
  });
});
