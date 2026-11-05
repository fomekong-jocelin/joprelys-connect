import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { Observable, finalize } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { interpolate } from '../../core/i18n/interpolate';
import { SpatialApiService } from '../../patient/spatial-api.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { DialogService } from '../../shared/ui/dialog.service';
import { IconComponent } from '../../shared/ui/icon.component';
import { LoadErrorComponent } from '../../shared/ui/load-error.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { RbacApiService } from '../rbac/rbac-api.service';
import {
  BED_STATE_STYLES,
  BedActionId,
  BedDisplayState,
  BedPermissions,
  FILTERABLE_BED_STATES,
  bedDisplayState,
} from './bed-display-state';
import { BedTileComponent } from './bed-tile.component';
import { BedConfiguration, SpaceOccupancyView } from './spatial-configuration.models';

export type BedStateFilter = BedDisplayState | 'ALL';

interface RoomView {
  readonly room: SpaceOccupancyView;
  readonly beds: readonly BedConfiguration[];
}

/** Suivi des lits : toutes les chambres d'un coup d'œil, filtrables par état. */
@Component({
  selector: 'app-spatial-management-page',
  standalone: true,
  imports: [RouterLink, IconComponent, AppShellComponent, PageHeaderComponent, LoadErrorComponent, BedTileComponent],
  templateUrl: './spatial-management-page.component.html',
})
export class SpatialManagementPageComponent implements OnInit {
  private readonly spatialApi = inject(SpatialApiService);
  private readonly i18n = inject(I18nService);
  private readonly rbacApi = inject(RbacApiService);
  private readonly dialog = inject(DialogService);

  readonly t = (key: string) => this.i18n.t(key);
  readonly filterStates = FILTERABLE_BED_STATES;
  readonly stateStyles = BED_STATE_STYLES;

  readonly rooms = signal<SpaceOccupancyView[]>([]);
  readonly loading = signal(false);
  readonly refreshing = signal(false);
  /** Clé i18n de l'échec de chargement : distinct de « aucune chambre configurée ». */
  readonly loadError = signal<string | null>(null);
  readonly loadedAt = signal<Date | null>(null);
  readonly stateFilter = signal<BedStateFilter>('ALL');
  /** Lit dont une action est en cours : empêche les doubles clics et les actions concurrentes. */
  readonly pendingBedId = signal<string | null>(null);
  readonly bedActionError = signal<string | null>(null);
  /** Annonce vocale du dernier changement d'état (région aria-live). */
  readonly announcement = signal('');

  readonly permissions = computed<BedPermissions>(() => ({
    capacity: this.rbacApi.hasPermission('BED_OPERATIONAL_STATUS_MANAGE'),
    cleaning: this.rbacApi.hasPermission('BED_CLEANING_MANAGE'),
    maintenance: this.rbacApi.hasPermission('BED_MAINTENANCE_MANAGE'),
  }));
  readonly canConfigure = computed(() => this.rbacApi.hasPermission('SPATIAL_CONFIGURATION_MANAGE'));

  readonly totals = computed(() => this.rooms().reduce(
    (sum, room) => ({
      free: sum.free + room.availableBeds,
      open: sum.open + room.openBeds,
      occupied: sum.occupied + room.occupiedBeds,
      installed: sum.installed + room.installedBeds,
    }),
    { free: 0, open: 0, occupied: 0, installed: 0 },
  ));

  readonly occupancyRate = computed(() => {
    const { occupied, open } = this.totals();
    return open === 0 ? 0 : Math.round((occupied / open) * 100);
  });

  readonly stateCounts = computed(() => {
    const counts: Record<BedStateFilter, number> = {
      ALL: 0, FREE: 0, OCCUPIED: 0, CLEANING: 0, MAINTENANCE: 0, CLOSED: 0, UNAVAILABLE: 0,
    };
    for (const room of this.rooms()) {
      for (const bed of room.beds) {
        counts.ALL++;
        counts[bedDisplayState(bed)]++;
      }
    }
    return counts;
  });

