import { Component, computed, inject, input, output, OnInit, OnDestroy, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { I18nService } from '../../core/i18n/i18n.service';
import { RbacApiService } from '../../clinic/rbac/rbac-api.service';
import { ActivePatientService } from '../../patient/active-patient.service';
import { PatientApiService } from '../../patient/patient-api.service';

export interface NavItem {
  path: string;
  label: string;
  iconName: string;
  isHeader?: boolean;
  indent?: boolean;
}

@Component({
  selector: 'app-shell-nav',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    @for (item of menuItems(); track (item.path + '-' + item.label)) {
      @if (item.isHeader) {
        @if (!sidebarCollapsed() || isMobile()) {
          <div class="ui-nav-header">
            {{ item.label }}
          </div>
        } @else {
          <div class="h-px bg-[var(--app-border)] my-2"></div>
        }
      } @else {
        <a
          [routerLink]="item.path"
          routerLinkActive="bg-[var(--app-surface-muted)] text-brand-cyan font-bold border-l-2 border-brand-cyan"
          [class.justify-center]="sidebarCollapsed() && !isMobile()"
          [class.pl-8]="item.indent && (!sidebarCollapsed() || isMobile())"
          [title]="(sidebarCollapsed() && !isMobile()) ? item.label : ''"
          (click)="isMobile() ? linkClicked.emit() : null"
          class="ui-nav-link"
        >
          <span class="flex items-center justify-center w-5 h-5 flex-shrink-0">
            @switch (item.iconName) {
              @case ('dashboard') {
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 12l8.954-8.955c.44-.439 1.152-.439 1.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75M8.25 21h8.25" />
                </svg>
              }
              @case ('clinics') {
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 21v-8.25M15.75 21v-8.25M8.25 21v-8.25M3 9l9-6 9 6m-1.5 12V10.332A48.36 48.36 0 0012 9.75c-2.551 0-5.056.2-7.5.582V21M3 21h18M12 6.75h.008v.008H12V6.75z" />
                </svg>
              }
              @case ('labOrders') {
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9.75 3.104v1.244c0 .593-.193 1.168-.55 1.637L4.75 11.96c-.357.47-.55 1.045-.55 1.638v3.152c0 1.242 1.01 2.25 2.25 2.25h11.1c1.242 0 2.25-1.008 2.25-2.25v-3.152c0-.593-.193-1.168-.55-1.637l-4.45-5.975a2.72 2.72 0 00-.55-1.637V3.104M9.75 3.104c0-.528.435-.953.97-.953h2.56c.535 0 .97.425.97.953M9.75 3.104h4.5M18.75 14.25h-13.5" />
                </svg>
              }
              @case ('prescriptions') {
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 002.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 00-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 00.75-.75 2.25 2.25 0 00-.1-.664m-5.8 0A2.251 2.251 0 0113.5 2.25H15c1.03 0 1.9.693 2.166 1.638m-7.377 2.24a4.5 4.5 0 112.924-2.924M7.5 19.5h-.75A2.25 2.25 0 014.5 17.25V5.37c0-1.135.845-2.098 1.976-2.192.373-.03.748-.057 1.123-.08M15 12.75l1.5 1.5 3-3" />
                </svg>
              }
              @case ('stocks') {
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M20.25 7.5l-.625 10.632a2.25 2.25 0 01-2.247 2.118H6.622a2.25 2.25 0 01-2.247-2.118L3.75 7.5M10 11.25h4M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125z" />
                </svg>
              }
              @case ('patients') {
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
                </svg>
              }
              @case ('staff') {
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M18 18.72a9.094 9.094 0 003.741-.479 3 3 0 00-4.682-2.72m.94 3.198l.001.031c0 .225-.012.447-.037.666A11.944 11.944 0 0112 21c-2.17 0-4.207-.576-5.963-1.584A6.062 6.062 0 016 18.719m12 0a5.971 5.971 0 00-.941-3.197m0 0A5.995 5.995 0 0012 12.75a5.995 5.995 0 00-5.058 2.772m0 0a3 3 0 00-4.681 2.72 8.986 8.986 0 003.74.477m.94-3.197a5.971 5.971 0 00-.94 3.197M15 6.75a3 3 0 11-6 0 3 3 0 016 0zm6 3a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0zm-13.5 0a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0z" />
                </svg>
              }
              @case ('consents') {
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285z" />
                </svg>
              }
              @case ('audit') {
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              }
              @case ('requests') {
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
                </svg>
              }
              @case ('notifications') {
                <div class="relative">
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0M3.124 7.5A8.969 8.969 0 015.292 3m13.416 0a8.969 8.969 0 012.168 4.5" />
                  </svg>
                  @if (unreadCount() > 0) {
                    <span class="absolute -top-1 -right-1 flex h-3.5 w-3.5 items-center justify-center rounded-full text-[8px] font-bold" style="background:var(--brand-danger);color:var(--text-inverse);">
                      {{ unreadCount() > 9 ? '9+' : unreadCount() }}
                    </span>
                  }
                </div>
              }
              @case ('preRegistrations') {
                <div class="relative">
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
                  </svg>
                  @if (pendingPreRegistrationsCount() > 0) {
                    <span class="absolute -top-1 -right-1 flex h-3.5 min-w-3.5 items-center justify-center rounded-full px-0.5 text-[8px] font-bold" style="background:var(--brand-danger);color:var(--text-inverse);">
                      {{ pendingPreRegistrationsCount() > 99 ? '99+' : pendingPreRegistrationsCount() }}
                    </span>
                  }
                </div>
              }
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
  readonly i18n = inject(I18nService);
  readonly activePatientService = inject(ActivePatientService);
  private readonly patientApiService = inject(PatientApiService);
  private readonly rbacApi = inject(RbacApiService);

  pendingPreRegistrationsCount = signal(0);
  private intervalId: ReturnType<typeof setInterval> | null = null;

  readonly session = input.required<{ role: string; name: string } | null>();
  readonly sidebarCollapsed = input<boolean>(false);
  readonly isMobile = input<boolean>(false);
  readonly unreadCount = input<number>(0);
  readonly linkClicked = output<void>();

  readonly activePatient = this.activePatientService.patient;

  ngOnInit(): void {
    this.loadEffectiveAccess();
    this.refreshPendingCount();
    this.intervalId = setInterval(() => this.refreshPendingCount(), 30000);
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  private loadEffectiveAccess(): void {
    const currentSession = this.session();
    if (!currentSession) return;
    const roles = currentSession.role.split(',').map((role) => role.trim()).filter(Boolean);
    if (roles.includes('PATIENT')) return;
    this.rbacApi.ensureMyAccess().subscribe({ error: () => undefined });
  }

  private refreshPendingCount(): void {
    const currentSession = this.session();
    if (!currentSession) return;
    const roles = currentSession.role.split(',').map((role) => role.trim());
    const canReadAdmissions = this.rbacApi.hasPermission('PATIENT_READ') || this.rbacApi.hasPermission('PATIENT_WRITE');
    if (canReadAdmissions || roles.includes('AGENT_ACCUEIL') || roles.includes('ADMIN_CLINIQUE')) {
      this.patientApiService.getPendingPreRegistrations(0, 1).subscribe({
        next: (res) => this.pendingPreRegistrationsCount.set(res.totalElements),
        error: () => undefined,
      });
    }
  }

  readonly menuItems = computed(() => {
    const currentSession = this.session();
    if (!currentSession) return [];

    const roles = currentSession.role.split(',').map((role) => role.trim()).filter(Boolean);
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
      addUniqueItem({ path: '/dashboard', label: this.i18n.t('menu.dashboard'), iconName: 'dashboard' });
    }

    if (hasRole('ADMIN_JOPRELYS', 'SUPER_ADMIN') || hasPermission('ORGANIZATION_MANAGE')) {
      addUniqueItem({ path: '/organizations', label: this.i18n.t('menu.clinics'), iconName: 'clinics' });
    }
    if (hasRole('ADMIN_CLINIQUE') || hasPermission('USER_READ', 'USER_MANAGE')) {
      addUniqueItem({ path: '/clinic/staff', label: this.i18n.t('menu.staff'), iconName: 'staff' });
    }
    if (hasRole('ADMIN_CLINIQUE', 'ADMIN_JOPRELYS', 'SUPER_ADMIN') || hasPermission('RBAC_READ', 'RBAC_MANAGE')) {
      addUniqueItem({ path: '/clinic/rbac', label: this.i18n.t('menu.rbac', 'Rôles et permissions'), iconName: 'consents' });
    }
    if (hasRole('ADMIN_CLINIQUE', 'AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN') || hasPermission('PATIENT_READ')) {
      addUniqueItem({ path: '/patients', label: this.i18n.t('menu.patients'), iconName: 'patients' });
    }
    if (hasRole('ADMIN_CLINIQUE', 'AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN') || hasPermission('HOSPITALIZATION_READ', 'HOSPITALIZATION_MANAGE')) {
      addUniqueItem({ path: '/clinic/spatial', label: this.i18n.t('menu.spatial'), iconName: 'stocks' });
    }
    if (hasRole('ADMIN_CLINIQUE', 'AGENT_ACCUEIL', 'DAF', 'SECRETAIRE_COMPTABLE')
      || hasPermission('BILLING_INVOICE_READ', 'BILLING_INVOICE_WRITE', 'INSURANCE_BORDEREAU_READ', 'ACCOUNTING_DASHBOARD_READ')) {
      addUniqueItem({ path: '/clinic/billing', label: this.i18n.t('menu.billing'), iconName: 'audit' });
    }
    if (hasRole('CAISSIER') || hasPermission('CASH_QUEUE_READ', 'CASH_PAYMENT_COLLECT', 'CASH_SESSION_OPEN')) {
      addUniqueItem({ path: '/clinic/cashier', label: this.i18n.t('menu.cashier'), iconName: 'audit' });
    }
    if (hasRole('AGENT_ACCUEIL', 'ADMIN_CLINIQUE') || hasPermission('PATIENT_WRITE')) {
      addUniqueItem({ path: '/clinic/reception', label: this.i18n.t('menu.reception'), iconName: 'staff' });
      addUniqueItem({ path: '/clinic/admissions/pre-registrations', label: this.i18n.t('menu.preRegistrations'), iconName: 'preRegistrations' });
    }
    if (hasRole('INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE') || hasPermission('CLINICAL_READ', 'CLINICAL_WRITE')) {
      addUniqueItem({ path: '/clinic/emergencies', label: this.i18n.t('menu.emergencies'), iconName: 'dashboard' });
    }
    if (hasRole('ADMIN_CLINIQUE')) {
      addUniqueItem({ path: '/clinic/duplicates', label: this.i18n.t('menu.duplicates'), iconName: 'patients' });
    }
    if (hasRole('BIOLOGISTE', 'ADMIN_JOPRELYS') || hasPermission('LAB_ORDER_READ', 'LAB_ORDER_WRITE')) {
      addUniqueItem({ path: '/clinic/lab-orders', label: this.i18n.t('menu.labOrders'), iconName: 'labOrders' });
    }
    if (hasRole('PHARMACIEN', 'ADMIN_JOPRELYS') || hasPermission('PHARMACY_PRESCRIPTION_READ')) {
      addUniqueItem({ path: '/pharmacy/prescriptions', label: this.i18n.t('menu.prescriptions'), iconName: 'prescriptions' });
    }
    if (hasRole('PHARMACIEN', 'ADMIN_CLINIQUE', 'ADMIN_JOPRELYS', 'GESTIONNAIRE_STOCK') || hasPermission('STOCK_READ', 'STOCK_MANAGE')) {
      addUniqueItem({ path: '/pharmacy/stocks', label: this.i18n.t('menu.stocks'), iconName: 'stocks' });
    }

    if (hasRole('PATIENT')) {
      addUniqueItem({ path: '/patient/dashboard', label: this.i18n.t('menu.patientDashboard'), iconName: 'dashboard' });
      addUniqueItem({ path: '/patient/profile', label: this.i18n.t('menu.patientProfile'), iconName: 'patients' });
      addUniqueItem({ path: '/patient/summary', label: this.i18n.t('menu.patientSummary'), iconName: 'prescriptions' });
      addUniqueItem({ path: '/patient/prescriptions', label: this.i18n.t('menu.patientPrescriptions'), iconName: 'prescriptions' });
      addUniqueItem({ path: '/patient/results', label: this.i18n.t('menu.patientResults'), iconName: 'labOrders' });
      addUniqueItem({ path: '/patient/documents', label: this.i18n.t('menu.patientDocuments'), iconName: 'audit' });
      addUniqueItem({ path: '/patient/qr-code', label: this.i18n.t('menu.patientQrCode'), iconName: 'patients' });
      addUniqueItem({ path: '/patient/consents', label: this.i18n.t('menu.patientConsents'), iconName: 'consents' });
      addUniqueItem({ path: '/patient/privacy', label: this.i18n.t('menu.patientPrivacy'), iconName: 'consents' });
      addUniqueItem({ path: '/patient/audit', label: this.i18n.t('menu.patientAudit'), iconName: 'audit' });
      addUniqueItem({ path: '/patient/requests', label: this.i18n.t('menu.patientRequests'), iconName: 'requests' });
      addUniqueItem({ path: '/patient/notifications', label: this.i18n.t('menu.patientNotifications'), iconName: 'notifications' });
    }

    const activePatientObj = this.activePatient();
    if (activePatientObj && !hasRole('PATIENT')) {
      const id = activePatientObj.id;
      const patientName = activePatientObj.fullName;
      const patientsIndex = items.findIndex((item) => item.path === '/patients');
      if (patientsIndex !== -1) {
        const subItems: NavItem[] = [
          { path: '', label: `Dossier: ${patientName.split(' ')[0]}`, iconName: 'patients', isHeader: true },
          { path: `/patients/${id}/profile`, label: this.i18n.t('menu.patientDetail.profile'), iconName: 'patients', indent: true },
        ];

        if (hasRole('MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE') || hasPermission('CLINICAL_READ', 'LAB_ORDER_READ', 'HOSPITALIZATION_READ')) {
          subItems.push(
            { path: `/patients/${id}/consultations`, label: this.i18n.t('menu.patientDetail.consultations'), iconName: 'prescriptions', indent: true },
            { path: `/patients/${id}/lab-orders`, label: this.i18n.t('menu.patientDetail.labOrders'), iconName: 'labOrders', indent: true },
            { path: `/patients/${id}/hospitalizations`, label: this.i18n.t('menu.patientDetail.hospitalization'), iconName: 'stocks', indent: true },
          );
        }

        if (hasRole('MEDECIN', 'ADMIN_CLINIQUE', 'AUDITEUR') || hasPermission('AUDIT_READ')) {
          subItems.push({ path: `/patients/${id}/audit-trail`, label: this.i18n.t('menu.patientDetail.audit'), iconName: 'audit', indent: true });
        }

        items.splice(patientsIndex + 1, 0, ...subItems);
      }
    }

    return items;
  });
}
