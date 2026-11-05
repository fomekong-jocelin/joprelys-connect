import { Component, computed, inject, input, output, signal } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';
import { interpolate } from '../../core/i18n/interpolate';
import { AlertComponent } from '../../shared/ui/alert.component';
import { RbacPermission, RbacRole, RbacUserAccess } from './rbac.models';
import { effectivePermissionGroups, matchesUser, roleIdsOf, roleNamesOf, sameMembers } from './rbac-view';

export interface UserRolesSaveEvent {
  readonly userId: string;
  readonly roleIds: string[];
}

/** Onglet « Personnes » : trouver quelqu'un, cocher ses rôles, voir ce qu'il peut faire. */
@Component({
  selector: 'app-rbac-users-panel',
  standalone: true,
  imports: [AlertComponent],
  templateUrl: './rbac-users-panel.component.html',
})
export class RbacUsersPanelComponent {
  private readonly i18n = inject(I18nService);

  readonly users = input.required<readonly RbacUserAccess[]>();
  readonly roles = input.required<readonly RbacRole[]>();
  readonly permissions = input.required<readonly RbacPermission[]>();
  readonly saving = input(false);
  readonly save = output<UserRolesSaveEvent>();

  readonly t = (key: string) => this.i18n.t(key);
  readonly query = signal('');
  private readonly selectedUserId = signal<string | null>(null);
  /** Rôles cochés et pas encore enregistrés ; `null` = ceux du serveur. */
  private readonly draftRoleIds = signal<string[] | null>(null);

  readonly filteredUsers = computed(() => this.users().filter((user) => matchesUser(user, this.query())));
  readonly selectedUser = computed(() =>
    this.users().find((user) => user.id === this.selectedUserId()) ?? this.users()[0] ?? null);
  readonly assignableRoles = computed(() => this.roles().filter((role) => role.assignable && role.enabled));
  readonly savedRoleIds = computed(() => roleIdsOf(this.selectedUser(), this.roles()));
  readonly checkedRoleIds = computed(() => this.draftRoleIds() ?? this.savedRoleIds());
  readonly dirty = computed(() => !sameMembers(this.checkedRoleIds(), this.savedRoleIds()));
  readonly readOnly = computed(() => !!this.selectedUser()?.currentUser);
  readonly canSave = computed(() => this.dirty() && !this.readOnly() && !this.saving() && this.checkedRoleIds().length > 0);
  readonly permissionGroups = computed(() =>
    effectivePermissionGroups(this.selectedUser()?.permissions ?? [], this.permissions()));
  readonly permissionCount = computed(() => this.selectedUser()?.permissions.length ?? 0);

  select(user: RbacUserAccess): void {
    this.selectedUserId.set(user.id);
    this.draftRoleIds.set(null);
  }

  toggleRole(roleId: string): void {
    if (this.readOnly()) return;
    const current = this.checkedRoleIds();
    this.draftRoleIds.set(current.includes(roleId) ? current.filter((id) => id !== roleId) : [...current, roleId]);
  }

  submit(): void {
    const user = this.selectedUser();
    if (!user || !this.canSave()) return;
    this.save.emit({ userId: user.id, roleIds: this.checkedRoleIds() });
  }

  roleSummary(user: RbacUserAccess): string {
    const names = roleNamesOf(user, this.roles());
    return names.length > 0 ? names.join(' · ') : this.t('rbac.noRole');
  }

  domainLabel(domain: string): string {
    return this.i18n.t(`rbac.domain.${domain}`, domain);
  }

  permissionsSummary(): string {
    return interpolate(this.t('rbac.users.effectivePermissions'), { count: this.permissionCount() });
  }
}
