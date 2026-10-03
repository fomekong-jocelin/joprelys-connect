import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { AiConsultationApiService } from '../../consultation/ai-consultation-api.service';
import { AiVitalsApiService } from '../../consultation/ai-vitals-api.service';
import { Visit } from '../../visit/visit.models';
import { VisitApiService } from '../../visit/visit-api.service';
import { VisitVitalsFormModalComponent } from './visit-vitals-form-modal.component';

describe('VisitVitalsFormModalComponent', () => {
  let fixture: ComponentFixture<VisitVitalsFormModalComponent>;
  let component: VisitVitalsFormModalComponent;
  let mockVisitApi: { saveVitals: ReturnType<typeof vi.fn> };

  const visit = {
    id: 'visit-1',
    visitNumber: 'VIS-001',
    patientId: 'patient-1',
    patientName: 'Patient Test',
    patientDpu: 'DPU-001',
    reason: 'Fièvre',
    orientation: 'CONSULTATION',
    status: 'EN_COURS',
    createdAt: '2026-07-26T09:00:00Z',
  } as Visit;

  beforeEach(async () => {
    mockVisitApi = { saveVitals: vi.fn().mockReturnValue(of({ weight: 70, height: 175, bmi: 22.86 })) };

    await TestBed.configureTestingModule({
      imports: [VisitVitalsFormModalComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        { provide: VisitApiService, useValue: mockVisitApi },
        {
          provide: I18nService,
          useValue: {
            t: (key: string, fallback?: string) => fallback ?? key,
            currentLanguage: () => 'fr',
            locale: signal<'fr' | 'en'>('fr'),
            setLocale: vi.fn(),
          },
        },
        { provide: AiVitalsApiService, useValue: { analyzeText: vi.fn(), analyzeAudio: vi.fn() } },
        { provide: AiConsultationApiService, useValue: { synthesizeSpeech: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VisitVitalsFormModalComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('visit', visit);
  });

  it('should render the assistant declaratively inside the vitals modal', () => {
    fixture.detectChanges();

    const modalPanel = fixture.nativeElement.querySelector('[role="dialog"]') as HTMLElement;
    const assistantHost = modalPanel?.querySelector('app-smart-vitals-assistant') as HTMLElement;
    const assistantSection = assistantHost?.querySelector('section') as HTMLElement;

    expect(assistantHost).toBeTruthy();
    expect(modalPanel.contains(assistantHost)).toBe(true);
    expect(assistantSection.className).toContain('relative');
    expect(assistantSection.className).not.toContain('fixed');
    expect(component.currentVitalsForAssistant()).toEqual({});
  });

  it('should prefill a new measurement with the previous one', () => {
    fixture.componentRef.setInput('visit', { ...visit, vitals: { temperature: 38.5, pulse: 90 } });
    fixture.detectChanges();

    expect(component.vitalsTemp).toBe(38.5);
    expect(component.vitalsPulse).toBe(90);
  });

  it('should calculate BMI only when weight and height are provided', () => {
    component.vitalsWeight = 70;
    component.vitalsHeight = 175;
    expect(component.computedBmi).toBe(22.86);

    component.vitalsHeight = undefined;
    expect(component.computedBmi).toBeNull();
  });

  it('should validate pain scale bounds', () => {
    for (const value of [undefined, 0, 5, 10]) {
      component.vitalsPain = value;
      expect(component.isPainInvalid()).toBe(false);
    }
    for (const value of [-1, 11]) {
      component.vitalsPain = value;
      expect(component.isPainInvalid()).toBe(true);
    }
  });

  it('should block a diastolic pressure above the systolic one', () => {
    component.vitalsSystolic = 80;
    component.vitalsDiastolic = 120;
    expect(component.isBloodPressureInconsistent()).toBe(true);
    expect(component.isAnyVitalInvalid()).toBe(true);
  });

  it('should warn without blocking when glycemia looks like mmol/L', () => {
    component.vitalsGlycemia = 5.5;
    expect(component.isGlycemiaUnitSuspicious()).toBe(true);
    expect(component.isAnyVitalInvalid()).toBe(false);
  });

  it('should save the measurement with pain scale and emit it', () => {
    const saved = vi.fn();
    component.saved.subscribe(saved);
    component.vitalsWeight = 70;
    component.vitalsHeight = 175;
    component.vitalsPain = 6;

    component.submitVitals();

    expect(mockVisitApi.saveVitals).toHaveBeenCalledWith('visit-1', expect.objectContaining({
      weight: 70,
      height: 175,
      painScale: 6,
    }));
    expect(saved).toHaveBeenCalled();
  });
});
