import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { I18nService } from '../../../core/i18n/i18n.service';
import { IconComponent } from '../../../shared/ui/icon.component';
import { linkedAppIdentity } from './linked-app-catalog';
import { LinkedApp } from './linked-apps.models';

/** Une application reliée : identité, données partagées, période, et l'action qui révoque. */
@Component({
  selector: 'app-linked-app-card',
  imports: [DatePipe, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article class="ui-card flex flex-col gap-4 p-4 md:p-5" [attr.aria-labelledby]="headingId()">
      <header class="flex items-center gap-3">
        <div
          class="flex h-12 w-12 shrink-0 items-center justify-center border bg-white p-1.5"
          style="border-radius: var(--radius-brand-sm); border-color: var(--app-border)"
          aria-hidden="true"
        >
          @if (identity().logoUrl; as logo) {
            <img [src]="logo" alt="" width="36" height="36" class="h-9 w-9 object-contain" />
          } @else {
            <app-ui-icon name="shield-check" class="h-6 w-6" style="color: var(--text-muted)" />
          }
        </div>
        <div class="min-w-0">
          <h2 [id]="headingId()" class="truncate text-base font-bold" style="color: var(--text-primary)">
            {{ displayName() }}
          </h2>
          <p class="text-sm" style="color: var(--text-secondary)">
            {{ i18n.t('patient.linkedApps.linkedOn') }}
            <span class="font-semibold tabular-nums">{{ app().linkedAt | date: 'dd/MM/yyyy' }}</span>
          </p>
        </div>
      </header>

      <div>
        <p class="mb-2 text-xs font-semibold" style="color: var(--text-muted)">
          {{ i18n.t('patient.linkedApps.sharedData') }}
        </p>
        @if (app().scopes.length > 0) {
          <ul class="flex flex-wrap gap-2" role="list">
            @for (scope of app().scopes; track scope) {
              <li
                class="px-2.5 py-1 text-xs font-semibold"
                style="border-radius: var(--radius-brand-sm); background: var(--brand-primary-subtle); color: var(--brand-primary)"
              >
                {{ i18n.t('patient.linkedApps.scope.' + scope) }}
              </li>
            }
          </ul>
        } @else {
          <p class="text-sm" style="color: var(--text-secondary)">{{ i18n.t('patient.linkedApps.noScopes') }}</p>
        }
      </div>

      <footer
        class="flex flex-col gap-3 border-t pt-4 sm:flex-row sm:items-center sm:justify-between"
        style="border-color: var(--app-border)"
      >
        @if (app().validUntil; as validUntil) {
          <p class="text-sm" style="color: var(--text-secondary)">
            {{ i18n.t('patient.linkedApps.validUntil') }}
            <span class="font-semibold tabular-nums" style="color: var(--text-primary)">{{ validUntil | date: 'dd/MM/yyyy' }}</span>
          </p>
        } @else {
          <span></span>
        }
        <button
          type="button"
          class="ui-button ui-button-danger min-h-11 w-full sm:w-auto"
          [disabled]="revoking()"
          [attr.aria-label]="revokeLabel()"
          [attr.data-testid]="'linked-app-revoke-' + app().id"
          (click)="revoke.emit(app())"
        >
          {{ revoking() ? i18n.t('patient.linkedApps.revoking') : i18n.t('patient.linkedApps.revoke') }}
        </button>
      </footer>
    </article>
  `,
})
export class LinkedAppCardComponent {
  readonly i18n = inject(I18nService);

  readonly app = input.required<LinkedApp>();
  readonly revoking = input(false);
  readonly revoke = output<LinkedApp>();

  readonly identity = computed(() => linkedAppIdentity(this.app().application));
  readonly displayName = computed(() => this.identity().name ?? this.i18n.t('patient.linkedApps.unknownApp'));
  readonly headingId = computed(() => `linked-app-${this.app().id}`);
  readonly revokeLabel = computed(() => `${this.i18n.t('patient.linkedApps.revoke')} : ${this.displayName()}`);
}
