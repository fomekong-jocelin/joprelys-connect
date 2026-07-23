import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { IconComponent } from '../../shared/ui/icon.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { OrganizationApiService } from '../organizations/organization-api.service';
import { Organization } from '../organizations/organizations.models';
import { RbacApiService } from '../rbac/rbac-api.service';
import { HospitalOrganizationApiService } from './hospital-organization-api.service';
import {
  HospitalServiceCatalogEntry,
  MedicalSpecialtyCatalogEntry,
  OrganizationalUnit,
  OrganizationalUnitType,
  SaveOrganizationalUnitPayload,
} from './hospital-organization.models';

interface EditorState {
  readonly id: string | null;
  unitType: OrganizationalUnitType;
  code: string;
  parentId: string | null;
  name: string;
  serviceCatalogCode: string;
}

interface UnitRow {
  readonly unit: OrganizationalUnit;
  readonly depth: number;
}

@Component({
  selector: 'app-hospital-organization-page',
  standalone: true,
  imports: [CommonModule, FormsModule, AppShellComponent, IconComponent, PageHeaderComponent],
  templateUrl: './hospital-organization-page.component.html',
})
export class HospitalOrganizationPageComponent implements OnInit {
  private readonly api = inject(HospitalOrganizationApiService);
  private readonly organizationApi = inject(OrganizationApiService);
  private readonly rbacApi = inject(RbacApiService);
  private readonly i18n = inject(I18nService);

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
  readonly editor = signal<EditorState | null>(null);
  readonly platformAdministrator = computed(() => this.rbacApi.hasPermission('ORGANIZATION_MANAGE'));
  readonly t = (key: string, fallback?: string) => this.i18n.t(key, fallback);

  readonly rows = computed<UnitRow[]>(() => this.flattenUnits(this.units()));

  readonly parentOptions = computed(() => {
    const state = this.editor();
    if (!state) return [];
    return this.units().filter((unit) =>
      unit.active && unit.id !== state.id && this.parentTypeAllowed(unit.unitType, state.unitType));
  });

  ngOnInit(): void {
    this.loadCatalogs();
    if (this.platformAdministrator()) {
      this.loadOrganizations();
      return;
    }
    this.loadUnits();
  }

