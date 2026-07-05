import { Component, computed, inject } from '@angular/core';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter, map, startWith } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';
import { I18nService } from '../../core/i18n/i18n.service';

interface BreadcrumbItem {
  label: string;
  url: string;
}

@Component({
  selector: 'app-breadcrumb',
  standalone: true,
  imports: [RouterLink],
  template: `
    @if (breadcrumbs().length > 0) {
      <nav class="flex items-center space-x-2 px-4 md:px-6 py-3 border-b border-[var(--app-border)] bg-[var(--app-surface-muted)] text-xs font-semibold text-slate-500 select-none">
        <a routerLink="/" class="hover:text-slate-900 dark:hover:text-slate-200 transition-colors">
          {{ i18n.t('breadcrumb.home') }}
        </a>
        @for (item of breadcrumbs(); track item.url; let last = $last) {
          <span class="text-slate-400 dark:text-slate-600">/</span>
          @if (last) {
            <span class="text-slate-800 dark:text-slate-300 font-bold truncate max-w-[200px]">{{ item.label }}</span>
          } @else {
            <a [routerLink]="item.url" class="hover:text-slate-900 dark:hover:text-slate-200 transition-colors truncate max-w-[150px]">
              {{ item.label }}
            </a>
          }
        }
      </nav>
    }
  `,
})
export class BreadcrumbComponent {
  private readonly router = inject(Router);
  readonly i18n = inject(I18nService);

  private readonly navigationEnd$ = this.router.events.pipe(
    filter((event): event is NavigationEnd => event instanceof NavigationEnd),
    startWith(null)
  );

  readonly breadcrumbs = toSignal(
    this.navigationEnd$.pipe(
      map(() => this.buildBreadcrumbs())
    ),
    { initialValue: [] as BreadcrumbItem[] }
  );

  private buildBreadcrumbs(): BreadcrumbItem[] {
    const breadcrumbs: BreadcrumbItem[] = [];
    let route = this.router.routerState.snapshot.root;
    let url = '';

    while (route.firstChild) {
      route = route.firstChild;
      const routeConfig = route.routeConfig;
      if (!routeConfig || !routeConfig.path) continue;

      // Handle path with parameters: replace them with actual parameter values in URL
      const pathSegments = routeConfig.path.split('/');
      const resolvedSegments = pathSegments.map(segment => {
        if (segment.startsWith(':')) {
          const paramName = segment.substring(1);
          return route.params[paramName] || segment;
        }
        return segment;
      });

      url += '/' + resolvedSegments.join('/');

      // Retrieve label from route data or fallback to route path or key in i18n
      let label = route.data['breadcrumb'] || '';
      if (!label) {
        // Fallback to translating the path in i18n
        // e.g. path 'organizations' -> i18n key 'breadcrumb.organizations'
        const cleanPath = routeConfig.path.replace(/:[^\/]+/g, '*'); // replace route params with '*'
        const i18nKey = `breadcrumb.${cleanPath.replace(/\//g, '.')}`;
        const translated = this.i18n.t(i18nKey);
        // If translation is the key itself, try fallback to capitalized segment
        label = translated !== i18nKey ? translated : this.capitalize(resolvedSegments.join(' '));
      } else {
        // Resolve dynamic placeholder/keys from route data
        label = this.i18n.t(label);
      }

      breadcrumbs.push({ label, url });
    }

    return breadcrumbs;
  }

  private capitalize(str: string): string {
    if (!str) return '';
    return str.charAt(0).toUpperCase() + str.slice(1);
  }
}
