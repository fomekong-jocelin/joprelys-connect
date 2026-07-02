import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="app-container flex flex-col gap-4 py-8 sm:flex-row sm:items-end sm:justify-between">
      <div class="space-y-1">
        @if (backLink()) {
          <a [routerLink]="backLink()" class="ui-link text-xs">{{ backLabel() }}</a>
        }
        <h1 class="ui-title text-2xl">{{ title() }}</h1>
        @if (subtitle()) {
          <p class="text-sm font-medium" style="color: var(--text-secondary)">{{ subtitle() }}</p>
        }
      </div>
      <div class="flex w-full items-center gap-3 sm:w-auto">
        <ng-content></ng-content>
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
