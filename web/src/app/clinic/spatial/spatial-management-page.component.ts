import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { I18nService } from '../../core/i18n/i18n.service';
import { Ward } from '../../patient/patient.models';
import { SpatialApiService } from '../../patient/spatial-api.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { IconComponent } from '../../shared/ui/icon.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { RbacApiService } from '../rbac/rbac-api.service';
import {
  BedCapacityStatus,
  BedCapacityView,
  WardCapacityView,
} from './bed-capacity.models';

@Component({
  selector: 'app-spatial-management-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, IconComponent, AppShellComponent, PageHeaderComponent],
  template: `
    <app-shell>
      <app-page-header
        [title]="t('spatial.title')"
        [subtitle]="t('spatial.subtitle')"
      >
        @if (canConfigure()) {
          <a routerLink="/clinic/spatial/configuration" class="ui-button ui-button-secondary">
            <app-ui-icon name="building" />
            {{ t('spatial.config.open') }}
          </a>
        }
        <div class="flex items-center gap-2">
          <label class="text-xs font-bold text-[var(--text-secondary)] uppercase tracking-wider whitespace-nowrap">
            {{ t('spatial.selectWard') }}&nbsp;:
          </label>
          <select
            [(ngModel)]="selectedWardId"
            (change)="onWardChange()"
            class="ui-select text-xs min-w-[200px]"
          >
            <option value="">-- {{ t('spatial.selectWardPlaceholder') }} --</option>
            @for (ward of wards(); track ward.id) {
              <option [value]="ward.id">{{ ward.name }}</option>
            }
          </select>
        </div>
      </app-page-header>

      <div class="app-container pb-12">
        @if (loading()) {
          <div class="flex justify-center items-center py-12">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-cyan"></div>
          </div>
        } @else if (!selectedWardId()) {
          <div class="ui-card-muted p-12 text-center">
            <app-ui-icon name="bed" class="text-brand-cyan text-4xl mb-4" />
            <h3 class="text-sm font-bold text-[var(--text-primary)] mb-1">
              {{ t('spatial.noWardSelected') }}
            </h3>
            <p class="text-xs text-[var(--text-muted)]">
              {{ t('spatial.noWardSelectedDetail') }}
            </p>
          </div>
        } @else if (occupancy()) {
          <div class="space-y-6">
            <div class="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-6 gap-4">
              <div class="ui-card-muted p-4">
                <div class="text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider mb-1">
                  {{ t('spatial.capacity.installed') }}
                </div>
                <div class="text-lg font-black text-[var(--text-primary)]">
                  {{ occupancy()?.totalBedsCount }}
                </div>
              </div>
              <div class="ui-card-muted p-4">
                <div class="text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider mb-1">
                  {{ t('spatial.capacity.open') }}
                </div>
                <div class="text-lg font-black text-[var(--text-primary)]">
                  {{ occupancy()?.openBedsCount }}
                </div>
              </div>
              <div class="ui-card-muted p-4">
                <div class="text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider mb-1">
                  {{ t('spatial.capacity.ready') }}
                </div>
                <div class="text-lg font-black" [style.color]="'var(--brand-info)'">
                  {{ occupancy()?.readyBedsCount }}
                </div>
              </div>
              <div class="ui-card-muted p-4">
                <div class="text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider mb-1">
                  {{ t('spatial.occupiedBeds') }}
                </div>
                <div class="text-lg font-black" [style.color]="(occupancy()?.occupiedBedsCount ?? 0) > 0 ? 'var(--brand-danger)' : 'var(--text-primary)'">
                  {{ occupancy()?.occupiedBedsCount }}
                </div>
              </div>
              <div class="ui-card-muted p-4">
                <div class="text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider mb-1">
                  {{ t('spatial.capacity.available') }}
                </div>
                <div class="text-lg font-black" [style.color]="'var(--brand-success)'">
                  {{ availableBedsCount() }}
                </div>
              </div>
              <div class="ui-card-muted p-4">
                <div class="text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider mb-1">
                  {{ t('spatial.occupancyRate') }}
                </div>
                <div class="text-lg font-black" [style.color]="'var(--brand-info)'">
                  {{ occupancyRate() }} %
                </div>
              </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              @for (room of occupancy()?.rooms; track room.id) {
                <div class="ui-card p-5 flex flex-col justify-between">
                  <div>
                    <div class="flex justify-between items-start pb-3 border-b border-[var(--app-border)]/60 mb-4">
                      <div>
                        <h3 class="text-sm font-bold text-[var(--text-primary)]">
                          {{ t('spatial.room') }} {{ room.roomNumber }}
                        </h3>
                        <span class="inline-flex items-center px-1.5 py-0.5 rounded-sm text-[9px] font-bold mt-1 bg-[var(--app-surface-muted)] text-[var(--text-secondary)] border border-[var(--app-border)]/60">
                          {{ room.comfortLevel }}
                        </span>
                      </div>
                      <div class="text-[11px] text-[var(--text-muted)] font-medium flex items-center gap-1">
                        <app-ui-icon name="users" class="text-[var(--text-muted)]" />
                        {{ room.beds.length }} / {{ room.capacity }}
                      </div>
                    </div>

                    <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      @for (bed of room.beds; track bed.id) {
                        <div
                          class="p-3 border rounded-md transition-all flex flex-col gap-2"
                          [ngStyle]="getBedStyle(bed)"
                        >
                          <div class="flex justify-between items-start gap-2">
                            <span class="text-xs font-bold text-[var(--text-primary)] flex items-center gap-1">
                              <app-ui-icon name="bed" class="text-[var(--text-muted)]" />
                              {{ bed.bedNumber }}
                            </span>
                            <span
                              class="text-[9px] font-black uppercase tracking-wider px-1.5 py-0.5 rounded-sm border"
                              [ngStyle]="getCapacityBadgeStyle(bed.capacityStatus)"
                            >
                              {{ t('spatial.capacity.status.' + bed.capacityStatus) }}
                            </span>
                          </div>

                          <div class="flex flex-wrap gap-1">
                            <span
                              class="text-[9px] font-black uppercase tracking-wider px-1.5 py-0.5 rounded-sm"
                              [ngStyle]="getReadinessBadgeStyle(bed.readinessStatus)"
                            >
                              {{ t('spatial.readiness.status.' + bed.readinessStatus) }}
                            </span>
                            <span
                              class="text-[9px] font-black uppercase tracking-wider px-1.5 py-0.5 rounded-sm"
                              [ngStyle]="getUsageBadgeStyle(bed.usageStatus)"
                            >
                              {{ t('spatial.usage.status.' + bed.usageStatus) }}
                            </span>
                          </div>

                          @if (canModify() && bed.usageStatus !== 'OCCUPIED') {
                            <div class="flex flex-wrap gap-1 mt-1 border-t border-[var(--app-border)]/40 pt-2 justify-end">
                              <button
                                (click)="changeBedCapacityStatus(bed.id, bed.capacityStatus === 'OPEN' ? 'CLOSED' : 'OPEN')"
                                class="inline-flex items-center gap-1 px-1.5 py-0.5 text-[9px] font-black rounded-sm border cursor-pointer"
                                [style.background]="'var(--app-surface-muted)'"
                                [style.color]="'var(--text-secondary)'"
                                [style.border-color]="'var(--app-border)'"
                              >
                                <app-ui-icon [name]="bed.capacityStatus === 'OPEN' ? 'x-mark' : 'check'" />
                                {{ t(bed.capacityStatus === 'OPEN' ? 'spatial.action.closeCapacity' : 'spatial.action.openCapacity') }}
                              </button>

                              @if (bed.capacityStatus === 'OPEN' && bed.readinessStatus !== 'READY') {
                                <button
                                  (click)="changeBedStatus(bed.id, 'FREE')"
                                  class="inline-flex items-center gap-1 px-1.5 py-0.5 text-[9px] font-black rounded-sm border cursor-pointer"
                                  [style.background]="'var(--brand-success-subtle)'"
                                  [style.color]="'var(--brand-success-text)'"
                                  [style.border-color]="'color-mix(in srgb, var(--brand-success-muted) 80%, transparent)'"
                                >
                                  <app-ui-icon name="check" />
                                  {{ t('spatial.action.makeFree') }}
                                </button>
                              } @else if (bed.capacityStatus === 'OPEN' && bed.readinessStatus === 'READY') {
                                <button
                                  (click)="changeBedStatus(bed.id, 'MAINTENANCE')"
                                  class="inline-flex items-center gap-1 px-1.5 py-0.5 text-[9px] font-black rounded-sm border cursor-pointer"
                                  [style.background]="'var(--app-surface-muted)'"
                                  [style.color]="'var(--text-secondary)'"
                                  [style.border-color]="'var(--app-border)'"
                                >
                                  <app-ui-icon name="wrench" />
                                  {{ t('spatial.action.maintenance') }}
                                </button>
                              }
                            </div>
                          }
                        </div>
                      }
                    </div>
                  </div>
                </div>
              }
            </div>
          </div>
        }
      </div>
    </app-shell>
  `,
})
export class SpatialManagementPageComponent implements OnInit {
  private readonly spatialApi = inject(SpatialApiService);
  private readonly i18n = inject(I18nService);
  private readonly rbacApi = inject(RbacApiService);

