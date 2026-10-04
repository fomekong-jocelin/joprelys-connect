import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthTokenStorageService } from '../auth/auth-token-storage.service';
import { I18nService } from '../core/i18n/i18n.service';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { ButtonComponent } from '../shared/ui/button.component';
import { ActiveVisitQueueComponent } from './queue/active-visit-queue.component';
import { RbacApiService } from './rbac/rbac-api.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
  imports: [AppShellComponent, RouterLink, ButtonComponent, ActiveVisitQueueComponent],
})
export class DashboardComponent {
  private readonly tokenStorage = inject(AuthTokenStorageService);
  private readonly i18n = inject(I18nService);
  private readonly rbacApi = inject(RbacApiService);

  readonly session = computed(() => {
    const currentSession = this.tokenStorage.session();
    if (!currentSession) return null;
    return {
      ...currentSession,
      role: this.localizeRoles(currentSession.role),
    };
  });
  readonly welcomeLabel = computed(() =>
    this.i18n.t('dashboard.greeting', this.i18n.t('dashboard.welcome')),
  );
  readonly authorizedLabel = computed(() =>
    this.i18n.t('dashboard.welcomeSubtitle', this.i18n.t('dashboard.authorized')),
  );
  readonly roleLabel = computed(() =>
    this.i18n.t('dashboard.profileLabel', this.i18n.t('dashboard.role')),
  );

  readonly showAuditSecurityModal = signal(false);

  readonly isClinicalRole = computed(() => this.hasPermission('VISIT_READ'));

  hasPermission(permissions: string[] | string): boolean {
    const expected = Array.isArray(permissions) ? permissions : [permissions];
    return expected.some((permission) => this.rbacApi.hasPermission(permission));
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  openAuditSecurityModal(): void {
    this.showAuditSecurityModal.set(true);
  }

  closeAuditSecurityModal(): void {
    this.showAuditSecurityModal.set(false);
  }

  private localizeRoles(rawRoles: string): string {
    return rawRoles
      .split(',')
      .map((role) => role.trim())
      .filter(Boolean)
      .map((role) => this.i18n.t(`role.${role}`, role))
      .join(' · ');
  }
}
