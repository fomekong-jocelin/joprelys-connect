import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { PatientProfileCardComponent } from './components/patient-profile-card.component';
import { PatientVisitsListComponent } from './components/patient-visits-list.component';
import { PatientPortalService, PatientPortalMeResponse } from './services/patient-portal.service';

const MOCK_PATIENT: PatientPortalMeResponse = {
  id: 'patient-id-123',
  globalPatientNumber: 'PAT-20260702-000001',
  localPatientNumber: 'LOC-A-001',
  fullName: 'Jean Patient A',
  gender: 'MASCULIN',
  birthDate: '1990-01-01',
  phone: '+237 699 99 99 99',
  city: 'Douala',
  district: 'Akwa',
  address: 'Rue 1',
  emergencyContactName: 'Marie',
  emergencyContactPhone: '+237 677 77 77 77',
  allergies: 'Aucune',
  medicalHistory: 'Aucun',
  consultations: [
    {
      visitId: 'visit-id-123',
      visitNumber: 'VIS-20260702-000001',
      visitDate: '2026-07-02',
      doctorName: 'Dr. Alpha',
      clinicName: 'PÉDIATRIE',
      diagnosis: 'Fièvre passagère',
      documentId: 'doc-id-123',
      documentStatus: 'VALID'
    }
  ]
};

describe('PatientProfileCardComponent', () => {
  it('should display patient profile details correctly', () => {
    const fixture = TestBed.createComponent(PatientProfileCardComponent);
    fixture.componentInstance.patient = MOCK_PATIENT;
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('h2')?.textContent).toContain('Jean Patient A');
    expect(element.textContent).toContain('PAT-20260702-000001');
    expect(element.textContent).toContain('Aucune');
  });
});

describe('PatientVisitsListComponent', () => {
  it('should list patient visits and emit download event', () => {
    const fixture = TestBed.createComponent(PatientVisitsListComponent);
    fixture.componentInstance.consultations = MOCK_PATIENT.consultations;
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Dr. Alpha');
    expect(element.textContent).toContain('Fièvre passagère');

    let emittedId: string | undefined;
    fixture.componentInstance.download.subscribe((id) => (emittedId = id));

    const btn = element.querySelector('button');
    expect(btn).toBeTruthy();
    btn?.click();
    expect(emittedId).toBe('visit-id-123');
  });
});

describe('PatientPortalService', () => {
  let service: PatientPortalService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        PatientPortalService
      ]
    });
    service = TestBed.inject(PatientPortalService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should call getMe and return patient data', () => {
    service.getMe().subscribe((data) => {
      expect(data.fullName).toBe('Jean Patient A');
    });

    const req = httpTesting.expectOne('/api/patient/me');
    expect(req.request.method).toBe('GET');
    req.flush(MOCK_PATIENT);
  });

  it('should call getConsents and updateConsent', () => {
    const mockConsents = [
      { organizationId: 'org-1', organizationName: 'Clinic A', status: 'ACTIVE', isCreator: true },
      { organizationId: 'org-2', organizationName: 'Clinic B', status: 'NONE', isCreator: false }
    ];

    service.getConsents().subscribe((data) => {
      expect(data.length).toBe(2);
      expect(data[0].organizationName).toBe('Clinic A');
    });

    const getReq = httpTesting.expectOne('/api/patient/consents');
    expect(getReq.request.method).toBe('GET');
    getReq.flush(mockConsents);

    service.updateConsent('org-2', 'ACTIVE').subscribe();
    const postReq = httpTesting.expectOne('/api/patient/consents/org-2?status=ACTIVE');
    expect(postReq.request.method).toBe('POST');
    postReq.flush(null);
  });
});
