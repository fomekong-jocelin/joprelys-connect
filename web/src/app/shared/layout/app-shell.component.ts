import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthApiService } from '../../auth/auth-api.service';
import { AuthTokenStorageService } from '../../auth/auth-token-storage.service';
import { APP_BRAND_CONFIG, AppLocale } from '../../core/config/app-brand.config';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppLogoComponent } from '../ui/app-logo.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [AppLogoComponent, RouterLink],
  template: `
    <main class="app-page flex min-h-screen flex-col">
      <header class="app-topbar">
        <div class="app-container flex items-center justify-between py-4">
          <a routerLink="/dashboard" class="inline-flex w-fit">
            <app-logo />
          </a>

          <div class="flex items-center gap-4">
            <!-- Sélecteur de langue statique et élégant -->
            <div class="flex items-center gap-1.5 text-xs font-bold border border-[var(--app-border)] bg-[var(--app-surface-muted)] px-2.5 py-1.5 rounded-sm select-none">
              <button
                type="button"
                (click)="setLang('fr')"
                [class]="locale() === 'fr' ? 'text-brand-cyan font-extrabold pointer-events-none' : 'text-slate-400 dark:text-slate-500 hover:text-slate-600 dark:hover:text-slate-300 cursor-pointer'"
              >
                FR
              </button>
              <span class="text-slate-300 dark:text-slate-700">|</span>
              <button
                type="button"
                (click)="setLang('en')"
                [class]="locale() === 'en' ? 'text-brand-cyan font-extrabold pointer-events-none' : 'text-slate-400 dark:text-slate-500 hover:text-slate-600 dark:hover:text-slate-300 cursor-pointer'"
              >
                EN
              </button>
            </div>

            <!-- Séparateur vertical discret -->
            <div class="h-6 w-px bg-slate-200 dark:bg-slate-800"></div>

            @if (session(); as currentSession) {
              <!-- Groupe Profil Utilisateur -->
              <div class="flex items-center gap-3">
                <div class="hidden text-right sm:flex sm:flex-col justify-center">
                  <span class="font-display text-sm font-extrabold leading-none text-slate-800 dark:text-slate-200">
                    {{ currentSession.name }}
                  </span>
                  <span class="text-[0.68rem] font-extrabold uppercase tracking-wider mt-1 text-slate-400 dark:text-slate-500">
                    {{ currentSession.role }}
                  </span>
                </div>
                <div class="ui-avatar shadow-xs select-none">{{ currentSession.name.charAt(0) }}</div>
              </div>

              <!-- Bouton Déconnexion Premium - Icône uniquement de taille fixe -->
              <button
                type="button"
                (click)="logout()"
                [title]="logoutLabel()"
                class="inline-flex items-center justify-center w-9 h-9 rounded-sm border border-[var(--app-border)] bg-[var(--app-surface)] text-slate-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-950/20 hover:border-red-200 dark:hover:border-red-900 transition-all duration-150 cursor-pointer ml-1"
              >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" class="w-4.5 h-4.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75" />
                </svg>
              </button>
            }
          </div>
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
  readonly locale = this.i18n.locale;
  readonly links = APP_BRAND_CONFIG.publicLinks;
  readonly logoutLabel = computed(() => this.i18n.t('shell.logout'));
  readonly termsLabel = computed(() => this.i18n.t('shell.terms'));
  readonly privacyLabel = computed(() => this.i18n.t('shell.privacy'));
  readonly helpLabel = computed(() => this.i18n.t('shell.help'));
  readonly copyrightLabel = computed(() =>
    this.i18n.t('shell.copyright').replace('{publisher}', APP_BRAND_CONFIG.publisherName)
  );

  setLang(lang: AppLocale): void {
    this.i18n.setLocale(lang);
  }

  logout(): void {
    this.authApi.logout().subscribe({
      next: () => this.router.navigate(['/']),
      error: () => this.router.navigate(['/']),
    });
  }
}