  loadUnits(): void {
    if (!this.canManageScope()) {
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

  onOrganizationChange(): void {
    this.units.set([]);
    this.clearMessages();
    this.loadUnits();
  }

  onIncludeInactiveChange(value: boolean): void {
    this.includeInactive.set(value);
    this.loadUnits();
  }

  canManageScope(): boolean {
    return !this.platformAdministrator() || this.selectedOrganizationId().length > 0;
  }

  openCreate(unitType: OrganizationalUnitType, parentId: string | null = null): void {
    this.clearMessages();
    this.editor.set({
      id: null,
      unitType,
      code: '',
      parentId,
      name: '',
      serviceCatalogCode: '',
    });
  }

  openEdit(unit: OrganizationalUnit): void {
    this.clearMessages();
    this.editor.set({
      id: unit.id,
      unitType: unit.unitType,
      code: unit.code,
      parentId: unit.parentId,
      name: unit.unitType === 'SERVICE' ? '' : unit.name,
      serviceCatalogCode: unit.serviceCatalogCode ?? '',
    });
  }

  closeEditor(): void {
    if (!this.busy()) this.editor.set(null);
  }

  canSubmit(state: EditorState): boolean {
    if (!state.code.trim()) return false;
    if (state.unitType === 'SERVICE') return state.serviceCatalogCode.length > 0;
    if (state.unitType === 'CARE_UNIT' && !state.parentId) return false;
    return state.name.trim().length >= 2;
  }

  submit(): void {
    const state = this.editor();
    if (!state || !this.canSubmit(state)) return;

    const payload: SaveOrganizationalUnitPayload = {
      code: state.code.trim(),
      unitType: state.unitType,
      parentId: state.parentId || null,
      name: state.unitType === 'SERVICE' ? null : state.name.trim(),
      serviceCatalogCode: state.unitType === 'SERVICE' ? state.serviceCatalogCode : null,
    };
    this.busy.set(true);
    this.clearMessages();
    const request = state.id
      ? this.api.updateUnit(state.id, payload, this.scopeOrganizationId())
      : this.api.createUnit(payload, this.scopeOrganizationId());
    request.subscribe({
      next: () => {
        this.busy.set(false);
        this.editor.set(null);
        this.successMessage.set(this.t('hospitalOrg.feedback.saved'));
        this.loadUnits();
      },
      error: (error: HttpErrorResponse) => this.handleOperationError(error),
    });
  }

  setActive(unit: OrganizationalUnit, active: boolean): void {
    this.busy.set(true);
    this.clearMessages();
    this.api.setActive(unit.id, active, this.scopeOrganizationId()).subscribe({
      next: () => {
        this.busy.set(false);
        this.successMessage.set(this.t(active ? 'hospitalOrg.feedback.activated' : 'hospitalOrg.feedback.deactivated'));
        this.loadUnits();
      },
      error: (error: HttpErrorResponse) => this.handleOperationError(error),
    });
  }

  canAddChild(unit: OrganizationalUnit): boolean {
    return unit.active && unit.unitType !== 'CARE_UNIT';
  }

  nextChildType(unit: OrganizationalUnit): OrganizationalUnitType {
    return switchChild(unit.unitType);
  }

  typeLabel(type: OrganizationalUnitType): string {
    return this.t(`hospitalOrg.unitType.${type}`);
  }

  serviceLabel(entry: HospitalServiceCatalogEntry): string {
    return this.i18n.locale() === 'en' ? entry.nameEn : entry.nameFr;
  }

  specialtyLabel(entry: MedicalSpecialtyCatalogEntry): string {
    return this.i18n.locale() === 'en' ? entry.nameEn : entry.nameFr;
  }

  editorTitle(state: EditorState): string {
    const action = state.id ? 'edit' : 'create';
    return this.t(`hospitalOrg.editor.${action}`);
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
        if (preferred) this.loadUnits(); else this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => this.handleError(error),
    });
  }

  private flattenUnits(units: OrganizationalUnit[]): UnitRow[] {
    const children = new Map<string | null, OrganizationalUnit[]>();
    for (const unit of units) {
      const key = unit.parentId;
      children.set(key, [...(children.get(key) ?? []), unit]);
    }
    for (const values of children.values()) values.sort((a, b) => a.name.localeCompare(b.name));

    const rows: UnitRow[] = [];
    const visit = (parentId: string | null, depth: number) => {
      for (const unit of children.get(parentId) ?? []) {
        rows.push({ unit, depth });
        visit(unit.id, depth + 1);
      }
    };
    visit(null, 0);
    return rows;
  }

  private parentTypeAllowed(parent: OrganizationalUnitType, child: OrganizationalUnitType): boolean {
    if (parent === 'POLE') return child === 'DEPARTMENT' || child === 'SERVICE';
    if (parent === 'DEPARTMENT') return child === 'SERVICE';
    return parent === 'SERVICE' && child === 'CARE_UNIT';
  }

  private handleOperationError(error: HttpErrorResponse): void {
    this.busy.set(false);
    this.handleError(error);
  }

  private handleError(error: HttpErrorResponse): void {
    const body = error.error as { detail?: string; error?: { message?: string } } | null;
    this.errorMessage.set(body?.error?.message ?? body?.detail ?? this.t('hospitalOrg.feedback.error'));
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

function switchChild(type: OrganizationalUnitType): OrganizationalUnitType {
  if (type === 'POLE') return 'DEPARTMENT';
  if (type === 'DEPARTMENT') return 'SERVICE';
  if (type === 'SERVICE') return 'CARE_UNIT';
  return 'CARE_UNIT';
}
