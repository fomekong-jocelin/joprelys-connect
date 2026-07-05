import { TestBed, ComponentFixture } from '@angular/core/testing';
import { PatientDetailComponent } from './patient-detail.component';
import { PatientProfileTabComponent } from './detail/patient-profile-tab.component';
import { PatientConsultationsTabComponent } from './detail/patient-consultations-tab.component';
import { PatientLabOrdersTabComponent } from './detail/patient-lab-orders-tab.component';
import { PatientAuditTrailTabComponent } from './detail/patient-audit-trail-tab.component';
import { AuthTokenStorageService } from '../auth/auth-token-storage.service';
import { VisitApiService } from '../visit/visit-api.service';
import { ConsultationApiService } from '../consultation/consultation-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { AuditApiService } from '../audit/audit-api.service';
import { PatientApiService } from './patient-api.service';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { Patient, LabOrder, LabResult } from './patient.models';
import { Consultation } from '../consultation/consultation.models';
import { AuditLog } from '../audit/audit.models';

describe('PatientDetail System Tests', () => {
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
    createdAt: '2026-07-05T12:00:00Z',
    updatedAt: '2026-07-05T12:00:00Z'
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
      createdAt: '2026-07-05T12:00:00Z',
      updatedAt: '2026-07-05T12:00:00Z',
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
      createdAt: '2026-07-05T12:00:00Z'
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

  let mockAuthToken: any;
  let mockVisitApi: any;
  let mockConsultationApi: any;
  let mockAuditApi: any;
  let mockPatientApi: any;
  let mockI18n: any;
  let mockParentDetail: any;

  beforeEach(() => {
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
      cancelDocument: vi.fn().mockReturnValue(of({ id: 'doc-uuid-1', status: 'ANNULE' })),
      transmitPrescription: vi.fn().mockReturnValue(of({}))
    };

    mockAuditApi = {
      getPatientLogs: vi.fn().mockReturnValue(of(mockAuditLogs)),
      getOrganizationLogs: vi.fn().mockReturnValue(of([]))
    };

    mockPatientApi = {
      getPatientLabOrders: vi.fn().mockReturnValue(of(mockLabOrders)),
      getPatientLabResults: vi.fn().mockReturnValue(of(mockLabResults)),
      getAllergies: vi.fn().mockReturnValue(of([])),
      getMedicalHistory: vi.fn().mockReturnValue(of([])),
      getVaccinations: vi.fn().mockReturnValue(of([])),
      getById: vi.fn().mockReturnValue(of(mockPatient)),
      triggerEmergencyAccess: vi.fn().mockReturnValue(of(undefined))
    };

    mockI18n = {
      t: vi.fn().mockImplementation((key) => key),
      locale: signal('FR')
    };

    mockParentDetail = {
      patient: signal(mockPatient)
    };
  });

  describe('PatientDetailComponent', () => {
    let component: PatientDetailComponent;
    let fixture: ComponentFixture<PatientDetailComponent>;

    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [PatientDetailComponent],
        providers: [
          provideRouter([
            { path: 'patients/:id', redirectTo: '' }
          ]),
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

    it('should evaluate canViewAudit correctly based on roles', () => {
      expect(component.canViewAudit()).toBe(true); // MEDECIN

      mockAuthToken.session.set({
        name: 'Dr. Alpha',
        email: 'medecin@joprelys.local',
        role: 'ADMIN_CLINIQUE',
        org: 'org-1'
      });
      fixture.detectChanges();
      expect(component.canViewAudit()).toBe(true);

      mockAuthToken.session.set({
        name: 'Dr. Alpha',
        email: 'medecin@joprelys.local',
        role: 'AUDITEUR',
        org: 'org-1'
      });
      fixture.detectChanges();
      expect(component.canViewAudit()).toBe(true);

      mockAuthToken.session.set({
        name: 'Dr. Alpha',
        email: 'medecin@joprelys.local',
        role: 'AGENT_ACCUEIL',
        org: 'org-1'
      });
      fixture.detectChanges();
      expect(component.canViewAudit()).toBe(false);
    });
  });

  describe('PatientProfileTabComponent', () => {
    let component: PatientProfileTabComponent;
    let fixture: ComponentFixture<PatientProfileTabComponent>;

    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [PatientProfileTabComponent],
        providers: [
          { provide: PatientDetailComponent, useValue: mockParentDetail },
          { provide: PatientApiService, useValue: mockPatientApi },
          { provide: I18nService, useValue: mockI18n }
        ]
      }).compileComponents();

      fixture = TestBed.createComponent(PatientProfileTabComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('should calculate age correctly', () => {
      expect(component.age()).toBe(41); // 2026 - 1985
    });
  });

  describe('PatientConsultationsTabComponent', () => {
    let component: PatientConsultationsTabComponent;
    let fixture: ComponentFixture<PatientConsultationsTabComponent>;

    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [PatientConsultationsTabComponent],
        providers: [
          { provide: PatientDetailComponent, useValue: mockParentDetail },
          { provide: AuthTokenStorageService, useValue: mockAuthToken },
          { provide: ConsultationApiService, useValue: mockConsultationApi },
          { provide: I18nService, useValue: mockI18n }
        ]
      }).compileComponents();

      fixture = TestBed.createComponent(PatientConsultationsTabComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('should load history and display consultations', () => {
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

    it('should call transmitPrescription api and reload history on success', () => {
      mockConsultationApi.transmitPrescription.mockReturnValue(of({}));
      const consultWithPresc = {
        ...mockConsultations[0],
        prescriptionId: 'presc-uuid-123',
        prescriptionTransmissionStatus: 'NOT_TRANSMITTED'
      };
      component.transmitPrescription(consultWithPresc);
      expect(mockConsultationApi.transmitPrescription).toHaveBeenCalledWith('presc-uuid-123');
      expect(mockConsultationApi.getPatientConsultations).toHaveBeenCalled();
    });
  });

  describe('PatientAuditTrailTabComponent', () => {
    let component: PatientAuditTrailTabComponent;
    let fixture: ComponentFixture<PatientAuditTrailTabComponent>;

    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [PatientAuditTrailTabComponent],
        providers: [
          { provide: PatientDetailComponent, useValue: mockParentDetail },
          { provide: AuthTokenStorageService, useValue: mockAuthToken },
          { provide: AuditApiService, useValue: mockAuditApi },
          { provide: I18nService, useValue: mockI18n }
        ]
      }).compileComponents();

      fixture = TestBed.createComponent(PatientAuditTrailTabComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('should load audit logs', () => {
      expect(mockAuditApi.getPatientLogs).toHaveBeenCalledWith('pat-1');
      expect(component.auditLogs()).toEqual(mockAuditLogs);
    });
  });

  describe('PatientLabOrdersTabComponent', () => {
    let component: PatientLabOrdersTabComponent;
    let fixture: ComponentFixture<PatientLabOrdersTabComponent>;

    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [PatientLabOrdersTabComponent],
        providers: [
          { provide: PatientDetailComponent, useValue: mockParentDetail },
          { provide: PatientApiService, useValue: mockPatientApi }
        ]
      }).compileComponents();

      fixture = TestBed.createComponent(PatientLabOrdersTabComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('should load lab orders and results', () => {
      expect(mockPatientApi.getPatientLabOrders).toHaveBeenCalledWith('pat-1');
      expect(mockPatientApi.getPatientLabResults).toHaveBeenCalledWith('pat-1');
      expect(component.labOrders()).toEqual(mockLabOrders);
      expect(component.labResults()).toEqual(mockLabResults);
    });

    it('should compute analyteNames, filteredResults, and chartPoints correctly', () => {
      expect(component.analyteNames()).toEqual(['Glucose à jeun']);
      expect(component.selectedAnalyte()).toBe('Glucose à jeun');
      expect(component.filteredResults()).toEqual(mockLabResults);
      expect(component.chartPoints().length).toBe(1);
      expect(component.chartPoints()[0].val).toBe(1.45);
      expect(component.chartPoints()[0].interpretation).toBe('ELEVE');
    });
  });
});
