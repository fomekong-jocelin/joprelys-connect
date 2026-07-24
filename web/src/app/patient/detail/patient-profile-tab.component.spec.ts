import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { EmergencyApiService } from '../../emergency/emergency-api.service';
import { PatientDetailComponent } from '../patient-detail.component';
import { Patient } from '../patient.models';
import { PatientProfileTabComponent } from './patient-profile-tab.component';

describe('PatientProfileTabComponent design hierarchy', () => {
  let fixture: ComponentFixture<PatientProfileTabComponent>;

  const patient = {
    id: 'patient-1',
    fullName: 'Jocelin FOMEKONG',
    gender: 'MASCULIN',
    birthDate: '1987-06-29',
    city: 'Douala',
    status: 'ACTIVE',
    identityStatus: 'VERIFIED',
  } as Patient;

  const i18n = {
    t: (key: string) => key,
    locale: signal<'fr' | 'en'>('fr'),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PatientProfileTabComponent],
      providers: [
        { provide: I18nService, useValue: i18n },
        { provide: EmergencyApiService, useValue: { getPatientEmergencies: vi.fn().mockReturnValue(of([])) } },
        {
          provide: PatientDetailComponent,
          useValue: {
            patient: signal(patient),
            i18n,
            loadPatient: vi.fn(),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PatientProfileTabComponent);
    fixture.detectChanges();
  });

  afterEach(() => fixture.destroy());

  it('should use an explicit 24px design-system gap between profile sections', () => {
    const layout = fixture.nativeElement.querySelector('.grid.gap-6');

    expect(layout).not.toBeNull();
  });

  it('should keep longitudinal medical sections folded on the initial profile view', () => {
    expect(fixture.componentInstance.medicalExpanded()).toBe(false);
    expect(fixture.nativeElement.querySelector('app-patient-medical-info')).toBeNull();
  });

  it('should allow the user to expand medical sections explicitly', () => {
    fixture.componentInstance.toggleMedicalInformation();

    expect(fixture.componentInstance.medicalExpanded()).toBe(true);
  });
});