  readonly t = (key: string) => this.i18n.t(key);

  readonly wards = signal<Ward[]>([]);
  readonly selectedWardId = signal<string>('');
  readonly occupancy = signal<WardCapacityView | null>(null);
  readonly loading = signal(false);

  readonly availableBedsCount = computed(() => this.occupancy()?.availableBedsCount ?? 0);

  readonly occupancyRate = computed(() => {
    const current = this.occupancy();
    if (!current || current.openBedsCount === 0) {
      return 0;
    }
    return Math.round((current.occupiedBedsCount / current.openBedsCount) * 100);
  });

  ngOnInit(): void {
    this.loadWards();
  }

  loadWards(): void {
    this.loading.set(true);
    this.spatialApi.listWards().subscribe({
      next: (data) => {
        this.wards.set(data);
        if (data.length > 0) {
          this.selectedWardId.set(data[0].id);
          this.loadOccupancy(data[0].id);
        } else {
          this.loading.set(false);
        }
      },
      error: () => this.loading.set(false),
    });
  }

  loadOccupancy(wardId: string): void {
    this.loading.set(true);
    this.spatialApi.getWardOccupancy(wardId).subscribe({
      next: (data) => {
        this.occupancy.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onWardChange(): void {
    const wardId = this.selectedWardId();
    if (wardId) {
      this.loadOccupancy(wardId);
    } else {
      this.occupancy.set(null);
    }
  }

  changeBedStatus(
    bedId: string,
    newStatus: 'FREE' | 'OCCUPIED' | 'CLEANING' | 'MAINTENANCE',
  ): void {
    this.spatialApi.updateBedStatus(bedId, newStatus).subscribe({
      next: () => this.reloadSelectedWard(),
    });
  }

  changeBedCapacityStatus(bedId: string, newStatus: BedCapacityStatus): void {
    this.spatialApi.updateBedCapacityStatus(bedId, newStatus).subscribe({
      next: () => this.reloadSelectedWard(),
    });
  }

  canModify(): boolean {
    return this.rbacApi.hasPermission('BED_OPERATIONAL_STATUS_MANAGE');
  }

  canConfigure(): boolean {
    return this.rbacApi.hasPermission('SPATIAL_CONFIGURATION_MANAGE');
  }

  getBedStyle(bed: BedCapacityView): Record<string, string> {
    if (bed.capacityStatus === 'CLOSED') {
      return {
        background: 'var(--app-surface-muted)',
        'border-color': 'var(--app-border)',
        opacity: '0.78',
      };
    }
    if (bed.usageStatus === 'OCCUPIED') {
      return {
        background: 'var(--brand-danger-subtle)',
        'border-color': 'var(--brand-danger-border)',
      };
    }
    if (bed.readinessStatus === 'CLEANING') {
      return {
        background: 'var(--brand-warning-subtle)',
        'border-color': 'var(--brand-warning-border)',
      };
    }
    if (bed.readinessStatus === 'MAINTENANCE') {
      return {
        background: 'var(--app-surface-muted)',
        'border-color': 'var(--app-border)',
      };
    }
    return {
      background: 'var(--brand-success-subtle)',
      'border-color': 'color-mix(in srgb, var(--brand-success-muted) 80%, transparent)',
    };
  }

  getCapacityBadgeStyle(status: BedCapacityStatus): Record<string, string> {
    return status === 'OPEN'
      ? {
          background: 'var(--brand-success-subtle)',
          color: 'var(--brand-success-text)',
          'border-color': 'color-mix(in srgb, var(--brand-success-muted) 80%, transparent)',
        }
      : {
          background: 'var(--app-surface-muted)',
          color: 'var(--text-secondary)',
          'border-color': 'var(--app-border)',
        };
  }

  getReadinessBadgeStyle(status: BedCapacityView['readinessStatus']): Record<string, string> {
    if (status === 'READY') {
      return { background: 'var(--brand-success-muted)', color: 'var(--brand-success-text)' };
    }
    if (status === 'CLEANING') {
      return { background: 'var(--brand-warning-muted)', color: 'var(--brand-warning-text)' };
    }
    return { background: 'var(--app-border)', color: 'var(--text-secondary)' };
  }

  getUsageBadgeStyle(status: BedCapacityView['usageStatus']): Record<string, string> {
    return status === 'OCCUPIED'
      ? { background: 'var(--brand-danger-muted)', color: 'var(--brand-danger-text)' }
      : { background: 'var(--app-surface-muted)', color: 'var(--text-secondary)' };
  }

  private reloadSelectedWard(): void {
    const wardId = this.selectedWardId();
    if (wardId) {
      this.loadOccupancy(wardId);
    }
  }
}
