import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { OrganizationApiService } from '../organizations/organization-api.service';
import { Organization } from '../organizations/organizations.models';
import { RbacApiService } from './rbac-api.service';
import { RbacManagementComponent } from './rbac-management.component';
import { RbacPermission, RbacRole, RbacUserAccess } from './rbac.models';

describe('RbacManagementComponent', () => {
  let component: RbacManagementComponent;
  let api: {
    listRoles: ReturnType<typeof vi.fn>;
    listPermissions: ReturnType<typeof vi.fn>;
    listUsers: ReturnType<typeof vi.fn>;
    ensureMyAccess: ReturnType<typeof vi.fn>;
    listAudit: ReturnType<typeof vi.fn>;
    replaceUserRoles: ReturnType<typeof vi.fn>;
    createRole: ReturnType<typeof vi.fn>;
    updateRole: ReturnType<typeof vi.fn>;
    replaceRolePermissions: ReturnType<typeof vi.fn>;
  };
  let organizationApi: { list: ReturnType<typeof vi.fn> };

  const permissions: RbacPermission[] = [
    { code: 'CASH_QUEUE_READ', domain: 'CAISSE', name: 'Consulter la file' },
    { code: 'CASH_PAYMENT_COLLECT', domain: 'CAISSE', name: 'Encaisser' },
  ];

  const roles: RbacRole[] = [
    {
      id: 'role-admin',
      code: 'ADMIN_CLINIQUE',
      name: 'Administrateur clinique',
      systemRole: true,
      assignable: true,
      enabled: true,
      permissions: ['RBAC_MANAGE'],
    },
    {
      id: 'role-cashier',
      code: 'CAISSIER',
      name: 'Caissier',
      systemRole: true,
      assignable: true,
      enabled: true,
      permissions: ['CASH_QUEUE_READ', 'CASH_PAYMENT_COLLECT'],
    },
  ];

  const users: RbacUserAccess[] = [
    {
      id: 'user-admin',
      email: 'admin@test.local',
      displayName: 'Admin clinique',
      enabled: true,
      currentUser: true,
      roles: ['ADMIN_CLINIQUE'],
      permissions: ['RBAC_MANAGE'],
    },
    {
      id: 'user-cashier',
      email: 'cashier@test.local',
      displayName: 'Caissier test',
      enabled: true,
      currentUser: false,
      roles: ['CAISSIER'],
      permissions: ['CASH_QUEUE_READ', 'CASH_PAYMENT_COLLECT'],
    },
  ];

  const organization: Organization = {
    id: 'org-clinic-1',
    name: 'Clinique Saint Jean',
    email: 'contact@saint-jean.test',
    city: 'Douala',
    status: 'ACTIVE',
    createdAt: '2026-07-11T00:00:00Z',
    country: 'Cameroun',
    type: 'CLINIC',
    responsibleName: 'Direction',
    apiEnabled: true,
  };

  beforeEach(() => {
    sessionStorage.clear();
    api = {
      listRoles: vi.fn(() => of(roles)),
      listPermissions: vi.fn(() => of(permissions)),
      listUsers: vi.fn(() => of(users)),
      ensureMyAccess: vi.fn(() => of({ userId: 'user-admin', roles: ['ADMIN_CLINIQUE'], permissions: ['RBAC_MANAGE'] })),
      listAudit: vi.fn(() => of([])),
      replaceUserRoles: vi.fn(),
      createRole: vi.fn(),
      updateRole: vi.fn(),
      replaceRolePermissions: vi.fn(),
    };
    organizationApi = { list: vi.fn(() => of([organization])) };

    TestBed.configureTestingModule({
      imports: [RbacManagementComponent],
      providers: [
        { provide: RbacApiService, useValue: api },
        { provide: OrganizationApiService, useValue: organizationApi },
        { provide: I18nService, useValue: { t: (_key: string, fallback?: string) => fallback ?? _key } },
      ],
    });
    TestBed.overrideComponent(RbacManagementComponent, { set: { template: '' } });

    component = TestBed.createComponent(RbacManagementComponent).componentInstance;
  });

  it('should load roles, permissions and users with an initial selection', () => {
    component.loadWorkspace();

    expect(component.roles()).toEqual(roles);
    expect(component.permissions()).toEqual(permissions);
    expect(component.users()).toEqual(users);
    expect(component.selectedUserId()).toBe('user-admin');
    expect(component.selectedRoleId()).toBe('role-admin');
    expect(component.loading()).toBe(false);
  });

  it('should let a platform administrator select the clinic scope', () => {
    api.ensureMyAccess.mockReturnValue(of({
      userId: 'platform-admin',
      roles: ['ADMIN_JOPRELYS'],
      permissions: ['RBAC_MANAGE'],
    }));

    component.ngOnInit();

    expect(component.platformAdministrator()).toBe(true);
    expect(component.selectedOrganizationId()).toBe(organization.id);
    expect(api.listRoles).toHaveBeenCalledWith(organization.id);
    expect(api.listUsers).toHaveBeenCalledWith(organization.id);
  });

  it('should prevent changing the roles of the current user in the UI', () => {
    component.roles.set(roles);
    component.users.set(users);
    component.selectUser(users[0]);
    const before = component.selectedUserRoleIds();

    component.toggleUserRole('role-cashier');
    component.saveUserRoles();

    expect(component.selectedUserRoleIds()).toEqual(before);
    expect(api.replaceUserRoles).not.toHaveBeenCalled();
  });

  it('should create a custom role with the selected permissions', () => {
    const created: RbacRole = {
      id: 'role-custom',
      code: 'CAISSE_SUPERVISEUR',
      name: 'Superviseur caisse',
      description: 'Contrôle la caisse.',
      systemRole: false,
      assignable: true,
      enabled: true,
      permissions: ['CASH_QUEUE_READ'],
    };
    api.createRole.mockReturnValue(of(created));
    component.roles.set(roles);
    component.newRole();
    component.roleCode.set('CAISSE_SUPERVISEUR');
    component.roleName.set('Superviseur caisse');
    component.roleDescription.set('Contrôle la caisse.');
    component.selectedPermissionCodes.set(['CASH_QUEUE_READ']);

    component.saveRole();

    expect(api.createRole).toHaveBeenCalledWith({
      code: 'CAISSE_SUPERVISEUR',
      name: 'Superviseur caisse',
      description: 'Contrôle la caisse.',
      assignable: true,
      permissionCodes: ['CASH_QUEUE_READ'],
    }, undefined);
    expect(component.roles()).toContainEqual(created);
    expect(component.selectedRoleId()).toBe('role-custom');
    expect(component.saving()).toBe(false);
  });

  it('should expose a readable error when workspace loading fails', () => {
    api.listRoles.mockReturnValue(throwError(() => ({ error: { detail: 'Accès refusé' } })));

    component.loadWorkspace();

    expect(component.error()).toBe('Accès refusé');
    expect(component.loading()).toBe(false);
  });
});
