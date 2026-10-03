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
      getPreRegistrations: vi.fn().mockImplementation(() => of(JSON.parse(JSON.stringify(mockPreRegistrationsPage)))),
      getAdmissionQrCode: vi.fn().mockReturnValue(of(new Blob())),
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
      session: signal({ role: 'AGENT_ACCUEIL', name: 'Dr Sophie Martin' }),
      registerSessionBoundaryCleanup: vi.fn()
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
    expect(mockPatientApi.getPreRegistrations).toHaveBeenCalledWith('AWAITING_VALIDATION', 0, 10);
    expect(component.preRegistrations().length).toBe(2);
    expect(component.preRegistrations()[0].id).toBe('prereg-1');
  });

  it('should request validated pre-registrations from the backend', () => {
    mockPreRegistrationsPage.content = [{ ...mockPreRegistrationsPage.content[0], status: 'VALIDATED' }];
    component.setFilter('VALIDATED');
    expect(component.filter()).toBe('VALIDATED');
    expect(mockPatientApi.getPreRegistrations).toHaveBeenLastCalledWith('VALIDATED', 0, 10);
    expect(component.preRegistrations().length).toBe(1);
    expect(component.preRegistrations()[0].id).toBe('prereg-1');
  });

  it('should expose the linked patient of a validated request', () => {
    const validated = { ...component.preRegistrations()[1], status: 'VALIDATED' as const, validatedPatientId: 'patient-42' };
    expect(component.linkedPatientId(validated)).toBe('patient-42');
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
    expect(mockPatientApi.getPreRegistrations).toHaveBeenCalled();
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

  it('should load the admission QR code from the backend, never from a third party', () => {
    const createObjectUrl = vi.fn().mockReturnValue('blob:qr');
    Object.defineProperty(window.URL, 'createObjectURL', { value: createObjectUrl, configurable: true });
    Object.defineProperty(window.URL, 'revokeObjectURL', { value: vi.fn(), configurable: true });

    component.openQrModal();

    expect(mockPatientApi.getAdmissionQrCode).toHaveBeenCalledTimes(1);
    expect(component.qrCodeUrl()).toBe('blob:qr');
  });

  it('should open and close QR code modal', () => {
    component.openQrModal();
    expect(component.showQrCodeModal()).toBe(true);
    component.closeQrModal();
    expect(component.showQrCodeModal()).toBe(false);
  });
});
