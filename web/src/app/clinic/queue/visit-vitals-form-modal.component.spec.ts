import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { SmartVitalsAssistantComponent } from '../../consultation/smart-vitals-assistant.component';
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
    mockVisitApi = {
      saveVitals: vi.fn().mockReturnValue(of({ weight: 70, height: 175, bmi: 22.86 })),
    };

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

    expect(mockVisitApi.saveVitals).toHaveBeenCalledWith(
      'visit-1',
      expect.objectContaining({
        weight: 70,
        height: 175,
        painScale: 6,
      }),
    );
    expect(saved).toHaveBeenCalled();
  });

  it('keeps patient header and saving actions outside the scrolling content', () => {
    fixture.detectChanges();
    const body = fixture.nativeElement.querySelector('[data-testid="vitals-scroll-body"]');
    expect(body.className).toContain('overflow-y-auto');
    expect(body.contains(fixture.nativeElement.querySelector('#visit-vitals-title'))).toBe(false);
    expect(
      body.contains(fixture.nativeElement.querySelector('[data-testid="vitals-actions"]')),
    ).toBe(false);
    for (const label of body.querySelectorAll('label[for^="visit-vitals-"]')) {
      expect(body.querySelector(`#${label.htmlFor}`)).not.toBeNull();
    }
    expect(body.querySelectorAll('label[for^="visit-vitals-"]')).toHaveLength(10);
  });

  it('waits for assistant capture or analysis to finish before saving', () => {
    fixture.detectChanges();
    const assistant = fixture.debugElement.query(By.directive(SmartVitalsAssistantComponent))
      .componentInstance as SmartVitalsAssistantComponent;
    assistant.activityChange.emit(true);
    component.submitVitals();
    expect(mockVisitApi.saveVitals).not.toHaveBeenCalled();
    assistant.activityChange.emit(false);
    component.vitalsTemp = 37.5;
    component.submitVitals();
    expect(mockVisitApi.saveVitals).toHaveBeenCalledOnce();
  });

  it('transfers proposed values without saving until final confirmation', () => {
    fixture.detectChanges();
    const assistant = fixture.debugElement.query(By.directive(SmartVitalsAssistantComponent))
      .componentInstance as SmartVitalsAssistantComponent;
    assistant.handleProposal({
      transcript: 'Test',
      vitals: { temperature: 37.5 },
      assistantMessage: '',
      needsConfirmation: false,
      confirmationReason: '',
    });
    expect(component.vitalsTemp).toBeUndefined();
    assistant.applyCurrentProposal();
    expect(component.vitalsTemp).toBe(37.5);
    expect(mockVisitApi.saveVitals).not.toHaveBeenCalled();
    component.submitVitals();
    expect(mockVisitApi.saveVitals).toHaveBeenCalledOnce();
  });

  it('preserves typed measurements when entry mode changes', () => {
    fixture.detectChanges();
    component.vitalsTemp = 37.5;
    const assistant = fixture.debugElement.query(By.directive(SmartVitalsAssistantComponent))
      .componentInstance as SmartVitalsAssistantComponent;
    assistant.selectMode('realtime');
    assistant.selectMode('manual');
    expect(component.vitalsTemp).toBe(37.5);
    expect(mockVisitApi.saveVitals).not.toHaveBeenCalled();
  });

  it('contains keyboard focus and closes with Escape', () => {
    fixture.detectChanges();
    const dialog: HTMLElement = fixture.nativeElement.querySelector('[role="dialog"]');
    const first: HTMLButtonElement = dialog.querySelector('button')!;
    first.focus();
    const backward = new KeyboardEvent('keydown', {
      key: 'Tab',
      shiftKey: true,
      bubbles: true,
      cancelable: true,
    });
    first.dispatchEvent(backward);
    expect(backward.defaultPrevented).toBe(true);
    expect(dialog.contains(document.activeElement)).toBe(true);
    const last = document.activeElement as HTMLElement;
    last.dispatchEvent(
      new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true }),
    );
    expect(document.activeElement).toBe(first);
    const closed = vi.fn();
    component.closed.subscribe(closed);
    dialog.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    expect(closed).toHaveBeenCalledOnce();
  });
});
