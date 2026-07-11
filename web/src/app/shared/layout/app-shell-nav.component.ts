import { Component, computed, inject, input, output, OnDestroy, OnInit, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
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
}

@Component({
  selector: 'app-shell-nav',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, IconComponent],
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
          [title]="(sidebarCollapsed() && !isMobile()) ? item.label : ''"
          (click)="isMobile() ? linkClicked.emit() : null"
          class="ui-nav-link"
        >
          <span class="relative flex h-5 w-5 flex-shrink-0 items-center justify-center">
            <app-ui-icon [name]="item.iconName" class="text-[1.15rem]" />
            @if (badgeFor(item); as badge) {
              <span
                class="absolute -right-2 -top-2 flex h-4 min-w-4 items-center justify-center rounded-full px-1 text-[8px] font-extrabold"
                style="background: var(--brand-danger); color: var(--text-inverse)"
              >
                {{ badge }}
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
  readonly i18n = inject(I18nService);
  readonly activePatientService = inject(ActivePatientService);
  private readonly patientApiService = inject(PatientApiService);

  readonly pendingPreRegistrationsCount = signal(0);
  private intervalId: ReturnType<typeof setInterval> | null = null;

  readonly session = input.required<{ role: string; name: string } | null>();
  readonly sidebarCollapsed = input<boolean>(false);
  readonly isMobile = input<boolean>(false);
  readonly unreadCount = input<number>(0);
  readonly linkClicked = output<void>();
  readonly activePatient = this.activePatientService.patient;

  ngOnInit(): void {
    this.refreshPendingCount();
    this.intervalId = setInterval(() => this.refreshPendingCount(), 30000);
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  readonly menuItems = computed<NavItem[]>(() => {
    const currentSession = this.session();
    if (!currentSession) return [];

    const roles = this.roles(currentSession.role);
    const items: NavItem[] = [];
    const addUniqueItem = (item: NavItem) => {
      if (!items.some((candidate) => candidate.path === item.path)) {
        items.push(item);
      }
    };

    if (roles.includes('ADMIN_JOPRELYS')) {
      addUniqueItem(this.item('/dashboard', 'menu.dashboard', 'chart-bar'));
      addUniqueItem(this.item('/organizations', 'menu.clinics', 'building'));
      addUniqueItem(this.item('/clinic/lab-orders', 'menu.labOrders', 'clipboard-document-list'));
      addUniqueItem(this.item('/pharmacy/prescriptions', 'menu.prescriptions', 'document-text'));
      addUniqueItem(this.item('/pharmacy/stocks', 'menu.stocks', 'folder-open'));
    }

    if (roles.includes('ADMIN_CLINIQUE')) {
      addUniqueItem(this.item('/dashboard', 'menu.dashboard', 'chart-bar'));
      addUniqueItem(this.item('/patients', 'menu.patients', 'users'));
      addUniqueItem(this.item('/clinic/spatial', 'menu.spatial', 'building'));
      addUniqueItem(this.item('/clinic/billing', 'menu.billing', 'banknotes'));
      addUniqueItem(this.item('/clinic/reception', 'menu.reception', 'users'));
      addUniqueItem(this.item('/clinic/emergencies', 'menu.emergencies', 'bolt'));
      addUniqueItem(this.item('/clinic/admissions/pre-registrations', 'menu.preRegistrations', 'clipboard-document-list'));
      addUniqueItem(this.item('/clinic/duplicates', 'menu.duplicates', 'users'));
      addUniqueItem(this.item('/clinic/staff', 'menu.staff', 'shield-check'));
      addUniqueItem(this.item('/pharmacy/stocks', 'menu.stocks', 'folder-open'));
    }

    if (roles.some((role) => ['AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN'].includes(role))) {
      addUniqueItem(this.item('/dashboard', 'menu.dashboard', 'chart-bar'));
      addUniqueItem(this.item('/patients', 'menu.patients', 'users'));
      addUniqueItem(this.item('/clinic/spatial', 'menu.spatial', 'building'));

      if (roles.includes('AGENT_ACCUEIL')) {
        addUniqueItem(this.item('/clinic/admissions/pre-registrations', 'menu.preRegistrations', 'clipboard-document-list'));
        addUniqueItem(this.item('/clinic/reception', 'menu.reception', 'users'));
        addUniqueItem(this.item('/clinic/billing', 'menu.billing', 'banknotes'));
      }
      if (roles.includes('INFIRMIER') || roles.includes('MEDECIN')) {
        addUniqueItem(this.item('/clinic/emergencies', 'menu.emergencies', 'bolt'));
      }
    }

    if (roles.includes('CAISSIER')) {
      addUniqueItem(this.item('/dashboard', 'menu.dashboard', 'chart-bar'));
      addUniqueItem(this.item('/clinic/cashier', 'menu.cashier', 'banknotes'));
    }

    if (roles.includes('DAF') || roles.includes('SECRETAIRE_COMPTABLE')) {
      addUniqueItem(this.item('/dashboard', 'menu.dashboard', 'chart-bar'));
      addUniqueItem(this.item('/clinic/billing', 'menu.billing', 'banknotes'));
    }

    if (roles.includes('AUDITEUR')) {
      addUniqueItem(this.item('/dashboard', 'menu.dashboard', 'chart-bar'));
    }

    if (roles.includes('BIOLOGISTE')) {
      addUniqueItem(this.item('/dashboard', 'menu.dashboard', 'chart-bar'));
      addUniqueItem(this.item('/clinic/lab-orders', 'menu.labOrders', 'clipboard-document-list'));
    }

    if (roles.includes('PHARMACIEN')) {
      addUniqueItem(this.item('/dashboard', 'menu.dashboard', 'chart-bar'));
      addUniqueItem(this.item('/pharmacy/prescriptions', 'menu.prescriptions', 'document-text'));
      addUniqueItem(this.item('/pharmacy/stocks', 'menu.stocks', 'folder-open'));
    }

    if (roles.includes('PATIENT')) {
      addUniqueItem(this.item('/patient/dashboard', 'menu.patientDashboard', 'chart-bar'));
      addUniqueItem(this.item('/patient/profile', 'menu.patientProfile', 'users'));
      addUniqueItem(this.item('/patient/summary', 'menu.patientSummary', 'document-text'));
      addUniqueItem(this.item('/patient/prescriptions', 'menu.patientPrescriptions', 'document-text'));
      addUniqueItem(this.item('/patient/results', 'menu.patientResults', 'clipboard-document-list'));
      addUniqueItem(this.item('/patient/documents', 'menu.patientDocuments', 'folder-open'));
      addUniqueItem(this.item('/patient/qr-code', 'menu.patientQrCode', 'information-circle'));
      addUniqueItem(this.item('/patient/consents', 'menu.patientConsents', 'shield-check'));
      addUniqueItem(this.item('/patient/privacy', 'menu.patientPrivacy', 'shield-check'));
      addUniqueItem(this.item('/patient/audit', 'menu.patientAudit', 'document-text'));
      addUniqueItem(this.item('/patient/requests', 'menu.patientRequests', 'clipboard-document-list'));
      addUniqueItem(this.item('/patient/notifications', 'menu.patientNotifications', 'information-circle'));
    }

    this.appendActivePatientItems(items, roles);
    return items;
  });

  badgeFor(item: NavItem): string | null {
    if (item.path === '/clinic/admissions/pre-registrations' && this.pendingPreRegistrationsCount() > 0) {
      return this.formatBadge(this.pendingPreRegistrationsCount());
    }
    if (item.path === '/patient/notifications' && this.unreadCount() > 0) {
      return this.formatBadge(this.unreadCount());
    }
    return null;
  }

  private refreshPendingCount(): void {
    const currentSession = this.session();
    if (!currentSession) return;
    const roles = this.roles(currentSession.role);
    if (roles.includes('AGENT_ACCUEIL') || roles.includes('ADMIN_CLINIQUE')) {
      this.patientApiService.getPendingPreRegistrations(0, 1).subscribe({
        next: (response) => this.pendingPreRegistrationsCount.set(response.totalElements),
        error: () => this.pendingPreRegistrationsCount.set(0),
      });
    }
  }

  private appendActivePatientItems(items: NavItem[], roles: string[]): void {
    const activePatient = this.activePatient();
    if (!activePatient || roles.includes('PATIENT')) return;

    const patientsIndex = items.findIndex((item) => item.path === '/patients');
    if (patientsIndex === -1) return;

    const patientId = activePatient.id;
    const patientFirstName = activePatient.fullName.split(' ')[0];
    const subItems: NavItem[] = [
      { path: '', label: `Dossier: ${patientFirstName}`, iconName: 'users', isHeader: true },
      {
        path: `/patients/${patientId}/profile`,
        label: this.i18n.t('menu.patientDetail.profile'),
        iconName: 'users',
        indent: true,
      },
    ];

    if (roles.some((role) => ['MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE'].includes(role))) {
      subItems.push(
        {
          path: `/patients/${patientId}/consultations`,
          label: this.i18n.t('menu.patientDetail.consultations'),
          iconName: 'stethoscope',
          indent: true,
        },
        {
          path: `/patients/${patientId}/lab-orders`,
          label: this.i18n.t('menu.patientDetail.labOrders'),
          iconName: 'clipboard-document-list',
          indent: true,
        },
        {
          path: `/patients/${patientId}/hospitalizations`,
          label: this.i18n.t('menu.patientDetail.hospitalization'),
          iconName: 'bed',
          indent: true,
        },
      );
    }

    if (roles.some((role) => ['MEDECIN', 'ADMIN_CLINIQUE', 'AUDITEUR'].includes(role))) {
      subItems.push({
        path: `/patients/${patientId}/audit-trail`,
        label: this.i18n.t('menu.patientDetail.audit'),
        iconName: 'document-text',
        indent: true,
      });
    }

    items.splice(patientsIndex + 1, 0, ...subItems);
  }

  private item(path: string, labelKey: string, iconName: UiIconName): NavItem {
    return { path, label: this.i18n.t(labelKey), iconName };
  }

  private roles(rawRoles: string): string[] {
    return rawRoles.split(',').map((role) => role.trim()).filter(Boolean);
  }

  private formatBadge(count: number): string {
    return count > 9 ? '9+' : count.toString();
  }
}
