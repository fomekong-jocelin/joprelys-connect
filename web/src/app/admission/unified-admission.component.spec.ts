import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { EmergencyApiService } from '../emergency/emergency-api.service';
import { PatientApiService } from '../patient/patient-api.service';
import { Patient } from '../patient/patient.models';
import { VisitApiService } from '../visit/visit-api.service';
import { UnifiedAdmissionComponent } from './unified-admission.component';

describe('UnifiedAdmissionComponent', () => {
  let fixture: ComponentFixture<UnifiedAdmissionComponent>;
  let component: UnifiedAdmissionComponent;
  let emergencyApi: {
    create: ReturnType<typeof vi.fn>;
    createProvisionalAdmission: ReturnType<typeof vi.fn>;
  };

  const patient = {
    id: 'patient-1',
    organizationId: 'org-1',
    globalPatientNumber: 'DPU-001',
    localPatientNumber: 'PAT-001',
    fullName: 'Patient Test',
    displayName: 'Patient Test',
    gender: 'MASCULIN',
    birthDate: '1990-01-01',
    phone: '+237699000000',
    city: 'Douala',
    status: 'ACTIVE',
    createdAt: '2026-07-11T10:00:00Z',
    updatedAt: '2026-07-11T10:00:00Z',
  };

  const translations: Record<string, string> = {
    'admission.requiredThirdParty': 'Renseignez le nom, le téléphone et le lien de la personne ayant amené le patient.',
  };

  let visitApi: { create: ReturnType<typeof vi.fn>; getAdmissionOptions: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    visitApi = {
      create: vi.fn().mockReturnValue(of({ id: 'visit-1' })),
      getAdmissionOptions: vi.fn().mockReturnValue(of({
        services: ['Médecine générale', 'Pédiatrie'],
        practitioners: [
          { id: 'doctor-1', displayName: 'Dr Péd', role: 'MEDECIN', unitNames: ['Pédiatrie'] },
          { id: 'doctor-2', displayName: 'Dr MG', role: 'MEDECIN', unitNames: ['Médecine générale'] },
        ],
      })),
    };
    emergencyApi = {
      create: vi.fn().mockReturnValue(of({ id: 'emergency-1' })),
      createProvisionalAdmission: vi.fn().mockReturnValue(of({
        id: 'emergency-provisional-1',
        patientId: 'patient-provisional-1',
        patientName: 'URG-TEMP-20260711-000001',
      })),
    };

    await TestBed.configureTestingModule({
      imports: [UnifiedAdmissionComponent],
      providers: [
        {
          provide: I18nService,
          useValue: {
            locale: vi.fn().mockReturnValue('fr'),
            t: vi.fn((key: string, fallback?: string) => translations[key] ?? fallback ?? key),
          },
        },
        {
          provide: PatientApiService,
          useValue: {
            list: vi.fn().mockReturnValue(of([patient])),
            create: vi.fn().mockReturnValue(of(patient)),
          },
        },
        { provide: EmergencyApiService, useValue: emergencyApi },
        { provide: VisitApiService, useValue: visitApi },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UnifiedAdmissionComponent);
    fixture.componentRef.setInput('initialCarePath', 'EMERGENCY');
    component = fixture.componentInstance;
    fixture.detectChanges();
    component.form.patchValue({ patientId: patient.id });
  });

  it('guides the user through three steps', () => {
    expect(component.currentStep()).toBe(1);

    component.nextStep();
    expect(component.currentStep()).toBe(2);

    component.nextStep();
    expect(component.currentStep()).toBe(3);
  });

  it('requires third-party identity when arrival mode is accompanied', () => {
    component.nextStep();
    component.form.patchValue({ arrivalMode: 'ACCOMPANIED' });

    component.nextStep();

    expect(component.currentStep()).toBe(2);
    expect(component.error()).toContain('nom');
  });

  it('sends the third-party information with an existing-patient emergency', () => {
    component.nextStep();
    component.form.patchValue({
      arrivalMode: 'ACCOMPANIED',
      thirdPartyName: 'Paul Tamo',
      thirdPartyPhone: '+237699000111',
      thirdPartyRelationship: 'WITNESS',
      thirdPartyIdDocument: 'CNI 123456789',
      thirdPartyCircumstances: 'Patient trouvé sur la voie publique.',
      thirdPartyConsentToContact: true,
    });
    component.nextStep();
    component.form.patchValue({ chiefComplaint: 'Traumatisme' });

    component.submit();

    expect(emergencyApi.create).toHaveBeenCalledWith(expect.objectContaining({
      arrivalMode: 'ACCOMPANIED',
      thirdPartyName: 'Paul Tamo',
      thirdPartyPhone: '+237699000111',
      thirdPartyRelationship: 'WITNESS',
      thirdPartyConsentToContact: true,
    }));
  });

  it('uses one atomic request for an unknown patient emergency', () => {
    component.setPatientMode('PROVISIONAL');
    component.nextStep();
    component.form.patchValue({ arrivalMode: 'AMBULANCE' });
    component.nextStep();
    component.form.patchValue({ chiefComplaint: 'Patient inconscient' });

    component.submit();

    expect(emergencyApi.createProvisionalAdmission).toHaveBeenCalledWith(expect.objectContaining({
      requestId: expect.any(String),
      patient: expect.objectContaining({ confidenceLevel: 'NONE' }),
      emergency: expect.objectContaining({ chiefComplaint: 'Patient inconscient' }),
    }));
    expect(emergencyApi.create).not.toHaveBeenCalled();
  });

  it('opens a normal visit with the shared orientation codes for the searched patient', () => {
    component.setCarePath('NORMAL');
    component.onPatientSelected(patient as unknown as Patient);
    component.nextStep();
    fixture.detectChanges();
    component.form.patchValue({
      reason: 'Fièvre depuis 3 jours',
      orientation: 'CONSULTATION',
      service: 'Pédiatrie',
      mainPractitionerId: 'doctor-1',
    });
    component.nextStep();

    component.submit();

    expect(fixture.nativeElement.querySelector('app-visit-details-fields')).toBeTruthy();
    expect(visitApi.create).toHaveBeenCalledWith(expect.objectContaining({
      patientId: 'patient-1',
      reason: 'Fièvre depuis 3 jours',
      orientation: 'CONSULTATION',
      service: 'Pédiatrie',
      mainPractitionerId: 'doctor-1',
    }));
  });

  it('does not offer emergency orientation in the normal path', () => {
    component.setCarePath('NORMAL');
    component.onPatientSelected(patient as unknown as Patient);
    component.nextStep();
    fixture.detectChanges();

    const options = Array.from(fixture.nativeElement.querySelectorAll('#visit-orientation option'))
      .map((option) => (option as HTMLOptionElement).value);
    expect(options).not.toContain('EMERGENCY');
    expect(options).toContain('CONSULTATION');
  });

  it('lists services and filters practitioners by the selected service from admission options', () => {
    component.setCarePath('NORMAL');
    component.onPatientSelected(patient as unknown as Patient);
    component.nextStep();
    fixture.detectChanges();
    component.form.patchValue({ service: 'Pédiatrie' });
    fixture.detectChanges();

    const options = (id: string) => Array.from(fixture.nativeElement.querySelectorAll(`#${id} option`))
      .map((option) => (option as HTMLOptionElement).textContent?.trim());
    expect(options('visit-service')).toEqual(expect.arrayContaining(['Médecine générale', 'Pédiatrie']));
    expect(options('visit-practitioner')).toContain('Dr Péd');
    expect(options('visit-practitioner')).not.toContain('Dr MG');
  });
});
