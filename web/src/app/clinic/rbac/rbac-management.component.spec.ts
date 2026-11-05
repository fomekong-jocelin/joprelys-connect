import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { RBAC_PERMISSIONS, RBAC_ROLES, RBAC_USERS, rbacI18nStub } from '../../../testing/rbac-fixtures';
import { OrganizationApiService } from '../organizations/organization-api.service';
import { Organization } from '../organizations/organizations.models';
import { RbacApiService } from './rbac-api.service';
import { RbacManagementComponent } from './rbac-management.component';
import { RbacRole } from './rbac.models';

describe('RbacManagementComponent', () => {
  let fixture: ComponentFixture<RbacManagementComponent>;
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
  let route: { snapshot: { queryParamMap: ReturnType<typeof convertToParamMap> } };
  let i18n: ReturnType<typeof rbacI18nStub>;

  const organization: Organization = {
    id: 'org-clinic-1',
    name: 'Clinique de test',
    email: 'contact@clinique.test',
    city: 'Douala',
    status: 'ACTIVE',
    createdAt: '2026-07-11T00:00:00Z',
    country: 'Cameroun',
    type: 'CLINIC',
    responsibleName: 'Direction',
    apiEnabled: true,
  };
  const custom = RBAC_ROLES[2];

  beforeEach(() => {
    sessionStorage.clear();
    api = {
      listRoles: vi.fn(() => of(RBAC_ROLES)),
      listPermissions: vi.fn(() => of(RBAC_PERMISSIONS)),
      listUsers: vi.fn(() => of(RBAC_USERS)),
      ensureMyAccess: vi.fn(() => of({ userId: 'user-admin', roles: ['ADMIN_CLINIQUE'], permissions: ['RBAC_MANAGE'] })),
      listAudit: vi.fn(() => of([])),
      replaceUserRoles: vi.fn(),
      createRole: vi.fn(),
      updateRole: vi.fn(),
      replaceRolePermissions: vi.fn(),
    };
    organizationApi = { list: vi.fn(() => of([organization])) };
    route = { snapshot: { queryParamMap: convertToParamMap({}) } };
    i18n = rbacI18nStub();

    TestBed.configureTestingModule({
      imports: [RbacManagementComponent],
      providers: [
        { provide: RbacApiService, useValue: api },
        { provide: ActivatedRoute, useValue: route },
        { provide: OrganizationApiService, useValue: organizationApi },
        { provide: I18nService, useValue: i18n },
      ],
    });
    TestBed.overrideComponent(RbacManagementComponent, {
      set: { imports: [], template: '' },
    });
    fixture = TestBed.createComponent(RbacManagementComponent);
    component = fixture.componentInstance;
  });

  it('loads the requested audit tab once after resolving the workspace', () => {
    route.snapshot.queryParamMap = convertToParamMap({ tab: 'audit' });
    component.ngOnInit();

    expect(component.activeTab()).toBe('audit');
    expect(api.listAudit).toHaveBeenCalledTimes(1);
    expect(component.loading()).toBe(false);
  });

  it('loads roles, permissions and people of the facility', () => {
    component.ngOnInit();

    expect(component.roles()).toEqual(RBAC_ROLES);
    expect(component.permissions()).toEqual(RBAC_PERMISSIONS);
    expect(component.users()).toEqual(RBAC_USERS);
    expect(api.listRoles).toHaveBeenCalledWith(undefined);
    expect(component.loading()).toBe(false);
  });

  it('lets a platform administrator choose the facility scope', () => {
    api.ensureMyAccess.mockReturnValue(of({
      userId: 'platform-admin',
      roles: ['ADMIN_JOPRELYS'],
      permissions: ['RBAC_MANAGE', 'ORGANIZATION_MANAGE'],
    }));

    component.ngOnInit();

    expect(component.platformAdministrator()).toBe(true);
    expect(component.selectedOrganizationId()).toBe(organization.id);
    expect(api.listRoles).toHaveBeenCalledWith(organization.id);
    expect(api.listUsers).toHaveBeenCalledWith(organization.id);
  });

  it('saves the roles of a person and confirms it', () => {
    const updated = { ...RBAC_USERS[1], roles: ['CAISSIER', 'ADMIN_CLINIQUE'] };
    api.replaceUserRoles.mockReturnValue(of(updated));
    component.ngOnInit();

    component.saveUserRoles({ userId: 'user-cashier', roleIds: ['role-cashier', 'role-admin'] });

    expect(api.replaceUserRoles).toHaveBeenCalledWith('user-cashier', ['role-cashier', 'role-admin'], undefined);
    expect(component.users()[1]).toEqual(updated);
    expect(component.success()).toBe('rbac.userRolesSaved');
    expect(component.saving()).toBe(false);
  });

  it('creates a role and adds it to the list', () => {
    const created: RbacRole = { ...custom, id: 'role-new', code: 'AGENT_ACCUEIL', name: 'Agent d’accueil' };
    api.createRole.mockReturnValue(of(created));
    component.ngOnInit();
    const request = { code: 'AGENT_ACCUEIL', name: 'Agent d’accueil', assignable: true, permissionCodes: ['CASH_QUEUE_READ'] };

    component.saveRole({ id: null, request });

    expect(api.createRole).toHaveBeenCalledWith(request, undefined);
    expect(component.roles()).toContainEqual(created);
    expect(component.success()).toBe('rbac.roleSaved');
  });

  it('updates a role then replaces its permissions', () => {
    const changed: RbacRole = { ...custom, permissions: ['CASH_QUEUE_READ', 'CASH_PAYMENT_COLLECT'] };
    api.updateRole.mockReturnValue(of(custom));
    api.replaceRolePermissions.mockReturnValue(of(changed));
    component.ngOnInit();
    const request = { code: custom.code, name: custom.name, assignable: true, enabled: true };

    component.saveRole({ id: custom.id, request, permissionCodes: changed.permissions });

    expect(api.updateRole).toHaveBeenCalledWith(custom.id, request, undefined);
    expect(api.replaceRolePermissions).toHaveBeenCalledWith(custom.id, changed.permissions, undefined);
    expect(component.roles().find((role) => role.id === custom.id)).toEqual(changed);
  });

  it('keeps the input and explains a refused change', () => {
    api.replaceUserRoles.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 403,
      error: { detail: 'Un administrateur ne peut pas modifier ses propres rôles.' },
    })));
    component.ngOnInit();

    component.saveUserRoles({ userId: 'user-cashier', roleIds: ['role-admin'] });

    expect(component.error()).toBe('rbac.forbidden Un administrateur ne peut pas modifier ses propres rôles.');
    expect(component.users()).toEqual(RBAC_USERS);
    expect(component.saving()).toBe(false);
  });

  it('shows the translated load error without the French server detail in English', () => {
    i18n.locale.set('en');
    api.listRoles.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500, error: { detail: 'Erreur interne' } })));

    component.ngOnInit();

    expect(component.error()).toBe('rbac.loadError');
    expect(component.loading()).toBe(false);
  });

  it('moves between tabs with the arrow keys', () => {
    component.ngOnInit();
    const tablist = document.createElement('div');
    const event = new KeyboardEvent('keydown', { key: 'ArrowLeft', cancelable: true });
    Object.defineProperty(event, 'currentTarget', { value: tablist });

    component.moveTab(event);

    expect(component.activeTab()).toBe('audit');
    expect(event.defaultPrevented).toBe(true);
    expect(api.listAudit).toHaveBeenCalledTimes(1);
  });
});
