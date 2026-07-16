import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { RbacApiService } from '../rbac/rbac-api.service';
import { RbacRole } from '../rbac/rbac.models';
import { StaffApiService } from './staff-api.service';
import { StaffManagementComponent } from './staff-management.component';
import { StaffMember } from './staff.models';

describe('StaffManagementComponent', () => {
  let component: StaffManagementComponent;
  let fixture: ComponentFixture<StaffManagementComponent>;
  let mockApi: {
    list: ReturnType<typeof vi.fn>;
    invite: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    toggleStatus: ReturnType<typeof vi.fn>;
  };
  let mockRbacApi: { listRoles: ReturnType<typeof vi.fn> };

  const roles: RbacRole[] = [
    {
      id: 'role-doctor',
      code: 'MEDECIN',
      name: 'Médecin',
      systemRole: true,
      assignable: true,
      enabled: true,
      permissions: ['CLINICAL_READ'],
    },
    {
      id: 'role-cashier',
      code: 'CAISSIER',
      name: 'Caissier',
      systemRole: true,
      assignable: true,
      enabled: true,
      permissions: ['CASH_PAYMENT_COLLECT'],
    },
    {
      id: 'role-daf',
      code: 'DAF',
      name: 'Directeur administratif et financier',
      systemRole: true,
      assignable: true,
      enabled: true,
      permissions: ['ACCOUNTING_DASHBOARD_READ'],
    },
    {
      id: 'role-custom',
      code: 'SUPERVISEUR_CAISSE',
      name: 'Superviseur caisse',
      systemRole: false,
      assignable: true,
      enabled: true,
      permissions: ['CASH_QUEUE_READ'],
    },
  ];

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

  beforeEach(async () => {
    mockApi = {
      list: vi.fn().mockReturnValue(of(staff)),
      invite: vi.fn(),
      update: vi.fn(),
      toggleStatus: vi.fn(),
    };
    mockRbacApi = {
      listRoles: vi.fn().mockReturnValue(of(roles)),
    };

    await TestBed.configureTestingModule({
      imports: [StaffManagementComponent],
      providers: [
        provideRouter([]),
        { provide: StaffApiService, useValue: mockApi },
        { provide: RbacApiService, useValue: mockRbacApi },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(StaffManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load staff and every assignable RBAC role on init', () => {
    expect(mockApi.list).toHaveBeenCalled();
    expect(mockRbacApi.listRoles).toHaveBeenCalled();
    expect(component.staff()).toEqual(staff);
    expect(component.roles().map((role) => role.code)).toEqual([
      'CAISSIER',
      'DAF',
      'MEDECIN',
      'SUPERVISEUR_CAISSE',
    ]);
    expect(component.loading()).toBe(false);
  });

  it('should invite staff with multiple system and custom roles', () => {
    component.displayName.set('Responsable Finance');
    component.email.set('finance@joprelys.local');
    component.selectedRoles.set(['DAF', 'CAISSIER', 'SUPERVISEUR_CAISSE']);
    mockApi.invite.mockReturnValue(of({
      ...staff[0],
      id: 'staff-2',
      email: 'finance@joprelys.local',
      displayName: 'Responsable Finance',
      role: 'DAF,CAISSIER,SUPERVISEUR_CAISSE',
    }));

    component.submitForm();

    expect(mockApi.invite).toHaveBeenCalledWith({
      displayName: 'Responsable Finance',
      email: 'finance@joprelys.local',
      roles: ['DAF', 'CAISSIER', 'SUPERVISEUR_CAISSE'],
    });
    expect(component.staff().length).toBe(2);
  });

  it('should update the effective roles of a selected staff member', () => {
    component.startEdit(staff[0]);
    component.displayName.set('Dr Alpha Senior');
    component.selectedRoles.set(['MEDECIN', 'SUPERVISEUR_CAISSE']);
    mockApi.update.mockReturnValue(of({
      ...staff[0],
      displayName: 'Dr Alpha Senior',
      role: 'MEDECIN,SUPERVISEUR_CAISSE',
    }));

    component.submitForm();

    expect(mockApi.update).toHaveBeenCalledWith('staff-1', expect.objectContaining({
      displayName: 'Dr Alpha Senior',
      roles: ['MEDECIN', 'SUPERVISEUR_CAISSE'],
    }));
    expect(component.staff()[0].role).toBe('MEDECIN,SUPERVISEUR_CAISSE');
  });

  it('should toggle staff status', () => {
    mockApi.toggleStatus.mockReturnValue(of({ ...staff[0], enabled: false }));

    component.toggleStatus(staff[0]);

    expect(mockApi.toggleStatus).toHaveBeenCalledWith('staff-1');
    expect(component.staff()[0].enabled).toBe(false);
  });
});
