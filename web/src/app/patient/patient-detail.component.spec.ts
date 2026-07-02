import { TestBed, ComponentFixture } from '@angular/core/testing';
import { PatientDetailComponent } from './patient-detail.component';
import { AuthTokenStorageService } from '../auth/auth-token-storage.service';
import { VisitApiService } from '../visit/visit-api.service';
import { ConsultationApiService } from '../consultation/consultation-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { Patient } from './patient.models';
import { Consultation } from '../consultation/consultation.models';

describe('PatientDetailComponent', () => {
  let component: PatientDetailComponent;
  let fixture: ComponentFixture<PatientDetailComponent>;
  let mockAuthToken: any;
  let mockVisitApi: any;
  let mockConsultationApi: any;
  let mockI18n: any;

  const mockPatient: Patient = {
    id: 'pat-1',
    organizationId: 'org-1',
    globalPatientNumber: 'DPU-001',
    localPatientNumber: 'PAT-001',
    fullName: 'Jean Patient',
    gender: 'MASCULIN',
    birthDate: '1985-05-15',
    phone: '+237699999999',
    city: 'Douala',
    status: 'ACTIVE',
    createdAt: '2026-07-02T12:00:00Z',
    updatedAt: '2026-07-02T12:00:00Z'
  };

  const mockConsultations: Consultation[] = [
    {
      id: 'c-1',
      visitId: 'visit-1',
      visitNumber: 'VIS-001',
      doctorId: 'doc-1',
      doctorName: 'Dr. Alpha',
      documentNumber: 'DOC-001',
      symptoms: 'Fever',
      diagnosis: 'Malaria',
      status: 'BROUILLON',
      createdAt: '2026-07-02T12:00:00Z',
      updatedAt: '2026-07-02T12:00:00Z',
      documentId: 'doc-uuid-1',
      documentStatus: 'VALID'
    }
  ];

  beforeEach(async () => {
    mockAuthToken = {
      session: signal({
        name: 'Dr. Alpha',
        email: 'medecin@joprelys.local',
        role: 'MEDECIN',
        org: 'org-1'
      })
    };

    mockVisitApi = {
      getActiveVisits: vi.fn().mockReturnValue(of([])),
      create: vi.fn()
    };

    mockConsultationApi = {
      getPatientConsultations: vi.fn().mockReturnValue(of(mockConsultations)),
      downloadDocument: vi.fn(),
      revokeDocument: vi.fn().mockReturnValue(of({ id: 'doc-uuid-1', status: 'REVOQUE' })),
      cancelDocument: vi.fn().mockReturnValue(of({ id: 'doc-uuid-1', status: 'ANNULE' }))
    };

    mockI18n = {
      t: vi.fn().mockImplementation((key) => key)
    };

    await TestBed.configureTestingModule({
      imports: [PatientDetailComponent],
      providers: [
        provideRouter([]),
        { provide: AuthTokenStorageService, useValue: mockAuthToken },
        { provide: VisitApiService, useValue: mockVisitApi },
        { provide: ConsultationApiService, useValue: mockConsultationApi },
        { provide: I18nService, useValue: mockI18n }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PatientDetailComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('patient', mockPatient);
    fixture.detectChanges();
  });

  it('should load history and display consultations', () => {
    component.toggleHistory();
    expect(mockConsultationApi.getPatientConsultations).toHaveBeenCalledWith('pat-1');
    expect(component.consultationHistory()).toEqual(mockConsultations);
  });

  it('should open and close revoke modal', () => {
    component.openRevokeModal(mockConsultations[0]);
    expect(component.showRevokeModal).toBe(true);
    expect(component.selectedConsultation()).toEqual(mockConsultations[0]);

    component.closeRevokeModal();
    expect(component.showRevokeModal).toBe(false);
    expect(component.selectedConsultation()).toBeNull();
  });

  it('should call revoke api and update status locally on success', () => {
    component.consultationHistory.set([{ ...mockConsultations[0] }]);
    component.openRevokeModal(component.consultationHistory()[0]);
    component.revokeActionType = 'REVOKE';
    component.revokeReason = 'Erreur posologie';

    component.submitRevocation();

    expect(mockConsultationApi.revokeDocument).toHaveBeenCalledWith('doc-uuid-1', 'Erreur posologie');
    expect(component.consultationHistory()[0].documentStatus).toBe('REVOQUE');
    expect(component.showRevokeModal).toBe(false);
  });

  it('should call cancel api and update status locally on success', () => {
    component.consultationHistory.set([{ ...mockConsultations[0] }]);
    component.openRevokeModal(component.consultationHistory()[0]);
    component.revokeActionType = 'CANCEL';
    component.revokeReason = 'Erreur doublon';

    component.submitRevocation();

    expect(mockConsultationApi.cancelDocument).toHaveBeenCalledWith('doc-uuid-1', 'Erreur doublon');
    expect(component.consultationHistory()[0].documentStatus).toBe('ANNULE');
    expect(component.showRevokeModal).toBe(false);
  });
});
