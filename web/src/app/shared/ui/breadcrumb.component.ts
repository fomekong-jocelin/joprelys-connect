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
      <nav
        class="flex items-center space-x-2 px-4 md:px-6 py-3 border-b border-[var(--app-border)] bg-[var(--app-surface-muted)] text-xs font-semibold select-none"
        style="color:var(--text-muted)"
        aria-label="Fil d'Ariane"
      >
        <a
          routerLink="/"
          class="transition-colors"
          style="color:var(--text-muted)"
          [style.color]="'var(--text-muted)'"
          onmouseenter="this.style.color='var(--text-primary)'"
          onmouseleave="this.style.color='var(--text-muted)'"
        >
          {{ i18n.t('breadcrumb.home') }}
        </a>
        @for (item of breadcrumbs(); track item.url; let last = $last) {
          <span aria-hidden="true" style="color:var(--divider)">/</span>
          @if (last) {
            <span class="font-bold truncate max-w-[200px]" style="color:var(--text-primary)" aria-current="page">
              {{ item.label }}
            </span>
          } @else {
            <a
              [routerLink]="item.url"
              class="transition-colors truncate max-w-[150px]"
              style="color:var(--text-muted)"
              onmouseenter="this.style.color='var(--text-primary)'"
              onmouseleave="this.style.color='var(--text-muted)'"
            >
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
        const cleanPath = routeConfig.path.replace(/:[^\/]+/g, '*');
        const i18nKey = `breadcrumb.${cleanPath.replace(/\//g, '.')}`;
        const translated = this.i18n.t(i18nKey);
        label = translated !== i18nKey ? translated : this.capitalize(resolvedSegments.join(' '));
      } else {
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
