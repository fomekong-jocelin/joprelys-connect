import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NEVER, of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { AiConsultationApiService } from './ai-consultation-api.service';
import { AiVitalsApiService } from './ai-vitals-api.service';
import { SmartVitalsAssistantComponent } from './smart-vitals-assistant.component';

describe('SmartVitalsAssistantComponent', () => {
  let fixture: ComponentFixture<SmartVitalsAssistantComponent>;
  let component: SmartVitalsAssistantComponent;
  let vitalsApi: { analyzeText: ReturnType<typeof vi.fn>; analyzeAudio: ReturnType<typeof vi.fn> };
  let voiceApi: { synthesizeSpeech: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    vitalsApi = {
      analyzeText: vi.fn(),
      analyzeAudio: vi.fn(),
    };
    voiceApi = {
      synthesizeSpeech: vi.fn().mockReturnValue(NEVER),
    };

    await TestBed.configureTestingModule({
      imports: [SmartVitalsAssistantComponent],
      providers: [
        provideHttpClient(),
        { provide: AiVitalsApiService, useValue: vitalsApi },
        { provide: AiConsultationApiService, useValue: voiceApi },
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

  it('should prefill detected vitals without saving them', () => {
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
    expect(emitted).toHaveBeenCalledWith(proposal);
    expect(component.lastTranscript()).toBe(proposal.transcript);
    expect(component.proposalEntries()).toHaveLength(2);
  });

  it('should not emit an empty proposal when clarification is required', () => {
    vitalsApi.analyzeText.mockReturnValue(of({
      transcript: 'Tension douze sur huit',
      vitals: {},
      assistantMessage: 'Pouvez-vous confirmer la tension en mmHg ?',
      needsConfirmation: true,
      confirmationReason: 'Valeur abrégée ambiguë.',
    }));
    const emitted = vi.fn();
    component.proposed.subscribe(emitted);

    component.textInput = 'Tension douze sur huit';
    component.sendText();

    expect(emitted).not.toHaveBeenCalled();
    expect(component.needsConfirmation()).toBe(true);
  });

  it('should accept realtime proposals without triggering legacy TTS twice', () => {
    const proposal = {
      transcript: 'Saturation 97',
      vitals: { spo2: 97 },
      assistantMessage: 'Saturation détectée.',
      needsConfirmation: false,
      confirmationReason: '',
    };
    const emitted = vi.fn();
    component.proposed.subscribe(emitted);

    component.handleProposal(proposal, false);

    expect(emitted).toHaveBeenCalledWith(proposal);
    expect(voiceApi.synthesizeSpeech).not.toHaveBeenCalled();
  });
});
