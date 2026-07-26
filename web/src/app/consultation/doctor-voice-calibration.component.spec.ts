import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { AmbientAudioCaptureService, AmbientCaptureState } from './ambient-audio-capture.service';
import {
  DoctorVoiceCalibrationComponent,
} from './doctor-voice-calibration.component';
import {
  DoctorVoiceCalibrationService,
  DoctorVoiceCalibrationState,
} from './doctor-voice-calibration.service';

describe('DoctorVoiceCalibrationComponent', () => {
  let fixture: ComponentFixture<DoctorVoiceCalibrationComponent>;
  let ambientState: BehaviorSubject<AmbientCaptureState>;
  let calibrationState: BehaviorSubject<DoctorVoiceCalibrationState>;
  let ambient: {
    state$: BehaviorSubject<AmbientCaptureState>;
    mediaStreamForVisit: ReturnType<typeof vi.fn>;
  };
  let calibration: {
    state$: BehaviorSubject<DoctorVoiceCalibrationState>;
    state: ReturnType<typeof vi.fn>;
    calibrate: ReturnType<typeof vi.fn>;
    clear: ReturnType<typeof vi.fn>;
  };
  const stream = { getAudioTracks: () => [] } as unknown as MediaStream;

  beforeEach(async () => {
    ambientState = new BehaviorSubject<AmbientCaptureState>({
      supported: true,
      active: false,
      starting: false,
      recovering: false,
      pendingChunks: 0,
      pendingBytes: 0,
      uploading: false,
      online: true,
      storagePressure: false,
      lastError: null,
    });
    calibrationState = new BehaviorSubject<DoctorVoiceCalibrationState>({
      status: 'idle',
      visitId: null,
      progress: 0,
      error: null,
      calibratedAt: null,
    });
    ambient = {
      state$: ambientState,
      mediaStreamForVisit: vi.fn().mockReturnValue(null),
    };
    calibration = {
      state$: calibrationState,
      state: vi.fn().mockImplementation(() => calibrationState.value),
      calibrate: vi.fn().mockResolvedValue(new ArrayBuffer(44)),
      clear: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [DoctorVoiceCalibrationComponent],
      providers: [
        { provide: AmbientAudioCaptureService, useValue: ambient },
        { provide: DoctorVoiceCalibrationService, useValue: calibration },
        {
          provide: I18nService,
          useValue: { t: (_key: string, fallback?: string) => fallback ?? _key },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DoctorVoiceCalibrationComponent);
    fixture.componentRef.setInput('visitId', 'visit-1');
    fixture.detectChanges();
  });

  afterEach(() => TestBed.resetTestingModule());

  it('should keep calibration disabled while the ambient capture is inactive', () => {
    const button = calibrationButton();

    expect(button.disabled).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Activez d’abord la capture Realtime sécurisée');
  });

  it('should pass the exact existing ambient MediaStream to calibration', async () => {
    ambient.mediaStreamForVisit.mockReturnValue(stream);
    ambientState.next({ ...ambientState.value, active: true });
    fixture.detectChanges();

    calibrationButton().click();
    await fixture.whenStable();

    expect(calibration.calibrate).toHaveBeenCalledWith('visit-1', stream);
  });

  it('should display calibrated state and allow clearing only that visit', () => {
    ambient.mediaStreamForVisit.mockReturnValue(stream);
    ambientState.next({ ...ambientState.value, active: true });
    calibrationState.next({
      status: 'calibrated',
      visitId: 'visit-1',
      progress: 1,
      error: null,
      calibratedAt: Date.now(),
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Voix médecin calibrée');
    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    const clear = buttons.find(button => button.textContent?.includes('Effacer'));
    expect(clear).toBeTruthy();
    clear!.click();
    expect(calibration.clear).toHaveBeenCalledWith('visit-1');
  });

  function calibrationButton(): HTMLButtonElement {
    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    return buttons.at(-1)!;
  }
});
