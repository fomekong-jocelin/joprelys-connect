import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin, Observable, of, switchMap } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { SpatialApiService } from '../../patient/spatial-api.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { IconComponent } from '../../shared/ui/icon.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { HospitalOrganizationApiService } from '../hospital-organization/hospital-organization-api.service';
import { HospitalServiceCatalogEntry, OrganizationalUnit } from '../hospital-organization/hospital-organization.models';
import { OrganizationApiService } from '../organizations/organization-api.service';
import { Organization } from '../organizations/organizations.models';
import { RbacApiService } from '../rbac/rbac-api.service';
import {
  BedConfiguration,
  FacilityLocationNode,
  FacilityLocationNodeType,
  FacilitySpace,
  SpaceTypeEntry,
  UnitSpaceAssignment,
} from './spatial-configuration.models';

type EditorKind = 'location' | 'space' | 'bed' | 'assignment';

interface EditorState {
  kind: EditorKind;
  id: string | null;
  code: string;
  name: string;
  parentId: string;
  nodeType: FacilityLocationNodeType;
  locationNodeId: string;
  spaceTypeCode: string;
  enableInpatientProfile: boolean;
  comfortLevel: string;
  spaceId: string;
  bedNumber: string;
  organizationalUnitId: string;
  validFrom: string;
  validTo: string;
}

@Component({
  selector: 'app-spatial-configuration-page',
  standalone: true,
  imports: [CommonModule, FormsModule, AppShellComponent, PageHeaderComponent, IconComponent],
  templateUrl: './spatial-configuration-page.component.html',
})
export class SpatialConfigurationPageComponent implements OnInit {
  private readonly spatialApi = inject(SpatialApiService);
  private readonly hospitalOrganizationApi = inject(HospitalOrganizationApiService);
  private readonly i18n = inject(I18nService);
  private readonly rbacApi = inject(RbacApiService);
  private readonly organizationApi = inject(OrganizationApiService);

  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly editor = signal<EditorState | null>(null);

  readonly locations = signal<FacilityLocationNode[]>([]);
  readonly spaces = signal<FacilitySpace[]>([]);
  readonly spaceTypes = signal<SpaceTypeEntry[]>([]);
  readonly units = signal<OrganizationalUnit[]>([]);
  readonly serviceCatalog = signal<HospitalServiceCatalogEntry[]>([]);
  readonly assignments = signal<UnitSpaceAssignment[]>([]);
  readonly bedsBySpace = signal<Record<string, BedConfiguration[]>>({});

  readonly platformAdministrator = computed(() => this.rbacApi.hasPermission('ORGANIZATION_MANAGE'));
  readonly organizations = signal<Organization[]>([]);
  readonly selectedOrganizationId = signal('');
  readonly activeUnits = computed(() => this.units().filter((unit) => unit.active && (unit.unitType === 'SERVICE' || unit.unitType === 'CARE_UNIT')));
  readonly activeSpaces = computed(() => this.spaces().filter((space) => space.active));
  readonly inpatientSpaces = computed(() => this.spaces().filter((space) => space.inpatientProfile));

  readonly t = (key: string, fallback = key) => this.i18n.t(key, fallback);

  ngOnInit(): void {
    if (this.platformAdministrator()) {
      this.loadOrganizations();
      return;
    }
    this.loadConfiguration();
  }

