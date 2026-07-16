import { TestBed, ComponentFixture } from '@angular/core/testing';
import { OrganizationListComponent } from './organization-list.component';
import { OrganizationApiService } from './organization-api.service';
import { of, throwError } from 'rxjs';
import { Organization } from './organizations.models';
import { RouterTestingModule } from '@angular/router/testing';

describe('OrganizationListComponent', () => {
  let component: OrganizationListComponent;
  let fixture: ComponentFixture<OrganizationListComponent>;
  let mockApi: any;

  const mockOrgs: Organization[] = [
    {
      id: '1',
      name: 'Clinique de la Paix',
      email: 'paix@joprelys.local',
      city: 'Yaoundé',
      status: 'ACTIVE',
      createdAt: '2026-07-02T12:00:00Z',
      country: 'Cameroun',
      type: 'CLINIC',
      responsibleName: 'Dr. Paix',
      apiEnabled: true
    }
  ];

  beforeEach(async () => {
    mockApi = {
      list: vi.fn().mockReturnValue(of(mockOrgs)),
      create: vi.fn(),
      updateStatus: vi.fn(),
      update: vi.fn(),
      listApiKeys: vi.fn().mockReturnValue(of([])),
      generateApiKey: vi.fn(),
      revokeApiKey: vi.fn(),
      createClinicAdmin: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [OrganizationListComponent, RouterTestingModule],
      providers: [
        { provide: OrganizationApiService, useValue: mockApi }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OrganizationListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load organizations on init', () => {
    expect(mockApi.list).toHaveBeenCalled();
    expect(component.list()).toEqual(mockOrgs);
    expect(component.loading()).toBe(false);
  });

  it('should call create organization api on submit', () => {
    component.name.set('Clinique de l\'Espoir');
    component.email.set('espoir@joprelys.local');
    component.city.set('Douala');
    component.country.set('Cameroun');
    component.type.set('CLINIC');
    component.responsibleName.set('Dr. Espoir');
    component.apiEnabled.set(true);

    mockApi.create.mockReturnValue(of({
      id: '2',
      name: 'Clinique de l\'Espoir',
      email: 'espoir@joprelys.local',
      city: 'Douala',
      status: 'ACTIVE',
      createdAt: '2026-07-02T12:00:00Z',
      country: 'Cameroun',
      type: 'CLINIC',
      responsibleName: 'Dr. Espoir',
      apiEnabled: true,
      logoPath: undefined
    }));

    component.submit();

    expect(mockApi.create).toHaveBeenCalledWith({
      name: 'Clinique de l\'Espoir',
      email: 'espoir@joprelys.local',
      phone: '',
      address: '',
      city: 'Douala',
      country: 'Cameroun',
      type: 'CLINIC',
      responsibleName: 'Dr. Espoir',
      apiEnabled: true
    });
    expect(component.list().length).toBe(2);
  });

  it('should display the normalized backend message when admin email delivery fails', () => {
    component.openAdminForm(mockOrgs[0]);
    component.adminDisplayName.set('Admin Clinique');
    component.adminEmail.set('admin@clinic.test');
    mockApi.createClinicAdmin.mockReturnValue(throwError(() => ({
      status: 503,
      error: { error: { code: 'MAIL_DELIVERY_UNAVAILABLE', message: 'Service e-mail indisponible.' } }
    })));

    component.submitAdmin();

    expect(component.adminFormError()).toBe('Service e-mail indisponible.');
    expect(component.adminFormLoading()).toBe(false);
  });
});