  /** Chambres affichées : toutes sans filtre, sinon seulement celles qui ont au moins un lit dans l'état choisi. */
  readonly visibleRooms = computed<RoomView[]>(() => {
    const filter = this.stateFilter();
    return this.rooms()
      .map((room) => ({
        room,
        beds: filter === 'ALL' ? room.beds : room.beds.filter((bed) => bedDisplayState(bed) === filter),
      }))
      .filter((view) => filter === 'ALL' || view.beds.length > 0);
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const hasData = this.rooms().length > 0;
    (hasData ? this.refreshing : this.loading).set(true);
    this.loadError.set(null);
    this.spatialApi.listInpatientOccupancy()
      .pipe(finalize(() => {
        this.loading.set(false);
        this.refreshing.set(false);
      }))
      .subscribe({
        next: (rooms) => {
          this.rooms.set(rooms);
          this.loadedAt.set(new Date());
        },
        error: () => this.loadError.set('spatial.loadError.occupancy'),
      });
  }

  setFilter(filter: BedStateFilter): void {
    this.stateFilter.set(this.stateFilter() === filter ? 'ALL' : filter);
  }

  onBedAction(bed: BedConfiguration, action: BedActionId): void {
    switch (action) {
      case 'START_CLEANING': return this.changeBedCleaningStatus(bed.id, 'CLEANING');
      case 'END_CLEANING': return this.changeBedCleaningStatus(bed.id, 'READY');
      case 'START_MAINTENANCE': return this.changeBedMaintenanceStatus(bed.id, 'MAINTENANCE');
      case 'END_MAINTENANCE': return this.changeBedMaintenanceStatus(bed.id, 'READY');
      case 'CLOSE': void this.changeBedCapacityStatus(bed.id, 'CLOSED'); return;
      case 'REOPEN': void this.changeBedCapacityStatus(bed.id, 'OPEN'); return;
    }
  }

  changeBedCleaningStatus(bedId: string, newStatus: 'CLEANING' | 'READY'): void {
    this.runBedAction(bedId, () => this.spatialApi.updateBedCleaningStatus(bedId, newStatus));
  }

  changeBedMaintenanceStatus(bedId: string, newStatus: 'MAINTENANCE' | 'READY'): void {
    this.runBedAction(bedId, () => this.spatialApi.updateBedMaintenanceStatus(bedId, newStatus));
  }

  /** La fermeture retire un lit de la capacité d'accueil : elle est confirmée avec un récapitulatif. */
  async changeBedCapacityStatus(bedId: string, newStatus: 'OPEN' | 'CLOSED'): Promise<void> {
    if (this.pendingBedId()) return;
    if (newStatus === 'CLOSED' && !(await this.confirmBedClosure(bedId))) return;
    this.runBedAction(bedId, () => this.spatialApi.updateBedCapacityStatus(bedId, newStatus));
  }

  /** Libellé accordé au nombre (« 1 lit libre », « 2 lits libres ») selon les règles de la langue courante. */
  plural(baseKey: string, count: number, params: Record<string, string | number> = {}): string {
    const form = new Intl.PluralRules(this.i18n.currentLanguage()).select(count) === 'one' ? 'one' : 'other';
    return interpolate(this.t(`${baseKey}.${form}`), { count, ...params });
  }

  format(key: string, params: Record<string, string | number>): string {
    return interpolate(this.t(key), params);
  }

  loadedAtLabel(): string {
    const at = this.loadedAt();
    if (!at) return '';
    const time = at.toLocaleTimeString(this.i18n.currentLanguage(), { hour: '2-digit', minute: '2-digit' });
    return this.format('spatial.tracking.updatedAt', { time });
  }

  private confirmBedClosure(bedId: string): Promise<boolean> {
    const room = this.rooms().find((candidate) => candidate.beds.some((bed) => bed.id === bedId));
    const bed = room?.beds.find((candidate) => candidate.id === bedId);
    const closesFreeBed = bed ? bedDisplayState(bed) === 'FREE' : false;
    const params = {
      bed: bed?.bedNumber ?? bedId,
      space: room?.spaceName ?? '',
      available: Math.max(this.totals().free - (closesFreeBed ? 1 : 0), 0),
    };
    return this.dialog.confirm({
      title: interpolate(this.t('spatial.closeBed.confirmTitle'), params),
      message: interpolate(this.t('spatial.closeBed.confirmMessage'), params),
      confirmLabel: this.t('spatial.closeBed.confirmAction'),
      cancelLabel: this.t('common.cancel'),
      tone: 'danger',
      icon: 'bed',
    });
  }

  private runBedAction(bedId: string, request: () => Observable<unknown>): void {
    if (this.pendingBedId()) return;
    this.pendingBedId.set(bedId);
    this.bedActionError.set(null);
    this.announcement.set('');
    request().pipe(finalize(() => this.pendingBedId.set(null))).subscribe({
      next: () => {
        this.announcement.set(this.format('spatial.tracking.updated', { bed: this.bedNumber(bedId) }));
        this.load();
      },
      error: (error: unknown) => {
        const conflict = error instanceof HttpErrorResponse && error.status === 409;
        const forbidden = error instanceof HttpErrorResponse && error.status === 403;
        this.bedActionError.set(this.t(
          conflict ? 'spatial.bedAction.conflict' : forbidden ? 'spatial.bedAction.forbidden' : 'spatial.bedAction.error',
        ));
        if (conflict) this.load();
      },
    });
  }

  private bedNumber(bedId: string): string {
    for (const room of this.rooms()) {
      const bed = room.beds.find((candidate) => candidate.id === bedId);
      if (bed) return bed.bedNumber;
    }
    return '';
  }
}
