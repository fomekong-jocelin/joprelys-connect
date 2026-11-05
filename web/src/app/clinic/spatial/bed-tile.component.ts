import { Component, computed, inject, input, output } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';
import { interpolate } from '../../core/i18n/interpolate';
import { IconComponent } from '../../shared/ui/icon.component';
import { MenuButtonComponent, MenuButtonItem } from '../../shared/ui/menu-button.component';
import { BED_STATE_STYLES, BedActionId, BedPermissions, bedActions, bedDisplayState } from './bed-display-state';
import { BedConfiguration } from './spatial-configuration.models';

/** Un lit : son numéro, un seul état lisible et les actions que l'utilisateur a le droit de faire. */
@Component({
  selector: 'app-bed-tile',
  standalone: true,
  imports: [IconComponent, MenuButtonComponent],
  template: `
    <article
      class="flex h-full min-w-0 flex-wrap items-center gap-x-3 gap-y-2 rounded-[var(--radius-brand-md)] border p-3"
      [class]="style().tile"
      [attr.aria-label]="ariaLabel()"
    >
      <div class="min-w-0 flex-1 basis-24">
        <p class="break-words text-base font-bold text-[var(--text-primary)]">{{ bed().bedNumber }}</p>
        <span class="mt-1 inline-flex items-center gap-1 rounded-[var(--radius-brand-sm)] border px-2 py-0.5 text-xs font-semibold" [class]="style().pill">
          <app-ui-icon [name]="style().icon" aria-hidden="true" />
          {{ stateLabel() }}
        </span>
      </div>
      @if (actions().length > 0) {
        <div class="flex min-w-0 flex-wrap gap-1">
          @if (useMenu()) {
            <app-menu-button
              [label]="t('spatial.tracking.changeState')"
              [items]="menuItems()"
              [icon]="null"
              buttonClass="ui-button-ghost"
              [disabled]="busy()"
              (selected)="emit($event)"
            />
          } @else {
            @for (action of actions(); track action) {
              <button type="button" class="ui-button ui-button-ghost" [disabled]="busy()" (click)="act.emit(action)">
                {{ t('spatial.tracking.action.' + action) }}
              </button>
            }
          }
        </div>
      }
    </article>
  `,
})
export class BedTileComponent {
  private readonly i18n = inject(I18nService);

  readonly bed = input.required<BedConfiguration>();
  readonly permissions = input.required<BedPermissions>();
  readonly busy = input(false);
  readonly act = output<BedActionId>();

  readonly t = (key: string) => this.i18n.t(key);
  readonly state = computed(() => bedDisplayState(this.bed()));
  readonly style = computed(() => BED_STATE_STYLES[this.state()]);
  readonly stateLabel = computed(() => this.t(`spatial.bedState.${this.state()}`));
  readonly actions = computed(() => bedActions(this.bed(), this.permissions()));
  /** Un lit libre peut partir dans plusieurs états : on regroupe les choix dans un menu. */
  readonly useMenu = computed(() => this.state() === 'FREE' && this.actions().length > 1);
  readonly menuItems = computed<MenuButtonItem[]>(() => this.actions().map((action) => ({
    id: action,
    label: this.t(`spatial.tracking.action.${action}`),
  })));
  readonly ariaLabel = computed(() => interpolate(this.t('spatial.tracking.bedLabel'), {
    bed: this.bed().bedNumber,
    state: this.stateLabel(),
  }));

  emit(id: string): void {
    const action = this.actions().find((candidate) => candidate === id);
    if (action) this.act.emit(action);
  }
}
