import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { HospitalOrganizationApiService } from '../clinic/hospital-organization/hospital-organization-api.service';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { EmergencyApiService } from '../emergency/emergency-api.service';
import { PatientApiService } from '../patient/patient-api.service';
import { VisitApiService } from '../visit/visit-api.service';
import { UnifiedAdmissionComponent } from './unified-admission.component';

describe('UnifiedAdmissionComponent', () => {
  let fixture: ComponentFixture<UnifiedAdmissionComponent>;
  let component: UnifiedAdmissionComponent;
  let permissions: ReturnType<typeof signal<Set<string>>>;
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

  beforeEach(async () => {
    localStorage.clear();
    permissions = signal(new Set(['PATIENT_READ', 'PATIENT_WRITE', 'EMERGENCY_WRITE', 'VISIT_CREATE']));
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
          provide: RbacApiService,
          useValue: {
            hasPermission: (permission: string) => permissions().has(permission),
          },
        },
        {
          provide: PatientApiService,
          useValue: {
            list: vi.fn().mockReturnValue(of([patient])),
            create: vi.fn().mockReturnValue(of(patient)),
          },
        },
        {
          provide: HospitalOrganizationApiService,
          useValue: { listServiceCatalog: vi.fn().mockReturnValue(of([])) },
        },
        { provide: EmergencyApiService, useValue: emergencyApi },
        { provide: VisitApiService, useValue: { create: vi.fn().mockReturnValue(of({ id: 'visit-1' })) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UnifiedAdmissionComponent);
    fixture.componentRef.setInput('initialCarePath', 'EMERGENCY');
    component = fixture.componentInstance;
    fixture.detectChanges();
    component.form.patchValue({ patientId: patient.id });
  });

  afterEach(() => localStorage.clear());

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

  it('does not expose or activate existing-patient mode without PATIENT_READ', () => {
    permissions.set(new Set(['EMERGENCY_WRITE']));
    fixture.componentRef.setInput('allowNewPatient', false);
    fixture.detectChanges();

    component.setPatientMode('EXISTING');
    component.nextStep();
    fixture.detectChanges();

    expect(component.allowExistingPatient()).toBe(false);
    expect(component.patientMode).toBe('PROVISIONAL');
    expect(fixture.nativeElement.textContent).not.toContain('admission.existing');
  });

  it('does not expose or activate new-patient mode when the caller lacks PATIENT_WRITE', () => {
    fixture.componentRef.setInput('allowNewPatient', false);
    fixture.detectChanges();

    component.setPatientMode('NEW');
    fixture.detectChanges();

    expect(component.patientMode).toBe('EXISTING');
    expect(fixture.nativeElement.textContent).not.toContain('admission.new');
  });

  it('locks the care path when the caller exposes only one authorized workflow', () => {
    fixture.componentRef.setInput('allowCarePathSwitch', false);
    fixture.detectChanges();

    component.setCarePath('NORMAL');
    fixture.detectChanges();

    expect(component.carePath).toBe('EMERGENCY');
    expect(fixture.nativeElement.textContent).not.toContain('admission.normalHint');
  });

  it('normalizes a restored draft that contains a now-forbidden patient mode and care path', () => {
    localStorage.setItem('joprelys_admission_draft', JSON.stringify({
      currentStep: 2,
      formValue: {
        carePath: 'EMERGENCY',
        patientMode: 'NEW',
        fullName: 'Brouillon interdit',
      },
    }));

    const restrictedFixture = TestBed.createComponent(UnifiedAdmissionComponent);
    restrictedFixture.componentRef.setInput('initialCarePath', 'NORMAL');
    restrictedFixture.componentRef.setInput('allowCarePathSwitch', false);
    restrictedFixture.componentRef.setInput('allowNewPatient', false);
    restrictedFixture.componentRef.setInput('allowProvisionalPatient', false);
    restrictedFixture.detectChanges();

    const restricted = restrictedFixture.componentInstance;
    expect(restricted.carePath).toBe('NORMAL');
    expect(restricted.patientMode).toBe('EXISTING');
    expect(restricted.currentStep()).toBe(2);

    restrictedFixture.destroy();
  });
});
