import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { StaffApiService } from '../clinic/staff/staff-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { EmergencyDocumentApiService } from '../emergency/document/emergency-document-api.service';
import { PatientApiService } from './patient-api.service';
import { EmergencyHospitalizationContinuationComponent } from './emergency-hospitalization-continuation.component';
import { SpatialApiService } from './spatial-api.service';

describe('EmergencyHospitalizationContinuationComponent', () => {
  let fixture: ComponentFixture<EmergencyHospitalizationContinuationComponent>;
  let patientApi: { admitPatient: ReturnType<typeof vi.fn> };
  let documentApi: { generateBundle: ReturnType<typeof vi.fn> };
  let rbacApi: { hasPermission: ReturnType<typeof vi.fn> };
  let spatialApi: { getConfiguration: ReturnType<typeof vi.fn> };
  let staffApi: { list: ReturnType<typeof vi.fn> };

  const configureInputs = (): void => {
    fixture.componentRef.setInput('patientId', 'patient-1');
    fixture.componentRef.setInput('emergencyId', 'emergency-1');
    fixture.componentRef.setInput('identityStatus', 'PROVISIONAL_URGENCY');
    fixture.componentRef.setInput('temporaryPatientNumber', 'URG-TEMP-20260721-000001');
    fixture.detectChanges();
  };

  beforeEach(async () => {
    patientApi = {
      admitPatient: vi.fn().mockReturnValue(of({ id: 'stay-1' })),
    };
    documentApi = {
      generateBundle: vi.fn().mockReturnValue(of([{ id: 'doc-1' }])),
    };
    rbacApi = {
      hasPermission: vi.fn().mockImplementation((permission: string) => permission === 'HOSPITALIZATION_ADMIT'),
    };
    spatialApi = {
      getConfiguration: vi.fn().mockReturnValue(of({
        wards: [
          {
            id: 'ward-admin',
            name: 'Caisse',
            serviceType: 'ADMINISTRATIVE',
            allowsRooms: false,
            rooms: [],
          },
          {
            id: 'ward-hospital',
            name: 'Médecine',
            serviceType: 'HOSPITALIZATION',
            allowsRooms: true,
            rooms: [{
              id: 'room-1',
              wardId: 'ward-hospital',
              roomNumber: '101',
              capacity: 1,
              comfortLevel: 'STANDARD',
              beds: [{
                id: 'bed-1',
                roomId: 'room-1',
                bedNumber: '101-A',
                status: 'FREE',
                version: 0,
              }],
            }],
          },
        ],
      })),
    };
    staffApi = {
      list: vi.fn().mockReturnValue(of([
        {
          id: 'doctor-1',
          email: 'doctor@joprelys.com',
          displayName: 'Dr Test',
          role: 'MEDECIN',
          enabled: true,
          createdAt: '2026-07-21T00:00:00Z',
        },
      ])),
    };

    await TestBed.configureTestingModule({
      imports: [EmergencyHospitalizationContinuationComponent],
      providers: [
        { provide: PatientApiService, useValue: patientApi },
        { provide: EmergencyDocumentApiService, useValue: documentApi },
        { provide: RbacApiService, useValue: rbacApi },
        { provide: SpatialApiService, useValue: spatialApi },
        { provide: StaffApiService, useValue: staffApi },
        {
          provide: I18nService,
          useValue: { t: (_key: string, fallback: string) => fallback },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EmergencyHospitalizationContinuationComponent);
    configureInputs();
  });

  it('only proposes services that can host a patient', () => {
    expect(fixture.componentInstance.eligibleWards().map((ward) => ward.name)).toEqual(['Médecine']);
    expect(fixture.nativeElement.textContent).not.toContain('Caisse');
    expect(fixture.nativeElement.textContent).toContain('URG-TEMP-20260721-000001');
  });

  it('admits from the emergency without requiring a pre-existing visit and secures documents', () => {
    const admitted = vi.fn();
    fixture.componentInstance.admitted.subscribe(admitted);
    fixture.componentInstance.selectedBedId.set('bed-1');
    fixture.componentInstance.responsiblePractitionerId = 'doctor-1';
    fixture.componentInstance.admissionReason = 'Surveillance après stabilisation';

    fixture.componentInstance.submit(new Event('submit'));

    expect(patientApi.admitPatient).toHaveBeenCalledWith({
      patientId: 'patient-1',
      serviceName: 'Médecine',
      roomNumber: '101',
      bedNumber: '101-A',
      admissionReason: 'Surveillance après stabilisation',
      emergencyId: 'emergency-1',
      responsiblePractitionerId: 'doctor-1',
    });
    expect(documentApi.generateBundle).toHaveBeenCalledWith('emergency-1');
    expect(admitted).toHaveBeenCalledOnce();
  });

  it('does not load admission data or execute admission without the dedicated permission', () => {
    fixture.destroy();
    rbacApi.hasPermission.mockReturnValue(false);
    spatialApi.getConfiguration.mockClear();
    staffApi.list.mockClear();
    patientApi.admitPatient.mockClear();
    documentApi.generateBundle.mockClear();

    fixture = TestBed.createComponent(EmergencyHospitalizationContinuationComponent);
    configureInputs();

    fixture.componentInstance.selectedBedId.set('bed-1');
    fixture.componentInstance.responsiblePractitionerId = 'doctor-1';
    fixture.componentInstance.admissionReason = 'Surveillance après stabilisation';
    fixture.componentInstance.submit(new Event('submit'));

    expect(fixture.nativeElement.textContent).toContain(
      'Votre profil peut consulter le contexte d’urgence, mais ne peut pas créer un séjour hospitalier.',
    );
    expect(spatialApi.getConfiguration).not.toHaveBeenCalled();
    expect(staffApi.list).not.toHaveBeenCalled();
    expect(patientApi.admitPatient).not.toHaveBeenCalled();
    expect(documentApi.generateBundle).not.toHaveBeenCalled();
  });
});
