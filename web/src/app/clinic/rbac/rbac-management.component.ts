import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { forkJoin, switchMap } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { AlertComponent } from '../../shared/ui/alert.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { OrganizationApiService } from '../organizations/organization-api.service';
import { Organization } from '../organizations/organizations.models';
import { RbacApiService } from './rbac-api.service';
import { RbacAuditEntry, RbacPermission, RbacRole, RbacUserAccess } from './rbac.models';

const RBAC_ORGANIZATION_SCOPE_KEY = 'joprelys.rbac.organizationScope';

@Component({
  selector: 'app-rbac-management',
  standalone: true,
  imports: [CommonModule, AppShellComponent, AlertComponent, PageHeaderComponent],
  templateUrl: './rbac-management.component.html',
  styleUrl: './rbac-management.component.css',
})
export class RbacManagementComponent implements OnInit {
  private readonly api = inject(RbacApiService);
  private readonly organizationApi = inject(OrganizationApiService);
  private readonly i18n = inject(I18nService);

  readonly activeTab = signal<'users' | 'roles' | 'audit'>('users');
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  readonly platformAdministrator = signal(false);
  readonly organizations = signal<Organization[]>([]);
  readonly selectedOrganizationId = signal<string | null>(null);

  readonly roles = signal<RbacRole[]>([]);
  readonly permissions = signal<RbacPermission[]>([]);
  readonly users = signal<RbacUserAccess[]>([]);
  readonly auditEntries = signal<RbacAuditEntry[]>([]);

  readonly selectedUserId = signal<string | null>(null);
  readonly selectedUserRoleIds = signal<string[]>([]);
  readonly selectedRoleId = signal<string | null>(null);
  readonly selectedPermissionCodes = signal<string[]>([]);

  readonly roleCode = signal('');
  readonly roleName = signal('');
  readonly roleDescription = signal('');
  readonly roleAssignable = signal(true);
  readonly roleEnabled = signal(true);

  readonly selectedOrganization = computed(() =>
    this.organizations().find((organization) => organization.id === this.selectedOrganizationId()) ?? null,
  );
  readonly selectedUser = computed(() =>
    this.users().find((user) => user.id === this.selectedUserId()) ?? null,
  );
  readonly selectedRole = computed(() =>
    this.roles().find((role) => role.id === this.selectedRoleId()) ?? null,
  );
  readonly assignableRoles = computed(() =>
    this.roles().filter((role) => role.assignable && role.enabled),
  );
  readonly groupedPermissions = computed(() => {
    const groups = new Map<string, RbacPermission[]>();
    for (const permission of this.permissions()) {
      const items = groups.get(permission.domain) ?? [];
      items.push(permission);
      groups.set(permission.domain, items);
    }
    return [...groups.entries()].map(([domain, items]) => ({ domain, items }));
  });
  readonly isEditingSystemRole = computed(() => this.selectedRole()?.systemRole ?? false);

  ngOnInit(): void {
    this.bootstrapWorkspace();
  }

  setTab(tab: 'users' | 'roles' | 'audit'): void {
    this.activeTab.set(tab);
    this.error.set(null);
    this.success.set(null);
    if (tab === 'audit' && this.auditEntries().length === 0) {
      this.loadAudit();
    }
  }

  selectOrganization(organizationId: string): void {
    if (!organizationId || organizationId === this.selectedOrganizationId()) return;
    this.selectedOrganizationId.set(organizationId);
    try {
      sessionStorage.setItem(RBAC_ORGANIZATION_SCOPE_KEY, organizationId);
    } catch {
      // Storage can be unavailable in hardened browsers or SSR tests.
    }
    this.resetWorkspaceSelections();
    this.loadWorkspace();
  }

