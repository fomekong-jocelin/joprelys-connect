import { provideHttpClient } from '@angular/common/http';
import { Component, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { EMPTY, Subject, of, throwError } from 'rxjs';
import { LabOrderApiService } from '../clinic/lab/lab-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { VisitApiService } from '../visit/visit-api.service';
import { VitalsHistoryComponent } from '../visit/vitals-history.component';
import { AiConsultationApiService, AiSessionResponse } from './ai-consultation-api.service';
import { ClassicVoiceRecorderService } from './classic-voice-recorder.service';
import { ConsultationApiService } from './consultation-api.service';
import { ConsultationComponent } from './consultation.component';
import { Consultation } from './consultation.models';
import { RealtimeClinicalIntakeApiService } from './realtime-clinical-intake-api.service';
import { RealtimeVoiceControllerComponent } from './realtime-voice-controller.component';
import { VoiceAssistantPanelComponent } from './voice-assistant-panel.component';

@Component({ selector: 'app-shell', template: '<ng-content />' })
class ShellStub {}

@Component({ selector: 'app-vitals-history', template: '' })
class HistoryStub {
  @Input() visitId = '';
  @Input() refreshKey: unknown;
}

@Component({ selector: 'app-realtime-voice-controller', template: '' })
class RealtimeStub {
  @Input() visitId = '';
  @Input() session: unknown;
  @Input() enabled = false;
  @Input() blocked = false;
}

describe('Consultation workspace visibility with the actual assistant', () => {
  let fixture: ComponentFixture<ConsultationComponent>;
  let component: ConsultationComponent;
  let existing: Subject<Consultation>;
  let startSession: ReturnType<typeof vi.fn>;
  let recorderStart: ReturnType<typeof vi.fn>;
  let saveConsultation: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    existing = new Subject<Consultation>();
    startSession = vi.fn().mockReturnValue(of({ sessionId: 'test-session', draft: {} }));
    recorderStart = vi.fn().mockResolvedValue(undefined);
    saveConsultation = vi.fn();
    await TestBed.configureTestingModule({
      imports: [ConsultationComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'test-visit' } } },
        },
        {
          provide: VisitApiService,
          useValue: { getVitals: () => of(null), getById: () => of({}) },
        },
        { provide: LabOrderApiService, useValue: {} },
        {
          provide: ConsultationApiService,
          useValue: {
            getConsultation: () => existing,
            getPrescription: () => EMPTY,
            saveConsultation,
          },
        },
        { provide: AiConsultationApiService, useValue: { startSession } },
        { provide: RealtimeClinicalIntakeApiService, useValue: { list: () => of([]) } },
        {
          provide: ClassicVoiceRecorderService,
          useValue: { supported: true, dispose: vi.fn(), start: recorderStart, stop: vi.fn() },
        },
        {
          provide: I18nService,
          useValue: { t: (key: string) => key, currentLanguage: () => 'fr' },
        },
      ],
    })
      .overrideComponent(ConsultationComponent, {
        remove: { imports: [AppShellComponent, VitalsHistoryComponent] },
        add: { imports: [ShellStub, HistoryStub] },
      })
      .overrideComponent(VoiceAssistantPanelComponent, {
        remove: { imports: [RealtimeVoiceControllerComponent] },
        add: { imports: [RealtimeStub] },
      })
      .compileComponents();
    fixture = TestBed.createComponent(ConsultationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function assistant(): VoiceAssistantPanelComponent {
    return fixture.debugElement.query(By.directive(VoiceAssistantPanelComponent)).componentInstance;
  }

  it('opens a pristine manual form via the rendered mode action without announcing AI validation', () => {
    fixture.nativeElement.querySelector('input[value="manual"]').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    fixture.nativeElement.querySelector('[data-testid="start-consultation-mode"]').click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('form')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[formControlName="symptoms"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[formControlName="diagnosis"]')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('consultation.entry.formOpenTitle');
    expect(fixture.nativeElement.textContent).not.toContain('consultation.ai.formReadyTitle');
    expect(component.form.pristine).toBe(true);
    expect(component.form.invalid).toBe(true);
    expect(component.successMessage()).toBe('');
    expect(startSession).not.toHaveBeenCalled();
    expect(recorderStart).not.toHaveBeenCalled();
    component.onSave();
    expect(saveConsultation).not.toHaveBeenCalled();
  });

  it('shows a loaded consultation and its saved values even while pristine', () => {
    existing.next({
      id: 'test-note',
      symptoms: 'Observation de test',
      diagnosis: 'Conclusion de test',
    } as Consultation);
    fixture.detectChanges();
    expect(component.form.pristine).toBe(true);
    expect(fixture.nativeElement.querySelector('[formControlName="symptoms"]').value).toBe(
      'Observation de test',
    );
    expect(assistant().formReady).toBe(true);
    expect(fixture.nativeElement.textContent).not.toContain('consultation.ai.formReadyTitle');
  });

  it('opens fields and save actions only after explicit acceptance of the AI report', () => {
    const voice = assistant();
    voice.session.set({
      draft: { symptoms: 'Observation de test', diagnosis: 'Conclusion de test' },
    } as AiSessionResponse);
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    voice.applyCurrentDraft();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[formControlName="symptoms"]').value).toBe(
      'Observation de test',
    );
    expect(fixture.nativeElement.textContent).toContain('consultation.ai.formReadyTitle');
    expect(fixture.nativeElement.textContent).toContain('consultation.actions.saveDraft');
    expect(saveConsultation).not.toHaveBeenCalled();
  });

  it('preserves entered values through capture and reopening the form', () => {
    assistant().openManualForm();
    component.form.patchValue({
      symptoms: 'Observation conservée',
      diagnosis: 'Conclusion de test',
    });
    fixture.detectChanges();
    assistant().startRealtime();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    expect(component.form.get('symptoms')?.value).toBe('Observation conservée');
    const manualButton = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      (button) =>
        (button as HTMLButtonElement).textContent?.includes('consultation.entry.switchToManual'),
    ) as HTMLButtonElement;
    manualButton.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[formControlName="symptoms"]').value).toBe(
      'Observation conservée',
    );
  });

  it('keeps fields visible when realtime initialization fails', () => {
    assistant().openManualForm();
    fixture.detectChanges();
    startSession.mockReturnValue(throwError(() => new Error('Channel unavailable')));
    assistant().startRealtime();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('form')).not.toBeNull();
    expect(assistant().errorMessage()).not.toBe('');
    expect(component.formWorkspaceReady()).toBe(true);
  });

  it('starts dictation only after its explicit action', async () => {
    fixture.nativeElement.querySelector('input[value="dictation"]').click();
    fixture.detectChanges();
    expect(recorderStart).not.toHaveBeenCalled();
    fixture.nativeElement.querySelector('[data-testid="start-consultation-mode"]').click();
    await fixture.whenStable();
    expect(recorderStart).toHaveBeenCalledTimes(1);
    expect(startSession).not.toHaveBeenCalled();
    expect(assistant().stage()).toBe('CAPTURE_DICTATION');
    expect(assistant().recording()).toBe(true);
  });

  it('does not infer report validation or form visibility from prefilled text', () => {
    component.form.patchValue({ symptoms: 'Observation de test' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    expect(fixture.nativeElement.querySelector('app-consultation-entry-mode')).not.toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('consultation.ai.formReadyTitle');
  });

  it('prevents duplicate dictation starts while microphone permission is pending', async () => {
    let allowMicrophone!: () => void;
    recorderStart.mockReturnValue(
      new Promise<void>((resolve) => {
        allowMicrophone = resolve;
      }),
    );
    assistant().startEntryMode('dictation');
    assistant().toggleRecording();
    expect(recorderStart).toHaveBeenCalledTimes(1);
    expect(assistant().busy()).toBe(true);
    allowMicrophone();
    await fixture.whenStable();
    expect(assistant().busy()).toBe(false);
    expect(assistant().recording()).toBe(true);
  });

  it('allows the rendered manual fallback after microphone permission is denied', async () => {
    recorderStart.mockRejectedValue(new Error('Permission denied'));
    assistant().startEntryMode('dictation');
    await fixture.whenStable();
    fixture.detectChanges();
    const manualButton = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      (button) =>
        (button as HTMLButtonElement).textContent?.includes('consultation.entry.switchToManual'),
    ) as HTMLButtonElement;
    expect(manualButton.disabled).toBe(false);
    manualButton.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('form')).not.toBeNull();
    expect(saveConsultation).not.toHaveBeenCalled();
  });
});
