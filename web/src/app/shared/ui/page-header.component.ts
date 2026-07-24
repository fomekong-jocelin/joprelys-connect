import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="app-container py-5 sm:py-7">
      <div class="flex min-w-0 flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
        <div class="min-w-0 max-w-3xl space-y-1">
          @if (backLink()) {
            <a [routerLink]="backLink()" class="ui-link text-xs">{{ backLabel() }}</a>
          }
          <h1 class="ui-title break-words text-2xl">{{ title() }}</h1>
          @if (subtitle()) {
            <p class="max-w-3xl break-words text-sm font-medium leading-5" style="color: var(--text-secondary)">{{ subtitle() }}</p>
          }
        </div>
        <div class="flex w-full min-w-0 flex-col items-stretch gap-2 md:w-auto md:flex-row md:flex-wrap md:items-center md:justify-end">
          <ng-content></ng-content>
        </div>
      </div>
    </section>
  `,
})
export class PageHeaderComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string | null>(null);
  readonly backLink = input<string | null>(null);
  readonly backLabel = input('Retour');
}
