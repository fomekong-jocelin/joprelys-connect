import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { FileDragDropComponent } from '../shared/ui/file-drag-drop.component';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
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
  for (const status of [403, 500]) {
    it(`keeps the profile editable when assignments fail with ${status}`, () => {
      staffApi.getOwnActiveAssignments.mockReturnValue(throwError(() => new HttpErrorResponse({ status })));
      component.loadProfile();
      httpTesting.expectOne('/api/profile').flush({ displayName: 'Admin plateforme', role: 'ADMIN_JOPRELYS' });
      fixture.detectChanges();
      expect(component.displayName()).toBe('Admin plateforme');
      expect(component.loading()).toBe(false);
      expect(component.error()).toBeNull();
      expect(component.assignments()).toEqual({ specialties: [], unitAssignments: [] });
      expect(component.assignmentsWarning()).toBeTruthy();
      expect(fixture.nativeElement.querySelector('input[required]').disabled).toBe(false);
    });
  }

  it('uploads the physician signature and saves the canonical PNG path in the profile', () => {
    const signatureUploader = fixture.debugElement.queryAll(By.directive(FileDragDropComponent))[1].componentInstance as FileDragDropComponent;
    expect(signatureUploader.accept()).toBe('image/png, image/jpeg');
    signatureUploader.fileSelected.emit(new File(['synthetic'], 'signature.jpg', { type: 'image/jpeg' }));
    const upload = httpTesting.expectOne('/api/files/upload');
    expect((upload.request.body as FormData).get('type')).toBe('signature');
    upload.flush({ filePath: 'uploads/signature/test.png', viewUrl: '/api/public/files/view?path=uploads/signature/test.png' });
    expect(component.signaturePath()).toBe('uploads/signature/test.png');
    component.saveProfile();
    const save = httpTesting.expectOne('/api/profile');
    expect(save.request.method).toBe('PUT');
    expect(save.request.body.signaturePath).toBe('uploads/signature/test.png');
    save.flush({});
    expect(component.error()).toBeNull();
  });

  it('still reports a failure of the primary profile API', () => {
    component.loadProfile();
    httpTesting.expectOne('/api/profile').flush({}, { status: 500, statusText: 'Server Error' });
    expect(component.loading()).toBe(false);
    expect(component.error()).toBeTruthy();
  });

});
