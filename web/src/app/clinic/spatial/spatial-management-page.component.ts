import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SpatialApiService } from '../../patient/spatial-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { AuthTokenStorageService } from '../../auth/auth-token-storage.service';
import { Ward, WardOccupancy, Bed } from '../../patient/patient.models';
import { IconComponent } from '../../shared/ui/icon.component';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';

@Component({
  selector: 'app-spatial-management-page',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent, AppShellComponent, PageHeaderComponent],
  template: `
    <app-shell>
      <app-page-header
        [title]="t('spatial.title')"
        [subtitle]="t('spatial.subtitle')"
      >
        <!-- Sélecteur de Service -->
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
            @for (w of wards(); track w.id) {
              <option [value]="w.id">{{ w.name }}</option>
            }
          </select>
        </div>
      </app-page-header>

      <div class="app-container pb-12">
        <!-- États de chargement / vide -->
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
          <!-- Vue du service sélectionné -->
          <div class="space-y-6">
            <!-- Compteurs du service -->
            <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div class="ui-card-muted p-4">
                <div class="text-[10px] font-bold text-[var(--text-muted)] uppercase tracking-wider mb-1">
                  {{ t('spatial.totalBeds') }}
                </div>
                <div class="text-lg font-black text-[var(--text-primary)]">
                  {{ occupancy()?.totalBedsCount }}
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
                  {{ t('spatial.freeBeds') }}
                </div>
                <div class="text-lg font-black" [style.color]="'var(--brand-success)'">
                  {{ (occupancy()?.totalBedsCount ?? 0) - (occupancy()?.occupiedBedsCount ?? 0) }}
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

            <!-- Grille des chambres -->
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

                    <!-- Grille des lits -->
                    <div class="grid grid-cols-2 gap-3">
                      @for (bed of room.beds; track bed.id) {
                        <div
                          class="p-3 border rounded-md transition-all flex flex-col justify-between gap-2"
                          [ngStyle]="getBedStyle(bed.status)"
                        >
                          <div class="flex justify-between items-start gap-1">
                            <span class="text-xs font-bold text-[var(--text-primary)] flex items-center gap-1">
                              <app-ui-icon name="bed" class="text-[var(--text-muted)]" />
                              {{ bed.bedNumber }}
                            </span>
                          </div>
                          
                          <span class="text-[9px] font-black uppercase tracking-wider self-start px-1.5 py-0.5 rounded-sm" [ngStyle]="getBadgeStyle(bed.status)">
                            {{ t('spatial.status.' + bed.status.toLowerCase()) }}
                          </span>

                          <!-- Actions rapides sur lit pour les soignants -->
                          @if (canModify() && bed.status !== 'OCCUPIED') {
                            <div class="flex gap-1 mt-1 border-t border-[var(--app-border)]/40 pt-2 justify-end">
                              @if (bed.status === 'CLEANING') {
                                <button
                                  (click)="changeBedStatus(bed.id, 'FREE')"
                                  class="inline-flex items-center gap-1 px-1.5 py-0.5 text-[9px] font-black rounded-sm border cursor-pointer"
                                  [style.background]="'var(--brand-success-subtle)'"
                                  [style.color]="'var(--brand-success-text)'"
                                  [style.border-color]="'color-mix(in srgb, var(--brand-success-muted) 80%, transparent)'"
                                  title="Mettre libre"
                                >
                                  <app-ui-icon name="check" />
                                  {{ t('spatial.action.makeFree') }}
                                </button>
                              } @else if (bed.status === 'FREE') {
                                <button
                                  (click)="changeBedStatus(bed.id, 'MAINTENANCE')"
                                  class="inline-flex items-center gap-1 px-1.5 py-0.5 text-[9px] font-black rounded-sm border cursor-pointer"
                                  [style.background]="'var(--app-surface-muted)'"
                                  [style.color]="'var(--text-secondary)'"
                                  [style.border-color]="'var(--app-border)'"
                                  title="Mettre en maintenance"
                                >
                                  <app-ui-icon name="wrench" />
                                  {{ t('spatial.action.maintenance') }}
                                </button>
                              } @else if (bed.status === 'MAINTENANCE') {
                                  <button
                                  (click)="changeBedStatus(bed.id, 'FREE')"
                                  class="inline-flex items-center gap-1 px-1.5 py-0.5 text-[9px] font-black rounded-sm border cursor-pointer"
                                  [style.background]="'var(--brand-success-subtle)'"
                                  [style.color]="'var(--brand-success-text)'"
                                  [style.border-color]="'color-mix(in srgb, var(--brand-success-muted) 80%, transparent)'"
                                  title="Mettre libre"
                                >
                                  <app-ui-icon name="check" />
                                  {{ t('spatial.action.makeFree') }}
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
  `
})
export class SpatialManagementPageComponent implements OnInit {
  private readonly spatialApi = inject(SpatialApiService);
  private readonly i18n = inject(I18nService);
  private readonly tokenStorage = inject(AuthTokenStorageService);

