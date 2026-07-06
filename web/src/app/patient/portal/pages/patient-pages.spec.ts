import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { provideI18nTesting } from '../../../../testing/i18n-testing';
import { PatientDocumentsPageComponent } from './patient-documents-page.component';
import { PatientPrivacyPageComponent } from './patient-privacy-page.component';
import { PatientProfilePageComponent } from './patient-profile-page.component';
import { PatientQrCodePageComponent } from './patient-qr-code-page.component';
import { PatientPortalMeResponse, PatientPortalService } from '../services/patient-portal.service';

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
      clinicName: 'Clinique Test',
      diagnosis: 'Fièvre passagère',
      documentId: 'doc-id-123',
      documentStatus: 'VALID'
    }
  ]
};

describe('PatientProfilePageComponent', () => {
  it('should load and display patient profile', () => {
    const portalService = { getMe: vi.fn().mockReturnValue(of(MOCK_PATIENT)) };
    TestBed.configureTestingModule({
      imports: [PatientProfilePageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideI18nTesting(),
        provideRouter([]),
        { provide: PatientPortalService, useValue: portalService }
      ]
    });

    const fixture = TestBed.createComponent(PatientProfilePageComponent);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Jean Patient A');
    expect(element.textContent).toContain('PAT-20260702-000001');
    expect(portalService.getMe).toHaveBeenCalled();
  });

  it('should display error when profile loading fails', () => {
    const portalService = { getMe: vi.fn().mockReturnValue(throwError(() => ({ error: { detail: 'Erreur profil' } }))) };
    TestBed.configureTestingModule({
      imports: [PatientProfilePageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideI18nTesting(),
        provideRouter([]),
        { provide: PatientPortalService, useValue: portalService }
      ]
    });

    const fixture = TestBed.createComponent(PatientProfilePageComponent);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Erreur profil');
  });
});

describe('PatientDocumentsPageComponent', () => {
  it('should list downloadable documents', () => {
    const portalService = {
      getMe: vi.fn().mockReturnValue(of(MOCK_PATIENT)),
      downloadDocument: vi.fn().mockReturnValue(of(new Blob()))
    };
    TestBed.configureTestingModule({
      imports: [PatientDocumentsPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideI18nTesting(),
        provideRouter([]),
        { provide: PatientPortalService, useValue: portalService }
      ]
    });

    const fixture = TestBed.createComponent(PatientDocumentsPageComponent);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('VIS-20260702-000001');
    expect(element.textContent).toContain('Télécharger');
  });

  it('should show empty state when no documents', () => {
    const portalService = { getMe: vi.fn().mockReturnValue(of({ ...MOCK_PATIENT, consultations: [] })) };
    TestBed.configureTestingModule({
      imports: [PatientDocumentsPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideI18nTesting(),
        provideRouter([]),
        { provide: PatientPortalService, useValue: portalService }
      ]
    });

    const fixture = TestBed.createComponent(PatientDocumentsPageComponent);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Aucun document médical disponible');
  });
});

describe('PatientQrCodePageComponent', () => {
  it('should display patient QR code page', () => {
    const portalService = { getMe: vi.fn().mockReturnValue(of(MOCK_PATIENT)) };
    TestBed.configureTestingModule({
      imports: [PatientQrCodePageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideI18nTesting(),
        provideRouter([]),
        { provide: PatientPortalService, useValue: portalService }
      ]
    });

    const fixture = TestBed.createComponent(PatientQrCodePageComponent);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('PAT-20260702-000001');
    expect(element.querySelector('img')).toBeTruthy();
  });
});

describe('PatientPrivacyPageComponent', () => {
  it('should display active consents', () => {
    const portalService = {
      getConsents: vi.fn().mockReturnValue(of([
        { organizationId: 'org-1', organizationName: 'Clinique Test', status: 'ACTIVE', isCreator: true }
      ]))
    };
    TestBed.configureTestingModule({
      imports: [PatientPrivacyPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideI18nTesting(),
        provideRouter([]),
        { provide: PatientPortalService, useValue: portalService }
      ]
    });

    const fixture = TestBed.createComponent(PatientPrivacyPageComponent);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Clinique Test');
    expect(element.textContent).toContain('Actif');
  });

  it('should show empty state when no active consents', () => {
    const portalService = { getConsents: vi.fn().mockReturnValue(of([])) };
    TestBed.configureTestingModule({
      imports: [PatientPrivacyPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideI18nTesting(),
        provideRouter([]),
        { provide: PatientPortalService, useValue: portalService }
      ]
    });

    const fixture = TestBed.createComponent(PatientPrivacyPageComponent);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Aucun consentement actif');
  });
});
