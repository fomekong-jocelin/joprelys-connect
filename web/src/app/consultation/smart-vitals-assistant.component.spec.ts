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

    expect(vitalsApi.analyzeText).toHaveBeenCalledWith('visit-1', corrected.transcript, 'fr', {});
    expect(component.lastTranscript()).toBe(corrected.transcript);
    expect(component.proposalApplied()).toBe(false);
  });

  it('uses the shared listening surface while classic vitals dictation records', () => {
    component.recording.set(true);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-voice-listening-surface')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Écoute en cours... Parlez naturellement');
  });

  it('starts in manual mode and keeps text entry in its own labeled textarea', () => {
    expect(component.inputMode()).toBe('manual');
    expect(fixture.nativeElement.querySelector('input[value="manual"]').checked).toBe(true);
    expect(fixture.nativeElement.querySelector('label[for="vitals-text-input"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('textarea#vitals-text-input')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="start-vitals-realtime"]')).toBeNull();
    expect(component.realtimeEnabled()).toBe(false);
  });

  it('selects continuous listening without opening the microphone', () => {
    const enable = vi.spyOn(component, 'enableRealtime');
    fixture.nativeElement.querySelector('input[value="realtime"]').click();
    fixture.detectChanges();
    expect(component.inputMode()).toBe('realtime');
    expect(component.realtimeEnabled()).toBe(false);
    expect(enable).not.toHaveBeenCalled();
    fixture.nativeElement.querySelector('[data-testid="start-vitals-realtime"]').click();
    expect(enable).toHaveBeenCalledTimes(1);
    expect(component.realtimeEnabled()).toBe(true);
  });

  it('keeps unavailable dictation disabled and manual fields available', () => {
    expect(fixture.nativeElement.querySelector('input[value="dictation"]').disabled).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('vitals.entry.dictationUnavailable');
    expect(fixture.nativeElement.querySelector('input[value="manual"]').disabled).toBe(false);
  });

  it('does not apply an earlier proposal while a new analysis is running', () => {
    const emit = vi.fn();
    component.proposed.subscribe(emit);
    component.handleProposal({
      transcript: 'Test',
      vitals: { spo2: 98 },
      assistantMessage: '',
      needsConfirmation: false,
      confirmationReason: '',
    });
    component.busy.set(true);
    component.applyCurrentProposal();
    expect(emit).not.toHaveBeenCalled();
  });

  it('returns to manual mode and stops continuous listening when reduced', () => {
    component.enableRealtime();
    component.toggleExpanded();
    expect(component.inputMode()).toBe('manual');
    expect(component.realtimeEnabled()).toBe(false);
  });

  it('reports capture and analysis activity without saving clinical values', () => {
    const active = vi.fn();
    component.activityChange.subscribe(active);
    component.busy.set(true);
    fixture.detectChanges();
    expect(active).toHaveBeenLastCalledWith(true);
    component.busy.set(false);
    fixture.detectChanges();
    expect(active).toHaveBeenLastCalledWith(false);
  });

  it.each(['destroy', 'collapse'] as const)(
    'avoids duplicate microphone requests and releases a stream granted after %s',
    async (action) => {
      const mediaDescriptor = Object.getOwnPropertyDescriptor(navigator, 'mediaDevices');
      let grant!: (stream: MediaStream) => void;
      const getUserMedia = vi.fn().mockReturnValue(
        new Promise<MediaStream>((resolve) => {
          grant = resolve;
        }),
      );
      Object.defineProperty(navigator, 'mediaDevices', {
        configurable: true,
        value: { getUserMedia },
      });
      Object.defineProperty(component, 'mediaRecorderSupported', { value: true });
      const stop = vi.fn();
      try {
        component.selectMode('dictation');
        component.toggleRecording();
        component.toggleRecording();
        expect(getUserMedia).toHaveBeenCalledTimes(1);
        expect(component.recordingStarting()).toBe(true);
        if (action === 'destroy') fixture.destroy();
        else component.toggleExpanded();
        grant({ getTracks: () => [{ stop }] } as unknown as MediaStream);
        await Promise.resolve();
        expect(stop).toHaveBeenCalledTimes(1);
        expect(component.recording()).toBe(false);
        expect(vitalsApi.analyzeAudio).not.toHaveBeenCalled();
      } finally {
        if (mediaDescriptor) Object.defineProperty(navigator, 'mediaDevices', mediaDescriptor);
        else Reflect.deleteProperty(navigator, 'mediaDevices');
      }
    },
  );

  it('selects supported dictation without starting it until the explicit action', () => {
    Object.defineProperty(component, 'mediaRecorderSupported', { value: true });
    fixture.detectChanges();
    const record = vi.spyOn(component, 'toggleRecording').mockImplementation(() => undefined);
    fixture.nativeElement.querySelector('input[value="dictation"]').click();
    fixture.detectChanges();
    expect(component.inputMode()).toBe('dictation');
    expect(record).not.toHaveBeenCalled();
    fixture.nativeElement.querySelector('[data-testid="start-vitals-dictation"]').click();
    expect(record).toHaveBeenCalledOnce();
  });
});
