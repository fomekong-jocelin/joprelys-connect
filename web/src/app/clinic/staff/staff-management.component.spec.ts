import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { StaffApiService } from './staff-api.service';
import { StaffManagementComponent } from './staff-management.component';
import { StaffMember, StaffRoleDefinition } from './staff.models';

describe('StaffManagementComponent', () => {
  let component: StaffManagementComponent;
  let fixture: ComponentFixture<StaffManagementComponent>;
  let mockApi: {
    list: ReturnType<typeof vi.fn>;
    listRoles: ReturnType<typeof vi.fn>;
    invite: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    toggleStatus: ReturnType<typeof vi.fn>;
  };

  const staff: StaffMember[] = [
    {
      id: 'staff-1',
      email: 'medecin@joprelys.local',
      displayName: 'Dr Alpha',
      role: 'MEDECIN',
      enabled: true,
      createdAt: '2026-07-02T12:00:00Z',
    },
  ];

  const roles: StaffRoleDefinition[] = [
    {
      code: 'DAF',
      labelKey: 'staff.roles.DAF',
      descriptionKey: 'staff.roleDescriptions.DAF',
      category: 'FINANCE',
      sensitive: true,
    },
    {
      code: 'CAISSIER',
      labelKey: 'staff.roles.CAISSIER',
      descriptionKey: 'staff.roleDescriptions.CAISSIER',
      category: 'FINANCE',
      sensitive: false,
    },
    {
      code: 'MEDECIN',
      labelKey: 'staff.roles.MEDECIN',
      descriptionKey: 'staff.roleDescriptions.MEDECIN',
      category: 'CLINICAL',
      sensitive: false,
    },
    {
      code: 'PHARMACIEN',
      labelKey: 'staff.roles.PHARMACIEN',
      descriptionKey: 'staff.roleDescriptions.PHARMACIEN',
      category: 'CLINICAL',
      sensitive: false,
    },
  ];

  beforeEach(async () => {
    mockApi = {
      list: vi.fn().mockReturnValue(of(staff)),
      listRoles: vi.fn().mockReturnValue(of(roles)),
      invite: vi.fn(),
      update: vi.fn(),
      toggleStatus: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [StaffManagementComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        { provide: StaffApiService, useValue: mockApi },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(StaffManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load staff and the RBAC role catalogue on init', () => {
    expect(mockApi.list).toHaveBeenCalled();
    expect(mockApi.listRoles).toHaveBeenCalled();
    expect(component.staff()).toEqual(staff);
    expect(component.roleDefinitions()).toEqual(roles);
    expect(component.loading()).toBe(false);
  });

  it('should allow several roles and preserve catalogue order', () => {
    component.selectedRoles.set(['MEDECIN']);

    component.toggleRole('CAISSIER');
    component.toggleRole('DAF');

    expect(component.selectedRoles()).toEqual(['DAF', 'CAISSIER', 'MEDECIN']);
    expect(component.hasSelectedRole('DAF')).toBe(true);
  });

  it('should invite a staff member with several roles', () => {
    component.displayName.set('Responsable finance');
    component.email.set('finance@joprelys.local');
    component.selectedRoles.set(['DAF', 'CAISSIER']);
    mockApi.invite.mockReturnValue(of({
      ...staff[0],
      id: 'staff-2',
      email: 'finance@joprelys.local',
      displayName: 'Responsable finance',
      role: 'DAF,CAISSIER',
      temporaryPassword: 'Jop-ABC123',
    }));

    component.submitForm();

    expect(mockApi.invite).toHaveBeenCalledWith({
      displayName: 'Responsable finance',
      email: 'finance@joprelys.local',
      role: 'DAF,CAISSIER',
    });
    expect(component.temporaryPassword()).toBe('Jop-ABC123');
    expect(component.staff().length).toBe(2);
  });

  it('should update selected staff member with several roles', () => {
    component.startEdit(staff[0]);
    component.displayName.set('Dr Alpha Senior');
    component.selectedRoles.set(['DAF', 'MEDECIN']);
    mockApi.update.mockReturnValue(of({
      ...staff[0],
      displayName: 'Dr Alpha Senior',
      role: 'DAF,MEDECIN',
    }));

    component.submitForm();

    expect(mockApi.update).toHaveBeenCalledWith('staff-1', expect.objectContaining({
      displayName: 'Dr Alpha Senior',
      role: 'DAF,MEDECIN',
    }));
    expect(component.staff()[0].role).toBe('DAF,MEDECIN');
  });

  it('should reject a form without any selected role', () => {
    component.displayName.set('Sans rôle');
    component.email.set('sans-role@joprelys.local');
    component.selectedRoles.set([]);

    component.submitForm();

    expect(mockApi.invite).not.toHaveBeenCalled();
    expect(component.formError()).toBeTruthy();
  });

  it('should toggle staff status', () => {
    mockApi.toggleStatus.mockReturnValue(of({ ...staff[0], enabled: false }));

    component.toggleStatus(staff[0]);

    expect(mockApi.toggleStatus).toHaveBeenCalledWith('staff-1');
    expect(component.staff()[0].enabled).toBe(false);
  });
});