  loadWorkspace(): void {
    const organizationId = this.scopeOrganizationId();
    if (this.platformAdministrator() && !organizationId) {
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      roles: this.api.listRoles(organizationId),
      permissions: this.api.listPermissions(),
      users: this.api.listUsers(organizationId),
    }).subscribe({
      next: ({ roles, permissions, users }) => {
        this.roles.set(roles);
        this.permissions.set(permissions);
        this.users.set(users);
        if (!this.selectedUserId() && users.length > 0) {
          this.selectUser(users[0]);
        }
        if (!this.selectedRoleId() && roles.length > 0) {
          this.selectRole(roles[0]);
        }
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(this.errorMessage(error, this.t('rbac.loadError', 'Impossible de charger les droits d’accès.')));
        this.loading.set(false);
      },
    });
  }

  loadAudit(): void {
    const organizationId = this.scopeOrganizationId();
    if (this.platformAdministrator() && !organizationId) return;

    this.loading.set(true);
    this.api.listAudit(organizationId).subscribe({
      next: (entries) => {
        this.auditEntries.set(entries);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(this.errorMessage(error, this.t('rbac.auditLoadError', 'Impossible de charger le journal RBAC.')));
        this.loading.set(false);
      },
    });
  }

  selectUser(user: RbacUserAccess): void {
    this.selectedUserId.set(user.id);
    this.selectedUserRoleIds.set(
      this.roles().filter((role) => user.roles.includes(role.code)).map((role) => role.id),
    );
    this.error.set(null);
    this.success.set(null);
  }

  toggleUserRole(roleId: string): void {
    if (this.selectedUser()?.currentUser) return;
    this.selectedUserRoleIds.update((current) =>
      current.includes(roleId) ? current.filter((id) => id !== roleId) : [...current, roleId],
    );
  }

  saveUserRoles(): void {
    const user = this.selectedUser();
    if (!user || user.currentUser || this.selectedUserRoleIds().length === 0) return;
    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
    this.api.replaceUserRoles(user.id, this.selectedUserRoleIds(), this.scopeOrganizationId()).subscribe({
      next: (updated) => {
        this.users.update((users) => users.map((item) => item.id === updated.id ? updated : item));
        this.selectUser(updated);
        this.success.set(this.t('rbac.userRolesSaved', 'Les rôles de l’utilisateur ont été mis à jour.'));
        this.saving.set(false);
      },
      error: (error) => {
        this.error.set(this.errorMessage(error, this.t('rbac.saveError', 'La mise à jour des rôles a échoué.')));
        this.saving.set(false);
      },
    });
  }

  newRole(): void {
    this.selectedRoleId.set(null);
    this.roleCode.set('');
    this.roleName.set('');
    this.roleDescription.set('');
    this.roleAssignable.set(true);
    this.roleEnabled.set(true);
    this.selectedPermissionCodes.set([]);
    this.error.set(null);
    this.success.set(null);
  }

  selectRole(role: RbacRole): void {
    this.selectedRoleId.set(role.id);
    this.roleCode.set(role.code);
    this.roleName.set(role.name);
    this.roleDescription.set(role.description ?? '');
    this.roleAssignable.set(role.assignable);
    this.roleEnabled.set(role.enabled);
    this.selectedPermissionCodes.set([...role.permissions]);
    this.error.set(null);
    this.success.set(null);
  }

  togglePermission(permissionCode: string): void {
    if (this.isEditingSystemRole()) return;
    this.selectedPermissionCodes.update((current) =>
      current.includes(permissionCode)
        ? current.filter((code) => code !== permissionCode)
        : [...current, permissionCode],
    );
  }

  saveRole(): void {
    if (!this.roleCode().trim() || !this.roleName().trim() || this.isEditingSystemRole()) return;
    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
    const selected = this.selectedRole();
    const organizationId = this.scopeOrganizationId();
    if (!selected) {
      this.api.createRole({
        code: this.roleCode().trim(),
        name: this.roleName().trim(),
        description: this.roleDescription().trim() || undefined,
        assignable: this.roleAssignable(),
        permissionCodes: this.selectedPermissionCodes(),
      }, organizationId).subscribe({
        next: (created) => this.finishRoleSave(created),
        error: (error) => this.failRoleSave(error),
      });
      return;
    }

    this.api.updateRole(selected.id, {
      code: this.roleCode().trim(),
      name: this.roleName().trim(),
      description: this.roleDescription().trim() || undefined,
      assignable: this.roleAssignable(),
      enabled: this.roleEnabled(),
    }, organizationId).pipe(
      switchMap(() => this.api.replaceRolePermissions(
        selected.id,
        this.selectedPermissionCodes(),
        organizationId,
      )),
    ).subscribe({
      next: (updated) => this.finishRoleSave(updated),
      error: (error) => this.failRoleSave(error),
    });
  }

  permissionChecked(code: string): boolean {
    return this.selectedPermissionCodes().includes(code);
  }

  userRoleChecked(roleId: string): boolean {
    return this.selectedUserRoleIds().includes(roleId);
  }

  roleSummary(user: RbacUserAccess): string {
    return user.roles.length > 0 ? user.roles.join(' · ') : this.t('rbac.noRole', 'Aucun rôle');
  }

  t(key: string, fallback: string): string {
    return this.i18n.t(key, fallback);
  }

  private bootstrapWorkspace(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.ensureMyAccess(true).subscribe({
      next: (access) => {
        const platform = access.roles.some((role) => role === 'ADMIN_JOPRELYS' || role === 'SUPER_ADMIN');
        this.platformAdministrator.set(platform);
        if (platform) {
          this.loadOrganizations();
        } else {
          this.loadWorkspace();
        }
      },
      error: (error) => {
        this.error.set(this.errorMessage(error, this.t('rbac.loadError', 'Impossible de charger les droits d’accès.')));
        this.loading.set(false);
      },
    });
  }

  private loadOrganizations(): void {
    this.organizationApi.list().subscribe({
      next: (organizations) => {
        const sorted = [...organizations].sort((left, right) => {
          if (left.status !== right.status) return left.status === 'ACTIVE' ? -1 : 1;
          return left.name.localeCompare(right.name);
        });
        this.organizations.set(sorted);
        const rememberedId = this.readRememberedOrganizationId();
        const selected = sorted.find((organization) => organization.id === rememberedId)
          ?? sorted.find((organization) => organization.status === 'ACTIVE')
          ?? sorted[0]
          ?? null;
        this.selectedOrganizationId.set(selected?.id ?? null);
        if (selected) {
          this.loadWorkspace();
        } else {
          this.error.set(this.t('rbac.noOrganization', 'Aucun établissement n’est disponible pour l’administration des droits.'));
          this.loading.set(false);
        }
      },
      error: (error) => {
        this.error.set(this.errorMessage(error, this.t('rbac.organizationLoadError', 'Impossible de charger les établissements.')));
        this.loading.set(false);
      },
    });
  }

  private readRememberedOrganizationId(): string | null {
    try {
      return sessionStorage.getItem(RBAC_ORGANIZATION_SCOPE_KEY);
    } catch {
      return null;
    }
  }

  private scopeOrganizationId(): string | undefined {
    return this.platformAdministrator() ? this.selectedOrganizationId() ?? undefined : undefined;
  }

  private resetWorkspaceSelections(): void {
    this.roles.set([]);
    this.users.set([]);
    this.auditEntries.set([]);
    this.selectedUserId.set(null);
    this.selectedUserRoleIds.set([]);
    this.selectedRoleId.set(null);
    this.selectedPermissionCodes.set([]);
    this.newRole();
  }

  private finishRoleSave(role: RbacRole): void {
    this.roles.update((roles) => {
      const exists = roles.some((item) => item.id === role.id);
      return exists ? roles.map((item) => item.id === role.id ? role : item) : [...roles, role];
    });
    this.selectRole(role);
    this.success.set(this.t('rbac.roleSaved', 'Le rôle et ses permissions ont été enregistrés.'));
    this.saving.set(false);
  }

  private failRoleSave(error: unknown): void {
    this.error.set(this.errorMessage(error, this.t('rbac.saveError', 'L’enregistrement a échoué.')));
    this.saving.set(false);
  }

  private errorMessage(error: unknown, fallback: string): string {
    const candidate = error as { error?: { detail?: string; message?: string } };
    return candidate?.error?.detail ?? candidate?.error?.message ?? fallback;
  }
}
