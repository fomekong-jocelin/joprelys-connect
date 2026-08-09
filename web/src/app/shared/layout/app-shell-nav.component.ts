import { Component, computed, inject, input, OnDestroy, OnInit, output, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { RbacApiService } from '../../clinic/rbac/rbac-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { PatientApiService } from '../../patient/patient-api.service';
import {
  canAccessBillingManagement,
  hasAnyPermission,
  PROFESSIONAL_ACCESS_POLICIES,
} from '../../auth/professional-access-policies';
import { IconComponent, UiIconName } from '../ui/icon.component';

export interface NavItem {
  path: string;
  label: string;
  iconName: UiIconName;
  isHeader?: boolean;
  indent?: boolean;
  badgeValue?: number;
}

@Component({
  selector: 'app-shell-nav',
  standalone: true,
  imports: [IconComponent, RouterLink, RouterLinkActive],
  template: `
    @for (item of menuItems(); track (item.path + '-' + item.label)) {
      @if (item.isHeader) {
        @if (!sidebarCollapsed() || isMobile()) {
          <div class="ui-nav-header">{{ item.label }}</div>
        } @else {
          <div class="my-2 h-px bg-[var(--app-border)]"></div>
        }
      } @else {
        <a
          [routerLink]="item.path"
          routerLinkActive="bg-[var(--app-surface-muted)] text-brand-cyan font-bold border-l-2 border-brand-cyan"
          [class.justify-center]="sidebarCollapsed() && !isMobile()"
          [class.pl-8]="item.indent && (!sidebarCollapsed() || isMobile())"
          [title]="sidebarCollapsed() && !isMobile() ? item.label : ''"
          (click)="isMobile() ? linkClicked.emit() : null"
          class="ui-nav-link"
        >
          <span class="relative flex h-5 w-5 shrink-0 items-center justify-center">
            <app-ui-icon [name]="item.iconName" class="h-5 w-5" />
            @if ((item.badgeValue ?? 0) > 0) {
              <span
                class="absolute -right-1 -top-1 flex h-3.5 min-w-3.5 items-center justify-center rounded-full px-0.5 text-[8px] font-bold"
                style="background:var(--brand-danger);color:var(--text-inverse);"
              >
                {{ badgeLabel(item.badgeValue ?? 0) }}
              </span>
            }
          </span>
          @if (!sidebarCollapsed() || isMobile()) {
            <span class="truncate">{{ item.label }}</span>
          }
        </a>
      }
    }
  `,
})
export class AppShellNavComponent implements OnInit, OnDestroy {
  private readonly i18n = inject(I18nService);
  private readonly patientApiService = inject(PatientApiService);
  private readonly rbacApi = inject(RbacApiService);

  readonly session = input.required<{ role: string; name: string } | null>();
  readonly sidebarCollapsed = input(false);
  readonly isMobile = input(false);
  readonly unreadCount = input(0);
  readonly linkClicked = output<void>();

  readonly pendingPreRegistrationsCount = signal(0);

  private intervalId: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.loadEffectiveAccess();
    this.refreshPendingCount();
    this.intervalId = setInterval(() => this.refreshPendingCount(), 30_000);
  }

  ngOnDestroy(): void {
    if (this.intervalId !== null) {
      clearInterval(this.intervalId);
    }
  }

  readonly menuItems = computed<NavItem[]>(() => {
    const currentSession = this.session();
    if (!currentSession) return [];

    const declaredRoles = this.parseRoles(currentSession.role);
    if (declaredRoles.includes('PATIENT')) {
      return this.patientMenuItems();
    }

    const effectiveAccess = this.rbacApi.access();
    const permissions = new Set(effectiveAccess?.permissions ?? []);
    const items: NavItem[] = [];
    const hasPermission = (...codes: string[]) => codes.some((code) => permissions.has(code));
    const addUniqueItem = (item: NavItem) => {
      if (!items.some((existing) => existing.path === item.path)) {
        items.push(item);
      }
    };

    addUniqueItem(this.item('/dashboard', 'menu.dashboard', 'chart-bar'));
    const canManageFacility = hasPermission('ORGANIZATION_MANAGE', 'USER_MANAGE', 'ORGANIZATION_STRUCTURE_MANAGE');
    if (canManageFacility) {
      addUniqueItem(this.section('menu.section.facility'));
    }
    if (hasPermission('ORGANIZATION_MANAGE')) {
      addUniqueItem(this.item('/organizations', 'menu.clinics', 'building'));
    }
    if (hasPermission('ORGANIZATION_STRUCTURE_MANAGE')) {
      addUniqueItem(this.item('/clinic/hospital-organization', 'menu.hospitalOrganization', 'building'));
    }
    if (hasPermission('USER_MANAGE')) {
      addUniqueItem(this.item('/clinic/staff', 'menu.staff', 'users'));
    }
    if (hasPermission('RBAC_MANAGE')) {
      addUniqueItem(this.item('/clinic/rbac', 'menu.rbac', 'shield-check'));
    }
    if (hasPermission('PATIENT_READ')) {
      addUniqueItem(this.item('/patients', 'menu.patients', 'users'));
    }
    if (hasPermission('AVAILABILITY_MANAGE')) {
      addUniqueItem(this.item('/clinic/availability', 'menu.availability', 'calendar'));
    }
    if (hasPermission('APPOINTMENT_READ_OWN')) {
      addUniqueItem(this.item('/clinic/appointments', 'menu.doctorAppointments', 'calendar'));
    }
    const canViewCapacity = hasAnyPermission(permissions, PROFESSIONAL_ACCESS_POLICIES.spatial)
      || hasPermission('SPATIAL_CONFIGURATION_MANAGE');
    if (canViewCapacity) {
      addUniqueItem(this.section('menu.section.capacity'));
    }
    if (hasAnyPermission(permissions, PROFESSIONAL_ACCESS_POLICIES.spatial)) {
      addUniqueItem(this.item('/clinic/spatial', 'menu.spatial', 'bed'));
    }
    if (hasPermission('SPATIAL_CONFIGURATION_MANAGE')) {
      addUniqueItem(this.item('/clinic/spatial/configuration', 'menu.spatialConfig', 'building'));
    }
    if (canAccessBillingManagement(permissions)) {
      addUniqueItem(this.item('/clinic/billing', 'menu.billing', 'receipt-percent'));
    }
    if (hasAnyPermission(permissions, PROFESSIONAL_ACCESS_POLICIES.cashier)) {
      addUniqueItem(this.item('/clinic/cashier', 'menu.cashier', 'banknotes'));
    }
    if (hasPermission('RECEPTION_READ')) {
      addUniqueItem(this.item('/clinic/reception', 'menu.reception', 'users'));
    }
    if (hasPermission('PATIENT_WRITE')) {
      addUniqueItem({
        ...this.item('/clinic/admissions/pre-registrations', 'menu.preRegistrations', 'clipboard-document-list'),
        badgeValue: this.pendingPreRegistrationsCount(),
      });
    }
    if (hasPermission('EMERGENCY_READ')) {
      addUniqueItem(this.item('/clinic/emergencies', 'menu.emergencies', 'bolt'));
    }
    if (hasPermission('PATIENT_MERGE')) {
      addUniqueItem(this.item('/clinic/patient-reconciliation', 'menu.patientReconciliation', 'clipboard-document-list'));
      addUniqueItem(this.item('/clinic/duplicates', 'menu.duplicates', 'users'));
    }
    if (hasAnyPermission(permissions, PROFESSIONAL_ACCESS_POLICIES.labQueue)) {
      addUniqueItem(this.item('/clinic/lab-orders', 'menu.labOrders', 'clipboard-document-list'));
    }
    if (hasPermission('PHARMACY_PRESCRIPTION_READ')) {
      addUniqueItem(this.item('/pharmacy/prescriptions', 'menu.prescriptions', 'document-text'));
    }
    if (hasPermission('STOCK_READ', 'STOCK_MANAGE')) {
      addUniqueItem(this.item('/pharmacy/stocks', 'menu.stocks', 'folder-open'));
    }

    return items;
  });

  badgeLabel(value: number): string {
    return value > 99 ? '99+' : String(value);
  }

  private loadEffectiveAccess(): void {
    const currentSession = this.session();
    if (!currentSession || this.parseRoles(currentSession.role).includes('PATIENT')) return;
    this.rbacApi.ensureMyAccess().subscribe({
      next: () => this.refreshPendingCount(),
      error: () => undefined,
    });
  }

  private refreshPendingCount(): void {
    const currentSession = this.session();
    if (!currentSession) return;

    const declaredRoles = this.parseRoles(currentSession.role);
    if (declaredRoles.includes('PATIENT')) return;

    const canReadAdmissions = this.rbacApi.hasPermission('PATIENT_WRITE');
    if (!canReadAdmissions) return;

    this.patientApiService.getPendingPreRegistrations(0, 1).subscribe({
      next: (response) => this.pendingPreRegistrationsCount.set(response.totalElements),
      error: () => undefined,
    });
  }

  private item(path: string, translationKey: string, iconName: UiIconName, indent = false): NavItem {
    return {
      path,
      label: this.i18n.t(translationKey),
      iconName,
      indent,
    };
  }

  private section(translationKey: string): NavItem {
    return {
      path: `section:${translationKey}`,
      label: this.i18n.t(translationKey),
      iconName: 'building',
      isHeader: true,
    };
  }

  private patientMenuItems(): NavItem[] {
    return [
      this.item('/patient/dashboard', 'menu.patientDashboard', 'chart-bar'),
      this.item('/patient/appointments', 'menu.patientAppointments', 'calendar'),
      this.item('/patient/profile', 'menu.patientProfile', 'users'),
      this.item('/patient/summary', 'menu.patientSummary', 'document-text'),
      this.item('/patient/prescriptions', 'menu.patientPrescriptions', 'document-text'),
      this.item('/patient/results', 'menu.patientResults', 'clipboard-document-list'),
      this.item('/patient/documents', 'menu.patientDocuments', 'folder-open'),
      this.item('/patient/qr-code', 'menu.patientQrCode', 'information-circle'),
      this.item('/patient/consents', 'menu.patientConsents', 'shield-check'),
      this.item('/patient/privacy', 'menu.patientPrivacy', 'shield-check'),
      this.item('/patient/audit', 'menu.patientAudit', 'clipboard-document-list'),
      this.item('/patient/requests', 'menu.patientRequests', 'document-text'),
      {
        ...this.item('/patient/notifications', 'menu.patientNotifications', 'information-circle'),
        badgeValue: this.unreadCount(),
      },
    ];
  }

  private parseRoles(value: string): string[] {
    return value.split(',').map((role) => role.trim()).filter(Boolean);
  }
}
