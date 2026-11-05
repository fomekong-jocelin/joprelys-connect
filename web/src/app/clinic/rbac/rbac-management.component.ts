import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Observable, forkJoin, switchMap } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { AlertComponent } from '../../shared/ui/alert.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { OrganizationApiService } from '../organizations/organization-api.service';
import { Organization } from '../organizations/organizations.models';
import { RbacApiService } from './rbac-api.service';
import { RbacAuditTableComponent } from './rbac-audit-table.component';
import { RbacAuditEntry, RbacPermission, RbacRole, RbacUserAccess } from './rbac.models';
import { RbacRoleEditorComponent, RoleSaveEvent } from './rbac-role-editor.component';
import { RbacUsersPanelComponent, UserRolesSaveEvent } from './rbac-users-panel.component';

const RBAC_ORGANIZATION_SCOPE_KEY = 'joprelys.rbac.organizationScope';
export const RBAC_TABS = ['users', 'roles', 'audit'] as const;
export type RbacTab = (typeof RBAC_TABS)[number];

/** Rôles et autorisations : la page charge et enregistre ; chaque onglet porte sa propre saisie. */
@Component({
  selector: 'app-rbac-management',
  standalone: true,
  imports: [AppShellComponent, AlertComponent, PageHeaderComponent, RbacUsersPanelComponent, RbacRoleEditorComponent, RbacAuditTableComponent],
  templateUrl: './rbac-management.component.html',
})
export class RbacManagementComponent implements OnInit {
  private readonly api = inject(RbacApiService);
  private readonly route = inject(ActivatedRoute, { optional: true });
  private readonly organizationApi = inject(OrganizationApiService);
  private readonly i18n = inject(I18nService);
  private readonly usersPanel = viewChild(RbacUsersPanelComponent);
  private readonly roleEditor = viewChild(RbacRoleEditorComponent);

  readonly t = (key: string) => this.i18n.t(key);
  readonly tabs = RBAC_TABS;
  readonly activeTab = signal<RbacTab>('users');
  readonly loading = signal(false);
  readonly auditLoading = signal(false);
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

  ngOnInit(): void {
    if (this.route?.snapshot.queryParamMap.get('tab') === 'audit') this.activeTab.set('audit');
    this.bootstrapWorkspace();
  }

  setTab(tab: RbacTab): void {
    this.activeTab.set(tab);
    this.clearMessages();
    if (tab === 'audit' && this.auditEntries().length === 0) this.loadAudit();
  }

  /** Flèches gauche/droite entre onglets (motif ARIA « tabs »). */
  moveTab(event: KeyboardEvent): void {
    const offset = event.key === 'ArrowRight' ? 1 : event.key === 'ArrowLeft' ? -1 : 0;
    if (!offset) return;
    event.preventDefault();
    const next = this.tabs[(this.tabs.indexOf(this.activeTab()) + offset + this.tabs.length) % this.tabs.length];
    this.setTab(next);
    (event.currentTarget as HTMLElement).querySelector<HTMLElement>(`#rbac-tab-${next}`)?.focus();
  }

