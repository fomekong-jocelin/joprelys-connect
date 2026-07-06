import { Injectable, inject } from '@angular/core';
import { Router, NavigationEnd, ActivatedRoute } from '@angular/router';
import { filter, map } from 'rxjs/operators';
import { APP_BRAND_CONFIG } from '../config/app-brand.config';
import { I18nService } from '../i18n/i18n.service';

@Injectable({ providedIn: 'root' })
export class AppTitleService {
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly i18n = inject(I18nService);
  private readonly brand = APP_BRAND_CONFIG;

  init(): void {
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        map(() => this.getDeepestTitle(this.activatedRoute))
      )
      .subscribe((title) => {
        const fullTitle = title ? `${title} | ${this.brand.appName}` : this.brand.appName;
        document.title = fullTitle;
      });
  }

  private getDeepestTitle(route: ActivatedRoute): string | null {
    let current: ActivatedRoute | null = route;
    let titleKey: string | null = null;

    while (current) {
      const routeTitle = current.snapshot.data?.['title'];
      if (routeTitle) {
        titleKey = routeTitle;
      }
      current = current.firstChild;
    }

    return titleKey ? this.i18n.t(titleKey) : null;
  }
}
