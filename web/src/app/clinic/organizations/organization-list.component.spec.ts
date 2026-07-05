import { TestBed, ComponentFixture } from '@angular/core/testing';
import { OrganizationListComponent } from './organization-list.component';
import { OrganizationApiService } from './organization-api.service';
import { of } from 'rxjs';
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
      revokeApiKey: vi.fn()
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
      apiEnabled: true
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
});