  selectOrganization(organizationId: string): void {
    if (!organizationId || organizationId === this.selectedOrganizationId()) return;
    this.selectedOrganizationId.set(organizationId);
    try {
      sessionStorage.setItem(RBAC_ORGANIZATION_SCOPE_KEY, organizationId);
    } catch {
      // Le stockage peut être bloqué (navigation privée) : le choix vaut alors pour la page seulement.
    }
    this.roles.set([]);
    this.users.set([]);
    this.auditEntries.set([]);
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
        this.loading.set(false);
        if (this.activeTab() === 'audit') this.loadAudit();
      },
      error: (error: unknown) => {
        this.error.set(this.errorMessage(error, 'rbac.loadError'));
        this.loading.set(false);
      },
    });
  }

  loadAudit(): void {
    const organizationId = this.scopeOrganizationId();
    if (this.platformAdministrator() && !organizationId) return;
    this.auditLoading.set(true);
    this.api.listAudit(organizationId).subscribe({
      next: (entries) => {
        this.auditEntries.set(entries);
        this.auditLoading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(this.errorMessage(error, 'rbac.auditLoadError'));
        this.auditLoading.set(false);
      },
    });
  }

  saveUserRoles(event: UserRolesSaveEvent): void {
    this.run(this.api.replaceUserRoles(event.userId, event.roleIds, this.scopeOrganizationId()), (updated: RbacUserAccess) => {
      this.users.update((users) => users.map((user) => user.id === updated.id ? updated : user));
      this.usersPanel()?.select(updated);
      this.success.set(this.t('rbac.userRolesSaved'));
    });
  }

  saveRole(event: RoleSaveEvent): void {
    const organizationId = this.scopeOrganizationId();
    const request: Observable<RbacRole> = event.id === null
      ? this.api.createRole(event.request, organizationId)
      : this.api.updateRole(event.id, event.request, organizationId)
        .pipe(switchMap(() => this.api.replaceRolePermissions(event.id, event.permissionCodes, organizationId)));
    this.run(request, (role: RbacRole) => {
      this.roles.update((roles) => roles.some((item) => item.id === role.id)
        ? roles.map((item) => item.id === role.id ? role : item)
        : [...roles, role]);
      this.roleEditor()?.select(role);
      this.success.set(this.t('rbac.roleSaved'));
    });
  }

  showNotice(message: string): void {
    this.error.set(null);
    this.success.set(message);
  }

  private run<T>(request: Observable<T>, onSuccess: (result: T) => void): void {
    if (this.saving()) return;
    this.saving.set(true);
    this.clearMessages();
    request.subscribe({
      next: (result) => {
        this.saving.set(false);
        onSuccess(result);
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.error.set(this.errorMessage(error, 'rbac.saveError'));
      },
    });
  }

  private bootstrapWorkspace(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.ensureMyAccess(true).subscribe({
      next: (access) => {
        const platform = access.permissions.includes('ORGANIZATION_MANAGE');
        this.platformAdministrator.set(platform);
        if (platform) this.loadOrganizations();
        else this.loadWorkspace();
      },
      error: (error: unknown) => {
        this.error.set(this.errorMessage(error, 'rbac.loadError'));
        this.loading.set(false);
      },
    });
  }

  private loadOrganizations(): void {
    this.organizationApi.list().subscribe({
      next: (organizations) => {
        const sorted = [...organizations].sort((left, right) => left.status !== right.status
          ? (left.status === 'ACTIVE' ? -1 : 1)
          : left.name.localeCompare(right.name));
        this.organizations.set(sorted);
        const remembered = this.readRememberedOrganizationId();
        const selected = sorted.find((organization) => organization.id === remembered)
          ?? sorted.find((organization) => organization.status === 'ACTIVE')
          ?? sorted[0]
          ?? null;
        this.selectedOrganizationId.set(selected?.id ?? null);
        if (selected) {
          this.loadWorkspace();
        } else {
          this.error.set(this.t('rbac.noOrganization'));
          this.loading.set(false);
        }
      },
      error: (error: unknown) => {
        this.error.set(this.errorMessage(error, 'rbac.organizationLoadError'));
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

  private clearMessages(): void {
    this.error.set(null);
    this.success.set(null);
  }

  /** Message traduit ; la précision métier du serveur (en français) s'y ajoute quand l'écran est en français. */
  private errorMessage(error: unknown, fallbackKey: string): string {
    const body = error instanceof HttpErrorResponse ? error.error as { detail?: unknown } | null : null;
    const detail = this.i18n.locale() === 'fr' && typeof body?.detail === 'string' ? body.detail : '';
    const message = this.t(error instanceof HttpErrorResponse && error.status === 403 ? 'rbac.forbidden' : fallbackKey);
    return detail ? `${message} ${detail}` : message;
  }
}
