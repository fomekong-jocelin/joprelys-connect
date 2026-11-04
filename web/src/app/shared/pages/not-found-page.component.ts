import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DashboardRouteService } from '../../auth/dashboard-route.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../layout/app-shell.component';
import { IconComponent } from '../ui/icon.component';

/** Page 404 : toute URL inconnue reste dans l'application et propose un retour au tableau de bord du rôle. */
@Component({
  selector: 'app-not-found-page',
  imports: [AppShellComponent, IconComponent, RouterLink],
  template: `
    <app-shell>
      <section class="flex min-h-[60vh] items-center justify-center px-4 py-10" aria-labelledby="not-found-title">
        <div class="ui-card w-full max-w-md p-6 text-center sm:p-8">
          <span
            class="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-[var(--radius-brand-md)] border border-[var(--brand-primary-border)] bg-[var(--brand-primary-subtle)] text-xl text-[var(--brand-primary)]"
            aria-hidden="true"
          >
            <app-ui-icon name="magnifying-glass" />
          </span>
          <p class="ui-label text-[var(--text-muted)]">{{ i18n.t('notFound.code') }}</p>
          <h1 id="not-found-title" class="mt-1 font-display text-2xl font-bold text-[var(--text-primary)]">
            {{ i18n.t('notFound.title') }}
          </h1>
          <p class="mt-2 text-sm leading-6 text-[var(--text-secondary)]">{{ i18n.t('notFound.message') }}</p>
          <a
            class="ui-button ui-button-primary mt-6 min-h-11 w-full"
            [routerLink]="dashboardRoute.url()"
            data-testid="not-found-home"
          >
            {{ i18n.t(dashboardRoute.hasSession() ? 'notFound.backToDashboard' : 'notFound.backToHome') }}
          </a>
        </div>
      </section>
    </app-shell>
  `,
})
export class NotFoundPageComponent {
  readonly i18n = inject(I18nService);
  readonly dashboardRoute = inject(DashboardRouteService);
}
