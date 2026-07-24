import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { EmergencyApiService } from '../../emergency/emergency-api.service';
import { EmergencyRecord } from '../../emergency/emergency.models';
import { PatientEmergencyContextComponent } from './patient-emergency-context.component';

describe('PatientEmergencyContextComponent disclosure', () => {
  let fixture: ComponentFixture<PatientEmergencyContextComponent>;
  let emergencyApi: { getPatientEmergencies: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    emergencyApi = { getPatientEmergencies: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [PatientEmergencyContextComponent],
      providers: [
        {
          provide: I18nService,
          useValue: {
            t: (key: string, fallback?: string) => fallback ?? key,
            locale: signal<'fr' | 'en'>('fr'),
          },
        },
        { provide: EmergencyApiService, useValue: emergencyApi },
      ],
    }).compileComponents();
  });

  afterEach(() => fixture?.destroy());

  it('should keep historical emergency context folded when no active emergency exists', () => {
    emergencyApi.getPatientEmergencies.mockReturnValue(of([]));
    fixture = TestBed.createComponent(PatientEmergencyContextComponent);
    fixture.componentRef.setInput('patientId', 'patient-1');
    fixture.detectChanges();

    expect(fixture.componentInstance.expanded()).toBe(false);
    expect(fixture.nativeElement.querySelector('button')?.getAttribute('aria-expanded')).toBe('false');
  });

  it('should automatically reveal an active emergency because it is safety-critical', () => {
    const activeEmergency: EmergencyRecord = {
      id: 'emergency-1',
      organizationId: 'organization-1',
      patientId: 'patient-1',
      patientName: 'Patient Test',
      globalPatientNumber: 'DPU-TEST-001',
      localPatientNumber: 'PAT-TEST-001',
      arrivalMode: 'WALK_IN',
      triageLevel: 'GREEN',
      hemodynamicStatus: 'STABLE',
      chiefComplaint: 'Céphalée',
      thirdPartyRecorded: false,
      resuscitationLogs: [],
      createdAt: '2026-07-24T12:00:00Z',
      updatedAt: '2026-07-24T12:00:00Z',
    };
    emergencyApi.getPatientEmergencies.mockReturnValue(of([activeEmergency]));
    fixture = TestBed.createComponent(PatientEmergencyContextComponent);
    fixture.componentRef.setInput('patientId', 'patient-1');
    fixture.detectChanges();

    expect(fixture.componentInstance.expanded()).toBe(true);
  });
});
