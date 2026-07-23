import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { I18nService } from '../../core/i18n/i18n.service';
import { SpatialApiService } from '../../patient/spatial-api.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { IconComponent } from '../../shared/ui/icon.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { RbacApiService } from '../rbac/rbac-api.service';
import { BedConfiguration, FacilitySpace, SpaceOccupancyView } from './spatial-configuration.models';

@Component({
  selector: 'app-spatial-management-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, IconComponent, AppShellComponent, PageHeaderComponent],
  template: `
    <app-shell>
      <app-page-header
        [title]="t('spatial.title', 'Capacité des espaces')"
        [subtitle]="t('spatial.subtitle', 'Suivez les lits par espace d’hébergement sans confondre service médical et salle physique.')"
      >
        @if (canConfigure()) {
          <a routerLink="/clinic/spatial/configuration" class="ui-button ui-button-secondary">
            <app-ui-icon name="building" /> {{ t('spatial.config.open', 'Configurer les espaces') }}
          </a>
        }
        <div class="flex w-full items-center gap-2 sm:w-auto">
          <label class="shrink-0 text-xs font-bold uppercase tracking-wider text-[var(--text-secondary)]">
            {{ t('spatial.space.singular', 'Espace') }}&nbsp;:
          </label>
          <select
            [ngModel]="selectedSpaceId()"
            (ngModelChange)="selectedSpaceId.set($event); onSpaceChange()"
            class="ui-select min-w-0 flex-1 text-xs sm:min-w-[240px]"
          >
            <option value="">-- {{ t('spatial.space.select', 'Sélectionner un espace') }} --</option>
            @for (space of spaces(); track space.id) {
              <option [value]="space.id">{{ space.name }} · {{ space.code }}</option>
            }
          </select>
        </div>
      </app-page-header>

      <main class="app-container min-w-0 overflow-x-hidden pb-12">
        @if (loading()) {
          <div class="flex items-center justify-center py-12"><div class="h-8 w-8 animate-spin rounded-full border-b-2 border-brand-cyan"></div></div>
        } @else if (spaces().length === 0) {
          <div class="ui-card-muted p-12 text-center">
            <app-ui-icon name="bed" class="mb-4 text-4xl text-brand-cyan" />
            <h3 class="mb-1 text-sm font-bold text-[var(--text-primary)]">{{ t('spatial.capacity.noInpatientSpace', 'Aucun espace d’hébergement configuré') }}</h3>
            <p class="text-xs text-[var(--text-muted)]">{{ t('spatial.capacity.noInpatientSpaceDetail', 'Créez un Space compatible avec les lits depuis la configuration spatiale.') }}</p>
          </div>
        } @else if (!selectedSpaceId()) {
          <div class="ui-card-muted p-12 text-center">
            <app-ui-icon name="bed" class="mb-4 text-4xl text-brand-cyan" />
            <h3 class="text-sm font-bold text-[var(--text-primary)]">{{ t('spatial.space.select', 'Sélectionner un espace') }}</h3>
          </div>
        } @else if (occupancy(); as current) {
          <div class="space-y-6">
            <section class="ui-card p-4 sm:p-5">
              <p class="text-[10px] font-black uppercase tracking-wider text-brand-cyan">{{ current.spaceCode }}</p>
              <h2 class="mt-1 break-words text-lg font-black text-[var(--text-primary)]">{{ current.spaceName }}</h2>
            </section>

            <section class="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6">
              <div class="ui-card-muted p-4"><p class="text-[10px] font-bold uppercase text-[var(--text-muted)]">{{ t('spatial.capacity.installed', 'Installés') }}</p><p class="mt-1 text-lg font-black text-[var(--text-primary)]">{{ current.installedBeds }}</p></div>
              <div class="ui-card-muted p-4"><p class="text-[10px] font-bold uppercase text-[var(--text-muted)]">{{ t('spatial.capacity.open', 'Ouverts') }}</p><p class="mt-1 text-lg font-black text-[var(--text-primary)]">{{ current.openBeds }}</p></div>
              <div class="ui-card-muted p-4"><p class="text-[10px] font-bold uppercase text-[var(--text-muted)]">{{ t('spatial.capacity.ready', 'Prêts') }}</p><p class="mt-1 text-lg font-black text-[var(--brand-info)]">{{ current.readyBeds }}</p></div>
              <div class="ui-card-muted p-4"><p class="text-[10px] font-bold uppercase text-[var(--text-muted)]">{{ t('spatial.occupiedBeds', 'Occupés') }}</p><p class="mt-1 text-lg font-black text-[var(--brand-danger-text)]">{{ current.occupiedBeds }}</p></div>
              <div class="ui-card-muted p-4"><p class="text-[10px] font-bold uppercase text-[var(--text-muted)]">{{ t('spatial.capacity.available', 'Disponibles') }}</p><p class="mt-1 text-lg font-black text-[var(--brand-success-text)]">{{ current.availableBeds }}</p></div>
              <div class="ui-card-muted p-4"><p class="text-[10px] font-bold uppercase text-[var(--text-muted)]">{{ t('spatial.occupancyRate', 'Occupation') }}</p><p class="mt-1 text-lg font-black text-[var(--brand-info)]">{{ occupancyRate() }} %</p></div>
            </section>

            <section class="ui-card p-4 sm:p-5">
              <div class="mb-4">
                <p class="text-[10px] font-bold uppercase tracking-wider text-brand-cyan">{{ t('spatial.beds.title', 'Lits') }}</p>
                <h3 class="mt-1 text-base font-bold text-[var(--text-primary)]">{{ t('spatial.capacity.operationalStatus', 'État opérationnel') }}</h3>
              </div>
              <div class="grid min-w-0 grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
                @for (bed of current.beds; track bed.id) {
                  <article class="min-w-0 rounded-[var(--radius-brand-md)] border p-3 transition-all" [ngStyle]="getBedStyle(bed)">
                    <div class="flex items-start justify-between gap-2">
                      <span class="min-w-0 break-all text-sm font-black text-[var(--text-primary)]"><app-ui-icon name="bed" /> {{ bed.bedNumber }}</span>
                      <span class="shrink-0 rounded-sm border px-1.5 py-0.5 text-[9px] font-black uppercase" [ngStyle]="getCapacityBadgeStyle(bed.capacityStatus)">{{ t('spatial.capacity.status.' + bed.capacityStatus, bed.capacityStatus) }}</span>
                    </div>
                    <div class="mt-2 flex flex-wrap gap-1">
                      <span class="rounded-sm px-1.5 py-0.5 text-[9px] font-black uppercase" [ngStyle]="getReadinessBadgeStyle(bed.readinessStatus)">{{ t('spatial.readiness.status.' + bed.readinessStatus, bed.readinessStatus) }}</span>
                      <span class="rounded-sm px-1.5 py-0.5 text-[9px] font-black uppercase" [ngStyle]="getUsageBadgeStyle(bed.usageStatus)">{{ t('spatial.usage.status.' + bed.usageStatus, bed.usageStatus) }}</span>
                    </div>

                    @if (bed.usageStatus !== 'OCCUPIED' && canOperateBed()) {
                      <div class="mt-3 flex flex-wrap justify-end gap-1 border-t border-[var(--app-border)]/40 pt-2">
                        @if (canManageCapacity()) {
                          <button type="button" class="ui-button ui-button-secondary px-2 py-1 text-[10px]" (click)="changeBedCapacityStatus(bed.id, bed.capacityStatus === 'OPEN' ? 'CLOSED' : 'OPEN')">
                            <app-ui-icon [name]="bed.capacityStatus === 'OPEN' ? 'x-mark' : 'check'" />
                            {{ t(bed.capacityStatus === 'OPEN' ? 'spatial.action.closeCapacity' : 'spatial.action.openCapacity', bed.capacityStatus === 'OPEN' ? 'Fermer' : 'Ouvrir') }}
                          </button>
                        }
                        @if (bed.capacityStatus === 'OPEN' && bed.readinessStatus === 'READY') {
                          @if (canManageCleaning()) { <button type="button" class="ui-button ui-button-secondary px-2 py-1 text-[10px]" (click)="changeBedCleaningStatus(bed.id, 'CLEANING')"><app-ui-icon name="arrow-path" /> {{ t('spatial.action.cleaning', 'Nettoyage') }}</button> }
                          @if (canManageMaintenance()) { <button type="button" class="ui-button ui-button-secondary px-2 py-1 text-[10px]" (click)="changeBedMaintenanceStatus(bed.id, 'MAINTENANCE')"><app-ui-icon name="wrench" /> {{ t('spatial.action.maintenance', 'Maintenance') }}</button> }
                        } @else if (bed.capacityStatus === 'OPEN' && bed.readinessStatus === 'CLEANING' && canManageCleaning()) {
                          <button type="button" class="ui-button ui-button-secondary px-2 py-1 text-[10px]" (click)="changeBedCleaningStatus(bed.id, 'READY')"><app-ui-icon name="check" /> {{ t('spatial.action.cleaningComplete', 'Nettoyage terminé') }}</button>
                        } @else if (bed.capacityStatus === 'OPEN' && bed.readinessStatus === 'MAINTENANCE' && canManageMaintenance()) {
                          <button type="button" class="ui-button ui-button-secondary px-2 py-1 text-[10px]" (click)="changeBedMaintenanceStatus(bed.id, 'READY')"><app-ui-icon name="check" /> {{ t('spatial.action.maintenanceComplete', 'Maintenance terminée') }}</button>
                        }
                      </div>
                    }
                  </article>
                }
              </div>
            </section>
          </div>
        }
      </main>
    </app-shell>
  `,
})
export class SpatialManagementPageComponent implements OnInit {
  private readonly spatialApi = inject(SpatialApiService);
  private readonly i18n = inject(I18nService);
  private readonly rbacApi = inject(RbacApiService);

