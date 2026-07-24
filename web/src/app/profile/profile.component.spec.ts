import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { StaffApiService } from '../clinic/staff/staff-api.service';
import { ProfileComponent } from './profile.component';

describe('ProfileComponent', () => {
  let fixture: ComponentFixture<ProfileComponent>;
  let component: ProfileComponent;
  let httpTesting: HttpTestingController;
  let staffApi: { getOwnActiveAssignments: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    staffApi = {
      getOwnActiveAssignments: vi.fn().mockReturnValue(of({
        specialties: [{
          specialtyCode: 'GENERAL_MEDICINE',
          nameFr: 'Médecine générale',
          nameEn: 'General medicine',
          primary: true,
          validFrom: '2026-07-24T06:00:00Z',
        }],
        unitAssignments: [{
          organizationalUnitId: 'unit-general',
          unitCode: 'SRV-MG',
          nameFr: 'Médecine générale',
          nameEn: 'General medicine',
          assignmentRoleCode: 'PRACTITIONER',
          assignmentRoleNameFr: 'Praticien',
          assignmentRoleNameEn: 'Practitioner',
          primary: true,
          validFrom: '2026-07-24T06:00:00Z',
        }],
      })),
    };

    await TestBed.configureTestingModule({
      imports: [ProfileComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: StaffApiService, useValue: staffApi },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    httpTesting.expectOne('/api/profile').flush({
      id: 'staff-1',
      email: 'doctor@joprelys.local',
      displayName: 'Dr Démo',
      role: 'MEDECIN',
      enabled: true,
      createdAt: '2026-07-24T05:00:00Z',
      phone: '+237677889900',
      registrationNumber: 'ONMC-DEMO-001',
      bio: 'Médecin de démonstration',
    });
    fixture.detectChanges();
  });

  afterEach(() => httpTesting.verify());

  it('should load and render active structured professional assignments as read-only context', () => {
    expect(staffApi.getOwnActiveAssignments).toHaveBeenCalled();
    expect(component.assignments().specialties[0].specialtyCode).toBe('GENERAL_MEDICINE');
    expect(component.assignments().unitAssignments[0].assignmentRoleCode).toBe('PRACTITIONER');

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Médecine générale');
    expect(text).toContain('Praticien');
    expect(text).toContain('MEDECIN');
  });
});
