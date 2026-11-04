import { Component, inject, input, output } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';
import { IconComponent } from './icon.component';

/**
 * État d'erreur de chargement distinct d'un état vide : une donnée non chargée ne doit jamais
 * être présentée comme « aucune donnée » (ex. allergies non vérifiées ≠ aucune allergie).
 */
@Component({
  selector: 'app-ui-load-error',
  imports: [IconComponent],
  template: `
    <div
      role="alert"
      class="flex flex-col gap-3 rounded-[var(--radius-brand-md)] border border-[var(--brand-danger-border)] bg-[var(--brand-danger-subtle)] p-3 text-[var(--brand-danger-text)] sm:flex-row sm:items-center sm:justify-between sm:p-4"
      [attr.data-testid]="testId()"
    >
      <p class="flex min-w-0 items-start gap-2 text-sm font-bold leading-5">
        <app-ui-icon name="exclamation-triangle" class="mt-0.5 shrink-0" aria-hidden="true" />
        <span>{{ message() }}</span>
      </p>
      <button
        type="button"
        class="ui-button ui-button-secondary min-h-11 w-full shrink-0 sm:w-auto"
        [disabled]="retrying()"
        (click)="retry.emit()"
      >
        <app-ui-icon name="arrow-path" aria-hidden="true" />
        {{ i18n.t('common.retry') }}
      </button>
    </div>
  `,
})
export class LoadErrorComponent {
  readonly i18n = inject(I18nService);

  readonly message = input.required<string>();
  readonly retrying = input(false);
  readonly testId = input<string | null>(null);
  readonly retry = output<void>();
}