  readonly t = (key: string) => this.i18n.t(key);

  readonly wards = signal<Ward[]>([]);
  readonly selectedWardId = signal<string>('');
  readonly occupancy = signal<WardOccupancy | null>(null);
  readonly loading = signal<boolean>(false);

  readonly session = this.tokenStorage.session;

  readonly occupancyRate = computed(() => {
    const occ = this.occupancy();
    if (!occ || occ.totalBedsCount === 0) return 0;
    return Math.round((occ.occupiedBedsCount / occ.totalBedsCount) * 100);
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
      error: () => this.loading.set(false)
    });
  }

  loadOccupancy(wardId: string): void {
    this.loading.set(true);
    this.spatialApi.getWardOccupancy(wardId).subscribe({
      next: (data) => {
        this.occupancy.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
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

  changeBedStatus(bedId: string, newStatus: 'FREE' | 'OCCUPIED' | 'CLEANING' | 'MAINTENANCE'): void {
    this.spatialApi.updateBedStatus(bedId, newStatus).subscribe({
      next: () => {
        const wardId = this.selectedWardId();
        if (wardId) {
          this.loadOccupancy(wardId);
        }
      }
    });
  }

  canModify(): boolean {
    const role = this.session()?.role;
    if (!role) return false;
    const roles = role.split(',').map((r) => r.trim());
    return roles.some((r) => ['INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE'].includes(r));
  }

  getBedStyle(status: string): Record<string, string> {
    switch (status) {
      case 'FREE':
        return {
          background: 'var(--brand-success-subtle)',
          'border-color': 'color-mix(in srgb, var(--brand-success-muted) 80%, transparent)',
        };
      case 'OCCUPIED':
        return {
          background: 'var(--brand-danger-subtle)',
          'border-color': 'var(--brand-danger-border)',
        };
      case 'CLEANING':
        return {
          background: 'var(--brand-warning-subtle)',
          'border-color': 'var(--brand-warning-border)',
        };
      case 'MAINTENANCE':
        return {
          background: 'var(--app-surface-muted)',
          'border-color': 'var(--app-border)',
        };
      default:
        return {};
    }
  }

  getBadgeStyle(status: string): Record<string, string> {
    switch (status) {
      case 'FREE':
        return {
          background: 'var(--brand-success-muted)',
          color: 'var(--brand-success-text)',
        };
      case 'OCCUPIED':
        return {
          background: 'var(--brand-danger-muted)',
          color: 'var(--brand-danger-text)',
        };
      case 'CLEANING':
        return {
          background: 'var(--brand-warning-muted)',
          color: 'var(--brand-warning-text)',
        };
      case 'MAINTENANCE':
        return {
          background: 'var(--app-border)',
          color: 'var(--text-secondary)',
        };
      default:
        return {};
    }
  }
}
