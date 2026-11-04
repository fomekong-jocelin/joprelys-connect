import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DashboardRouteService } from '../../auth/dashboard-route.service';
import { safeInternalUrl } from '../../auth/safe-return-url';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../layout/app-shell.component';
import { IconComponent } from '../ui/icon.component';

/**
 * Affiché quand le contrôle d'accès n'a pas pu joindre le serveur (réseau coupé, 5xx) :
 * l'utilisateur comprend la situation et relance la page demandée, au lieu d'un écran vide.
 */
@Component({
  selector: 'app-server-unavailable-page',
  imports: [AppShellComponent, IconComponent],
  template: `
    <app-shell>
      <section class="flex min-h-[60vh] items-center justify-center px-4 py-10" aria-labelledby="server-unavailable-title">
        <div class="ui-card w-full max-w-md p-6 text-center sm:p-8" role="alert">
          <span
            class="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-[var(--radius-brand-md)] border border-[var(--brand-warning-border)] bg-[var(--brand-warning-subtle)] text-xl text-[var(--brand-warning-text)]"
            aria-hidden="true"
          >
            <app-ui-icon name="exclamation-triangle" />
          </span>
          <h1 id="server-unavailable-title" class="font-display text-2xl font-bold text-[var(--text-primary)]">
            {{ i18n.t('serverUnavailable.title') }}
          </h1>
          <p class="mt-2 text-sm leading-6 text-[var(--text-secondary)]">{{ i18n.t('serverUnavailable.message') }}</p>
          <button
            type="button"
            class="ui-button ui-button-primary mt-6 min-h-11 w-full"
            [disabled]="retrying()"
            (click)="retry()"
            data-testid="server-unavailable-retry"
          >
            <app-ui-icon name="arrow-path" aria-hidden="true" />
            {{ i18n.t(retrying() ? 'serverUnavailable.retrying' : 'common.retry') }}
          </button>
        </div>
      </section>
    </app-shell>
  `,
})
export class ServerUnavailablePageComponent {
  readonly i18n = inject(I18nService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly dashboardRoute = inject(DashboardRouteService);

  readonly retrying = signal(false);

  /** Page à relancer : uniquement une URL interne, sinon le tableau de bord du rôle. */
  targetUrl(): string {
    return safeInternalUrl(this.route.snapshot.queryParamMap.get('returnUrl')) ?? this.dashboardRoute.url();
  }

  async retry(): Promise<void> {
    if (this.retrying()) return;
    this.retrying.set(true);
    try {
      await this.router.navigateByUrl(this.targetUrl());
    } finally {
      this.retrying.set(false);
    }
  }
}
