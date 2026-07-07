import { TestBed, ComponentFixture } from '@angular/core/testing';
import { PreRegistrationsListComponent } from './pre-registrations-list.component';
import { PatientApiService } from '../patient-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { of, throwError } from 'rxjs';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { AuthTokenStorageService } from '../../auth/auth-token-storage.service';

describe('PreRegistrationsListComponent', () => {
  let component: PreRegistrationsListComponent;
  let fixture: ComponentFixture<PreRegistrationsListComponent>;
  let mockPatientApi: any;
  let mockI18n: any;

  let mockPreRegistrationsPage: any;

  beforeEach(async () => {
    mockPreRegistrationsPage = {
      content: [
        {
          id: 'prereg-1',
          firstName: 'Jean',
          lastName: 'Dupont',
          gender: 'MASCULIN',
          birthDate: '1985-05-20',
          phone: '+237677112233',
          status: 'AWAITING_VALIDATION',
          createdAt: '2026-07-07T08:00:00Z',
          similarityScore: 90.0,
          similarPatientId: 'patient-exist-123',
          similarPatientName: 'Jean Dupont'
        },
        {
          id: 'prereg-2',
          firstName: 'Marie',
          lastName: 'Ngo',
          gender: 'FEMININ',
          birthDate: '1992-10-12',
          phone: '+237699445566',
          status: 'AWAITING_VALIDATION',
          createdAt: '2026-07-07T08:05:00Z'
        }
      ],
      totalElements: 2,
      totalPages: 1,
      size: 10,
      number: 0
    };

    mockPatientApi = {
      getPendingPreRegistrations: vi.fn().mockImplementation(() => of(JSON.parse(JSON.stringify(mockPreRegistrationsPage)))),
      validatePreRegistration: vi.fn().mockReturnValue(of({ patientId: 'patient-new-123', status: 'VALIDATED' })),
      rejectPreRegistration: vi.fn().mockReturnValue(of(null)),
      downloadSummaryPdf: vi.fn().mockReturnValue(of(new Blob()))
    };

    mockI18n = {
      t: vi.fn().mockImplementation((key) => key),
      locale: signal('fr')
    };

    const mockTokenStorage = {
      accessToken: 'header.eyJzdWIiOiJhZG1pbkBjbGluaXF1ZS5sb2NhbCIsImVtYWlsIjoiYWRtaW5AY2xpbmlxdWUubG9jYWwiLCJkaXNwbGF5TmFtZSI6IkRyIFNvcGhpZSBNYXJ0aW4iLCJyb2xlIjoiQURNSU5fQ0xJTklRVUUiLCJvcmciOiIzZmZjMzAzOS04MjEzLTQ2MTMtOTc1YS0zOTUwM2RjMGNlNWQifQ.signature',
      session: signal({ role: 'AGENT_ACCUEIL', name: 'Dr Sophie Martin' })
    };

    await TestBed.configureTestingModule({
      imports: [PreRegistrationsListComponent],
      providers: [
        provideRouter([]),
        { provide: PatientApiService, useValue: mockPatientApi },
        { provide: I18nService, useValue: mockI18n },
        { provide: AuthTokenStorageService, useValue: mockTokenStorage }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PreRegistrationsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize and load awaiting pre-registrations list', () => {
    expect(component).toBeTruthy();
    expect(mockPatientApi.getPendingPreRegistrations).toHaveBeenCalledWith(0, 10);
    expect(component.preRegistrations().length).toBe(2);
    expect(component.preRegistrations()[0].id).toBe('prereg-1');
  });

  it('should filter pre-registrations list by status validated', () => {
    mockPreRegistrationsPage.content[0].status = 'VALIDATED';
    component.setFilter('VALIDATED');
    expect(component.filter()).toBe('VALIDATED');
    expect(component.preRegistrations().length).toBe(1);
    expect(component.preRegistrations()[0].id).toBe('prereg-1');
  });

  it('should open details drawer and set reconciliation options', () => {
    const item = component.preRegistrations()[0];
    component.openDetails(item);

    expect(component.selectedPreRegistration()).toBe(item);
    expect(component.reconcileOption()).toBe('MERGE'); // similarPatientId is present
  });

  it('should close details drawer when closeDetails is called', () => {
    component.openDetails(component.preRegistrations()[0]);
    component.closeDetails();
    expect(component.selectedPreRegistration()).toBeNull();
  });

  it('should reject a pre-registration request with confirm dialog', () => {
    // Spy on window.confirm
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);

    component.rejectRequest('prereg-2');

    expect(confirmSpy).toHaveBeenCalled();
    expect(mockPatientApi.rejectPreRegistration).toHaveBeenCalledWith('prereg-2');
    expect(component.successMessage()).toBe('preRegistrations.successRejected');
    expect(mockPatientApi.getPendingPreRegistrations).toHaveBeenCalled();
  });

  it('should validate a pre-registration request as a new patient and trigger PDF download', () => {
    const item = component.preRegistrations()[1]; // prereg-2 (no similarity)
    component.openDetails(item);
    component.setReconcileOption('NEW');

    // Spy on downloadSummaryPdf method
    const downloadSpy = vi.spyOn(component, 'downloadSummaryPdf').mockImplementation(() => {});

    component.validateRequest(item);

    expect(mockPatientApi.validatePreRegistration).toHaveBeenCalledWith('prereg-2', {
      firstName: 'Marie',
      lastName: 'Ngo',
      gender: 'FEMININ',
      birthDate: '1992-10-12',
      bloodGroup: undefined,
      phone: '+237699445566',
      email: undefined,
      address: undefined,
      emergencyContactName: undefined,
      emergencyContactPhone: undefined,
      emergencyContactRelation: undefined,
      reconcileWithPatientId: undefined
    });
    expect(component.validatedPatientId()).toBe('patient-new-123');
    expect(downloadSpy).toHaveBeenCalledWith('patient-new-123');
  });

  it('should validate a pre-registration request by merging with existing patient', () => {
    const item = component.preRegistrations()[0]; // prereg-1 (has similarity)
    component.openDetails(item);
    component.setReconcileOption('MERGE');

    const downloadSpy = vi.spyOn(component, 'downloadSummaryPdf').mockImplementation(() => {});

    component.validateRequest(item);

    expect(mockPatientApi.validatePreRegistration).toHaveBeenCalledWith('prereg-1', {
      firstName: 'Jean',
      lastName: 'Dupont',
      gender: 'MASCULIN',
      birthDate: '1985-05-20',
      bloodGroup: undefined,
      phone: '+237677112233',
      email: undefined,
      address: undefined,
      emergencyContactName: undefined,
      emergencyContactPhone: undefined,
      emergencyContactRelation: undefined,
      reconcileWithPatientId: 'patient-exist-123'
    });
    expect(downloadSpy).toHaveBeenCalledWith('patient-new-123');
  });

  it('should handle validation errors', () => {
    mockPatientApi.validatePreRegistration.mockReturnValue(throwError(() => ({
      status: 400,
      error: { message: 'Données de validation incorrectes' }
    })));

    const item = component.preRegistrations()[1];
    component.openDetails(item);
    component.validateRequest(item);

    expect(component.error()).toBe('Données de validation incorrectes');
    expect(component.validatedPatientId()).toBeNull();
  });

  it('should generate clinic admission link and QR code url based on token claims', () => {
    expect(component.getOrganizationId()).toBe('3ffc3039-8213-4613-975a-39503dc0ce5d');
    expect(component.getAdmissionLink()).toContain('/public/register?orgId=3ffc3039-8213-4613-975a-39503dc0ce5d');
    expect(component.getQrCodeUrl()).toContain('https://api.qrserver.com/v1/create-qr-code/');
  });

  it('should open and close QR code modal', () => {
    component.openQrModal();
    expect(component.showQrCodeModal()).toBe(true);
    component.closeQrModal();
    expect(component.showQrCodeModal()).toBe(false);
  });
});