  readonly t = (key: string, fallback?: string) => this.i18n.t(key, fallback);
  readonly spaces = signal<FacilitySpace[]>([]);
  readonly selectedSpaceId = signal('');
  readonly occupancy = signal<SpaceOccupancyView | null>(null);
  readonly loading = signal(false);

  readonly occupancyRate = computed(() => {
    const current = this.occupancy();
    if (!current || current.openBeds === 0) return 0;
    return Math.round((current.occupiedBeds / current.openBeds) * 100);
  });

  ngOnInit(): void {
    this.loadSpaces();
  }

  loadSpaces(): void {
    this.loading.set(true);
    this.spatialApi.listSpaces(undefined, undefined, false).subscribe({
      next: (data) => {
        const inpatientSpaces = data.filter((space) => space.inpatientProfile && space.active);
        this.spaces.set(inpatientSpaces);
        if (inpatientSpaces.length > 0) {
          this.selectedSpaceId.set(inpatientSpaces[0].id);
          this.loadOccupancy(inpatientSpaces[0].id);
        } else {
          this.occupancy.set(null);
          this.loading.set(false);
        }
      },
      error: () => this.loading.set(false),
    });
  }

  loadOccupancy(spaceId: string): void {
    this.loading.set(true);
    this.spatialApi.getSpaceOccupancy(spaceId).subscribe({
      next: (data) => {
        this.occupancy.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.occupancy.set(null);
        this.loading.set(false);
      },
    });
  }

