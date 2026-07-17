import { Component, computed, inject, input, OnDestroy, OnInit, output, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { RbacApiService } from '../../clinic/rbac/rbac-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { ActivePatientService } from '../../patient/active-patient.service';
import { PatientApiService } from '../../patient/patient-api.service';
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
  private readonly activePatientService = inject(ActivePatientService);
  private readonly patientApiService = inject(PatientApiService);
  private readonly rbacApi = inject(RbacApiService);

  readonly session = input.required<{ role: string; name: string } | null>();
  readonly sidebarCollapsed = input(false);
  readonly isMobile = input(false);
  readonly unreadCount = input(0);
  readonly linkClicked = output<void>();

  readonly pendingPreRegistrationsCount = signal(0);
  readonly activePatient = this.activePatientService.patient;

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

    const roles = this.parseRoles(currentSession.role);
    const permissions = new Set(this.rbacApi.access()?.permissions ?? []);
    const items: NavItem[] = [];
    const hasRole = (...codes: string[]) => codes.some((code) => roles.includes(code));
    const hasPermission = (...codes: string[]) => codes.some((code) => permissions.has(code));
    const addUniqueItem = (item: NavItem) => {
      if (!items.some((existing) => existing.path === item.path)) {
        items.push(item);
      }
    };

    if (!hasRole('PATIENT')) {
      addUniqueItem(this.item('/dashboard', 'menu.dashboard', 'chart-bar'));
    }
    if (hasRole('ADMIN_JOPRELYS', 'SUPER_ADMIN') || hasPermission('ORGANIZATION_MANAGE')) {
      addUniqueItem(this.item('/organizations', 'menu.clinics', 'building'));
    }
    if (hasRole('ADMIN_CLINIQUE') || hasPermission('USER_READ', 'USER_MANAGE')) {
      addUniqueItem(this.item('/clinic/staff', 'menu.staff', 'users'));
    }
    if (hasRole('ADMIN_CLINIQUE', 'ADMIN_JOPRELYS', 'SUPER_ADMIN') || hasPermission('RBAC_READ', 'RBAC_MANAGE')) {
      addUniqueItem(this.item('/clinic/rbac', 'menu.rbac', 'shield-check'));
    }
    if (hasRole('ADMIN_CLINIQUE', 'AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN') || hasPermission('PATIENT_READ')) {
      addUniqueItem(this.item('/patients', 'menu.patients', 'users'));
    }
    if (hasRole('ADMIN_CLINIQUE', 'AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN') || hasPermission('HOSPITALIZATION_READ', 'HOSPITALIZATION_MANAGE')) {
      addUniqueItem(this.item('/clinic/spatial', 'menu.spatial', 'bed'));
    }
    if (hasRole('ADMIN_CLINIQUE', 'ADMIN_JOPRELYS', 'SUPER_ADMIN')) {
      addUniqueItem(this.item('/clinic/spatial/configuration', 'menu.spatialConfig', 'building'));
    }
    if (
      hasRole('ADMIN_CLINIQUE', 'AGENT_ACCUEIL', 'DAF', 'SECRETAIRE_COMPTABLE')
      || hasPermission('BILLING_INVOICE_READ', 'BILLING_INVOICE_WRITE', 'INSURANCE_BORDEREAU_READ', 'ACCOUNTING_DASHBOARD_READ')
    ) {
      addUniqueItem(this.item('/clinic/billing', 'menu.billing', 'receipt-percent'));
    }
    if (hasRole('CAISSIER') || hasPermission('CASH_QUEUE_READ', 'CASH_PAYMENT_COLLECT', 'CASH_SESSION_OPEN')) {
      addUniqueItem(this.item('/clinic/cashier', 'menu.cashier', 'banknotes'));
    }
    if (hasRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE') || hasPermission('PATIENT_WRITE')) {
      addUniqueItem(this.item('/clinic/reception', 'menu.reception', 'users'));
      addUniqueItem({
        ...this.item('/clinic/admissions/pre-registrations', 'menu.preRegistrations', 'clipboard-document-list'),
        badgeValue: this.pendingPreRegistrationsCount(),
      });
    }
    if (hasRole('INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE') || hasPermission('CLINICAL_READ', 'CLINICAL_WRITE')) {
      addUniqueItem(this.item('/clinic/emergencies', 'menu.emergencies', 'bolt'));
    }
    if (hasRole('ADMIN_CLINIQUE') || hasPermission('PATIENT_MERGE')) {
      addUniqueItem(this.item('/clinic/patient-reconciliation', 'menu.patientReconciliation', 'clipboard-document-list'));
      addUniqueItem(this.item('/clinic/duplicates', 'menu.duplicates', 'users'));
    }
    if (hasRole('BIOLOGISTE', 'ADMIN_JOPRELYS') || hasPermission('LAB_ORDER_READ', 'LAB_ORDER_WRITE')) {
      addUniqueItem(this.item('/clinic/lab-orders', 'menu.labOrders', 'clipboard-document-list'));
    }
    if (hasRole('PHARMACIEN', 'ADMIN_JOPRELYS') || hasPermission('PHARMACY_PRESCRIPTION_READ')) {
      addUniqueItem(this.item('/pharmacy/prescriptions', 'menu.prescriptions', 'document-text'));
    }
    if (hasRole('PHARMACIEN', 'ADMIN_CLINIQUE', 'ADMIN_JOPRELYS', 'GESTIONNAIRE_STOCK') || hasPermission('STOCK_READ', 'STOCK_MANAGE')) {
      addUniqueItem(this.item('/pharmacy/stocks', 'menu.stocks', 'folder-open'));
    }

    if (hasRole('PATIENT')) {
      addUniqueItem(this.item('/patient/dashboard', 'menu.patientDashboard', 'chart-bar'));
      addUniqueItem(this.item('/patient/profile', 'menu.patientProfile', 'users'));
      addUniqueItem(this.item('/patient/summary', 'menu.patientSummary', 'document-text'));
      addUniqueItem(this.item('/patient/prescriptions', 'menu.patientPrescriptions', 'document-text'));
      addUniqueItem(this.item('/patient/results', 'menu.patientResults', 'clipboard-document-list'));
      addUniqueItem(this.item('/patient/documents', 'menu.patientDocuments', 'folder-open'));
      addUniqueItem(this.item('/patient/qr-code', 'menu.patientQrCode', 'information-circle'));
      addUniqueItem(this.item('/patient/consents', 'menu.patientConsents', 'shield-check'));
      addUniqueItem(this.item('/patient/privacy', 'menu.patientPrivacy', 'shield-check'));
      addUniqueItem(this.item('/patient/audit', 'menu.patientAudit', 'clipboard-document-list'));
      addUniqueItem(this.item('/patient/requests', 'menu.patientRequests', 'document-text'));
      addUniqueItem({
        ...this.item('/patient/notifications', 'menu.patientNotifications', 'information-circle'),
        badgeValue: this.unreadCount(),
      });
    }

    this.appendActivePatientItems(items, roles, permissions);
    return items;
  });

  badgeLabel(value: number): string {
    return value > 99 ? '99+' : String(value);
  }

  private loadEffectiveAccess(): void {
    const currentSession = this.session();
    if (!currentSession || this.parseRoles(currentSession.role).includes('PATIENT')) return;
    this.rbacApi.ensureMyAccess().subscribe({ error: () => undefined });
  }

  private refreshPendingCount(): void {
    const currentSession = this.session();
    if (!currentSession) return;

    const roles = this.parseRoles(currentSession.role);
    const canReadAdmissions = this.rbacApi.hasPermission('PATIENT_WRITE');
    if (!canReadAdmissions && !roles.includes('AGENT_ACCUEIL') && !roles.includes('ADMIN_CLINIQUE')) return;

    this.patientApiService.getPendingPreRegistrations(0, 1).subscribe({
      next: (response) => this.pendingPreRegistrationsCount.set(response.totalElements),
      error: () => undefined,
    });
  }

  private appendActivePatientItems(
    items: NavItem[],
    roles: string[],
    permissions: Set<string>
  ): void {
    const patient = this.activePatient();
    if (!patient || roles.includes('PATIENT')) return;

    const patientsIndex = items.findIndex((item) => item.path === '/patients');
    if (patientsIndex === -1) return;

    const displayName = patient.displayName
      || patient.fullName
      || patient.temporaryPatientNumber
      || patient.globalPatientNumber;
    const shortName = displayName.trim().split(/\s+/)[0];
    const profileLabel = this.i18n.t('menu.patientDetail.profile');
    const subItems: NavItem[] = [
      {
        path: '',
        label: `${profileLabel}: ${shortName}`,
        iconName: 'users',
        isHeader: true,
      },
      this.item(`/patients/${patient.id}/profile`, 'menu.patientDetail.profile', 'users', true),
    ];

    const hasClinicalAccess = roles.some((role) => ['MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE'].includes(role))
      || ['CLINICAL_READ', 'LAB_ORDER_READ', 'HOSPITALIZATION_READ'].some((code) => permissions.has(code));
    if (hasClinicalAccess) {
      subItems.push(
        this.item(`/patients/${patient.id}/consultations`, 'menu.patientDetail.consultations', 'document-text', true),
        this.item(`/patients/${patient.id}/lab-orders`, 'menu.patientDetail.labOrders', 'clipboard-document-list', true),
        this.item(`/patients/${patient.id}/hospitalizations`, 'menu.patientDetail.hospitalization', 'bed', true),
      );
    }

    const hasAuditAccess = roles.some((role) => ['MEDECIN', 'ADMIN_CLINIQUE', 'AUDITEUR'].includes(role))
      || permissions.has('AUDIT_READ');
    if (hasAuditAccess) {
      subItems.push(this.item(`/patients/${patient.id}/audit-trail`, 'menu.patientDetail.audit', 'clipboard-document-list', true));
    }

    items.splice(patientsIndex + 1, 0, ...subItems);
  }

  private item(path: string, translationKey: string, iconName: UiIconName, indent = false): NavItem {
    return {
      path,
      label: this.i18n.t(translationKey),
      iconName,
      indent,
    };
  }

  private parseRoles(value: string): string[] {
    return value.split(',').map((role) => role.trim()).filter(Boolean);
  }
}
