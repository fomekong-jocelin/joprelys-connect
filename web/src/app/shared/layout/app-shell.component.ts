import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthApiService } from '../../auth/auth-api.service';
import { AuthTokenStorageService } from '../../auth/auth-token-storage.service';
import { APP_BRAND_CONFIG } from '../../core/config/app-brand.config';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppLogoComponent } from '../ui/app-logo.component';
import { ButtonComponent } from '../ui/button.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [AppLogoComponent, ButtonComponent, RouterLink],
  template: `
    <main class="app-page flex min-h-screen flex-col">
      <header class="app-topbar">
        <div class="app-container flex flex-wrap items-center justify-between gap-3 py-4">
          <a routerLink="/dashboard" class="inline-flex w-fit">
            <app-logo />
          </a>

          @if (session(); as currentSession) {
            <div class="flex items-center gap-3">
              <div class="hidden text-right sm:flex sm:flex-col">
                <span class="font-display text-sm font-extrabold" style="color: var(--text-primary)">
                  {{ currentSession.name }}
                </span>
                <span class="text-[0.68rem] font-extrabold uppercase tracking-wider" style="color: var(--text-muted)">
                  {{ currentSession.role }}
                </span>
              </div>
              <div class="ui-avatar">{{ currentSession.name.charAt(0) }}</div>
              <app-ui-button variant="link" (pressed)="logout()">
                {{ logoutLabel() }}
              </app-ui-button>
            </div>
          }
        </div>
      </header>

      <section class="flex-1">
        <ng-content></ng-content>
      </section>

      <footer class="app-footer">
        <div class="app-container flex flex-col gap-2 py-5 text-xs font-semibold sm:flex-row sm:items-center sm:justify-between">
          <span>{{ copyrightLabel() }}</span>
          <div class="flex flex-wrap gap-3">
            <a class="ui-link" [href]="links.terms">{{ termsLabel() }}</a>
            <a class="ui-link" [href]="links.privacy">{{ privacyLabel() }}</a>
            <a class="ui-link" [href]="links.help">{{ helpLabel() }}</a>
          </div>
        </div>
      </footer>
    </main>
  `,
})
export class AppShellComponent {
  private readonly authApi = inject(AuthApiService);
  private readonly tokenStorage = inject(AuthTokenStorageService);
  private readonly router = inject(Router);
  private readonly i18n = inject(I18nService);

  readonly session = this.tokenStorage.session;
  readonly links = APP_BRAND_CONFIG.publicLinks;
  readonly logoutLabel = computed(() => this.i18n.t('shell.logout'));
  readonly termsLabel = computed(() => this.i18n.t('shell.terms'));
  readonly privacyLabel = computed(() => this.i18n.t('shell.privacy'));
  readonly helpLabel = computed(() => this.i18n.t('shell.help'));
  readonly copyrightLabel = computed(() =>
    this.i18n.t('shell.copyright').replace('{publisher}', APP_BRAND_CONFIG.publisherName)
  );

  logout(): void {
    this.authApi.logout().subscribe({
      next: () => this.router.navigate(['/']),
      error: () => this.router.navigate(['/']),
    });
  }
}
