import { NgTemplateOutlet } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable, forkJoin, of, switchMap } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { interpolate } from '../../core/i18n/interpolate';
import { SpatialApiService } from '../../patient/spatial-api.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { IconComponent } from '../../shared/ui/icon.component';
import { LoadErrorComponent } from '../../shared/ui/load-error.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { HospitalOrganizationApiService } from '../hospital-organization/hospital-organization-api.service';
import { unitDisplayName } from '../hospital-organization/hospital-organization-hierarchy';
import { HospitalServiceCatalogEntry, OrganizationalUnit } from '../hospital-organization/hospital-organization.models';
import { OrganizationApiService } from '../organizations/organization-api.service';
import { Organization } from '../organizations/organizations.models';
import { RbacApiService } from '../rbac/rbac-api.service';
import { PlaceEditorComponent } from './configuration/place-editor.component';
import { EditorError, PlaceEditorRequest, PlaceSaveEvent, UnitOption } from './configuration/place-editor.models';
import { buildPlaceTree, currentAssignments, inactiveCount } from './configuration/place-tree';
import { RoomCardComponent, RoomService } from './configuration/room-card.component';
import {
  BedConfiguration,
  FacilityLocationNode,
  FacilitySpace,
  SpaceTypeEntry,
  UnitSpaceAssignment,
} from './spatial-configuration.models';

/** Chambres et lits : où sont les salles, quels lits ont les chambres, quel service utilise chaque salle. */
@Component({
  selector: 'app-spatial-configuration-page',
  standalone: true,
  imports: [
    FormsModule,
    NgTemplateOutlet,
    AppShellComponent,
    PageHeaderComponent,
    IconComponent,
    LoadErrorComponent,
    PlaceEditorComponent,
    RoomCardComponent,
  ],
  templateUrl: './spatial-configuration-page.component.html',
})
export class SpatialConfigurationPageComponent implements OnInit {
  private readonly spatialApi = inject(SpatialApiService);
  private readonly hospitalOrganizationApi = inject(HospitalOrganizationApiService);
  private readonly i18n = inject(I18nService);
  private readonly rbacApi = inject(RbacApiService);
  private readonly organizationApi = inject(OrganizationApiService);

  readonly t = (key: string) => this.i18n.t(key);

  readonly loading = signal(true);
  readonly loadFailed = signal(false);
  readonly busy = signal(false);
  readonly feedback = signal('');
  readonly editorRequest = signal<PlaceEditorRequest | null>(null);
  readonly editorError = signal<EditorError | null>(null);
  readonly showInactive = signal(false);

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
  readonly canManageScope = computed(() => !this.platformAdministrator() || this.selectedOrganizationId().length > 0);

  readonly tree = computed(() => buildPlaceTree(this.locations(), this.spaces(), this.showInactive()));
  readonly inactiveTotal = computed(() => inactiveCount(this.locations(), this.spaces()));
  readonly isEmpty = computed(() => this.locations().length === 0 && this.spaces().length === 0);
  readonly unitOptions = computed<UnitOption[]>(() => this.units()
    .filter((unit) => unit.active && (unit.unitType === 'SERVICE' || unit.unitType === 'CARE_UNIT'))
    .map((unit) => ({ id: unit.id, label: this.unitLabel(unit.id) }))
    .sort((left, right) => left.label.localeCompare(right.label)));

  ngOnInit(): void {
    if (this.platformAdministrator()) this.loadOrganizations();
    else this.loadConfiguration();
  }

