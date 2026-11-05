import { Component, computed, inject, input, output } from '@angular/core';
import { I18nService } from '../../../core/i18n/i18n.service';
import { interpolate } from '../../../core/i18n/interpolate';
import { AppDatePipe } from '../../../shared/formatting/app-date.pipe';
import { IconComponent } from '../../../shared/ui/icon.component';
import { BED_STATE_STYLES, bedDisplayState } from '../bed-display-state';
import { BedConfiguration, FacilitySpace, UnitSpaceAssignment } from '../spatial-configuration.models';

export interface RoomService {
  readonly assignment: UnitSpaceAssignment;
  readonly label: string;
}

/** Une salle : son type, les services qui l'utilisent et, pour une chambre, ses lits. */
@Component({
  selector: 'app-room-card',
  standalone: true,
  imports: [IconComponent],
  template: `
    <article class="min-w-0 rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface)] p-3 shadow-[var(--shadow-panel-subtle)]" [class.opacity-70]="!space().active">
      <div class="flex min-w-0 flex-wrap items-start justify-between gap-2">
        <div class="min-w-0">
          <h4 class="break-words text-base font-semibold text-[var(--text-primary)]">{{ space().name }}</h4>
          <p class="text-sm text-[var(--text-secondary)]">
            {{ typeLabel() }}
            @if (!space().active) { · {{ t('spatial.config.inactive') }} }
          </p>
        </div>
        @if (canManage()) {
          <button type="button" class="ui-button ui-button-ghost" (click)="space().active ? edit.emit() : reactivate.emit()">
            {{ t(space().active ? 'common.edit' : 'spatial.config.reactivate') }}
          </button>
        }
      </div>

      @if (space().active) {
        <div class="mt-3 flex min-w-0 flex-wrap items-center gap-2">
          <span class="text-sm text-[var(--text-secondary)]">{{ t('spatial.config.usedBy') }}</span>
          @for (service of services(); track service.assignment.id) {
            <button
              type="button"
              class="inline-flex min-h-11 min-w-0 flex-col items-start justify-center rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] px-3 text-left focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--brand-primary)] disabled:cursor-default"
              [disabled]="!canManage()"
              (click)="editAssignment.emit(service.assignment)"
            >
              <span class="break-words text-sm font-semibold text-[var(--text-primary)]">{{ service.label }}</span>
              <span class="text-xs text-[var(--text-secondary)]">{{ period(service.assignment) }}</span>
            </button>
          } @empty {
            <span class="text-sm text-[var(--text-muted)]">{{ t('spatial.config.noService') }}</span>
          }
          @if (canManage()) {
            <button type="button" class="ui-button ui-button-ghost" (click)="assign.emit()">
              <app-ui-icon name="plus" aria-hidden="true" /> {{ t('spatial.config.assign') }}
            </button>
          }
        </div>

        @if (space().inpatientProfile) {
          <div class="mt-3 flex min-w-0 flex-wrap items-center gap-2">
            <span class="text-sm text-[var(--text-secondary)]">{{ t('spatial.config.beds') }}</span>
            @for (bed of beds(); track bed.id) {
              <button
                type="button"
                class="inline-flex min-h-11 items-center gap-2 rounded-[var(--radius-brand-sm)] border px-3 text-sm font-semibold focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--brand-primary)] disabled:cursor-default"
                [class]="stateStyle(bed).pill"
                [disabled]="!canManage()"
                [attr.aria-label]="bedLabel(bed)"
                (click)="editBed.emit(bed)"
              >
                <app-ui-icon [name]="stateStyle(bed).icon" aria-hidden="true" />
                {{ bed.bedNumber }}
              </button>
            } @empty {
              <span class="text-sm text-[var(--text-muted)]">{{ t('spatial.config.noBed') }}</span>
            }
            @if (canManage()) {
              <button type="button" class="ui-button ui-button-ghost" (click)="addBed.emit()">
                <app-ui-icon name="plus" aria-hidden="true" /> {{ t('spatial.config.addBed') }}
              </button>
            }
          </div>
        }
      }
    </article>
  `,
})
export class RoomCardComponent {
  private readonly i18n = inject(I18nService);
  private readonly datePipe = new AppDatePipe();

  readonly space = input.required<FacilitySpace>();
  readonly typeLabel = input.required<string>();
  readonly services = input<readonly RoomService[]>([]);
  readonly beds = input<readonly BedConfiguration[]>([]);
  readonly canManage = input(true);
  readonly edit = output<void>();
  readonly reactivate = output<void>();
  readonly assign = output<void>();
  readonly editAssignment = output<UnitSpaceAssignment>();
  readonly addBed = output<void>();
  readonly editBed = output<BedConfiguration>();

  readonly t = (key: string) => this.i18n.t(key);

  stateStyle(bed: BedConfiguration) {
    return BED_STATE_STYLES[bedDisplayState(bed)];
  }

  bedLabel(bed: BedConfiguration): string {
    return interpolate(this.t('spatial.config.editBedLabel'), {
      bed: bed.bedNumber,
      state: this.t(`spatial.bedState.${bedDisplayState(bed)}`),
    });
  }

  period(assignment: UnitSpaceAssignment): string {
    const from = this.datePipe.transform(assignment.validFrom);
    if (!assignment.validTo) return interpolate(this.t('spatial.config.since'), { from });
    return interpolate(this.t('spatial.config.between'), { from, to: this.datePipe.transform(assignment.validTo) });
  }
}
