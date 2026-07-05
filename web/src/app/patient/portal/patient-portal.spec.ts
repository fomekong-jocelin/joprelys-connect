import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { PatientProfileCardComponent } from './components/patient-profile-card.component';
import { PatientVisitsListComponent } from './components/patient-visits-list.component';
import { PatientPortalService, PatientPortalMeResponse } from './services/patient-portal.service';
import { PatientAuditListComponent } from './components/patient-audit-list.component';
import { PatientConsentsListComponent } from './components/patient-consents-list.component';
import { PatientNotificationsComponent } from './components/patient-notifications.component';

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
    fixture.componentInstance.expandedConsultations['visit-id-123'] = true;
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Dr. Alpha');
    expect(element.textContent).toContain('Fièvre passagère');

    let emittedId: string | undefined;
    fixture.componentInstance.download.subscribe((id) => (emittedId = id));

    const btn = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Télécharger PDF'));
    expect(btn).toBeTruthy();
    btn?.click();
    expect(emittedId).toBe('visit-id-123');
  });

  it('should display teletransmission button and emit transmit event', () => {
    const fixture = TestBed.createComponent(PatientVisitsListComponent);
    const consultWithPresc = {
      ...MOCK_PATIENT.consultations[0],
      prescriptionId: 'presc-id-123',
      prescriptionTransmissionStatus: 'NOT_TRANSMITTED'
    };
    fixture.componentInstance.consultations = [consultWithPresc];
    fixture.componentInstance.expandedConsultations['visit-id-123'] = true;
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Télétransmission');

    let emittedPrescId: string | undefined;
    fixture.componentInstance.transmit.subscribe((id) => (emittedPrescId = id));

    const btn = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Télétransmettre à AllôPharma'));
    expect(btn).toBeTruthy();
    btn?.click();
    expect(emittedPrescId).toBe('presc-id-123');
  });
});

describe('PatientAuditListComponent', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PatientAuditListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        PatientPortalService
      ]
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should fetch and display audit logs list', () => {
    const fixture = TestBed.createComponent(PatientAuditListComponent);
    fixture.detectChanges();

    const mockLogs = [
      { id: 'log-1', action: 'EMERGENCY_ACCESS', reason: 'Arrêt cardiaque', ipAddress: '127.0.0.1', userAgent: 'Chrome', status: 'SUCCESS', createdAt: '2026-07-02T12:00:00Z', organizationName: 'Clinique Test A' }
    ];

    const req = httpTesting.expectOne('/api/patient/audit-logs');
    expect(req.request.method).toBe('GET');
    req.flush(mockLogs);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Clinique Test A');
    expect(element.textContent).toContain('Urgence');
    expect(element.textContent).toContain('Arrêt cardiaque');
    expect(element.textContent).toContain('SUCCESS');
  });
});

describe('PatientConsentsListComponent', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PatientConsentsListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        PatientPortalService
      ]
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should fetch and display consents list and allow toggling', () => {
    const fixture = TestBed.createComponent(PatientConsentsListComponent);
    fixture.detectChanges();

    const mockConsents = [
      { organizationId: 'org-1', organizationName: 'Clinique Test A', status: 'ACTIVE', isCreator: true },
      { organizationId: 'org-2', organizationName: 'Clinique Test B', status: 'REVOKED', isCreator: false }
    ];

    const getReq = httpTesting.expectOne('/api/patient/consents');
    expect(getReq.request.method).toBe('GET');
    getReq.flush(mockConsents);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Clinique Test A');
    expect(element.textContent).toContain('Établissement Créateur');
    expect(element.textContent).toContain('Clinique Test B');
    expect(element.textContent).toContain('Accès révoqué');

    const btn = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Accès révoqué'));
    expect(btn).toBeTruthy();
    btn?.click();

    const postReq = httpTesting.expectOne('/api/patient/consents/org-2?status=ACTIVE');
    expect(postReq.request.method).toBe('POST');
    postReq.flush(null);

    fixture.detectChanges();
    expect(element.textContent).toContain('Accès autorisé');
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

  it('should call getAuditLogs and return patient audit logs list', () => {
    const mockLogs = [
      { id: 'log-1', action: 'EMERGENCY_ACCESS', reason: 'Arrêt cardio', ipAddress: '127.0.0.1', userAgent: 'Chrome', status: 'SUCCESS', createdAt: '2026-07-02T12:00:00Z', organizationName: 'Clinique Test A' }
    ];

    service.getAuditLogs().subscribe((data) => {
      expect(data.length).toBe(1);
      expect(data[0].action).toBe('EMERGENCY_ACCESS');
      expect(data[0].organizationName).toBe('Clinique Test A');
    });

    const req = httpTesting.expectOne('/api/patient/audit-logs');
    expect(req.request.method).toBe('GET');
    req.flush(mockLogs);
  });

  it('should call transmitPrescription', () => {
    service.transmitPrescription('presc-id-123').subscribe();

    const req = httpTesting.expectOne('/api/patient/me/prescriptions/presc-id-123/transmit');
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('should call getMedicalSummary', () => {
    service.getMedicalSummary().subscribe((data) => {
      expect(data).toBeTruthy();
    });

    const req = httpTesting.expectOne('/api/patient/medical-summary');
    expect(req.request.method).toBe('GET');
    req.flush({ fullName: 'Jean Patient A' });
  });

  it('should call downloadSummaryPdf', () => {
    service.downloadSummaryPdf().subscribe((blob) => {
      expect(blob).toBeTruthy();
    });

    const req = httpTesting.expectOne('/api/patient/summary-pdf');
    expect(req.request.method).toBe('GET');
    req.flush(new Blob());
  });
});

describe('PatientNotificationsComponent', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PatientNotificationsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        PatientPortalService
      ]
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should fetch, display, and mark notifications as read', () => {
    const fixture = TestBed.createComponent(PatientNotificationsComponent);
    fixture.detectChanges();

    const mockNotifs = [
      { id: 'notif-1', patientId: 'p-1', title: 'Alerte sécurité', message: 'Accès urgence', type: 'SECURITY', status: 'NON_LU', createdAt: '2026-07-02T12:00:00Z' },
      { id: 'notif-2', patientId: 'p-1', title: 'Info visite', message: 'Nouvel examen dispo', type: 'INFO', status: 'LU', createdAt: '2026-07-02T13:00:00Z' }
    ];

    const getReq = httpTesting.expectOne('/api/patient/notifications');
    expect(getReq.request.method).toBe('GET');
    getReq.flush(mockNotifs);

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Alerte sécurité');
    expect(element.textContent).toContain('Info visite');
    expect(element.textContent).toContain('Accès urgence');

    // Click "Marquer lu" for notif-1
    const btn = Array.from(element.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Marquer lu'));
    expect(btn).toBeTruthy();
    btn?.click();

    const postReq = httpTesting.expectOne('/api/patient/notifications/notif-1/read');
    expect(postReq.request.method).toBe('POST');
    postReq.flush({ ...mockNotifs[0], status: 'LU' });

    fixture.detectChanges();
  });
});
