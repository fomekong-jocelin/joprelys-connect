import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { PatientApiService } from './patient-api.service';
import { PatientListComponent } from './patient-list.component';
import { Patient } from './patient.models';

describe('PatientListComponent', () => {
  let component: PatientListComponent;
  let fixture: ComponentFixture<PatientListComponent>;
  let router: Router;
  let permissions: ReturnType<typeof signal<Set<string>>>;
  let mockApi: {
    list: ReturnType<typeof vi.fn>;
    getById: ReturnType<typeof vi.fn>;
    triggerEmergencyAccess: ReturnType<typeof vi.fn>;
  };

  const mockPatients: Patient[] = [
    {
      id: '1',
      organizationId: 'org1',
      globalPatientNumber: 'DPU-JOP-20260702-000001',
      localPatientNumber: 'PAT-20260702-000001',
      fullName: 'Jean Dupont',
      gender: 'MASCULIN',
      birthDate: '1990-05-15',
      phone: '+237699999999',
      city: 'Douala',
      status: 'ACTIVE',
      createdAt: '2026-07-02T12:00:00Z',
      updatedAt: '2026-07-02T12:00:00Z',
    },
  ];

  beforeEach(async () => {
    mockApi = {
      list: vi.fn().mockReturnValue(of(mockPatients)),
      getById: vi.fn().mockReturnValue(of(mockPatients[0])),
      triggerEmergencyAccess: vi.fn().mockReturnValue(of(void 0)),
    };
    permissions = signal(new Set(['PATIENT_READ', 'PATIENT_WRITE', 'VISIT_CREATE']));

    await TestBed.configureTestingModule({
      imports: [PatientListComponent],
      providers: [
        provideRouter([
          { path: 'patients/:id', redirectTo: '' },
          { path: 'clinic/emergencies', redirectTo: '' },
        ]),
        { provide: PatientApiService, useValue: mockApi },
        {
          provide: RbacApiService,
          useValue: {
            hasPermission: (permission: string) => permissions().has(permission),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PatientListComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should load patients on init', () => {
    expect(mockApi.list).toHaveBeenCalled();
    expect(component.list()).toEqual(mockPatients);
    expect(component.loading()).toBe(false);
  });

  it('should open the unified admission workspace when VISIT_CREATE is granted', () => {
    expect(component.canCreateVisit()).toBe(true);
    expect(component.showCreateForm()).toBe(false);

    component.toggleCreateForm();

    expect(component.showCreateForm()).toBe(true);
  });

  it('should hide and refuse the admission workspace without VISIT_CREATE', () => {
    permissions.set(new Set(['PATIENT_READ']));
    fixture.detectChanges();

    expect(component.canCreateVisit()).toBe(false);
    expect(fixture.nativeElement.textContent).not.toContain('admission.title');

    component.toggleCreateForm();
    expect(component.showCreateForm()).toBe(false);
  });

  it('should expose new-patient creation only with PATIENT_WRITE', () => {
    permissions.set(new Set(['PATIENT_READ', 'VISIT_CREATE']));
    fixture.detectChanges();

    expect(component.canCreateVisit()).toBe(true);
    expect(component.canCreatePatient()).toBe(false);
  });

  it('should navigate to the patient record after a normal admission', () => {
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    component.showCreateForm.set(true);

    component.onAdmissionCompleted({
      carePath: 'NORMAL',
      patientId: '1',
      patientDisplayName: 'Jean Dupont',
      visitId: 'visit-1',
    });

    expect(component.showCreateForm()).toBe(false);
    expect(navigateSpy).toHaveBeenCalledWith(['/patients', '1']);
  });

  it('should navigate to emergency operations after an emergency admission', () => {
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    component.onAdmissionCompleted({
      carePath: 'EMERGENCY',
      patientId: 'urg-temp-1',
      patientDisplayName: 'URG-TEMP-20260711-000001',
      emergencyId: 'emergency-1',
    });

    expect(navigateSpy).toHaveBeenCalledWith(['/clinic/emergencies']);
  });
});
