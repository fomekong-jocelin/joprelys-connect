import { TestBed, ComponentFixture } from '@angular/core/testing';
import { PatientDetailComponent } from './patient-detail.component';
import { AuthTokenStorageService } from '../auth/auth-token-storage.service';
import { VisitApiService } from '../visit/visit-api.service';
import { ConsultationApiService } from '../consultation/consultation-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { AuditApiService } from '../audit/audit-api.service';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { Patient, LabOrder, LabResult } from './patient.models';
import { Consultation } from '../consultation/consultation.models';
import { AuditLog } from '../audit/audit.models';
import { PatientApiService } from './patient-api.service';

describe('PatientDetailComponent', () => {
  let component: PatientDetailComponent;
  let fixture: ComponentFixture<PatientDetailComponent>;
  let mockAuthToken: any;
  let mockVisitApi: any;
  let mockConsultationApi: any;
  let mockAuditApi: any;
  let mockPatientApi: any;
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

  const mockAuditLogs: AuditLog[] = [
    {
      id: 'log-1',
      actorUserId: 'user-1',
      actorName: 'Dr. Alpha',
      actorOrganizationId: 'org-1',
      patientId: 'pat-1',
      patientName: 'Jean Patient',
      resourceType: 'PATIENT_RECORD',
      resourceId: 'pat-1',
      action: 'CONSULTATION',
      reason: 'Accès dossier',
      ipAddress: '127.0.0.1',
      userAgent: 'Mozilla',
      status: 'SUCCESS',
      createdAt: '2026-07-02T12:00:00Z'
    }
  ];

  const mockLabOrders: LabOrder[] = [
    {
      id: 'lo-1',
      examRequestNumber: 'EXAM-REQ-20260703-000042',
      patientId: 'pat-1',
      patientName: 'Jean Patient',
      requesterPractitionerId: 'doc-1',
      requesterPractitionerName: 'Dr. Alpha',
      sourceOrganizationId: 'org-1',
      examType: 'LABORATOIRE',
      exams: ['GLYSEMIE_A_JEUN'],
      priority: 'NORMALE',
      status: 'VALIDATED',
      createdAt: '2026-07-03T10:00:00Z'
    }
  ];

  const mockLabResults: LabResult[] = [
    {
      id: 'lr-1',
      resultNumber: 'EXAM-RES-20260703-000001',
      examRequestNumber: 'EXAM-REQ-20260703-000042',
      patientId: 'pat-1',
      validatorName: 'Dr. Jean Kamdem',
      analyteName: 'Glucose à jeun',
      value: '1.45',
      unit: 'g/L',
      referenceRange: '0.70 - 1.10',
      interpretation: 'ELEVE',
      comment: 'Patient à jeun depuis 12h',
      createdAt: '2026-07-03T10:15:00Z',
      validatedAt: '2026-07-03T10:15:00Z'
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

    mockAuditApi = {
      getPatientLogs: vi.fn().mockReturnValue(of(mockAuditLogs)),
      getOrganizationLogs: vi.fn().mockReturnValue(of([]))
    };

    mockPatientApi = {
      getPatientLabOrders: vi.fn().mockReturnValue(of(mockLabOrders)),
      getPatientLabResults: vi.fn().mockReturnValue(of(mockLabResults)),
      getAllergies: vi.fn().mockReturnValue(of([]))
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
        { provide: AuditApiService, useValue: mockAuditApi },
        { provide: PatientApiService, useValue: mockPatientApi },
        { provide: I18nService, useValue: mockI18n }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PatientDetailComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('patient', mockPatient);
    fixture.detectChanges();
  });

  it('should load history and display consultations when medical tab is selected', () => {
    component.setActiveTab('medical');
    expect(mockConsultationApi.getPatientConsultations).toHaveBeenCalledWith('pat-1');
    expect(component.consultationHistory()).toEqual(mockConsultations);
    expect(component.activeTab()).toBe('medical');
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

  it('should load audit logs when audit tab is selected', () => {
    component.setActiveTab('audit');
    expect(mockAuditApi.getPatientLogs).toHaveBeenCalledWith('pat-1');
    expect(component.auditLogs()).toEqual(mockAuditLogs);
    expect(component.activeTab()).toBe('audit');
  });

  it('should load lab orders and results when lab tab is selected', () => {
    component.setActiveTab('lab');
    expect(mockPatientApi.getPatientLabOrders).toHaveBeenCalledWith('pat-1');
    expect(mockPatientApi.getPatientLabResults).toHaveBeenCalledWith('pat-1');
    expect(component.labOrders()).toEqual(mockLabOrders);
    expect(component.labResults()).toEqual(mockLabResults);
    expect(component.activeTab()).toBe('lab');
  });

  it('should compute analyteNames, filteredResults, and chartPoints correctly', () => {
    component.setActiveTab('lab');
    expect(component.analyteNames()).toEqual(['Glucose à jeun']);
    expect(component.selectedAnalyte()).toBe('Glucose à jeun');
    expect(component.filteredResults()).toEqual(mockLabResults);
    expect(component.chartPoints().length).toBe(1);
    expect(component.chartPoints()[0].val).toBe(1.45);
    expect(component.chartPoints()[0].interpretation).toBe('ELEVE');
  });

  it('should evaluate canViewAudit correctly based on roles', () => {
    expect(component.canViewAudit()).toBe(true); // MEDECIN

    mockAuthToken.session.set({ role: 'ADMIN_CLINIQUE' });
    expect(component.canViewAudit()).toBe(true);

    mockAuthToken.session.set({ role: 'AUDITEUR' });
    expect(component.canViewAudit()).toBe(true);

    mockAuthToken.session.set({ role: 'AGENT_ACCUEIL' });
    expect(component.canViewAudit()).toBe(false);
  });
});
