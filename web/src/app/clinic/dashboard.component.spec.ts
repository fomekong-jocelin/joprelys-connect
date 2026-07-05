import { TestBed, ComponentFixture } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';
import { AuthTokenStorageService } from '../auth/auth-token-storage.service';
import { VisitApiService } from '../visit/visit-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let mockAuthToken: any;
  let mockVisitApi: any;
  let mockI18n: any;

  beforeEach(async () => {
    mockAuthToken = {
      session: signal({
        name: 'Jean Medecin',
        email: 'medecin@joprelys.local',
        role: 'MEDECIN',
        org: 'org-1'
      })
    };

    mockVisitApi = {
      getActiveVisits: vi.fn().mockReturnValue(of([])),
      closeVisit: vi.fn(),
      saveVitals: vi.fn().mockReturnValue(of({ weight: 70, height: 175, bmi: 22.86 }))
    };

    mockI18n = {
      t: vi.fn().mockImplementation((key) => key),
      locale: signal('fr')
    };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: AuthTokenStorageService, useValue: mockAuthToken },
        { provide: VisitApiService, useValue: mockVisitApi },
        { provide: I18nService, useValue: mockI18n }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load active visits on init if clinical role', () => {
    expect(mockVisitApi.getActiveVisits).toHaveBeenCalled();
    expect(component.isLoadingQueue()).toBe(false);
    expect(component.activeVisits()).toEqual([]);
  });

  it('should calculate BMI correctly when weight and height are provided', () => {
    component.vitalsWeight = 70;
    component.vitalsHeight = 175;
    expect(component.computedBmi).toBe(22.86); // 70 / 1.75^2 = 22.857 -> 22.86
  });

  it('should return null BMI if height or weight is missing', () => {
    component.vitalsWeight = undefined;
    component.vitalsHeight = 175;
    expect(component.computedBmi).toBeNull();

    component.vitalsWeight = 70;
    component.vitalsHeight = undefined;
    expect(component.computedBmi).toBeNull();
  });

  it('should save vitals for the selected active visit and reload the queue', () => {
    component.openVitalsModal({
      id: 'visit-1',
      visitNumber: 'VIS-001',
      patientId: 'patient-1',
      patientName: 'Patient Test',
      patientDpu: 'DPU-001',
      reason: 'Fièvre',
      orientation: 'Tri',
      status: 'EN_COURS',
      createdAt: '2026-07-02T08:00:00Z'
    });
    component.vitalsWeight = 70;
    component.vitalsHeight = 175;

    component.submitVitals();

    expect(mockVisitApi.saveVitals).toHaveBeenCalledWith('visit-1', expect.objectContaining({
      weight: 70,
      height: 175
    }));
    expect(component.showVitalsModal()).toBe(false);
    expect(component.selectedVisitForVitals()).toBeNull();
    expect(mockVisitApi.getActiveVisits).toHaveBeenCalledTimes(2);
  });

  it('should validate pain scale values correctly', () => {
    component.vitalsPain = undefined;
    expect(component.isPainInvalid()).toBe(false);

    component.vitalsPain = 0;
    expect(component.isPainInvalid()).toBe(false);

    component.vitalsPain = 5;
    expect(component.isPainInvalid()).toBe(false);

    component.vitalsPain = 10;
    expect(component.isPainInvalid()).toBe(false);

    component.vitalsPain = -1;
    expect(component.isPainInvalid()).toBe(true);

    component.vitalsPain = 11;
    expect(component.isPainInvalid()).toBe(true);
  });

  it('should submit pain scale with other vitals', () => {
    component.openVitalsModal({
      id: 'visit-1',
      visitNumber: 'VIS-001',
      patientId: 'patient-1',
      patientName: 'Patient Test',
      patientDpu: 'DPU-001',
      reason: 'Fièvre',
      orientation: 'Tri',
      status: 'EN_COURS',
      createdAt: '2026-07-02T08:00:00Z'
    });
    component.vitalsWeight = 70;
    component.vitalsHeight = 175;
    component.vitalsPain = 6;

    component.submitVitals();

    expect(mockVisitApi.saveVitals).toHaveBeenCalledWith('visit-1', expect.objectContaining({
      weight: 70,
      height: 175,
      painScale: 6
    }));
  });
});

