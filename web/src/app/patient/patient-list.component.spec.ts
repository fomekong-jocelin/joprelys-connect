import { TestBed, ComponentFixture } from '@angular/core/testing';
import { PatientListComponent } from './patient-list.component';
import { PatientApiService } from './patient-api.service';
import { of } from 'rxjs';
import { Patient } from './patient.models';
import { provideRouter } from '@angular/router';

describe('PatientListComponent', () => {
  let component: PatientListComponent;
  let fixture: ComponentFixture<PatientListComponent>;
  let mockApi: any;

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
      create: vi.fn(),
      getById: vi.fn().mockReturnValue(of(mockPatients[0])),
    };

    await TestBed.configureTestingModule({
      imports: [PatientListComponent],
      providers: [
        provideRouter([]),
        { provide: PatientApiService, useValue: mockApi },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PatientListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load patients on init', () => {
    expect(mockApi.list).toHaveBeenCalled();
    expect(component.list()).toEqual(mockPatients);
    expect(component.loading()).toBe(false);
  });

  it('should call create patient api on submit', () => {
    component.fullName.set('Jean Dupont');
    component.gender.set('MASCULIN');
    component.birthDate.set('1990-05-15');
    component.phone.set('+237699999999');
    component.city.set('Douala');

    mockApi.create.mockReturnValue(of(mockPatients[0]));

    component.submit();

    expect(mockApi.create).toHaveBeenCalledWith({
      fullName: 'Jean Dupont',
      gender: 'MASCULIN',
      birthDate: '1990-05-15',
      phone: '+237699999999',
      city: 'Douala',
      district: undefined,
      address: undefined,
      emergencyContactName: undefined,
      emergencyContactPhone: undefined,
      allergies: undefined,
      medicalHistory: undefined,
    });
    expect(component.list().length).toBe(2);
  });
});