  loadConfiguration(): void {
    if (!this.canManageScope()) {
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    this.loadFailed.set(false);
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
        const rooms = data.spaces.filter((space) => space.inpatientProfile);
        if (rooms.length === 0) return of({} as Record<string, BedConfiguration[]>);
        return forkJoin(Object.fromEntries(rooms.map((room) => [room.id, this.spatialApi.listBeds(room.id, scope)]))) as
          Observable<Record<string, BedConfiguration[]>>;
      }),
    ).subscribe({
      next: (beds) => {
        this.bedsBySpace.set(beds);
        this.loading.set(false);
      },
      error: () => {
        this.loadFailed.set(true);
        this.loading.set(false);
      },
    });
  }

  retry(): void {
    if (this.platformAdministrator() && this.organizations().length === 0) this.loadOrganizations();
    else this.loadConfiguration();
  }

  onOrganizationChange(organizationId: string): void {
    this.selectedOrganizationId.set(organizationId);
    this.feedback.set('');
    this.loadConfiguration();
  }

  openEditor(request: PlaceEditorRequest): void {
    this.editorError.set(null);
    this.feedback.set('');
    this.editorRequest.set(request);
  }

  /** Le niveau de confort n'est pas dans la liste des salles : il est lu avant d'ouvrir la fenêtre. */
  editSpace(space: FacilitySpace): void {
    if (!space.inpatientProfile) {
      this.openEditor({ kind: 'space', space });
      return;
    }
    this.spatialApi.getInpatientProfile(space.id, this.scopeOrganizationId()).subscribe({
      next: (profile) => this.openEditor({ kind: 'space', space, comfortLevel: profile.comfortLevel }),
      error: () => this.openEditor({ kind: 'space', space }),
    });
  }

  closeEditor(): void {
    if (!this.busy()) this.editorRequest.set(null);
  }

  save(event: PlaceSaveEvent): void {
    this.runEditorAction(this.saveRequest(event), 'spatial.config.saved');
  }

  /** Suppression d'un lit, ou désactivation d'un lieu ou d'une salle, confirmée dans la fenêtre. */
  remove(): void {
    const request = this.editorRequest();
    const scope = this.scopeOrganizationId();
    if (request?.kind === 'bed' && request.bed) {
      this.runEditorAction(this.spatialApi.deleteBed(request.bed.id, scope), 'spatial.config.bedRemoved');
    } else if (request?.kind === 'location' && request.location) {
      this.runEditorAction(this.spatialApi.setLocationActive(request.location.id, false, scope), 'spatial.config.deactivated');
    } else if (request?.kind === 'space' && request.space) {
      this.runEditorAction(this.spatialApi.setSpaceActive(request.space.id, false, scope), 'spatial.config.deactivated');
    }
  }

  reactivateLocation(location: FacilityLocationNode): void {
    this.runPageAction(this.spatialApi.setLocationActive(location.id, true, this.scopeOrganizationId()), location.name);
  }

  reactivateSpace(space: FacilitySpace): void {
    this.runPageAction(this.spatialApi.setSpaceActive(space.id, true, this.scopeOrganizationId()), space.name);
  }

  beds(spaceId: string): BedConfiguration[] {
    return this.bedsBySpace()[spaceId] ?? [];
  }

  roomServices(spaceId: string): RoomService[] {
    return currentAssignments(this.assignments(), spaceId)
      .map((assignment) => ({ assignment, label: this.unitLabel(assignment.organizationalUnitId) }));
  }

  spaceTypeLabel(code: string): string {
    const entry = this.spaceTypes().find((type) => type.code === code);
    if (!entry) return code;
    return this.i18n.locale() === 'en' ? entry.nameEn : entry.nameFr;
  }

  unitLabel(unitId: string): string {
    const unit = this.units().find((item) => item.id === unitId);
    return unit ? unitDisplayName(unit, this.serviceCatalog(), this.i18n.locale()) : this.t('spatial.config.unknownService');
  }

  private saveRequest(event: PlaceSaveEvent): Observable<unknown> {
    const scope = this.scopeOrganizationId();
    switch (event.kind) {
      case 'location':
        return event.id ? this.spatialApi.updateLocation(event.id, event.payload, scope) : this.spatialApi.createLocation(event.payload, scope);
      case 'space': {
        const saved = event.id ? this.spatialApi.updateSpace(event.id, event.payload, scope) : this.spatialApi.createSpace(event.payload, scope);
        const comfortLevel = event.comfortLevel;
        if (!comfortLevel) return saved;
        return saved.pipe(switchMap((space) => this.spatialApi.saveInpatientProfile(space.id, { comfortLevel }, scope)));
      }
      case 'bed':
        return event.id ? this.spatialApi.updateBed(event.id, event.payload, scope) : this.spatialApi.createBed(event.payload, scope);
      case 'assignment':
        return event.id
          ? this.spatialApi.updateUnitSpaceAssignment(event.id, event.payload, scope)
          : this.spatialApi.createUnitSpaceAssignment(event.payload, scope);
    }
  }

  private runEditorAction(request: Observable<unknown>, successKey: string): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.editorError.set(null);
    request.subscribe({
      next: () => {
        this.busy.set(false);
        this.editorRequest.set(null);
        this.feedback.set(this.t(successKey));
        this.loadConfiguration();
      },
      error: (error: unknown) => {
        this.busy.set(false);
        this.editorError.set(this.toEditorError(error));
      },
    });
  }

  private runPageAction(request: Observable<unknown>, name: string): void {
    if (this.busy()) return;
    this.busy.set(true);
    request.subscribe({
      next: () => {
        this.busy.set(false);
        this.feedback.set(interpolate(this.t('spatial.config.reactivated'), { name }));
        this.loadConfiguration();
      },
      error: (error: unknown) => {
        this.busy.set(false);
        this.feedback.set(this.toEditorError(error).message);
      },
    });
  }

  /** Message traduit selon le statut ; la précision métier du serveur (en français) n'est montrée qu'en français. */
  private toEditorError(error: unknown): EditorError {
    const status = error instanceof HttpErrorResponse ? error.status : 0;
    const key = status === 403 ? 'forbidden' : status === 409 ? 'conflict' : status === 400 || status === 422 ? 'invalid' : 'generic';
    const body = error instanceof HttpErrorResponse ? error.error as { detail?: unknown } | null : null;
    const detail = this.i18n.locale() === 'fr' && typeof body?.detail === 'string' ? body.detail : '';
    return { message: this.t(`spatial.config.error.${key}`), detail };
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
      error: () => {
        this.loadFailed.set(true);
        this.loading.set(false);
      },
    });
  }

  private scopeOrganizationId(): string | undefined {
    return this.platformAdministrator() ? this.selectedOrganizationId() || undefined : undefined;
  }
}
