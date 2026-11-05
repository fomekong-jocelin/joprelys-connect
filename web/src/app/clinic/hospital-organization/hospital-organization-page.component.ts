import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiErrorI18nService } from '../../core/i18n/api-error-i18n.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { interpolate } from '../../core/i18n/interpolate';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { IconComponent, UiIconName } from '../../shared/ui/icon.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { OrganizationApiService } from '../organizations/organization-api.service';
import { Organization } from '../organizations/organizations.models';
import { RbacApiService } from '../rbac/rbac-api.service';
import { HospitalOrganizationApiService } from './hospital-organization-api.service';
import { UNIT_LEVELS, buildUnitTree, unitDisplayName } from './hospital-organization-hierarchy';
import {
  HospitalServiceCatalogEntry,
  MedicalSpecialtyCatalogEntry,
  OrganizationalUnit,
  OrganizationalUnitType,
} from './hospital-organization.models';
import { OrgUnitEditorComponent, UnitEditorRequest, UnitSaveEvent } from './org-unit-editor.component';

const UNIT_ICONS: Readonly<Record<OrganizationalUnitType, UiIconName>> = {
  POLE: 'building',
  DEPARTMENT: 'folder-open',
  SERVICE: 'stethoscope',
  CARE_UNIT: 'bed',
};

@Component({
  selector: 'app-hospital-organization-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    AppShellComponent,
    IconComponent,
    PageHeaderComponent,
    OrgUnitEditorComponent,
  ],
  templateUrl: './hospital-organization-page.component.html',
})
export class HospitalOrganizationPageComponent implements OnInit {
  private readonly api = inject(HospitalOrganizationApiService);
  private readonly organizationApi = inject(OrganizationApiService);
  private readonly rbacApi = inject(RbacApiService);
  private readonly i18n = inject(I18nService);
  private readonly apiErrorI18n = inject(ApiErrorI18nService);

  readonly levels = UNIT_LEVELS;
  readonly units = signal<OrganizationalUnit[]>([]);
  readonly serviceCatalog = signal<HospitalServiceCatalogEntry[]>([]);
  readonly specialtyCatalog = signal<MedicalSpecialtyCatalogEntry[]>([]);
  readonly organizations = signal<Organization[]>([]);
  readonly selectedOrganizationId = signal('');
  readonly includeInactive = signal(false);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly editor = signal<UnitEditorRequest | null>(null);
  readonly platformAdministrator = computed(() => this.rbacApi.hasPermission('ORGANIZATION_MANAGE'));
  readonly t = (key: string) => this.i18n.t(key);
  readonly tree = computed(() => buildUnitTree(this.units(), (unit) => this.unitLabel(unit)));
  readonly scopeMissing = computed(() => this.platformAdministrator() && !this.selectedOrganizationId());
  readonly empty = computed(() => !this.scopeMissing() && !this.loading() && this.units().length === 0);
  readonly showHeaderAction = computed(() => !this.scopeMissing() && !this.empty());
  readonly catalogSummary = computed(() => interpolate(this.t('hospitalOrg.catalogs.summary'), {
    services: this.serviceCatalog().length,
    specialties: this.specialtyCatalog().length,
  }));

  ngOnInit(): void {
    this.loadCatalogs();
    if (this.platformAdministrator()) {
      this.loadOrganizations();
      return;
    }
    this.loadUnits();
  }

  loadUnits(): void {
    if (this.scopeMissing()) {
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    this.api.listUnits(this.scopeOrganizationId(), this.includeInactive()).subscribe({
      next: (units) => {
        this.units.set(units);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => this.handleError(error),
    });
  }

  onOrganizationChange(organizationId: string): void {
    this.selectedOrganizationId.set(organizationId);
    this.units.set([]);
    this.clearMessages();
    this.loadUnits();
  }

  onIncludeInactiveChange(value: boolean): void {
    this.includeInactive.set(value);
    this.loadUnits();
  }

  canManageStaff(): boolean {
    return !this.platformAdministrator() && this.rbacApi.hasPermission('USER_MANAGE');
  }

  openCreate(unitType: OrganizationalUnitType, parentId: string | null = null): void {
    this.clearMessages();
    this.editor.set({ mode: 'create', unitType, parentId });
  }

  openEdit(unit: OrganizationalUnit): void {
    this.clearMessages();
    this.editor.set({ mode: 'edit', unit });
  }

  closeEditor(): void {
    if (this.busy()) return;
    this.editor.set(null);
    this.errorMessage.set('');
  }

  save({ id, payload }: UnitSaveEvent): void {
    this.busy.set(true);
    this.clearMessages();
    const request = id
      ? this.api.updateUnit(id, payload, this.scopeOrganizationId())
      : this.api.createUnit(payload, this.scopeOrganizationId());
    request.subscribe({
      next: () => this.afterMutation('hospitalOrg.feedback.saved'),
      error: (error: HttpErrorResponse) => this.handleOperationError(error),
    });
  }

  setActive(unit: OrganizationalUnit, active: boolean): void {
    this.busy.set(true);
    this.clearMessages();
    this.api.setActive(unit.id, active, this.scopeOrganizationId()).subscribe({
      next: () => this.afterMutation(active ? 'hospitalOrg.feedback.activated' : 'hospitalOrg.feedback.deactivated'),
      error: (error: HttpErrorResponse) => this.handleOperationError(error),
    });
  }

  typeLabel(type: OrganizationalUnitType): string {
    return this.t(`hospitalOrg.unitType.${type}`);
  }

  unitIcon(type: OrganizationalUnitType): UiIconName {
    return UNIT_ICONS[type];
  }

  actionLabel(key: string, unitLabel: string): string {
    return interpolate(this.t(key), { unit: unitLabel });
  }

  unitLabel(unit: OrganizationalUnit): string {
    return unitDisplayName(unit, this.serviceCatalog(), this.i18n.locale());
  }

  catalogLabel(entry: HospitalServiceCatalogEntry | MedicalSpecialtyCatalogEntry): string {
    return this.i18n.locale() === 'en' ? entry.nameEn : entry.nameFr;
  }

  private afterMutation(feedbackKey: string): void {
    this.busy.set(false);
    this.editor.set(null);
    this.successMessage.set(this.t(feedbackKey));
    this.loadUnits();
  }

  private loadCatalogs(): void {
    this.api.listServiceCatalog().subscribe({ next: (values) => this.serviceCatalog.set(values) });
    this.api.listSpecialtyCatalog().subscribe({ next: (values) => this.specialtyCatalog.set(values) });
  }

  private loadOrganizations(): void {
    this.loading.set(true);
    this.organizationApi.list().subscribe({
      next: (organizations) => {
        this.organizations.set(organizations);
        const preferred = organizations.find((organization) => organization.status === 'ACTIVE') ?? organizations[0];
        this.selectedOrganizationId.set(preferred?.id ?? '');
        this.loadUnits();
      },
      error: (error: HttpErrorResponse) => this.handleError(error),
    });
  }

  private handleOperationError(error: HttpErrorResponse): void {
    this.busy.set(false);
    this.handleError(error);
  }

  private handleError(error: HttpErrorResponse): void {
    this.errorMessage.set(this.apiErrorI18n.message(error, 'hospitalOrg.errors', 'hospitalOrg.feedback.error'));
    this.loading.set(false);
  }

  private clearMessages(): void {
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  private scopeOrganizationId(): string | undefined {
    return this.platformAdministrator() ? this.selectedOrganizationId() || undefined : undefined;
  }
}