  loadConfiguration(preserveMessages = false): void {
    if (!this.canManageScope()) {
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    if (!preserveMessages) this.clearMessages();
    const scope = this.scopeOrganizationId();
    forkJoin({
      locations: this.spatialApi.listLocations(scope, true),
      spaces: this.spatialApi.listSpaces(scope, undefined, true),
      spaceTypes: this.spatialApi.listSpaceTypes(),
      units: this.hospitalOrganizationApi.listUnits(scope, true),
      serviceCatalog: this.hospitalOrganizationApi.listServiceCatalog(),
      assignments: this.spatialApi.listUnitSpaceAssignments(scope),
    }).pipe(
      switchMap((data) => {
        this.locations.set(data.locations);
        this.spaces.set(data.spaces);
        this.spaceTypes.set(data.spaceTypes);
        this.units.set(data.units);
        this.serviceCatalog.set(data.serviceCatalog);
        this.assignments.set(data.assignments);
        const inpatient = data.spaces.filter((space) => space.inpatientProfile);
        if (inpatient.length === 0) return of({} as Record<string, BedConfiguration[]>);
        const requests = Object.fromEntries(inpatient.map((space) => [space.id, this.spatialApi.listBeds(space.id, scope)]));
        return forkJoin(requests) as Observable<Record<string, BedConfiguration[]>>;
      }),
    ).subscribe({
      next: (beds) => {
        this.bedsBySpace.set(beds);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(this.extractError(error));
        this.loading.set(false);
      },
    });
  }

  onOrganizationChange(): void {
    this.locations.set([]);
    this.spaces.set([]);
    this.units.set([]);
    this.assignments.set([]);
    this.bedsBySpace.set({});
    this.loadConfiguration();
  }

  canManageScope(): boolean {
    return !this.platformAdministrator() || this.selectedOrganizationId().length > 0;
  }

  openLocationEditor(location?: FacilityLocationNode): void {
    this.editor.set({
      ...this.emptyEditor('location'),
      id: location?.id ?? null,
      code: location?.code ?? '',
      name: location?.name ?? '',
      parentId: location?.parentId ?? '',
      nodeType: location?.nodeType ?? 'SITE',
    });
  }

  openSpaceEditor(space?: FacilitySpace): void {
    const type = this.spaceTypes().find((entry) => entry.code === space?.spaceTypeCode);
    this.editor.set({
      ...this.emptyEditor('space'),
      id: space?.id ?? null,
      code: space?.code ?? '',
      name: space?.name ?? '',
      locationNodeId: space?.locationNodeId ?? '',
      spaceTypeCode: space?.spaceTypeCode ?? this.spaceTypes()[0]?.code ?? '',
      enableInpatientProfile: space?.inpatientProfile ?? false,
      comfortLevel: type?.inpatientCompatible ? 'STANDARD' : 'STANDARD',
    });
    if (space?.inpatientProfile) {
      this.spatialApi.getInpatientProfile(space.id, this.scopeOrganizationId()).subscribe({
        next: (profile) => {
          const current = this.editor();
          if (current?.kind === 'space' && current.id === space.id) {
            this.editor.set({ ...current, comfortLevel: profile.comfortLevel });
          }
        },
      });
    }
  }

  openBedEditor(space: FacilitySpace, bed?: BedConfiguration): void {
    this.editor.set({
      ...this.emptyEditor('bed'),
      id: bed?.id ?? null,
      spaceId: space.id,
      bedNumber: bed?.bedNumber ?? '',
    });
  }

  openAssignmentEditor(assignment?: UnitSpaceAssignment): void {
    this.editor.set({
      ...this.emptyEditor('assignment'),
      id: assignment?.id ?? null,
      organizationalUnitId: assignment?.organizationalUnitId ?? this.activeUnits()[0]?.id ?? '',
      spaceId: assignment?.spaceId ?? this.activeSpaces()[0]?.id ?? '',
      validFrom: assignment ? this.toLocalDateTime(assignment.validFrom) : this.toLocalDateTime(new Date().toISOString()),
      validTo: assignment?.validTo ? this.toLocalDateTime(assignment.validTo) : '',
    });
  }

  closeEditor(): void {
    if (!this.busy()) this.editor.set(null);
  }

  submitEditor(): void {
    const form = this.editor();
    if (!form || !this.canSubmit(form)) return;
    this.busy.set(true);
    this.clearMessages();
    this.saveRequest(form).subscribe({
      next: () => {
        this.editor.set(null);
        this.successMessage.set(this.t('spatial.config.saveSuccess', 'Configuration enregistrée.'));
        this.busy.set(false);
        this.loadConfiguration(true);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(this.extractError(error));
        this.busy.set(false);
      },
    });
  }

  canSubmit(form: EditorState): boolean {
    if (form.kind === 'location') return Boolean(form.code.trim() && form.name.trim() && form.nodeType);
    if (form.kind === 'space') return Boolean(form.code.trim() && form.name.trim() && form.spaceTypeCode);
    if (form.kind === 'bed') return Boolean(form.spaceId && form.bedNumber.trim());
    return Boolean(form.organizationalUnitId && form.spaceId && form.validFrom);
  }

  setLocationActive(location: FacilityLocationNode): void {
    this.runMutation(this.spatialApi.setLocationActive(location.id, !location.active, this.scopeOrganizationId()));
  }

  setSpaceActive(space: FacilitySpace): void {
    this.runMutation(this.spatialApi.setSpaceActive(space.id, !space.active, this.scopeOrganizationId()));
  }

  deleteBed(bed: BedConfiguration): void {
    this.runMutation(this.spatialApi.deleteBed(bed.id, this.scopeOrganizationId()));
  }

  beds(spaceId: string): BedConfiguration[] {
    return this.bedsBySpace()[spaceId] ?? [];
  }

  locationLabel(locationId?: string | null): string {
    if (!locationId) return this.t('spatial.location.root', 'Directement sous l’établissement');
    const location = this.locations().find((item) => item.id === locationId);
    return location ? `${location.name} · ${location.code}` : locationId;
  }

  spaceTypeLabel(code: string): string {
    const entry = this.spaceTypes().find((type) => type.code === code);
    if (!entry) return code;
    return this.i18n.currentLanguage() === 'en' ? entry.nameEn : entry.nameFr;
  }

  unitLabel(unitId: string): string {
    const unit = this.units().find((item) => item.id === unitId);
    if (!unit) return unitId;
    if (unit.name) return unit.name;
    const catalog = this.serviceCatalog().find((entry) => entry.code === unit.serviceCatalogCode);
    return catalog ? (this.i18n.currentLanguage() === 'en' ? catalog.nameEn : catalog.nameFr) : unit.code;
  }

  spaceLabel(spaceId: string): string {
    const space = this.spaces().find((item) => item.id === spaceId);
    return space ? space.name : spaceId;
  }

  isInpatientCompatible(code: string): boolean {
    return this.spaceTypes().find((type) => type.code === code)?.inpatientCompatible ?? false;
  }

  editorTitle(form: EditorState): string {
    const verb = form.id ? this.t('common.edit', 'Modifier') : this.t('common.create', 'Créer');
    const noun = {
      location: this.t('spatial.location.singular', 'localisation'),
      space: this.t('spatial.space.singular', 'espace'),
      bed: this.t('spatial.bed.singular', 'lit'),
      assignment: this.t('spatial.assignment.singular', 'rattachement'),
    }[form.kind];
    return `${verb} ${noun}`;
  }

  private saveRequest(form: EditorState): Observable<unknown> {
    const scope = this.scopeOrganizationId();
    if (form.kind === 'location') {
      const payload = {
        parentId: form.parentId || null,
        code: form.code.trim(),
        name: form.name.trim(),
        nodeType: form.nodeType,
      };
      return form.id
        ? this.spatialApi.updateLocation(form.id, payload, scope)
        : this.spatialApi.createLocation(payload, scope);
    }
    if (form.kind === 'space') {
      const payload = {
        locationNodeId: form.locationNodeId || null,
        code: form.code.trim(),
        name: form.name.trim(),
        spaceTypeCode: form.spaceTypeCode,
        enableInpatientProfile: form.enableInpatientProfile,
      };
      const saveSpace = form.id
        ? this.spatialApi.updateSpace(form.id, payload, scope)
        : this.spatialApi.createSpace(payload, scope);
      return saveSpace.pipe(switchMap((saved) => {
        if (!saved.inpatientProfile && !form.enableInpatientProfile) return of(saved);
        return this.spatialApi.saveInpatientProfile(saved.id, { comfortLevel: form.comfortLevel || 'STANDARD' }, scope);
      }));
    }
    if (form.kind === 'bed') {
      const payload = { spaceId: form.spaceId, bedNumber: form.bedNumber.trim() };
      return form.id
        ? this.spatialApi.updateBed(form.id, payload, scope)
        : this.spatialApi.createBed(payload, scope);
    }
    const payload = {
      organizationalUnitId: form.organizationalUnitId,
      spaceId: form.spaceId,
      validFrom: new Date(form.validFrom).toISOString(),
      validTo: form.validTo ? new Date(form.validTo).toISOString() : null,
    };
    return form.id
      ? this.spatialApi.updateUnitSpaceAssignment(form.id, payload, scope)
      : this.spatialApi.createUnitSpaceAssignment(payload, scope);
  }

  private runMutation(request: Observable<unknown>): void {
    this.busy.set(true);
    this.clearMessages();
    request.subscribe({
      next: () => {
        this.successMessage.set(this.t('spatial.config.saveSuccess', 'Configuration enregistrée.'));
        this.busy.set(false);
        this.loadConfiguration(true);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(this.extractError(error));
        this.busy.set(false);
      },
    });
  }

  private emptyEditor(kind: EditorKind): EditorState {
    return {
      kind,
      id: null,
      code: '',
      name: '',
      parentId: '',
      nodeType: 'SITE',
      locationNodeId: '',
      spaceTypeCode: '',
      enableInpatientProfile: false,
      comfortLevel: 'STANDARD',
      spaceId: '',
      bedNumber: '',
      organizationalUnitId: '',
      validFrom: '',
      validTo: '',
    };
  }

  private loadOrganizations(): void {
    this.loading.set(true);
    this.organizationApi.list().subscribe({
      next: (organizations) => {
        this.organizations.set(organizations);
        const preferred = organizations.find((organization) => organization.status === 'ACTIVE') ?? organizations[0];
        this.selectedOrganizationId.set(preferred?.id ?? '');
        if (preferred) this.loadConfiguration();
        else this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(this.extractError(error));
        this.loading.set(false);
      },
    });
  }

  private clearMessages(): void {
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  private extractError(error: HttpErrorResponse): string {
    const body = error.error as { detail?: string; error?: { message?: string } } | null;
    return body?.error?.message ?? body?.detail ?? this.t('spatial.config.genericError', 'La configuration spatiale n’a pas pu être enregistrée.');
  }

  private toLocalDateTime(value: string): string {
    const date = new Date(value);
    const pad = (part: number) => String(part).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }

  private scopeOrganizationId(): string | undefined {
    return this.platformAdministrator() ? this.selectedOrganizationId() || undefined : undefined;
  }
}
