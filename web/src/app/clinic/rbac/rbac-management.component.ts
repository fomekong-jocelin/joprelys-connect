import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { forkJoin, switchMap } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { AlertComponent } from '../../shared/ui/alert.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { RbacApiService } from './rbac-api.service';
import { RbacAuditEntry, RbacPermission, RbacRole, RbacUserAccess } from './rbac.models';

@Component({
  selector: 'app-rbac-management',
  standalone: true,
  imports: [CommonModule, AppShellComponent, AlertComponent, PageHeaderComponent],
  templateUrl: './rbac-management.component.html',
  styleUrl: './rbac-management.component.css',
})
export class RbacManagementComponent implements OnInit {
  private readonly api = inject(RbacApiService);
  private readonly i18n = inject(I18nService);

  readonly activeTab = signal<'users' | 'roles' | 'audit'>('users');
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

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
    this.loadWorkspace();
  }

  setTab(tab: 'users' | 'roles' | 'audit'): void {
    this.activeTab.set(tab);
    this.error.set(null);
    this.success.set(null);
    if (tab === 'audit' && this.auditEntries().length === 0) {
      this.loadAudit();
    }
  }

  loadWorkspace(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      roles: this.api.listRoles(),
      permissions: this.api.listPermissions(),
      users: this.api.listUsers(),
      access: this.api.ensureMyAccess(true),
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
    this.loading.set(true);
    this.api.listAudit().subscribe({
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
    this.api.replaceUserRoles(user.id, this.selectedUserRoleIds()).subscribe({
      next: (updated) => {
        this.users.update((users) => users.map((item) => item.id === updated.id ? updated : item));
        this.selectUser(updated);
        this.api.ensureMyAccess(true).subscribe();
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
    if (!selected) {
      this.api.createRole({
        code: this.roleCode().trim(),
        name: this.roleName().trim(),
        description: this.roleDescription().trim() || undefined,
        assignable: this.roleAssignable(),
        permissionCodes: this.selectedPermissionCodes(),
      }).subscribe({
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
    }).pipe(
      switchMap(() => this.api.replaceRolePermissions(selected.id, this.selectedPermissionCodes())),
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
