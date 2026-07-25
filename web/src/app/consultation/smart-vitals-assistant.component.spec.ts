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
});