  onSpaceChange(): void {
    const spaceId = this.selectedSpaceId();
    if (spaceId) this.loadOccupancy(spaceId);
    else this.occupancy.set(null);
  }

  changeBedCleaningStatus(bedId: string, newStatus: 'CLEANING' | 'READY'): void {
    this.spatialApi.updateBedCleaningStatus(bedId, newStatus).subscribe({ next: () => this.reloadSelectedSpace() });
  }

  changeBedMaintenanceStatus(bedId: string, newStatus: 'MAINTENANCE' | 'READY'): void {
    this.spatialApi.updateBedMaintenanceStatus(bedId, newStatus).subscribe({ next: () => this.reloadSelectedSpace() });
  }

  changeBedCapacityStatus(bedId: string, newStatus: 'OPEN' | 'CLOSED'): void {
    this.spatialApi.updateBedCapacityStatus(bedId, newStatus).subscribe({ next: () => this.reloadSelectedSpace() });
  }

  canManageCapacity(): boolean { return this.rbacApi.hasPermission('BED_OPERATIONAL_STATUS_MANAGE'); }
  canManageCleaning(): boolean { return this.rbacApi.hasPermission('BED_CLEANING_MANAGE'); }
  canManageMaintenance(): boolean { return this.rbacApi.hasPermission('BED_MAINTENANCE_MANAGE'); }
  canOperateBed(): boolean { return this.canManageCapacity() || this.canManageCleaning() || this.canManageMaintenance(); }
  canConfigure(): boolean { return this.rbacApi.hasPermission('SPATIAL_CONFIGURATION_MANAGE'); }

  getBedStyle(bed: BedConfiguration): Record<string, string> {
    if (bed.capacityStatus === 'CLOSED') return { background: 'var(--app-surface-muted)', 'border-color': 'var(--app-border)', opacity: '0.78' };
    if (bed.usageStatus === 'OCCUPIED') return { background: 'var(--brand-danger-subtle)', 'border-color': 'var(--brand-danger-border)' };
    if (bed.readinessStatus === 'CLEANING') return { background: 'var(--brand-warning-subtle)', 'border-color': 'var(--brand-warning-border)' };
    if (bed.readinessStatus === 'MAINTENANCE') return { background: 'var(--app-surface-muted)', 'border-color': 'var(--app-border)' };
    return { background: 'var(--brand-success-subtle)', 'border-color': 'color-mix(in srgb, var(--brand-success-muted) 80%, transparent)' };
  }

  getCapacityBadgeStyle(status: 'OPEN' | 'CLOSED'): Record<string, string> {
    return status === 'OPEN'
      ? { background: 'var(--brand-success-subtle)', color: 'var(--brand-success-text)', 'border-color': 'color-mix(in srgb, var(--brand-success-muted) 80%, transparent)' }
      : { background: 'var(--app-surface-muted)', color: 'var(--text-secondary)', 'border-color': 'var(--app-border)' };
  }

  getReadinessBadgeStyle(status: BedConfiguration['readinessStatus']): Record<string, string> {
    if (status === 'READY') return { background: 'var(--brand-success-muted)', color: 'var(--brand-success-text)' };
    if (status === 'CLEANING') return { background: 'var(--brand-warning-muted)', color: 'var(--brand-warning-text)' };
    return { background: 'var(--app-border)', color: 'var(--text-secondary)' };
  }

  getUsageBadgeStyle(status: BedConfiguration['usageStatus']): Record<string, string> {
    return status === 'OCCUPIED'
      ? { background: 'var(--brand-danger-muted)', color: 'var(--brand-danger-text)' }
      : { background: 'var(--app-surface-muted)', color: 'var(--text-secondary)' };
  }

  private reloadSelectedSpace(): void {
    const spaceId = this.selectedSpaceId();
    if (spaceId) this.loadOccupancy(spaceId);
  }
}
